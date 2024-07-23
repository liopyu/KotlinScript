package net.liopyu.kotlinscript.ast;

import java.util.ArrayList;
import java.util.List;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;


public class BlockNode extends ASTNode implements Parsable {
    private List<ASTNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        parser.consume(TokenType.LEFT_BRACE); // consume '{'

        while (parser.currentToken().getType() != TokenType.RIGHT_BRACE) {
            statements.add(parser.parseStatement());
        }

        parser.consume(TokenType.RIGHT_BRACE); // consume '}'
        return this;
    }
}