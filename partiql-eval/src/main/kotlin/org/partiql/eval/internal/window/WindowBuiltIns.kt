package org.partiql.eval.internal.window

import org.partiql.eval.WindowFunction
import org.partiql.plan.WindowFunctionSignature

internal object WindowBuiltIns {
    fun get(signature: WindowFunctionSignature): WindowFunction {
        return when (signature.name) {
            "row_number" -> RowNumberFunction()
            "rank" -> RankFunction()
            "dense_rank" -> TODO()
            "percent_rank" -> TODO()
            "cume_dist" -> TODO()
            "ntile" -> TODO()
            "lag" -> TODO()
            "lead" -> TODO()
            "first_value" -> TODO()
            "last_value" -> TODO()
            "nth_value" -> TODO()
            else -> throw IllegalArgumentException("Unknown window function: ${signature.name}")
        }
    }
}
