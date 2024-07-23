package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;

public class BinaryOperationNode extends ASTNode {
    public final ASTNode left;
    public final TokenType operator;
    public final ASTNode right;

    public BinaryOperationNode(ASTNode left, TokenType operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}
