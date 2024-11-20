package org.partiql.iondb

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

object FunctionRandom : Function {
    override fun getName(): String {
        return "random"
    }

    val random = java.util.Random()

    override fun getParameters(): Array<Parameter> {
        return emptyArray()
    }

    override fun getReturnType(args: Array<PType>): PType {
        return PType.integer()
    }

    override fun getInstance(args: Array<PType>): Function.Instance {
        val r = random
        // The red IntelliJ errors are coming from being in the same gradle project
        return object : Function.Instance("random", emptyArray(), PType.integer()) {
            override fun invoke(args: Array<Datum>): Datum {
                val result = r.nextInt()
                return Datum.integer(result)
            }
        }
    }
}
