package net.liopyu.kotlinscript.token;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Tokenizer {
    private final String script;
    private int currentPosition;
    private int currentLine;
    private int currentColumn;
    private Token nextToken;
    private TokenPos tokenPos = new TokenPos(currentLine,currentColumn);
    public Tokenizer(String script) {
        this.script = script;
        this.currentPosition = 0;
        this.currentLine = 1;
        this.currentColumn = 1;
        advance(); // Initialize the first token
    }

    public TokenPos getTokenPos() {
        return tokenPos;
    }
    public void setTokenPos(int line, int column) {
        this.tokenPos = new TokenPos(line,column);
    }
    private void advance() {
        nextToken = getToken();
    }

    public boolean hasNextToken() {
        return nextToken != null;
    }

    public Token getNextToken() {
        Token currentToken = nextToken;
        advance();
        return currentToken;
    }

    private void advancePosition() {
        currentPosition++;
        currentColumn++;
    }

    private void advancePosition(char ch) {
        if (ch == '\n') {
            currentLine++;
            currentColumn = 1;
        } else {
            currentColumn++;
        }
        currentPosition++;
    }

    private Token getToken() {
        if (currentPosition >= script.length()) {
            this.setTokenPos(currentLine, currentColumn);
            return null;
        }

        char ch = script.charAt(currentPosition);
        this.setTokenPos(currentLine, currentColumn);

        // Skip whitespace
        while (isWhitespace(ch)) {
            advancePosition(ch);
            if (currentPosition >= script.length()) {
                this.setTokenPos(currentLine, currentColumn);
                return new Token(TokenType.EOF, "", tokenPos);
            }
            ch = script.charAt(currentPosition);
        }

        if (isAlpha(ch) || ch == '_') {
            return readIdentifierOrKeyword(tokenPos);
        } else if (isDigit(ch)) {
            return readNumber(tokenPos);
        } else if (ch == '"' || ch == '\'') {
            return readString(ch, tokenPos);
        } else if (ch == '/' && currentPosition + 1 < script.length() && (script.charAt(currentPosition + 1) == '/' || script.charAt(currentPosition + 1) == '*')) {
            return readComment(tokenPos);
        } else {
            return generateToken(tokenPos);
        }
    }

    private boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    private boolean isAlpha(char ch) {
        return Character.isLetter(ch);
    }

    private boolean isDigit(char ch) {
        return Character.isDigit(ch);
    }

    private static final Pattern FLOATING_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    private Token readIdentifierOrKeyword(TokenPos pos) {
        int start = currentPosition;
        while (currentPosition < script.length() && (isAlpha(script.charAt(currentPosition)) || isDigit(script.charAt(currentPosition)) || script.charAt(currentPosition) == '_')) {
            advancePosition();
        }
        String identifier = script.substring(start, currentPosition);
        TokenType type = getTokenType(identifier);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        return new Token(type, identifier, pos);
    }

    private Token readNumber(TokenPos pos) {
        int start = currentPosition;
        while (currentPosition < script.length() && isDigit(script.charAt(currentPosition))) {
            advancePosition();
        }
        if (currentPosition < script.length() && script.charAt(currentPosition) == '.') {
            advancePosition();
            while (currentPosition < script.length() && isDigit(script.charAt(currentPosition))) {
                advancePosition();
            }
        }
        String number = script.substring(start, currentPosition);
        return new Token(TokenType.FLOATING, number, pos);
    }

    private Token readString(char quote, TokenPos pos) {
        int start = currentPosition;
        advancePosition(); // Skip the opening quote
        while (currentPosition < script.length() && script.charAt(currentPosition) != quote) {
            if (script.charAt(currentPosition) == '\\') { // Handle escape sequences
                advancePosition();
            }
            advancePosition();
        }
        if (currentPosition < script.length()) {
            advancePosition(); // Skip the closing quote
        }
        String string = script.substring(start, currentPosition);
        return new Token(TokenType.STRING, string, pos);
    }

    private Token readComment(TokenPos pos) {
        int start = currentPosition;
        if (script.charAt(currentPosition + 1) == '/') {
            advancePosition();
            advancePosition(); // Skip '//'
            while (currentPosition < script.length() && script.charAt(currentPosition) != '\n') {
                advancePosition();
            }
        } else {
            advancePosition();
            advancePosition(); // Skip '/*'
            while (currentPosition < script.length() && !(script.charAt(currentPosition) == '*' && currentPosition + 1 < script.length() && script.charAt(currentPosition + 1) == '/')) {
                advancePosition();
            }
            if (currentPosition < script.length()) {
                advancePosition();
                advancePosition(); // Skip '*/'
            }
        }
        String comment = script.substring(start, currentPosition);
        return new Token(TokenType.COMMENT, comment, pos);
    }

    public static TokenType getTokenType(String identifier) {
        switch (identifier) {
            // Add cases for all token types
            case "!": return TokenType.NOT;
            case "!=": return TokenType.NE;
            case "!==": return TokenType.NE_STRICT;
            case "%": return TokenType.MOD;
            case "%=": return TokenType.ASSIGN_MOD;
            case "&": return TokenType.BIT_AND;
            case "&&": return TokenType.AND;
            case "&=": return TokenType.ASSIGN_BIT_AND;
            case "(": return TokenType.LPAREN;
            case ")": return TokenType.RPAREN;
            case "*": return TokenType.MUL;
            case "*=": return TokenType.ASSIGN_MUL;
            case "+": return TokenType.ADD;
            case "++": return TokenType.INCPREFIX;
            case "+=": return TokenType.ASSIGN_ADD;
            case ",": return TokenType.COMMARIGHT;
            case "-": return TokenType.SUB;
            case "--": return TokenType.DECPREFIX;
            case "-=": return TokenType.ASSIGN_SUB;
            case ".": return TokenType.PERIOD;
            case "...": return TokenType.ELLIPSIS;
            case "/": return TokenType.DIV;
            case "/=": return TokenType.ASSIGN_DIV;
            case ":": return TokenType.COLON;
            case ";": return TokenType.SEMICOLON;
            case "<": return TokenType.LT;
            case "<<": return TokenType.SHL;
            case "<<=": return TokenType.ASSIGN_SHL;
            case "<=": return TokenType.LE;
            case "=": return TokenType.ASSIGN;
            case "==": return TokenType.EQ;
            case "===": return TokenType.EQ_STRICT;
            case "=>": return TokenType.ARROW;
            case ">": return TokenType.GT;
            case ">=": return TokenType.GE;
            case ">>": return TokenType.SAR;
            case ">>=": return TokenType.ASSIGN_SAR;
            case ">>>": return TokenType.SHR;
            case ">>>=": return TokenType.ASSIGN_SHR;
            case "?": return TokenType.TERNARY;
            case "[": return TokenType.LBRACKET;
            case "]": return TokenType.RBRACKET;
            case "^": return TokenType.BIT_XOR;
            case "^=": return TokenType.ASSIGN_BIT_XOR;
            case "{": return TokenType.LBRACE;
            case "|": return TokenType.BIT_OR;
            case "|=": return TokenType.ASSIGN_BIT_OR;
            case "||": return TokenType.OR;
            case "}": return TokenType.RBRACE;
            case "~": return TokenType.BIT_NOT;
            case "print": return TokenType.PRINT;
            case "var": return TokenType.VAR;
            case "val": return TokenType.VAL;
            default:
                if (FLOATING_PATTERN.matcher(identifier).matches()) {
                    return TokenType.FLOATING;
                }
                return null;
        }
    }

    public static TokenType.TokenKind getTokenKind(String identifier) {
        switch (identifier) {
            case "var":
            case "val":
            case "print":
                return TokenType.TokenKind.KEYWORD;
            case "!":
                return TokenType.TokenKind.UNARY;
            case "!=":
            case "!==":
            case "%":
            case "%=":
            case "&":
            case "&&":
            case "&=":
            case "*":
            case "*=":
            case "+":
            case "++":
            case "+=":
            case ",":
            case "-":
            case "--":
            case "-=":
            case "/":
            case "/=":
            case ":":
            case ";":
            case "<":
            case "<<":
            case "<<=":
            case "<=":
            case "=":
            case "==":
            case "===":
            case "=>":
            case ">":
            case ">=":
            case ">>":
            case ">>=":
            case ">>>":
            case ">>>=":
            case "?":
            case "^":
            case "^=":
            case "|":
            case "||":
            case "|=":
                return TokenType.TokenKind.BINARY;
            case "(":
            case ")":
            case "[":
            case "]":
            case "{":
            case "}":
            case ".":
            case "...":
                return TokenType.TokenKind.BRACKET;
            case "~":
                return TokenType.TokenKind.UNARY;
            default:
                return null;
        }
    }

    public Token generateToken(TokenPos pos) {
        char ch = script.charAt(currentPosition);
        advancePosition();  // Advance the position past the character

        switch (ch) {
            case '(':
                return new Token(TokenType.LPAREN, String.valueOf(ch), pos);
            case ')':
                return new Token(TokenType.RPAREN, String.valueOf(ch), pos);
            case '{':
                return new Token(TokenType.LBRACE, String.valueOf(ch), pos);
            case '}':
                return new Token(TokenType.RBRACE, String.valueOf(ch), pos);
            case ':':
                return new Token(TokenType.COLON, String.valueOf(ch), pos);
            case ',':
                return new Token(TokenType.COMMARIGHT, String.valueOf(ch), pos);
            case '+':
                return new Token(TokenType.ADD, String.valueOf(ch), pos);
            case '-':
                return new Token(TokenType.SUB, String.valueOf(ch), pos);
            case '*':
                return new Token(TokenType.MUL, String.valueOf(ch), pos);
            case '/':
                return new Token(TokenType.DIV, String.valueOf(ch), pos);
            case '%':
                return new Token(TokenType.MOD, String.valueOf(ch), pos);
            case '!':
                if (peekNextChar() == '=') {
                    advancePosition();
                    if (peekNextChar() == '=') {
                        advancePosition();
                        return new Token(TokenType.NE_STRICT, "!==", pos);
                    }
                    return new Token(TokenType.NE, "!=", pos);
                }
                return new Token(TokenType.NOT, String.valueOf(ch), pos);
            case '&':
                if (peekNextChar() == '&') {
                    advancePosition();
                    return new Token(TokenType.AND, "&&", pos);
                }
                if (peekNextChar() == '=') {
                    advancePosition();
                    return new Token(TokenType.ASSIGN_BIT_AND, "&=", pos);
                }
                return new Token(TokenType.BIT_AND, String.valueOf(ch), pos);
            case '|':
                if (peekNextChar() == '|') {
                    advancePosition();
                    return new Token(TokenType.OR, "||", pos);
                }
                if (peekNextChar() == '=') {
                    advancePosition();
                    return new Token(TokenType.ASSIGN_BIT_OR, "|=", pos);
                }
                return new Token(TokenType.BIT_OR, String.valueOf(ch), pos);
            case '^':
                if (peekNextChar() == '=') {
                    advancePosition();
                    return new Token(TokenType.ASSIGN_BIT_XOR, "^=", pos);
                }
                return new Token(TokenType.BIT_XOR, String.valueOf(ch), pos);
            case '<':
                if (peekNextChar() == '<') {
                    advancePosition();
                    if (peekNextChar() == '=') {
                        advancePosition();
                        return new Token(TokenType.ASSIGN_SHL, "<<=", pos);
                    }
                    return new Token(TokenType.SHL, "<<", pos);
                }
                if (peekNextChar() == '=') {
                    advancePosition();
                    return new Token(TokenType.LE, "<=", pos);
                }
                return new Token(TokenType.LT, String.valueOf(ch), pos);
            case '>':
                if (peekNextChar() == '>') {
                    advancePosition();
                    if (peekNextChar() == '>') {
                        advancePosition();
                        if (peekNextChar() == '=') {
                            advancePosition();
                            return new Token(TokenType.ASSIGN_SHR, ">>>=", pos);
                        }
                        return new Token(TokenType.SHR, ">>>", pos);
                    }
                    if (peekNextChar() == '=') {
                        advancePosition();
                        return new Token(TokenType.ASSIGN_SAR, ">>=", pos);
                    }
                    return new Token(TokenType.SAR, ">>", pos);
                }
                if (peekNextChar() == '=') {
                    advancePosition();
                    return new Token(TokenType.GE, ">=", pos);
                }
                return new Token(TokenType.GT, String.valueOf(ch), pos);
            case '=':
                if (peekNextChar() == '=') {
                    advancePosition();
                    if (peekNextChar() == '=') {
                        advancePosition();
                        return new Token(TokenType.EQ_STRICT, "===", pos);
                    }
                    return new Token(TokenType.EQ, "==", pos);
                }
                return new Token(TokenType.ASSIGN, String.valueOf(ch), pos);
            case '.':
                if (peekNextChar() == '.' && peekNextNextChar() == '.') {
                    advancePosition();
                    advancePosition();
                    return new Token(TokenType.ELLIPSIS, "...", pos);
                }
                return new Token(TokenType.PERIOD, String.valueOf(ch), pos);
            case '?':
                return new Token(TokenType.TERNARY, String.valueOf(ch), pos);
            case ';':
                return new Token(TokenType.SEMICOLON, String.valueOf(ch), pos);
            case '[':
                return new Token(TokenType.LBRACKET, String.valueOf(ch), pos);
            case ']':
                return new Token(TokenType.RBRACKET, String.valueOf(ch), pos);
            case '~':
                return new Token(TokenType.BIT_NOT, String.valueOf(ch), pos);
            default:
                throw new IllegalArgumentException("Unexpected character: " + ch);
        }
    }

    private char peekNextChar() {
        if (currentPosition + 1 < script.length()) {
            return script.charAt(currentPosition + 1);
        }
        return '\0';
    }

    private char peekNextNextChar() {
        if (currentPosition + 2 < script.length()) {
            return script.charAt(currentPosition + 2);
        }
        return '\0';
    }

    public static ArrayList<Token> tokenize(String script) {
        Tokenizer tokenizer = new Tokenizer(script);
        ArrayList<Token> tokens = new ArrayList<>();
        while (tokenizer.hasNextToken()) {
            tokens.add(tokenizer.getNextToken());
        }
        return tokens;
    }
}