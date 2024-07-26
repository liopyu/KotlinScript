package net.liopyu.kotlinscript.ast.astinterface;

import net.liopyu.kotlinscript.util.Parser;

public interface Optimizable {
    Object optimize(Parser parser);
}
