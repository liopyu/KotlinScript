package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Tokenizer;
import net.liopyu.kotlinscript.util.Executor;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Scope;
import net.liopyu.kotlinscript.util.TypeChecker;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KotlinScript() throws IOException {
        //Executor.main(null);
    }
    public static void main(String[] args) throws IOException {
        String path = "run/scripts/script.kts"; // Path to the .kts file

        // Step 1: Read the KotlinScript file
        String sourceCode = new String(Files.readAllBytes(Paths.get(path)));

        // Step 2: Tokenization
        Tokenizer tokenizer = new Tokenizer();
        List<Tokenizer.Token> tokens = tokenizer.tokenize(sourceCode);
        tokens.add(new Tokenizer.Token("EOF", ""));

        // Step 3: Parsing
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();

        // Step 4: Semantic Analysis
        /*Scope globalScope = new Scope(null); // Create a global scope
        TypeChecker typeChecker = new TypeChecker(globalScope);
        typeChecker.check(program);*/

        // Optional: Step 5: Code Generation and Execution
        // CodeGenerator codeGenerator = new CodeGenerator();
        // String generatedCode = codeGenerator.generate(program);
        // System.out.println(generatedCode);

        System.out.println("Type checking completed successfully.");
    }
}
