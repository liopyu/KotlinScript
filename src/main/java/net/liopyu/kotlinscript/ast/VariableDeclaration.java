package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;
import net.liopyu.kotlinscript.token.TokenType;

public class VariableDeclaration extends ASTNode {
    public String name;
    public ASTNode initializer;

    public VariableDeclaration() {
        // Default constructor
    }

    public VariableDeclaration(String name, ASTNode initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public void parse(ParserContext context) {
        name = context.consume(TokenType.IDENTIFIER, "Expect variable name.").value;
        context.consume(TokenType.ASSIGN, "Expect '=' after variable name.");
        initializer = context.parseExpression();
        if (context.match(TokenType.SEMICOLON)) {
            context.consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        }
    }

    @Override
    public Object eval(Scope scope) {
        Object value = initializer.eval(scope);
        if (value == null) {
            throw new RuntimeException("Variable initializer evaluated to null");
        }
        String type = value.getClass().getSimpleName();
        scope.declareVariable(name, type);
        scope.setVariable(name, value);
        return value;
    }

    @Override
    public String toString() {
        return String.format("VariableDeclaration(name=%s, initializer=%s)", name, initializer);
    }
}