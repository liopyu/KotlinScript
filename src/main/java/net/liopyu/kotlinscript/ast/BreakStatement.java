package net.liopyu.kotlinscript.ast;

public class BreakStatement extends ASTNode {
    public final String label;

    public BreakStatement(String label) {
        this.label = label;
    }
}