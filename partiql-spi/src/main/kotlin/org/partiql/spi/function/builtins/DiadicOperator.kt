package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.concurrent.Callable
import java.util.function.BiFunction

/**
 * This represents an operator backed by a function overload. Note that the name of the operator is hidden
 * using [FunctionUtils.hide].
 *
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 *
 * Implementations of this should invoke [fillTable] in the constructor of the function.
 * @param hidesName dictates whether the [name] should be hidden; true by default.
 */
internal abstract class DiadicOperator(
    name: String,
    private val lhs: PType,
    private val rhs: PType,
    hidesName: Boolean = true
) : FnOverload() {

    private val name = when (hidesName) {
        true -> FunctionUtils.hide(name)
        false -> name
    }

    companion object {
        private val DEC_TINY_INT = PType.decimal(3, 0)
        private val NUM_TINY_INT = PType.numeric(3, 0)
        private val DEC_SMALL_INT = PType.decimal(5, 0)
        private val NUM_SMALL_INT = PType.numeric(5, 0)
        private val DEC_INT = PType.decimal(10, 0)
        private val NUM_INT = PType.numeric(10, 0)
        private val DEC_BIG_INT = PType.decimal(19, 0)
        private val NUM_BIG_INT = PType.numeric(19, 0)
    }

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(name, listOf(lhs, rhs))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val lhs = args[0]
        val rhs = args[1]
        val (newLhs, newRhs) = getOperands(lhs, rhs) ?: return null
        val instance = instances[lhs.code()][rhs.code()]
        return instance.apply(newLhs, newRhs)
    }

    private fun getOperands(lhs: PType, rhs: PType): Pair<PType, PType>? {
        val lhsPrecedence = TYPE_PRECEDENCE[lhs.code()] ?: return null
        val rhsPrecedence = TYPE_PRECEDENCE[rhs.code()] ?: return null
        val (newLhs, newRhs) = when (lhsPrecedence.compareTo(rhsPrecedence)) {
            -1 -> (rhs to rhs)
            0 -> (lhs to rhs)
            else -> (lhs to lhs)
        }
        return newLhs to newRhs
    }

    /**
     * @param booleanLhs TODO
     * @param booleanRhs TODO
     * @return TODO
     */
    open fun getBooleanInstance(booleanLhs: PType, booleanRhs: PType): Fn? {
        return null
    }

    /**
     * @param stringLhs TODO
     * @param stringRhs TODO
     * @return TODO
     */
    open fun getStringInstance(stringLhs: PType, stringRhs: PType): Fn? {
        return null
    }

    /**
     * @param charLhs TODO
     * @param charRhs TODO
     * @return TODO
     */
    open fun getCharInstance(charLhs: PType, charRhs: PType): Fn? {
        return null
    }

    /**
     * @param varcharLhs TODO
     * @param varcharRhs TODO
     * @return TODO
     */
    open fun getVarcharInstance(varcharLhs: PType, varcharRhs: PType): Fn? {
        return null
    }

    /**
     * @param blobLhs TODO
     * @param blobRhs TODO
     * @return TODO
     */
    open fun getBlobInstance(blobLhs: PType, blobRhs: PType): Fn? {
        return null
    }

    /**
     * @param clobLhs TODO
     * @param clobRhs TODO
     * @return TODO
     */
    open fun getClobInstance(clobLhs: PType, clobRhs: PType): Fn? {
        return null
    }

    /**
     * @param dateLhs TODO
     * @param dateRhs TODO
     * @return TODO
     */
    open fun getDateInstance(dateLhs: PType, dateRhs: PType): Fn? {
        return null
    }

    /**
     * @param timeLhs TODO
     * @param timeRhs TODO
     * @return TODO
     */
    open fun getTimeInstance(timeLhs: PType, timeRhs: PType): Fn? {
        return null
    }

    /**
     * @param timestampLhs TODO
     * @param timestampRhs TODO
     * @return TODO
     */
    open fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Fn? {
        return null
    }

    /**
     * @param integerLhs TODO
     * @param integerRhs TODO
     * @return TODO
     */
    open fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Fn? {
        return null
    }

    /**
     * @param tinyIntLhs TODO
     * @param tinyIntRhs TODO
     * @return TODO
     */
    open fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param smallIntLhs TODO
     * @param smallIntRhs TODO
     * @return TODO
     */
    open fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param bigIntLhs TODO
     * @param bigIntRhs TODO
     * @return TODO
     */
    open fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param numericLhs TODO
     * @param numericRhs TODO
     * @return TODO
     */
    open fun getNumericInstance(numericLhs: PType, numericRhs: PType): Fn? {
        return null
    }

    /**
     * @param decimalLhs TODO
     * @param decimalRhs TODO
     * @return TODO
     */
    open fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn? {
        return null
    }

    /**
     * @param realLhs TODO
     * @param realRhs TODO
     * @return TODO
     */
    open fun getRealInstance(realLhs: PType, realRhs: PType): Fn? {
        return null
    }

    /**
     * @param doubleLhs TODO
     * @param doubleRhs TODO
     * @return TODO
     */
    open fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Fn? {
        return null
    }

    /**
     * This is used when all operands are NULL/MISSING.
     * @return an instance of a function
     */
    open fun getUnknownInstance(): Fn? {
        return null
    }

    /**
     * This is a lookup table for finding the appropriate instance for the given types. The table is
     * initialized on construction using the get*Instance methods.
     */
    private val instances: Array<Array<BiFunction<PType, PType, Fn?>>> = Array(PType.codes().size) {
        Array(PType.codes().size) {
            EmptyBiFunc
        }
    }

    private object EmptyBiFunc : BiFunction<PType, PType, Fn?> {
        override fun apply(lhs: PType, rhs: PType): Fn? {
            return null
        }
    }

    protected fun fillTable(lhs: Int, rhs: Int, instance: BiFunction<PType, PType, Fn?>) {
        instances[lhs][rhs] = instance
    }

    private fun fillNumberTable(highPrecedence: Int, instance: (PType, PType) -> Fn?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.NUMBER, instance)
    }

    private fun fillCharacterStringTable(highPrecedence: Int, instance: (PType, PType) -> Fn?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.TEXT, instance)
    }

    private fun fillPrioritizedTable(highPrecedence: Int, family: SqlTypeFamily, instance: (PType, PType) -> Fn?) {
        val members = family.members + setOf(PType.UNKNOWN)
        members.filter {
            (TYPE_PRECEDENCE[highPrecedence]!! > TYPE_PRECEDENCE[it]!!)
        }.forEach {
            fillTable(highPrecedence, it, LeftOnly(instance))
            fillTable(it, highPrecedence, RightOnly(instance))
        }
        fillTable(highPrecedence, highPrecedence, Exact(instance))
    }

    private fun fillBooleanTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.BOOL, PType.BOOL, Exact(instance))
        fillTable(PType.BOOL, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.UNKNOWN, PType.BOOL, RightOnly(instance))
    }

    private fun fillTimestampTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMPZ, Exact(instance))
        fillTable(PType.TIMESTAMP, PType.TIMESTAMP, Exact(instance))
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMP, Exact(instance))
        fillTable(PType.TIMESTAMP, PType.TIMESTAMPZ, Exact(instance))
        fillTable(PType.TIMESTAMP, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.TIMESTAMPZ, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.UNKNOWN, PType.TIMESTAMP, RightOnly(instance))
        fillTable(PType.UNKNOWN, PType.TIMESTAMPZ, RightOnly(instance))
    }

    private fun fillTimeTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.TIMEZ, PType.TIMEZ, Exact(instance))
        fillTable(PType.TIME, PType.TIME, Exact(instance))
        fillTable(PType.TIMEZ, PType.TIME, Exact(instance))
        fillTable(PType.TIME, PType.TIMEZ, Exact(instance))
        fillTable(PType.TIMEZ, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.TIME, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.UNKNOWN, PType.TIME, RightOnly(instance))
        fillTable(PType.UNKNOWN, PType.TIMEZ, RightOnly(instance))
    }

    private fun fillDateTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.DATE, PType.DATE, Exact(instance))
        fillTable(PType.DATE, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.UNKNOWN, PType.DATE, RightOnly(instance))
    }

    private fun fillBlobTable(instance: BiFunction<PType, PType, Fn?>) {
        fillTable(PType.BLOB, PType.BLOB, Exact(instance))
        fillTable(PType.BLOB, PType.UNKNOWN, LeftOnly(instance))
        fillTable(PType.UNKNOWN, PType.BLOB, RightOnly(instance))
    }

    private fun fillNumericTable() {
        // Tiny Int
        fillTable(PType.TINYINT, PType.NUMERIC, ReplaceLeft(NUM_TINY_INT, ::getNumericInstance))
        fillTable(PType.NUMERIC, PType.TINYINT, ReplaceRight(NUM_TINY_INT, ::getNumericInstance))

        // Small Int
        fillTable(PType.SMALLINT, PType.NUMERIC, ReplaceLeft(NUM_SMALL_INT, ::getNumericInstance))
        fillTable(PType.NUMERIC, PType.SMALLINT, ReplaceRight(NUM_SMALL_INT, ::getNumericInstance))

        // Integer
        fillTable(PType.INTEGER, PType.NUMERIC, ReplaceLeft(NUM_INT, ::getNumericInstance))
        fillTable(PType.NUMERIC, PType.INTEGER, ReplaceRight(NUM_INT, ::getNumericInstance))

        // Big Int
        fillTable(PType.BIGINT, PType.NUMERIC, ReplaceLeft(NUM_BIG_INT, ::getNumericInstance))
        fillTable(PType.NUMERIC, PType.BIGINT, ReplaceRight(NUM_BIG_INT, ::getNumericInstance))

        // Numeric
        fillTable(PType.NUMERIC, PType.NUMERIC, ::getNumericInstance)
        fillTable(PType.UNKNOWN, PType.NUMERIC, RightOnly(::getNumericInstance))
        fillTable(PType.NUMERIC, PType.UNKNOWN, LeftOnly(::getNumericInstance))
    }

    private class Exact(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(t, u)
        }
    }

    private class Switch(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(u, t)
        }
    }

    private class RightOnly(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(u, u)
        }
    }

    private class LeftOnly(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(t, t)
        }
    }

    private class ReplaceLeft(private val left: PType, private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(left, u)
        }
    }

    private class ReplaceRight(private val right: PType, private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(t, right)
        }
    }

    private fun fillDecimalTable() {
        // Tiny Int
        fillTable(PType.TINYINT, PType.DECIMAL, ReplaceLeft(DEC_TINY_INT, ::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.TINYINT, ReplaceRight(DEC_TINY_INT, ::getDecimalInstance))

        // Small Int
        fillTable(PType.SMALLINT, PType.DECIMAL, ReplaceLeft(DEC_SMALL_INT, ::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.SMALLINT, ReplaceRight(DEC_SMALL_INT, ::getDecimalInstance))

        // Integer
        fillTable(PType.INTEGER, PType.DECIMAL, ReplaceLeft(DEC_INT, ::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.INTEGER, ReplaceRight(DEC_INT, ::getDecimalInstance))

        // Big Int
        fillTable(PType.BIGINT, PType.DECIMAL, ReplaceLeft(DEC_BIG_INT, ::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.BIGINT, ReplaceRight(DEC_BIG_INT, ::getDecimalInstance))

        // Numeric
        fillTable(PType.NUMERIC, PType.DECIMAL, NumericDecimalBiFunction(::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.NUMERIC, DecimalNumericBiFunction(::getDecimalInstance))

        // Decimal
        fillTable(PType.DECIMAL, PType.DECIMAL, ::getDecimalInstance)
        fillTable(PType.UNKNOWN, PType.DECIMAL, RightOnly(::getDecimalInstance))
        fillTable(PType.DECIMAL, PType.UNKNOWN, LeftOnly(::getDecimalInstance))
    }

    private fun fillUnknownTable() {
        fillTable(PType.UNKNOWN, PType.UNKNOWN, NoArgBiFunction(::getUnknownInstance))
    }

    private class NumericDecimalBiFunction(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(PType.decimal(t.precision, t.scale), u)
        }
    }

    private class DecimalNumericBiFunction(private val impl: BiFunction<PType, PType, Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return impl.apply(t, PType.decimal(u.precision, u.scale))
        }
    }

    private class NoArgBiFunction(private val r: Callable<Fn?>) : BiFunction<PType, PType, Fn?> {
        override fun apply(t: PType, u: PType): Fn? {
            return r.call()
        }
    }

    protected fun fillTable() {
        fillBooleanTable(::getBooleanInstance)
        fillNumberTable(PType.TINYINT, ::getTinyIntInstance)
        fillNumberTable(PType.SMALLINT, ::getSmallIntInstance)
        fillNumberTable(PType.INTEGER, ::getIntegerInstance)
        fillNumberTable(PType.BIGINT, ::getBigIntInstance)
        fillDecimalTable()
        fillNumericTable()
        fillNumberTable(PType.REAL, ::getRealInstance)
        fillNumberTable(PType.DOUBLE, ::getDoubleInstance)
        fillTimeTable(::getTimeInstance)
        fillDateTable(::getDateInstance)
        fillBlobTable(::getBlobInstance)
        fillTimestampTable(::getTimestampInstance)
        fillCharacterStringTable(PType.STRING, ::getStringInstance)
        fillCharacterStringTable(PType.CHAR, ::getCharInstance)
        fillCharacterStringTable(PType.VARCHAR, ::getVarcharInstance)
        fillCharacterStringTable(PType.CLOB, ::getClobInstance)
        fillUnknownTable()
    }

    protected fun basic(returns: PType, lhs: PType, rhs: PType, invocation: java.util.function.Function<Array<Datum>, Datum>): Fn {
        return Function.instance(
            name = name,
            returns = returns,
            parameters = arrayOf(
                Parameter("lhs", lhs),
                Parameter("rhs", rhs),
            ),
            invoke = invocation
        )
    }

    protected fun basic(returns: PType, args: PType, invocation: java.util.function.Function<Array<Datum>, Datum>): Fn {
        return basic(returns, args, args, invocation)
    }

    protected fun basic(arg: PType, invocation: java.util.function.Function<Array<Datum>, Datum>): Fn {
        return basic(arg, arg, arg, invocation)
    }
}
