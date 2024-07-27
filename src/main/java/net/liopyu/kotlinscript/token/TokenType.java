package net.liopyu.kotlinscript.token;

public enum TokenType {
    IDENTIFIER("", 0, false),
    NUMBER("", 0, false),
    STRING("", 0, false),
    KEYWORD("", 0, false),
    OPERATOR("\\", 14, true),

    // Operators
    PLUS("+", 14, true),
    MINUS("-", 14, true),
    MULTIPLY("*", 15, true),
    DIVIDE("/", 15, true),
    ASSIGN("=", 1, false),
    EQUAL_EQUAL("==", 10, false),
    BANG_EQUAL("!=", 10, false),
    GREATER(">", 12, false),
    GREATER_EQUAL(">=", 12, false),
    LESS("<", 12, false),
    LESS_EQUAL("<=", 12, false),
    AND("&&", 5, false),
    OR("||", 4, false),
    BANG("!", 3, true),

    // Symbols
    LEFT_PAREN("(", 16, true),
    RIGHT_PAREN(")", 16, true),
    LEFT_BRACE("{", 16, true),
    RIGHT_BRACE("}", 16, true),
    SEMICOLON(";", 0, true),
    COLON(":", 0, true),
    COMMA(",", 0, true),
    QUESTION_MARK("?", 0, true),
    STAR("*", 0, true),
    SLASH("/", 0, true),

    // Keywords
    KEYWORD_VAL("val", 0, false),
    KEYWORD_VAR("var", 0, false),
    KEYWORD_FUN("fun", 0, false),
    KEYWORD_IF("if", 0, false),
    KEYWORD_ELSE("else", 0, false),

    KEYWORD_PRINT("print", 0, false),

    // Miscellaneous
    EOF("", 0, false),
    UNKNOWN("", 0, false),
    COMMENT("", 0, false);

    public final String symbol;
    public final int precedence;
    public final boolean isUnary;

    TokenType(String symbol, int precedence, boolean isUnary) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.isUnary = isUnary;
    }

    public static TokenType fromSymbol(String symbol) {
        switch (symbol) {
            case "val":
                return KEYWORD_VAL;
            case "var":
                return KEYWORD_VAR;
            case "fun":
                return KEYWORD_FUN;
            case "if":
                return KEYWORD_IF;
            case "else":
                return KEYWORD_ELSE;
            case "print":
                return KEYWORD_PRINT;
            default:
                return IDENTIFIER; // Handle as an identifier if not a keyword
        }
    }

}