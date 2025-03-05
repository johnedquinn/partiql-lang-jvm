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

    override fun openPeeking(env: Environment) {
        input.open(env)
        this._env = env
        _currentPartition = emptyArray()
        _orderingGroupStart = 1 // TODO
        _orderingGroupEnd = 1 // TODO
        functions.map { it.reset(object : WindowPartition {}) }
    }

    override fun peek(): Row? {
        if (input.hasNext().not()) {
            return null
        }

        // Get the sorted input
        val inputRow = input.next()
        val newEnv = _env.push(inputRow)

        // Determine if new partition
        val inputPartition = Array(partitionBy.size) { partitionBy[it].eval(newEnv) }
        val isNewPartition = comparator.compare(inputPartition, _currentPartition) != 0
        if (isNewPartition) {
            functions.map { it.reset(object : WindowPartition {}) } // Reset functions
            _orderingGroupStart = 1 // TODO
            _orderingGroupEnd = 1 // TODO
            _currentPartition = inputPartition
        }

        // Evaluate functions
        val functions = functions.map { it.eval(newEnv, _orderingGroupStart, _orderingGroupEnd) }
        val outputRow = inputRow.concat(Row.of(*functions.toTypedArray()))
        return outputRow
    }

    override fun closePeeking() {
        input.close()
    }
}
