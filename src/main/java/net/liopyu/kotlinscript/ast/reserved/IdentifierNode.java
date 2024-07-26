package net.liopyu.kotlinscript.ast.reserved;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.ast.astinterface.Value;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenPos;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class IdentifierNode extends ASTNode implements Parsable, Value {
    private String name;

    public IdentifierNode() {}

    public IdentifierNode(String name, TokenPos pos) {
        this.name = name;
        this.setPos(pos);
    }

    @Override
    public String getValue() {
        return name;
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

    @Override
    public void append(AstStringBuilder builder) {

    }
}