package net.liopyu.kotlinscript.token;

import java.util.ArrayList;
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
        int start = currentPosition + 1; // Start after the opening quote
        currentPosition++; // Move past the opening quote

        while (currentPosition < script.length() && script.charAt(currentPosition) != quote) {
            if (script.charAt(currentPosition) == '\\') { // Handle escape sequences
                currentPosition++; // Skip the escape character
            }
            currentPosition++;
        }

        String string = script.substring(start, currentPosition); // Extract the string without the quotes
        currentPosition++; // Move past the closing quote

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
        currentPosition++;  // Advance the position past the character

        switch (ch) {
            case '(':
                return new Token(TokenType.LEFT_PAREN, String.valueOf(ch));
            case ')':
                return new Token(TokenType.RIGHT_PAREN, String.valueOf(ch));
            case '{':
                return new Token(TokenType.LEFT_BRACE, String.valueOf(ch));
            case '}':
                return new Token(TokenType.RIGHT_BRACE, String.valueOf(ch));
            case ':':
                return new Token(TokenType.COLON, String.valueOf(ch));
            case ',':
                return new Token(TokenType.COMMA, String.valueOf(ch));
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '&':
            case '|':
            case '^':
                return new Token(TokenType.OPERATOR, String.valueOf(ch));  // Handle arithmetic and bitwise operators
            default:
                // If the character is not recognized, you might want to handle it as an error or as an 'other' type
                return new Token(TokenType.OTHER, String.valueOf(ch));
        }
    }


    private TokenType stringToKeyword(String identifier) {
        switch (identifier) {
            case "var":
                return TokenType.KEYWORD;
            case "print":
                return TokenType.KEYWORD;
            case "fun":
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