package net.liopyu.kotlinscript.ast;

public class VariableDeclaration extends ASTNode {
    public final String name;
    public final ASTNode initializer;

    public VariableDeclaration(String name, ASTNode initializer) {
        this.name = name;
        this.initializer = initializer;
    }
}