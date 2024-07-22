package net.liopyu.kotlinscript.token;
import net.liopyu.kotlinscript.util.TokenPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Tokenizer {
    private final String script;
    private int currentPosition;
    private Token nextToken;

    public Tokenizer(String script) {
        this.script = script;
        this.currentPosition = 0;
        advance(); // Initialize the first token
    }

    private void advance() {
        nextToken = getToken();
    }

    public boolean hasNextToken() {
        return nextToken != null;
    }

    public Token getNextToken() {
        Token currentToken = nextToken;
        advance();
        return currentToken;
    }

    private Token getToken() {
        if (currentPosition >= script.length()) {
            return null;
        }

        char ch = script.charAt(currentPosition);

        // Skip whitespace
        while (isWhitespace(ch)) {
            currentPosition++;
            if (currentPosition >= script.length()) {
                return null;
            }
            ch = script.charAt(currentPosition);
        }

        if (isAlpha(ch) || ch == '_') {
            return readIdentifierOrKeyword();
        } else if (isDigit(ch)) {
            return readNumber();
        } else if (ch == '"' || ch == '\'') {
            return readString(ch);
        } else if (ch == '/' && currentPosition + 1 < script.length() && (script.charAt(currentPosition + 1) == '/' || script.charAt(currentPosition + 1) == '*')) {
            return readComment();
        } else {
            return readOperatorOrPunctuation();
        }
    }

    private boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    private boolean isAlpha(char ch) {
        return Character.isLetter(ch);
    }

    private boolean isDigit(char ch) {
        return Character.isDigit(ch);
    }

    private Token readIdentifierOrKeyword() {
        int start = currentPosition;
        while (currentPosition < script.length() && (isAlpha(script.charAt(currentPosition)) || isDigit(script.charAt(currentPosition)) || script.charAt(currentPosition) == '_')) {
            currentPosition++;
        }
        String identifier = script.substring(start, currentPosition);
        TokenType type = stringToKeyword(identifier);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        return new Token(type, identifier);
    }

    private Token readNumber() {
        int start = currentPosition;
        while (currentPosition < script.length() && isDigit(script.charAt(currentPosition))) {
            currentPosition++;
        }
        if (currentPosition < script.length() && script.charAt(currentPosition) == '.') {
            currentPosition++;
            while (currentPosition < script.length() && isDigit(script.charAt(currentPosition))) {
                currentPosition++;
            }
        }
        String number = script.substring(start, currentPosition);
        return new Token(TokenType.NUMBER, number);
    }

    private Token readString(char quote) {
        int start = currentPosition;
        currentPosition++; // Skip the opening quote
        while (currentPosition < script.length() && script.charAt(currentPosition) != quote) {
            if (script.charAt(currentPosition) == '\\') { // Handle escape sequences
                currentPosition++;
            }
            currentPosition++;
        }
        if (currentPosition < script.length()) {
            currentPosition++; // Skip the closing quote
        }
        String string = script.substring(start, currentPosition);
        return new Token(TokenType.STRING, string);
    }

    private Token readComment() {
        int start = currentPosition;
        if (script.charAt(currentPosition + 1) == '/') {
            currentPosition += 2; // Skip '//'
            while (currentPosition < script.length() && script.charAt(currentPosition) != '\n') {
                currentPosition++;
            }
        } else {
            currentPosition += 2; // Skip '/*'
            while (currentPosition < script.length() && !(script.charAt(currentPosition) == '*' && currentPosition + 1 < script.length() && script.charAt(currentPosition + 1) == '/')) {
                currentPosition++;
            }
            if (currentPosition < script.length()) {
                currentPosition += 2; // Skip '*/'
            }
        }
        String comment = script.substring(start, currentPosition);
        return new Token(TokenType.COMMENT, comment);
    }

    private Token readOperatorOrPunctuation() {
        char ch = script.charAt(currentPosition);
        currentPosition++;
        return new Token(TokenType.OPERATOR, String.valueOf(ch));
    }

    private TokenType stringToKeyword(String identifier) {
        switch (identifier) {
            case "var":
            case "val":
            case "print":
                return TokenType.KEYWORD;
            default:
                return null;
        }
    }

    public static ArrayList<Token> tokenize(String script) {
        Tokenizer tokenizer = new Tokenizer(script);
        ArrayList<Token> tokens = new ArrayList<>();
        while (tokenizer.hasNextToken()) {
            tokens.add(tokenizer.getNextToken());
        }
        return tokens;
    }
}
