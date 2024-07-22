package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;

import java.util.ArrayList;
import java.util.Stack;

public class Executor implements ASTVisitor {
    private Stack<Scope> scopeStack = new Stack<>();

    public Executor() {
        scopeStack.push(new Scope(null)); // Global scope
    }

    public void execute(ArrayList<ASTNode> nodes) {
        for (ASTNode node : nodes) {
            if (node != null) {
                node.accept(this);
            }
        }
    }

    @Override
    public void visit(VariableDeclarationNode node) {
        Scope currentScope = scopeStack.peek();
        currentScope.declareVariable(node.getName(), node.getValue());
        System.out.println("Variable " + node.getName() + " assigned value " + node.getValue());
    }

    @Override
    public void visit(PrintNode node) {
        try {
            Scope currentScope = scopeStack.peek();
            Object value = currentScope.getVariable(node.getVariableName());
            System.out.println(value);
        } catch (VariableNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void visit(BlockNode node) {
        scopeStack.push(new Scope(scopeStack.peek())); // Enter new scope
        for (ASTNode statement : node.getStatements()) {
            if (statement != null) {
                statement.accept(this);
            }
        }
        scopeStack.pop();
    }
    @Override
    public void visit(CommentNode node) {
    };
}