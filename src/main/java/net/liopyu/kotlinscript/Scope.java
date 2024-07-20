package net.liopyu.kotlinscript;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Object> variables;
    private final Map<String, Boolean> immutabilityMap;
    private final Map<String, Class<?>> variableTypes;
    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.immutabilityMap = new HashMap<>();
        this.variableTypes = new HashMap<>();
    }
    public boolean isVariableDefined(String name) {
        if (variables.containsKey(name)) {
            return true;  // Found in the current scope
        } else if (parent != null) {
            return parent.isVariableDefined(name);  // Recursively check in parent scopes
        }
        return false;  // Not found in any scope
    }
    public void defineVariable(String name, Object value, boolean isImmutable, Class<?> type) {
        if (isVariableDefined(name)) {
            throw new RuntimeException("Variable '" + name + "' already defined in this or a parent scope");
        }
        variables.put(name, value);
        immutabilityMap.put(name, isImmutable);
        variableTypes.put(name, type);
    }

    public Object getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parent != null) {
            return parent.getVariable(name); // Recursively check parent scopes
        } else {
            throw new RuntimeException("Variable not found: " + name);
        }
    }
    public boolean hasVariable(String name) {
        if (variables.containsKey(name)) {
            return true;
        } else if (parent != null) {
            return parent.hasVariable(name); // Recursively check parent scopes
        }
        return false;
    }
    public Class<?> getVariableType(String name) {
        if (variableTypes.containsKey(name)) {
            return variableTypes.get(name);
        } else if (parent != null) {
            return parent.getVariableType(name); // Recursively check parent scopes for type
        } else {
            throw new RuntimeException("Type not found for variable: " + name);
        }
    }

    public void setVariable(String name, Object value) {
        if (immutabilityMap.getOrDefault(name, false)) {
            throw new RuntimeException("Cannot modify immutable variable: " + name);
        }
        if (variables.containsKey(name)) {
            variables.put(name, value);
        } else if (parent != null) {
            parent.setVariable(name, value);
        } else {
            throw new RuntimeException("Variable not defined: " + name);
        }
    }

    public void defineVariable(String name, Object value) {
        variables.put(name, value);
    }
}
