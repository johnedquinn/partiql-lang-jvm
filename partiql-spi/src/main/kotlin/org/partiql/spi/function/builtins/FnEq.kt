// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.function.Function

/**
 * According to SQL:1999:
 * > If either XV or YV is the null value, then `X <comp op> Y` is unknown
 *
 * According to the PartiQL Specification:
 * > Equality never fails in the type-checking mode and never returns MISSING in the permissive mode. Instead, it can
 * compare values of any two types, according to the rules of the PartiQL type system. For example, 5 = 'a' is false.
 *
 * For the existing conformance tests, whenever an operand is NULL or MISSING, the output is NULL. This implementation
 * follows this.
 *
 * TODO: The PartiQL Specification needs to clearly define the semantics of MISSING. That being said, this implementation
 *  follows the existing conformance tests and SQL:1999.
 */
private val name = FunctionUtils.hide("eq")
internal val FnEq = FnOverload.Builder(name)
    .addParameters(PType.dynamic(), PType.dynamic())
    .returns(PType.bool())
    .isNullCall(true)
    .isMissingCall(false)
    .body(Impl)
    .build()

private object Impl : Function<Array<Datum>, Datum> {
    private val comparator = Datum.comparator()
    override fun apply(t: Array<Datum>): Datum {
        val lhs = t[0]
        val rhs = t[1]
        return if (lhs.isMissing || rhs.isMissing) {
            Datum.nullValue(PType.bool())
        } else {
            Datum.bool(comparator.compare(lhs, rhs) == 0)
        }
    }
}
