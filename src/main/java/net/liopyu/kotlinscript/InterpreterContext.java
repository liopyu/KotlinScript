package net.liopyu.kotlinscript;

public class InterpreterContext {
    Scope currentScope;

    public InterpreterContext() {
        this.currentScope = new Scope(null);
    }

    public void enterFunction() {
        currentScope = new Scope(currentScope);
    }

    public void exitFunction() {
        currentScope = currentScope.parentScope;
    }

    public void enterBlock() {
        currentScope = new Scope(currentScope);
    }

    public void exitBlock() {
        currentScope = currentScope.parentScope;
    }
}
