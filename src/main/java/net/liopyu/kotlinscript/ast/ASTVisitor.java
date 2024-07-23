package net.liopyu.kotlinscript.ast;

public interface ASTVisitor {
    void visit(VariableDeclarationNode node);
    void visit(PrintNode node);
    void visit(BlockNode node);
    void visit(CommentNode node);
    void visit(FunctionDeclarationNode node);
    void visit(ExpressionNode node);
    void visit(IdentifierNode node);
    void visit(StringLiteralNode node);
    void visit(FunctionCallNode node);
    void visit(NumberLiteralNode node);
    void visit(BinaryExpressionNode node);
}
