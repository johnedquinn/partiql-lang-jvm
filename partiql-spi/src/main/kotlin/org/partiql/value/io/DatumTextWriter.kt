/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.io

import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.io.PrintStream

/**
 * [PartiQLValueWriter] which outputs PartiQL text.
 *
 * @property out        PrintStream
 * @property formatted  Print with newlines and indents
 * @property indent     Indent prefix, default is 2-spaces
 */
public class DatumTextWriter(
    private val out: PrintStream,
    private val formatted: Boolean = true,
    private val indent: String = "  ",
) : DatumWriter {

    override fun append(value: Datum): DatumWriter {
        val format = if (formatted) Format(indent) else null
        visit(value, format)
        return this
    }

    override fun close() {
        out.close()
    }

    private fun visit(datum: Datum, format: Format?) {
        if (datum.isNull) {
            out.append("null")
            return
        }
        if (datum.isMissing) {
            out.append("missing")
            return
        }
        when (datum.type.kind) {
            PType.Kind.BAG -> visitBag(datum, format)
            PType.Kind.DYNAMIC -> TODO()
            PType.Kind.BOOL -> TODO()
            PType.Kind.TINYINT -> TODO()
            PType.Kind.SMALLINT -> TODO()
            PType.Kind.INTEGER -> TODO()
            PType.Kind.BIGINT -> TODO()
            PType.Kind.NUMERIC -> TODO()
            PType.Kind.DECIMAL -> TODO()
            PType.Kind.DECIMAL_ARBITRARY -> TODO()
            PType.Kind.REAL -> TODO()
            PType.Kind.DOUBLE -> TODO()
            PType.Kind.CHAR -> TODO()
            PType.Kind.VARCHAR -> TODO()
            PType.Kind.STRING -> visitString(datum,  format)
            PType.Kind.SYMBOL -> TODO()
            PType.Kind.BLOB -> TODO()
            PType.Kind.CLOB -> TODO()
            PType.Kind.DATE -> TODO()
            PType.Kind.TIME -> TODO()
            PType.Kind.TIMEZ -> TODO()
            PType.Kind.TIMESTAMP -> TODO()
            PType.Kind.TIMESTAMPZ -> TODO()
            PType.Kind.ARRAY -> TODO()
            PType.Kind.ROW -> visitRow(datum, format)
            PType.Kind.SEXP -> TODO()
            PType.Kind.STRUCT -> visitStruct(datum, format)
            PType.Kind.UNKNOWN -> TODO()
            PType.Kind.VARIANT -> TODO()
        }
    }

    private fun visitBag(datum: Datum, format: Format?) {
        out.collection(datum, format, "<<" to ">>")
    }

    private fun visitStruct(datum: Datum, format: Format?) {
        out.fields(datum, format)
    }

    private fun visitString(datum: Datum, format: Format?) {
        out.append("\"")
        out.append(datum.string)
        out.append("\"")
    }

    private fun visitRow(datum: Datum, format: Format?) {
        out.fields(datum, format)
    }

    private fun PrintStream.fields(datum: Datum, format: Format?) {
        val nested = format?.nest()
        append("{")
        val fieldIter = datum.fields.iterator()
        var first = true
        while (fieldIter.hasNext()) {
            if (first) {
                first = false
                appendLine()
            } else {
                appendLine(",")
            }
            val field = fieldIter.next()
            val k = field.name
            val v = field.value
            append(nested?.prefix)
            append(k)
            append(": ")
            visit(v, nested)
        }
        appendLine()
        append(format?.prefix)
        append("}")
    }

    private fun PrintStream.collection(
        v: Datum,
        format: Format?,
        terminals: Pair<String, String>,
        separator: CharSequence = ",",
    ) {
        val nested = format?.nest()
        appendLine(terminals.first)
        val vIter = v.iterator()
        var first = true
        while (vIter.hasNext()) {
            if (first) {
                first = false
            } else {
                appendLine(separator)
            }
            val e = vIter.next()
            append(nested?.prefix)
            visit(e, nested)
        }
        appendLine()
        append(format?.prefix)
        append(terminals.second)
    }

    /**
     * Text format
     *
     * @param indent    Index prefix
     */
    private data class Format(
        val indent: String = "  ",
        val prefix: String = "",
    ) {
        fun nest() = copy(prefix = prefix + indent)
    }
}