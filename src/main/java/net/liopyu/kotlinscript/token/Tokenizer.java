package net.liopyu.kotlinscript.token;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private static final String KEYWORD = "\\b(fun|val|var|if|else|for|return|break|continue|class|try|catch|finally|when|is|in)\\b";
    private static final String IDENTIFIER = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";
    private static final String LITERAL = "\\b\\d+\\b|\\b\\d+\\.\\d*\\b|\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'";
    private static final String OPERATOR = "[+\\-*/=]=?|!=|<=|>=|<|>|\\+\\+|\\-\\-|&&|\\|\\|";
    private static final String PUNCTUATION = "[{}(),;\\[\\]]";
    private static final String COMMENT = "//[^\n]*|/\\*.*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            String.join("|", COMMENT, KEYWORD, IDENTIFIER, LITERAL, OPERATOR, PUNCTUATION)
    );

    public static ArrayList<Token> tokenize(String input) {
        ArrayList<Token> tokens = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            String text = matcher.group();
            if (text.startsWith("//") || text.startsWith("/*")) {
                tokens.add(new Token(text, Token.Type.COMMENT, matcher.start()));
            } else if (text.matches(KEYWORD)) {
                tokens.add(new Token(text, Token.Type.KEYWORD, matcher.start()));
            } else if (text.matches(IDENTIFIER)) {
                tokens.add(new Token(text, Token.Type.IDENTIFIER, matcher.start()));
            } else if (text.matches(LITERAL)) {
                tokens.add(new Token(text, Token.Type.LITERAL, matcher.start()));
            } else if (text.matches(OPERATOR)) {
                tokens.add(new Token(text, Token.Type.OPERATOR, matcher.start()));
            } else if (text.matches(PUNCTUATION)) {
                tokens.add(new Token(text, Token.Type.PUNCTUATION, matcher.start()));
            } else if (text.trim().isEmpty()) {
                tokens.add(new Token(text, Token.Type.WHITESPACE, matcher.start()));
            }
        }
        return tokens;
    }
}
