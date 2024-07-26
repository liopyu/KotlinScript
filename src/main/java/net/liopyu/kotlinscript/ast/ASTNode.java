package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.astinterface.AppendableAst;
import net.liopyu.kotlinscript.token.TokenPos;
import net.liopyu.kotlinscript.token.TokenPosSupplier;
import net.liopyu.kotlinscript.util.Parsable;

public abstract class ASTNode implements Parsable, AppendableAst, TokenPosSupplier {
    private TokenPos pos;
    @Override
    public TokenPos getPos() {
        return pos;
    }
    public void setPos(TokenPos pos) {
        this.pos = pos;
    }

    public abstract void accept(ASTVisitor visitor);
}
