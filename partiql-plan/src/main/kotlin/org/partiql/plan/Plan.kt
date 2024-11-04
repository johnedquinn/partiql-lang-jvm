package org.partiql.plan

/**
 * A [Plan] holds operations that can be executed.
 */
public interface Plan : PlanNode {

    /**
     * The plan version for serialization and debugging.
     *
     * @return
     */
    public fun getVersion(): Version = object : Version {
        override fun toString(): String = "1"
    }

    /**
     * The plan operation to execute.
     *
     * TODO consider `getOperations(): List<Operation>`.
     *
     * @return
     */
    public fun getOperation(): Operation

    override fun <R : Any?, C : Any?> accept(visitor: Visitor<R, C>, ctx: C): R {
        return visitor.visitPlan(this, ctx)
    }
}
