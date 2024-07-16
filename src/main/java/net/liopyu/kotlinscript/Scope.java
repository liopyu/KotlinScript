package net.liopyu.kotlinscript;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Map<String, Object> variables = new HashMap<>();
    Scope parentScope;

    public Scope(Scope parent) {
        this.parentScope = parent;
    }

    public void declareVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parentScope != null) {
            return parentScope.getVariable(name);
        }
        throw new RuntimeException("Variable not found: " + name); // or return null
    }

    public void setVariable(String name, Object value) {
        if (variables.containsKey(name)) {
            variables.put(name, value);
        } else if (parentScope != null) {
            parentScope.setVariable(name, value);
        } else {
            throw new RuntimeException("Variable not declared: " + name);
        }
    }
}
