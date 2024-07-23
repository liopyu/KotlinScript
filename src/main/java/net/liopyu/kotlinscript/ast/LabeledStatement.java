package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class LabeledStatement extends ASTNode {
    public final String label;
    public final ASTNode statement;

    public LabeledStatement(String label, ASTNode statement) {
        this.label = label;
        this.statement = statement;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}