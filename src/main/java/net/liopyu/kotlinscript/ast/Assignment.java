package net.liopyu.kotlinscript.ast;


public class Assignment extends ASTNode {
    public final String name;
    public final ASTNode value;

    public Assignment(String name, ASTNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("Assignment(name=%s, value=%s)", name, value);
    }
}