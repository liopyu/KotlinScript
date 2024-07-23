package net.liopyu.kotlinscript.ast;

public class BinaryOperation extends ASTNode {
    public final ASTNode left;
    public final String operator;
    public final ASTNode right;

    public BinaryOperation(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "BinaryOperation{" +
                "left=" + left +
                ", operator='" + operator + '\'' +
                ", right=" + right +
                '}';
    }
}
