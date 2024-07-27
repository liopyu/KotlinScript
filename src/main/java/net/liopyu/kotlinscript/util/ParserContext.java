package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParserContext {
    public final List<Token> tokens;
    public int current = 0;

    public ParserContext(List<Token> tokens) {
        this.tokens = tokens;
    }
    public ParserContext(List<Token> tokens, int current) {
        this.tokens = tokens;
        this.current = current;
    }
    public ParserContext(ParserContext other) {
        this.tokens = other.tokens;
        this.current = other.current;
    }
    public ASTNode parseExpression() {
        Stack<ASTNode> nodeStack = new Stack<>();
        Stack<Token> operatorStack = new Stack<>();

        while (current < tokens.size()) {
            Token token = tokens.get(current);

            switch (token.type) {
                case NUMBER:
                    parsePrimary();
                    break;
                case IDENTIFIER:
                    nodeStack.push(new IdentifierNode(token.value));
                    current++;
                    break;
                case STRING:
                    nodeStack.push(new LiteralNode(token.value));
                    current++;
                    break;
                case PLUS:
                case MINUS:
                case MULTIPLY:
                case DIVIDE:
                case ASSIGN:
                    while (!operatorStack.isEmpty() && precedence(operatorStack.peek()) >= precedence(token)) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    operatorStack.push(token);
                    current++;
                    break;
                case LEFT_PAREN:
                    operatorStack.push(token);
                    current++;
                    break;
                case RIGHT_PAREN:
                    while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.LEFT_PAREN) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    if (!operatorStack.isEmpty() && operatorStack.peek().type == TokenType.LEFT_PAREN) {
                        operatorStack.pop(); // Pop the LEFT_PAREN
                    }
                    current++;
                    break;
                default:
                    throw new RuntimeException("Unexpected token type in expression: " + token.type);
            }
        }

        while (!operatorStack.isEmpty()) {
            processOperator(nodeStack, operatorStack.pop());
        }

        return nodeStack.isEmpty() ? null : nodeStack.pop();
    }
    private int precedence(Token operator) {
        // Return the precedence based on the operator
        switch (operator.value) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
            default:
                return 0;
        }
    }
    private void processOperator(Stack<ASTNode> nodeStack, Token operator) {
        if (nodeStack.size() < 2) {
            throw new IllegalStateException("Insufficient values in the expression stack for operation " + operator.value);
        }

        ASTNode right = nodeStack.pop(); // Right operand
        ASTNode left = nodeStack.pop();  // Left operand

        // Create a new BinaryExpressionNode with the left and right operands and the operator string
        nodeStack.push(new BinaryOperationNode(left, operator, right));
    }
    public ASTNode parsePrimary() {
        Token token = peek();
        if (token.type == TokenType.NUMBER || token.type == TokenType.STRING) {
            LiteralNode node = new LiteralNode();
            node.parse(this); // Ensure parse method is called
            return node;
        }
        // Handle other primary expressions
        return null;
    }
    public ASTNode parseStatement() {
        // Implementation for parsing a statement
        return null;
    }
    public Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    public Token consume(TokenType type) {
        if (check(type)) return advance();
        return null;
    }
    public Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParseException(peek(), message);
    }

    public boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    public boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    public boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    public Token peek() {
        return tokens.get(current);
    }

    public Token previous() {
        return tokens.get(current - 1);
    }
}