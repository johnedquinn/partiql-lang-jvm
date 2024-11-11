package org.partiql.plan

import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexType

/**
 * TODO DOCUMENTATION
 */
public interface Operation : PlanNode {

    /**
     * PartiQL Query Statement — i.e. SELECT-FROM
     */
    public interface Query : Operation {

        /**
         * Returns the root expression of the query.
         */
        public fun getRex(): Rex

        /**
         * Returns the type of the root expression of the query.
         */
        public fun getType(): RexType = getRex().getType()

        override fun <R : Any?, C : Any?> accept(visitor: Visitor<R, C>, ctx: C): R {
            return visitor.visitQuery(this, ctx)
        }
    }
}

internal class QueryImpl(private val rex: Rex) : Operation.Query {
    override fun getRex(): Rex = rex

    override fun debugString(): MutableMap<String, Any> {
        return mutableMapOf(
            "name" to this::class.java.name,
            "rex" to rex.debugString(),
        )
    }

    override fun toString(): String {
        return "Query(rex=$rex)"
    }
}
