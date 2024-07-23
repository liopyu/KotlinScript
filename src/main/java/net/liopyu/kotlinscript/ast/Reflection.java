package net.liopyu.kotlinscript.ast;

public class Reflection extends ASTNode {
    public final String operation;
    public final String target;

    public Reflection(String operation, String target) {
        this.operation = operation;
        this.target = target;
    }
}