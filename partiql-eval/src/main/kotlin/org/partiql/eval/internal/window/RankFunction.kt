package org.partiql.eval.internal.window

import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

internal class RankFunction : WindowFunction {
    override fun reset(partition: WindowPartition) {
        // TODO
    }

    override fun processRow(): Datum {
        return Datum.bigint(1)
    }
}
