package net.liopyu.kotlinscript.util;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, String> variables;
    private final Map<String, String> functions;
    private String currentClass; // Add this field to track the current class

    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.currentClass = parent != null ? parent.currentClass : null;
    }

    public void declareVariable(String name, String type) {
        variables.put(name, type);
    }

    public String resolveVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.resolveVariable(name);
        }
        return null;
    }

    public void declareFunction(String name, String type) {
        functions.put(name, type);
    }

    public String resolveFunction(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        if (parent != null) {
            return parent.resolveFunction(name);
        }
        return null;
    }

    public void setCurrentClass(String className) {
        this.currentClass = className;
    }

    public String resolveClass() {
        return currentClass;
    }
}