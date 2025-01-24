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
 * Function (operator) for the `IS MISSING` special form. Its name is hidden via [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("is_missing")
internal val Fn_IS_MISSING__ANY__BOOL = FnOverload.Builder(name)
    .returns(PType.bool())
    .addParameter(Parameter("value", PType.dynamic()))
    .isNullCall(false)
    .isMissingCall(false)
    .body(IsMissingBody)
    .build()

private object IsMissingBody : Function<Array<Datum>, Datum> {
    override fun apply(args: Array<Datum>): Datum {
        return Datum.bool(args[0].isMissing)
    }
}
