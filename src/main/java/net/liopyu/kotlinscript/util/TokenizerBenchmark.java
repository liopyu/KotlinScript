package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.ArrayList;

public class TokenizerBenchmark {
    public static void main(String[] args) {
        String script =
                "var counter = 5\n" +
                        "var counterString = \"5\"\n" +
                        "var pi = 3.14\n" +
                        "var isTrue = true\n" +
                        "{\n" +
                        "    var counter1 = 10\n" +
                        "    //print(counter1)\n" +
                        "    {\n" +
                        "        var nestedCounter = 20\n" +
                        "        /* Multi-line\n" +
                        "           comment */\n" +
                        "        var nestedString = \"nested\"\n" +
                        "        print(nestedCounter)\n" +
                        "        print(nestedString)\n" +
                        "    }\n" +
                        "    print(counter1)\n" +
                        "}\n" +
                        "var anotherVar = 42\n" +
                        "var anotherString = \"another\"\n" +
                        "print(counter)\n" +
                        "print(counterString)\n" +
                        "print(pi)\n" +
                        "print(isTrue)\n" +
                        "print(anotherVar)\n" +
                        "print(anotherString)\n" +
                        "var finalVar = 100\n" +
                        "print(finalVar)\n" +
                        "{\n" +
                        "    var shadowVar = 1000\n" +
                        "    print(shadowVar)\n" +
                        "}\n" +
                        "print(finalVar)\n" +
                        "{\n" +
                        "    var lastBlockVar = 200\n" +
                        "    print(lastBlockVar)\n" +
                        "    // End of script\n" +
                        "}\n";

        long startTime = System.nanoTime();
        ArrayList<Token> tokens = Tokenizer.tokenize(script);
        long endTime = System.nanoTime();

        System.out.println("Character-based tokenizer time: " + (endTime - startTime) + " ns");
        Parser parser = new Parser(tokens);
        ArrayList<ASTNode> nodes = parser.parse();

        Executor executor = new Executor();
        executor.execute(nodes);
        // Print tokens to verify correctness
        tokens.forEach(System.out::println);
    }
}

