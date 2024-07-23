package net.liopyu.kotlinscript.ast;

import java.util.List;

public class ConstructorDeclaration extends ASTNode {
    public final List<String> parameters;
    public final List<ASTNode> body;

    public ConstructorDeclaration(List<String> parameters, List<ASTNode> body) {
        this.parameters = parameters;
        this.body = body;
    }
}