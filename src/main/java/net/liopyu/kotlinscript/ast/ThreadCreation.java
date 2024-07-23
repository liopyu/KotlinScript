package net.liopyu.kotlinscript.ast;

public class ThreadCreation extends ASTNode {
    public final ASTNode runnable;

    public ThreadCreation(ASTNode runnable) {
        this.runnable = runnable;
    }
}