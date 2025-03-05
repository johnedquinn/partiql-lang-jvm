package org.partiql.eval.internal.window

import org.partiql.eval.WindowFunction
import org.partiql.plan.WindowFunctionSignature

internal object WindowBuiltIns {
    fun getImpl(signature: WindowFunctionSignature): WindowFunction {
        return when (signature.name) {
            "rank" -> RankFunction()
            else -> throw IllegalArgumentException("Unknown window function: ${signature.name}")
        }
    }
}
