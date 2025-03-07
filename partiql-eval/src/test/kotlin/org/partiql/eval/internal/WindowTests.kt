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

        private class Employee(val id: Int, val name: String, val department: String, val age: Int) {
            fun toDatum(): Datum {
                return Datum.struct(
                    Field.of("id", Datum.integer(id)),
                    Field.of("name", Datum.string(name)),
                    Field.of("department", Datum.string(department)),
                    Field.of("age", Datum.integer(age))
                )
            }
        }

        private val employees = listOf(
            Employee(0, "Jacob", "Marketing", 40),
            Employee(1, "Marcus", "Sales", 28),
            Employee(2, "Shelly", "Research", 35),
            Employee(3, "Alexa", "Research", 30),
            Employee(4, "Raghavan", "Sales", 28),
            Employee(5, "Yi", "Research", 25),
            Employee(6, "Megan", "Marketing", 32),
            Employee(7, "Amanda", "Research", 32),
            Employee(8, "Samantha", "Sales", 29),
            Employee(9, "Mason", "Research", 30)
        )

        private val globals = listOf(
            Global(
                name = "employee",
                value = Datum.bag(employees.map { it.toDatum() })
            )
        )

        private const val FALLBACK: String = "UNKNOWN"

        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Simple SFW",
                mode = Mode.PERMISSIVE(),
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.id) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.id) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.id) as _row_number,
                        LAG(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.id) AS _lag,
                        LEAD(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.id) AS _lead
                    FROM employee AS t
                    LIMIT 2;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(employees[0].id, employees[0].name, 1, 1, 1, FALLBACK, employees[6].name),
                    rowOf(employees[6].id, employees[6].name, 2, 2, 2, employees[0].name, FALLBACK),
                ),
                globals = globals
            ),
        )

        private fun rowOf(id: Int, name: String, rank: Long, denseRank: Long, rowNumber: Long, lag: String, lead: String): Datum {
            return Datum.struct(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(name)),
                Field.of("_rank", Datum.bigint(rank)),
                Field.of("_dense_rank", Datum.bigint(denseRank)),
                Field.of("_row_number", Datum.bigint(rowNumber)),
                Field.of("_lag", Datum.string(lag)),
                Field.of("_lead", Datum.string(lead))
            )
        }

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
