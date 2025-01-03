package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class From extends AstNode {
    @NotNull
    @Getter
    private final List<FromTableRef> tableRefs;

    public From(@NotNull List<FromTableRef> tableRefs) {
        this.tableRefs = tableRefs;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(tableRefs);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFrom(this, ctx);
    }
}
