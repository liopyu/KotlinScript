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
        if (line.matches("[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\(.*\\)")) {
            try {
                String alias = line.substring(0, line.indexOf('.'));
                String methodName = line.substring(line.indexOf('.') + 1, line.indexOf('('));

                // Retrieve the class context or instance directly from the current scope
                ContextUtils.ClassContext classContext = (ContextUtils.ClassContext) scopeChain.currentScope().getVariable(alias);
                if (classContext != null) {
                    Method method = classContext.clazz.getMethod(methodName);
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        Object result = method.invoke(null);
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

    private Class<?>[] getParameterTypes(String methodArgs) {
        if (methodArgs.isEmpty()) {
            return new Class<?>[0];
        }

        Object[] args = parseArguments(methodArgs);
        Class<?>[] parameterTypes = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass(); // Get the class of each parsed argument
        }

        return parameterTypes;
    }


    private void handleMethodResult(Method method, Object result, String methodName) {
        if (method.getReturnType().equals(Void.TYPE)) {
            KotlinScript.LOGGER.info("Executed void method: " + methodName);
        } else {
            // Store result in current scope with a unique result variable name
            String resultVariableName = methodName + "Result";
            scopeChain.currentScope().defineVariable(resultVariableName, result, false, method.getReturnType());
            KotlinScript.LOGGER.info("Stored '" + resultVariableName + "' with value: " + result);
        }
    }
    private Object[] parseArguments(String methodArgs) {
        if (methodArgs.isEmpty()) {
            return new Object[0]; // No arguments
        }

        String[] parts = methodArgs.split(",");
        Object[] arguments = new Object[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String arg = parts[i].trim();
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arguments[i] = arg.substring(1, arg.length() - 1); // String literal
            } else if (arg.matches("-?\\d+")) {
                arguments[i] = Integer.parseInt(arg); // Integer
            } else if (arg.matches("-?\\d*\\.\\d+")) {
                arguments[i] = Double.parseDouble(arg); // Double
            } else {
                // Handle variables and complex types
                Object value = scopeChain.currentScope().getVariable(arg);
                if (value != null) {
                    arguments[i] = value;
                } else {
                    throw new RuntimeException("Argument not found in scope: " + arg);
                }
            }
        }
        return arguments;
    }
}
