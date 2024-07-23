package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.util.ParseException;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class LiteralNode extends ASTNode {
    public Object value;

    public LiteralNode() {
        // Default constructor
    }

    public LiteralNode(Object value) {
        this.value = value;
    }

    @Override
    public void parse(ParserContext context) {
        Token token = context.advance();
        switch (token.type) {
            case NUMBER:
                value = Integer.parseInt(token.value);
                break;
            case STRING:
                value = token.value;
                break;
            default:
                throw new ParseException(token, "Unexpected literal type");
        }
    }

    @Override
    public Object eval(Scope scope) {
        return value;
    }

    @Override
    public String toString() {
        return String.format("LiteralNode(value=%s)", value);
    }
}