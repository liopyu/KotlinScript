package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.Parsable;

public abstract class ASTNode implements Parsable {
    public abstract void accept(ASTVisitor visitor);
}
