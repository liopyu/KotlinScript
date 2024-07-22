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
            if (token.type == Token.Type.KEYWORD && token.text.equals("val")) {
                handleVariableDeclaration();
            } else if (token.type == Token.Type.IDENTIFIER && token.text.equals("println")) {
                handlePrintStatement();
            } else {
                currentTokenIndex++;
            }
        }
    }

    private void handleVariableDeclaration() {
        currentTokenIndex++; // Skip 'val'
        String varName = tokens.get(currentTokenIndex).text;
        currentTokenIndex++; // Skip variable name
        currentTokenIndex++; // Skip '='
        StringBuilder expression = new StringBuilder();
        while (currentTokenIndex < tokens.size() && tokens.get(currentTokenIndex).type != Token.Type.PUNCTUATION) {
            expression.append(tokens.get(currentTokenIndex).text).append(" ");
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip the punctuation (e.g., ';')
        globalScope.put(varName, evaluateExpression(expression.toString().trim()));
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
                sb.append(" ").append(token.text).append(" "); // Append operator
            }
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip ')'
        System.out.println(evaluateExpression(sb.toString().trim()));
    }

    private Object evaluateExpression(String expression) {
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
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: " + expression);
                }
                Object b = stack.pop();
                Object a = stack.pop();
                if (token.text.equals("+")) {
                    try {
                        int result = Integer.parseInt(a.toString().trim()) + Integer.parseInt(b.toString().trim());
                        stack.push(result);
                    } catch (NumberFormatException e) {
                        stack.push(a.toString() + b.toString());
                    }
                } else if (token.text.equals("*")) {
                    int result = Integer.parseInt(a.toString().trim()) * Integer.parseInt(b.toString().trim());
                    stack.push(result);
                }
                // Handle other operators as needed
            }
        }
        return stack.isEmpty() ? "" : stack.pop();
    }
}