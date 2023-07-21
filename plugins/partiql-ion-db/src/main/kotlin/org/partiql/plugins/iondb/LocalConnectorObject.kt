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

package org.partiql.plugins.iondb

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.PartiQLValueType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.clobValue
import org.partiql.value.decimalValue
import org.partiql.value.float64Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import java.nio.file.Path
import kotlin.io.path.inputStream

/**
 * This mock implementation of [ConnectorObject] is used to parse the [valuePath] into a [StaticType]. Currently,
 * this implementation allows for Tables, Structs, Ints, Decimals, and Booleans. When [LocalConnectorMetadata] requests
 * for the object's [StaticType], it returns the parsed descriptor.
 */
internal class LocalConnectorObject(
    private val valuePath: Path,
    private val descriptorPath: Path
) : ConnectorObject {

    private val readerBuilder = IonReaderBuilder.standard()
    private val ionLoader = createIonElementLoader()

    public fun getDescriptor(): StaticType = StaticType.ANY

    private fun getValueDescriptor(): PartiQLValueType {
        val stream = descriptorPath.inputStream()
        val reader = readerBuilder.build(stream)
        val element = ionLoader.loadSingleElement(reader)
        reader.close()
        if (element !is StructElement) { error("Expected struct descriptor") }
        val type = element["type"] as? SymbolElement ?: error("Expected symbol type")
        return PartiQLValueType.valueOf(type.textValue)
    }

    @OptIn(PartiQLValueExperimental::class)
    public fun getValue(): PartiQLValue {
        val descriptor = getValueDescriptor()
        val isCollection = when (descriptor) {
            PartiQLValueType.BAG, PartiQLValueType.SEXP, PartiQLValueType.LIST -> true
            else -> false
        }
        val stream = valuePath.inputStream()
        val reader = readerBuilder.build(stream)
        val values = mutableListOf<PartiQLValue>()
        var value = getNextPartiQLValue(reader)
        if (isCollection.not()) {
            return value ?: error("Expected a single PartiQL Value but got none")
        }
        while (value != null) {
            values.add(value)
            value = getNextPartiQLValue(reader)
        }
        reader.close()
        stream.close()
        return when (descriptor) {
            PartiQLValueType.BAG -> bagValue(values)
            PartiQLValueType.LIST -> listValue(values)
            PartiQLValueType.SEXP -> sexpValue(values)
            else -> error("Shouldn't have gotten here")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun getNextPartiQLValue(reader: IonReader): PartiQLValue? {
        val type = reader.next() ?: return null
        val value = when (type) {
            IonType.STRING -> stringValue(reader.stringValue())
            IonType.NULL -> nullValue()
            IonType.BOOL -> boolValue(reader.booleanValue())
            IonType.INT -> intValue(reader.intValue().toBigInteger())
            IonType.FLOAT -> float64Value(reader.doubleValue())
            IonType.DECIMAL -> decimalValue(reader.decimalValue())
            IonType.TIMESTAMP -> TODO()
            IonType.SYMBOL -> symbolValue(reader.symbolValue().text)
            IonType.CLOB -> clobValue(reader.stringValue().toByteArray())
            IonType.BLOB -> blobValue(reader.stringValue().toByteArray())
            IonType.LIST -> {
                reader.stepIn()
                val values = getSequenceOfValues(reader)
                reader.stepOut()
                listValue(values)
            }
            IonType.SEXP -> {
                reader.stepIn()
                val values = getSequenceOfValues(reader)
                reader.stepOut()
                sexpValue(values)
            }
            IonType.STRUCT -> {
                reader.stepIn()
                val values = getStructValues(reader)
                reader.stepOut()
                structValue(values)
            }
            IonType.DATAGRAM -> TODO()
        }
        return value
    }
    
    @OptIn(PartiQLValueExperimental::class)
    private fun getSequenceOfValues(reader: IonReader): List<PartiQLValue> {
        val values = mutableListOf<PartiQLValue>()
        var value = getNextPartiQLValue(reader)
        while (value != null) {
            values.add(value)
            value = getNextPartiQLValue(reader)
        }
        return values
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun getStructValues(reader: IonReader): List<Pair<String, PartiQLValue>> {
        val values = mutableListOf<Pair<String, PartiQLValue>>()
        var value = getNextPartiQLValue(reader)
        while (value != null) {
            values.add(reader.fieldName to value)
            value = getNextPartiQLValue(reader)
        }
        return values
    }
}
