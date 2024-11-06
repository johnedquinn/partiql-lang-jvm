package org.partiql.plan.rel

import org.partiql.plan.Visitor
import org.partiql.plan.rex.Rex

/**
 * Logical `PROJECTION` operator
 */
public interface RelProject : Rel {

    public fun getInput(): Rel

    public fun getProjections(): List<Rex>

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitProject(this, ctx)
}

/**
 * Default [RelProject] implementation.
 */
public class RelProjectImpl(input: Rel, projections: List<Rex>) : RelProject {

    // DO NOT USE FINAL
    private var _input = input
    private var _projections = projections

    override fun getInput(): Rel = _input

    override fun getProjections(): List<Rex> = _projections

    override fun getType(): RelType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rel> = listOf(_input)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelProject) return false
        if (_input != other.getInput()) return false
        if (_projections != other.getProjections()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _projections.hashCode()
        return result
    }

    override fun debugString(): MutableMap<String, Any> {
        return mutableMapOf(
            "name" to this::class.java.name,
            "input" to _input.debugString(),
            "projections" to _projections.map { it.debugString() },
            // TODO "type" to getType()
        )
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("RelProjectImpl(")
        sb.append("input=")
        sb.append(_input)
        sb.append(", projections=")
        sb.append(_projections)
        sb.append(")")
        return sb.toString()
    }
}
