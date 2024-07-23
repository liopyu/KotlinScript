package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.*;

public class Parser {
    private List<Token> tokens;
    private int current = 0;
    private ParserContext context;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.context = new ParserContext(tokens);
    }

    public ASTNode parse() {
        System.out.println("Parsing tokens: " + tokens);
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            logCurrentToken();
            ASTNode declaration = declaration();
            if (declaration != null) {
                statements.add(declaration);
            }
        }
        return new Program(statements);
    }

    private ASTNode declaration() {
        System.out.println("Parsing declaration at token: " + peek());
        if (isAtEnd()) {
            return null;
        }
        if (match(TokenType.KEYWORD_VAL) || match(TokenType.KEYWORD_VAR)) {
            return variableDeclaration();
        }
        if (match(TokenType.KEYWORD_FUN)) {
            return functionDeclaration();
        }
        return statement();
    }

    private ASTNode variableDeclaration() {
        System.out.println("Parsing variable declaration");
        if (!match(TokenType.IDENTIFIER)) {
            throw new ParseException(peek(), "Expect variable name.");
        }
        String name = previous().value;  // Assuming match advances the token

        consume(TokenType.ASSIGN, "Expect '=' after variable name");
        ASTNode initializer = expression();  // Parse the initializer expression

        if (check(TokenType.SEMICOLON)) {
            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        }
        return new VariableDeclaration(name, initializer);
    }



    private ASTNode functionDeclaration() {
        System.out.println("Parsing function declaration");
        String name = consume(TokenType.IDENTIFIER, "Expect function name.").value;
        consume(TokenType.RIGHT_PAREN, "(", "Expect '(' after function name.");
        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.LEFT_PAREN, ")")) {
            do {
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name.").value);
            } while (match(TokenType.COMMA, ","));
        }
        consume(TokenType.LEFT_PAREN, ")", "Expect ')' after parameters.");
        String returnType = "void"; // Default return type
        consume(TokenType.LEFT_BRACE, "{", "Expect '{' before function body.");
        List<ASTNode> body = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE, "}")) {
            body.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "}", "Expect '}' after function body.");
        return new FunctionDeclaration(name, parameters, body, returnType);
    }

    private ASTNode block() {
        List<ASTNode> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE, "}")) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "}", "Expect '}' after block.");
        return new BlockNode(statements);
    }

    private ASTNode statement() {
        System.out.println("Parsing statement at token: " + peek());
        if (match(TokenType.KEYWORD_PRINT)) {
            return printStatement();
        }
        if (match(TokenType.KEYWORD_IF)) {
            return ifStatement();
        }
        return expressionStatement();
    }

    private ASTNode ifStatement() {
        System.out.println("Parsing if statement");
        consume(TokenType.LEFT_PAREN, "(", "Expect '(' after 'if'.");
        ASTNode condition = expression();
        consume(TokenType.RIGHT_PAREN, ")", "Expect ')' after if condition.");
        System.out.println("Parsed if condition: " + condition);

        consume(TokenType.LEFT_BRACE, "{", "Expect '{' before if body.");
        List<ASTNode> thenBranch = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE, "}")) {
            thenBranch.add(statement());
        }
        consume(TokenType.RIGHT_BRACE, "}", "Expect '}' after if body.");

        List<ASTNode> elseBranch = new ArrayList<>();
        if (match(TokenType.KEYWORD, "else")) {
            consume(TokenType.LEFT_BRACE, "{", "Expect '{' before else body.");
            while (!check(TokenType.RIGHT_BRACE, "}")) {
                elseBranch.add(statement());
            }
            consume(TokenType.RIGHT_BRACE, "}", "Expect '}' after else body.");
        }

        return new IfStatementNode();
    }

    private ASTNode printStatement() {
        System.out.println("Parsing print statement");
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'print'.");
        ASTNode value = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
        return new PrintStatement(value);
    }


    private ASTNode expressionStatement() {
        System.out.println("Parsing expression statement");
        ASTNode expr = expression();
        return expr;
    }

    private ASTNode expression() {
        System.out.println("Parsing expression");
        return assignment();
    }

    private ASTNode assignment() {
        ASTNode expr = ternary();
        while (match(TokenType.ASSIGN, "=")) {
            System.out.println("Parsing assignment");
            ASTNode value = expression();
            if (expr instanceof IdentifierNode) {
                String name = ((IdentifierNode) expr).name;
                return new Assignment(name, value);
            }
            throw error(peek(), "Invalid assignment target.");
        }
        return expr;
    }

    private ASTNode ternary() {
        System.out.println("Parsing ternary");
        ASTNode expr = or();
        if (match(TokenType.QUESTION_MARK, "?")) {
            ASTNode trueExpr = expression();
            consume(TokenType.COLON, ":", "Expect ':' after true branch of ternary.");
            ASTNode falseExpr = ternary();
            return new TernaryOperation(expr, trueExpr, falseExpr);
        }
        return expr;
    }

    private ASTNode or() {
        System.out.println("Parsing or");
        ASTNode expr = and();
        while (match(TokenType.OR, "||")) {
            Token operator = previous();
            ASTNode right = and();
            expr = new BinaryOperationNode(expr, operator, right);
        }
        return expr;
    }

    private ASTNode and() {
        System.out.println("Parsing and");
        ASTNode expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            ASTNode right = equality();
            expr = new BinaryOperationNode(expr, operator, right); // Assuming BinaryOperationNode takes left, operator, and right ASTNodes
        }
        return expr;
    }


    private ASTNode equality() {
        System.out.println("Parsing equality");
        ASTNode expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            ASTNode right = comparison();
            expr = new BinaryOperationNode(expr, operator, right);
        }
        return expr;
    }
    private boolean match(TokenType type) {
            if (check(type)) {
                advance();
                return true;
            }
        return false;
    }
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private ASTNode comparison() {
        System.out.println("Parsing comparison");
        ASTNode expr = addition();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL,  TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            ASTNode right = addition();
            expr = new BinaryOperationNode(expr, operator, right);
            System.out.println("Parsed comparison operation: " + expr);
        }
        return expr;
    }

    private ASTNode addition() {
        System.out.println("Parsing addition");
        ASTNode expr = multiplication();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            ASTNode right = multiplication();
            expr = new BinaryOperationNode(expr, operator, right);
        }
        return expr;
    }

    private ASTNode multiplication() {
        System.out.println("Parsing multiplication");
        ASTNode expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            ASTNode right = unary();
            expr = new BinaryOperationNode(expr, operator, right);
        }
        return expr;
    }

    private ASTNode unary() {
        System.out.println("Parsing unary");
        if (match(TokenType.MINUS, TokenType.BANG)) {
            Token operator = previous();
            ASTNode right = unary();
            return new UnaryOperation(operator.value, right);
        }
        return primary();
    }

    private ASTNode primary() {
        System.out.println("Parsing primary. Current token: " + context.peek());
        if (context.isAtEnd()) {
            throw new ParseException(context.peek(), "Unexpected end of input.");
        }

        if (context.match(TokenType.NUMBER)) {
            Token token = context.previous();  // Get the last matched token, which should be the number
            LiteralNode node = new LiteralNode(Integer.parseInt(token.value));
            System.out.println("Parsed number literal: " + node.value);
            return node;
        }

        if (context.match(TokenType.STRING)) {
            Token token = context.previous();  // Get the last matched token, which should be the string
            LiteralNode node = new LiteralNode(token.value);
            System.out.println("Parsed string literal: " + node.value);
            return node;
        }

        if (context.match(TokenType.IDENTIFIER)) {
            Token token = context.previous();  // Get the last matched token, which should be the identifier
            IdentifierNode node = new IdentifierNode(token.value);
            System.out.println("Parsed identifier: " + node.name);
            return node;
        }

        if (context.match(TokenType.LEFT_PAREN)) {
            System.out.println("Parsed open parenthesis");
            ASTNode expr = expression();  // Ensure this call to expression() is correctly implemented
            context.consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }

        throw new ParseException(context.peek(), "Expect expression.");
    }


    private void logCurrentToken() {
        System.out.println("Current token: " + peek());
    }

    private boolean match(TokenType type, String... values) {
        if (isAtEnd()) return false;
        if (peek().type == type && (values.length == 0 || Arrays.asList(values).contains(peek().value))) {
            advance();
            System.out.println("Matched " + type + " with values " + Arrays.toString(values));
            return true;
        }
        return false;
    }

    private boolean check(TokenType type, String value) {
        if (isAtEnd()) return false;
        return peek().type == type && peek().value.equals(value);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String value, String message) {
        if (check(type, value)) return advance();
        throw new ParseException(peek(), message);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseException error(Token token, String message) {
        return new ParseException(token, message);
    }


}