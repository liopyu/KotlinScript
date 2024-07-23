package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class SynchronizedBlock extends ASTNode {
    public final ASTNode expression;
    public final List<ASTNode> body;

    public SynchronizedBlock(ASTNode expression, List<ASTNode> body) {
        this.expression = expression;
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