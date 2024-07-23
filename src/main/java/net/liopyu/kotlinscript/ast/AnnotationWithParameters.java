package net.liopyu.kotlinscript.ast;

import java.util.Map;

public class AnnotationWithParameters extends ASTNode {
    public final String name;
    public final Map<String, String> parameters;

    public AnnotationWithParameters(String name, Map<String, String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
}