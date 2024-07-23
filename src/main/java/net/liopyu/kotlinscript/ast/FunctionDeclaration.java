package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class FunctionDeclaration extends ASTNode {
    public final String name;
    public final List<String> parameters;
    public final List<ASTNode> body;
    public final String returnType; // Add this field

    public FunctionDeclaration(String name, List<String> parameters, List<ASTNode> body, String returnType) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.returnType = returnType; // Initialize this field
    }

    @Override
    public String toString() {
        return String.format("FunctionDeclaration(name=%s, parameters=%s, body=%s, returnType=%s)", name, parameters, body, returnType);
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}