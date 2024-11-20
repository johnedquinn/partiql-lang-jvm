package org.partiql.iondb

import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.types.PType

class IonTable(
    private val name: Name,
    private val path: String
) : Table{

    override fun getName(): Name {
        return name
    }

    override fun getSchema(): PType {
        return PType.dynamic()
    }

    override fun getDatum(): Datum {
        return Datum.bag(IonTableIterable(path))
    }

    private class IonTableIterable(private val path: String) : Iterable<Datum> {
        override fun iterator(): Iterator<Datum> {
            val stream = this::class.java.classLoader.getResourceAsStream(path) ?: error("Could not find file $path")
            val r = DatumReader.ion(stream)
            return IonTableIterator(r)
        }
    }

    private class IonTableIterator(private val reader: DatumReader) :  Iterator<Datum> {
        private var next: Datum? = null
        override fun hasNext(): Boolean {
            if (next == null) {
                next = peek()
            }
            return next != null
        }

        override fun next(): Datum {
            if (next == null) {
                next = peek()
            }
            val result = next ?: error("No more elements")
            next = null
            return result
        }

        private fun peek(): Datum? {
            return reader.next()
        }
    }
}