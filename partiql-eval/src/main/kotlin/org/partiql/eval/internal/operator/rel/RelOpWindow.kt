package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition

internal class RelOpWindow(
    private val input: ExprRelation,
    private val functions: List<WindowFunction>,
    private val partitionBy: List<ExprValue>,
    private val sortBy: List<Collation>
) : RelOpPeeking() {

    private lateinit var _env: Environment
    private var _orderingGroupStart: Long = 0
    private var _orderingGroupEnd: Long = 0

    override fun openPeeking(env: Environment) {
        input.open(env)
        this._env = env

        functions.map { it.reset(object : WindowPartition {}) } // TODO: Determine when to reset
    }

    override fun peek(): Row? {
        if (input.hasNext().not()) return null
        val inputRow = input.next()
        val newEnv = _env.push(inputRow)
        val functions = functions.map {
            it.eval(newEnv, 1, 1) // TODO: Pass in ordering group info
        }
        val outputRow = inputRow.concat(Row.of(*functions.toTypedArray()))
        return outputRow
    }

    override fun closePeeking() {
        input.close()
    }
}
