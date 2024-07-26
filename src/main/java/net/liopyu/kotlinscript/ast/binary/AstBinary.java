package net.liopyu.kotlinscript.ast.binary;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.AstExpression;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.util.Parser;

import java.util.Stack;

public abstract class AstBinary extends AstExpression {
    public Object left;
    public Object right;

    @Override
    public final void append(AstStringBuilder builder) {
        builder.append('(');
        builder.appendValue(left);
        appendSymbol(builder.builder);
        builder.appendValue(right);
        builder.append(')');
    }

    public Object getLeft() {
        return left;
    }

    public Object getRight() {
        return right;
    }

    public abstract void appendSymbol(StringBuilder builder);

    @Override
    public Object optimize(Parser parser) {
        left = parser.optimize(left);
        right = parser.optimize(right);
        return this;
    }

    public void setLeft(ASTNode left) {
        this.left = left;
    }

    public void setRight(ASTNode right) {
        this.right = right;
    }
    public abstract String getOperator();

}
