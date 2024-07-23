package net.liopyu.kotlinscript.ast;

public class MapDeclaration extends ASTNode {
    public final String name;
    public final String keyType;
    public final String valueType;

    public MapDeclaration(String name, String keyType, String valueType) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
    }
}