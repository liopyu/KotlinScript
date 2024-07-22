package net.liopyu.kotlinscript.token;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Tokenizer {

    public static ArrayList<Token> tokenize(String script) {
        ArrayList<Token> tokens = new ArrayList<>();
        Pattern tokenPatterns = Pattern.compile(
                "(?<KEYWORD>\\b(var|val|print)\\b)|" + // Keywords
                        "(?<IDENTIFIER>[a-zA-Z_][a-zA-Z0-9_]*)|" + // Identifiers
                        "(?<NUMBER>\\b\\d+\\.\\d+|\\b\\d+\\b)|" + // Numbers (including floating-point numbers)
                        "(?<STRING>\"[^\"]*\")|" + // Strings
                        "(?<OPERATOR>[=])|" + // Operators
                        "(?<PARENTHESIS>[()])|" + // Parentheses
                        "(?<WHITESPACE>\\s+)|" + // Whitespace
                        "(?<OTHER>.)" // Any other character
        );
        Matcher matcher = tokenPatterns.matcher(script);

        while (matcher.find()) {
            if (matcher.group("KEYWORD") != null) {
                tokens.add(new Token(TokenType.KEYWORD, matcher.group("KEYWORD")));
            } else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, matcher.group("IDENTIFIER")));
            } else if (matcher.group("NUMBER") != null) {
                tokens.add(new Token(TokenType.NUMBER, matcher.group("NUMBER")));
            } else if (matcher.group("STRING") != null) {
                tokens.add(new Token(TokenType.STRING, matcher.group("STRING")));
            } else if (matcher.group("OPERATOR") != null) {
                tokens.add(new Token(TokenType.OPERATOR, matcher.group("OPERATOR")));
            } else if (matcher.group("PARENTHESIS") != null) {
                tokens.add(new Token(TokenType.PARENTHESIS, matcher.group("PARENTHESIS")));
            } else if (matcher.group("WHITESPACE") != null) {
                // Ignore whitespace
            } else if (matcher.group("OTHER") != null) {
                tokens.add(new Token(TokenType.OTHER, matcher.group("OTHER")));
            }
        }
        return tokens;
    }
}