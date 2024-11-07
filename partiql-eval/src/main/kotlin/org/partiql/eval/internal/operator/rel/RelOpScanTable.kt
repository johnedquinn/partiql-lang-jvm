package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.spi.RecordCursor
import org.partiql.spi.catalog.Table

internal class RelOpScanTable(
    private val expr: Table,
    private val columns: List<Int>
) : ExprRelation {

    private lateinit var records: Iterator<Row>

    override fun open(env: Environment) {
        records = RecordIterator(expr.getRecordSet().cursor, columns)
    }

    override fun hasNext(): Boolean = records.hasNext()

    override fun next(): Row {
        return records.next()
    }

    override fun close() {}

    private class RecordIterator(val rc: RecordCursor, val columns: List<Int>): Iterator<Row> {
        var hasNext = true
        var waiting = true
        override fun hasNext(): Boolean {
            if (waiting) {
                waiting = false
                hasNext = rc.next()
            }
            return hasNext
        }

        override fun next(): Row {
            waiting = true
            return Row(Array(columns.size) { rc.getDatum(columns[it]) })
        }

    }
}
