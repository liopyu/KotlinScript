package net.liopyu.kotlinscript.token;
import org.openjdk.nashorn.internal.parser.TokenKind;

import java.util.Locale;

import static net.liopyu.kotlinscript.token.TokenType.TokenKind.SPECIAL;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.UNARY;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.BINARY;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.BRACKET;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.KEYWORD;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.IR;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.LITERAL;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.FUTURE;
import static net.liopyu.kotlinscript.token.TokenType.TokenKind.FUTURESTRICT;


public enum TokenType {
    ERROR                (SPECIAL,  null),
    EOF                  (SPECIAL,  null),
    EOL                  (SPECIAL,  null),
    COMMENT              (SPECIAL,  null),
    FLOATING            (LITERAL,   null),
    STRING         (LITERAL,  null),
    IDENTIFIER         (LITERAL,  null),
    OPERATOR         (LITERAL,  null),
    NOT            (TokenKind.UNARY,   "!",    14, false),
    NE             (TokenKind.BINARY,  "!=",    9, true),
    NE_STRICT      (TokenKind.BINARY,  "!==",   9, true),
    MOD            (TokenKind.BINARY,  "%",    13, true),
    ASSIGN_MOD     (TokenKind.BINARY,  "%=",    2, false),
    BIT_AND        (TokenKind.BINARY,  "&",     8, true),
    AND            (TokenKind.BINARY,  "&&",    5, true),
    ASSIGN_BIT_AND (TokenKind.BINARY,  "&=",    2, false),
    LPAREN         (TokenKind.BRACKET, "(",    16, true),
    RPAREN         (TokenKind.BRACKET, ")",     0, true),
    MUL            (TokenKind.BINARY,  "*",    13, true),
    ASSIGN_MUL     (TokenKind.BINARY,  "*=",    2, false),
    POS            (TokenKind.UNARY,   "+",    14, false),
    ADD            (TokenKind.BINARY,  "+",    12, true),
    INCPREFIX      (TokenKind.UNARY,   "++",   15, true),
    ASSIGN_ADD     (TokenKind.BINARY,  "+=",    2, false),
    COMMARIGHT     (TokenKind.BINARY,  ",",     1, true),
    NEG            (TokenKind.UNARY,   "-",    14, false),
    SUB            (TokenKind.BINARY,  "-",    12, true),
    DECPREFIX      (TokenKind.UNARY,   "--",   15, true),
    ASSIGN_SUB     (TokenKind.BINARY,  "-=",    2, false),
    PERIOD         (TokenKind.BRACKET, ".",    17, true),
    DIV            (TokenKind.BINARY,  "/",    13, true),
    ASSIGN_DIV     (TokenKind.BINARY,  "/=",    2, false),
    COLON          (TokenKind.BINARY,  ":"),
    SEMICOLON      (TokenKind.BINARY,  ";"),
    LT             (TokenKind.BINARY,  "<",    10, true),
    SHL            (TokenKind.BINARY,  "<<",   11, true),
    ASSIGN_SHL     (TokenKind.BINARY,  "<<=",   2, false),
    LE             (TokenKind.BINARY,  "<=",   10, true),
    ASSIGN         (TokenKind.BINARY,  "=",     2, false),
    EQ             (TokenKind.BINARY,  "==",    9, true),
    EQ_STRICT      (TokenKind.BINARY,  "===",   9, true),
    ARROW          (TokenKind.BINARY,  "=>",    2, true),
    GT             (TokenKind.BINARY,  ">",    10, true),
    GE             (TokenKind.BINARY,  ">=",   10, true),
    SAR            (TokenKind.BINARY,  ">>",   11, true),
    ASSIGN_SAR     (TokenKind.BINARY,  ">>=",   2, false),
    SHR            (TokenKind.BINARY,  ">>>",  11, true),
    ASSIGN_SHR     (TokenKind.BINARY,  ">>>=",  2, false),
    TERNARY        (TokenKind.BINARY,  "?",     3, false),
    LBRACKET       (TokenKind.BRACKET, "[",    17, true),
    RBRACKET       (TokenKind.BRACKET, "]",     0, true),
    BIT_XOR        (TokenKind.BINARY,  "^",     7, true),
    ASSIGN_BIT_XOR (TokenKind.BINARY,  "^=",    2, false),
    LBRACE         (TokenKind.BRACKET,  "{"),
    BIT_OR         (TokenKind.BINARY,  "|",     6, true),
    ASSIGN_BIT_OR  (TokenKind.BINARY,  "|=",    2, false),
    OR             (TokenKind.BINARY,  "||",    4, true),
    RBRACE         (TokenKind.BRACKET, "}"),
    BIT_NOT        (TokenKind.UNARY,   "~",     14, false),
    ELLIPSIS       (TokenKind.UNARY,   "..."),
    PRINT            (KEYWORD,  "print"),
    VAR            (KEYWORD,  "var"),
    FUN            (KEYWORD,  "fun"),
    VAL            (KEYWORD,  "val");
    private final TokenKind kind;
    private final String name;
    private final int precedence;
    private final boolean isLeftAssociative;
    private TokenType next;
    private static final TokenType[] values;

    TokenType(final TokenKind kind, final String name) {
        this.kind = kind;
        this.name = name;
        this.precedence = 0;
        this.isLeftAssociative = false;
        this.next = null;
    }

    TokenType(final TokenKind kind, final String name, final int precedence, final boolean isLeftAssociative) {
        this.kind = kind;
        this.name = name;
        this.precedence = precedence;
        this.isLeftAssociative = isLeftAssociative;
        this.next = null;
    }

    public boolean needsParens(final TokenType other, final boolean isLeft) {
        return other.precedence != 0 &&
                (precedence > other.precedence ||
                        precedence == other.precedence && isLeftAssociative && !isLeft);
    }

    public boolean isOperator(final boolean noIn) {
        return kind == TokenKind.BINARY && precedence != 0;
    }

    public int getLength() {
        return name.length();
    }

    public String getName() {
        return name;
    }

    public String getNameOrType() {
        return name == null ? super.name().toLowerCase(Locale.ENGLISH) : name;
    }

    public TokenType getNext() {
        return next;
    }

    public void setNext(final TokenType next) {
        this.next = next;
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

    boolean startsWith(final char c) {
        return name != null && name.length() > 0 && name.charAt(0) == c;
    }

    static TokenType[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return getNameOrType();
    }

    static {
        values = TokenType.values();
    }
    public enum TokenKind {
        /** Error, EOF, EOL...*/
        SPECIAL,
        /** Unary operators. */
        UNARY,
        /** Binary operators. */
        BINARY,
        /** [] () {} */
        BRACKET,
        /** String recognized as a keyword. */
        KEYWORD,
        /** Literal constant. */
        LITERAL,
        /** IR only token. */
        IR,
        /** Token reserved for future usage. */
        FUTURE,
        /** Token reserved for future in strict mode. */
        FUTURESTRICT
    }
}



