package net.liopyu.kotlinscript.ast;

import java.util.List;

public class Closure extends ASTNode {
    public final List<String> parameters;
    public final List<ASTNode> body;

    public Closure(List<String> parameters, List<ASTNode> body) {
        this.parameters = parameters;
        this.body = body;
    }
}