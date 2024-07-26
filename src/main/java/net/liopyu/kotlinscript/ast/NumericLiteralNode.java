package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.astinterface.Value;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenPos;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

public class NumericLiteralNode extends ASTNode implements Parsable, Value {
    private double aDouble;

    public NumericLiteralNode() {}

    public NumericLiteralNode(double aDouble, TokenPos pos) {
        this.aDouble = aDouble;
        this.setPos(pos);
    }

    public double getDouble() {
        return aDouble;
    }

    @Override
    public String getValue() {
        return String.valueOf(aDouble);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        Token token = parser.consume(TokenType.FLOATING);
        this.aDouble = Double.parseDouble(token.getValue());
        return this;
    }

    @Override
    public void append(AstStringBuilder builder) {

    }
}