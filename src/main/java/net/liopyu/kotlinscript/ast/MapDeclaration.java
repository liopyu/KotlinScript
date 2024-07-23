package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class MapDeclaration extends ASTNode {
    public final String name;
    public final String keyType;
    public final String valueType;

    public MapDeclaration(String name, String keyType, String valueType) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}