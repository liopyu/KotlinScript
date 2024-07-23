package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;

import java.util.List;

public class Program extends ASTNode {
    public final List<ASTNode> statements;

    public Program(List<ASTNode> statements) {
        this.statements = statements;
    }
}