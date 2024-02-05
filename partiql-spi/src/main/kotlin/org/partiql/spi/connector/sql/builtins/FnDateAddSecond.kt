// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.check
import org.partiql.value.timeValue
import org.partiql.value.timestampValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT32_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusSeconds(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT64_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusSeconds(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT_TIME__TIME : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIME,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimeValue>()
        val datetimeValue = datetime.value!!
        // TODO: We need to consider overflow here
        val intervalValue = interval.long!!
        return timeValue(datetimeValue.plusSeconds(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT32),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int32Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusSeconds(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT64),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<Int64Value>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusSeconds(intervalValue))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "date_add_second",
        returns = TIMESTAMP,
        parameters = listOf(
            FnParameter("interval", INT),
            FnParameter("datetime", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val interval = args[0].check<IntValue>()
        val datetime = args[1].check<TimestampValue>()
        val datetimeValue = datetime.value!!
        // TODO: We need to consider overflow here
        val intervalValue = interval.long!!
        return timestampValue(datetimeValue.plusSeconds(intervalValue))
    }
}