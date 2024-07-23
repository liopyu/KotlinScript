package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class DefaultParameter extends ASTNode {
    public final String name;
    public final ASTNode defaultValue;

    public DefaultParameter(String name, ASTNode defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}