package net.liopyu.kotlinscript.token;

public class TokenPos {
    private final int line;
    private final int column;

    public TokenPos(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "Line: " + line + ", Column: " + column;
    }
}
