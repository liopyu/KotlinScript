package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.binary.*;
import net.liopyu.kotlinscript.ast.expression.FunctionDeclarationNode;
import net.liopyu.kotlinscript.ast.expression.VariableDeclarationNode;
import net.liopyu.kotlinscript.ast.reserved.*;

import java.util.ArrayList;
import java.util.Stack;

public class Executor implements ASTVisitor {
    private Stack<Scope> scopeStack = new Stack<>();
    private Object lastEvaluatedValue;
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

    public Stack<Scope> getScopeStack() {
        return scopeStack;
    }

    public Object getLastEvaluatedValue() {
        return lastEvaluatedValue;
    }
    public void pushScope(Scope scope) {
        scopeStack.push(scope);
    }

    public void popScope() {
        scopeStack.pop();
    }
    private Object evaluateExpression(ASTNode expression) throws VariableNotFoundException {
        if (expression instanceof IdentifierNode) {
            Scope currentScope = getScopeStack().peek();
            return currentScope.getVariable(((IdentifierNode) expression).getName());
        } else if (expression instanceof StringLiteralNode) {
            return ((StringLiteralNode) expression).getValue();
        } else if (expression instanceof NumericLiteralNode) {
            return ((NumericLiteralNode) expression).getValue();
        } else  if (expression instanceof AstBinary binNode) {
            Object leftVal = evaluateExpression((ASTNode) binNode.getLeft());
            Object rightVal = evaluateExpression((ASTNode)binNode.getRight());
            return applyOperator(binNode.getOperator(), leftVal, rightVal);
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + expression.getClass());
        }
    }
    private Object applyOperator(String operator, Object leftVal, Object rightVal) {
        switch (operator) {
            case "+":
                if (leftVal instanceof String || rightVal instanceof String) {
                    return leftVal.toString() + rightVal.toString();
                } else if (leftVal instanceof Number && rightVal instanceof Number) {
                    return ((Number) leftVal).doubleValue() + ((Number) rightVal).doubleValue();
                }
                break;
            case "-":
                // Subtract numbers
                return ((Number) leftVal).doubleValue() - ((Number) rightVal).doubleValue();
            case "*":
                // Multiply numbers
                return ((Number) leftVal).doubleValue() * ((Number) rightVal).doubleValue();
            case "/":
                // Divide numbers, handle division by zero
                if (((Number) rightVal).doubleValue() == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return ((Number) leftVal).doubleValue() / ((Number) rightVal).doubleValue();
        }
        throw new IllegalArgumentException("Unsupported operator: " + operator);
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
            ASTNode expression = node.getExpression();
            Object value = evaluateExpression(expression);
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
    }


    @Override
    public void visit(FunctionDeclarationNode node) {
        Scope currentScope = scopeStack.peek();
        ContextUtils.Function function = new ContextUtils.Function(node.getParameters(), node.getBody(), scopeStack.peek());
        currentScope.declareFunction(node.getName(), function);
        currentScope.declareVariable(node.getName(), function); // Treat the function as a variable
    }


    @Override
    public void visit(NumericLiteralNode node) {
        lastEvaluatedValue = node.getValue();
    }

    @Override
    public void visit(StringLiteralNode node) {
        lastEvaluatedValue = node.getValue();
    }

    @Override
    public void visit(IdentifierNode node) {
        Scope currentScope = scopeStack.peek();
        lastEvaluatedValue = currentScope.getVariable(node.getName());
    }


    @Override
    public void visit(AdditionNode node) {
        visit((AstBinary) node);
    }

    @Override
    public void visit(SubtractionNode node) {
        visit((AstBinary) node);
    }

    @Override
    public void visit(MultiplicationNode node) {
        visit((AstBinary) node);
    }

    @Override
    public void visit(DivisionNode node) {
        visit((AstBinary) node);
    }

    @Override
    public void visit(AstBinary node) {
        Object leftVal = evaluateExpression((ASTNode) node.getLeft());
        Object rightVal = evaluateExpression((ASTNode) node.getRight());
        lastEvaluatedValue = applyOperator(node.getOperator(), leftVal, rightVal);
    }


    ;
}