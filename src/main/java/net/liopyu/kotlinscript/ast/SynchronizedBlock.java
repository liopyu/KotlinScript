package net.liopyu.kotlinscript.ast;

import java.util.List;

public class SynchronizedBlock extends ASTNode {
    public final ASTNode expression;
    public final List<ASTNode> body;

    public SynchronizedBlock(ASTNode expression, List<ASTNode> body) {
        this.expression = expression;
        this.body = body;
    }
}