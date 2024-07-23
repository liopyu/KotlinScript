package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class StaticDeclaration extends ASTNode {
    public final ASTNode member;

    public StaticDeclaration(ASTNode member) {
        this.member = member;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}
