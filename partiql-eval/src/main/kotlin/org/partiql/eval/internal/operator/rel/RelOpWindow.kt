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
    private var _orderingGroupStart: Long = 0
    private var _orderingGroupEnd: Long = 0
    private var _partitionNumber: Long = 0
    private var _orderingGroupNumber: Long = 0
    private var _partition: List<Row> = emptyList()
    private var leftoverRow: Row? = null

    override fun openPeeking(env: Environment) {
        input.open(env)
        this._env = env
        _currentPartition = emptyArray()
        _orderingGroupStart = 1 // TODO
        _orderingGroupEnd = 1 // TODO
        _partitionNumber = -1L
        functions.map { it.reset(object : WindowPartition {}) }
    }

    override fun peek(): Row? {
        // Check if there is an existing partition. If so, evaluate and return.
        _partitionNumber++
        if (_partition.size > _partitionNumber) {
            return produceResult()
        }

        // Create new partition's first row
        _partitionNumber = 0L
        val newPartition = mutableListOf<Row>()
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
        val newEnv = _env.push(firstRow)
        val firstRowPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(newEnv) }

        // Add partition's remaining rows
        while (input.hasNext()) {
            val nextRow = input.next()
            val nextEnv = _env.push(firstRow)
            val nextPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(nextEnv) }
            val isNewPartition = comparator.compare(firstRowPartitionKeys, nextPartitionKeys) != 0
            if (isNewPartition) {
                leftoverRow = nextRow
                break
            } else {
                newPartition.add(nextRow)
            }
        }
        _partition = newPartition
        return produceResult()
    }

    /**
     * This produces a result using the existing partition.
     */
    private fun produceResult(): Row {
        val row = _partition[_partitionNumber.toInt()]
        val newEnv = _env.push(row)
        val functions = functions.map { it.eval(newEnv, _orderingGroupStart, _orderingGroupEnd) }
        val outputRow = row.concat(Row.of(*functions.toTypedArray()))
        return outputRow
    }

    override fun closePeeking() {
        input.close()
    }
}
