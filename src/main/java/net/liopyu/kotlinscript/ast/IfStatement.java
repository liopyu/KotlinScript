package net.liopyu.kotlinscript.ast;

import java.util.List;

public class IfStatement extends ASTNode {
    public final ASTNode condition;
    public final List<ASTNode> thenBranch;
    public final List<ASTNode> elseBranch;

    public IfStatement(ASTNode condition, List<ASTNode> thenBranch, List<ASTNode> elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}