package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.ParserContext;
import net.liopyu.kotlinscript.util.Scope;

import java.util.List;

public class ClassDeclaration extends ASTNode {
    public final String name;
    public final String parentClass;
    public final List<ASTNode> members; // Add this field to store class members

    public ClassDeclaration(String name, String parentClass, List<ASTNode> members) {
        this.name = name;
        this.parentClass = parentClass;
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