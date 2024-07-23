package net.liopyu.kotlinscript.ast;

import java.util.List;

public class DoWhileStatement extends ASTNode {
    public final ASTNode condition;
    public final List<ASTNode> body;

    public DoWhileStatement(ASTNode condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }
}