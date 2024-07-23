package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class IdentifierNode extends ASTNode implements Parsable {
    private String name;

    public IdentifierNode() {}

    public IdentifierNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        Token token = parser.consume(TokenType.IDENTIFIER); // Only consume the identifier token
        this.name = token.getValue();
        return this;
    }

}