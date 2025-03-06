package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

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
                mode = Mode.STRICT(),
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        -- RANK() OVER (PARTITION BY t.partition_no ORDER BY t.id) AS _rank,
                        ROW_NUMBER() OVER (PARTITION BY t.partition_no ORDER BY t.id) as _row_number,
                        LAG(t.name, 1, 'UNKNOWN') OVER (PARTITION BY t.partition_no ORDER BY t.id) AS _lag,
                        LEAD(t.name, 1, 'UNKNOWN') OVER (PARTITION BY t.partition_no ORDER BY t.id) AS _lead
                    FROM <<
                        { 'id': 0, 'name': 'A', 'partition_no': 0 },
                        { 'id': 1, 'name': 'B', 'partition_no': 1 },
                        { 'id': 2, 'name': 'C', 'partition_no': 2 },
                        { 'id': 3, 'name': 'D', 'partition_no': 0 },
                        { 'id': 4, 'name': 'E', 'partition_no': 1 },
                        { 'id': 5, 'name': 'F', 'partition_no': 2 }
                    >> t
                    LIMIT 2;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("_id", Datum.integer(0)),
                        Field.of("_name", Datum.string("A")),
                        // Field.of("_rank", Datum.bigint(1L)),
                        Field.of("_row_number", Datum.bigint(1L)),
                        Field.of("_lag", Datum.string("UNKNOWN")),
                        Field.of("_lead", Datum.string("D"))
                    ),
                    Datum.struct(
                        Field.of("_id", Datum.integer(3)),
                        Field.of("_name", Datum.string("D")),
                        // Field.of("_rank", Datum.bigint(2L)),
                        Field.of("_row_number", Datum.bigint(2L)),
                        Field.of("_lag", Datum.string("A")),
                        Field.of("_lead", Datum.string("B"))
                    ),
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
