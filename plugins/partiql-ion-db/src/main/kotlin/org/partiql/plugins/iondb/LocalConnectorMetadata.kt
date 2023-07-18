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

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonBinaryWriterBuilder
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.PartiQLValueType
import org.partiql.types.StaticType
import org.partiql.value.BoolValue
import org.partiql.value.DecimalValue
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.streams.toList

/**
 * This mock implementation of [ConnectorMetadata] searches for JSON files from its [root] to
 * resolve requests for any [BindingPath].
 */
class LocalConnectorMetadata(val name: String, private val root: Path) : ConnectorMetadata {

    override fun getObjectType(session: ConnectorSession, handle: ConnectorObjectHandle): StaticType {
        val ionHandle = handle.value as LocalConnectorObject
        return ionHandle.getDescriptor()
    }

    override fun getObjectHandle(
        session: ConnectorSession,
        path: BindingPath
    ): ConnectorObjectHandle? {
        val resolvedObject = resolveObject(root, path.steps) ?: return null
        return ConnectorObjectHandle(
            absolutePath = ConnectorObjectPath(resolvedObject.names),
            value = LocalConnectorObject(resolvedObject.path)
        )
    }

    // TODO: COW Hack
    @OptIn(PartiQLValueExperimental::class)
    override fun getValue(session: ConnectorSession, handle: ConnectorObjectHandle): PartiQLValue {
        val ionHandle = handle.value as LocalConnectorObject
        return ionHandle.getValue()
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun createValue(session: ConnectorSession, path: BindingPath, value: PartiQLValue) {
        var current = root
        path.steps.subList(0, path.steps.lastIndex).forEach { name ->
            current = resolveDirectory(current, name) ?: error("Could not resolve dir $name")
        }
        val finalFile = current.resolve(path.steps.last().name + ".10n")
        if (finalFile.exists()) {
            error("Value $path already exists!")
        }
        val finalPath = finalFile.createFile()
        val stream = finalPath.outputStream()
        val writer = IonBinaryWriterBuilder.standard().build(stream)
        writePartiQLValue(writer, value)
        writer.close()
        stream.close()
    }

    override fun createTable(session: ConnectorSession, path: BindingPath, schema: StaticType) {
        var current = root
        path.steps.subList(0, path.steps.lastIndex).forEach { name ->
            current = resolveDirectory(current, name) ?: error("Could not resolve dir $name")
        }
        val finalFile = current.resolve(path.steps.last().name + ".10n")
        if (finalFile.exists()) {
            error("Value $path already exists!")
        }
        finalFile.createFile()
    }

    override fun listSchemas(session: ConnectorSession): List<String> {
        return Files.list(root).filter {
            it.isDirectory()
        }.map {
            it.nameWithoutExtension
        }.toList()
    }

    override fun listTables(session: ConnectorSession, schema: BindingPath): List<String> {
        var current = root
        schema.steps.forEach { name ->
            current = resolveDirectory(current, name) ?: error("Could not resolve schema $name")
        }
        return Files.list(current).filter {
            it.isDirectory().not()
        }.map {
            it.nameWithoutExtension
        }.toList()
    }

    override fun listValues(session: ConnectorSession, schema: BindingPath): List<String> {
        var current = root
        schema.steps.forEach { name ->
            current = resolveDirectory(current, name) ?: error("Could not resolve schema $name")
        }
        return Files.list(current).filter {
            it.isDirectory().not()
        }.map {
            it.nameWithoutExtension
        }.toList()
    }

    //
    //
    // HELPER METHODS
    //
    //

    private class NamespaceMetadata(
        val names: List<String>,
        val path: Path
    )

    @OptIn(PartiQLValueExperimental::class)
    private fun writePartiQLValue(writer: IonWriter, value: PartiQLValue): Unit = when (value.type) {
        PartiQLValueType.INT -> writer.writeInt((value as IntValue).long)
        PartiQLValueType.BOOL -> writer.writeBool((value as BoolValue).value)
        PartiQLValueType.INT8 -> writer.writeInt((value as Int8Value).long)
        PartiQLValueType.INT16 -> writer.writeInt((value as Int16Value).long)
        PartiQLValueType.INT32 -> writer.writeInt((value as Int32Value).long)
        PartiQLValueType.INT64 -> writer.writeInt((value as Int64Value).long)
        PartiQLValueType.DECIMAL -> writer.writeDecimal((value as DecimalValue).double.toBigDecimal())
        PartiQLValueType.FLOAT32 -> TODO()
        PartiQLValueType.FLOAT64 -> TODO()
        PartiQLValueType.CHAR -> TODO()
        PartiQLValueType.STRING -> writer.writeString((value as StringValue).value)
        PartiQLValueType.SYMBOL -> TODO()
        PartiQLValueType.BINARY -> TODO()
        PartiQLValueType.BYTE -> TODO()
        PartiQLValueType.BLOB -> TODO()
        PartiQLValueType.CLOB -> TODO()
        PartiQLValueType.DATE -> TODO()
        PartiQLValueType.TIME -> TODO()
        PartiQLValueType.TIMESTAMP -> TODO()
        PartiQLValueType.INTERVAL -> TODO()
        PartiQLValueType.BAG -> TODO()
        PartiQLValueType.LIST -> TODO()
        PartiQLValueType.SEXP -> TODO()
        PartiQLValueType.STRUCT -> {
            writer.stepIn(IonType.STRUCT)
            val structValue = value as StructValue<*>
            structValue.forEach { element: Pair<String, PartiQLValue> ->
                writer.setFieldName(element.first)
                writePartiQLValue(writer, element.second)
            }
            writer.stepOut()
        }
        PartiQLValueType.NULL -> TODO()
        PartiQLValueType.MISSING -> TODO()
        PartiQLValueType.NULLABLE_BOOL -> TODO()
        PartiQLValueType.NULLABLE_INT8 -> TODO()
        PartiQLValueType.NULLABLE_INT16 -> TODO()
        PartiQLValueType.NULLABLE_INT32 -> TODO()
        PartiQLValueType.NULLABLE_INT64 -> TODO()
        PartiQLValueType.NULLABLE_INT -> TODO()
        PartiQLValueType.NULLABLE_DECIMAL -> TODO()
        PartiQLValueType.NULLABLE_FLOAT32 -> TODO()
        PartiQLValueType.NULLABLE_FLOAT64 -> TODO()
        PartiQLValueType.NULLABLE_CHAR -> TODO()
        PartiQLValueType.NULLABLE_STRING -> TODO()
        PartiQLValueType.NULLABLE_SYMBOL -> TODO()
        PartiQLValueType.NULLABLE_BINARY -> TODO()
        PartiQLValueType.NULLABLE_BYTE -> TODO()
        PartiQLValueType.NULLABLE_BLOB -> TODO()
        PartiQLValueType.NULLABLE_CLOB -> TODO()
        PartiQLValueType.NULLABLE_DATE -> TODO()
        PartiQLValueType.NULLABLE_TIME -> TODO()
        PartiQLValueType.NULLABLE_TIMESTAMP -> TODO()
        PartiQLValueType.NULLABLE_INTERVAL -> TODO()
        PartiQLValueType.NULLABLE_BAG -> TODO()
        PartiQLValueType.NULLABLE_LIST -> TODO()
        PartiQLValueType.NULLABLE_SEXP -> TODO()
        PartiQLValueType.NULLABLE_STRUCT -> TODO()
    }

    private fun resolveObject(root: Path, names: List<BindingName>): NamespaceMetadata? {
        var current = root
        val fileNames = mutableListOf<String>()
        names.forEach { name ->
            current = resolveDirectory(current, name) ?: return@forEach
            fileNames.add(current.fileName.toString())
        }
        if (fileNames.lastIndex == names.lastIndex) {
            return null
        }
        val table = names[fileNames.size]
        val tablePaths = Files.list(current).toList()
        var filename = ""
        val tableDef = tablePaths.firstOrNull { file ->
            if (file.extension.lowercase() != "ion" && file.extension.lowercase() != "10n") {
                return@firstOrNull false
            }
            filename = file.getName(file.nameCount - 1).nameWithoutExtension
            table.isEquivalentTo(filename)
        } ?: return null
        return NamespaceMetadata(
            names = fileNames + listOf(filename),
            path = tableDef
        )
    }

    private fun resolveDirectory(root: Path, name: BindingName): Path? {
        val schemaPaths = Files.list(root).toList()
        return schemaPaths.firstOrNull { directory ->
            val filename = directory.getName(directory.nameCount - 1).toString()
            name.isEquivalentTo(filename)
        }
    }
}
