package org.partiql.cli.catalogs

import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.ipc.ArrowStreamReader
import org.partiql.spi.RecordCursor
import org.partiql.spi.value.Datum

class ParquetRecordCursor(
    private val reader: ArrowStreamReader
) : RecordCursor {
    private var row = -1
    private var rowMax = 0
    private var needsNewBatch = true
    private val root = reader.vectorSchemaRoot
    val colSize = root.schema.fields.size

    override fun getDatum(field: Int): Datum {
        return try {
            Datum.integer((root.getVector(field) as IntVector).get(row))
        } catch (t: Throwable) {
            throw IllegalStateException("field: $field row: $row, rowMax: $rowMax", t)
        }
    }

    override fun next(): Boolean {
        if (needsNewBatch) {
            val loaded = reader.loadNextBatch()
            if (!loaded) {
                return false
            }
            rowMax = root.rowCount
            row = -1
            needsNewBatch = false
        }
        row++
        return row < rowMax
    }
}
