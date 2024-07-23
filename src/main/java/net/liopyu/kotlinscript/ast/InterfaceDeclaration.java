package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class InterfaceDeclaration extends ASTNode {
    public final String name;
    public final List<ASTNode> methods;

    public InterfaceDeclaration(String name, List<ASTNode> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}