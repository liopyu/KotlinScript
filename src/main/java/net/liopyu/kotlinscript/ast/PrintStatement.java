package net.liopyu.kotlinscript.ast;

public class PrintStatement extends ASTNode {
    public final ASTNode expression;

    public PrintStatement(ASTNode expression) {
        this.expression = expression;
    }
}