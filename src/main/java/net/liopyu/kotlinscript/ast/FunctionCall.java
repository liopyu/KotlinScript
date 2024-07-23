package net.liopyu.kotlinscript.ast;

import java.util.List;

public class FunctionCall extends ASTNode {
    public final String name;
    public final List<ASTNode> arguments;

    public FunctionCall(String name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}