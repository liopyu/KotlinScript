package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class Reflection extends ASTNode {
    public final String operation;
    public final String target;

    public Reflection(String operation, String target) {
        this.operation = operation;
        this.target = target;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}