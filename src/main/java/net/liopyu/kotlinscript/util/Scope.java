package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.astinterface.Evaluable;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Map<String, Object> variables = new HashMap<>();
    private final Map<String, ContextUtils.Function> functions;
    private Scope parentScope;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.functions = new HashMap<>();
    }
    public Object eval(Object expr) {
        if (expr instanceof Evaluable) {
            return ((Evaluable) expr).eval(this);
        }
        return expr;
    }
    public double asDouble(Object expr) {
        if (expr instanceof Number) {
            return ((Number) expr).doubleValue();
        }
        // Additional logic for converting other types to double
        throw new IllegalArgumentException("Cannot convert to double: " + expr);
    }
    public void asString(Object obj, StringBuilder sb, boolean escape) {
        sb.append(obj.toString());
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
    public void declareFunction(String name, ContextUtils.Function function) {  // Accept Function instead of FunctionDeclarationNode
        functions.put(name, function);
    }
    public ContextUtils.Function getFunction(String name) {  // Return Function instead of FunctionDeclarationNode
        if (functions.containsKey(name)) {
            return functions.get(name);
        } else if (parentScope != null) {
            return parentScope.getFunction(name);
        } else {
            throw new VariableNotFoundException("Function not found: " + name);
        }
    }
}