package net.liopyu.kotlinscript;

import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

public class TestClass {
    public static Logger LOGGER = KotlinScriptInterpreter.LOGGER;
    public void execute(Consumer<String> consumer) {
        // Invoking the consumer with a specific string
        consumer.accept("Test input from TestClass");
    }
}
