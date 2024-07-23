package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class ForStatement extends ASTNode {
    public final ASTNode initializer;
    public final ASTNode condition;
    public final ASTNode increment;
    public final List<ASTNode> body;

    public ForStatement(ASTNode initializer, ASTNode condition, ASTNode increment, List<ASTNode> body) {
        this.initializer = initializer;
        this.condition = condition;
        this.increment = increment;
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