package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.spi.function.Function

/**
 * Logical operator for a scalar function call.
 */
public interface RexCall : Rex {

    /**
     * Returns the function to invoke.
     */
    public fun getFunction(): Function.Instance

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)
}

/**
 * Default [RexCall] implementation meant for extension.
 */
internal class RexCallImpl(function: Function.Instance, args: List<Rex>) : RexCall {

    // DO NOT USE FINAL
    private var _function: Function.Instance = function
    private var _args: List<Rex> = args
    private var _type: RexType = RexType(function.returns)

    override fun getFunction(): Function.Instance = _function

    override fun getArgs(): List<Rex> = _args

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = _args

    override fun debugString(): MutableMap<String, Any> {
        return mutableMapOf(
            "name" to this::class.java.name,
            "function" to _function,
            "args" to _args.map { it.debugString() },
            "type" to _type.toString()
        )
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("RexCall(")
        sb.append("instance=")
        sb.append(_function.toString())
        sb.append(", args=")
        sb.append('(')
        _args.forEachIndexed { i, arg ->
            sb.append(arg)
            if (i < _args.size - 1) {
                sb.append(", ")
            }
        }
        sb.append(')')
        sb.append(')')
        return sb.toString()
        return super.toString()
    }
}
