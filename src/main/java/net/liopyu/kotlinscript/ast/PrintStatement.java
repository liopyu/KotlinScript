package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class PrintStatement extends ASTNode {
    public ASTNode expression;

    public PrintStatement(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public void parse(ParserContext context) {
        context.consume(TokenType.KEYWORD_PRINT, "Expect 'print' keyword.");
        context.consume(TokenType.LEFT_PAREN, "Expect '(' after 'print'.");
        expression = context.parseExpression();
        context.consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
        context.consume(TokenType.SEMICOLON, "Expect ';' after print statement.");
    }

    @Override
    public Object eval(Scope scope) {
        Object value = expression.eval(scope);
        System.out.println(value);
        return null;
    }
}