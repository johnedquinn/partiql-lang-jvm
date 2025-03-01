package org.partiql.planner.internal

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.types.PType

/**
 * Basic catalog implementation used for testing; consider merging with MemoryConnector?
 *
 * TODO COMBINE WITH MemoryCatalog as the standard catalog implementation.
 */
class TestCatalog private constructor(
    private val name: String,
    private val root: Tree,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        var curr: Tree = root
        for (part in name) {
            curr = curr.get(Identifier.delimited(part).first()) ?: break
        }
        return curr.table
    }

    override fun resolveTable(session: Session, identifier: Identifier): Name? {
        var curr: Tree = root
        for (part in identifier) {
            curr = curr.get(part) ?: break
        }
        return curr.table?.getName()
    }

    private class Tree(
        @JvmField val name: String,
        @JvmField var table: Table?,
        @JvmField val children: MutableMap<String, Tree>,
    ) {

        /**
         * TODO ambiguous binding error?
         */
        fun get(part: Identifier.Simple): Tree? {
            // regular, search insensitively
            if (part.isRegular()) {
                for (child in children.values) {
                    if (part.matches(child.name)) {
                        return child
                    }
                }
            }
            // delimited, search exact
            return children[part.getText()]
        }

        fun getOrPut(name: String): Tree = children.getOrPut(name) { Tree(name, null, mutableMapOf()) }
    }

    override fun toString(): String = buildString {
        for (child in root.children.values) {
            append(toString(child))
        }
    }

    private fun toString(tree: Tree, prefix: String? = null): String = buildString {
        val pre = if (prefix != null) prefix + "." + tree.name else tree.name
        appendLine(pre)
        for (child in tree.children.values) {
            append(toString(child, pre))
        }
    }

    companion object {

        @JvmStatic
        fun empty(name: String): TestCatalog = TestCatalog(name, Tree(name, null, mutableMapOf()))

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * Perhaps this will be moved to Catalog.
     */
    class Builder {

        private var name: String? = null
        private val root = Tree(".", null, mutableMapOf())

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun createTable(name: Name, schema: PType): Builder {
            var curr = root
            for (part in name) {
                // upsert namespaces
                curr = curr.getOrPut(part)
            }
            curr.table = Table.empty(name, schema)
            return this
        }

        fun build(): Catalog {
            if (name == null) {
                error("Catalog must have a name")
            }
            return TestCatalog(name!!, root)
        }
    }
}
