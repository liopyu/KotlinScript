package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class CaseClause extends ASTNode {
    public final ASTNode value;
    public final List<ASTNode> statements;

    public CaseClause(ASTNode value, List<ASTNode> statements) {
        this.value = value;
        this.statements = statements;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}