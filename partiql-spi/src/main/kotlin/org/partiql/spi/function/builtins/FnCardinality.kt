package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_CARDINALITY__BAG__INT32 = Function.overload(

    name = "cardinality",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.bag()),
    ),

) { args ->
    val container = args[0]
    Datum.integer(container.count())
}

internal val Fn_CARDINALITY__LIST__INT32 = Function.overload(

    name = "cardinality",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.array()),
    ),

) { args ->
    val container = args[0]
    Datum.integer(container.count())
}

internal val Fn_CARDINALITY__STRUCT__INT32 = Function.overload(

    name = "cardinality",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.struct()),
    ),

) { args ->
    val container = args[0]
    var count = 0
    val iter = container.fields.iterator()
    while (iter.hasNext()) {
        iter.next()
        count++
    }
    Datum.integer(count)
}
