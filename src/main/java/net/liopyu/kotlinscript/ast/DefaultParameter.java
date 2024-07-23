package net.liopyu.kotlinscript.ast;

public
class DefaultParameter extends ASTNode {
    public final String name;
    public final ASTNode defaultValue;

    public DefaultParameter(String name, ASTNode defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }
}