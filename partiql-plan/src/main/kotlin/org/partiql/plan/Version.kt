package org.partiql.plan

/**
 * Marker interface for some version structure.
 */
public interface Version {

    /**
     * The only required method is toString.
     */
    override fun toString(): String
}

internal class VersionImpl(private val version: String) : Version {
    override fun toString(): String = version
}
