package org.partiql.runner.test

import org.opentest4j.TestAbortedException
import org.partiql.eval.Statement
import org.partiql.runner.CompileType
import org.partiql.runner.schema.Assertion
import org.partiql.runner.schema.TestCase

/**
 * TestRunner delegates execution to the underlying TestExecutor, but orchestrates test assertions.
 */
class TestRunner(private val factory: TestExecutor.Factory<Statement, Statement>) {

    fun test(case: TestCase.Eval, skipList: Set<Pair<String, CompileType>>) {
        runSkipped(skipList.contains(Pair(case.name, case.compileOptions)), TestEval(factory, case, skipList))
    }

    private class TestEval(
        private val factory: TestExecutor.Factory<Statement, Statement>,
        private val case: TestCase.Eval,
        private val skipList: Set<Pair<String, CompileType>>
    ): Runnable {
        override fun run() {
            val executor = factory.create(case.env, case.compileOptions)
            val input = case.statement
            run(input, case, executor)
        }

        private fun run(input: String, case: TestCase, executor: TestExecutor<Statement, Statement>) {
            when (val assertion = case.assertion) {
                is Assertion.EvaluationSuccess -> {
                    val statement = executor.prepare(input)
                    val actual = executor.execute(statement)
                    val expect = executor.fromIon(assertion.expectedResult)
                    if (!executor.compare(actual, expect)) {
                        val ion = executor.toIon(actual)
                        val message = buildString {
                            appendLine("*** EXPECTED != ACTUAL ***")
                            appendLine("Mode     : ${case.compileOptions}")
                            appendLine("Expected : ${assertion.expectedResult}")
                            appendLine("Actual   : $ion")
                        }
                        error(message)
                    }
                    actual.close()
                    expect.close()
                    statement.close()
                }
                is Assertion.EvaluationFailure -> {
                    var thrown: Throwable? = null
                    val ion = try {
                        val statement = executor.prepare(input)
                        val actual = executor.execute(statement)
                        executor.toIon(actual)
                    } catch (t: Throwable) {
                        thrown = t
                    }
                    if (thrown == null) {
                        val message = buildString {
                            appendLine("Expected error to be thrown but none was thrown.")
                            appendLine("Actual Result: $ion")
                        }
                        error(message)
                    }
                }
            }
        }
    }

    fun test(case: TestCase.Equiv, skipList: Set<Pair<String, CompileType>>) {
        runSkipped(skipList.contains(Pair(case.name, case.compileOptions)), TestEquiv(factory, case))
    }

    private class TestEquiv(
        private val factory: TestExecutor.Factory<Statement, Statement>,
        private val case: TestCase.Equiv,
    ): Runnable {
        override fun run() {
            val executor = factory.create(case.env, case.compileOptions)
            case.statements.forEach { run(it, case, executor) }
        }

        private fun run(input: String, case: TestCase, executor: TestExecutor<Statement, Statement>) {
            when (val assertion = case.assertion) {
                is Assertion.EvaluationSuccess -> {
                    val statement = executor.prepare(input)
                    val actual = executor.execute(statement)
                    val expect = executor.fromIon(assertion.expectedResult)
                    if (!executor.compare(actual, expect)) {
                        val ion = executor.toIon(actual)
                        val message = buildString {
                            appendLine("*** EXPECTED != ACTUAL ***")
                            appendLine("Mode     : ${case.compileOptions}")
                            appendLine("Expected : ${assertion.expectedResult}")
                            appendLine("Actual   : $ion")
                        }
                        error(message)
                    }
                    actual.close()
                    expect.close()
                    statement.close()
                }
                is Assertion.EvaluationFailure -> {
                    var thrown: Throwable? = null
                    val ion = try {
                        val statement = executor.prepare(input)
                        val actual = executor.execute(statement)
                        executor.toIon(actual)
                    } catch (t: Throwable) {
                        thrown = t
                    }
                    if (thrown == null) {
                        val message = buildString {
                            appendLine("Expected error to be thrown but none was thrown.")
                            appendLine("Actual Result: $ion")
                        }
                        error(message)
                    }
                }
            }
        }
    }

    /**
     * If [markedAsSkip], run the [testExecution] and make sure it fails. If it fails, ignore the test by throwing a
     * [TestAbortedException]. If it succeeds, mark the test as failed. This will enforce updating the skip-list.
     */
    private fun runSkipped(markedAsSkip: Boolean, testExecution: Runnable) {
        when (markedAsSkip) {
            false -> testExecution.run()
            true -> {
                var isError = false
                try {
                    testExecution.run()
                } catch (e: Throwable) {
                    isError = true
                }
                when (isError) {
                    true -> throw TestAbortedException()
                    false -> error("Test marked skipped doesn't fail. Please update the skip-list.")
                }
            }
        }
    }
}
