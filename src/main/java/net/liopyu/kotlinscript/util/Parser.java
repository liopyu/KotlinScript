package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private List<Tokenizer.Token> tokens;
    private int current = 0;

    public Parser(List<Tokenizer.Token> tokens) {
        this.tokens = tokens;
    }

    public ASTNode parse() {
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return new Program(statements);
    }

    private ASTNode declaration() {
        if (match("KEYWORD", "val", "var")) {
            return variableDeclaration();
        }
        if (match("KEYWORD", "fun")) {
            return functionDeclaration();
        }
        return statement();
    }

    private ASTNode variableDeclaration() {
        String name = consume("IDENTIFIER", "Expect variable name.").value;
        consume("SYMBOL", "=", "Expect '=' after variable name.");
        ASTNode initializer = expression();
        consume("SYMBOL", ";", "Expect ';' after variable declaration.");
        return new VariableDeclaration(name, initializer);
    }

    private ASTNode functionDeclaration() {
        String name = consume("IDENTIFIER", "Expect function name.").value;
        consume("SYMBOL", "(", "Expect '(' after function name.");
        List<String> parameters = new ArrayList<>();
        if (!check("SYMBOL", ")")) {
            do {
                parameters.add(consume("IDENTIFIER", "Expect parameter name.").value);
            } while (match("SYMBOL", ","));
        }
        consume("SYMBOL", ")", "Expect ')' after parameters.");

        String returnType = "void"; // Default return type
        if (match("SYMBOL", ":")) {
            returnType = consume("IDENTIFIER", "Expect return type.").value;
        }

        consume("SYMBOL", "{", "Expect '{' before function body.");
        ASTNode body = block();
        return new FunctionDeclaration(name, parameters, ((Block) body).statements, returnType);
    }
    private ASTNode block() {
        List<ASTNode> statements = new ArrayList<>();
        while (!check("SYMBOL", "}")) {
            statements.add(declaration());
        }
        consume("SYMBOL", "}", "Expect '}' after block.");
        return new Block(statements);
    }

    private ASTNode statement() {
        if (match("KEYWORD", "print")) {
            return printStatement();
        }
        return expressionStatement();
    }

    private ASTNode printStatement() {
        ASTNode value = expression();
        consume("SYMBOL", ";", "Expect ';' after value.");
        return new PrintStatement(value);
    }

    private ASTNode expressionStatement() {
        ASTNode expr = expression();
        consume("SYMBOL", ";", "Expect ';' after expression.");
        return expr;
    }

    private ASTNode expression() {
        return assignment();
    }

    private ASTNode assignment() {
        ASTNode expr = ternary();
        if (match("SYMBOL", "=")) {
            ASTNode value = ternary();
            if (expr instanceof Identifier) {
                String name = ((Identifier) expr).name;
                return new Assignment(name, value);
            }
            throw error(peek(), "Invalid assignment target.");
        }
        return expr;
    }

    private ASTNode ternary() {
        ASTNode expr = or();
        if (match("SYMBOL", "?")) {
            ASTNode trueExpr = expression();
            consume("SYMBOL", ":", "Expect ':' after true branch of ternary.");
            ASTNode falseExpr = ternary();
            return new TernaryOperation(expr, trueExpr, falseExpr);
        }
        return expr;
    }

    private ASTNode or() {
        ASTNode expr = and();
        while (match("SYMBOL", "||")) {
            Tokenizer.Token operator = previous();
            ASTNode right = and();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode and() {
        ASTNode expr = equality();
        while (match("SYMBOL", "&&")) {
            Tokenizer.Token operator = previous();
            ASTNode right = equality();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode equality() {
        ASTNode expr = comparison();
        while (match("SYMBOL", "==", "!=")) {
            Tokenizer.Token operator = previous();
            ASTNode right = comparison();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode comparison() {
        ASTNode expr = addition();
        while (match("SYMBOL", ">", ">=", "<", "<=")) {
            Tokenizer.Token operator = previous();
            ASTNode right = addition();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode addition() {
        ASTNode expr = multiplication();
        while (match("SYMBOL", "+", "-")) {
            Tokenizer.Token operator = previous();
            ASTNode right = multiplication();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode multiplication() {
        ASTNode expr = unary();
        while (match("SYMBOL", "*", "/")) {
            Tokenizer.Token operator = previous();
            ASTNode right = unary();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode unary() {
        if (match("SYMBOL", "-", "!")) {
            Tokenizer.Token operator = previous();
            ASTNode right = unary();
            return new UnaryOperation(operator.value, right);
        }
        return primary();
    }

    private ASTNode primary() {
        if (match("NUMBER")) {
            return new Literal(previous().value);
        }
        if (match("STRING")) {
            return new Literal(previous().value);
        }
        if (match("IDENTIFIER")) {
            return new Identifier(previous().value);
        }
        if (match("SYMBOL", "(")) {
            ASTNode expr = expression();
            consume("SYMBOL", ")", "Expect ')' after expression.");
            return expr;
        }
        throw error(peek(), "Expect expression.");
    }


    private boolean match(String type, String... values) {
        for (String value : values) {
            if (check(type, value)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(String type, String value) {
        if (isAtEnd()) return false;
        return peek().type.equals(type) && peek().value.equals(value);
    }

    private boolean check(String type) {
        if (isAtEnd()) return false;
        return peek().type.equals(type);
    }

    private Tokenizer.Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type.equals("EOF");
    }

    private Tokenizer.Token peek() {
        return tokens.get(current);
    }

    private Tokenizer.Token previous() {
        return tokens.get(current - 1);
    }

    private Tokenizer.Token consume(String type, String value, String message) {
        if (check(type, value)) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token consume(String type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseException error(Tokenizer.Token token, String message) {
        return new ParseException(token, message);
    }

    public static class ParseException extends RuntimeException {
        public final Tokenizer.Token token;

        public ParseException(Tokenizer.Token token, String message) {
            super(message);
            this.token = token;
        }
    }
}