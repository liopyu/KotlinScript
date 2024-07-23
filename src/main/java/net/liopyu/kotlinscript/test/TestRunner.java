package net.liopyu.kotlinscript.test;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.token.Tokenizer;
import net.liopyu.kotlinscript.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) throws IOException {
        runTest("Test 1: Generic Class and Method",
                "class MyGenericClass<T> { fun genericMethod(param: T) { print(param) } } fun main() { val myInstance = MyGenericClass<String>() myInstance.genericMethod(\"Test\") }",
                "public class GeneratedProgram {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        MyGenericClass<String> myInstance = new MyGenericClass<>();\n" +
                        "        myInstance.genericMethod(\"Test\");\n" +
                        "    }\n" +
                        "}\n" +
                        "public class MyGenericClass<T> {\n" +
                        "    public void genericMethod(T param) {\n" +
                        "        System.out.println(param);\n" +
                        "    }\n" +
                        "}\n");

        runTest("Test 2: Inline Function",
                "inline fun add(a: Int, b: Int): Int { return a + b } fun main() { val result = add(5, 3) print(result) }",
                "public class GeneratedProgram {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        int result = add(5, 3);\n" +
                        "        System.out.println(result);\n" +
                        "    }\n" +
                        "    public static inline int add(int a, int b) {\n" +
                        "        return a + b;\n" +
                        "    }\n" +
                        "}\n");

        runTest("Test 3: Custom Operator",
                "operator fun plus(a: Int, b: Int): Int { return a + b } fun main() { val sum = 5 + 3 print(sum) }",
                "public class GeneratedProgram {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        int sum = addFunction(5, 3);\n" +
                        "        System.out.println(sum);\n" +
                        "    }\n" +
                        "    public static int addFunction(int a, int b) {\n" +
                        "        return a + b;\n" +
                        "    }\n" +
                        "}\n");

        runTest("Test 4: Annotations with Parameters",
                "@MyAnnotation(key1 = \"value1\", key2 = \"value2\") class AnnotatedClass { fun display() { print(\"Annotated Class\") } } fun main() { val instance = AnnotatedClass() instance.display() }",
                "public class GeneratedProgram {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        AnnotatedClass instance = new AnnotatedClass();\n" +
                        "        instance.display();\n" +
                        "    }\n" +
                        "}\n" +
                        "@MyAnnotation(key1 = \"value1\", key2 = \"value2\")\n" +
                        "public class AnnotatedClass {\n" +
                        "    public void display() {\n" +
                        "        System.out.println(\"Annotated Class\");\n" +
                        "    }\n" +
                        "}\n");
    }

    private static void runTest(String testName, String script, String expectedOutput) throws IOException {
        String path = "run/scripts/script.kts"; // Path to the .kts file

        Executor executor = new Executor();
        executor.executeScript(path);

        System.out.println("Execution completed successfully.");
        System.out.println("Executing script from file: " + path);

        // Step 1: Read the KotlinScript file
        String sourceCode = new String(Files.readAllBytes(Paths.get(path)));
        System.out.println("Source code: " + sourceCode);

        // Step 2: Tokenization
        Tokenizer tokenizer = new Tokenizer(sourceCode);
        List<Token> tokens = tokenizer.tokenize();
        tokens.add(new Token(TokenType.EOF, ""));
        System.out.println("Tokens: " + tokens);

        // Step 3: Parsing
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();
        System.out.println("AST: " + program);


        // Step 5: Execution
        executor.execute(program);
    }
}
