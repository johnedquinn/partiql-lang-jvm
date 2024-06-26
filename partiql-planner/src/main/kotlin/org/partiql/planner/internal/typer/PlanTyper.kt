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

package org.partiql.planner.internal.typer

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.PlanningProblemDetails
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ResolutionStrategy
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.aggResolved
import org.partiql.planner.internal.ir.fnResolved
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.rel
import org.partiql.planner.internal.ir.relOpAggregate
import org.partiql.planner.internal.ir.relOpAggregateCall
import org.partiql.planner.internal.ir.relOpDistinct
import org.partiql.planner.internal.ir.relOpErr
import org.partiql.planner.internal.ir.relOpExclude
import org.partiql.planner.internal.ir.relOpExcludeItem
import org.partiql.planner.internal.ir.relOpFilter
import org.partiql.planner.internal.ir.relOpJoin
import org.partiql.planner.internal.ir.relOpLimit
import org.partiql.planner.internal.ir.relOpOffset
import org.partiql.planner.internal.ir.relOpProject
import org.partiql.planner.internal.ir.relOpScan
import org.partiql.planner.internal.ir.relOpScanIndexed
import org.partiql.planner.internal.ir.relOpSort
import org.partiql.planner.internal.ir.relOpUnpivot
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallDynamic
import org.partiql.planner.internal.ir.rexOpCallDynamicCandidate
import org.partiql.planner.internal.ir.rexOpCallStatic
import org.partiql.planner.internal.ir.rexOpCaseBranch
import org.partiql.planner.internal.ir.rexOpCoalesce
import org.partiql.planner.internal.ir.rexOpCollection
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpNullif
import org.partiql.planner.internal.ir.rexOpPathIndex
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpPivot
import org.partiql.planner.internal.ir.rexOpSelect
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpTupleUnion
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.planner.internal.ir.util.PlanRewriter
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BoolType
import org.partiql.types.CollectionType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.ANY
import org.partiql.types.StaticType.Companion.BOOL
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BoolValue
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TextValue
import org.partiql.value.boolValue
import org.partiql.value.stringValue

/**
 * Rewrites an untyped algebraic translation of the query to be both typed and have resolved variables.
 *
 * @property env
 * @property onProblem
 */
