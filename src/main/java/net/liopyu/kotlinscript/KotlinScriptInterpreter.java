package net.liopyu.kotlinscript;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.liopyu.kotlinscript.KeywordHandler.importedClasses;

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
            try (Scanner scanner = new Scanner(new FileInputStream(file), StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    interpretLine(line, scanner); // Pass the scanner for line-by-line interpretation
                }
            }
        }
    }
    public boolean executeVariable(String variableName) {
        Object value = scopeChain.currentScope().getVariable(variableName);
        if (value instanceof ContextUtils.MethodReferenceContext) {
            try {
                ((ContextUtils.MethodReferenceContext) value).invoke();
                return true;
            } catch (Exception e) {
                KotlinScript.LOGGER.error("Error invoking method for variable: " + variableName, e);
            return false;
            }
        } else {
            KotlinScript.LOGGER.info("Variable value: " + value);
            return false;
        }
    }
    public void interpretLine(String line, Scanner scanner) throws FileNotFoundException {
        // Trim the line and remove the portion after any comment markers.
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim(); // Trim after cutting off the comment.
        }

        // Proceed only if the line is not empty after removing comments.
        if (line.isEmpty()) {
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
        } else if (line.matches("\\w+")) {
            if (!executeVariable(line)) {  // Attempt to execute if it's a single word that might be a variable/method.
                KotlinScript.LOGGER.info("Command or variable not executed: " + line);
            }
        } else if (line.startsWith("{")) {
            keywordHandler.handleNewScope(scanner); // Recursive handling of new scope
        } else {
            // Handle possible class method invocation or variable assignment if not a simple keyword or variable execution
            handleAssignmentOrMethodCall(line);
        }
    }

    private void handleAssignmentOrMethodCall(String line) {
        if (line.contains("=")) {
            String[] parts = line.split("=", 2);
            String variableName = parts[0].trim();
            String valueExpression = parts[1].trim();

            if (scopeChain.currentScope().hasVariable(variableName)) {
                // Evaluate the expression (can be a direct value or another variable)
                Object value = evaluateExpression(valueExpression);
                scopeChain.currentScope().setVariable(variableName, value);
                KotlinScript.LOGGER.info("Variable updated: " + variableName + " = " + value);
            } else {
                KotlinScript.LOGGER.error("Unrecognized command: " + line);
            }
        } else {
            // Assume it's a method call if no '=' is present
            executePossibleMethodCall(line);
        }
    }
    private Object evaluateExpression(String expr) {
        // Handle simple expressions - extend this to include more complex evaluations
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);  // String literals
        } else if (expr.matches("-?\\d+(\\.\\d+)?")) {
            return Double.parseDouble(expr);  // Numeric values
        } else {
            // Assume it is a variable or needs further processing
            Object value = scopeChain.currentScope().getVariable(expr);
            if (value != null) {
                return value;
            } else {
                KotlinScript.LOGGER.error("Expression evaluation failed for: " + expr);
                return null;  // or throw an exception based on your error handling strategy
            }
        }
    }
    private void executePossibleMethodCall(String line) {
        // Pattern to match both static calls and instance method calls on new instances
        Pattern pattern = Pattern.compile("([A-Za-z0-9_]+)\\(\\)\\.([A-Za-z0-9_]+)\\((.*)\\)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String alias = matcher.group(1);
            String methodName = matcher.group(2);
            String args = matcher.group(3);  // For future use to handle methods with arguments

            try {
                ContextUtils.ClassContext classContext = importedClasses.get(alias);
                if (classContext == null) {
                    KotlinScript.LOGGER.error("Class not found for alias: " + alias);
                    return;
                }

                // Create an instance and invoke the method
                Object instance = classContext.createInstance();
                Method method = classContext.clazz.getMethod(methodName);  // Simplify by assuming no args for now
                Object result = method.invoke(instance);
                KotlinScript.LOGGER.info("Method call on new instance result: " + (result != null ? result.toString() : "null"));
            } catch (Exception e) {
                KotlinScript.LOGGER.error("Error executing method on new instance: " + methodName + " for alias: " + alias, e);
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
