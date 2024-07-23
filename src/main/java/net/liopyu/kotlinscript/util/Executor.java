package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Executor {
    private Scope currentScope;
    private Map<String, Object> variables;

    public Executor() {
        currentScope = new Scope(null); // Global scope
        variables = new HashMap<>();
    }

    public void execute(ASTNode node) {
        System.out.println("Executing node: " + node);
        if (node instanceof Program) {
            executeProgram((Program) node);
        } else if (node instanceof VariableDeclaration) {
            executeVariableDeclaration((VariableDeclaration) node);
        } else if (node instanceof FunctionDeclaration) {
            executeFunctionDeclaration((FunctionDeclaration) node);
        } else if (node instanceof PrintStatement) {
            executePrintStatement((PrintStatement) node);
        } else if (node instanceof BinaryOperation) {
            executeBinaryOperation((BinaryOperation) node);
        } else if (node instanceof FunctionCall) {
            executeFunctionCall((FunctionCall) node);
        } else if (node instanceof Identifier) {
            executeIdentifier((Identifier) node);
        } else if (node instanceof Literal) {
            executeLiteral((Literal) node);
        } else if (node instanceof IfStatement) {
            executeIfStatement((IfStatement) node);
        } else if (node instanceof WhileStatement) {
            executeWhileStatement((WhileStatement) node);
        } else if (node instanceof ReturnStatement) {
            executeReturnStatement((ReturnStatement) node);
        } else if (node instanceof Block) {
            executeBlock((Block) node);
        } else {
            throw new RuntimeException("Unknown AST node type: " + node.getClass());
        }
    }

    private void executeProgram(Program program) {
        System.out.println("Executing program");
        for (ASTNode statement : program.statements) {
            execute(statement);
        }
    }

    private void executeVariableDeclaration(VariableDeclaration node) {
        System.out.println("Executing variable declaration: " + node.name);
        Object value = evaluate(node.initializer);
        variables.put(node.name, value);
        currentScope.declareVariable(node.name, value.getClass().getSimpleName());
    }

    private void executeFunctionDeclaration(FunctionDeclaration node) {
        System.out.println("Executing function declaration: " + node.name);
        currentScope.declareFunction(node.name, node.returnType);
        variables.put(node.name, node); // Store the node itself for function calls
    }

    private void executePrintStatement(PrintStatement node) {
        System.out.println("Executing print statement");
        Object value = evaluate(node.expression);
        System.out.println(value);
    }

    private void executeBinaryOperation(BinaryOperation node) {
        System.out.println("Executing binary operation: " + node.operator);
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);
        switch (node.operator) {
            case "+":
                variables.put(node.toString(), (int) left + (int) right);
                break;
            case "-":
                variables.put(node.toString(), (int) left - (int) right);
                break;
            case "*":
                variables.put(node.toString(), (int) left * (int) right);
                break;
            case "/":
                variables.put(node.toString(), (int) left / (int) right);
                break;
            case "==":
                variables.put(node.toString(), left.equals(right));
                break;
            case "!=":
                variables.put(node.toString(), !left.equals(right));
                break;
            default:
                throw new RuntimeException("Unknown operator: " + node.operator);
        }
    }

    private void executeFunctionCall(FunctionCall node) {
        FunctionDeclaration function = (FunctionDeclaration) variables.get(node.name);
        if (function == null) {
            throw new RuntimeException("Function not defined: " + node.name);
        }

        // Setup function scope
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope);

        // Assign arguments to parameters
        if (node.arguments.size() != function.parameters.size()) {
            throw new RuntimeException("Argument count mismatch for function " + node.name);
        }

        for (int i = 0; i < node.arguments.size(); i++) {
            String paramName = function.parameters.get(i);
            Object value = evaluate(node.arguments.get(i));
            currentScope.declareVariable(paramName, (String) value);
        }

        // Execute function body
        executeBlock(new Block(function.body));

        // Restore scope
        currentScope = previousScope;
    }


    private void executeIdentifier(Identifier node) {
        System.out.println("Executing identifier: " + node.name);
        Object value = variables.get(node.name);
        if (value == null) {
            throw new RuntimeException("Variable not found: " + node.name);
        }
        variables.put(node.toString(), value);
    }

    private void executeLiteral(Literal node) {
        System.out.println("Executing literal: " + node.value);
        variables.put(node.toString(), node.value);
    }

    private Object evaluate(ASTNode node) {
        System.out.println("Evaluating node: " + node);
        if (node instanceof Literal) {
            return ((Literal) node).value;
        } else if (node instanceof Identifier) {
            return variables.get(((Identifier) node).name);
        } else if (node instanceof BinaryOperation) {
            return evaluateBinaryOperation((BinaryOperation) node);
        } else if (node instanceof FunctionCall) {
            return evaluateFunctionCall((FunctionCall) node);
        } else if (node instanceof UnaryOperation) {
            return evaluateUnaryOperation((UnaryOperation) node);
        } else {
            throw new RuntimeException("Unknown AST node type: " + node.getClass());
        }
    }

    private Object evaluateBinaryOperation(BinaryOperation node) {
        System.out.println("Evaluating binary operation: " + node.operator);
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);
        switch (node.operator) {
            case "+":
                return (int) left + (int) right;
            case "-":
                return (int) left - (int) right;
            case "*":
                return (int) left * (int) right;
            case "/":
                return (int) left / (int) right;
            case "==":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            default:
                throw new RuntimeException("Unknown operator: " + node.operator);
        }
    }

    private Object evaluateFunctionCall(FunctionCall node) {
        System.out.println("Evaluating function call: " + node.name);
        FunctionDeclaration function = (FunctionDeclaration) variables.get(node.name);
        if (function == null) {
            throw new RuntimeException("Function not found: " + node.name);
        }

        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the function

        for (int i = 0; i < function.parameters.size(); i++) {
            String paramName = function.parameters.get(i);
            ASTNode argument = node.arguments.get(i);
            Object value = evaluate(argument);
            variables.put(paramName, value);
            currentScope.declareVariable(paramName, value.getClass().getSimpleName());
        }

        for (ASTNode statement : function.body) {
            execute(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        return null;
    }

    private Object evaluateUnaryOperation(UnaryOperation node) {
        System.out.println("Evaluating unary operation: " + node.operator);
        Object right = evaluate(node.right);
        switch (node.operator) {
            case "-":
                return -(int) right;
            case "!":
                return !(boolean) right;
            default:
                throw new RuntimeException("Unknown operator: " + node.operator);
        }
    }

    private void executeIfStatement(IfStatement node) {
        System.out.println("Executing if statement");
        Object condition = evaluate(node.condition);
        if ((boolean) condition) {
            for (ASTNode statement : node.thenBranch) {
                execute(statement);
            }
        } else {
            for (ASTNode statement : node.elseBranch) {
                execute(statement);
            }
        }
    }

    private void executeWhileStatement(WhileStatement node) {
        System.out.println("Executing while statement");
        while ((boolean) evaluate(node.condition)) {
            for (ASTNode statement : node.body) {
                execute(statement);
            }
        }
    }

    private void executeReturnStatement(ReturnStatement node) {
        System.out.println("Executing return statement");
        // Handle return statements if necessary
        // This would depend on your execution model and function handling
    }

    private void executeBlock(Block node) {
        System.out.println("Executing block");
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the block
        for (ASTNode statement : node.statements) {
            execute(statement);
        }
        currentScope = previousScope; // Restore the previous scope
    }

    public void executeScript(String filePath) throws IOException {
        System.out.println("Executing script from file: " + filePath);

        // Step 1: Read the KotlinScript file
        String sourceCode = new String(Files.readAllBytes(Paths.get(filePath)));
        System.out.println("Source code: " + sourceCode);

        // Step 2: Tokenization
        Tokenizer tokenizer = new Tokenizer();
        List<Tokenizer.Token> tokens = tokenizer.tokenize(sourceCode);
        tokens.add(new Tokenizer.Token("EOF", ""));
        System.out.println("Tokens: " + tokens);

        // Step 3: Parsing
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();
        System.out.println("AST: " + program);

        // Step 4: Semantic Analysis
        Scope globalScope = new Scope(null); // Create a global scope
        TypeChecker typeChecker = new TypeChecker(globalScope);
        typeChecker.check(program);

        // Step 5: Execution
        execute(program);
    }

    public static void main(String[] args) throws IOException {
        String path = "run/scripts/script.kts"; // Path to the .kts file

        Executor executor = new Executor();
        executor.executeScript(path);

        System.out.println("Execution completed successfully.");
    }
}