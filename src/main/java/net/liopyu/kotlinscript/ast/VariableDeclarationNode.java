package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

public class VariableDeclarationNode extends ASTNode implements Parsable {
    private String name;
    private Object value;

    public VariableDeclarationNode() {}

    public VariableDeclarationNode(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        parser.consume(TokenType.KEYWORD); // consume 'var'
        Token identifier = parser.consume(TokenType.IDENTIFIER);
        parser.consume(TokenType.OPERATOR); // consume '='
        Token valueToken = parser.consumeValue(); // Handle value token separately

        this.name = identifier.getValue();
        this.value = parser.parseValue(valueToken);

        return this;
    }
}
