package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action.Query
import org.partiql.planner.PartiQLPlanner
import org.partiql.runner.CompileType
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.ValueUtils
import org.partiql.value.io.DatumIonReaderBuilder
import org.partiql.value.toIon
import kotlin.test.assertEquals

/**
 * @property session
 * @property mode
 */
class EvalExecutor(
    private val session: Session,
    private val mode: Mode,
) : TestExecutor<Statement, Statement> {

    val compiler = PartiQLCompiler.standard()
    val parser = PartiQLParser.standard()
    val planner = PartiQLPlanner.standard()
    val comparator = Datum.comparator()

    override fun prepare(input: String): Statement {
        val listener = getErrorListener(mode)
        val ctx = Context.of(listener)
        val parseResult = parser.parse(input, ctx)
        assertEquals(1, parseResult.statements.size)
        val ast = parseResult.statements[0]
        val plan = planner.plan(ast, session, ctx).plan
        return compiler.prepare(plan, mode, ctx)
    }

    private fun getErrorListener(mode: Mode): PErrorListener {
        return when (mode.code()) {
            Mode.PERMISSIVE -> PErrorListener.abortOnError()
            Mode.STRICT -> object : PErrorListener {
                override fun report(error: PError) {
                    when (error.severity.code()) {
                        Severity.ERROR -> throw PRuntimeException(error)
                        Severity.WARNING -> warning(error)
                        else -> error("Unhandled severity.")
                    }
                }

                private fun warning(error: PError) {
                    when (error.code()) {
                        PError.PATH_KEY_NEVER_SUCCEEDS,
                        PError.PATH_INDEX_NEVER_SUCCEEDS,
                        PError.VAR_REF_NOT_FOUND,
                        PError.PATH_SYMBOL_NEVER_SUCCEEDS -> {
                            error.severity = Severity.ERROR()
                            report(error)
                        }

                        else -> {
                            // Do nothing
                        }
                    }
                }
            }
            else -> error("This mode is not handled.")
        }
    }

    override fun execute(input: Statement): Statement {
        return input
    }

    override fun fromIon(value: IonValue): Statement {
        return IonStatement(value)
    }

    private class IonStatement(private val value: IonValue): Statement {
        override fun close() {
        }

        override fun execute(): Datum {
            return DatumIonReaderBuilder.standard().build(value.toIonElement()).read()
        }
    }

    override fun toIon(value: Statement): IonValue {
        val d = value.execute()
        val partiql = ValueUtils.newPartiQLValue(d)
        value.close()
        return partiql.toIon().toIonValue(ION)
    }

    // TODO: Use DATUM
    override fun compare(actual: Statement, expect: Statement): Boolean {
        val actualD = actual.execute()
        val expectD = expect.execute()
        val v1 = DatumMaterialize.materialize(actualD)
        val v2 = DatumMaterialize.materialize(expectD)
        val result = valueComparison(v1, v2)
        actual.close()
        expect.close()
        return result
    }

    // Value comparison of PartiQL Value that utilized Ion Hashcode.
    // in here, null.bool is considered equivalent to null
    // missing is considered different from null
    // annotation::1 is considered different from 1
    // 1 of type INT is considered the same as 1 of type INT32
    // we should probably consider adding our own hashcode implementation
    private fun valueComparison(v1: Datum, v2: Datum): Boolean {
        // Additional check to put on annotation
        // we want to have
        // annotation::null.int == annotation::null.bool  <- True
        // annotation::null.int == other::null.int <- False
        // TODO: Annotations
        if (v1.isNull && v2.isNull) {
            return true
        }
        // TODO: this comparator is designed for order by
        //  One of the false result it might produce is that
        //  it treats MISSING and NULL equally.
        //  we should move to hash or equals in value class once
        //  we finished implementing those.
        if (comparator.compare(v1, v2) == 0) {
            return true
        }
        val pv1 = ValueUtils.newPartiQLValue(v1)
        val pv2 = ValueUtils.newPartiQLValue(v2)
        if (pv1.toIon().hashCode() == pv2.toIon().hashCode()) {
            return true
        }
        // Ion element hash code contains a bug
        // Hashcode of BigIntIntElementImpl(BigInteger.ONE) is not the same as that of LongIntElementImpl(1)
        if (pv1.toIon().type == ElementType.INT && pv2.toIon().type == ElementType.INT) {
            return pv1.toIon().asAnyElement().bigIntegerValue == pv2.toIon().asAnyElement().bigIntegerValue
        }
        return false
    }

    object Factory : TestExecutor.Factory<Statement, Statement> {

        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()

        override fun create(env: IonStruct, options: CompileType): TestExecutor<Statement, Statement> {
            // infer catalog from conformance test `env`
            val catalog = infer(env.toIonElement() as StructElement)
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val mode = when (options) {
                CompileType.PERMISSIVE -> Mode.PERMISSIVE()
                CompileType.STRICT -> Mode.STRICT()
            }
            return EvalExecutor(session, mode)
        }

        /**
         * Produces an inferred catalog from the environment.
         *
         * @param env
         * @return
         */
        private fun infer(env: StructElement): Catalog {
            val map = mutableMapOf<String, PType>()
            env.fields.forEach {
                map[it.name] = inferEnv(it.value)
            }
            return Catalog.builder()
                .name("default")
                .apply { load(env) }
                .build()
        }

        /**
         * Uses the planner to infer the type of the environment.
         */
        private fun inferEnv(env: AnyElement): PType {
            val catalog = Catalog.builder().name("default").build()
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val parseResult = parser.parse("`$env`")
            assertEquals(1, parseResult.statements.size)
            val stmt = parseResult.statements[0]
            val plan = planner.plan(stmt, session).plan
            return (plan.action as Query).getRex().getType().getPType()
        }

        /**
         * Loads each declared global of the catalog from the data element.
         *
         * TODO until this point, PartiQL Kotlin has only done top-level bindings.
         * TODO https://github.com/partiql/partiql-tests/issues/127
         *
         * Test data is "PartiQL encoded as Ion" hence we need the PartiQLValueIonReader.
         */
        private fun Catalog.Builder.load(env: StructElement) {
            for (f in env.fields) {
                val name = Name.of(f.name)
                val datum = DatumIonReaderBuilder.standard().build(f.value).read()
                val table = Table.standard(
                    name = name,
                    schema = PType.dynamic(),
                    datum = datum,
                )
                define(table)
            }
        }
    }
}
