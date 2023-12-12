package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelIntersect(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : Operator.Relation {

    private var seen: MutableSet<Record> = mutableSetOf()
    private var init: Boolean = false

    override fun open() {
        lhs.open()
        rhs.open()
        init = false
        seen = mutableSetOf()
    }

    override fun next(): Record? {
        if (!init) {
            seed()
        }
        while (true) {
            val row = rhs.next() ?: return null
            if (seen.contains(row)) {
                return row
            }
        }
    }

    override fun close() {
        lhs.close()
        rhs.close()
        seen.clear()
    }

    /**
     * Read the entire left-hand-side into our search structure.
     */
    private fun seed() {
        init = true
        while (true) {
            val row = lhs.next() ?: break
            seen.add(row)
        }
    }
}