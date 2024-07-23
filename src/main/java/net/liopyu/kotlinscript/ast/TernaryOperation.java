package net.liopyu.kotlinscript.ast;

public class TernaryOperation extends ASTNode {
    public final ASTNode condition;
    public final ASTNode trueExpr;
    public final ASTNode falseExpr;

    public TernaryOperation(ASTNode condition, ASTNode trueExpr, ASTNode falseExpr) {
        this.condition = condition;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;
    }
}