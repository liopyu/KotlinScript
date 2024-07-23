package net.liopyu.kotlinscript.ast;

import java.util.List;

public class CaseClause extends ASTNode {
    public final ASTNode value;
    public final List<ASTNode> statements;

    public CaseClause(ASTNode value, List<ASTNode> statements) {
        this.value = value;
        this.statements = statements;
    }
}