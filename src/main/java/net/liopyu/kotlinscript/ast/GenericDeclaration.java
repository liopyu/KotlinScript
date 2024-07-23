package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class GenericDeclaration extends ASTNode {
    public final String name;
    public final List<String> typeParameters;
    public final List<ASTNode> members; // Add this field to store class members

    public GenericDeclaration(String name, List<String> typeParameters, List<ASTNode> members) {
        this.name = name;
        this.typeParameters = typeParameters;
        this.members = members;
    }

    @Override
    public void parse(ParserContext context) {

    }

    @Override
    public Object eval(Scope scope) {
        return null;
    }
}