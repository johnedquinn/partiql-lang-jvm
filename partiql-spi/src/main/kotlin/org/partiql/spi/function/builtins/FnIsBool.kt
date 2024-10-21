// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_BOOL__ANY__BOOL = Function.static(
    name = "is_bool",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
) { args ->
    Datum.bool(args[0].type.kind == PType.Kind.BOOL)
}