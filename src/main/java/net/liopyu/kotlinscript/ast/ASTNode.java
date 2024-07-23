package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParseException;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public abstract class ASTNode {
    public abstract void parse(ParserContext context);
    public abstract Object eval(Scope scope);

    protected void consume(ParserContext context, TokenType expectedType, String errorMessage) {
        if (!context.match(expectedType)) {
            throw new ParseException(context.peek(), errorMessage);
        }
    }
}