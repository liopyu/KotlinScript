package net.liopyu.kotlinscript.util;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, String> variableTypes;
    private final Map<String, Object> variableValues;
    private final Map<String, String> functions;
    private String currentClass;

    public Scope(Scope parent) {
        this.parent = parent;
        this.variableTypes = new HashMap<>();
        this.variableValues = new HashMap<>();
        this.functions = new HashMap<>();
        this.currentClass = parent != null ? parent.currentClass : null;
    }

    public void declareVariable(String name, String type) {
        variableTypes.put(name, type);
    }

    public String resolveVariableType(String name) {
        if (variableTypes.containsKey(name)) {
            return variableTypes.get(name);
        }
        if (parent != null) {
            return parent.resolveVariableType(name);
        }
        return null;
    }

    public void setVariable(String name, Object value) {
        if (variableTypes.containsKey(name)) {
            variableValues.put(name, value);
        } else if (parent != null) {
            parent.setVariable(name, value);
        } else {
            throw new RuntimeException("Variable not declared: " + name);
        }
    }

    public Object getVariable(String name) {
        if (variableValues.containsKey(name)) {
            return variableValues.get(name);
        }
        if (parent != null) {
            return parent.getVariable(name);
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