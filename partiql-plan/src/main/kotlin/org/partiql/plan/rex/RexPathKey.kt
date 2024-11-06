package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * Logical operator for path lookup by key.
 */
public interface RexPathKey : Rex {

    public fun getOperand(): Rex

    public fun getKey(): Rex

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitPathKey(this, ctx)
}

/**
 * Standard internal implementation for [RexPathKey].
 */
internal class RexPathKeyImpl(operand: Rex, key: Rex, type: RexType) : RexPathKey {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _key = key
    private var _type = type

    override fun getOperand() = _operand

    override fun getKey() = _key

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = listOf(_operand, _key)

    override fun debugString(): MutableMap<String, Any> {
        return mutableMapOf(
            "name" to this::class.java.name,
            "operand" to _operand.debugString(),
            "key" to _key.debugString(),
            "type" to _type.toString()
        )
    }

    override fun toString(): String {
        return "RexPathKey(operand=$_operand, key=$_key, type=$_type)"
    }
}
