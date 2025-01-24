// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.function.Function

/**
 * Function (operator) for the `IS NULL` special form. Its name is hidden via [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("is_null")
internal val Fn_IS_NULL__ANY__BOOL = FnOverload.Builder(name)
    .addParameter(PType.dynamic())
    .returns(PType.bool())
    .isNullCall(false)
    .isMissingCall(false)
    .body(FnIsNull)
    .build()

private object FnIsNull : Function<Array<Datum>, Datum> {
    
    override fun apply(t: Array<Datum>): Datum {
        if (t[0].isMissing) {
            return Datum.bool(true)
        }
        return Datum.bool(t[0].isNull)
    }
}
