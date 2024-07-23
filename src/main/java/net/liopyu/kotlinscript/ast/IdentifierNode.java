package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

public class IdentifierNode extends ASTNode {
    public final String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public void parse(ParserContext context) {
        // Since the name is now passed in during construction, parsing might adjust to context needs
        // This method might not be needed if the name is always set via the constructor
    }

    @Override
    public Object eval(Scope scope) {
        Object value = scope.getVariable(name);
        if (value == null) {
            throw new RuntimeException("Variable '" + name + "' not found in scope.");
        }
        return value;
    }
}