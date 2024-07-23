package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.ArrayList;
import java.util.List;

public class IfStatementNode extends ASTNode {
    public ASTNode condition;
    public List<ASTNode> thenBranch;
    public List<ASTNode> elseBranch;

    public IfStatementNode() {
        this.thenBranch = new ArrayList<>();
        this.elseBranch = new ArrayList<>();
    }

    @Override
    public void parse(ParserContext context) {
        context.consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        condition = context.parseExpression();
        context.consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        thenBranch = parseBlock(context);

        if (context.match(TokenType.KEYWORD_ELSE)) {
            elseBranch = parseBlock(context);
        }
    }

    private List<ASTNode> parseBlock(ParserContext context) {
        BlockNode block = new BlockNode();
        block.parse(context);
        return block.statements;
    }

    @Override
    public Object eval(Scope scope) {
        if ((boolean) condition.eval(scope)) {
            for (ASTNode statement : thenBranch) {
                statement.eval(scope);
            }
        } else if (elseBranch != null) {
            for (ASTNode statement : elseBranch) {
                statement.eval(scope);
            }
        }
        return null;
    }
}