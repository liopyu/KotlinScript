package net.liopyu.kotlinscript.ast;

import java.util.List;

public class AbstractMethodDeclaration extends ASTNode {
    public final String name;
    public final List<String> parameters;

    public AbstractMethodDeclaration(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
}