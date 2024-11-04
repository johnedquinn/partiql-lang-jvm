package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

public interface PlanNode {
    @NotNull
    default String getTag() {
        Random r = new Random();
        return "plan-" + r.nextInt();
    }

    default <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        throw new UnsupportedOperationException();
    }
}
