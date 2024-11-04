package org.partiql.plan

/**
 * Operator is the interface for a logical plan operator.
 */
public interface Operator : PlanNode {

    public fun getChildren(): Collection<Operator>
}
