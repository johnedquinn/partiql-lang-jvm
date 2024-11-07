package org.partiql.cli.catalogs

import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.ipc.ArrowStreamReader
import org.partiql.spi.RecordCursor
import org.partiql.spi.RecordSet
import java.io.File
import java.io.FileInputStream

class ParquetRecordSet(
    private val fileName: String
) : RecordSet {

    val file = File(fileName)
    private val rootAllocator: BufferAllocator = RootAllocator()
    private val fileInputStreamForStream: FileInputStream = FileInputStream(file)
    private val reader: ArrowStreamReader = ArrowStreamReader(fileInputStreamForStream, rootAllocator)

    override fun getCursor(): RecordCursor {
        return ParquetRecordCursor(reader)
    }
}
