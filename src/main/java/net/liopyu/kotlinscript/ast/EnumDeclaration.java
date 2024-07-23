package net.liopyu.kotlinscript.ast;

import java.util.List;

public class EnumDeclaration extends ASTNode {
    public final String name;
    public final List<String> values;

    public EnumDeclaration(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }
}