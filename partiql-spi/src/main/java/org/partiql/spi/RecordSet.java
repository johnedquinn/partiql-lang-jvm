package org.partiql.spi;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
public interface RecordSet {

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    RecordCursor getCursor();
}
