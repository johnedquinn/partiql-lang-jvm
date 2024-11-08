package org.partiql.cli.catalogs

import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.ipc.ArrowStreamReader
import org.apache.arrow.vector.ipc.ArrowStreamWriter
import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.Field
import org.apache.arrow.vector.types.pojo.FieldType
import org.apache.arrow.vector.types.pojo.Schema
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class ParquetReaderWriter {

    val rowCount: Int = 1_000_000
    val rowIndices = (0 until rowCount)
    val colCount: Int = 1_000
    val colIndices = (0 until colCount)
    val colName: (Int) -> String = { "col$it" }
    val fileName = "file_cols_${colCount}_rows_$rowCount.arrow"

    @Test
    fun write() {
        RootAllocator().use { rootAllocator ->
            val fields = colIndices.map {
                Field(colName(it), FieldType.notNullable(ArrowType.Int(32, true)), null)
            }
            val schemaPerson = Schema(fields)
            VectorSchemaRoot.create(schemaPerson, rootAllocator).use { vectorSchemaRoot ->
                colIndices.forEach {
                    val nameVector = vectorSchemaRoot.getVector(colName(it)) as IntVector
                    nameVector.allocateNew(rowCount)
                    rowIndices.forEach { rowIdx ->
                        nameVector[rowIdx] = rowIdx
                    }
                }
                vectorSchemaRoot.rowCount = rowCount
                try {
                    val file: File = File(fileName)
                    val fileOutputStream: FileOutputStream = FileOutputStream(file)
                    ByteArrayOutputStream().use { out ->
                        ArrowStreamWriter(vectorSchemaRoot, null, fileOutputStream.channel).use { writer ->
                            writer.start()
                            writer.writeBatch()
                            println("Number of rows written: " + vectorSchemaRoot.rowCount)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Test
    fun read() {
        val buffer: BufferAllocator = RootAllocator()
        val file = File(fileName)
        val rootAllocator: BufferAllocator = RootAllocator()
        val fileInputStreamForStream: FileInputStream = FileInputStream(file)
        val reader: ArrowStreamReader = ArrowStreamReader(fileInputStreamForStream, rootAllocator)
        while (reader.loadNextBatch()) {
            val root = reader.vectorSchemaRoot
            val vector0 = root.getVector(0) as IntVector
            for (i in 0 until vector0.valueCount) {
                println(vector0[i])
            }
        }
    }
}
