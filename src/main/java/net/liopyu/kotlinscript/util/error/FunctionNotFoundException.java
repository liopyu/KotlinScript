package net.liopyu.kotlinscript.util.error;

public class FunctionNotFoundException extends Exception {
    public FunctionNotFoundException(String message) {
        super(message);
    }
}