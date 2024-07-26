package net.liopyu.kotlinscript.ast.astinterface;

import net.liopyu.kotlinscript.ast.AstStringBuilder;

public interface AppendableAst {
    void append(AstStringBuilder builder);
}