package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.KotlinScript;
import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.ast.astinterface.Optimizable;
import net.liopyu.kotlinscript.ast.binary.AdditionNode;
import net.liopyu.kotlinscript.ast.binary.DivisionNode;
import net.liopyu.kotlinscript.ast.binary.MultiplicationNode;
import net.liopyu.kotlinscript.ast.binary.SubtractionNode;
import net.liopyu.kotlinscript.ast.reserved.CommentNode;
import net.liopyu.kotlinscript.ast.reserved.IdentifierNode;
import net.liopyu.kotlinscript.ast.reserved.PrintNode;
import net.liopyu.kotlinscript.ast.expression.VariableDeclarationNode;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;

import java.util.ArrayList;
import java.util.Stack;

public class Parser {
    public ArrayList<Token> tokens;
    public int currentTokenIndex = 0;

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
        switch (token.getType()) {
            case VAL:
                if (token.getValue().equals("val")) {
                    return new VariableDeclarationNode(null, null).parse(this);
                }
                break;
            case VAR:
                if (token.getValue().equals("var")) {
                    return new VariableDeclarationNode(null, null).parse(this);
                }
                break;
            case PRINT:
                if (token.getValue().equals("print")) {
                    return new PrintNode(null).parse(this);
                }
                break;
            case LBRACE:
                if (token.getValue().equals("{")) {
                    BlockNode node = new BlockNode();
                    return node.parse(this);
                }
                break;
            case COMMENT:
                return new CommentNode(null).parse(this);
            default:
                currentTokenIndex++;
                break;
        }
        return null;
    }

    public ASTNode parseExpression() {
        Stack<ASTNode> nodeStack = new Stack<>();
        Stack<Token> operatorStack = new Stack<>();

        while (currentTokenIndex < tokens.size()) {
            Token token = getNextToken(); // This will also advance the current position

            switch (token.getType()) {
                case FLOATING:
                case IDENTIFIER:
                case STRING:
                    nodeStack.push(new StringLiteralNode(token.getValue()));
                    break;
                case OPERATOR:
                    while (!operatorStack.isEmpty() && precedence(operatorStack.peek()) >= precedence(token)) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    operatorStack.push(token);
                    break;
                case LPAREN:
                    operatorStack.push(token);
                    break;
                case RPAREN:
                    if (operatorStack.isEmpty() || operatorStack.peek().getType() != TokenType.LPAREN) {
                        throw new RuntimeException("Mismatched parentheses: No matching LPAREN for RPAREN.");
                    }
                    while (!operatorStack.isEmpty() && operatorStack.peek().getType() != TokenType.LPAREN) {
                        processOperator(nodeStack, operatorStack.pop());
                    }
                    if (!operatorStack.isEmpty() && operatorStack.peek().getType() == TokenType.LPAREN) {
                        operatorStack.pop();
                    }
                    if (operatorStack.isEmpty() || (operatorStack.peek().getType() != TokenType.OPERATOR && operatorStack.peek().getType() != TokenType.LPAREN)) {
                        if (!nodeStack.isEmpty()) {
                            return nodeStack.pop();
                        }
                    }
                    break;
                case EOF:
                    KotlinScript.LOGGER.info("end of file");
                    break;
                default:
                    throw new RuntimeException("Unexpected token type in expression: " + token);
            }
        }

        while (!operatorStack.isEmpty()) {
            processOperator(nodeStack, operatorStack.pop());
        }

        return nodeStack.isEmpty() ? null : nodeStack.pop();
    }

    public Token getNextToken() {
        if (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            currentTokenIndex++;  // Move to the next token
            return token;
        }
        return new Token(TokenType.EOF, "", currentToken().getPos());  // Return EOF token if no more tokens are available
    }


    private void processOperator(Stack<ASTNode> nodeStack, Token operator) {
        if (nodeStack.size() < 2) {
            throw new IllegalStateException("Insufficient values in the expression stack for operation " + operator.getValue());
        }

        // Handling different types of operators
        switch (operator.getValue()) {
            case "+":
                nodeStack.push(new AdditionNode());
                break;
            case "-":
                nodeStack.push(new SubtractionNode());
                break;
            case "*":
                nodeStack.push(new MultiplicationNode());
                break;
            case "/":
                nodeStack.push(new DivisionNode());
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator.getValue());
        }
    }

    public Object optimize(Object expr) {
        if (expr instanceof Optimizable) {
            return ((Optimizable) expr).optimize(this);
        }
        return expr;
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
    public ASTNode parsePrimary() {
        Token token = peek();
        if (token.getType() == TokenType.FLOATING) {
            NumericLiteralNode node = new NumericLiteralNode();
            node.parse(this); // Ensure parse method is called
            return node;
        }else if (token.getType() == TokenType.STRING) {

        }
        // Handle other primary expressions
        return null;
    }
    public ASTNode parsePrimaryExpression() {
        Token token = tokens.get(currentTokenIndex);
        switch (token.getType()) {
            case IDENTIFIER:
                currentTokenIndex++;
                return new IdentifierNode(token.getValue(),token.getPos());
            case FLOATING:
                currentTokenIndex++;
                return new NumericLiteralNode(Double.parseDouble(token.getValue()),token.getPos());
            case STRING:
                currentTokenIndex++;
                return new StringLiteralNode(token.getValue());
            case LPAREN:
                currentTokenIndex++;  // consume '('
                ASTNode expression = parseExpression();  // Recursively parse the expression inside parentheses
                if (tokens.get(currentTokenIndex).getType() == TokenType.RPAREN) {
                    currentTokenIndex++;  // consume ')'
                } else {
                    throw new VariableNotFoundException("Expected ')'");
                }
                return expression;
            default:
                throw new VariableNotFoundException("Unexpected token type in primary expression: " + token.getType());
        }
    }
    public Token advance() {
        if (!isAtEnd()) currentTokenIndex++;
        return previous();
    }
    public Token consume(TokenType type) {
        if (isAtEnd()) {
            throw new RuntimeException("Unexpected end of file.");
        }
        if (check(type)) {
            return advance();
        } else {
            throw new RuntimeException("Expected token of type " + type + " but found " + peek().getType());
        }
    }

    public Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParserException(peek(), message);
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
        return peek().getType() == type;
    }
    public Token currentToken() {
        if (currentTokenIndex >= tokens.size()) {
            throw new IllegalStateException("Attempted to access a token but none are available.");
        }
        return tokens.get(currentTokenIndex);
    }

    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    public Token peek() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        }
        return new Token(TokenType.EOF, "",currentToken().getPos()); // Return EOF token if out of bounds
    }

    public Token previous() {
        return tokens.get(currentTokenIndex - 1);
    }
}