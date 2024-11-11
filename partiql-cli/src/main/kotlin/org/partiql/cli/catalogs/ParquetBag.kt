package org.partiql.cli.catalogs

import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType

class ParquetBag(
    val rs: ParquetRecordSet
) : Datum {
    override fun getType(): PType = PType.bag()
    override fun iterator(): MutableIterator<Datum> {
        val cursor = rs.cursor
        return CursorToStructIterator(cursor)
    }

    private class CursorToStructIterator(val cursor: ParquetRecordCursor) : IteratorPeeking<Datum>() {
        val colCount = cursor.colSize
        val colIndices = 0 until colCount

        override fun peek(): Datum? {
            val hasNext = cursor.next()
            if (!hasNext) {
                return null
            }
            val fields = colIndices.map { i ->
                Field.of("col$i", cursor.getDatum(i))
            }
            return Datum.struct(fields)
        }
    }
}
