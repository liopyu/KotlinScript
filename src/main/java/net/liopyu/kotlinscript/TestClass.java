package net.liopyu.kotlinscript;

import java.util.function.Consumer;

public class TestClass {
    public static void execute() {
        KotlinScript.LOGGER.info("Executing method");
    }
    public static String someString() {
        return "Some String";
    }
    public void execute(Consumer<String> consumer) {
        consumer.accept("Test input from TestClass");
    }

}
