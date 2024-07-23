package net.liopyu.kotlinscript.ast;

public class Literal extends ASTNode {
    public final String value;

    public Literal(String value) {
        this.value = value;
    }
}