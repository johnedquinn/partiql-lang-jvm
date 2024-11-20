package org.partiql.iondb

import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.IdentifierChain
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprOperator

object AggregationSimplification : AstRewriter<Unit>() {
    override fun visitExprCall(node: ExprCall, ctx: Unit): AstNode {
        if (isAverage(node.function)) {
            return ExprOperator(
                "/",
                ExprCall(
                    IdentifierChain(Identifier("sum", false), null),
                    node.args,
                    node.setq
                ),
                ExprCall(
                    IdentifierChain(Identifier("count", false), null),
                    emptyList(),
                    node.setq
                )
            )
        }
        return super.visitExprCall(node, ctx)
    }

    private fun name(id: IdentifierChain): IdentifierChain {
        var current: IdentifierChain = id
        while (current.next != null) {
            current = current.next!!
        }
        return current
    }

    private fun isAverage(node: IdentifierChain): Boolean {
        return name(node).root.symbol.lowercase() == "avg"
    }
}
