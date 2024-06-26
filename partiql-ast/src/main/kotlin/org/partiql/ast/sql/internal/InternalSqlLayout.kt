/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.ast.sql.internal

internal object InternalSqlLayout {

    internal fun format(head: InternalSqlBlock): String {
        val sb = StringBuilder()
        var curr: InternalSqlBlock? = head
        while (curr != null) {
            when (curr) {
                is InternalSqlBlock.NL -> sb.appendLine()
                is InternalSqlBlock.Text -> sb.append(curr.text)
                is InternalSqlBlock.Nest -> {
                    if (curr.prefix != null) sb.append(curr.prefix)
                    sb.append(format(curr.child))
                    if (curr.postfix != null) sb.append(curr.postfix)
                }
            }
            curr = curr.next
        }
        return sb.toString()
    }
}
