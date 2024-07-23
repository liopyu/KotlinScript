package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

public class NumberLiteralNode extends ASTNode implements Parsable {
    private double value;

    public NumberLiteralNode() {}

    public NumberLiteralNode(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        Token token = parser.consume(TokenType.NUMBER);
        this.value = Double.parseDouble(token.getValue());
        return this;
    }
}
