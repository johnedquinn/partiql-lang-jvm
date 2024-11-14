/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package eval

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Result of these benchmarks:
 * Benchmark                               Mode  Cnt  Score    Error  Units
 * PartiQLBenchmark.minusIntDynamicStatic  avgt   20  0.150 ±  0.005  us/op
 * PartiQLBenchmark.minusIntStatic         avgt   20  0.010 ±  0.001  us/op
 * PartiQLBenchmark.plusIntDynamicStatic   avgt   20  0.081 ±  0.005  us/op
 * PartiQLBenchmark.plusIntStatic          avgt   20  0.010 ±  0.001  us/op
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class PartiQLBenchmark {

    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @State(Scope.Thread)
    open class MyState {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val compiler = PartiQLCompiler.standard()

        // SESSION
        val session = Session.builder().catalog("mem").catalogs(
            Catalog.builder().name("mem").define(
                Table.Companion.standard(Name.of("a"), PType.dynamic(), Datum.integer(430)),
            ).define(
                Table.Companion.standard(Name.of("b"), PType.dynamic(), Datum.decimal(BigDecimal.valueOf(123456789, 3))),
            ).build()
        ).build()

        val plusIntQuery = "123 + 456"
        val plusIntAst = parser.parse(plusIntQuery).statements.first()
        val plusIntPlan = planner.plan(plusIntAst, Session.empty())
        val plusIntExecutable = compiler.prepare(plusIntPlan.plan, org.partiql.eval.Mode.STRICT())

        val minusIntQuery = "123 - 456"
        val minusIntAst = parser.parse(minusIntQuery).statements.first()
        val minusIntPlan = planner.plan(minusIntAst, Session.empty())
        val minusIntExecutable = compiler.prepare(minusIntPlan.plan, org.partiql.eval.Mode.STRICT())

        val plusIntDynamicQuery = "a + b"
        val plusIntDynamicAst = parser.parse(plusIntDynamicQuery).statements.first()
        val plusIntDynamicPlan = planner.plan(plusIntDynamicAst, session)
        val plusIntDynamicExecutable = compiler.prepare(plusIntDynamicPlan.plan, org.partiql.eval.Mode.STRICT())

        val minusIntDynamicQuery = "a - b"
        val minusIntDynamicAst = parser.parse(minusIntDynamicQuery).statements.first()
        val minusIntDynamicPlan = planner.plan(minusIntDynamicAst, session)
        val minusIntDynamicExecutable = compiler.prepare(minusIntDynamicPlan.plan, org.partiql.eval.Mode.STRICT())
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun plusIntStatic(state: MyState, blackhole: Blackhole) {
        blackhole.consume(state.plusIntExecutable.execute())
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun minusIntStatic(state: MyState, blackhole: Blackhole) {
        blackhole.consume(state.minusIntExecutable.execute())
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun plusIntDynamicStatic(state: MyState, blackhole: Blackhole) {
        blackhole.consume(state.plusIntDynamicExecutable.execute())
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun minusIntDynamicStatic(state: MyState, blackhole: Blackhole) {
        blackhole.consume(state.minusIntDynamicExecutable.execute())
    }
}
