package net.liopyu.kotlinscript;

import java.util.ArrayDeque;
import java.util.Deque;

public class ScopeChain {
    private final Deque<Scope> scopes;

    public ScopeChain() {
        scopes = new ArrayDeque<>();
        scopes.push(new Scope(null)); // global scope
    }

    public void enterScope() {
        scopes.push(new Scope(scopes.peek()));
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        } else {
            throw new RuntimeException("Cannot exit global scope");
        }
    }

    public Scope currentScope() {
        return scopes.peek();
    }
}
