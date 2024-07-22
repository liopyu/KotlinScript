package net.liopyu.kotlinscript.token;

public class Token {
    public enum Type {
        KEYWORD, IDENTIFIER, LITERAL, OPERATOR, PUNCTUATION, WHITESPACE, COMMENT
    }

    public final String text;
    public final Type type;
    public int position;

    public Token(String text, Type type, int position) {
        this.text = text;
        this.type = type;
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("Token{text='%s', type=%s, position=%d}", text, type, position);
    }
}
