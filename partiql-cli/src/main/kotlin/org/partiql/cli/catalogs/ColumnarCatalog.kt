package org.partiql.cli.catalogs

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table

class ColumnarCatalog(private val catName: String) : Catalog {

    override fun getName(): String {
        return catName
    }

    override fun getTable(session: Session, identifier: Identifier): Table? {
        when (identifier.getIdentifier().getText()) {
            "tbl" -> return ColumnarTable("tbl")
        }
        return null
    }
}