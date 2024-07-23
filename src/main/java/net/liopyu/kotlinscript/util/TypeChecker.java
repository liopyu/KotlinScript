package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;

public class TypeChecker {
    private Scope currentScope;

    public TypeChecker(Scope globalScope) {
        this.currentScope = globalScope;
    }

    public void check(ASTNode node) {
        if (node instanceof VariableDeclaration) {
            checkVariableDeclaration((VariableDeclaration) node);
        } else if (node instanceof FunctionDeclaration) {
            checkFunctionDeclaration((FunctionDeclaration) node);
        } else if (node instanceof FunctionCall) {
            checkFunctionCall((FunctionCall) node);
        } else if (node instanceof BinaryOperation) {
            checkBinaryOperation((BinaryOperation) node);
        } else if (node instanceof LambdaExpression) {
            checkLambdaExpression((LambdaExpression) node);
        }
        // Add checks for other AST nodes as needed
    }

    private void checkVariableDeclaration(VariableDeclaration node) {
        String variableType = inferType(node.initializer);
        currentScope.declareVariable(node.name, variableType);
    }

    private void checkFunctionDeclaration(FunctionDeclaration node) {
        String returnType = node.returnType; // Use the returnType field
        currentScope.declareFunction(node.name, returnType);
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the function
        for (ASTNode statement : node.body) {
            check(statement);
        }
        currentScope = previousScope; // Restore the previous scope
    }

    private void checkFunctionCall(FunctionCall node) {
        String functionType = currentScope.resolveFunction(node.name);
        if (functionType == null) {
            EnhancedErrorReporter.reportError("Function not declared", node);
        }
        for (ASTNode argument : node.arguments) {
            check(argument);
        }
    }

    private void checkBinaryOperation(BinaryOperation node) {
        String leftType = inferType(node.left);
        String rightType = inferType(node.right);
        if (!leftType.equals(rightType)) {
            EnhancedErrorReporter.reportError("Type mismatch in binary operation", node);
        }
    }

    private void checkLambdaExpression(LambdaExpression node) {
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the lambda
        for (String parameter : node.parameters) {
            currentScope.declareVariable(parameter, "var"); // Infer parameter type as var for now
        }
        check(node.body);
        currentScope = previousScope; // Restore the previous scope
    }

    private String inferType(ASTNode node) {
        if (node instanceof Literal) {
            return ((Literal) node).value.getClass().getSimpleName();
        } else if (node instanceof Identifier) {
            return currentScope.resolveVariable(((Identifier) node).name);
        } else if (node instanceof BinaryOperation) {
            return inferType(((BinaryOperation) node).left); // Assume left and right types are the same
        } else if (node instanceof FunctionCall) {
            return currentScope.resolveFunction(((FunctionCall) node).name);
        }
        return "unknown";
    }
}
