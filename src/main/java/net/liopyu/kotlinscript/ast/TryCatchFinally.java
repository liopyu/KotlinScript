package net.liopyu.kotlinscript.ast;

import java.util.List;

public class TryCatchFinally extends ASTNode {
    public final List<ASTNode> tryBlock;
    public final List<ASTNode> catchBlock;
    public final List<ASTNode> finallyBlock;

    public TryCatchFinally(List<ASTNode> tryBlock, List<ASTNode> catchBlock, List<ASTNode> finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.finallyBlock = finallyBlock;
    }
}