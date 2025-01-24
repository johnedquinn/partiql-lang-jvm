// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.function.Function

private val NAME = FunctionUtils.hide("in_collection")
internal val FnInCollection = FnOverload.Builder(NAME)
    .addParameters(PType.dynamic(), PType.bag())
    .returns(PType.bool())
    .body(Body)
    .build()

private object Body : Function<Array<Datum>, Datum> {
    private val comparator = Datum.comparator()
    override fun apply(t: Array<Datum>): Datum {
        val value = t[0]
        val collection = t[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (comparator.compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}
