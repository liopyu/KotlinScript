package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.Map;

public class AnnotationWithParameters extends ASTNode {
    public final String name;
    public final Map<String, String> parameters;

    public AnnotationWithParameters(String name, Map<String, String> parameters) {
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