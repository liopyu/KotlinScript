package net.liopyu.kotlinscript.ast;

public
class ListDeclaration extends ASTNode {
    public final String name;
    public final String elementType;

    public ListDeclaration(String name, String elementType) {
        this.name = name;
        this.elementType = elementType;
    }
}
