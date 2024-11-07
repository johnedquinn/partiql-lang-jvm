package org.partiql.cli.catalogs

import org.partiql.spi.RecordCursor
import org.partiql.spi.value.Datum

class ColumnarRecordCursor(
    val iterator: Iterator<List<Datum>>
) : RecordCursor {
    private var data: List<Datum> = emptyList()

    override fun getDatum(field: Int): Datum {
        return data[field]
    }

    override fun next(): Boolean {
        if (iterator.hasNext()) {
            data = iterator.next()
            return true
        } else {
            return false
        }
    }
}
