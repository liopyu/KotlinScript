package net.liopyu.kotlinscript.ast;

public abstract class ASTNode {
    public abstract void accept(ASTVisitor visitor);
}
