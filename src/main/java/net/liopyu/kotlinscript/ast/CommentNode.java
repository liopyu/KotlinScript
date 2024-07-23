package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parsable;
import net.liopyu.kotlinscript.util.Parser;

public class CommentNode extends ASTNode implements Parsable {
    private String comment;

    public CommentNode() {}

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

    @Override
    public ASTNode parse(Parser parser) {
        Token commentToken = parser.consume(TokenType.COMMENT);
        this.comment = commentToken.getValue();
        return this;
    }
}