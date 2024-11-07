package org.partiql.cli.catalogs

import org.partiql.spi.RecordCursor
import org.partiql.spi.RecordSet
import org.partiql.spi.value.Datum

class ColumnarRecordSet(
    val data: Iterable<List<Datum>>
) : RecordSet {
    override fun getCursor(): RecordCursor {
        return ColumnarRecordCursor(data.iterator())
    }
}
