package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends ASTNode {
    public List<ASTNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public BlockNode(List<ASTNode> statements) {
        this.statements = statements;
    }

    @Override
    public void parse(ParserContext context) {
        context.consume(TokenType.LEFT_BRACE, "Expect '{' to start block.");
        while (!context.check(TokenType.RIGHT_BRACE) && !context.isAtEnd()) {
            ASTNode statement = context.parseStatement();
            statements.add(statement);
        }
        context.consume(TokenType.RIGHT_BRACE, "Expect '}' to end block.");
    }

    @Override
    public Object eval(Scope scope) {
        Scope blockScope = new Scope(scope);
        for (ASTNode statement : statements) {
            statement.eval(blockScope);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Block(statements=%s)", statements);
    }
}