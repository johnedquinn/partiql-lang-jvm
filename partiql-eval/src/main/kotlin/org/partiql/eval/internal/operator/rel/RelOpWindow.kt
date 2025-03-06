package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.spi.value.Datum

/**
 * Assume input has been sorted.
 */
internal class RelOpWindow(
    private val input: ExprRelation,
    private val functions: List<WindowFunction>,
    private val partitionBy: List<ExprValue>,
    private val sortBy: List<Collation>
) : RelOpPeeking() {

    private lateinit var _env: Environment
    private val comparator = DatumArrayComparator
    private lateinit var _currentPartition: Array<Datum>
    private var _partitionPeekingNumber: Long = 0
    private var _partition: LocalPartition = LocalPartition()
    private var leftoverRow: Row? = null

    override fun openPeeking(env: Environment) {
        input.open(env)
        this._env = env
        _currentPartition = emptyArray()
        _partitionPeekingNumber = -1L
        functions.map { it.reset(object : WindowPartition {}) }
    }

    override fun peek(): Row? {
        // Check if there is an existing partition. If so, evaluate and return.
        _partitionPeekingNumber++
        if (_partition.size() > _partitionPeekingNumber) {
            return produceResult()
        }
        _partitionPeekingNumber = 0L

        // Create new partition's first row
        var partitionCreationIndex = 0L
        val newPartition = mutableListOf<Row>()
        val newLocalPartition = LocalPartition()
        val firstRow = when {
            leftoverRow != null -> {
                val tempRow = leftoverRow!!
                leftoverRow = null
                tempRow
            }
            else -> when (input.hasNext()) {
                true -> input.next()
                false -> return null
            }
        }
        newPartition.add(firstRow)
        val infoIndex = newLocalPartition.add(OrderingInfo(partitionCreationIndex))
        newLocalPartition.add(firstRow, infoIndex)
        val newEnv = _env.push(firstRow)
        val firstRowPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(newEnv) }
        var previousRowSortKeys = Array(partitionBy.size) { sortBy[it].expr.eval(newEnv) }

        // Add partition's remaining rows
        while (input.hasNext()) {
            partitionCreationIndex++
            val nextRow = input.next()
            val nextEnv = _env.push(firstRow)

            // Stop if at next partition
            val nextPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(nextEnv) }
            val isNewPartition = comparator.compare(firstRowPartitionKeys, nextPartitionKeys) != 0
            if (isNewPartition) {
                leftoverRow = nextRow
                break
            } else {
                newPartition.add(nextRow)
                newLocalPartition.add(nextRow, infoIndex)
            }

            // Update ordering info (if necessary)
            val nextSortKeys = Array(partitionBy.size) { sortBy[it].expr.eval(nextEnv) }
            val isNewSortGroup = comparator.compare(previousRowSortKeys, nextSortKeys) != 0
            if (isNewSortGroup) {
                val info = newLocalPartition.getInfo(infoIndex)
                info.orderingEnd = partitionCreationIndex - 1 // TODO: Check this
                previousRowSortKeys = nextSortKeys
            }
        }
        _partition = newLocalPartition
        return produceResult()
    }

    private class LocalPartition {
        private val rows: MutableList<Row> = mutableListOf()
        private val orderingInfo: MutableList<OrderingInfo> = mutableListOf()
        private val orderingMap = mutableListOf<Int>()

        fun add(row: Row, info: Int): Int {
            val toReturn = rows.size
            rows.add(row)
            orderingMap.add(info)
            return toReturn
        }

        operator fun get(index: Int): Row {
            return rows[index]
        }

        fun add(info: OrderingInfo): Int {
            val toReturn = orderingInfo.size
            orderingInfo.add(info)
            return toReturn
        }

        fun getInfo(index: Int): OrderingInfo {
            return orderingInfo[orderingMap[index]]
        }

        fun size(): Int {
            return rows.size
        }
    }

    private class OrderingInfo(
        start: Long,
    ) {
        var orderingStart: Long = start
        var orderingEnd: Long = 0
    }

    /**
     * This produces a result using the existing partition.
     */
    private fun produceResult(): Row {
        val row = _partition[_partitionPeekingNumber.toInt()]
        val info = _partition.getInfo(_partitionPeekingNumber.toInt())
        val newEnv = _env.push(row)
        val functions = functions.map { it.eval(newEnv, info.orderingStart, info.orderingEnd) }
        val outputRow = row.concat(Row.of(*functions.toTypedArray()))
        return outputRow
    }

    override fun closePeeking() {
        input.close()
    }
}
