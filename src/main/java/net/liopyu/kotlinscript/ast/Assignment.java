package net.liopyu.kotlinscript.ast;


import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class Assignment extends ASTNode {
    public final String name;
    public final ASTNode value;

    public Assignment(String name, ASTNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("Assignment(name=%s, value=%s)", name, value);
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}