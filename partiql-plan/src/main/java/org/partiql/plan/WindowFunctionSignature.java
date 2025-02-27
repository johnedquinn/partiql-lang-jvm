package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * TODO
 */
public interface WindowFunctionSignature {

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public String getName();

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public List<PType> getParameterTypes();

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public PType getReturnType();

    /**
     * TODO
     * @return TODO
     */
    public boolean isIgnoreNulls();
}
