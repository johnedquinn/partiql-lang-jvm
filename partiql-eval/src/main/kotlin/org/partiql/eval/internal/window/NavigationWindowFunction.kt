package org.partiql.eval.internal.window

import org.partiql.eval.ExprValue
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

/**
 * This abstract class holds some common logic for navigation window function, i.e., LAG, LEAD
 */
internal abstract class NavigationWindowFunction(
    private val arguments: List<ExprValue>
) : WindowFunction {

    override fun reset(partition: WindowPartition) {
        // TODO
    }

    // TODO: Implement
    override fun processRow(): Datum {
        return Datum.bigint(1)
    }
}
