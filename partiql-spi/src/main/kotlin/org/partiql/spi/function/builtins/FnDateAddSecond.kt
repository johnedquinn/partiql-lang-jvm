// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_DATE_ADD_SECOND__INT32_TIME__TIME = Function.static(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].time
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.time(datetimeValue.plusSeconds(intervalValue))
}

internal val Fn_DATE_ADD_SECOND__INT64_TIME__TIME = Function.static(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].time
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.time(datetimeValue.plusSeconds(intervalValue))
}

internal val Fn_DATE_ADD_SECOND__INT_TIME__TIME = Function.static(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", DefaultNumeric.NUMERIC),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].bigInteger
    val datetime = args[1].time
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.time(datetimeValue.plusSeconds(intervalValue))
}

internal val Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_second",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
}

internal val Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_second",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
}

internal val Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_second",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", DefaultNumeric.NUMERIC),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].bigInteger
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue))
}
