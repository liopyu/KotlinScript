package net.liopyu.kotlinscript.ast.reserved;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.astinterface.Value;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class CommentNode extends ASTNode implements Parsable, Value {
    private String comment;

    public CommentNode(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getValue() {
        return comment;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
    public CommentNode parse(Parser parser) {
        Token commentToken = parser.tokens.get(parser.currentTokenIndex);
        parser.currentTokenIndex++; // Skip the comment\
        this.setComment(commentToken.getValue());
        return this;
    }

    @Override
    public void append(AstStringBuilder builder) {

    }
}