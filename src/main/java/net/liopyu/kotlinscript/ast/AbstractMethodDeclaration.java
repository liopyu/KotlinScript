package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class AbstractMethodDeclaration extends ASTNode {
    public final String name;
    public final List<String> parameters;

    public AbstractMethodDeclaration(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}