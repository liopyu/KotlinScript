package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class StringLiteralNode extends ASTNode implements Parsable {
    private String value;

    public StringLiteralNode() {}

    public StringLiteralNode(String value) {
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
        Token token = parser.consume(TokenType.STRING);
        this.value = token.getValue();
        return this;
    }
}