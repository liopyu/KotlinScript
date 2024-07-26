package net.liopyu.kotlinscript;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;
import java.util.function.Consumer;

public class TestClass {
    public TestClass(){
        KotlinScript.LOGGER.info("Instantiating class");
    }
    public static void execute() {
        KotlinScript.LOGGER.info("Executing method");
    }
    public void instancedExecute() {
        KotlinScript.LOGGER.info("Executing instanced method");
    }
    public static String someString() {
        return "Some String";
    }
    public void execute(Consumer<String> consumer) {
        consumer.accept("Test input from TestClass");
    }
    public static void main(String[] args) {
        String script = "print(\"test\")";
        ArrayList<Token> tokens = Tokenizer.tokenize(script);
        for (Token token : tokens) {
            System.out.println(token);  // Implicitly calls token.toString()
        }
    }
}
