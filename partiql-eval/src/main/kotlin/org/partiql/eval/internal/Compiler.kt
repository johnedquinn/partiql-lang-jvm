package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelDistinct
import org.partiql.eval.internal.operator.rel.RelExclude
import org.partiql.eval.internal.operator.rel.RelFilter
import org.partiql.eval.internal.operator.rel.RelJoinInner
import org.partiql.eval.internal.operator.rel.RelJoinLeft
import org.partiql.eval.internal.operator.rel.RelJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelJoinRight
import org.partiql.eval.internal.operator.rel.RelProject
import org.partiql.eval.internal.operator.rel.RelScan
import org.partiql.eval.internal.operator.rel.RelScanIndexed
import org.partiql.eval.internal.operator.rel.RelScanIndexedPermissive
import org.partiql.eval.internal.operator.rel.RelScanPermissive
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCallStatic
import org.partiql.eval.internal.operator.rex.ExprCase
import org.partiql.eval.internal.operator.rex.ExprCast
import org.partiql.eval.internal.operator.rex.ExprCollection
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPivotPermissive
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStruct
import org.partiql.eval.internal.operator.rex.ExprTupleUnion
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.plan.Catalog
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Ref
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import java.lang.IllegalStateException

internal class Compiler(
    private val plan: PartiQLPlan,
    private val session: PartiQLEngine.Session
) : PlanBaseVisitor<Operator, Symbols>() {

    fun compile(): Operator.Expr {
        // 1. Validate all references
        val symbols = Symbols.build(plan, session)
        // 2. Compile with built symbols
        return visitPartiQLPlan(plan, symbols)
    }

    override fun defaultReturn(node: PlanNode, ctx: Symbols): Operator {
        TODO("Not yet implemented")
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: Symbols): Operator {
        val message = buildString {
            this.appendLine(node.message)
            PlanPrinter.append(this, plan)
        }
        throw IllegalStateException(message)
    }

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: Symbols): Operator {
        throw IllegalStateException(node.message)
    }

    // TODO: Re-look at
    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Symbols): Operator.Expr {
        return visitStatement(node.statement, ctx) as Operator.Expr
    }

    // TODO: Re-look at
    override fun visitStatementQuery(node: Statement.Query, ctx: Symbols): Operator.Expr {
        return visitRex(node.root, ctx).modeHandled()
    }

    // REX

    override fun visitRex(node: Rex, ctx: Symbols): Operator.Expr {
        return super.visitRexOp(node.op, ctx) as Operator.Expr
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Symbols): Operator {
        val values = node.values.map { visitRex(it, ctx).modeHandled() }
        return ExprCollection(values)
    }
    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Symbols): Operator {
        val fields = node.fields.map {
            val value = visitRex(it.v, ctx).modeHandled()
            ExprStruct.Field(visitRex(it.k, ctx), value)
        }
        return ExprStruct(fields)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Symbols): Operator {
        val rel = visitRel(node.rel, ctx)
        val constructor = visitRex(node.constructor, ctx).modeHandled()
        return ExprSelect(rel, constructor)
    }

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: Symbols): Operator {
        val rel = visitRel(node.rel, ctx)
        val key = visitRex(node.key, ctx)
        val value = visitRex(node.value, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPivotPermissive(rel, key, value)
            PartiQLEngine.Mode.STRICT -> ExprPivot(rel, key, value)
        }
    }
    override fun visitRexOpVar(node: Rex.Op.Var, ctx: Symbols): Operator {
        return ExprVar(node.ref)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: Symbols): Operator = ctx.getGlobal(node.ref)

    override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Symbols): Operator {
        val root = visitRex(node.root, ctx)
        val key = visitRex(node.key, ctx)
        return ExprPathKey(root, key)
    }

    override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: Symbols): Operator {
        val root = visitRex(node.root, ctx)
        val symbol = node.key
        return ExprPathSymbol(root, symbol)
    }

    override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Symbols): Operator {
        val root = visitRex(node.root, ctx)
        val index = visitRex(node.key, ctx)
        return ExprPathIndex(root, index)
    }

    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: Symbols): Operator {
        val fn = ctx.getFn(node.fn)
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        val fnTakesInMissing = fn.signature.parameters.any {
            it.type == PartiQLValueType.MISSING || it.type == PartiQLValueType.ANY
        }
        return when (fnTakesInMissing) {
            true -> ExprCallStatic(fn, args.map { it.modeHandled() }.toTypedArray())
            false -> ExprCallStatic(fn, args)
        }
    }

    @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: Symbols): Operator {
        val args = node.args.map { visitRex(it, ctx).modeHandled() }.toTypedArray()
        val candidates = node.candidates.map { candidate ->
            val fn = ctx.getFn(candidate.fn)
            val types = fn.signature.parameters.map { it.type }.toTypedArray()
            val coercions = candidate.coercions.toTypedArray()
            ExprCallDynamic.Candidate(fn, types, coercions)
        }
        return ExprCallDynamic(candidates, args)
    }

    override fun visitRexOpCast(node: Rex.Op.Cast, ctx: Symbols): Operator {
        return ExprCast(visitRex(node.arg, ctx), node.cast)
    }

    // REL
    override fun visitRel(node: Rel, ctx: Symbols): Operator.Relation {
        return super.visitRelOp(node.op, ctx) as Operator.Relation
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Symbols): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScan(rex)
        }
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Symbols): Operator {
        val input = visitRel(node.input, ctx)
        val projections = node.projections.map { visitRex(it, ctx).modeHandled() }
        return RelProject(input, projections)
    }

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Symbols): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanIndexedPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScanIndexed(rex)
        }
    }

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Symbols): Operator {
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        return ExprTupleUnion(args)
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Symbols): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        val condition = visitRex(node.rex, ctx)
        return when (node.type) {
            Rel.Op.Join.Type.INNER -> RelJoinInner(lhs, rhs, condition)
            Rel.Op.Join.Type.LEFT -> RelJoinLeft(lhs, rhs, condition)
            Rel.Op.Join.Type.RIGHT -> RelJoinRight(lhs, rhs, condition)
            Rel.Op.Join.Type.FULL -> RelJoinOuterFull(lhs, rhs, condition)
        }
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: Symbols): Operator {
        val branches = node.branches.map { branch ->
            visitRex(branch.condition, ctx) to visitRex(branch.rex, ctx)
        }
        val default = visitRex(node.default, ctx)
        return ExprCase(branches, default)
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Symbols): Operator {
        return ExprLiteral(node.value)
    }

    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Symbols): Operator {
        val input = visitRel(node.input, ctx)
        return RelDistinct(input)
    }

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Symbols): Operator {
        val input = visitRel(node.input, ctx)
        val condition = visitRex(node.predicate, ctx)
        return RelFilter(input, condition)
    }

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Symbols): Operator {
        val input = visitRel(node.input, ctx)
        return RelExclude(input, node.paths)
    }

    // HELPERS

    private fun Operator.Expr.modeHandled(): Operator.Expr {
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPermissive(this)
            PartiQLEngine.Mode.STRICT -> this
        }
    }

    /**
     * Get a typed catalog item from a reference
     *
     * @param T
     * @return
     */
    private inline fun <reified T : Catalog.Item> Ref.get(): T {
        val item = plan.catalogs.getOrNull(catalog)?.items?.get(symbol)
        if (item == null || item !is T) {
            error("Invalid catalog reference, $this for type ${T::class}")
        }
        return item
    }
}