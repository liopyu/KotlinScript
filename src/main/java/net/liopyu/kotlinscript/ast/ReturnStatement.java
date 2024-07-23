package net.liopyu.kotlinscript.ast;

public class ReturnStatement extends ASTNode {
    public final ASTNode value;

    public ReturnStatement(ASTNode value) {
        this.value = value;
    }
}