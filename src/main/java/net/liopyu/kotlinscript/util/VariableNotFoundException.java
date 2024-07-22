package net.liopyu.kotlinscript.util;

public class VariableNotFoundException extends RuntimeException {
    public VariableNotFoundException(String message) {
        super(message);
    }
}