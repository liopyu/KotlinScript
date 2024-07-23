package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class ThreadCreation extends ASTNode {
    public final ASTNode runnable;

    public ThreadCreation(ASTNode runnable) {
        this.runnable = runnable;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}