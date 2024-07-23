package net.liopyu.kotlinscript.ast;

public class LabeledStatement extends ASTNode {
    public final String label;
    public final ASTNode statement;

    public LabeledStatement(String label, ASTNode statement) {
        this.label = label;
        this.statement = statement;
    }
}