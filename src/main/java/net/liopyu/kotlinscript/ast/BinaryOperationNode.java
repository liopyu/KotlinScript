package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class BinaryOperationNode extends ASTNode {
    public ASTNode left;
    public Token operator;
    public ASTNode right;

    public BinaryOperationNode(ASTNode left, Token operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public void parse(ParserContext context) {
        left = context.parseExpression();
        operator = context.advance();
        right = context.parseExpression();
    }

    @Override
    public Object eval(Scope scope) {
        Object leftValue = left.eval(scope);
        Object rightValue = right.eval(scope);

        switch (operator.type) {
            case PLUS:
                return (int) leftValue + (int) rightValue;
            case MINUS:
                return (int) leftValue - (int) rightValue;
            case MULTIPLY:
                return (int) leftValue * (int) rightValue;
            case DIVIDE:
                return (int) leftValue / (int) rightValue;
            default:
                throw new RuntimeException("Unknown operator: " + operator.type);
        }
    }

    @Override
    public String toString() {
        return String.format("BinaryOperationNode(left=%s, operator=%s, right=%s)", left, operator, right);
    }
}
