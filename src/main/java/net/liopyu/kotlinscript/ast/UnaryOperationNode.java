package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class UnaryOperationNode extends ASTNode {
    private final TokenType operator;
    private final ASTNode right;

    public UnaryOperationNode(TokenType operator, ASTNode right) {
        this.operator = operator;
        this.right = right;
    }

    public TokenType getOperator() {
        return operator;
    }

    public ASTNode getRight() {
        return right;
    }

    @Override
    public String toString() {
        return String.format("UnaryOperationNode(%s %s)", operator, right);
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}
