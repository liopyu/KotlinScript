package net.liopyu.kotlinscript.ast;

public class AstStringBuilder {
    public StringBuilder builder = new StringBuilder();

    public void append(char c) {
        builder.append(c);
    }

    public void appendValue(Object value) {
        if (value instanceof ASTNode) {
            ((ASTNode) value).append(this);
        } else {
            builder.append(value);
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
