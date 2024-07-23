package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class Annotation extends ASTNode {
    public final String name;

    public Annotation(String name) {
        this.name = name;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}
