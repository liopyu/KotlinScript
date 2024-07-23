package net.liopyu.kotlinscript.ast;

public class StaticDeclaration extends ASTNode {
    public final ASTNode member;

    public StaticDeclaration(ASTNode member) {
        this.member = member;
    }
}
