package net.liopyu.kotlinscript.token;


import net.liopyu.kotlinscript.util.ParseException;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private final String code;
    private int index = 0;

    public Tokenizer(String code) {
        this.code = code;
    }



    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (index < code.length()) {
            char current = code.charAt(index);

            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }

            if (Character.isLetter(current)) {
                tokens.add(tokenizeIdentifierOrKeyword());
                continue;
            }

            if (Character.isDigit(current)) {
                tokens.add(tokenizeNumber());
                continue;
            }

            if (current == '"' || current == '\'') {
                tokens.add(tokenizeString());
                continue;
            }

            tokens.add(tokenizeOperatorOrPunctuation());
            index++;
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private Token tokenizeIdentifierOrKeyword() {
        int start = index;
        while (index < code.length() && (Character.isLetterOrDigit(code.charAt(index)) || code.charAt(index) == '_')) {
            index++;
        }
        String identifier = code.substring(start, index);
        TokenType type = TokenType.fromSymbol(identifier);  // Use fromSymbol to determine the right enum
        return new Token(type, identifier);
    }



    private Token tokenizeNumber() {
        int start = index;
        while (index < code.length() && Character.isDigit(code.charAt(index))) {
            index++;
        }
        return new Token(TokenType.NUMBER, code.substring(start, index));
    }

    private Token tokenizeString() {
        char quote = code.charAt(index);
        int start = index;
        index++; // Skip the opening quote

        boolean escaping = false;  // Track if the current character is being escaped

        while (index < code.length()) {
            char current = code.charAt(index);

            if (escaping) {
                escaping = false; // Reset escaping since current character is escaped
            } else if (current == '\\') {
                escaping = true; // Next character is escaped
            } else if (current == quote && !escaping) {
                index++; // Skip the closing quote
                break;  // Correctly exit the loop upon finding the closing quote
            }

            index++;
        }

        if (index <= code.length() && code.charAt(index - 1) == quote) {
            String stringValue = code.substring(start, index); // Include quotes in the token value
            return new Token(TokenType.STRING, stringValue);
        } else {
            throw new ParseException("Unclosed string literal starting at position " + start);
        }
    }

    private Token tokenizeOperatorOrPunctuation() {
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
            case ',': type = TokenType.COMMA; break;
            default: type = TokenType.UNKNOWN; break; // Ensure any unrecognized character is marked unknown
        }
        index++; // Move past the character
        return new Token(type, String.valueOf(ch));
    }

    private boolean isKeyword(String word) {
        for (TokenType type : TokenType.values()) {
            if (type.symbol.equals(word)) {
                return true;  // Assuming all keyword types have a non-empty symbol
            }
        }
        return false;
    }

}