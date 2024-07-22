package net.liopyu.kotlinscript.ast;

public interface ASTVisitor {
    void visit(VariableDeclarationNode node);
    void visit(PrintNode node);
    void visit(BlockNode node);
    void visit(CommentNode node);
}
