package net.liopyu.kotlinscript.ast.reserved;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.StringLiteralNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;

public class PrintNode extends ASTNode {
    private ASTNode expression = null;
    private String message = null;
    public PrintNode(ASTNode expression) {
        this.expression = expression;
    }
    public PrintNode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ASTNode getExpression() {
        return expression;
    }

    public void setExpression(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
    @Override
    public ASTNode parse(Parser parser) {
        if (message == null) {
            parser.consume(TokenType.PRINT); // consume 'print'
            parser.consume(TokenType.LPAREN); // consume '('
            this.expression = parser.parseExpression(); // Parses the expression inside 'print'
            parser.consume(TokenType.RPAREN); // consume ')'
        }else if (expression == null) {
            parser.consume(TokenType.PRINT); // consume 'print'
            parser.consume(TokenType.LPAREN); // consume '('
            this.expression = new StringLiteralNode(message); // Parses the expression inside 'print'
            parser.consume(TokenType.RPAREN); // consume ')'
        }
        return this;
    }

    @Override
    public void append(AstStringBuilder builder) {

    }
}
