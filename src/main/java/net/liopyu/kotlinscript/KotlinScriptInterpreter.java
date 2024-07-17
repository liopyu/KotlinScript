package net.liopyu.kotlinscript;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import static net.liopyu.kotlinscript.KeywordHandler.importedClasses;

public class KotlinScriptInterpreter {
    public static final Map<String, InputStream> scriptMap = new HashMap<>();
    private final ScopeChain scopeChain;
    private final KeywordHandler keywordHandler;
    public KotlinScriptInterpreter() {
        this.scopeChain = new ScopeChain();
        this.keywordHandler = new KeywordHandler(scopeChain);
    }
    public void loadScriptsFromFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            KotlinScript.LOGGER.error("Invalid folder path: " + folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".kts"));
        if (files == null) {
            KotlinScript.LOGGER.error("Failed to list files in folder: " + folderPath);
            return;
        }

        for (File file : files) {
            KotlinScript.LOGGER.info("Loading script file: " + file.getName());
            try (Scanner scanner = new Scanner(new FileInputStream(file), StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String script = scanner.next();
                    scriptMap.put(file.getName(), new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
                } else {
                    KotlinScript.LOGGER.error("Empty or invalid script file: " + file.getName());
                }
            } catch (IOException e) {
                KotlinScript.LOGGER.error("Error reading script file: " + file.getName(), e);
            }
        }
    }

    public void interpretKotlinScripts() {
        for (InputStream scriptStream : scriptMap.values()) {
            String script = "";
            try (Scanner scanner = new Scanner(scriptStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                if (scanner.hasNext()) {
                    script = scanner.next();
                }
            }

            String[] lines = script.split("\\r?\\n");
            for (String line : lines) {
                interpretLine(line.trim());
            }
        }
    }
    private void interpretLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("//")) {
            return;
        }
        if (KotlinScriptHelperClass.isKeyWord(line)) {
            String keyword = KotlinScriptHelperClass.getKeyWord(line);
            switch (keyword) {
                case "val":
                case "var":
                    keywordHandler.handleDeclaration(line);
                    break;
                case "print":
                    keywordHandler.handlePrint(line);
                    break;
                case "import":
                    keywordHandler.importClass(line);
                    break;
                default:
                    KotlinScript.LOGGER.error("Unhandled keyword: " + keyword);
            }
        }  else {
            // Handle possible class method invocation
            executePossibleMethodCall(line);
        }
    }
    private void executePossibleMethodCall(String line) {
        if (line.matches("[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\(.*\\)")) { // Basic regex to check for method calls
            try {
                String alias = line.substring(0, line.indexOf('.'));
                String methodName = line.substring(line.indexOf('.') + 1, line.indexOf('('));

                // Retrieve the class context using the alias
                ContextUtils.ClassContext context = importedClasses.get(alias);
                if (context != null) {
                    Method method = context.clazz.getMethod(methodName); // Assuming no parameters for simplicity
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        Object result = method.invoke(null); // Invoke static method
                        KotlinScript.LOGGER.info("Method call result: " + (result != null ? result.toString() : "null"));
                    } else {
                        throw new IllegalArgumentException("Method " + methodName + " is not static and cannot be invoked this way.");
                    }
                } else {
                    throw new IllegalArgumentException("Class not found for alias: " + alias);
                }
            } catch (Exception e) {
                KotlinScript.LOGGER.error("Error executing method call: " + line, e);
            }
        } else {
            KotlinScript.LOGGER.error("Unrecognized command: " + line);
        }
    }

}
