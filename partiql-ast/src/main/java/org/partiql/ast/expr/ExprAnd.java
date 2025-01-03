package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprAnd extends Expr {
    @NotNull
    @Getter
    private final Expr lhs;

    @NotNull
    @Getter
    private final Expr rhs;

    public ExprAnd(@NotNull Expr lhs, @NotNull Expr rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprAnd(this, ctx);
    }
}
