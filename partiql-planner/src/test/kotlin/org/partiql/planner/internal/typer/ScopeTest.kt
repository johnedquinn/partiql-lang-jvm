package org.partiql.planner.internal.typer

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.relBinding
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.PType
import kotlin.test.assertEquals
import kotlin.test.fail

internal class ScopeTest {

    companion object {

        /**
         * <
         *   A : { B: <bool> } },
         *   a : { b: <bool> } },
         *   X : { ... } },
         *   x : { y: <bool>, ... } },
         *   Y : { ... } },
         *   T : { x: <bool>, x: <bool> } },
         * >
         */
        @JvmStatic
        val locals = TypeEnv(
            Env(
                Session.builder()
                    .catalog("currentCatalog")
                    .catalogs(
                        "currentCatalog" to object : ConnectorMetadata {
                            override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
                                return null
                            }
                        }
                    )
                    .build()
            ),
            Scope(
                listOf(
                    relBinding("A", struct("B" to PType.typeBool().toCType())),
                    relBinding("a", struct("b" to PType.typeBool().toCType())),
                    relBinding("X", struct(open = true)),
                    relBinding("x", struct("Y" to PType.typeBool().toCType(), open = false)), // We currently don't allow for partial schema structs
                    relBinding("y", struct(open = true)),
                    relBinding("T", struct("x" to PType.typeBool().toCType(), "x" to PType.typeBool().toCType())),
                ),
                outer = emptyList()
            )
        )

        private fun struct(vararg fields: Pair<String, CompilerType>, open: Boolean = false): CompilerType {
            return when (open) {
                true -> PType.typeStruct().toCType()
                false -> PType.typeRow(fields.map { CompilerType.Field(it.first, it.second) }).toCType()
            }
        }

        @JvmStatic
        public fun cases() = listOf<Pair<String, Int?>>(
            // root matching
            """ A.B """ to null,
            """ A."B" """ to null,
            """ "A".B """ to 0,
            """ "A"."B" """ to 0,
            """ "a".B """ to 1,
            """ "a"."B" """ to 1,
            """ x """ to null,
            // """ x.y """ to 3,
            """ y """ to 4,

            // struct searching
            """ b """ to null,
            """ "B" """ to 0,
            """ "b" """ to 1,
            """ "Y" """ to 3,

            // other
            """ T.x """ to 5
        )
    }

    @ParameterizedTest
    @MethodSource("cases")
    @Execution(ExecutionMode.CONCURRENT)
    fun resolve(case: Pair<String, Int?>) {
        val path = case.first.path()
        val expected = case.second
        val rex = locals.resolve(path)
        if (rex == null) {
            if (expected == null) {
                return // pass
            } else {
                fail("could not resolve variable")
            }
        }
        // For now, just traverse to the root
        var root = rex.op
        while (root !is Rex.Op.Var.Local) {
            root = when (root) {
                is Rex.Op.Path.Symbol -> root.root.op
                is Rex.Op.Path.Key -> root.root.op
                else -> {
                    fail("Expected path step of symbol or key, but found $root")
                }
            }
        }
        //
        assertEquals(expected, root.ref)
    }

    private fun String.path(): BindingPath {
        val steps = trim().split(".").map {
            when (it.startsWith("\"")) {
                true -> BindingName(it.drop(1).dropLast(1), BindingCase.SENSITIVE)
                else -> BindingName(it, BindingCase.INSENSITIVE)
            }
        }
        return BindingPath(steps)
    }
}