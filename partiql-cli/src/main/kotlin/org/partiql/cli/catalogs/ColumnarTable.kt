package org.partiql.cli.catalogs

import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.types.Field
import org.partiql.types.PType

class ColumnarTable(
    val name: String,
    val filePath: String,
    val colCount: Int
) : Table {

    override fun getFlags(): Int {
        return Table.ALLOWS_RECORD_SCAN
    }

    override fun getName(): Name {
        return Name.of(name)
    }

    override fun getSchema(): PType {
        return PType.bag(
            PType.row(
                List(colCount) { colIndex ->
                    Field.of("col$colIndex", PType.integer())
                }
            )
        )
    }

    override fun getDatum(): Datum {
        return ParquetBag(getRecordSet())
    }

    override fun getRecordSet(): ParquetRecordSet {
        return ParquetRecordSet(filePath)
    }
}
