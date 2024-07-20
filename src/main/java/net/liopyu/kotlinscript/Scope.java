package net.liopyu.kotlinscript;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Scope {
    private final Scope parent;
    final Map<String, Object> variables;
    private final Map<String, Boolean> immutabilityMap;
    private final Map<String, Class<?>> variableTypes;
    private final Map<String, Consumer<Scope>> functions;
    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.immutabilityMap = new HashMap<>();
        this.variableTypes = new HashMap<>();
        this.functions = new HashMap<>();
    }
    public void defineFunction(String name, Consumer<Scope> function) {
        if (functions.containsKey(name)) {
            throw new RuntimeException("Function " + name + " already defined in this scope");
        }
        functions.put(name, function);
    }
    public Consumer<Scope> getFunction(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name);
        } else if (parent != null) {
            return parent.getFunction(name); // Check parent scopes recursively
        }
        return null;
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
