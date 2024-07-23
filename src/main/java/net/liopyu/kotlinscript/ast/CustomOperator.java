package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class CustomOperator extends ASTNode {
    public final String symbol;
    public final String functionName;

    public CustomOperator(String symbol, String functionName) {
        this.symbol = symbol;
        this.functionName = functionName;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}