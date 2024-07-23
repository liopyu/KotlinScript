package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class EnumDeclaration extends ASTNode {
    public final String name;
    public final List<String> values;

    public EnumDeclaration(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}