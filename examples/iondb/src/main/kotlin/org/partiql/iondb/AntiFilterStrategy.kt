package org.partiql.iondb

import org.partiql.eval.Expr
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.compiler.Match
import org.partiql.eval.compiler.Pattern
import org.partiql.eval.compiler.Strategy
import org.partiql.plan.rel.RelFilter

object AntiFilterStrategy : Strategy(Pattern(RelFilter::class.java)) {
    override fun apply(match: Match): Expr {
        val children = match.children(0)
        val input = children[0] as ExprRelation
        val rex = children.getOrNull(1) ?: error("Match does not have index 1: $match")
        return RelAntiFilter(input, rex as ExprValue)
    }
}