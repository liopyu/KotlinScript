package net.liopyu.kotlinscript.ast;

public class AccessModifier extends ASTNode {
    public final String modifier;

    public AccessModifier(String modifier) {
        this.modifier = modifier;
    }
}