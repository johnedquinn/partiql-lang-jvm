package org.partiql.spi.function

import org.partiql.spi.types.PType
import java.util.concurrent.Callable

/**
 * Represents a SQL aggregation function, such as MAX, MIN, SUM, etc.
 */
internal object Aggregation {

    /**
     * @param name
     * @param parameters
     * @param returns
     * @param accumulator
     * @return
     */
    @JvmStatic
    inline fun overload(
        name: String,
        parameters: Array<Parameter>,
        returns: PType,
        crossinline accumulator: () -> Accumulator,
    ): AggOverload {
        return AggOverload.Builder(name)
            .returns(returns)
            .addParameters(*parameters.map { it.getType() }.toTypedArray())
            .body { accumulator.invoke() }
            .build()
    }
}
