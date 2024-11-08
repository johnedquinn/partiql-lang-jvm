package org.partiql.cli.catalogs

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table

class ColumnarCatalog(private val catName: String, private val rowCount: Long, private val colCount: Int) : Catalog {

    override fun getName(): String {
        return catName
    }

    override fun getTable(session: Session, identifier: Identifier): Table? {
        when (identifier.getIdentifier().getText()) {
            "t1" -> {
                return ColumnarTable(
                    "t1",
                    "/Users/johqunn/Development/partiql-lang-jvm/partiql-cli/file_cols_20_rows_100000.arrow",
                    20
                )
            }
            "t2" -> {
                return ColumnarTable(
                    "t2",
                    "/Users/johqunn/Development/partiql-lang-jvm/partiql-cli/file_cols_100_rows_100000.arrow",
                    100
                )
            }
            "t3" -> {
                return ColumnarTable(
                    "t3",
                    "/Users/johqunn/Development/partiql-lang-jvm/partiql-cli/file_cols_1000_rows_100000.arrow",
                    1000
                )
            }
            "t4" -> {
                return ColumnarTable(
                    "t4",
                    "/Users/johqunn/Development/partiql-lang-jvm/partiql-cli/file_cols_20_rows_1000000.arrow",
                    20
                )
            }
            "t5" -> {
                return ColumnarTable(
                    "t5",
                    "/Users/johqunn/Development/partiql-lang-jvm/partiql-cli/file_cols_1000_rows_1000000.arrow",
                    1000
                )
            }
        }
        return null
    }
}
