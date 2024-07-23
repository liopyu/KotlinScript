package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class LambdaExpression extends ASTNode {
    public final List<String> parameters;
    public final ASTNode body;

    public LambdaExpression(List<String> parameters, ASTNode body) {
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}
