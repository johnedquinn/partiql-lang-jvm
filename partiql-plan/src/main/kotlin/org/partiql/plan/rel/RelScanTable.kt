package org.partiql.plan.rel

import org.partiql.plan.Visitor
import org.partiql.plan.rex.RexTable

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v>`.
 */
public interface RelScanTable : Rel {

    /**
     * Returns the columns of the table to be scanned.
     */
    public fun getColumns(): List<Int>

    public fun getTable(): RexTable

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitScanTable(this, ctx)
}

/**
 * Default [RelScan] implementation.
 */
internal class RelScanTableImpl(input: RexTable, columns: List<Int>) : RelScanTable {

    // DO NOT USE FINAL
    private var _input: RexTable = input
    private var _columns: List<Int> = columns

    override fun getColumns(): List<Int> {
        return _columns
    }

    override fun getTable(): RexTable = _input

    override fun getType(): RelType {
        TODO("Implement getSchema for scan")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelScan) return false
        return _input == other.getInput()
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }

    override fun debugString(): MutableMap<String, Any> {
        return mutableMapOf(
            "name" to this::class.java.name,
            "input" to _input.debugString(),
            "columns" to _columns
        )
    }

    override fun toString(): String {
        return  "RelScanImpl(_input=$_input)"
    }
}
