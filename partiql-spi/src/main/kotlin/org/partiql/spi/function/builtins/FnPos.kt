// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

internal val Fn_POS__INT8__INT8 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.tinyint(),
    parameters = arrayOf(Parameter("value", PType.tinyint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT16__INT16 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT32__INT32 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),

) { args ->
    args[0]
}

internal val Fn_POS__INT64__INT64 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),

) { args ->
    args[0]
}

internal val Fn_POS__INT__INT = FunctionUtils.hidden(

    name = "pos",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(Parameter("value", DefaultNumeric.NUMERIC)),

) { args ->
    args[0]
}

internal val Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = FunctionUtils.hidden(

    name = "pos",
    returns = PType.decimal(38, 19), // TODO: Rewrite this using the new modeling
    parameters = arrayOf(Parameter("value", PType.decimal(38, 19))),

) { args ->
    args[0]
}

internal val Fn_POS__FLOAT32__FLOAT32 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),

) { args ->
    args[0]
}

internal val Fn_POS__FLOAT64__FLOAT64 = FunctionUtils.hidden(

    name = "pos",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),

) { args ->
    args[0]
}
