package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class ConstructorDeclaration extends ASTNode {
    public final List<String> parameters;
    public final List<ASTNode> body;

    public ConstructorDeclaration(List<String> parameters, List<ASTNode> body) {
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