package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.ast.astinterface.Evaluable;
import net.liopyu.kotlinscript.ast.astinterface.Optimizable;

public abstract class AstExpression extends ASTNode implements Evaluable, Optimizable {
}
