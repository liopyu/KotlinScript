package net.liopyu.kotlinscript.token;

public class Token {
    private TokenType type;
    private String value;
    private TokenPos pos;

    public Token(TokenType type, String value, TokenPos pos) {
        this.type = type;
        this.value = value;
        this.pos = pos;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public TokenPos getPos() {
        return new TokenPos(this.pos.getLine(), this.pos.getColumn());
    }

    @Override
    public String toString() {
        return "Token{" + "type=" + type + ", value='" + value + '\'' + ", pos=" + pos + '}';
    }
}