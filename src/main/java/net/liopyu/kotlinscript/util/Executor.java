package net.liopyu.kotlinscript.util;
import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.ASTVisitor;
import net.liopyu.kotlinscript.ast.PrintNode;
import net.liopyu.kotlinscript.ast.VariableDeclarationNode;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Executor implements ASTVisitor {
    private Map<String, Object> variables = new HashMap<>();

    public void execute(ArrayList<ASTNode> nodes) {
        for (ASTNode node : nodes) {
            node.accept(this);
        }
    }

    @Override
    public void visit(VariableDeclarationNode node) {
        variables.put(node.getName(), node.getValue());
        System.out.println("Variable " + node.getName() + " assigned value " + node.getValue());
    }

    @Override
    public void visit(PrintNode node) {
        Object value = variables.get(node.getVariableName());
        if (value != null) {
            System.out.println(value);
        } else {
            System.err.println("Variable " + node.getVariableName() + " not found.");
        }
    }
}