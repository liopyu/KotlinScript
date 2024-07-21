package net.liopyu.kotlinscript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class KotlinScriptInterpreter {
    public static final Map<String, File> scriptMap = new HashMap<>();
    private final ScopeChain scopeChain;
    private final KeywordHandler keywordHandler;
    public final String interpreterPathName;
    public KotlinScriptInterpreter(String pathName) {
        this.scopeChain = new ScopeChain();
        this.keywordHandler = new KeywordHandler(scopeChain);
        this.interpreterPathName = pathName;
    }

    public Scanner getOrCreateScanner(File file) throws FileNotFoundException {
        return new Scanner(new FileInputStream(file), StandardCharsets.UTF_8);
    }

    public void loadScriptsFromFolder() {
        File folder = new File(interpreterPathName);
        if (!folder.exists() || !folder.isDirectory()) {
            KotlinScript.LOGGER.error("Invalid folder path: " + interpreterPathName);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".kts"));
        if (files == null) {
            KotlinScript.LOGGER.error("Failed to list files in folder: " + interpreterPathName);
            return;
        }

        for (File file : files) {
            KotlinScript.LOGGER.info("Loading script file: " + file.getName());
            try (Scanner scanner = getOrCreateScanner(file)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    scriptMap.put(file.getName(), file);
                } else {
                    KotlinScript.LOGGER.error("Empty or invalid script file: " + file.getName());
                }
            } catch (IOException e) {
                KotlinScript.LOGGER.error("Error reading script file: " + file.getName(), e);
            }
        }
    }

    public void interpretKotlinScripts() throws FileNotFoundException {
        for (File file : scriptMap.values()) {
            try (EnhancedScanner scanner = new EnhancedScanner(new FileInputStream(file), StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    keywordHandler.interpretLine(line, scanner); // Pass the scanner for line-by-line interpretation
                }
            }
        }
    }
}
