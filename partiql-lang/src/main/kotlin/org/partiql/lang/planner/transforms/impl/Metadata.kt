/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner.transforms.impl

import com.amazon.ionelement.api.StructElement
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.time.Time
import org.partiql.lang.planner.transforms.ObjectHandle
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.Plugin
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.Constants
import org.partiql.types.PartiQLValueType
import org.partiql.types.StaticType
import org.partiql.value.BagValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.CharValue
import org.partiql.value.ClobValue
import org.partiql.value.DateValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.NullableBagValue
import org.partiql.value.NullableBlobValue
import org.partiql.value.NullableBoolValue
import org.partiql.value.NullableCharValue
import org.partiql.value.NullableClobValue
import org.partiql.value.NullableDateValue
import org.partiql.value.NullableInt16Value
import org.partiql.value.NullableInt32Value
import org.partiql.value.NullableInt8Value
import org.partiql.value.NullableListValue
import org.partiql.value.NullableSexpValue
import org.partiql.value.NullableStringValue
import org.partiql.value.NullableSymbolValue
import org.partiql.value.NullableTimeValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue

/**
 * Acts to consolidate multiple [org.partiql.spi.connector.ConnectorMetadata]'s.
 */
internal class Metadata(
    private val plugins: List<Plugin>,
    private val catalogMap: Map<String, StructElement>
) {

    private val connectorFactories = plugins.flatMap { it.getConnectorFactories() }
    private val connectorMap = catalogMap.toList().associate {
        val (catalogName, catalogConfig) = it
        catalogName to connectorFactories.first { factory ->
            val connectorName = catalogConfig[Constants.CONFIG_KEY_CONNECTOR_NAME].stringValue
            factory.getName() == connectorName
        }.create(catalogName, catalogConfig)
    }

    public fun getObjectHandle(session: PlannerSession, catalog: BindingName, path: BindingPath): ObjectHandle? {
        val connectorSession = session.toConnectorSession()
        val metadataInfo = getMetadata(session.toConnectorSession(), catalog) ?: return null
        return metadataInfo.metadata.getObjectHandle(connectorSession, path)?.let {
            ObjectHandle(
                connectorHandle = it,
                catalogName = metadataInfo.catalogName
            )
        }
    }

    public fun catalogExists(session: ConnectorSession, catalog: BindingName): Boolean {
        catalogMap.keys.firstOrNull { catalog.isEquivalentTo(it) } ?: return false
        return true
    }

    public fun getObjectHandle(session: ConnectorSession, catalog: BindingName, path: BindingPath): ObjectHandle? {
        val metadataInfo = getMetadata(session, catalog) ?: return null
        return metadataInfo.metadata.getObjectHandle(session, path)?.let {
            ObjectHandle(
                connectorHandle = it,
                catalogName = metadataInfo.catalogName
            )
        }
    }

    public fun listCatalogs(): List<String> {
        return catalogMap.keys.toList()
    }

    public fun listSchemas(session: ConnectorSession, catalog: BindingName): List<String> {
        val metadataInfo = getMetadata(session, catalog) ?: return emptyList()
        return metadataInfo.metadata.listSchemas(session)
    }

    public fun listTables(session: ConnectorSession, catalog: BindingName, path: BindingPath): List<String> {
        val metadataInfo = getMetadata(session, catalog) ?: return emptyList()
        return metadataInfo.metadata.listTables(session, path)
    }

    public fun listValues(session: ConnectorSession, catalog: BindingName, path: BindingPath): List<String> {
        val metadataInfo = getMetadata(session, catalog) ?: return emptyList()
        return metadataInfo.metadata.listValues(session, path)
    }

    public fun getObjectDescriptor(session: PlannerSession, handle: ObjectHandle): StaticType {
        val connectorSession = session.toConnectorSession()
        val metadata = getMetadata(session.toConnectorSession(), BindingName(handle.catalogName, BindingCase.SENSITIVE))!!.metadata
        return metadata.getObjectType(connectorSession, handle.connectorHandle)!!
    }

    // TODO: COW Hack
    // TODO: Exception hndling
    @OptIn(PartiQLValueExperimental::class)
    public fun getValue(session: ConnectorSession, handle: ObjectHandle): ExprValue {
        val metadata = getMetadata(session, BindingName(handle.catalogName, BindingCase.SENSITIVE))!!.metadata
        val partiqlValue = metadata.getValue(session, handle.connectorHandle)
        return partiqlValue.toExprValue()
    }

    // TODO: COW Hack
    // TODO: Exception hndling
    @OptIn(PartiQLValueExperimental::class)
    public fun createValue(session: ConnectorSession, catalog: BindingName, path: BindingPath, value: PartiQLValue): Boolean {
        val metadataInfo = getMetadata(session, catalog) ?: return false
        metadataInfo.metadata.createValue(session, path, value)
        return true
    }

    private fun getMetadata(connectorSession: ConnectorSession, catalogName: BindingName): MetadataInformation? {
        val catalogKey = catalogMap.keys.firstOrNull { catalogName.isEquivalentTo(it) } ?: return null
        val connector = connectorMap[catalogKey] ?: return null
        return MetadataInformation(catalogKey, connector.getMetadata(session = connectorSession))
    }
    
    private fun EvaluationSession.toConnectorSession(): ConnectorSession {
        TODO()
    }

    private class MetadataInformation(
        internal val catalogName: String,
        internal val metadata: ConnectorMetadata
    )

    internal class PartiQLtoExprValueTypeMismatchException(expectedType: String, actualType: PartiQLValueType) :
        Exception("When converting PartiQLValue to ExprValue, expected a $expectedType, but received $actualType")

    @Throws(PartiQLtoExprValueTypeMismatchException::class)
    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.toExprValue(): ExprValue {
        return when (this.type) {
            PartiQLValueType.BOOL -> (this as? BoolValue)?.value?.let { ExprValue.newBoolean(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("BoolValue", this.type)

            PartiQLValueType.INT8 -> (this as? Int8Value)?.int?.let { ExprValue.newInt(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("INT8", this.type)

            PartiQLValueType.INT16 -> (this as? Int16Value)?.int?.let { ExprValue.newInt(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("INT16", this.type)

            PartiQLValueType.INT32 -> (this as? Int32Value)?.int?.let { ExprValue.newInt(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("INT32", this.type)

            PartiQLValueType.INT64 -> (this as? Int64Value)?.long?.let { ExprValue.newInt(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("INT64", this.type)

            PartiQLValueType.INT -> (this as? IntValue)?.long?.let { ExprValue.newInt(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("INT", this.type)

            PartiQLValueType.DECIMAL -> (this as? DecimalValue)?.value?.let { ExprValue.newDecimal(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("DECIMAL", this.type)

            PartiQLValueType.FLOAT32 -> (this as? Float32Value)?.double?.let { ExprValue.newFloat(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("FLOAT32", this.type)

            PartiQLValueType.FLOAT64 -> (this as? Float64Value)?.double?.let { ExprValue.newFloat(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("FLOAT64", this.type)

            PartiQLValueType.CHAR -> (this as? CharValue)?.string?.let { ExprValue.newString(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("CHAR", this.type)

            PartiQLValueType.STRING -> (this as? StringValue)?.string?.let { ExprValue.newString(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("STRING", this.type)

            PartiQLValueType.SYMBOL -> (this as? SymbolValue)?.string?.let { ExprValue.newSymbol(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("SYMBOL", this.type)

            PartiQLValueType.BINARY -> TODO()

            PartiQLValueType.BYTE -> TODO()

            PartiQLValueType.BLOB -> (this as? BlobValue)?.value?.let { ExprValue.newBlob(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("BLOB", this.type)

            PartiQLValueType.CLOB -> (this as? ClobValue)?.value?.let { ExprValue.newClob(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("CLOB", this.type)

            PartiQLValueType.DATE -> (this as? DateValue)?.value?.let { ExprValue.newDate(it) }
                ?: throw PartiQLtoExprValueTypeMismatchException("DATE", this.type)

            PartiQLValueType.TIME -> {
                val timeValue = this as? TimeValue
                timeValue?.let { tv ->
                    val value = tv.value
                    val precision = tv.precision
                    val offset = tv.offset
                    val withzone = tv.withZone
                    if (withzone) {
                        offset?.let {
                            ExprValue.newTime(Time.of(value, precision, it))
                        }
                    } else {
                        ExprValue.newTime(Time.of(value, precision, null))
                    }
                } ?: throw PartiQLtoExprValueTypeMismatchException("TIME", this.type)
            }

            PartiQLValueType.TIMESTAMP -> TODO()
            // TODO: Implement
            //            {
            //                val timestampValue = partiqlValue as? TimestampValue
            //                timestampValue?.let { tv ->
            //                    val localDateTime = tv.value
            //                    val zoneOffset = tv.offset
            //                    val instant = localDateTime.atOffset(zoneOffset ?: ZoneOffset.UTC).toInstant()
            //                    val timestamp = Timestamp.forMillis(instant.toEpochMilli(), (zoneOffset?.totalSeconds ?: 0) / 60)
            //                    newTimestamp(timestamp)
            //                } ?: ExprValue.nullValue
            //            }

            PartiQLValueType.INTERVAL -> TODO()

            PartiQLValueType.BAG -> {
                (this as? BagValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newBag(it.asSequence()) }
                    ?: throw PartiQLtoExprValueTypeMismatchException("BAG", this.type)
            }

            PartiQLValueType.LIST -> {
                (this as? ListValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newList(it.asSequence()) }
                    ?: throw PartiQLtoExprValueTypeMismatchException("LIST", this.type)
            }

            PartiQLValueType.SEXP -> {
                (this as? SexpValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newSexp(it.asSequence()) }
                    ?: throw PartiQLtoExprValueTypeMismatchException("SEXP", this.type)
            }

            PartiQLValueType.STRUCT -> {
                (this as? StructValue<*>)?.fields?.map { it.second.toExprValue().namedValue(
                    ExprValue.newString(
                        it.first
                    )
                ) }
                    ?.let { ExprValue.newStruct(it.asSequence(), StructOrdering.ORDERED) }
                    ?: throw PartiQLtoExprValueTypeMismatchException("STRUCT", this.type)
            }

            PartiQLValueType.NULL -> ExprValue.nullValue

            PartiQLValueType.MISSING -> ExprValue.missingValue

            PartiQLValueType.NULLABLE_BOOL -> {
                (this as? NullableBoolValue)?.value?.let { ExprValue.newBoolean(it) }
                    ?: ExprValue.nullValue
            }

            PartiQLValueType.NULLABLE_INT8 -> (this as? NullableInt8Value)?.int?.let { ExprValue.newInt(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_INT16 -> (this as? NullableInt16Value)?.int?.let { ExprValue.newInt(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_INT32 -> (this as? NullableInt32Value)?.int?.let { ExprValue.newInt(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_INT64 -> (this as? Int64Value)?.long?.let { ExprValue.newInt(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_INT -> (this as? IntValue)?.long?.let { ExprValue.newInt(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_DECIMAL -> (this as? DecimalValue)?.value?.let { ExprValue.newDecimal(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_FLOAT32 -> (this as? Float32Value)?.double?.let { ExprValue.newFloat(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_FLOAT64 -> (this as? Float32Value)?.double?.let { ExprValue.newFloat(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_CHAR -> (this as? NullableCharValue)?.string?.let { ExprValue.newString(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_STRING -> (this as? NullableStringValue)?.string?.let {
                ExprValue.newString(
                    it
                )
            }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_SYMBOL -> (this as? NullableSymbolValue)?.string?.let {
                ExprValue.newSymbol(
                    it
                )
            }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_BINARY -> TODO()

            PartiQLValueType.NULLABLE_BYTE -> TODO()

            PartiQLValueType.NULLABLE_BLOB -> (this as? NullableBlobValue)?.value?.let { ExprValue.newBlob(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_CLOB -> (this as? NullableClobValue)?.value?.let { ExprValue.newClob(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_DATE -> (this as? NullableDateValue)?.value?.let { ExprValue.newDate(it) }
                ?: ExprValue.nullValue

            PartiQLValueType.NULLABLE_TIME -> {
                (this as? NullableTimeValue)?.let { tv ->
                    tv.value?.let { value ->
                        val precision = tv.precision
                        val offset = tv.offset
                        val withzone = tv.withZone
                        if (withzone) {
                            offset?.let { offsetValue -> ExprValue.newTime(Time.of(value, precision, offsetValue)) } ?: null
                        } else {
                            ExprValue.newTime(Time.of(value, precision, null))
                        }
                    }
                } ?: ExprValue.nullValue
            }

            PartiQLValueType.NULLABLE_TIMESTAMP -> TODO()
            // TODO: Implement
            //          {
            //                (partiqlValue as? NullableTimestampValue)?.let { tv ->
            //                    tv.value?.let { localDateTime ->
            //                        val zoneOffset = tv.offset
            //                        val instant = localDateTime.atOffset(zoneOffset ?: ZoneOffset.UTC).toInstant()
            //                        val timestamp = Timestamp.forMillis(instant.toEpochMilli(), (zoneOffset?.totalSeconds ?: 0) / 60)
            //                        newTimestamp(timestamp)
            //                    }
            //                } ?: ExprValue.nullValue
            //            }

            PartiQLValueType.NULLABLE_INTERVAL -> TODO() // add nullable interval conversion

            PartiQLValueType.NULLABLE_BAG -> {
                (this as? NullableBagValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newBag(it.asSequence()) } ?: ExprValue.nullValue
            }

            PartiQLValueType.NULLABLE_LIST -> {
                (this as? NullableListValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newList(it.asSequence()) } ?: ExprValue.nullValue
            }

            PartiQLValueType.NULLABLE_SEXP -> {
                (this as? NullableSexpValue<*>)?.elements?.map { it.toExprValue() }
                    ?.let { ExprValue.newSexp(it.asSequence()) } ?: ExprValue.nullValue
            }

            PartiQLValueType.NULLABLE_STRUCT -> {
                (this as? StructValue<*>)?.fields?.map { it.second.toExprValue().namedValue(
                    ExprValue.newString(
                        it.first
                    )
                ) }
                    ?.let { ExprValue.newStruct(it.asSequence(), StructOrdering.ORDERED) } ?: ExprValue.nullValue
            }
        }
    }
}
