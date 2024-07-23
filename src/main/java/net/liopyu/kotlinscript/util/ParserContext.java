package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.List;

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
        // Assuming parsePrimary is called for simplicity
        return parsePrimary();
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