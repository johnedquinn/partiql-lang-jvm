package org.partiql.cli.catalogs

import org.partiql.spi.RecordSet
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.types.Field
import org.partiql.types.PType

class ColumnarTable(
    val name: String,
) : Table {

    private val type = PType.bag(
        PType.row(
            Field.of("a", PType.integer()),
            Field.of("b", PType.integer()),
            Field.of("c", PType.integer()),
            Field.of("d", PType.integer()),
            Field.of("e", PType.integer()),
            Field.of("f", PType.integer()),
            Field.of("g", PType.integer()),
            Field.of("h", PType.integer()),
            Field.of("i", PType.integer()),
            Field.of("j", PType.integer()),
        )
    )

    private val columns = List(10_000) { rowIndex ->
        List(10) { colIndex ->
            val cell = (rowIndex * 10) + colIndex
            Datum.integer(cell)
        }
    }

    override fun getFlags(): Int {
        return Table.ALLOWS_RECORD_SCAN
    }

    override fun getName(): Name {
        return Name.of(name)
    }

    override fun getSchema(): PType {
        return type
    }

    override fun getDatum(): Datum {
        return Datum.bag(DatumBag(columns))
    }

    override fun getRecordSet(): RecordSet {
        return ColumnarRecordSet(columns)
    }

    private class DatumBag(val data: List<List<Datum>>) : Iterable<Datum> {
        override fun iterator(): Iterator<Datum> {
            return DatumIterator(data.iterator())
        }
    }

    private class DatumIterator(val iter: Iterator<List<Datum>>): Iterator<Datum> {
        override fun hasNext(): Boolean {
            return iter.hasNext()
        }

        override fun next(): Datum {
            val elements = iter.next()
            val fields = elements.mapIndexed { index, value ->
                val alphaIndex = index + 97
                org.partiql.spi.value.Field.of((alphaIndex.toChar()).toString(), value)
            }
            return Datum.struct(fields)
        }
    }
}