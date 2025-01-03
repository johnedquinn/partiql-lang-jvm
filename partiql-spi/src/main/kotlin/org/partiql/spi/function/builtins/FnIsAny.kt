// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal val Fn_IS_ANY__ANY__BOOL = Function.static(
    name = "is_any",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
    isNullCall = true,
) { _ ->
    TODO("Function is_any not implemented")
}
