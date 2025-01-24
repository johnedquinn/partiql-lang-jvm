// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.function.Function

/**
 * This is the boolean NOT predicate. Its name is hidden via the use of [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("not")
internal val Fn_NOT__BOOL__BOOL = FnOverload.Builder(name)
    .isNullCall(true)
    .isMissingCall(false)
    .addParameter(Parameter("value", PType.dynamic()))
    .returns(PType.bool())
    .body(FnNotImpl)
    .build()

private object FnNotImpl : Function<Array<Datum>, Datum> {
    override fun apply(t: Array<Datum>): Datum {
        val arg = t[0]
        if (arg.isMissing) {
            return Datum.nullValue(PType.bool())
        }
        val value = arg.boolean
        return Datum.bool(value.not())
    }
}