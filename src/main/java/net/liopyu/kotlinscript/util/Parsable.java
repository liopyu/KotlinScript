package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;

public interface Parsable {
    ASTNode parse(Parser parser);
}
