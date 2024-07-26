package net.liopyu.kotlinscript.ast.astinterface;

import net.liopyu.kotlinscript.util.Scope;

public interface Evaluable {
    Object eval(Scope scope);
    double evalDouble(Scope scope);
}
