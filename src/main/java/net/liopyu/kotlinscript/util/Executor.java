package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.util.error.FunctionNotFoundException;
import net.liopyu.kotlinscript.util.error.VariableNotFoundException;

import java.util.ArrayList;
import java.util.List;
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
    private boolean isTypeMatch(Object value, String typeName) {
        // Example: Check if value matches typeName
        if (typeName.equals("String") && value instanceof String) {
            return true;
        }
        // Add other type checks as necessary
        return false;
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
    public void visit(BinaryExpressionNode node) {
        Object leftValue = evaluateExpression(node.getLeft());
        Object rightValue = evaluateExpression(node.getRight());
        String operator = node.getOperator();
        Object result = applyOperator(operator, leftValue, rightValue);
        setLastEvaluatedValue(result); // Update the last evaluated value
        System.out.println(result);
    }

    private Object evaluateExpression(ASTNode expression) throws VariableNotFoundException {
        if (expression instanceof IdentifierNode) {
            Scope currentScope = scopeStack.peek();
            return currentScope.getVariable(((IdentifierNode) expression).getName());
        } else if (expression instanceof StringLiteralNode) {
            return ((StringLiteralNode) expression).getValue();
        } else if (expression instanceof NumberLiteralNode) {
            return ((NumberLiteralNode) expression).getValue();
        } else  if (expression instanceof BinaryExpressionNode) {
            BinaryExpressionNode binNode = (BinaryExpressionNode) expression;
            Object leftVal = evaluateExpression(binNode.getLeft());
            Object rightVal = evaluateExpression(binNode.getRight());
            return applyOperator(binNode.getOperator(), leftVal, rightVal);
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + expression.getClass());
        }
    }
    private Object applyOperator(String operator, Object leftVal, Object rightVal) {
        // Handle different types of operators
        switch (operator) {
            case "+":
                if (leftVal instanceof String || rightVal instanceof String) {
                    // If either operand is a string, perform string concatenation
                    return leftVal.toString() + rightVal.toString();
                } else if (leftVal instanceof Number && rightVal instanceof Number) {
                    // Add numbers
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
            // Include other operators as needed
        }
        throw new IllegalArgumentException("Unsupported operator: " + operator);
    }

    private Object evaluateBinaryExpression(Object leftValue, Object rightValue, String operator) {
        if (leftValue instanceof String || rightValue instanceof String) {
            if (operator.equals("+")) {
                return leftValue.toString() + rightValue.toString();
            }
        } else if (leftValue instanceof Number && rightValue instanceof Number) {
            double leftNumber = ((Number) leftValue).doubleValue();
            double rightNumber = ((Number) rightValue).doubleValue();
            switch (operator) {
                case "+":
                    return leftNumber + rightNumber;
                case "-":
                    return leftNumber - rightNumber;
                case "*":
                    return leftNumber * rightNumber;
                case "/":
                    return leftNumber / rightNumber;
            }
        }
        throw new IllegalArgumentException("Unsupported operator: " + operator);
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
        //Do nothing for comments
    };
    @Override
    public void visit(FunctionDeclarationNode node) {
        Scope currentScope = scopeStack.peek();
        ContextUtils.Function function = new ContextUtils.Function(node.getParameters(), node.getBody(), scopeStack.peek());
        currentScope.declareFunction(node.getName(), function);
        currentScope.declareVariable(node.getName(), function); // Treat the function as a variable
    }

    @Override
    public void visit(ExpressionNode node) {
        // Implement expression evaluation logic if needed
        System.out.println("Expression value: " + node.getValue());
    }
    @Override
    public void visit(IdentifierNode node) {
        try {
            // Retrieve the value from the current scope
            Scope currentScope = scopeStack.peek();
            Object value = currentScope.getVariable(node.getName());

            // Set this value as the last evaluated value in the executor
            setLastEvaluatedValue(value);

            // Optionally, you might print or further process the value
            System.out.println("Identifier: " + node.getName() + " = " + value);
        } catch (VariableNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            // Handle the error appropriately, possibly rethrow or log
        }
    }

    private Object lastEvaluatedValue;
    public Object getLastEvaluatedValue() {
        return lastEvaluatedValue;
    }

    public void setLastEvaluatedValue(Object value) {
        lastEvaluatedValue = value;
    }
    @Override
    public void visit(StringLiteralNode node) {
        String processedString = processEscapeCharacters(node.getValue());
        setLastEvaluatedValue(processedString); // Store processed string as the last evaluated value
        System.out.println("String literal processed: " + processedString);
    }
    private String processEscapeCharacters(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case 'n':
                        output.append('\n');
                        i++; // Skip the next character as it's part of the escape sequence
                        break;
                    case 't':
                        output.append('\t');
                        i++;
                        break;
                    case '\\':
                        output.append('\\');
                        i++;
                        break;
                    case '\"':
                        output.append('\"');
                        i++;
                        break;
                    case '\'':
                        output.append('\'');
                        i++;
                        break;
                    default:
                        output.append(c); // If no valid escape sequence, keep the backslash
                        break;
                }
            } else {
                output.append(c);
            }
        }
        return output.toString();
    }
    public void pushScope(Scope scope) {
        scopeStack.push(scope);
    }

    public void popScope() {
        scopeStack.pop();
    }
    @Override
    public void visit(FunctionCallNode node) {
        try {

            ContextUtils.Function function = scopeStack.peek().getFunction(node.getFunctionName());
            List<ASTNode> args = node.getArguments();

            if (args.size() != function.getParameters().size()) {
                throw new IllegalArgumentException("Argument count mismatch for function: " + node.getFunctionName());
            }

            for (int i = 0; i < args.size(); i++) {
                Object argValue = evaluateExpression(args.get(i));
                ContextUtils.Parameter param = function.getParameters().get(i);

                // Check if the runtime type of argValue matches param.getType()
                if (!isTypeMatch(argValue, param.getType())) {
                    throw new IllegalArgumentException("Type mismatch for parameter: " + param.getName());
                }
            }

            // Proceed with function execution as before
        } catch (FunctionNotFoundException e) {
            System.err.println("Function not found: " + node.getFunctionName());
        }
    }

    @Override
    public void visit(NumberLiteralNode node) {
        System.out.println(node.getValue());
    }
}