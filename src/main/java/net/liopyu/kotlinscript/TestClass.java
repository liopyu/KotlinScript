package net.liopyu.kotlinscript;

import org.slf4j.Logger;

import java.util.function.Consumer;

public class TestClass {
    public static Logger LOGGER = KotlinScriptInterpreter.LOGGER;
    public void execute(Consumer<String> consumer) {
        consumer.accept("Test input from TestClass");
    }
}
