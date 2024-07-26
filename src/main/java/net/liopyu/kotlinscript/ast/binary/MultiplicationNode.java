package net.liopyu.kotlinscript.ast.binary;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Scope;

public class MultiplicationNode extends AstBinary {

    @Override
    public void appendSymbol(StringBuilder builder) {
        builder.append('*');
    }
    @Override
    public String getOperator() {
        return "*";
    }
    @Override
    public Object eval(Scope scope) {
        var l = scope.eval(left);
        var r = scope.eval(right);

        if (l instanceof Number && r instanceof Number) {
            return ((Number) l).doubleValue() * ((Number) r).doubleValue();
        }
        return null;
    }

    @Override
    public double evalDouble(Scope scope) {
        return scope.asDouble(left) * scope.asDouble(right);
    }

    @Override
    public Object optimize(Parser parser) {
        super.optimize(parser);

        if (left instanceof Number l && right instanceof Number r) {
            return l.doubleValue() * r.doubleValue();
        }

        return this;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        return null;
    }
}
