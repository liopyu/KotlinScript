package net.liopyu.kotlinscript.ast;

public class Identifier extends ASTNode {
    public final String name;

    public Identifier(String name) {
        this.name = name;
    }
}