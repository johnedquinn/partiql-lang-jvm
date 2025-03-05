package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum

/**
 * This test file tests Common Table Expressions.
 */
class WindowTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("failureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun failureTests(tc: FailureTestCase) = tc.run()

    companion object {
        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Simple SFW",
                input = """
                    SELECT
                        RANK() OVER () AS _rank,
                        ROW_NUMBER() OVER () as _row_number
                    FROM <<
                        'hello',
                        'goodbye'
                    >> t
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
        )

        @JvmStatic
        fun failureTestCases() = listOf(
            FailureTestCase(
                name = "CTE with cardinality greater than 1 used in subquery",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM <<1, 2>> AS t
                    )
                    SELECT VALUE y + (SELECT * FROM x) FROM <<100>> AS y;
                """.trimIndent(),
            ),
        )
    }
}
