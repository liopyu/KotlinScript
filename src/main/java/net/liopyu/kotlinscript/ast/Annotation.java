package net.liopyu.kotlinscript.ast;

public class Annotation extends ASTNode {
    public final String name;

    public Annotation(String name) {
        this.name = name;
    }
}
