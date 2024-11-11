package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public interface PlanNode {
    @NotNull
    default String getTag() {
        Random r = new Random();
        return "plan-" + r.nextInt();
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    default Map<String, Object> debugString() {
        return new HashMap<String, Object>() {{
            put("class", this.getClass().getSimpleName());
            put("tag", getTag());
        }};
    }

    default <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        throw new UnsupportedOperationException();
    }
}
