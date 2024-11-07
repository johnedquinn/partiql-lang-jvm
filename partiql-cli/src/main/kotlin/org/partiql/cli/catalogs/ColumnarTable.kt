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
    private val _colCount = 26
    private val _rowCount = 600_000L

    override fun getFlags(): Int {
        return Table.ALLOWS_RECORD_SCAN
    }

    override fun getName(): Name {
        return Name.of(name)
    }

    override fun getSchema(): PType {
        return getDataType(_colCount)
    }

    override fun getDatum(): Datum {
        return Datum.bag(DatumBag(getDataIterable(_rowCount, _colCount)))
    }

    override fun getRecordSet(): RecordSet {
        return ParquetRecordSet("file_cols_20_rows_100000.arrow")
    }

    private class DatumBag(val data: Iterable<List<Datum>>) : Iterable<Datum> {
        override fun iterator(): Iterator<Datum> {
            val dataIter = data.iterator()
            return object : Iterator<Datum> {
                override fun hasNext(): Boolean {
                    return dataIter.hasNext()
                }

                override fun next(): Datum {
                    val fieldList = dataIter.next()
                    val fields = fieldList.mapIndexed { idx, it ->
                        val name = idx.toString()
                        org.partiql.spi.value.Field.of(name, it)
                    }
                    return Datum.struct(fields)
                }
            }
        }
    }

    private fun getDataIterable(rowCount: Long, colCount: Int): Iterable<List<Datum>> {
        return object : Iterable<List<Datum>> {
            override fun iterator(): Iterator<List<Datum>> {
                return FakeDataIterator(rowCount, colCount)
            }
        }
    }

    private fun getDataType(colCount: Int): PType {
        return PType.bag(
            PType.row(
                List(colCount) { colIndex ->
                    Field.of(colIndex.toString(), PType.string())
                }
            )
        )
    }

    private class FakeDataIterator(val rowCount: Long, val colCount: Int) : Iterator<List<Datum>> {
        var rowIndex = 0L
        override fun hasNext(): Boolean {
            return rowIndex < rowCount
        }

        override fun next(): List<Datum> {
            return DatumList(colCount, rowIndex++)
        }
    }

    private class DatumList(override val size: Int, private val rowIndex: Long) : List<Datum> {
        override fun contains(element: Datum): Boolean {
            TODO("Not yet implemented")
        }

        override fun containsAll(elements: Collection<Datum>): Boolean {
            TODO("Not yet implemented")
        }

        override fun get(index: Int): Datum {
            return Datum.string("${rowIndex}_$index")
        }

        override fun isEmpty(): Boolean {
            TODO("Not yet implemented")
        }

        override fun iterator(): Iterator<Datum> {
            return object : Iterator<Datum> {
                var index = 0
                override fun hasNext(): Boolean {
                    return index < size
                }

                override fun next(): Datum {
                    index++
                    return Datum.string("${rowIndex}_$index")
                }
            }
        }

        override fun listIterator(): ListIterator<Datum> {
            TODO("Not yet implemented")
        }

        override fun listIterator(index: Int): ListIterator<Datum> {
            TODO("Not yet implemented")
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<Datum> {
            TODO("Not yet implemented")
        }

        override fun lastIndexOf(element: Datum): Int {
            TODO("Not yet implemented")
        }

        override fun indexOf(element: Datum): Int {
            TODO("Not yet implemented")
        }
    }
}
