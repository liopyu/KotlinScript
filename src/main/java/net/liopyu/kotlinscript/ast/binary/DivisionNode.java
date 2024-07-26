package net.liopyu.kotlinscript.ast.binary;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Scope;

public class DivisionNode extends AstBinary {

    @Override
    public void appendSymbol(StringBuilder builder) {
        builder.append('/');
    }
    @Override
    public String getOperator() {
        return "/";
    }
    @Override
    public Object eval(Scope scope) {
        var l = scope.eval(left);
        var r = scope.eval(right);

        if (l instanceof Number && r instanceof Number) {
            if (((Number) r).doubleValue() == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return ((Number) l).doubleValue() / ((Number) r).doubleValue();
        }
        throw new ArithmeticException("Invalid division");
    }

    @Override
    public double evalDouble(Scope scope) {
        double divisor = scope.asDouble(right);
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return scope.asDouble(left) / divisor;
    }

    @Override
    public Object optimize(Parser parser) {
        super.optimize(parser);

        if (left instanceof Number l && right instanceof Number r) {
            if (r.doubleValue() == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return l.doubleValue() / r.doubleValue();
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
