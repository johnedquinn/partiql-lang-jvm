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

import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
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

    //
    //
    // HELPER METHODS
    //
    //

    private class NamespaceMetadata(
        val names: List<String>,
        val path: Path
    )

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
            println("Looking at $file with extension: ${file.extension.lowercase()}. Looking for table $table")
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
