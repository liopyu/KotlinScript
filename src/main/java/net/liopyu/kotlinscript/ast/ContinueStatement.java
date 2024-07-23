package net.liopyu.kotlinscript.ast;

public class ContinueStatement extends ASTNode {
    public final String label;

    public ContinueStatement(String label) {
        this.label = label;
    }
}