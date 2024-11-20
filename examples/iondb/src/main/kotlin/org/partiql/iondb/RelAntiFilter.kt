package org.partiql.iondb

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.spi.value.Datum
import org.partiql.types.PType

class RelAntiFilter(
    val input: ExprRelation,
    val predicate: ExprValue
) : ExprRelation {

    var next: Row? = null
    var env: Environment? = null

    override fun close() {
        input.close()
    }

    override fun hasNext(): Boolean {
        if (next == null) {
            next = peek(env!!)
        }
        return next != null
    }

    override fun next(): Row {
        if (next == null) {
            next = peek(env!!)
        }
        val result = next ?: throw IllegalStateException("No more rows")
        next = null
        return result
    }

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
    }

    private fun peek(env: Environment): Row? {
        while (input.hasNext()) {
            val row = input.next()
            val newEnv = env.push(row)
            val result = predicate.eval(newEnv)
            // This is the anti-filter
            if (!isTrue(result)) {
                return row
            }
        }
        if (input.hasNext()) {
            return input.next()
        }
        return null
    }

    private fun isTrue(d: Datum): Boolean {
        val isAbsent = d.isNull || d.isMissing
        if (isAbsent) {
            return false
        }
        return d.type.kind == PType.Kind.BOOL && d.boolean
    }
}
