package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class ListDeclaration extends ASTNode {
    public final String name;
    public final String elementType;

    public ListDeclaration(String name, String elementType) {
        this.name = name;
        this.elementType = elementType;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}
