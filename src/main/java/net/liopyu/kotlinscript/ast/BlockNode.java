package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.Scope;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends ASTNode {
    private List<ASTNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public void addStatement(ASTNode statement) {
        statements.add(statement);
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}