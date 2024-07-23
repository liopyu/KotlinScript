package net.liopyu.kotlinscript.util.error;

public class VariableNotFoundException extends RuntimeException {
    public VariableNotFoundException(String message) {
        super(message);
    }
}