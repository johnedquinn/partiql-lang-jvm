package org.partiql.runner.util

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Different methods for asserting value equality. The legacy comparator needed to compared on lowered IonValue but
 * we now have PartiQLValue which defines its own `equals` methods.
 *
 * @param T
 */
interface ValueEquals<T> {

    fun equals(left: T, right: T): Boolean

    companion object {

        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        val partiql: ValueEquals<PartiQLValue> = PartiQLValueEquals
    }
}

/**
 * Value equality using the [PartiQLValue] equality implementation.
 */
@OptIn(PartiQLValueExperimental::class)
private object PartiQLValueEquals : ValueEquals<PartiQLValue> {
    override fun equals(left: PartiQLValue, right: PartiQLValue) = left.equals(right)
}
