package net.liopyu.kotlinscript.ast;

import java.util.List;

public class InlineFunction extends ASTNode {
    public final String name;
    public final List<String> parameters;
    public final ASTNode body;

    public InlineFunction(String name, List<String> parameters, ASTNode body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}