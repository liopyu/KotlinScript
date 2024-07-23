package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class SwitchStatement extends ASTNode {
    public final ASTNode expression;
    public final List<CaseClause> cases;

    public SwitchStatement(ASTNode expression, List<CaseClause> cases) {
        this.expression = expression;
        this.cases = cases;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}