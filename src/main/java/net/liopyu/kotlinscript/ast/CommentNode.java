package net.liopyu.kotlinscript.ast;

public class CommentNode extends ASTNode {
    private String comment;

    public CommentNode(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}