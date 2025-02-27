package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * TODO
 */
public interface WindowFunctionNode {

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public WindowFunctionSignature getSignature();

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public List<Rex> getArguments();
}
