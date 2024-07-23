package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.*;

public class Parser {
    private List<Tokenizer.Token> tokens;
    private int current = 0;

    public Parser(List<Tokenizer.Token> tokens) {
        this.tokens = tokens;
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
        if (match("KEYWORD", "val", "var")) {
            return variableDeclaration();
        }
        if (match("KEYWORD", "fun")) {
            return functionDeclaration();
        }
        return statement();
    }



    private ASTNode variableDeclaration() {
        System.out.println("Parsing variable declaration");
        String name = consume("IDENTIFIER", "Expect variable name.").value;
        consume("SYMBOL", "=", "Expect '=' after variable name.");
        ASTNode initializer = expression();
        // Optional semicolon; do not throw an error if not found
        if (check("SYMBOL", ";")) {
            consume("SYMBOL", ";");
        }
        return new VariableDeclaration(name, initializer);
    }



    private ASTNode functionDeclaration() {
        System.out.println("Parsing function declaration");
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
        consume("SYMBOL", "{", "Expect '{' before function body.");
        List<ASTNode> body = new ArrayList<>();
        while (!check("SYMBOL", "}")) {
            body.add(declaration());
        }
        consume("SYMBOL", "}", "Expect '}' after function body.");
        return new FunctionDeclaration(name, parameters, body, returnType);
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
        System.out.println("Parsing statement at token: " + peek());
        if (match("KEYWORD", "print")) {
            return printStatement();
        }
        if (match("KEYWORD", "if")) {
            return ifStatement();
        }
        return expressionStatement();
    }
    private ASTNode ifStatement() {
        System.out.println("Parsing if statement");
        consume("SYMBOL", "(", "Expect '(' after 'if'.");
        ASTNode condition = expression();  // Parse the condition expression
        consume("SYMBOL", ")", "Expect ')' after if condition.");  // This is where it fails

        System.out.println("Parsed if condition: " + condition);

        consume("SYMBOL", "{", "Expect '{' before if body.");
        List<ASTNode> thenBranch = new ArrayList<>();
        while (!check("SYMBOL", "}")) {
            thenBranch.add(statement());
        }
        consume("SYMBOL", "}", "Expect '}' after if body.");

        List<ASTNode> elseBranch = new ArrayList<>();
        if (match("KEYWORD", "else")) {
            consume("SYMBOL", "{", "Expect '{' before else body.");
            while (!check("SYMBOL", "}")) {
                elseBranch.add(statement());
            }
            consume("SYMBOL", "}", "Expect '}' after else body.");
        }

        return new IfStatement(condition, thenBranch, elseBranch);
    }





    private ASTNode printStatement() {
        System.out.println("Parsing print statement");
        ASTNode value = expression();
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
        while (match("SYMBOL", "=")) {
            System.out.println("Parsing assignment");
            ASTNode value = expression();  // Recursive call to handle right-hand side of assignment
            if (expr instanceof Identifier) {
                String name = ((Identifier) expr).name;
                return new Assignment(name, value);
            }
            throw error(peek(), "Invalid assignment target.");
        }
        return expr;
    }



    private ASTNode ternary() {
        System.out.println("Parsing ternary");
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
        System.out.println("Parsing or");
        ASTNode expr = and();
        while (match("SYMBOL", "||")) {
            Tokenizer.Token operator = previous();
            ASTNode right = and();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }


    private ASTNode and() {
        System.out.println("Parsing and");
        ASTNode expr = equality();
        while (match("SYMBOL", "&&")) {
            Tokenizer.Token operator = previous();
            ASTNode right = equality();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }


    private ASTNode equality() {
        System.out.println("Parsing equality");
        ASTNode expr = comparison();
        while (match("SYMBOL", "==", "!=")) {
            Tokenizer.Token operator = previous();
            ASTNode right = comparison();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }


    private ASTNode comparison() {
        System.out.println("Parsing comparison");
        ASTNode expr = addition();
        while (match("SYMBOL", ">", ">=", "<", "<=")) {
            Tokenizer.Token operator = previous();
            ASTNode right = addition();
            expr = new BinaryOperation(expr, operator.value, right);
            System.out.println("Parsed comparison operation: " + expr);
        }
        return expr;
    }




    private ASTNode addition() {
        System.out.println("Parsing addition");
        ASTNode expr = multiplication();
        while (match("SYMBOL", "+", "-")) {
            Tokenizer.Token operator = previous();
            ASTNode right = multiplication();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode multiplication() {
        System.out.println("Parsing multiplication");
        ASTNode expr = unary();
        while (match("SYMBOL", "*", "/")) {
            Tokenizer.Token operator = previous();
            ASTNode right = unary();
            expr = new BinaryOperation(expr, operator.value, right);
        }
        return expr;
    }

    private ASTNode unary() {
        System.out.println("Parsing unary");
        if (match("SYMBOL", "-", "!")) {
            Tokenizer.Token operator = previous();
            ASTNode right = unary();
            return new UnaryOperation(operator.value, right);
        }
        return primary();
    }

    private ASTNode primary() {
        System.out.println("Parsing primary. Current token: " + peek());
        if (isAtEnd()) {
            throw error(peek(), "Unexpected end of input.");
        }

        if (match("NUMBER")) {
            System.out.println("Parsed number literal: " + previous().value);
            return new Literal(previous().value);  // Ensuring the value is correctly interpreted
        }

        if (match("STRING")) {
            System.out.println("Parsed string literal: " + previous().value);
            return new Literal(previous().value);
        }

        if (match("IDENTIFIER")) {
            System.out.println("Parsed identifier: " + previous().value);
            return new Identifier(previous().value);
        }

        if (match("SYMBOL", "(")) {
            System.out.println("Parsed open parenthesis");
            ASTNode expr = expression();
            consume("SYMBOL", ")", "Expect ')' after expression.");
            return expr;
        }

        throw error(peek(), "Expect expression.");
    }


    private void logCurrentToken() {
        System.out.println("Current token: " + peek());
    }


    private boolean match(String type, String... values) {
        if (isAtEnd()) return false;
        if (peek().type.equals(type) && (values.length == 0 || Arrays.asList(values).contains(peek().value))) {
            advance();
            System.out.println("Matched " + type + " with values " + Arrays.toString(values));
            return true;
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
        if (check(type, value)) {
            return advance();
        } else {
            throw new ParseException(peek(), message);
        }
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