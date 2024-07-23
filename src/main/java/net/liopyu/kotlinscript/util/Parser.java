package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.error.TokenException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Parser {
    private ArrayList<Token> tokens;
    private int currentTokenIndex = 0;

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public ArrayList<ASTNode> parse() {
        ArrayList<ASTNode> nodes = new ArrayList<>();
        while (currentTokenIndex < tokens.size()) {
            ASTNode node = parseStatement();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public ASTNode parseStatement() {
        if (currentTokenIndex >= tokens.size()) {
            return null;
        }

        Token token = tokens.get(currentTokenIndex);
        Parsable node;

        switch (token.getType().getKind()) {
            case KEYWORD:
                switch (token.getValue()) {
                    case "var":
                        node = new VariableDeclarationNode();
                        break;
                    case "print":
                        node = new PrintNode();
                        break;
                    case "fun":
                        node = new FunctionDeclarationNode();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown keyword: " + token.getValue());
                }
                break;
            case BRACES:
                node = new BlockNode();
                break;
            case COMMENT:
                node = new CommentNode();
                break;
            case PARENTHESIS:
                // This might be part of an expression or function call.
                node = parseExpression();
                break;
            default:
                node = parseExpression();  // Handle expressions by default
                break;
        }

        return node.parse(this);
    }

    public ASTNode parseExpression() {
        Stack<ASTNode> nodeStack = new Stack<>();
        Stack<Token> operatorStack = new Stack<>();

        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);

            switch (token.getType()) {
                case NUMBER:
                    nodeStack.push(new NumberLiteralNode(Double.parseDouble(token.getValue())));
                    currentTokenIndex++;
                    break;
                case IDENTIFIER:
                    nodeStack.push(new IdentifierNode(token.getValue()));
                    currentTokenIndex++;
                    break;
                case STRING:
                    nodeStack.push(new StringLiteralNode(token.getValue()));
                    currentTokenIndex++;
                    break;
                case OPERATOR:
                    while (!operatorStack.isEmpty() && precedence(operatorStack.peek()) >= precedence(token)) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    operatorStack.push(token);
                    currentTokenIndex++;
                    break;
                case LEFT_PAREN:
                    operatorStack.push(token);
                    currentTokenIndex++;
                    break;
                case RIGHT_PAREN:
                    while (!operatorStack.isEmpty() && operatorStack.peek().getType() != TokenType.LEFT_PAREN) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    if (!operatorStack.isEmpty() && operatorStack.peek().getType() == TokenType.LEFT_PAREN) {
                        operatorStack.pop(); // Pop the LEFT_PAREN
                    }
                    currentTokenIndex++;
                    break;
                default:
                    throw new TokenException("Unexpected token type in expression: " + token.getType());
            }
        }

        while (!operatorStack.isEmpty()) {
            processOperator(nodeStack, operatorStack.pop());
        }

        return nodeStack.isEmpty() ? null : nodeStack.pop();
    }



    private void processOperator(Stack<ASTNode> nodeStack, Token operator) {
        if (nodeStack.size() < 2) {
            throw new IllegalStateException("Insufficient values in the expression stack for operation " + operator.getValue());
        }

        ASTNode right = nodeStack.pop(); // Right operand
        ASTNode left = nodeStack.pop();  // Left operand

        // Create a new BinaryExpressionNode with the left and right operands and the operator string
        nodeStack.push(new BinaryExpressionNode(left, operator.getValue(), right));
    }

    private int precedence(Token operator) {
        // Return the precedence based on the operator
        switch (operator.getValue()) {
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
    private ASTNode parseBinaryExpression(ASTNode left) {
        Token operatorToken = consume(TokenType.OPERATOR); // Consume the operator safely
        ASTNode right = parsePrimaryExpression(); // Parse the right-hand operand safely
        return new BinaryExpressionNode(left, operatorToken.getValue(), right);
    }

    public ASTNode parsePrimaryExpression() {
        Token token = tokens.get(currentTokenIndex);
        switch (token.getType()) {
            case IDENTIFIER:
                currentTokenIndex++;
                return new IdentifierNode(token.getValue());
            case NUMBER:
                currentTokenIndex++;
                return new NumberLiteralNode(Double.parseDouble(token.getValue()));
            case STRING:
                currentTokenIndex++;
                return new StringLiteralNode(token.getValue());
            case LEFT_PAREN:
                currentTokenIndex++;  // consume '('
                ASTNode expression = parseExpression();  // Recursively parse the expression inside parentheses
                if (tokens.get(currentTokenIndex).getType() == TokenType.RIGHT_PAREN) {
                    currentTokenIndex++;  // consume ')'
                } else {
                    throw new TokenException("Expected ')'");
                }
                return expression;
            default:
                throw new TokenException("Unexpected token type in primary expression: " + token.getType());
        }
    }


    private ASTNode parseFunctionCallOrIdentifier() {
        Token identifier = consume(TokenType.IDENTIFIER);
        // Check if there is another token and it's a left parenthesis before deciding it's a function call
        if (currentTokenIndex < tokens.size() && currentToken().getType() == TokenType.LEFT_PAREN) {
            return parseFunctionCall(identifier.getValue());
        } else {
            return new IdentifierNode(identifier.getValue());
        }
    }


    private FunctionCallNode parseFunctionCall(String functionName) {
        consume(TokenType.LEFT_PAREN);
        List<ASTNode> arguments = new ArrayList<>();
        while (currentTokenIndex < tokens.size() && currentToken().getType() != TokenType.RIGHT_PAREN) {
            arguments.add(parseExpression());
            // Ensure there is another token before checking if it's a comma
            if (currentTokenIndex < tokens.size() && currentToken().getType() == TokenType.COMMA) {
                consume(TokenType.COMMA);
            }
        }
        // Ensure closing parenthesis is present before consuming it
        if (currentTokenIndex < tokens.size() && currentToken().getType() == TokenType.RIGHT_PAREN) {
            consume(TokenType.RIGHT_PAREN);
        } else {
            throw new IllegalStateException("Expected ')' at the end of function call for " + functionName);
        }
        return new FunctionCallNode(functionName, arguments);
    }





    public Token consume(TokenType expectedType) {
        if (currentTokenIndex >= tokens.size()) {
            System.out.println("No more tokens available. Expected: " + expectedType);
            throw new IllegalStateException("Attempted to consume a token but none are available.");
        }
        Token token = tokens.get(currentTokenIndex);
        System.out.println("Consuming Token: " + token.getType() + " Expected: " + expectedType);
        if (token.getType() != expectedType) {
            throw new IllegalArgumentException("Expected token of type " + expectedType + " but found " + token.getType());
        }
        currentTokenIndex++;
        return token;
    }



    public Token consume(TokenType... expectedTypes) {
        if (currentTokenIndex >= tokens.size()) {
            throw new IllegalStateException("No more tokens available");
        }

        Token token = tokens.get(currentTokenIndex);
        for (TokenType type : expectedTypes) {
            if (token.getType() == type) {
                currentTokenIndex++;
                return token;
            }
        }

        throw new IllegalArgumentException("Unexpected token: " + token.getValue());
    }

    public Token consumeValue() {
        Token token = currentToken();
        TokenType type = token.getType();
        if (type == TokenType.NUMBER || type == TokenType.STRING || type == TokenType.IDENTIFIER) {
            currentTokenIndex++;
            return token;
        }
        throw new IllegalArgumentException("Unexpected token: " + token.getValue());
    }

    public Object parseValue(Token valueToken) {
        switch (valueToken.getType()) {
            case NUMBER:
            case STRING:
                return parseLiteral(valueToken.getValue());
            case IDENTIFIER:
                if (valueToken.getValue().equals("true") || valueToken.getValue().equals("false")) {
                    return Boolean.parseBoolean(valueToken.getValue());
                }
                return valueToken.getValue();
            default:
                return null;
        }
    }

    private Object parseLiteral(String value) {
        if (value.contains(".")) {
            return Double.parseDouble(value);
        } else {
            return Integer.parseInt(value);
        }
    }
    public Token currentToken() {
        if (currentTokenIndex >= tokens.size()) {
            throw new IllegalStateException("Attempted to access a token but none are available.");
        }
        return tokens.get(currentTokenIndex);
    }


}