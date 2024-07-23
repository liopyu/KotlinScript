package net.liopyu.kotlinscript.ast;

import java.util.List;

public class InterfaceDeclaration extends ASTNode {
    public final String name;
    public final List<ASTNode> methods;

    public InterfaceDeclaration(String name, List<ASTNode> methods) {
        this.name = name;
        this.methods = methods;
    }
}