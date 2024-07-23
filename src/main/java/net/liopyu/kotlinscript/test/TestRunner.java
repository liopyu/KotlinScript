package net.liopyu.kotlinscript.test;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Tokenizer;
import net.liopyu.kotlinscript.util.CodeGenerator;
import net.liopyu.kotlinscript.util.EnhancedSemanticAnalyzer;
import net.liopyu.kotlinscript.util.Parser;

import java.util.List;

public class TestRunner {
    public static void main(String[] args) {
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

    private static void runTest(String testName, String script, String expectedOutput) {
        System.out.println("Running " + testName);
        Tokenizer tokenizer = new Tokenizer();
        List<Tokenizer.Token> tokens = tokenizer.tokenize(script);
        tokens.add(new Tokenizer.Token("EOF", ""));
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();
        EnhancedSemanticAnalyzer analyzer = new EnhancedSemanticAnalyzer();
        analyzer.analyze(program);
        CodeGenerator generator = new CodeGenerator();
        String javaCode = generator.generate(program);
        System.out.println("Generated Java Code:");
        System.out.println(javaCode);
        System.out.println("Expected Java Code:");
        System.out.println(expectedOutput);
        System.out.println(javaCode.equals(expectedOutput) ? "Test Passed" : "Test Failed");
    }
}
