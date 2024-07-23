package net.liopyu.kotlinscript.token;


import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final String[] KEYWORDS = {
            "val", "var", "fun", "if", "else", "print"
    };

    public static class Token {
        public final TokenType type;
        public final String value;

        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("Token(type=%s, value=%s)", type, value);
        }
    }

    public List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        int length = code.length();
        int index = 0;

        while (index < length) {
            char current = code.charAt(index);

            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }

            if (Character.isLetter(current)) {
                tokens.add(tokenizeIdentifierOrKeyword(code, index));
                index += tokens.get(tokens.size() - 1).value.length();
                continue;
            }

            if (Character.isDigit(current)) {
                tokens.add(tokenizeNumber(code, index));
                index += tokens.get(tokens.size() - 1).value.length();
                continue;
            }

            if (current == '"' || current == '\'') {
                tokens.add(tokenizeString(code, index));
                index += tokens.get(tokens.size() - 1).value.length();
                continue;
            }

            tokens.add(tokenizeOperatorOrPunctuation(code, index));
            index++;
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private Token tokenizeIdentifierOrKeyword(String code, int start) {
        int index = start;
        while (index < code.length() && (Character.isLetterOrDigit(code.charAt(index)) || code.charAt(index) == '_')) {
            index++;
        }
        String identifier = code.substring(start, index);
        TokenType type = isKeyword(identifier) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        return new Token(type, identifier);
    }

    private Token tokenizeNumber(String code, int start) {
        int index = start;
        while (index < code.length() && Character.isDigit(code.charAt(index))) {
            index++;
        }
        return new Token(TokenType.NUMBER, code.substring(start, index));
    }

    private Token tokenizeString(String code, int start) {
        char quote = code.charAt(start);
        int index = start + 1; // Skip the opening quote
        while (index < code.length() && code.charAt(index) != quote) {
            if (code.charAt(index) == '\\' && index + 1 < code.length()) { // Handle escape sequences
                index++; // Skip the escape character
            }
            index++;
        }
        index++; // Skip the closing quote
        return new Token(TokenType.STRING, code.substring(start, index));
    }

    private Token tokenizeOperatorOrPunctuation(String code, int index) {
        char ch = code.charAt(index);
        TokenType type;
        switch (ch) {
            case '+': type = TokenType.PLUS; break;
            case '-': type = TokenType.MINUS; break;
            case '*': type = TokenType.MULTIPLY; break;
            case '/': type = TokenType.DIVIDE; break;
            case '=': type = TokenType.ASSIGN; break;
            case '(': type = TokenType.LEFT_PAREN; break;
            case ')': type = TokenType.RIGHT_PAREN; break;
            case '{': type = TokenType.LEFT_BRACE; break;
            case '}': type = TokenType.RIGHT_BRACE; break;
            case ';': type = TokenType.SEMICOLON; break;
            case ':': type = TokenType.COLON; break;
            // Add cases for other symbols if needed
            default: type = TokenType.UNKNOWN; break; // Unknown symbol
        }
        index++;
        return new Token(type, String.valueOf(ch));
    }

    private boolean isKeyword(String word) {
        for (String keyword : KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }
}