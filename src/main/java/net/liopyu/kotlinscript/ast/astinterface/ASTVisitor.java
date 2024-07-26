package net.liopyu.kotlinscript.ast.astinterface;

import net.liopyu.kotlinscript.ast.BlockNode;
import net.liopyu.kotlinscript.ast.binary.*;
import net.liopyu.kotlinscript.ast.reserved.CommentNode;
import net.liopyu.kotlinscript.ast.NumericLiteralNode;
import net.liopyu.kotlinscript.ast.StringLiteralNode;
import net.liopyu.kotlinscript.ast.expression.FunctionDeclarationNode;
import net.liopyu.kotlinscript.ast.reserved.IdentifierNode;
import net.liopyu.kotlinscript.ast.reserved.PrintNode;
import net.liopyu.kotlinscript.ast.expression.VariableDeclarationNode;

public interface ASTVisitor {
    void visit(VariableDeclarationNode node);
    void visit(PrintNode node);
    void visit(BlockNode node);
    void visit(CommentNode node);
    void visit(FunctionDeclarationNode node);
    void visit(NumericLiteralNode node);
    void visit(StringLiteralNode node);
    void visit(IdentifierNode node);
    void visit(AdditionNode node);
    void visit(SubtractionNode node);
    void visit(MultiplicationNode node);
    void visit(DivisionNode node);
    void visit(AstBinary node);
}
