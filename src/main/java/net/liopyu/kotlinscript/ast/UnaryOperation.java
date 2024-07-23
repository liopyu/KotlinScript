package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class UnaryOperation extends ASTNode {
    public final String operator;
    public final ASTNode right;

    public UnaryOperation(String operator, ASTNode right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("UnaryOperation(operator=%s, right=%s)", operator, right);
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}