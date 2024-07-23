package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

public class BinaryExpressionNode extends ASTNode implements Parsable {
    private ASTNode left;
    private String operator;
    private ASTNode right;

    public BinaryExpressionNode() {}

    public BinaryExpressionNode(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ASTNode getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getRight() {
        return right;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        this.left = parser.parsePrimaryExpression();
        this.operator = parser.consume(TokenType.OPERATOR).getValue();
        this.right = parser.parsePrimaryExpression();
        return this;
    }
}
