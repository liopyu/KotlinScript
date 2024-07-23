package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class WhileStatement extends ASTNode {
    public final ASTNode condition;
    public final List<ASTNode> body;

    public WhileStatement(ASTNode condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}