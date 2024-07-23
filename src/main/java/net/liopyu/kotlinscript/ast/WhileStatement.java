package net.liopyu.kotlinscript.ast;

import java.util.List;

public class WhileStatement extends ASTNode {
    public final ASTNode condition;
    public final List<ASTNode> body;

    public WhileStatement(ASTNode condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }
}