@OptIn(PartiQLValueExperimental::class)
internal class PlanTyper(
    private val env: Env,
    private val onProblem: ProblemCallback,
) {

    /**
     * Rewrite the statement with inferred types and resolved variables
     */
    fun resolve(statement: Statement): Statement {
        if (statement !is Statement.Query) {
            throw IllegalArgumentException("PartiQLPlanner only supports Query statements")
        }
        // root TypeEnv has no bindings
        val typeEnv = TypeEnv(schema = emptyList())
        val root = statement.root.type(typeEnv)
        return statementQuery(root)
    }

    private companion object {
        private val FUNCTIONS_HANDLING_MISSING = setOf<String>("is_null", "is_missing", "eq", "and", "or", "not")
    }

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer TypeEnv of a query expression — only used by scan variable resolution.
     * @property strategy
     */
    private inner class RelTyper(
        private val outer: TypeEnv,
        private val strategy: ResolutionStrategy,
    ) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer, ResolutionStrategy.GLOBAL)
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val type = ctx!!.copyWithSchema(listOf(valueT))
            // rewrite
            val op = relOpScan(rex)
            return rel(type, op)
        }

        override fun visitRelOpErr(node: Rel.Op.Err, ctx: Rel.Type?): Rel {
            val type = ctx ?: relType(emptyList(), emptySet())
            return rel(type, node)
        }

        /**
         * The output schema of a `rel.op.scan_index` is the value binding and index binding.
         */
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer, ResolutionStrategy.GLOBAL)
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val indexT = StaticType.INT8
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT))
            // rewrite
            val op = relOpScanIndexed(rex)
            return rel(type, op)
        }

        /**
         * TODO handle NULL|STRUCT type
         */
        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer, ResolutionStrategy.GLOBAL)

            // only UNPIVOT a struct
            if (rex.type !is StructType) {
                handleUnexpectedType(rex.type, expected = setOf(StaticType.STRUCT))
                return rel(ctx!!, relOpErr("UNPIVOT on non-STRUCT type ${rex.type}"))
            }

            // compute element type
            val t = rex.type
            val e = if (t.contentClosed) {
                StaticType.unionOf(t.fields.map { it.value }.toSet()).flatten()
            } else {
                ANY
            }

            // compute rel type
            val kType = STRING
            val vType = e
            val type = ctx!!.copyWithSchema(listOf(kType, vType))

            // rewrite
            val op = relOpUnpivot(rex)
            return rel(type, op)
        }

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type?): Rel {
            val input = visitRel(node.input, ctx)
            return rel(input.type, relOpDistinct(input))
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema)
            val predicate = node.predicate.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpFilter(input, predicate)
            return rel(type, op)
        }

        override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema)
            val specs = node.specs.map {
                val rex = it.rex.type(typeEnv)
                it.copy(rex = rex)
            }
            // output schema of a sort is the same as the input
            val type = input.type.copy(props = setOf(Rel.Prop.ORDERED))
            // rewrite
            val op = relOpSort(input, specs)
            return rel(type, op)
        }

        override fun visitRelOpUnion(node: Rel.Op.Union, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Union")
        }

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Intersect")
        }

        override fun visitRelOpExcept(node: Rel.Op.Except, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Except")
        }

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type limit expression using outer scope with global resolution
            val limit = node.limit.type(outer, ResolutionStrategy.GLOBAL)
            // check types
            assertAsInt(limit.type)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpLimit(input, limit)
            return rel(type, op)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type offset expression using outer scope with global resolution
            val offset = node.offset.type(outer, ResolutionStrategy.GLOBAL)
            // check types
            assertAsInt(offset.type)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpOffset(input, offset)
            return rel(type, op)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema)
            val projections = node.projections.map {
                it.type(typeEnv)
            }
            // compute output schema
            val schema = projections.map { it.type }
            val type = ctx!!.copyWithSchema(schema)
            // rewrite
            val op = relOpProject(input, projections)
            return rel(type, op)
        }

        override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type?): Rel {
            // Rewrite LHS and RHS
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)

            // Calculate output schema given JOIN type
            val schema = lhs.type.schema + rhs.type.schema
            val type = relType(schema, ctx!!.props)

            // Type the condition on the output schema
            val condition = node.rex.type(TypeEnv(type.schema))

            val op = relOpJoin(lhs, rhs, condition, node.type)
            return rel(type, op)
        }

        /**
         * Initial implementation of `EXCLUDE` schema inference. Until an RFC is finalized for `EXCLUDE`
         * (https://github.com/partiql/partiql-spec/issues/39),
         *
         * This behavior is considered experimental and subject to change.
         *
         * This implementation includes
         *  - Excluding tuple bindings (e.g. t.a.b.c)
         *  - Excluding tuple wildcards (e.g. t.a.*.b)
         *  - Excluding collection indexes (e.g. t.a[0].b -- behavior subject to change; see below discussion)
         *  - Excluding collection wildcards (e.g. t.a[*].b)
         *
         * There are still discussion points regarding the following edge cases:
         *  - EXCLUDE on a tuple attribute that doesn't exist -- give an error/warning?
         *      - currently no error
         *  - EXCLUDE on a tuple attribute that has duplicates -- give an error/warning? exclude one? exclude both?
         *      - currently excludes both w/ no error
         *  - EXCLUDE on a collection index as the last step -- mark element type as optional?
         *      - currently element type as-is
         *  - EXCLUDE on a collection index w/ remaining path steps -- mark last step's type as optional?
         *      - currently marks last step's type as optional
         *  - EXCLUDE on a binding tuple variable (e.g. SELECT ... EXCLUDE t FROM t) -- error?
         *      - currently a parser error
         *  - EXCLUDE on a union type -- give an error/warning? no-op? exclude on each type in union?
         *      - currently exclude on each union type
         *  - If SELECT list includes an attribute that is excluded, we could consider giving an error in PlanTyper or
         * some other semantic pass
         *      - currently does not give an error
         */
        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // apply exclusions to the input schema
            val init = input.type.schema.map { it.copy() }
            val schema = node.items.fold((init)) { bindings, item -> excludeBindings(bindings, item) }

            // rewrite
            val type = ctx!!.copy(schema = schema)

            // resolve exclude path roots
            val newItems = node.items.map { item ->
                val resolvedRoot = when (val root = item.root) {
                    is Rex.Op.Var.Unresolved -> {
                        // resolve `root` to local binding
                        val locals = TypeEnv(input.type.schema)
                        val path = root.identifier.toBindingPath()
                        val resolved = locals.resolve(path)
                        if (resolved == null) {
                            handleUnresolvedExcludeRoot(root.identifier)
                            root
                        } else {
                            // root of exclude is always a symbol
                            resolved.op as Rex.Op.Var
                        }
                    }
                    is Rex.Op.Var.Resolved -> root
                }
                val steps = item.steps
                relOpExcludeItem(resolvedRoot, steps)
            }

            val op = relOpExclude(input, newItems)
            return rel(type, op)
        }

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // type the calls and groups
            val typer = RexTyper(TypeEnv(input.type.schema), ResolutionStrategy.LOCAL)

            // typing of aggregate calls is slightly more complicated because they are not expressions.
            val calls = node.calls.mapIndexed { i, call ->
                when (val agg = call.agg) {
                    is Agg.Resolved -> call to ctx!!.schema[i].type
                    is Agg.Unresolved -> typer.resolveAgg(agg, call.args)
                }
            }
            val groups = node.groups.map { typer.visitRex(it, null) }

            // Compute schema using order (calls...groups...)
            val schema = mutableListOf<StaticType>()
            schema += calls.map { it.second }
            schema += groups.map { it.type }

            // rewrite with typed calls and groups
            val type = ctx!!.copyWithSchema(schema)
            val op = relOpAggregate(
                input = input,
                strategy = node.strategy,
                calls = calls.map { it.first },
                groups = groups,
            )
            return rel(type, op)
        }
    }

    /**
     * Types a PartiQL expression tree. For now, we ignore the pre-existing type. We assume all existing types
     * are simply the `any`, so we keep the new type. Ideally we can programmatically calculate the most specific type.
     *
     * We should consider making the StaticType? parameter non-nullable.
     *
     * @property locals TypeEnv in which this rex tree is evaluated.
     */
    @OptIn(PartiQLValueExperimental::class)
    private inner class RexTyper(
        private val locals: TypeEnv,
        private val strategy: ResolutionStrategy,
    ) : PlanRewriter<StaticType?>() {

        override fun visitRex(node: Rex, ctx: StaticType?): Rex = visitRexOp(node.op, node.type) as Rex

        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType?): Rex {
            // type comes from RexConverter
            return rex(ctx!!, node)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: StaticType?): Rex {
            assert(node.ref < locals.schema.size) { "Invalid resolved variable (var ${node.ref}) for $locals" }
            val type = locals.schema[node.ref].type
            return rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType?): Rex {
            val path = node.identifier.toBindingPath()
            val strategy = when (node.scope) {
                Rex.Op.Var.Scope.DEFAULT -> strategy
                Rex.Op.Var.Scope.LOCAL -> ResolutionStrategy.LOCAL
            }
            val resolvedVar = env.resolve(path, locals, strategy)
            if (resolvedVar == null) {
                val details = handleUndefinedVariable(node.identifier, locals.schema.map { it.name }.toSet())
                return rex(ANY, rexOpErr(details.message))
            }
            return visitRex(resolvedVar, null)
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType?): Rex {
            val catalog = env.catalogs[node.ref.catalog]
            val type = catalog.symbols[node.ref.symbol].type
            return rex(type, node)
        }

        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)

            // Check Index Type
            if (!key.type.mayBeType<IntType>()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpErr("Collections must be indexed with integers, found ${key.type}"))
            }

            // Get Element Type(s)
            val elementTypes = root.type.allTypes.mapNotNull { type ->
                when (type) {
                    is ListType -> type.elementType
                    is SexpType -> type.elementType
                    else -> null
                }
            }

            // Check that Root was LIST or SEXP by checking accumuated element types
            if (elementTypes.isEmpty()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpErr("Only lists and s-expressions can be indexed with integers, found ${root.type}"))
            }
            return rex(unionOf(elementTypes), rexOpPathIndex(root, key))
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)

            // Check Key Type
            if (!key.type.mayBeType<StringType>()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpErr("Expected string but found: ${key.type}."))
            }

            // Check Root Type
            if (!root.type.mayBeType<StructType>()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpErr("Key lookup may only occur on structs, not ${root.type}."))
            }

            // Get Element Type
            val elementType = root.type.inferListNotNull { type ->
                val struct = type as? StructType ?: return@inferListNotNull null
                if (key.op is Rex.Op.Lit) {
                    val lit = key.op.value
                    if (lit is TextValue<*> && !lit.isNull) {
                        val id = identifierSymbol(lit.string!!, Identifier.CaseSensitivity.SENSITIVE)
                        inferStructLookup(struct, id)?.first
                    } else {
                        error("Expected text literal, but got $lit")
                    }
                } else {
                    // cannot infer type of non-literal path step because we don't know its value
                    // we might improve upon this with some constant folding prior to typing
                    ANY
                }
            }
            if (elementType.isEmpty()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpPathKey(root, key))
            }
            return rex(unionOf(elementType), rexOpPathKey(root, key))
        }

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)

            // Check Root Type
            if (!root.type.mayBeType<StructType>()) {
                handleAlwaysMissing()
                return rex(ANY, rexOpErr("Symbol lookup may only occur on structs, not ${root.type}."))
            }

            // Get Element Types
            val paths = root.type.inferRexListNotNull { type ->
                val struct = type as? StructType ?: return@inferRexListNotNull null
                val (pathType, replacementId) = inferStructLookup(
                    struct,
                    identifierSymbol(node.key, Identifier.CaseSensitivity.INSENSITIVE)
                ) ?: return@inferRexListNotNull null
                when (replacementId.caseSensitivity) {
                    Identifier.CaseSensitivity.INSENSITIVE -> rex(pathType, rexOpPathSymbol(root, replacementId.symbol))
                    Identifier.CaseSensitivity.SENSITIVE -> rex(
                        pathType,
                        rexOpPathKey(root, rexString(replacementId.symbol))
                    )
                }
            }
            // Determine output type
            val type = when (paths.size) {
                // Escape early since no inference could be made
                0 -> {
                    handleAlwaysMissing()
                    return rex(ANY, Rex.Op.Path.Symbol(root, node.key))
                }
                else -> unionOf(paths.map { it.type }.toSet())
            }

            // replace step only if all are disambiguated
            val allElementsInferred = paths.size == root.type.allTypes.size
            val firstPathOp = paths.first().op
            val replacementOp = when (allElementsInferred && paths.map { it.op }.all { it == firstPathOp }) {
                true -> firstPathOp
                false -> rexOpPathSymbol(root, node.key)
            }
            return rex(type, replacementOp)
        }

        private fun rexString(str: String) = rex(STRING, rexOpLit(stringValue(str)))

        /**
         * Resolve and type scalar function calls.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: StaticType?): Rex {
            // Already resolved; unreachable but handle gracefully.
            if (node.fn is Fn.Resolved) return rex(ctx!!, node)

            // Type the arguments
            val fn = node.fn as Fn.Unresolved
            val args = node.args.map { visitRex(it, null) }

            // Try to match the arguments to functions defined in the catalog
            return when (val match = env.resolveFn(fn, args)) {
                is FnMatch.Ok -> toRexCall(match, args)
                is FnMatch.Dynamic -> {
                    val types = mutableSetOf<StaticType>()
                    val candidates = match.candidates.map { candidate ->
                        val rex = toRexCall(candidate, args)
                        val staticCall =
                            rex.op as? Rex.Op.Call.Static ?: error("ToRexCall should always return a static call.")
                        val resolvedFn = staticCall.fn as? Fn.Resolved ?: error("This should have been resolved")
                        types.add(rex.type)
                        val coercions = candidate.mapping.map { it?.let { fnResolved(it) } }
                        rexOpCallDynamicCandidate(fn = resolvedFn, coercions = coercions)
                    }
                    val op = rexOpCallDynamic(args = args, candidates = candidates)
                    rex(type = unionOf(types).flatten(), op = op)
                }
                is FnMatch.Error -> {
                    handleUnknownFunction(match)
                    rexErr("Unknown scalar function $fn")
                }
            }
        }

        override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: StaticType?): Rex {
            return rex(ANY, rexOpErr("Direct dynamic calls are not supported. This should have been a static call."))
        }

        private fun toRexCall(
            match: FnMatch.Ok<FunctionSignature.Scalar>,
            args: List<Rex>,
        ): Rex {
            // Found a match!
            val newFn = fnResolved(match.signature)
            val newArgs = rewriteFnArgs(match.mapping, args)

            // Check literal missing inputs
            val argAlwaysMissing = args.any {
                val op = it.op as? Rex.Op.Lit ?: return@any false
                op.value is MissingValue
            }
            if (argAlwaysMissing) {
                // TODO: The V1 branch has support for isMissable and isMissingCall. This codebase, however, does not
                //  have support for these concepts yet. This specific commit (see Git blame) does not seek to add this
                //  functionality. Below is a work-around for the lack of "isMissable" and "isMissingCall"
                if (match.signature.name !in FUNCTIONS_HANDLING_MISSING) {
                    handleAlwaysMissing()
                }
            }

            // Type return
            val returns = newFn.signature.returns
            val op = rexOpCallStatic(newFn, newArgs)
            return rex(returns.toStaticType().flatten(), op)
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType?): Rex {
            // Rewrite CASE-WHEN branches
            val oldBranches = node.branches.toTypedArray()
            val newBranches = mutableListOf<Rex.Op.Case.Branch>()
            val typer = DynamicTyper()
            for (i in oldBranches.indices) {

                // Type the branch
                var branch = oldBranches[i]
                branch = visitRexOpCaseBranch(branch, branch.rex.type)

                // Emit typing error if a branch condition is never a boolean (prune)
                if (!branch.condition.type.mayBeType<BoolType>()) {
                    onProblem.invoke(
                        Problem(
                            UNKNOWN_PROBLEM_LOCATION,
                            PlanningProblemDetails.IncompatibleTypesForOp(branch.condition.type.allTypes, "CASE_WHEN")
                        )
                    )
                }

                // Accumulate typing information, but skip if literal NULL or MISSING
                typer.accumulate(branch.rex)
                newBranches.add(branch)
            }

            // Rewrite ELSE branch
            var newDefault = visitRex(node.default, null)
            typer.accumulate(newDefault)

            // Compute the CASE-WHEN type from the accumulator
            val (type, mapping) = typer.mapping()

            // Rewrite branches if we have coercions.
            if (mapping != null) {
                val msize = mapping.size
                val bsize = newBranches.size + 1
                assert(msize == bsize) { "Coercion mappings `len $msize` did not match the number of CASE-WHEN branches `len $bsize`" }
                // Rewrite branches
                for (i in newBranches.indices) {
                    val (operand, target) = mapping[i]
                    if (operand == target) continue // skip
                    val cast = env.fnResolver.cast(operand, target)
                    val branch = newBranches[i]
                    val rex = rex(type, rexOpCallStatic(fnResolved(cast), listOf(branch.rex)))
                    newBranches[i] = branch.copy(rex = rex)
                }
                // Rewrite default
                val (operand, target) = mapping.last()
                if (operand != target) {
                    val cast = env.fnResolver.cast(operand, target)
                    newDefault = rex(type, rexOpCallStatic(fnResolved(cast), listOf(newDefault)))
                }
            }

            // TODO constant folding in planner which also means branch pruning
            // This is added for backwards compatibility, we return the first branch if it's true
            if (boolOrNull(newBranches[0].condition.op) == true) {
                return newBranches[0].rex
            }

            val op = Rex.Op.Case(newBranches, newDefault)
            return rex(type, op)
        }

        // COALESCE(v1, v2,..., vN)
        // ==
        // CASE
        //     WHEN v1 IS NOT NULL THEN v1  -- WHEN branch always a boolean
        //     WHEN v2 IS NOT NULL THEN v2  -- WHEN branch always a boolean
        //     ... -- similarly for v3..vN-1
        //     ELSE vN
        // END
        // --> minimal common supertype of(<type v1>, <type v2>, ..., <type v3>)
        override fun visitRexOpCoalesce(node: Rex.Op.Coalesce, ctx: StaticType?): Rex {
            val args = node.args.map { visitRex(it, it.type) }.toMutableList()
            val typer = DynamicTyper()
            args.forEach { v -> typer.accumulate(v) }
            val (type, mapping) = typer.mapping()
            if (mapping != null) {
                assert(mapping.size == args.size) { "Coercion mappings `len ${mapping.size}` did not match the number of COALESCE arguments `len ${args.size}`" }
                for (i in args.indices) {
                    val (operand, target) = mapping[i]
                    if (operand == target) continue // skip; no coercion needed
                    val cast = env.fnResolver.cast(operand, target)
                    val rex = rex(type, rexOpCallStatic(fnResolved(cast), listOf(args[i])))
                    args[i] = rex
                }
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        // NULLIF(v1, v2)
        // ==
        // CASE
        //     WHEN v1 = v2 THEN NULL -- WHEN branch always a boolean
        //     ELSE v1
        // END
        // --> minimal common supertype of (NULL, <type v1>)
        override fun visitRexOpNullif(node: Rex.Op.Nullif, ctx: StaticType?): Rex {
            val value = visitRex(node.value, node.value.type)
            val nullifier = visitRex(node.nullifier, node.nullifier.type)
            val typer = DynamicTyper()

            // Accumulate typing information
            typer.accumulate(value)
            val (type, _) = typer.mapping()
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * Returns the boolean value of the expression. For now, only handle literals.
         */
        @OptIn(PartiQLValueExperimental::class)
        private fun boolOrNull(op: Rex.Op): Boolean? {
            return if (op is Rex.Op.Lit && op.value is BoolValue) op.value.value else null
        }

        /**
         * We need special handling for:
         * ```
         * CASE
         *   WHEN a IS STRUCT THEN a
         *   ELSE { 'a': a }
         * END
         * ```
         * When we type the above, if we know that `a` can be many different types (one of them being a struct),
         * then when we see the top-level `a IS STRUCT`, then we can assume that the `a` on the RHS is definitely a
         * struct. We handle this by using [foldCaseBranch].
         */
        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: StaticType?): Rex.Op.Case.Branch {
            val visitedCondition = visitRex(node.condition, node.condition.type)
            val visitedReturn = visitRex(node.rex, node.rex.type)
            return foldCaseBranch(visitedCondition, visitedReturn)
        }

        /**
         * This takes in a [Rex.Op.Case.Branch.condition] and [Rex.Op.Case.Branch.rex]. If the [condition] is a type check,
         * AKA `<var> IS STRUCT`, then this function will return a new [Rex.Op.Case.Branch] whose [Rex.Op.Case.Branch.rex]
         * assumes that the type will always be a struct. The [Rex.Op.Case.Branch.condition] will be replaced by a
         * boolean literal if it is KNOWN whether a branch will always/never execute. This can be used to prune the
         * branch in subsequent passes.
         *
         * TODO: Currently, this only folds type checking for STRUCTs. We need to add support for all other types.
         *
         * TODO: I added a check for [Rex.Op.Var.Resolved] as it seemed odd to replace a general expression like:
         *  `WHEN { 'a': { 'b': 1} }.a IS STRUCT THEN { 'a': { 'b': 1} }.a.b`. We can discuss this later, but I'm
         *  currently limiting the scope of this intentionally.
         */
        private fun foldCaseBranch(condition: Rex, result: Rex): Rex.Op.Case.Branch {
            val call = condition.op as? Rex.Op.Call ?: return rexOpCaseBranch(condition, result)
            when (call) {
                is Rex.Op.Call.Dynamic -> {
                    val rex = call.candidates.map { candidate ->
                        val fn = candidate.fn as? Fn.Resolved ?: return rexOpCaseBranch(condition, result)
                        if (fn.signature.name.equals("is_struct", ignoreCase = true).not()) {
                            return rexOpCaseBranch(condition, result)
                        }
                        val ref = call.args.getOrNull(0) ?: error("IS STRUCT requires an argument.")
                        // Replace the result's type
                        val type = unionOf(ref.type.allTypes.filterIsInstance<StructType>().toSet())
                        val replacementVal = ref.copy(type = type)
                        when (ref.op is Rex.Op.Var.Resolved) {
                            true -> RexReplacer.replace(result, ref, replacementVal)
                            false -> result
                        }
                    }
                    val type = rex.toUnionType().flatten()

                    return rexOpCaseBranch(condition, result.copy(type))
                }
                is Rex.Op.Call.Static -> {
                    val fn = call.fn as? Fn.Resolved ?: return rexOpCaseBranch(condition, result)
                    if (fn.signature.name.equals("is_struct", ignoreCase = true).not()) {
                        return rexOpCaseBranch(condition, result)
                    }
                    val ref = call.args.getOrNull(0) ?: error("IS STRUCT requires an argument.")
                    val simplifiedCondition = when {
                        ref.type.allTypes.all { it is StructType } -> rex(BOOL, rexOpLit(boolValue(true)))
                        ref.type.allTypes.none { it is StructType } -> rex(BOOL, rexOpLit(boolValue(false)))
                        else -> condition
                    }

                    // Replace the result's type
                    val type = unionOf(ref.type.allTypes.filterIsInstance<StructType>().toSet()).flatten()
                    val replacementVal = ref.copy(type = type)
                    val rex = when (ref.op is Rex.Op.Var.Resolved) {
                        true -> RexReplacer.replace(result, ref, replacementVal)
                        false -> result
                    }
                    return rexOpCaseBranch(simplifiedCondition, rex)
                }
            }
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType?): Rex {
            // Check Type
            if (ctx!! !is CollectionType) {
                handleUnexpectedType(ctx, setOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP))
                return rex(ANY, rexOpErr("Expected collection type"))
            }
            val values = node.values.map { visitRex(it, it.type) }
            val t = when (values.size) {
                0 -> ANY
                else -> values.toUnionType()
            }
            val type = when (ctx as CollectionType) {
                is BagType -> BagType(t)
                is ListType -> ListType(t)
                is SexpType -> SexpType(t)
            }
            return rex(type, rexOpCollection(values))
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType?): Rex {
            val fields = node.fields.map {
                val k = visitRex(it.k, it.k.type)
                val v = visitRex(it.v, it.v.type)
                rexOpStructField(k, v)
            }
            var structIsClosed = true
            val structTypeFields = mutableListOf<StructType.Field>()
            val structKeysSeent = mutableSetOf<String>()
            for (field in fields) {
                when (field.k.op) {
                    is Rex.Op.Lit -> {
                        // A field is only included in the StructType if its key is a text literal
                        val key = field.k.op
                        if (key.value is TextValue<*>) {
                            val name = key.value.string!!
                            val type = field.v.type
                            structKeysSeent.add(name)
                            structTypeFields.add(StructType.Field(name, type))
                        }
                    }
                    else -> {
                        if (field.k.type.allTypes.any { it.isText() }) {
                            // If the non-literal could be text, StructType will have open content.
                            structIsClosed = false
                        } else {
                            // A field with a non-literal key name is not included in the StructType.
                        }
                    }
                }
            }
            val type = StructType(
                fields = structTypeFields,
                contentClosed = structIsClosed,
                constraints = setOf(
                    TupleConstraint.Open(!structIsClosed),
                    TupleConstraint.UniqueAttrs(structKeysSeent.size == fields.size)
                ),
            )
            return rex(type, rexOpStruct(fields))
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: StaticType?): Rex {
            val rel = node.rel.type(locals)
            val typeEnv = TypeEnv(rel.type.schema)
            val key = node.key.type(typeEnv)
            val value = node.value.type(typeEnv)
            val type = StructType(
                contentClosed = false,
                constraints = setOf(TupleConstraint.Open(true))
            )
            val op = rexOpPivot(key, value, rel)
            return rex(type, op)
        }

        override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: StaticType?): Rex {
            val select = visitRexOpSelect(node.select, ctx).op as Rex.Op.Select
            val subquery = node.copy(select = select)
            return when (node.coercion) {
                Rex.Op.Subquery.Coercion.SCALAR -> visitRexOpSubqueryScalar(subquery, select.constructor.type)
                Rex.Op.Subquery.Coercion.ROW -> visitRexOpSubqueryRow(subquery, select.constructor.type)
            }
        }

        /**
         * Calculate output type of a row-value subquery.
         */
        private fun visitRexOpSubqueryRow(subquery: Rex.Op.Subquery, cons: StaticType): Rex {
            if (cons !is StructType) {
                return rexErr("Subquery with non-SQL SELECT cannot be coerced to a row-value expression. Found constructor type: $cons")
            }
            // Do a simple cardinality check for the moment.
            // TODO we can only check cardinality if we know we are in a a comparison operator.
            // val n = coercion.columns.size
            // val m = cons.fields.size
            // if (n != m) {
            //     return rexErr("Cannot coercion subquery with $m attributes to a row-value-expression with $n attributes")
            // }
            // If we made it this far, then we can coerce this subquery to the desired complex value
            val type = StaticType.LIST
            val op = subquery
            return rex(type, op)
        }

        /**
         * Calculate output type of a scalar subquery.
         */
        private fun visitRexOpSubqueryScalar(subquery: Rex.Op.Subquery, cons: StaticType): Rex {
            if (cons !is StructType) {
                return rexErr("Subquery with non-SQL SELECT cannot be coerced to a scalar. Found constructor type: $cons")
            }
            val n = cons.fields.size
            if (n != 1) {
                return rexErr("SELECT constructor with $n attributes cannot be coerced to a scalar. Found constructor type: $cons")
            }
            // If we made it this far, then we can coerce this subquery to a scalar
            val type = cons.fields.first().value
            val op = subquery
            return rex(type, op)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType?): Rex {
            val rel = node.rel.type(locals)
            val typeEnv = TypeEnv(rel.type.schema)
            var constructor = node.constructor.type(typeEnv)
            var constructorType = constructor.type
            // add the ordered property to the constructor
            if (constructorType is StructType) {
                // TODO: We shouldn't need to copy the ordered constraint.
                constructorType = constructorType.copy(
                    constraints = constructorType.constraints + setOf(TupleConstraint.Ordered)
                )
                constructor = rex(constructorType, constructor.op)
            }
            val type = when (rel.isOrdered()) {
                true -> ListType(constructor.type)
                else -> BagType(constructor.type)
            }
            return rex(type, rexOpSelect(constructor, rel))
        }

        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType?): Rex {
            val args = node.args.map { visitRex(it, ctx) }
            val type = when (args.size) {
                0 -> StructType(
                    fields = emptyMap(), contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                    )
                )
                else -> {
                    val argTypes = args.map { it.type }
                    val anyArgIsNotStruct = argTypes.any { argType -> !argType.mayBeType<StructType>() }
                    if (anyArgIsNotStruct) {
                        handleAlwaysMissing()
                    }
                    val potentialTypes = buildArgumentPermutations(argTypes).mapNotNull { argumentList ->
                        calculateTupleUnionOutputType(argumentList)
                    }
                    unionOf(potentialTypes.toSet()).flatten()
                }
            }
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        override fun visitRexOpErr(node: Rex.Op.Err, ctx: StaticType?): PlanNode {
            val type = ctx ?: ANY
            return rex(type, node)
        }

        // Helpers

        /**
         * Given a list of [args], this calculates the output type of `TUPLEUNION(args)`. NOTE: This does NOT handle union
         * types intentionally. This function expects that all arguments be flattened, and, if need be, that you invoke
         * this function multiple times based on the permutations of arguments.
         *
         * The signature of TUPLEUNION is: (LIST<STRUCT>) -> STRUCT.
         *
         * If any of the arguments are not a struct, we return null.
         *
         * Now, assuming all the other arguments are STRUCT, then we compute the output based on a number of factors:
         * - closed content
         * - ordering
         * - unique attributes
         *
         * If all arguments are closed content, then the output is closed content.
         * If all arguments are ordered, then the output is ordered.
         * If all arguments contain unique attributes AND all arguments are closed AND no fields clash, the output has
         *  unique attributes.
         */
        private fun calculateTupleUnionOutputType(args: List<StaticType>): StaticType? {
            val structFields = mutableListOf<StructType.Field>()
            var structAmount = 0
            var structIsClosed = true
            var structIsOrdered = true
            var uniqueAttrs = true
            args.forEach { arg ->
                when (arg) {
                    is StructType -> {
                        structAmount += 1
                        structFields.addAll(arg.fields)
                        structIsClosed = structIsClosed && arg.constraints.contains(TupleConstraint.Open(false))
                        structIsOrdered = structIsOrdered && arg.constraints.contains(TupleConstraint.Ordered)
                        uniqueAttrs = uniqueAttrs && arg.constraints.contains(TupleConstraint.UniqueAttrs(true))
                    }
                    is AnyOfType -> {
                        onProblem.invoke(
                            Problem(
                                UNKNOWN_PROBLEM_LOCATION,
                                PlanningProblemDetails.CompileError("TupleUnion wasn't normalized to exclude union types.")
                            )
                        )
                    }
                    else -> {
                        return null
                    }
                }
            }
            uniqueAttrs = when {
                structIsClosed.not() && structAmount > 1 -> false
                else -> uniqueAttrs
            }
            uniqueAttrs = uniqueAttrs && (structFields.size == structFields.distinctBy { it.key }.size)
            val orderedConstraint = when (structIsOrdered) {
                true -> TupleConstraint.Ordered
                false -> null
            }
            val constraints = setOfNotNull(
                TupleConstraint.Open(!structIsClosed), TupleConstraint.UniqueAttrs(uniqueAttrs), orderedConstraint
            )
            return StructType(
                fields = structFields.map { it }, contentClosed = structIsClosed, constraints = constraints
            )
        }

        /**
         * We are essentially making permutations of arguments that maintain the same initial ordering. For example,
         * consider the following args:
         * ```
         * [ 0 = UNION(INT, STRING), 1 = (DECIMAL, TIMESTAMP) ]
         * ```
         * This function will return:
         * ```
         * [
         *   [ 0 = INT, 1 = DECIMAL ],
         *   [ 0 = INT, 1 = TIMESTAMP ],
         *   [ 0 = STRING, 1 = DECIMAL ],
         *   [ 0 = STRING, 1 = TIMESTAMP ]
         * ]
         * ```
         *
         * Essentially, this becomes useful specifically in the case of TUPLEUNION, since we can make sure that
         * the ordering of argument's attributes remains the same. For example:
         * ```
         * TUPLEUNION( UNION(STRUCT(a, b), STRUCT(c)), UNION(STRUCT(d, e), STRUCT(f)) )
         * ```
         *
         * Then, the output of the tupleunion will have the output types of all of the below:
         * ```
         * TUPLEUNION(STRUCT(a,b), STRUCT(d,e)) --> STRUCT(a, b, d, e)
         * TUPLEUNION(STRUCT(a,b), STRUCT(f)) --> STRUCT(a, b, f)
         * TUPLEUNION(STRUCT(c), STRUCT(d,e)) --> STRUCT(c, d, e)
         * TUPLEUNION(STRUCT(c), STRUCT(f)) --> STRUCT(c, f)
         * ```
         */
        private fun buildArgumentPermutations(args: List<StaticType>): Sequence<List<StaticType>> {
            val flattenedArgs = args.map { it.flatten().allTypes }
            return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
        }

        private fun buildArgumentPermutations(
            args: List<List<StaticType>>,
            accumulator: List<StaticType>,
        ): Sequence<List<StaticType>> {
            if (args.isEmpty()) {
                return sequenceOf(accumulator)
            }
            val first = args.first()
            val rest = when (args.size) {
                1 -> emptyList()
                else -> args.subList(1, args.size)
            }
            return sequence {
                first.forEach { argSubType ->
                    yieldAll(buildArgumentPermutations(rest, accumulator + listOf(argSubType)))
                }
            }
        }

        // Helpers

        /**
         * Logic is as follows:
         * 1. If [struct] is closed and ordered:
         *   - If no item is found, return null
         *   - Else, grab first matching item and make sensitive.
         * 2. If [struct] is closed
         *   - AND no item is found, return null
         *   - AND only one item is present -> grab item and make sensitive.
         *   - AND more than one item is present, keep sensitivity and grab item.
         * 3. If [struct] is open, return [AnyType]
         *
         * @return a [Pair] where the [Pair.first] represents the type of the [step] and the [Pair.second] represents
         * the disambiguated [key].
         */
        private fun inferStructLookup(struct: StructType, key: Identifier.Symbol): Pair<StaticType, Identifier.Symbol>? {
            val binding = key.toBindingName()
            val isClosed = struct.constraints.contains(TupleConstraint.Open(false))
            val isOrdered = struct.constraints.contains(TupleConstraint.Ordered)
            val (name, type) = when {
                // 1. Struct is closed and ordered
                isClosed && isOrdered -> {
                    struct.fields.firstOrNull { entry -> binding.isEquivalentTo(entry.key) }?.let {
                        (sensitive(it.key) to it.value)
                    } ?: return null
                }
                // 2. Struct is closed
                isClosed -> {
                    val matches = struct.fields.filter { entry -> binding.isEquivalentTo(entry.key) }
                    when (matches.size) {
                        0 -> {
                            return null
                        }
                        1 -> matches.first().let { (sensitive(it.key) to it.value) }
                        else -> {
                            val firstKey = matches.first().key
                            val sharedKey = when (matches.all { it.key == firstKey }) {
                                true -> sensitive(firstKey)
                                false -> key
                            }
                            sharedKey to unionOf(matches.map { it.value }.toSet()).flatten()
                        }
                    }
                }
                // 3. Struct is open
                else -> key to ANY
            }
            return type to name
        }

        private fun sensitive(str: String): Identifier.Symbol =
            identifierSymbol(str, Identifier.CaseSensitivity.SENSITIVE)

        /**
         * Resolution and typing of aggregation function calls.
         *
         * I've chosen to place this in RexTyper because all arguments will be typed using the same locals.
         * There's no need to create new RexTyper instances for each argument. There is no reason to limit aggregations
         * to a single argument (covar, corr, pct, etc.) but in practice we typically only have single <value expression>.
         *
         * This method is _very_ similar to scalar function resolution, so it is temping to DRY these two out; but the
         * separation is cleaner as the typing of NULLS is subtly different.
         *
         * SQL-99 6.16 General Rules on <set function specification>
         *     Let TX be the single-column table that is the result of applying the <value expression>
         *     to each row of T and eliminating null values <--- all NULL values are eliminated as inputs
         */
        fun resolveAgg(agg: Agg.Unresolved, arguments: List<Rex>): Pair<Rel.Op.Aggregate.Call, StaticType> {
            val args = arguments.map {
                val arg = visitRex(it, it.type)
                arg
            }

            // Try to match the arguments to functions defined in the catalog
            return when (val match = env.resolveAgg(agg, args)) {
                is FnMatch.Ok -> {
                    // Found a match!
                    val newAgg = aggResolved(match.signature)
                    val newArgs = rewriteFnArgs(match.mapping, args)
                    val returns = newAgg.signature.returns

                    // Return type with calculated nullability
                    val type = when {
                        newAgg.signature.isNullable -> returns.toStaticType()
                        else -> returns.toStaticType()
                    }

                    // Finally, rewrite this node
                    relOpAggregateCall(newAgg, newArgs) to type
                }
                is FnMatch.Dynamic -> TODO("Dynamic aggregates not yet supported.")
                is FnMatch.Error -> {
                    handleUnknownFunction(match)
                    return relOpAggregateCall(agg, listOf(rexErr("MISSING"))) to ANY
                }
            }
        }
    }

    // HELPERS

    private fun Rel.type(locals: TypeEnv, strategy: ResolutionStrategy = ResolutionStrategy.LOCAL): Rel =
        RelTyper(locals, strategy).visitRel(this, null)

    private fun Rex.type(locals: TypeEnv, strategy: ResolutionStrategy = ResolutionStrategy.LOCAL) =
        RexTyper(locals, strategy).visitRex(this, this.type)

    private fun rexErr(message: String) = rex(ANY, rexOpErr(message))

    /**
     * I found decorating the tree with the binding names (for resolution) was easier than associating introduced
     * bindings with a node via an id->list<string> map. ONLY because right now I don't think we have a good way
     * of managing ids when trees are rewritten.
     *
     * We need a good answer for these questions before going for it:
     * - If you copy, should the id should come along for the ride?
     * - If someone writes their own pass and forgets to copy the id, then resolution could break.
     *
     * We may be able to eliminate this issue by keeping everything internal and running the typing pass first.
     * This is simple enough for now.
     */
    private fun Rel.Type.copyWithSchema(types: List<StaticType>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) })
    }

    private fun Identifier.toBindingPath() = when (this) {
        is Identifier.Qualified -> this.toBindingPath()
        is Identifier.Symbol -> BindingPath(listOf(this.toBindingName()))
    }

    private fun Identifier.Qualified.toBindingPath() =
        BindingPath(steps = listOf(this.root.toBindingName()) + steps.map { it.toBindingName() })

    private fun Identifier.Symbol.toBindingName() = BindingName(
        name = symbol,
        bindingCase = when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> BindingCase.SENSITIVE
            Identifier.CaseSensitivity.INSENSITIVE -> BindingCase.INSENSITIVE
        }
    )

    private fun Rel.isOrdered(): Boolean = type.props.contains(Rel.Prop.ORDERED)

    /**
     * Produce a union type from all the
     */
    private fun List<Rex>.toUnionType(): StaticType = unionOf(map { it.type }.toSet()).flatten()

    private fun getElementTypeForFromSource(fromSourceType: StaticType): StaticType = when (fromSourceType) {
        is BagType -> fromSourceType.elementType
        is ListType -> fromSourceType.elementType
        is AnyType -> ANY
        is AnyOfType -> unionOf(fromSourceType.types.map { getElementTypeForFromSource(it) }.toSet())
        // All the other types coerce into a bag of themselves (including null/missing/sexp).
        else -> fromSourceType
    }

    /**
     * Rewrites function arguments, wrapping in the given function if exists.
     */
    private fun rewriteFnArgs(mapping: List<FunctionSignature.Scalar?>, args: List<Rex>): List<Rex> {
        if (mapping.size != args.size) {
            error("Fatal, malformed function mapping") // should be unreachable given how a mapping is generated.
        }
        val newArgs = mutableListOf<Rex>()
        for (i in mapping.indices) {
            var a = args[i]
            val m = mapping[i]
            if (m != null) {
                // rewrite
                val type = m.returns.toStaticType()
                val cast = rexOpCallStatic(fnResolved(m), listOf(a))
                a = rex(type, cast)
            }
            newArgs.add(a)
        }
        return newArgs
    }

    private fun assertAsInt(type: StaticType) {
        if (type.flatten().allTypes.any { variant -> variant is IntType }.not()) {
            handleUnexpectedType(type, setOf(StaticType.INT))
        }
    }

    // ERRORS

    /**
     * Invokes [onProblem] with a newly created [PlanningProblemDetails.UndefinedVariable] and returns the
     * [PlanningProblemDetails.UndefinedVariable].
     */
    private fun handleUndefinedVariable(name: Identifier, locals: Set<String>): PlanningProblemDetails.UndefinedVariable {
        val planName = PlanTransform.visitIdentifier(name, onProblem)
        val details = PlanningProblemDetails.UndefinedVariable(planName, locals)
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = details
            )
        )
        return details
    }

    private fun handleUnexpectedType(actual: StaticType, expected: Set<StaticType>) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnexpectedType(actual, expected),
            )
        )
    }

    private fun handleUnknownFunction(match: FnMatch.Error<*>) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnknownFunction(
                    match.identifier.normalize(),
                    match.args.map { a -> a.type },
                )
            )
        )
    }

    private fun handleAlwaysMissing() {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.ExpressionAlwaysReturnsNullOrMissing
            )
        )
    }

    private fun handleUnresolvedExcludeRoot(root: Identifier) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnresolvedExcludeExprRoot(
                    when (root) {
                        is Identifier.Symbol -> root.symbol
                        is Identifier.Qualified -> root.toString()
                    }
                )
            )
        )
    }

    // HELPERS

    private fun Identifier.normalize(): String = when (this) {
        is Identifier.Qualified -> (listOf(root.normalize()) + steps.map { it.normalize() }).joinToString(".")
        is Identifier.Symbol -> when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> symbol
            Identifier.CaseSensitivity.INSENSITIVE -> symbol.lowercase()
        }
    }

    private fun excludeBindings(input: List<Rel.Binding>, item: Rel.Op.Exclude.Item): List<Rel.Binding> {
        var matchedRoot = false
        val output = input.map {
            when (val root = item.root) {
                is Rex.Op.Var.Unresolved -> {
                    when (val id = root.identifier) {
                        is Identifier.Symbol -> {
                            if (id.isEquivalentTo(it.name)) {
                                matchedRoot = true
                                // recompute the StaticType of this binding after apply the exclusions
                                val type = it.type.exclude(item.steps, false)
                                it.copy(type = type)
                            } else {
                                it
                            }
                        }
                        is Identifier.Qualified -> it
                    }
                }
                is Rex.Op.Var.Resolved -> it
            }
        }
        if (!matchedRoot && item.root is Rex.Op.Var.Unresolved) handleUnresolvedExcludeRoot(item.root.identifier)
        return output
    }

    private fun Identifier.Symbol.isEquivalentTo(other: String): Boolean = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> symbol.equals(other)
        Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other, ignoreCase = true)
    }
}
