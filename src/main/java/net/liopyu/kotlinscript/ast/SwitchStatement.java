package net.liopyu.kotlinscript.ast;

import java.util.List;

public class SwitchStatement extends ASTNode {
    public final ASTNode expression;
    public final List<CaseClause> cases;

    public SwitchStatement(ASTNode expression, List<CaseClause> cases) {
        this.expression = expression;
        this.cases = cases;
    }
}