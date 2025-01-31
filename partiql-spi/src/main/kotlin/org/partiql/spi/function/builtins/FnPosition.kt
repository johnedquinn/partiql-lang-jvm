// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointPosition
import org.partiql.spi.value.Datum

internal val Fn_POSITION__STRING_STRING__INT64 = FunctionUtils.hidden(

    name = "position",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("probe", PType.string()),
        Parameter("value", PType.string()),
    ),

) { args ->
    val s1 = args[0].string
    val s2 = args[1].string
    val result = s2.codepointPosition(s1)
    Datum.bigint(result.toLong())
}

internal val Fn_POSITION__CLOB_CLOB__INT64 = FunctionUtils.hidden(

    name = "position",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("probe", PType.clob(Int.MAX_VALUE)),
        Parameter("value", PType.clob(Int.MAX_VALUE)),
    ),

) { args ->
    val s1 = args[0].bytes.toString(Charsets.UTF_8)
    val s2 = args[1].bytes.toString(Charsets.UTF_8)
    val result = s2.codepointPosition(s1)
    Datum.bigint(result.toLong())
}
