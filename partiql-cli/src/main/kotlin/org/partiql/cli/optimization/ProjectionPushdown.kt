package org.partiql.cli.optimization

import org.partiql.plan.Operation
import org.partiql.plan.Operator
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Visitor
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelScanTable
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar
import org.partiql.spi.catalog.Table
import org.partiql.types.PType

class ProjectionPushdown : Visitor<PlanNode, ProjectionPushdown.Ctx> {

    class Ctx(
        val isInPath: Boolean
    )

    private val factory = PlanFactory.STANDARD
    private val projections = mutableMapOf<Int, MutableList<Rex>>()
    private val projectsAll = mutableSetOf<Int>()

    /**
     * @return the index of the inserted key; null if not inserted.
     */
    private fun potentiallyAdd(root: Rex, pathExpression: Rex): Rex {
        if (root is RexVar && root.getDepth() == 0) { // Only allow for single depth right now
            val index = add(root.getOffset(), pathExpression)
            return factory.rexVar(0, index)
        }
        return pathExpression
    }

    /**
     * @return the index of the inserted key
     */
    private fun add(local: Int, key: Rex): Int {
        if (!projections.containsKey(local)) {
            projections[local] = mutableListOf()
        }
        projections[local]!!.add(key)
        return projections[local]!!.lastIndex
    }

    fun accept(plan: Plan): Plan {
        val root = visit(plan.getOperation(), Ctx(false)) as Operation
        return factory.plan(root)
    }

    fun accept(node: PlanNode): PlanNode {
        return visit(node, Ctx(false))
    }

    override fun defaultReturn(operator: Operator, ctx: Ctx): PlanNode {
        TODO("Not yet implemented")
    }

    override fun visitLit(rex: RexLit, ctx: Ctx): Rex {
        return rex
    }

    override fun visitSelect(rex: RexSelect, ctx: Ctx): RexSelect {
        val constructor = visit(rex.getConstructor(), ctx) as Rex
        val embedded = ProjectionPushdown()
        val input = embedded.accept(rex.getInput()) as Rel
        return factory.rexSelect(input, constructor)
    }

    override fun visitProject(rel: RelProject, ctx: Ctx): PlanNode {
        val projections = rel.getProjections().map { visit(it, ctx) as Rex }
        val input = visit(rel.getInput(), ctx) as Rel
        return factory.relProject(input, projections)
    }

    override fun visitFilter(rel: RelFilter, ctx: Ctx): PlanNode {
        val predicate = visit(rel.getPredicate(), ctx) as Rex
        val input = visit(rel.getInput(), ctx) as Rel
        return factory.relFilter(input, predicate)
    }

    override fun visitStruct(rex: RexStruct, ctx: Ctx): PlanNode {
        val fields = rex.getFields().map { f ->
            val key = visit(f.getKey(), ctx) as Rex
            val value = visit(f.getValue(), ctx) as Rex
            RexStruct.Field(key, value)
        }
        return factory.rexStruct(fields)
    }

    override fun visitCall(rex: RexCall, ctx: Ctx): PlanNode {
        val args = rex.getArgs().map { visit(it, ctx) as Rex }
        val instance = rex.getFunction()
        return factory.rexCall(instance, args)
    }

    override fun visitQuery(node: Operation.Query, ctx: Ctx): Operation.Query {
        val rex = visit(node.getRex(), ctx) as Rex
        return factory.query(rex)
    }

    override fun visitPathKey(rex: RexPathKey, ctx: Ctx): Rex {
        val newCtx = Ctx(true)
        val root = visit(rex.getOperand(), newCtx) as Rex
        val key = visit(rex.getKey(), ctx) as Rex
        val path = factory.rexPathKey(root, key)
        return potentiallyAdd(root, path)
    }

    override fun visitPathSymbol(rex: RexPathSymbol, ctx: Ctx): Rex {
        val newCtx = Ctx(true)
        val root = visit(rex.getOperand(), newCtx) as Rex
        val symbol = rex.getSymbol()
        val path = factory.rexPathSymbol(root, symbol)
        return potentiallyAdd(root, path)
    }

    override fun visitVar(rex: RexVar, ctx: Ctx): Rex {
        potentiallyAdd(rex, rex) // TODO: Need to handle scenarios in which we project out the row/struct completely.
        if (!ctx.isInPath && rex.getDepth() == 0) { // TODO: Only handle single depths rn
            potentiallyAdd(rex, rex)
            projectsAll.add(rex.getOffset()) // TODO: Think about
        }
        return rex
    }

    // TODO
    override fun visitTable(rex: RexTable, ctx: Ctx): Rex {
        return rex
    }

    override fun visitScan(rel: RelScan, ctx: Ctx): Rel {
        val input: Rex = visit(rel.getInput(), ctx) as Rex
        // If there are projections, return a scan-project
        if (!projections[0].isNullOrEmpty()) { //  && !projectsAll.contains(0) TODO: Should we ignore if the scan rex is referenced directly? Probably.
            val scanTable = convertRexToScanTable(input)
            if (scanTable != null) {
                return scanTable
            }
            val scan = factory.relScan(input)
            return factory.relProject(scan, projections[0]!!.toList())
        }
        return factory.relScan(input)
    }

    /**
     * TODO
     */
    private fun convertRexToScanTable(input: Rex): RelScanTable? {
        if (input is RexTable) {
            val table = input.getTable()
            val type = table.getSchema()
            if ((table.getFlags() and Table.ALLOWS_RECORD_SCAN) == Table.ALLOWS_RECORD_SCAN && type.kind == PType.Kind.ROW) {
                val columnIndexes = projections[0]!!.map { proj ->
                    when (proj) {
                        is RexPathKey -> getIndexOfPathKey(type, proj) ?: return null
                        is RexPathSymbol -> getIndexOfPathSymbol(type, proj) ?: return null
                        else -> return null
                    }
                }
                return factory.relScanTable(input, columnIndexes)
            }
        }
        return null
    }

    /**
     * TODO
     * Assumes that [row] is definitely a ROW type.
     */
    private fun getIndexOfPathSymbol(row: PType, path: RexPathSymbol): Int? {
        val keyString = path.getSymbol()
        row.fields.forEachIndexed { index, field ->
            if (field.name.equals(keyString, true)) {
                return index
            }
        }
        return null
    }

    /**
     * TODO
     * Assumes that [row] is definitely a ROW type.
     */
    private fun getIndexOfPathKey(row: PType, path: RexPathKey): Int? {
        val keyString = getString(path.getKey()) ?: return null
        row.fields.forEachIndexed { index, field ->
            if (field.name == keyString) {
                return index
            }
        }
        return null
    }

    private fun getString(key: Rex): String? {
        if (key !is RexLit) {
            return null
        }
        val datum = key.getValue()
        if (datum.isNull || datum.isMissing) {
            return null
        }
        return when (datum.type.kind) {
            PType.Kind.STRING, PType.Kind.CHAR, PType.Kind.VARCHAR -> datum.string
            else -> null
        }
    }
}