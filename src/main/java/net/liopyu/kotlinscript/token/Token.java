package net.liopyu.kotlinscript.token;

public class Token {
    private final TokenType type;
    private final String value;
    private final TokenType.TokenKind kind;
    private final int precedence;
    private final boolean isLeftAssociative;

    public Token(TokenType type, String value) {
        this(type, value, type.getKind(), type.getPrecedence(), type.isLeftAssociative());
    }

    public Token(TokenType type, String value, TokenType.TokenKind kind, int precedence, boolean isLeftAssociative) {
        this.type = type;
        this.value = value;
        this.kind = kind;
        this.precedence = precedence;
        this.isLeftAssociative = isLeftAssociative;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public TokenType.TokenKind getKind() {
        return kind;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isLeftAssociative() {
        return isLeftAssociative;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                ", kind=" + kind +
                ", precedence=" + precedence +
                ", isLeftAssociative=" + isLeftAssociative +
                '}';
    }
}
