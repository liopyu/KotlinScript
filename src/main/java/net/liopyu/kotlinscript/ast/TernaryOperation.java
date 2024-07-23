package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class TernaryOperation extends ASTNode {
    public final ASTNode condition;
    public final ASTNode trueExpr;
    public final ASTNode falseExpr;

    public TernaryOperation(ASTNode condition, ASTNode trueExpr, ASTNode falseExpr) {
        this.condition = condition;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}