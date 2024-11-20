@file:JvmName("Main")

package org.partiql.iondb

import org.partiql.ast.Statement
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Operation
import org.partiql.plan.Operator
import org.partiql.plan.Plan
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.rex.Rex
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Connector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import java.io.PrintStream

/**
 * Entry-point to the PartiQL command-line utility.
 * Base:
 * ./gradlew :examples:iondb:run --args="'XXX'"
 *
 * Queries:
 * 1. Show how the registered error listener works: SELECT t.name, foo(1) FROM bdt.teams AS t;
 * 2. Getting data from a top-level table: SELECT VALUE p FROM persons p;
 * 3. Getting data from a nested table: SELECT * FROM bdt.teams AS t;
 * 4. Pathing on data: SELECT * FROM bdt.teams AS t;
 * 5. Constant fold example: 1 + 2;
 * 6. Show the anti-filter: SELECT VALUE p.age FROM persons p WHERE p.age > 30
 * 7. Show how warnings get printed: SELECT VALUE 1 + MISSING FROM persons p WHERE p.age > 30
 * 8. Show how the ast rewriter gets invoked: SELECT AVG(p.age) AS avg_age FROM persons p
 */
fun main(args: Array<String>) {

    // Create Shared IonDB components
    val session: Session = Session.builder()
        .catalogs(IonCatalog("iondb")) // Register catalogs
        .catalog("iondb") // Set the current catalog.
        .build()
    val sharedContext = Context.of(IonErrorListener) // Register the custom error listener

    // Get user input
    assert(args.size == 1) { "Expected exactly one argument, but got ${args.size}" }
    val input: String = args[0]

    // Parse
    val parser: PartiQLParser = PartiQLParser.standard()
    val parseResult: PartiQLParser.Result = parser.parse(input, sharedContext)
    val statements: List<Statement> = parseResult.statements
    assert(statements.size == 1) { "Expected exactly one statement, but got {${statements.size}}" }
    var statement: Statement = parseResult.statements[0]

    // Perform custom rewrite of the AST
    statement = AggregationSimplification.visitStatement(statement, Unit) as Statement

    // Plan
    val planner: PartiQLPlanner = PartiQLPlanner.standard()
    val planResult = planner.plan(statement, session, sharedContext)
    var plan: Plan = planResult.plan

    // Perform a constant fold (rewrite of the plan for replacing nodes)
    plan = ConstantFoldPlanRewriter.rewrite(plan)

    // Create compiler
    // Add a "strategy" to convert plan RelFilters to implementations that do the opposite (an Anti Filter)
    val strategy = AntiFilterStrategy
    val compiler: PartiQLCompiler = PartiQLCompiler.builder().addStrategy(strategy).build()

    // Compile
    val mode: Mode = Mode.STRICT() // TODO: Be able to pass this in to the application by the user.
    val compiledResult = compiler.prepare(plan, mode, sharedContext)

    // Execute
    val lazyData = compiledResult.execute()

    // Evaluate and Print
    DatumTextWriter(PrintStream(System.out)).append(lazyData).close()
}
