package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.FunctionDeclarationNode;
import net.liopyu.kotlinscript.util.error.FunctionNotFoundException;
import net.liopyu.kotlinscript.util.error.VariableNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Object> variables;
    private final Map<String, ContextUtils.Function> functions;

    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
    }

    public void declareVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) throws VariableNotFoundException {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parent != null) {
            return parent.getVariable(name);
        } else {
            throw new VariableNotFoundException("Variable not found: " + name);
        }
    }

    public void declareFunction(String name, ContextUtils.Function function) {  // Accept Function instead of FunctionDeclarationNode
        functions.put(name, function);
    }
    public ContextUtils.Function getFunction(String name) throws FunctionNotFoundException {  // Return Function instead of FunctionDeclarationNode
        if (functions.containsKey(name)) {
            return functions.get(name);
        } else if (parent != null) {
            return parent.getFunction(name);
        } else {
            throw new FunctionNotFoundException("Function not found: " + name);
        }
    }

}