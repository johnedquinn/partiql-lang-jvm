package org.partiql.cli.pipeline

import org.partiql.ast.v1.Statement
import org.partiql.cli.ErrorCodeString
import org.partiql.cli.optimization.ProjectionPushdown
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PErrorListenerException
import org.partiql.spi.value.Datum
import java.io.PrintStream

internal class Pipeline private constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val compiler: PartiQLCompiler,
    private val ctx: Context,
    private val mode: Mode,
    private val optimize: Boolean,
) {

    /**
     * TODO replace with the ResultSet equivalent?
     * @throws PipelineException when there are accumulated errors, or if the components have thrown an [PErrorListenerException].
     */
    @Throws(PipelineException::class)
    fun execute(statement: String, session: Session): Datum {
        val ast = parse(statement)
        val plan = plan(ast, session)
        println("Plan:")
        printPlan(plan)
        val optimizedPlan = try {
            when (optimize) {
                true -> {
                    println("Optimized Plan:")
                    val optimized = optimize(plan)
                    printPlan(optimized)
                    optimized
                }
                false -> plan
            }
        } catch (t: Throwable) {
            println("COULD NOT OPTIMIZE PLAN")
            t.printStackTrace()
            plan
        }
        return execute(optimizedPlan, session)
    }

    private fun parse(source: String): Statement {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            parser.parse(source, ctx)
        }
        if (result.statements.size != 1) {
            throw PipelineException("Expected exactly one statement, got: ${result.statements.size}")
        }
        return result.statements[0]
    }

    private fun plan(statement: Statement, session: Session): Plan {
        val result = listen(ctx.errorListener as AppPErrorListener) {
            planner.plan(statement, session, ctx)
        }
        return result.plan
    }

    private fun optimize(plan: Plan): Plan {
        return ProjectionPushdown().accept(plan)
    }

    private fun execute(plan: Plan, session: Session): Datum {
        val statement = listen(ctx.errorListener as AppPErrorListener) {
            compiler.prepare(plan, mode, ctx)
        }
        return statement.execute()
    }

    private fun <T> listen(listener: AppPErrorListener, action: () -> T): T {
        listener.clear()
        val result = try {
            action.invoke()
        } catch (e: PipelineException) {
            throw e
        }
        if (listener.hasErrors()) {
            throw PipelineException("Failed with given input. Please see the above errors.")
        }
        return result
    }

    companion object {

        fun default(out: PrintStream, config: Config): Pipeline {
            return create(Mode.PERMISSIVE(), out, config)
        }

        fun strict(out: PrintStream, config: Config): Pipeline {
            return create(Mode.STRICT(), out, config)
        }

        private fun create(mode: Mode, out: PrintStream, config: Config): Pipeline {
            val listener = config.getErrorListener(out)
            val ctx = Context.of(listener)
            val parser = PartiQLParser.Builder().build()
            val planner = PartiQLPlanner.builder().build()
            val compiler = PartiQLCompiler.builder().build()
            return Pipeline(parser, planner, compiler, ctx, mode, config.optimize)
        }

        private fun printPlan(plan: PlanNode) {
            val planDetails = plan.debugString()
            val sb = StringBuilder()
            sb.printPlan(planDetails, 0)
            val str = sb.toString()
            println(str)
        }

        private fun StringBuilder.printPlan(map: Map<*, *>, indent: Int) {
            appendLine("{")
            map.entries.forEach { (k, v) ->
                this.indent(indent + 1)
                this.append("\"$k\": ")
                when (v) {
                    is String -> this.appendLine("\"$v\"")
                    is Map<*, *> -> {
                        this.printPlan(v, indent + 1)
                    }
                    is List<*> -> {
                        this.printPlan(v, indent + 1)
                    }
                    else -> this.appendLine("$v")
                }
            }
            indent(indent)
            appendLine("}")
        }

        private fun StringBuilder.printPlan(list: List<*>, indent: Int) {
            appendLine("[")
            list.forEach { elt ->
                indent(indent + 1)
                when (elt) {
                    is String -> this.append("\"$elt\"")
                    is Map<*, *> -> {
                        this.printPlan(elt, indent + 1)
                    }
                    is List<*> -> {
                        this.printPlan(elt, indent + 1)
                    }
                    else -> this.appendLine("$elt")
                }
            }
            indent(indent)
            appendLine("]")
        }

        private fun StringBuilder.indent(int: Int) {
            for (i in 0 until int) {
                this.append(" ")
            }
        }
    }

    /**
     * Halts execution.
     */
    class PipelineException(override val message: String?) : PErrorListenerException(message)

    /**
     * Configuration for passing through user-defined configurations to the underlying components.
     */
    class Config(
        private val maxErrors: Int,
        private val inhibitWarnings: Boolean,
        private val warningsAsErrors: Array<ErrorCodeString>,
        val optimize: Boolean,
    ) {
        fun getErrorListener(out: PrintStream): AppPErrorListener {
            return AppPErrorListener(out, maxErrors, inhibitWarnings, warningsAsErrors)
        }
    }
}
