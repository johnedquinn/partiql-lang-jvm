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
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar

class ProjectionPushdown : Visitor<PlanNode, Unit> {

    private val factory = PlanFactory.STANDARD
    private val projections = mutableMapOf<Int, MutableSet<Rex>>()
    private val projectsAll = mutableSetOf<Int>()

    private fun potentiallyAdd(root: Rex, pathExpression: Rex) {
        if (root is RexVar && root.getDepth() == 0) { // Only allow for single depth right now
            add(root.getOffset(), pathExpression)
        }
    }

    private fun add(local: Int, key: Rex) {
        if (!projections.containsKey(local)) {
            projections[local] = mutableSetOf()
        }
        projections[local]!!.add(key)
    }

    fun accept(plan: Plan): Plan {
        val root = visit(plan.getOperation(), Unit) as Operation
        return factory.plan(root)
    }

    override fun defaultReturn(operator: Operator, ctx: Unit): PlanNode {
        TODO("Not yet implemented")
    }

    override fun visitLit(rex: RexLit, ctx: Unit): Rex {
        return rex
    }

    override fun visitSelect(rex: RexSelect, ctx: Unit): PlanNode {
        val input = visit(rex.getInput(), ctx) as Rel
        val constructor = visit(rex.getConstructor(), ctx) as Rex
        return factory.rexSelect(input, constructor)
    }

    override fun visitProject(rel: RelProject, ctx: Unit): PlanNode {
        val input = visit(rel.getInput(), ctx) as Rel
        val projections = rel.getProjections().map { visit(it, ctx) as Rex }
        return factory.relProject(input, projections)
    }

    override fun visitFilter(rel: RelFilter, ctx: Unit): PlanNode {
        val input = visit(rel.getInput(), ctx) as Rel
        val predicate = visit(rel.getPredicate(), ctx) as Rex
        return factory.relFilter(input, predicate)
    }

    override fun visitStruct(rex: RexStruct, ctx: Unit): PlanNode {
        val fields = rex.getFields().map { f ->
            val key = visit(f.getKey(), ctx) as Rex
            val value = visit(f.getValue(), ctx) as Rex
            RexStruct.Field(key, value)
        }
        return factory.rexStruct(fields)
    }

    override fun visitCall(rex: RexCall, ctx: Unit): PlanNode {
        val args = rex.getArgs().map { visit(it, ctx) as Rex }
        val instance = rex.getFunction()
        return factory.rexCall(instance, args)
    }

    override fun visitQuery(node: Operation.Query, ctx: Unit): Operation.Query {
        val rex = visit(node.getRex(), ctx) as Rex
        return object : Operation.Query {
            override fun getRex(): Rex {
                return rex
            }

            override fun <R : Any?, C : Any?> accept(visitor: Visitor<R, C>, ctx: C): R {
                return visitor.visitQuery(this, ctx)
            }
        }
    }

    override fun visitPathKey(rex: RexPathKey, ctx: Unit): Rex {
        val root = visitExplicit(rex.getOperand(), ctx) as Rex
        val key = visitExplicit(rex.getKey(), ctx) as Rex
        val path = factory.rexPathKey(root, key)
        potentiallyAdd(root, key)
        return path
    }

    override fun visitPathSymbol(rex: RexPathSymbol, ctx: Unit): Rex {
        val root = visitExplicit(rex.getOperand(), ctx) as Rex
        val symbol = rex.getSymbol()
        val path = factory.rexPathSymbol(rex, symbol)
        potentiallyAdd(root, path)
        return path
    }

    private fun visitExplicit(rex: Rex, ctx: Unit): PlanNode {
        return when (rex) {
            is RexVar -> return rex // No need to add to the block-list. See [visitVar]
            else -> visit(rex, ctx)
        }
    }

    override fun visitVar(rex: RexVar, ctx: Unit): Rex {
        if (rex.getDepth() == 0) { // TODO: Only handle single depths rn
            projectsAll.add(rex.getOffset())
        }
        return rex
    }

    // TODO
    override fun visitTable(rex: RexTable, ctx: Unit): Rex {
        return rex
    }

    override fun visitScan(rel: RelScan, ctx: Unit): Rel {
        val input: Rex = visit(rel.getInput(), ctx) as Rex
        // If there are projections, return a scan-project
        if (!projections[0].isNullOrEmpty() && !projectsAll.contains(0)) {
            val scan = factory.relScan(input)
            return factory.relProject(scan, projections[0]!!.toList())
        }
        return factory.relScan(input)
    }
}