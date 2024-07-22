package net.liopyu.kotlinscript.util;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Map<String, Object> variables = new HashMap<>();
    private Scope parentScope;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
    }

    public void declareVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parentScope != null) {
            return parentScope.getVariable(name);
        } else {
            throw new VariableNotFoundException("Variable " + name + " not found in the current or parent scopes.");
        }
    }
}