package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class PrintNode extends ASTNode implements Parsable {
    private ASTNode expression;

    public PrintNode() {}

    public PrintNode(ASTNode expression) {
        this.expression = expression;
    }

    public ASTNode getExpression() {
        return expression;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        parser.consume(TokenType.KEYWORD); // consume 'print'
        parser.consume(TokenType.LEFT_PAREN); // consume '('
        this.expression = parser.parseExpression();
        parser.consume(TokenType.RIGHT_PAREN); // consume ')'
        return this;
    }

}
