package org.partiql.iondb

import org.partiql.plan.Operation
import org.partiql.plan.Operator
import org.partiql.plan.Plan
import org.partiql.plan.Visitor
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum

internal object ConstantFoldPlanRewriter : Visitor<Any, Unit> {

    // Perform a constant fold
    // We are actively adding a rewriter to the plan, so this is a bit clunky.
    fun rewrite(plan: Plan): Plan {
        when (val planOp = plan.getOperation()) {
            is Operation.Query -> {
                val newRex = visit(planOp.getRex(), Unit)
                return object : Plan {
                    override fun getOperation(): Operation {
                        return object : Operation.Query {
                            override fun getRex(): Rex {
                                return newRex
                            }
                        }
                    }
                }
            }
            else -> error("Unexpected plan operation: $planOp")
        }
    }

    override fun defaultReturn(operator: Operator, ctx: Unit): Any {
        return operator
    }

    override fun visit(operator: Operator, ctx: Unit): Rex {
        return super.visit(operator, ctx) as Rex
    }

    //
    override fun visitCall(rex: RexCall, ctx: Unit): Rex {
        val fn: Function.Instance = rex.getFunction()
        val args = rex.getArgs().map { arg ->
            if (arg !is RexLit) {
                return rex
            }
            arg.getValue()
        }

        // This is in RC3, so name currently doesn't exist.
        // if (fn.name == "plus") {
        // val result = fn.invoke(args.toTypedArray())
        // return PlanFactory.STANDARD.rexLit(Datum.integer(1000))
        // }
        // return rex

        return PlanFactory.STANDARD.rexLit(Datum.integer(1000))
    }
}