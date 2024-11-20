package org.partiql.iondb

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Function

class IonCatalog(
    private val name: String
) : Catalog {
    override fun getName(): String {
        return name
    }

    override fun getTable(session: Session, identifier: Identifier): Table? {
        val normalizedIdentifier = normalize(identifier)
        val path = normalizedIdentifier.joinToString("/") + ".ion"

        // Check that the file exists
        this::class.java.classLoader.getResourceAsStream(path) ?: return null

        // Return the table reference
        val name = Name.of(normalizedIdentifier)
        return IonTable(name, path)
    }

    override fun getTable(session: Session, name: Name): Table? {
        val path = name.joinToString("/") + ".ion"
        this::class.java.classLoader.getResourceAsStream(path) ?: return null
        return IonTable(name, path)
    }

    private fun normalize(identifier: Identifier): List<String> {
        return identifier.getParts().map { part ->
            when (part.isRegular()) {
                true -> part.getText().toUpperCase()
                false -> part.getText()
            }
        }
    }

    override fun getFunctions(session: Session, name: String): Collection<Function> {
        val fns = super.getFunctions(session, name) // We are currently making it so that you don't have to do this.
        return fns + listOf(FunctionRandom)
    }
}
