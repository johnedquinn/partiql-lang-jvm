/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.builder.plan
import org.partiql.planner.internal.ir.fnUnresolved
import org.partiql.planner.internal.ir.identifierQualified
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallStatic
import org.partiql.planner.internal.ir.rexOpCoalesce
import org.partiql.planner.internal.ir.rexOpCollection
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpNullif
import org.partiql.planner.internal.ir.rexOpPathIndex
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpSubquery
import org.partiql.planner.internal.ir.rexOpTupleUnion
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.typer.toStaticType
import org.partiql.types.StaticType
import org.partiql.types.TimeType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.nullValue
import org.partiql.value.stringValue

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr, context: Env): Rex = expr.accept(ToRex, context) // expr.toRex()

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, Env>() {

        override fun defaultReturn(node: AstNode, context: Env): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprLit(node: Expr.Lit, context: Env): Rex {
            val type = node.value.type.toStaticType()
            val op = rexOpLit(node.value)
            return rex(type, op)
        }

        override fun visitExprIon(node: Expr.Ion, ctx: Env): Rex {
            val value =
                PartiQLValueIonReaderBuilder
                    .standard().build(node.value).read()
            val type = value.type.toStaticType()
            return rex(type, rexOpLit(value))
        }

        /**
         * !! IMPORTANT !!
         *
         * This is the top-level visit for handling subquery coercion. The default behavior is to coerce to a scalar.
         * In some situations, ie comparison to complex types we may make assertions on the desired type.
         *
         * It is recommended that every method (except for the exceptional cases) recurse the tree from visitExprCoerce.
         *
         *  - RHS of comparison when LHS is an array or collection expression; and visa-versa
         *  - It is the collection expression of a FROM clause or JOIN
         *  - It is the RHS of an IN predicate
         *  - It is an argument of an OUTER set operator.
         *
         * @param node
         * @param ctx
         * @return
         */
        private fun visitExprCoerce(node: Expr, ctx: Env, coercion: Rex.Op.Subquery.Coercion = Rex.Op.Subquery.Coercion.SCALAR): Rex {
            val rex = super.visitExpr(node, ctx)
            return when (rex.op is Rex.Op.Select) {
                true -> rex(StaticType.ANY, rexOpSubquery(rex.op, coercion))
                else -> rex
            }
        }

        override fun visitExprVar(node: Expr.Var, context: Env): Rex {
            val type = (StaticType.ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            return rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, context: Env): Rex {
            val type = (StaticType.ANY)
            // Args
            val arg = visitExprCoerce(node.expr, context)
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id, true)
            val op = rexOpCallStatic(fn, args)
            return rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, context: Env): Rex {
            val type = (StaticType.ANY)
            // Args
            val lhs = visitExprCoerce(node.lhs, context)
            val rhs = visitExprCoerce(node.rhs, context)
            val args = listOf(lhs, rhs)
            return when (node.op) {
                Expr.Binary.Op.NE -> {
                    val op = negate(call("eq", lhs, rhs))
                    rex(type, op)
                }
                else -> {
                    // Fn
                    val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
                    val fn = fnUnresolved(id, true)
                    // Rex
                    val op = rexOpCallStatic(fn, args)
                    rex(type, op)
                }
            }
        }

        private fun mergeIdentifiers(root: Identifier, steps: List<Identifier>): Identifier {
            if (steps.isEmpty()) {
                return root
            }
            val (newRoot, firstSteps) = when (root) {
                is Identifier.Symbol -> root to emptyList()
                is Identifier.Qualified -> root.root to root.steps
            }
            val followingSteps = steps.flatMap { step ->
                when (step) {
                    is Identifier.Symbol -> listOf(step)
                    is Identifier.Qualified -> listOf(step.root) + step.steps
                }
            }
            return identifierQualified(newRoot, firstSteps + followingSteps)
        }

        override fun visitExprPath(node: Expr.Path, context: Env): Rex {
            // Args
            val root = visitExprCoerce(node.root, context)

            // Attempt to create qualified identifier
            val (newRoot, newSteps) = when (val op = root.op) {
                is Rex.Op.Var.Unresolved -> {
                    val identifierSteps = mutableListOf<Identifier>()
                    run {
                        node.steps.forEach { step ->
                            if (step !is Expr.Path.Step.Symbol) {
                                return@run
                            }
                            identifierSteps.add(AstToPlan.convert(step.symbol))
                        }
                    }
                    when (identifierSteps.size) {
                        0 -> root to node.steps
                        else -> {
                            val newRoot = rex(StaticType.ANY, rexOpVarUnresolved(mergeIdentifiers(op.identifier, identifierSteps), op.scope))
                            val newSteps = node.steps.subList(identifierSteps.size, node.steps.size)
                            newRoot to newSteps
                        }
                    }
                }
                else -> root to node.steps
            }

            // Return wrapped path
            return when (newSteps.isEmpty()) {
                true -> newRoot
                false -> newSteps.fold(newRoot) { current, step ->
                    val path = when (step) {
                        is Expr.Path.Step.Index -> {
                            val key = visitExprCoerce(step.key, context)
                            when (val astKey = step.key) {
                                is Expr.Lit -> when (astKey.value) {
                                    is StringValue -> rexOpPathKey(current, key)
                                    else -> rexOpPathIndex(current, key)
                                }
                                is Expr.Cast -> when (astKey.asType is Type.String) {
                                    true -> rexOpPathKey(current, key)
                                    false -> rexOpPathIndex(current, key)
                                }
                                else -> rexOpPathIndex(current, key)
                            }
                        }
                        is Expr.Path.Step.Symbol -> {
                            val identifier = AstToPlan.convert(step.symbol)
                            when (identifier.caseSensitivity) {
                                Identifier.CaseSensitivity.SENSITIVE -> rexOpPathKey(current, rexString(identifier.symbol))
                                Identifier.CaseSensitivity.INSENSITIVE -> rexOpPathSymbol(current, identifier.symbol)
                            }
                        }
                        is Expr.Path.Step.Unpivot -> error("Unpivot path not supported yet")
                        is Expr.Path.Step.Wildcard -> error("Wildcard path not supported yet")
                    }
                    rex(StaticType.ANY, path)
                }
            }
        }

        private fun rexString(str: String) = rex(StaticType.STRING, rexOpLit(stringValue(str)))

        override fun visitExprCall(node: Expr.Call, context: Env): Rex {
            val type = (StaticType.ANY)
            // Fn
            val id = AstToPlan.convert(node.function)
            if (id is Identifier.Symbol && id.symbol.equals("TUPLEUNION", ignoreCase = true)) {
                return visitExprCallTupleUnion(node, context)
            }
            val fn = fnUnresolved(id, false)
            // Args
            val args = node.args.map { visitExprCoerce(it, context) }
            // Rex
            val op = rexOpCallStatic(fn, args)
            return rex(type, op)
        }

        private fun visitExprCallTupleUnion(node: Expr.Call, context: Env): Rex {
            val type = (StaticType.STRUCT)
            val args = node.args.map { visitExprCoerce(it, context) }.toMutableList()
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, context: Env) = plan {
            val type = (StaticType.ANY)
            val rex = when (node.expr) {
                null -> null
                else -> visitExprCoerce(node.expr!!, context) // match `rex
            }

            // Converts AST CASE (x) WHEN y THEN z --> Plan CASE WHEN x = y THEN z
            val id = identifierSymbol(Expr.Binary.Op.EQ.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id, true)
            val createBranch: (Rex, Rex) -> Rex.Op.Case.Branch = { condition: Rex, result: Rex ->
                val updatedCondition = when (rex) {
                    null -> condition
                    else -> rex(type, rexOpCallStatic(fn.copy(), listOf(rex, condition)))
                }
                rexOpCaseBranch(updatedCondition, result)
            }

            val branches = node.branches.map {
                val branchCondition = visitExprCoerce(it.condition, context)
                val branchRex = visitExprCoerce(it.expr, context)
                createBranch(branchCondition, branchRex)
            }.toMutableList()

            val defaultRex = when (val default = node.default) {
                null -> rex(type = StaticType.ANY, op = rexOpLit(value = nullValue()))
                else -> visitExprCoerce(default, context)
            }
            val op = rexOpCase(branches = branches, default = defaultRex)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, context: Env): Rex {
            val type = when (node.type) {
                Expr.Collection.Type.BAG -> StaticType.BAG
                Expr.Collection.Type.ARRAY -> StaticType.LIST
                Expr.Collection.Type.VALUES -> StaticType.LIST
                Expr.Collection.Type.LIST -> StaticType.LIST
                Expr.Collection.Type.SEXP -> StaticType.SEXP
            }
            val values = node.values.map { visitExprCoerce(it, context) }
            val op = rexOpCollection(values)
            return rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, context: Env): Rex {
            val type = (StaticType.STRUCT)
            val fields = node.fields.map {
                val k = visitExprCoerce(it.name, context)
                val v = visitExprCoerce(it.value, context)
                rexOpStructField(k, v)
            }
            val op = rexOpStruct(fields)
            return rex(type, op)
        }

        // SPECIAL FORMS

        /**
         * <arg0> NOT? LIKE <arg1> ( ESCAPE <arg2>)?
         */
        override fun visitExprLike(node: Expr.Like, ctx: Env): Rex {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = visitExprCoerce(node.pattern, ctx)
            val arg2 = node.escape?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            var call = when (arg2) {
                null -> call("like", arg0, arg1)
                else -> call("like_escape", arg0, arg1, arg2)
            }
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            return rex(type, call)
        }

        /**
         * <arg0> NOT? BETWEEN <arg1> AND <arg2>
         */
        override fun visitExprBetween(node: Expr.Between, ctx: Env): Rex = plan {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = visitExprCoerce(node.from, ctx)
            val arg2 = visitExprCoerce(node.to, ctx)
            // Call
            var call = call("between", arg0, arg1, arg2)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            rex(type, call)
        }

        /**
         * <arg0> NOT? IN <arg1>
         *
         * SQL Spec 1999 section 8.4
         * RVC IN IPV is equivalent to RVC = ANY IPV -> Quantified Comparison Predicate
         * Which means:
         * Let the expression be T in C, where C is [a1, ..., an]
         * T in C is true iff T = a_x is true for any a_x in [a1, ...., an]
         * T in C is false iff T = a_x is false for every a_x in [a1, ....., an ] or cardinality of the collection is 0.
         * Otherwise, T in C is unknown.
         *
         */
        override fun visitExprInCollection(node: Expr.InCollection, ctx: Env): Rex {
            val type = StaticType.BOOL
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx) // !! don't insert scalar subquery coercions

            // Call
            var call = call("in_collection", arg0, arg1)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            return rex(type, call)
        }

        /**
         * <arg0> IS <NOT>? <type>
         */
        override fun visitExprIsType(node: Expr.IsType, ctx: Env): Rex {
            val type = StaticType.BOOL
            // arg
            val arg0 = visitExprCoerce(node.value, ctx)

            var call = when (val targetType = node.type) {
                is Type.NullType -> call("is_null", arg0)
                is Type.Missing -> call("is_missing", arg0)
                is Type.Bool -> call("is_bool", arg0)
                is Type.Tinyint -> call("is_int8", arg0)
                is Type.Smallint, is Type.Int2 -> call("is_int16", arg0)
                is Type.Int4 -> call("is_int32", arg0)
                is Type.Bigint, is Type.Int8 -> call("is_int64", arg0)
                is Type.Int -> call("is_int", arg0)
                is Type.Real -> call("is_real", arg0)
                is Type.Float32 -> call("is_float32", arg0)
                is Type.Float64 -> call("is_float64", arg0)
                is Type.Decimal -> call("is_decimal", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                is Type.Numeric -> call("is_numeric", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                is Type.Char -> call("is_char", targetType.length.toRex(), arg0)
                is Type.Varchar -> call("is_varchar", targetType.length.toRex(), arg0)
                is Type.String -> call("is_string", targetType.length.toRex(), arg0)
                is Type.Symbol -> call("is_symbol", arg0)
                is Type.Bit -> call("is_bit", arg0)
                is Type.BitVarying -> call("is_bitVarying", arg0)
                is Type.ByteString -> call("is_byteString", arg0)
                is Type.Blob -> call("is_blob", arg0)
                is Type.Clob -> call("is_clob", arg0)
                is Type.Date -> call("is_date", arg0)
                is Type.Time -> call("is_time", arg0)
                // TODO: DO we want to seperate with time zone vs without time zone into two different type in the plan?
                //  leave the parameterized type out for now until the above is answered
                is Type.TimeWithTz -> call("is_timeWithTz", arg0)
                is Type.Timestamp -> call("is_timestamp", arg0)
                is Type.TimestampWithTz -> call("is_timestampWithTz", arg0)
                is Type.Interval -> call("is_interval", arg0)
                is Type.Bag -> call("is_bag", arg0)
                is Type.List -> call("is_list", arg0)
                is Type.Sexp -> call("is_sexp", arg0)
                is Type.Tuple -> call("is_tuple", arg0)
                is Type.Struct -> call("is_struct", arg0)
                is Type.Any -> call("is_any", arg0)
                is Type.Custom -> call("is_custom", arg0)
            }

            if (node.not == true) {
                call = negate(call)
            }

            return rex(type, call)
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Env): Rex {
            val type = StaticType.ANY
            val args = node.args.map { arg ->
                visitExprCoerce(arg, ctx)
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        override fun visitExprNullIf(node: Expr.NullIf, ctx: Env): Rex {
            val type = StaticType.ANY
            val value = visitExprCoerce(node.value, ctx)
            val nullifier = visitExprCoerce(node.nullifier, ctx)
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * SUBSTRING(<arg0> (FROM <arg1> (FOR <arg2>)?)? )
         */
        override fun visitExprSubstring(node: Expr.Substring, ctx: Env): Rex {
            val type = StaticType.ANY
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.start?.let { visitExprCoerce(it, ctx) } ?: rex(StaticType.INT, rexOpLit(int64Value(1)))
            val arg2 = node.length?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            val call = when (arg2) {
                null -> call("substring", arg0, arg1)
                else -> call("substring_length", arg0, arg1, arg2)
            }
            return rex(type, call)
        }

        /**
         * POSITION(<arg0> IN <arg1>)
         */
        override fun visitExprPosition(node: Expr.Position, ctx: Env): Rex {
            val type = StaticType.ANY
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call
            val call = call("position", arg0, arg1)
            return rex(type, call)
        }

        /**
         * TRIM([LEADING|TRAILING|BOTH]? (<arg1> FROM)? <arg0>)
         */
        override fun visitExprTrim(node: Expr.Trim, ctx: Env): Rex {
            val type = StaticType.TEXT
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.chars?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            val call = when (node.spec) {
                Expr.Trim.Spec.LEADING -> when (arg1) {
                    null -> call("trim_leading", arg0)
                    else -> call("trim_leading_chars", arg0, arg1)
                }
                Expr.Trim.Spec.TRAILING -> when (arg1) {
                    null -> call("trim_trailing", arg0)
                    else -> call("trim_trailing_chars", arg0, arg1)
                }
                // TODO: We may want to add a trim_both for trim(BOTH FROM arg)
                else -> when (arg1) {
                    null -> callNonHidden("trim", arg0)
                    else -> call("trim_chars", arg0, arg1)
                }
            }
            return rex(type, call)
        }

        override fun visitExprOverlay(node: Expr.Overlay, ctx: Env): Rex {
            TODO("SQL Special Form OVERLAY")
        }

        override fun visitExprExtract(node: Expr.Extract, ctx: Env): Rex {
            TODO("SQL Special Form EXTRACT")
        }

        // TODO: Ignoring type parameter now
        override fun visitExprCast(node: Expr.Cast, ctx: Env): Rex {
            val type = node.asType
            val arg0 = visitExprCoerce(node.value, ctx)
            return when (type) {
                is Type.NullType -> error("Cannot cast any value to NULL")
                is Type.Missing -> error("Cannot cast any value to MISSING")
                is Type.Bool -> rex(StaticType.BOOL, call("cast_bool", arg0))
                is Type.Tinyint -> TODO("Static Type does not have TINYINT type")
                is Type.Smallint, is Type.Int2 -> rex(StaticType.INT2, call("cast_int16", arg0))
                is Type.Int4 -> rex(StaticType.INT4, call("cast_int32", arg0))
                is Type.Bigint, is Type.Int8 -> rex(StaticType.INT8, call("cast_int64", arg0))
                is Type.Int -> rex(StaticType.INT, call("cast_int", arg0))
                is Type.Real -> TODO("Static Type does not have REAL type")
                is Type.Float32 -> TODO("Static Type does not have FLOAT32 type")
                is Type.Float64 -> rex(StaticType.FLOAT, call("cast_float64", arg0))
                is Type.Decimal -> rex(StaticType.DECIMAL, call("cast_decimal", arg0))
                is Type.Numeric -> rex(StaticType.DECIMAL, call("cast_numeric", arg0))
                is Type.Char -> rex(StaticType.CHAR, call("cast_char", arg0))
                is Type.Varchar -> rex(StaticType.STRING, call("cast_varchar", arg0))
                is Type.String -> rex(StaticType.STRING, call("cast_string", arg0))
                is Type.Symbol -> rex(StaticType.SYMBOL, call("cast_symbol", arg0))
                is Type.Bit -> TODO("Static Type does not have Bit type")
                is Type.BitVarying -> TODO("Static Type does not have BitVarying type")
                is Type.ByteString -> TODO("Static Type does not have ByteString type")
                is Type.Blob -> rex(StaticType.BLOB, call("cast_blob", arg0))
                is Type.Clob -> rex(StaticType.CLOB, call("cast_clob", arg0))
                is Type.Date -> rex(StaticType.DATE, call("cast_date", arg0))
                is Type.Time -> rex(StaticType.TIME, call("cast_time", arg0))
                is Type.TimeWithTz -> rex(TimeType(null, true), call("cast_timeWithTz", arg0))
                is Type.Timestamp -> TODO("Need to rebase main")
                is Type.TimestampWithTz -> rex(StaticType.TIMESTAMP, call("cast_timeWithTz", arg0))
                is Type.Interval -> TODO("Static Type does not have Interval type")
                is Type.Bag -> rex(StaticType.BAG, call("cast_bag", arg0))
                is Type.List -> rex(StaticType.LIST, call("cast_list", arg0))
                is Type.Sexp -> rex(StaticType.SEXP, call("cast_sexp", arg0))
                is Type.Tuple -> rex(StaticType.STRUCT, call("cast_tuple", arg0))
                is Type.Struct -> rex(StaticType.STRUCT, call("cast_struct", arg0))
                is Type.Any -> rex(StaticType.ANY, call("cast_any", arg0))
                is Type.Custom -> TODO("Custom type not supported ")
            }
        }

        override fun visitExprCanCast(node: Expr.CanCast, ctx: Env): Rex {
            TODO("PartiQL Special Form CAN_CAST")
        }

        override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Env): Rex {
            TODO("PartiQL Special Form CAN_LOSSLESS_CAST")
        }

        override fun visitExprDateAdd(node: Expr.DateAdd, ctx: Env): Rex {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_ADD(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_ADD(TIMEZONE_MINUTE, ...)")
                else -> call("date_add_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprDateDiff(node: Expr.DateDiff, ctx: Env): Rex {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_DIFF(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_DIFF(TIMEZONE_MINUTE, ...)")
                else -> call("date_diff_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: Env): Rex {
            val type = StaticType.ANY
            val fn = node.attribute.name.lowercase()
            val call = call(fn)
            return rex(type, call)
        }

        override fun visitExprSFW(node: Expr.SFW, context: Env): Rex = RelConverter.apply(node, context)

        // Helpers

        private fun bool(v: Boolean): Rex {
            val type = StaticType.BOOL
            val op = rexOpLit(boolValue(v))
            return rex(type, op)
        }

        private fun negate(call: Rex.Op.Call): Rex.Op.Call.Static {
            val name = Expr.Unary.Op.NOT.name
            val id = identifierSymbol(name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id, true)
            // wrap
            val arg = rex(StaticType.BOOL, call)
            // rewrite call
            return rexOpCallStatic(fn, listOf(arg))
        }

        /**
         * Create a [Rex.Op.Call.Static] node which has a hidden unresolved Function.
         * The purpose of having such hidden function is to prevent usage of generated function name in query text.
         */
        private fun call(name: String, vararg args: Rex): Rex.Op.Call.Static {
            val id = identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id, true)
            return rexOpCallStatic(fn, args.toList())
        }

        /**
         * Create a [Rex.Op.Call.Static] node which has a non-hidden unresolved Function.
         */
        private fun callNonHidden(name: String, vararg args: Rex): Rex.Op.Call.Static {
            val id = identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE)
            val fn = fnUnresolved(id, false)
            return rexOpCallStatic(fn, args.toList())
        }

        private fun Int?.toRex() = rex(StaticType.INT4, rexOpLit(int32Value(this)))
    }
}
