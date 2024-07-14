package net.liopyu.kotlinscript;

public class TestClass {
    public static void anyMethod() {
        KotlinScriptInterpreter.LOGGER.info("anyMethod executed");
    }

    public static void printMessage(String message) {
        KotlinScriptInterpreter.LOGGER.info("Message: " + message);
    }

    public void execute() {
        KotlinScriptInterpreter.LOGGER.info("TestClass instance executed");
    }

    public void execute(int i) {
        KotlinScriptInterpreter.LOGGER.info("TestClass instance executed: " + i);
    }

    public void execute(int i, int j) {
        KotlinScriptInterpreter.LOGGER.info("TestClass instance executed: " + i + ", " + j);
    }
}
