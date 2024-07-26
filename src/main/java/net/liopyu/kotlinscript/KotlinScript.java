package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.Tokenizer;
import net.liopyu.kotlinscript.util.Executor;
import net.liopyu.kotlinscript.util.Parser;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KotlinScript() throws FileNotFoundException {
        main(null);
    }
    public static void main(String[] args) {
        String path = "run/scripts/script.kts"; // Path to the .kts file
        try {
            String script = new String(Files.readAllBytes(Paths.get(path)));
            ArrayList<Token> tokens = Tokenizer.tokenize(script);
            tokens.forEach(System.out::println);

            Parser parser = new Parser(tokens);
            ArrayList<ASTNode> nodes = parser.parse();

            Executor executor = new Executor();
            executor.execute(nodes);
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

}
