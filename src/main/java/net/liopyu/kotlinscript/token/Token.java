package net.liopyu.kotlinscript.token;

public class Token {
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