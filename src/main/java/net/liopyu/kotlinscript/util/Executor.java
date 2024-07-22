package net.liopyu.kotlinscript.util;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Executor {
    private HashMap<String, Object> globalScope = new HashMap<>();
    private ArrayList<Token> tokens;
    private int currentTokenIndex = 0;

    public Executor(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public void execute() {
        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            if (token.type == Token.Type.KEYWORD && token.text.equals("fun")) {
                handleFunctionDefinition();
            } else if (token.type == Token.Type.KEYWORD && token.text.equals("val")) {
                handleVariableDeclaration();
            } else if (token.type == Token.Type.IDENTIFIER && token.text.equals("println")) {
                handlePrintStatement();
            } else if (token.type == Token.Type.IDENTIFIER && globalScope.containsKey(token.text)) {
                handleFunctionCall(token.text);
            } else {
                currentTokenIndex++;
            }
        }
    }

    private void handleFunctionDefinition() {
        currentTokenIndex++; // Skip 'fun'
        String functionName = tokens.get(currentTokenIndex).text;
        currentTokenIndex++; // Skip function name
        currentTokenIndex++; // Skip '('
        // Skipping parameter parsing for simplicity
        currentTokenIndex++; // Skip ')'
        currentTokenIndex++; // Skip '{'
        ArrayList<Token> functionBody = new ArrayList<>();
        while (!tokens.get(currentTokenIndex).text.equals("}")) {
            functionBody.add(tokens.get(currentTokenIndex));
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip '}'
        globalScope.put(functionName, functionBody);
    }

    private void handleVariableDeclaration() {
        currentTokenIndex++; // Skip 'val'
        String varName = tokens.get(currentTokenIndex).text;
        currentTokenIndex++; // Skip variable name
        currentTokenIndex++; // Skip '='
        StringBuilder expression = new StringBuilder();
        while (currentTokenIndex < tokens.size() && tokens.get(currentTokenIndex).type != Token.Type.PUNCTUATION) {
            expression.append(tokens.get(currentTokenIndex).text);
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip the punctuation (e.g., ';')
        globalScope.put(varName, evaluateExpression(expression.toString()));
    }

    private void handlePrintStatement() {
        currentTokenIndex++; // Skip 'println'
        currentTokenIndex++; // Skip '('
        StringBuilder sb = new StringBuilder();
        while (tokens.get(currentTokenIndex).type != Token.Type.PUNCTUATION || !tokens.get(currentTokenIndex).text.equals(")")) {
            Token token = tokens.get(currentTokenIndex);
            if (token.type == Token.Type.LITERAL) {
                sb.append(token.text.replace("\"", "")); // Remove quotes from string literals
            } else if (token.type == Token.Type.IDENTIFIER) {
                sb.append(evaluateExpression(token.text)); // Evaluate expression
            } else if (token.type == Token.Type.OPERATOR) {
                sb.append(" " + token.text + " "); // Append operator
            }
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip ')'
        currentTokenIndex++; // Skip ';' if present
        System.out.println(sb.toString());
    }

    private void handleFunctionCall(String functionName) {
        if (globalScope.containsKey(functionName)) {
            ArrayList<Token> functionBody = (ArrayList<Token>) globalScope.get(functionName);
            Executor functionExecutor = new Executor(functionBody);
            functionExecutor.globalScope.putAll(globalScope); // Pass the current scope
            functionExecutor.execute();
        }
        currentTokenIndex++;
    }

    private Object evaluateExpression(String expression) {
        // Split the expression into tokens
        ArrayList<Token> exprTokens = Tokenizer.tokenize(expression);
        Stack<Object> stack = new Stack<>();
        for (Token token : exprTokens) {
            if (token.type == Token.Type.LITERAL) {
                stack.push(token.text.replace("\"", "")); // Remove quotes from string literals
            } else if (token.type == Token.Type.IDENTIFIER) {
                if (globalScope.containsKey(token.text)) {
                    stack.push(globalScope.get(token.text));
                } else {
                    stack.push(token.text);
                }
            } else if (token.type == Token.Type.OPERATOR) {
                Object b = stack.pop();
                Object a = stack.pop();
                if (token.text.equals("+")) {
                    try {
                        int result = Integer.parseInt(a.toString()) + Integer.parseInt(b.toString());
                        stack.push(result);
                    } catch (NumberFormatException e) {
                        // Handle string concatenation
                        stack.push(a.toString() + b.toString());
                    }
                } else if (token.text.equals("*")) {
                    int result = Integer.parseInt(a.toString()) * Integer.parseInt(b.toString());
                    stack.push(result);
                }
                // Handle other operators as needed
            }
        }
        return stack.isEmpty() ? "" : stack.pop();
    }
}