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
        execute();  // Automatically start execution upon instantiation
    }

    private void execute() {
        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            System.out.println("Processing token: " + token.text + " of type " + token.type); // Debugging output
            if (token.type == Token.Type.KEYWORD && token.text.equals("val")) {
                handleVariableDeclaration();
            } else if (token.type == Token.Type.IDENTIFIER && token.text.equals("println")) {
                handlePrintStatement();
            } else {
                currentTokenIndex++; // Handle unrecognized tokens
            }
        }
    }

    private void handleVariableDeclaration() {
        currentTokenIndex++; // Skip 'val'
        String varName = tokens.get(currentTokenIndex).text; // Variable name
        currentTokenIndex++; // Move past the variable name
        currentTokenIndex++; // Skip '='
        StringBuilder expression = new StringBuilder();
        while (currentTokenIndex < tokens.size() && tokens.get(currentTokenIndex).type != Token.Type.PUNCTUATION) {
            expression.append(tokens.get(currentTokenIndex).text + " ");
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip punctuation such as ';'
        Object value = evaluateExpression(expression.toString().trim());
        globalScope.put(varName, value);
        System.out.println("Declared variable: " + varName + " with value: " + value); // Debugging output
    }

    private void handlePrintStatement() {
        currentTokenIndex++; // Skip 'println'
        currentTokenIndex++; // Skip '('
        StringBuilder expression = new StringBuilder();
        while (currentTokenIndex < tokens.size() && (tokens.get(currentTokenIndex).type != Token.Type.PUNCTUATION || !tokens.get(currentTokenIndex).text.equals(")"))) {
            expression.append(tokens.get(currentTokenIndex).text + " ");
            currentTokenIndex++;
        }
        currentTokenIndex++; // Skip ')'
        Object result = evaluateExpression(expression.toString().trim());
        System.out.println(result); // Actual print statement
    }

    private Object evaluateExpression(String expression) {
        Stack<Object> stack = new Stack<>();
        String[] parts = expression.split(" ");
        for (String part : parts) {
            try {
                if (part.matches("\\d+")) { // Handle integers
                    stack.push(Integer.parseInt(part));
                } else if (globalScope.containsKey(part)) { // Handle variables
                    stack.push(globalScope.get(part));
                } else if (part.matches("[+\\-*]")) { // Handle operators
                    if (stack.size() < 2) throw new IllegalArgumentException("Insufficient values in the stack for operation.");
                    Integer b = (Integer) stack.pop();
                    Integer a = (Integer) stack.pop();
                    switch (part) {
                        case "+": stack.push(a + b); break;
                        case "-": stack.push(a - b); break;
                        case "*": stack.push(a * b); break;
                    }
                } else {
                    throw new IllegalArgumentException("Unrecognized token: " + part);
                }
            } catch (Exception e) {
                System.err.println("Error processing part '" + part + "': " + e.getMessage());
                return "Error: " + e.getMessage();
            }
        }
        return stack.isEmpty() ? "Empty Expression" : stack.pop();
    }
}