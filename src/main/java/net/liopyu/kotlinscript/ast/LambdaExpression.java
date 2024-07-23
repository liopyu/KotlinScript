package net.liopyu.kotlinscript.ast;

import java.util.List;

public class LambdaExpression extends ASTNode {
    public final List<String> parameters;
    public final ASTNode body;

    public LambdaExpression(List<String> parameters, ASTNode body) {
        this.parameters = parameters;
        this.body = body;
    }
}
