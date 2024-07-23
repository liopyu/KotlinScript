package net.liopyu.kotlinscript.ast;

public class CustomOperator extends ASTNode {
    public final String symbol;
    public final String functionName;

    public CustomOperator(String symbol, String functionName) {
        this.symbol = symbol;
        this.functionName = functionName;
    }
}