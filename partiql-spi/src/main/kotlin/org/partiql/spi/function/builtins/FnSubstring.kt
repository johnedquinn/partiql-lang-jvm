// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.function.utils.StringUtils.codepointSubstring
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.function.Function

/**
 * Built-in function to the substring of an existing string. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * From the SQL-92 spec, page 135:
 * ```
 * 1) If <character substring function> is specified, then:
 *      a) Let C be the value of the <character value expression>,
 *      let LC be the length of C, and
 *      let S be the value of the <start position>.
 *
 *      b) If <string length> is specified, then:
 *      let L be the value of <string length> and
 *      let E be S+L.
 *      Otherwise:
 *          let E be the larger of LC + 1 and S.
 *
 *      c) If either C, S, or L is the null value, then the result of
 *      the <character substring function> is the null value.
 *
 *      d) If E is less than S, then an exception condition is raised:
 *      data exception-substring error.
 *
 *      e) Case:
 *          i) If S is greater than LC or if E is less than 1, then the
 *          result of the <character substring function> is a zero-
 *          length string.
 *
 *          ii) Otherwise,
 *              1) Let S1 be the larger of S and 1. Let E1 be the smaller
 *              of E and LC+1. Let L1 be E1-S1.
 *
 *              2) The result of the <character substring function> is
 *              a character string containing the L1 characters of C
 *              starting at character number S1 in the same order that
 *              the characters appear in C.
 *
 * Pseudocode:
 *      func substring():
 *          # Section 1-a
 *          str = <string to be sliced>
 *          strLength = LENGTH(str)
 *          startPos = <start position>
 *
 *          # Section 1-b
 *          sliceLength = <length of slice, optional>
 *          if sliceLength is specified:
 *              endPos = startPos + sliceLength
 *          else:
 *              endPos = greater_of(strLength + 1, startPos)
 *
 *          # Section 1-c:
 *          if str, startPos, or (sliceLength is specified and is null):
 *              null
 *
 *          # Section 1-d
 *          if endPos < startPos:
 *              throw exception
 *
 *          # Section 1-e-i
 *          if startPos > strLength or endPos < 1:
 *              ''
 *          else:
 *              # Section 1-e-ii
 *              S1 = greater_of(startPos, 1)
 *              E1 = lesser_of(endPos, strLength + 1)
 *              L1 = E1 - S1
 *              java's substring(C, S1, E1)
 */
internal val Fn_SUBSTRING__STRING_INT32__STRING = FnOverload.Builder("substring")
    .returns(PType.string())
    .addParameters(PType.string(), PType.integer())
    .body(FnSubstringStringIntImpl)
    .build()

private object FnSubstringStringIntImpl : Function<Array<Datum>, Datum> {
    override fun apply(t: Array<Datum>): Datum {
        val value = t[0].string
        val start = t[1].int
        val result = value.codepointSubstring(start)
        return Datum.string(result)
    }
}

internal val Fn_SUBSTRING__CLOB_INT64__CLOB = FnOverload.Builder("substring")
    .returns(PType.clob())
    .addParameters(PType.clob(), PType.integer())
    .body(FnSubstringClobIntImpl)
    .build()

private object FnSubstringClobIntImpl : Function<Array<Datum>, Datum> {
    override fun apply(t: Array<Datum>): Datum {
        val value = t[0].bytes.toString(Charsets.UTF_8)
        val start = t[1].int
        val result = value.codepointSubstring(start)
        return Datum.clob(result.toByteArray())
    }
}

internal val Fn_SUBSTRING__STRING_INT32_INT32__STRING = FnOverload.Builder("substring")
    .returns(PType.string())
    .addParameters(PType.string(), PType.integer(), PType.integer())
    .body(FnSubstringStringIntIntImpl)
    .build()

private object FnSubstringStringIntIntImpl : Function<Array<Datum>, Datum> {
    override fun apply(t: Array<Datum>): Datum {
        val value = t[0].string
        val start = t[1].int
        val end = t[2].int
        if (end < 0) {
            throw PErrors.internalErrorException(IllegalArgumentException("End must be non-negative."))
        }
        val result = value.codepointSubstring(start, end)
        return Datum.string(result)
    }
}

internal val Fn_SUBSTRING__CLOB_INT64_INT64__CLOB = FnOverload.Builder("substring")
    .returns(PType.clob())
    .addParameters(PType.clob(), PType.integer(), PType.integer())
    .body(FnSubstringClobIntIntImpl)
    .build()

private object FnSubstringClobIntIntImpl : Function<Array<Datum>, Datum> {
    override fun apply(t: Array<Datum>): Datum {
        val value = t[0].bytes.toString(Charsets.UTF_8)
        val start = t[1].int
        val end = t[2].int
        if (end < 0) {
            throw PErrors.internalErrorException(IllegalArgumentException("End must be non-negative."))
        }
        val result = value.codepointSubstring(start, end)
        return Datum.clob(result.toByteArray())
    }
}
