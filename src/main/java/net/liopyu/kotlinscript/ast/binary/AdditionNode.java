package net.liopyu.kotlinscript.ast.binary;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Scope;

public class AdditionNode extends AstBinary {

    @Override
    public ASTNode parse(Parser parser) {
        this.setLeft(parser.parsePrimaryExpression());
        Token operatorToken = parser.consume(TokenType.OPERATOR);
        this.setRight(parser.parsePrimaryExpression());
        this.setPos(operatorToken.getPos());
        return this;
    }
    @Override
    public String getOperator() {
        return "+";
    }
    @Override
    public void appendSymbol(StringBuilder builder) {
        builder.append('+');
    }

    @Override
    public Object eval(Scope scope) {
        var l = scope.eval(left);
        var r = scope.eval(right);

        if (l instanceof CharSequence || l instanceof Character || r instanceof CharSequence || r instanceof Character) {
            var sb = new StringBuilder();
            scope.asString(l, sb, false);
            scope.asString(r, sb, false);
            return sb.toString();
        } else if (l instanceof Number && r instanceof Number) {
            return ((Number) l).doubleValue() + ((Number) r).doubleValue();
        }
        return null;
    }

    @Override
    public double evalDouble(Scope scope) {
        return scope.asDouble(left) + scope.asDouble(right);
    }

    @Override
    public Object optimize(Parser parser) {
        super.optimize(parser);

        if (left instanceof Number l && right instanceof Number r) {
            return l.doubleValue() + r.doubleValue();
        } else if (left instanceof CharSequence l && right instanceof CharSequence r) {
            return l.toString() + r;
        }

        return this;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
