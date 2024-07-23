package net.liopyu.kotlinscript.token;


public enum TokenType {
    KEYWORD(TokenKind.KEYWORD, "keyword"),
    IDENTIFIER(TokenKind.IDENTIFIER, "identifier"),
    NUMBER(TokenKind.LITERAL, "number"),
    STRING(TokenKind.LITERAL, "string"),
    OPERATOR(TokenKind.OPERATOR, "operator"),
    PARENTHESIS(TokenKind.PARENTHESIS, "parenthesis"),
    LEFT_PAREN(TokenKind.PARENTHESIS, "("),
    RIGHT_PAREN(TokenKind.PARENTHESIS, ")"),
    LEFT_BRACE(TokenKind.BRACES, "{"),
    RIGHT_BRACE(TokenKind.BRACES, "}"),
    COLON(TokenKind.SEPARATOR, ":"),
    COMMA(TokenKind.OPERATOR, ",", 0, false),  // Add this line for comma
    WHITESPACE(TokenKind.WHITESPACE, "whitespace"),
    COMMENT(TokenKind.COMMENT, "comment"),
    OTHER(TokenKind.OTHER, "other");

    private final TokenKind kind;
    private final String name;
    private final int precedence;
    private final boolean isLeftAssociative;

    TokenType(final TokenKind kind, final String name) {
        this(kind, name, 0, false);
    }

    TokenType(final TokenKind kind, final String name, final int precedence, final boolean isLeftAssociative) {
        this.kind = kind;
        this.name = name;
        this.precedence = precedence;
        this.isLeftAssociative = isLeftAssociative;
    }

    public boolean needsParens(final TokenType other, final boolean isLeft) {
        return other.precedence != 0 &&
                (precedence > other.precedence ||
                        precedence == other.precedence && isLeftAssociative && !isLeft);
    }

    public boolean isOperator(final boolean noIn) {
        return kind == TokenKind.OPERATOR && precedence != 0;
    }

    public int getLength() {
        return name.length();
    }

    public String getName() {
        return name;
    }

    public TokenKind getKind() {
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
        return name;
    }

    public enum TokenKind {
        KEYWORD,
        IDENTIFIER,
        LITERAL,
        OPERATOR,
        PARENTHESIS,
        WHITESPACE,
        BRACES,
        COMMENT,
        SEPARATOR,
        OTHER
    }
}