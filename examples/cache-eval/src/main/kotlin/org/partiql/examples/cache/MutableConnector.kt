package org.partiql.examples.cache

import org.partiql.spi.BindingCase
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlConnector
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * The [MutableConnector] aims to allow you to swap out any globals/bindings during/prior the execution of a
 * prepared statement. While not exactly how others should do this, we are maintaining a global [bindings] to allow
 * the mutability of underlying bindings. You can call [Bindings.define] to define new bindings, and you can invoke
 * [Bindings.clear] to clear all globals.
 *
 * TODO: Ideally, there wouldn't be some global bindings. [getBindings] should probably take in a [ConnectorSession]
 *  so that implementers can create some map between the session and the ConnectorMetadata and ConnectorBindings. The
 *  Connector is not really supposed to contain state specific to a user. It is supposed to be used to create stateful
 *  classes. However, for the purpose of this experiment, we'll just use a global.
 */
class MutableConnector : SqlConnector() {

    val bindings: Bindings = Bindings()

    class Bindings : ConnectorBindings {
        private val bindings = mutableMapOf<String, Result>()
        private val lowerCaseBindings = mutableMapOf<String, String>()

        class Result @OptIn(PartiQLValueExperimental::class) constructor(
            val realName: String,
            val type: StaticType,
            val pValue: PartiQLValue?
        )

        fun clear() {
            this.bindings.clear()
            this.lowerCaseBindings.clear()
        }

        @OptIn(PartiQLValueExperimental::class)
        fun define(name: String, type: StaticType = StaticType.ANY, value: PartiQLValue? = null) {
            this.bindings[name] = Result(name, type, value)
            this.lowerCaseBindings[name.lowercase()] = name
        }

        operator fun get(name: String): Result? {
            return this.bindings[name]
        }

        fun getRealName(name: String, caseSensitive: Boolean = true): String? {
            return when (caseSensitive) {
                true -> this.bindings[name]?.realName
                false -> this.lowerCaseBindings[name]
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun getValue(path: ConnectorPath): PartiQLValue {
            assert(path.steps.size == 1)
            val name = path.steps.first()
            return this[name]!!.pValue!!
        }
    }

    override fun getMetadata(session: ConnectorSession): SqlMetadata {
        return Metadata(session, info, this)
    }

    override fun getBindings(): ConnectorBindings {
        return this.bindings
    }

    class Metadata(
        session: ConnectorSession,
        info: InfoSchema,
        private val connector: MutableConnector
    ) : SqlMetadata(session, info) {

        override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
            // We assume that this connector doesn't support schemas.
            if (path.steps.size != 1) {
                return null
            }
            val requested = path.steps.first()
            val caseSensitive = when (requested.case) {
                BindingCase.SENSITIVE -> true
                BindingCase.INSENSITIVE -> false
            }
            val realName = this.connector.bindings.getRealName(requested.name, caseSensitive) ?: return null
            val type = this.connector.bindings[realName]!!.type
            return ConnectorHandle.Obj(
                ConnectorPath(listOf(realName)),
                entity = object : ConnectorObject {
                    override fun getType(): StaticType = type
                }
            )
        }
    }
}
