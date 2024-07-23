package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnhancedSemanticAnalyzer {
    private final Map<String, Variable> globalScope = new HashMap<>();
    private final Map<String, Map<List<String>, Function>> functionScope = new HashMap<>();

    public void analyze(ASTNode node) {
        if (node instanceof Program) {
            analyzeProgram((Program) node);
        } else if (node instanceof VariableDeclaration) {
            analyzeVariableDeclaration((VariableDeclaration) node);
        } else if (node instanceof FunctionDeclaration) {
            analyzeFunctionDeclaration((FunctionDeclaration) node);
        } else if (node instanceof PrintStatement) {
            analyzePrintStatement((PrintStatement) node);
        } else if (node instanceof BinaryOperation) {
            analyzeBinaryOperation((BinaryOperation) node);
        } else if (node instanceof FunctionCall) {
            analyzeFunctionCall((FunctionCall) node);
        } else if (node instanceof Identifier) {
            analyzeIdentifier((Identifier) node);
        } else if (node instanceof Literal) {
            // Literals don't need analysis
        } else {
            throw new RuntimeException("Unknown AST node type: " + node.getClass());
        }
    }

    private void analyzeProgram(Program program) {
        for (ASTNode statement : program.statements) {
            analyze(statement);
        }
    }

    private void analyzeVariableDeclaration(VariableDeclaration variableDeclaration) {
        if (globalScope.containsKey(variableDeclaration.name)) {
            throw new RuntimeException("Variable already declared: " + variableDeclaration.name);
        }
        String type = inferType(variableDeclaration.initializer);
        globalScope.put(variableDeclaration.name, new Variable(type));
        analyze(variableDeclaration.initializer);
    }

    private void analyzeFunctionDeclaration(FunctionDeclaration functionDeclaration) {
        functionScope.putIfAbsent(functionDeclaration.name, new HashMap<>());
        Map<List<String>, Function> overloads = functionScope.get(functionDeclaration.name);
        if (overloads.containsKey(functionDeclaration.parameters)) {
            throw new RuntimeException("Function already declared with these parameters: " + functionDeclaration.name);
        }
        overloads.put(functionDeclaration.parameters, new Function(functionDeclaration.parameters));
        Map<String, Variable> localScope = new HashMap<>(globalScope);
        for (String parameter : functionDeclaration.parameters) {
            localScope.put(parameter, new Variable("parameter"));
        }
        for (ASTNode statement : functionDeclaration.body) {
            analyze(statement);
        }
    }

    private void analyzePrintStatement(PrintStatement printStatement) {
        analyze(printStatement.expression);
    }

    private void analyzeBinaryOperation(BinaryOperation binaryOperation) {
        analyze(binaryOperation.left);
        analyze(binaryOperation.right);
        String leftType = inferType(binaryOperation.left);
        String rightType = inferType(binaryOperation.right);
        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type mismatch in binary operation: " + leftType + " and " + rightType);
        }
    }

    private void analyzeFunctionCall(FunctionCall functionCall) {
        if (!functionScope.containsKey(functionCall.name)) {
            throw new RuntimeException("Function not declared: " + functionCall.name);
        }
        Map<List<String>, Function> overloads = functionScope.get(functionCall.name);
        List<String> argumentTypes = functionCall.arguments.stream()
                .map(this::inferType)
                .collect(Collectors.toList());
        if (!overloads.containsKey(argumentTypes)) {
            throw new RuntimeException("No matching function found for: " + functionCall.name);
        }
        for (ASTNode argument : functionCall.arguments) {
            analyze(argument);
        }
    }

    private void analyzeIdentifier(Identifier identifier) {
        if (!globalScope.containsKey(identifier.name)) {
            throw new RuntimeException("Variable not declared: " + identifier.name);
        }
    }

    private String inferType(ASTNode node) {
        if (node instanceof Literal) {
            return inferLiteralType((Literal) node);
        }
        if (node instanceof Identifier) {
            Variable variable = globalScope.get(((Identifier) node).name);
            if (variable == null) {
                throw new RuntimeException("Variable not declared: " + ((Identifier) node).name);
            }
            return variable.type;
        }
        if (node instanceof BinaryOperation) {
            String leftType = inferType(((BinaryOperation) node).left);
            String rightType = inferType(((BinaryOperation) node).right);
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type mismatch in binary operation: " + leftType + " and " + rightType);
            }
            return leftType;
        }
        if (node instanceof FunctionCall) {
            List<String> argumentTypes = ((FunctionCall) node).arguments.stream()
                    .map(this::inferType)
                    .collect(Collectors.toList());
            Map<List<String>, Function> overloads = functionScope.get(((FunctionCall) node).name);
            if (overloads == null) {
                throw new RuntimeException("Function not declared: " + ((FunctionCall) node).name);
            }
            if (!overloads.containsKey(argumentTypes)) {
                throw new RuntimeException("No matching function found for: " + ((FunctionCall) node).name);
            }
            return "void"; // Placeholder, actual type inference would depend on function return types
        }
        throw new RuntimeException("Cannot infer type of node: " + node.getClass());
    }

    private String inferLiteralType(Literal literal) {
        if (literal.value.matches("\\d+")) {
            return "int";
        }
        if (literal.value.matches("\".*\"")) {
            return "string";
        }
        throw new RuntimeException("Unknown literal type: " + literal.value);
    }

    private static class Variable {
        final String type;

        Variable(String type) {
            this.type = type;
        }
    }

    private static class Function {
        final List<String> parameters;

        Function(List<String> parameters) {
            this.parameters = parameters;
        }
    }

    public static void main(String[] args) {
        Tokenizer tokenizer = new Tokenizer();
        List<Tokenizer.Token> tokens = tokenizer.tokenize("val x = 10; fun main() { if (x > 5) { print(\"Greater\"); } else { print(\"Lesser\"); } for (var i = 0; i < 10; i = i + 1) { print(i); } switch(x) { case 1: print(\"One\"); break; case 10: print(\"Ten\"); break; default: print(\"Other\"); } } fun main(y: int) { print(y); }");
        tokens.add(new Tokenizer.Token("EOF", ""));
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();
        EnhancedSemanticAnalyzer analyzer = new EnhancedSemanticAnalyzer();
        analyzer.analyze(program);
        System.out.println("Semantic analysis completed successfully.");
    }
}