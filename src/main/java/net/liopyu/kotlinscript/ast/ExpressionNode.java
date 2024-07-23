package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

public class ExpressionNode extends ASTNode implements Parsable {
    private String value;

    public ExpressionNode() {}

    public ExpressionNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        Token token = parser.consumeValue();
        this.value = token.getValue();
        return this;
    }
}
