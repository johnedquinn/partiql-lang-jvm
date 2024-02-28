package org.partiql.examples.cache

import org.junit.jupiter.api.Test
import org.partiql.ast.Statement
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.int32Value
import org.partiql.value.structValue

class MainTest {
    companion object {
        private const val USER_ID = "EXAMPLE_USER"

        private const val CUSTOM_CATALOG_NAME = "MY_CUSTOM_CATALOG"

        private val DESIRED_EVALUATION_MODE = PartiQLEngine.Mode.PERMISSIVE

        @JvmStatic
        val parser = PartiQLParser.default()

        @JvmStatic
        private val planner = PartiQLPlannerBuilder().build()

        @JvmStatic
        private val engine = PartiQLEngine.builder().build()
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun testCache() {
        TestCase(
            query = """
                SELECT a + b AS result
                FROM t
            """.trimIndent(),
            expectations = listOf(
                TestCase.Expectation(
                    bindings = mapOf(
                        "t" to bagValue(
                            structValue(
                                "a" to int32Value(5),
                                "b" to int32Value(40),
                            )
                        )
                    ),
                    expectedResult = bagValue(
                        structValue(
                            "result" to int32Value(45)
                        )
                    )
                ),
                TestCase.Expectation(
                    bindings = mapOf(
                        "t" to bagValue(
                            structValue(
                                "a" to int32Value(27),
                                "b" to int32Value(3),
                            )
                        )
                    ),
                    expectedResult = bagValue(
                        structValue(
                            "result" to int32Value(30)
                        )
                    )
                ),
                TestCase.Expectation(
                    bindings = mapOf(
                        "t" to bagValue(
                            structValue(
                                "a" to int32Value(50),
                                "b" to int32Value(60),
                            )
                        )
                    ),
                    expectedResult = bagValue(
                        structValue(
                            "result" to int32Value(110)
                        )
                    )
                ),
            )
        ).assert()
    }

    /**
     * This [TestCase] aims to show how you can cache a plan (and even a prepared executable) and swap
     * out the globals/bindings prior to executing the prepared statement.
     *
     * So, the idea behind this is that, we are going to compile the [query] and we are going
     * to cache the prepared statement. Before planning, we are going to tell the planner that
     * the [expectations] bindings exist (though we won't actually instantiate their values -- we'll save
     * that for runtime). See how we instantiate [bindingNames] and how it is used.
     *
     * Then, we're going to loop through the [expectations]. For each expectation,
     * we are going to take the input bindings and give them to the [MutableConnector.Bindings]. Then, we'll
     * execute the query and materialize the data to make sure we get our expected result.
     */
    @OptIn(PartiQLValueExperimental::class)
    class TestCase(
        private val query: String,
        private val expectations: List<Expectation>
    ) {

        private val preparedStatement: PartiQLStatement<*>
        private val plan: PartiQLPlan
        private val bindings: MutableConnector.Bindings

        private val bindingNames = expectations.flatMap { expectation ->
            expectation.bindings.keys
        }.toSet()

        class Expectation(
            val bindings: Map<String, PartiQLValue>,
            val expectedResult: PartiQLValue
        )

        init {
            val connector = MutableConnector()
            this.bindings = connector.bindings
            val connectorSession: ConnectorSession = object : ConnectorSession {
                override fun getQueryId(): String = query.hashCode().toString()
                override fun getUserId(): String = USER_ID
            }
            val session: PartiQLPlanner.Session = PartiQLPlanner.Session(
                query.hashCode().toString(),
                USER_ID,
                CUSTOM_CATALOG_NAME,
                catalogs = mapOf(CUSTOM_CATALOG_NAME to connector.getMetadata(connectorSession))
            )
            val statement: Statement = parser.parse(query).root

            // Before planning, we need to add the globals to the Connector. Note, that we don't
            // even need to add their values. The planner just needs to know that, at execution time, the
            // values will exist with the type stated. We will use StaticType.ANY because we don't have any viable
            // information. If we did have more type information, we could add it here, and it would likely speed up our
            // execution, because the planner can perform some optimizations.
            this.bindingNames.forEach { bindingName ->
                this.bindings.define(bindingName, type = StaticType.ANY, value = null)
            }
            val plannerResult: PartiQLPlanner.Result = planner.plan(statement, session)
            val plan: PartiQLPlan = plannerResult.plan
            this.plan = plan
            val prepared: PartiQLStatement<*> = engine.prepare(
                plan = plan,
                session = PartiQLEngine.Session(
                    mapOf(CUSTOM_CATALOG_NAME to connector),
                    mode = DESIRED_EVALUATION_MODE
                )
            )
            this.preparedStatement = prepared
        }

        @OptIn(PartiQLValueExperimental::class)
        fun assert() {
            this.expectations.forEach { expectation ->
                this.bindings.clear()
                // We're going to update the values behind the ValueConnector so that they
                // can be updated per-testcase. With this, we don't need to re-plan, because the
                // names are the same.
                expectation.bindings.forEach { (name, value) ->
                    this.bindings.define(name, StaticType.ANY, value)
                }

                // Now, let's execute and materialize the query!
                val executionResult: PartiQLResult.Value = when (val returned = engine.execute(this.preparedStatement)) {
                    is PartiQLResult.Value -> returned
                    is PartiQLResult.Error -> {
                        PlanPrinter.append(System.err, this.plan)
                        throw returned.cause
                    }
                }
                val result = executionResult.value

                // Note that SELECTs are lazily materialized due to their potential for many elements. The
                // assertion below will materialize the data if the query is a SFW (SELECT-FROM-WHERE) query.
                assert(result == expectation.expectedResult) {
                    buildString {
                        appendLine("Expected : ${expectation.expectedResult}")
                        appendLine("Actual   : $result")
                    }
                }
            }
        }
    }
}
