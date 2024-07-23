package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class FunctionCall extends ASTNode {
    public final String name;
    public final List<ASTNode> arguments;

    public FunctionCall(String name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}