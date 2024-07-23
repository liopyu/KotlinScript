package net.liopyu.kotlinscript.token;


import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final String[] KEYWORDS = {
            "val", "var", "fun", "if", "else", "print"
    };

    public static class Token {
        public final String type;
        public final String value;

        public Token(String type, String value) {
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
                int start = index;
                while (index < length && Character.isLetterOrDigit(code.charAt(index))) {
                    index++;
                }
                String word = code.substring(start, index);
                if (isKeyword(word)) {
                    tokens.add(new Token("KEYWORD", word));
                } else {
                    tokens.add(new Token("IDENTIFIER", word));
                }
                continue;
            }

            if (Character.isDigit(current)) {
                int start = index;
                while (index < length && Character.isDigit(code.charAt(index))) {
                    index++;
                }
                tokens.add(new Token("NUMBER", code.substring(start, index)));
                continue;
            }

            switch (current) {
                case '+': case '-': case '*': case '/':
                case '=': case '(': case ')': case '{':
                case '}': case ';': case ':':
                    tokens.add(new Token("SYMBOL", Character.toString(current)));
                    index++;
                    break;
                case '"':
                    int start = index;
                    index++;
                    while (index < length && code.charAt(index) != '"') {
                        index++;
                    }
                    index++;
                    tokens.add(new Token("STRING", code.substring(start, index)));
                    break;
                default:
                    index++;
            }
        }
        return tokens;
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