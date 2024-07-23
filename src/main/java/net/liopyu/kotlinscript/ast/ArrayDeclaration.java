package net.liopyu.kotlinscript.ast;

public class ArrayDeclaration extends ASTNode {
    public final String name;
    public final ASTNode size;
    public final String elementType;

    public ArrayDeclaration(String name, ASTNode size, String elementType) {
        this.name = name;
        this.size = size;
        this.elementType = elementType;
    }
}