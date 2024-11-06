package org.partiql.spi;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * TODO
 */
public interface RecordCursor {
    /**
     * TODO
     * @param field TODO
     * @return TODO
     */
    @NotNull
    Datum getDatum(int field);
}
