package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotlinScriptInterpreter {
    public static final Logger LOGGER = LogUtils.getLogger();
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Object> variableContext = new HashMap<>();
    private final Map<String, Object> instanceContext = new HashMap<>();
    private final Map<String, Class<?>> importedClasses = new HashMap<>();
    private final Map<String, Object> classInstances = new HashMap<>();

    public KotlinScriptInterpreter() {
    }
    private void importClass(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            String simpleName = clazz.getSimpleName();
            importedClasses.put(simpleName, clazz);
            LOGGER.info("Imported class: " + fullClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + fullClassName, e);
        }
    }

    private void loadAndExecuteClass(String className, String instanceName) {
        try {
            // Load the class dynamically
            Class<?> clazz = Class.forName(className);
            // Create a new instance of the class
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // Store the instance in the context
            instanceContext.put(instanceName, instance);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + className, e);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error creating instance of class: " + className, e);
        }
    }
    private void callInstanceMethod(String instanceName, String methodName, Object... args) {
        Object instance = instanceContext.get(instanceName);
        if (instance == null) {
            LOGGER.error("Instance not found: " + instanceName);
            return;
        }
        try {
            // Get the class of the instance
            Class<?> clazz = instance.getClass();
            // Get the method by name and parameter types
            Method method = clazz.getMethod(methodName, toClassArray(args));
            // Invoke the method
            Object result = method.invoke(instance, args);
            if (result != null) {
                LOGGER.info(result.toString());
            }
        } catch (NoSuchMethodException e) {
            LOGGER.error("Method not found: " + methodName, e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Error invoking method: " + methodName, e);
        }
    }

    private Class<?>[] toClassArray(Object[] args) {
        Class<?>[] classes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            classes[i] = args[i].getClass();
        }
        return classes;
    }

    public void interpretKotlinScript(String scriptFilePath) {
        // Read the script file content
        InputStream scriptStream = getClass().getClassLoader().getResourceAsStream(scriptFilePath);
        if (scriptStream == null) {
            LOGGER.error("Script file not found: " + scriptFilePath);
            return;
        }

        String script = new Scanner(scriptStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        String[] lines = script.split("\\r?\\n");

        for (String line : lines) {
            interpretLine(line.trim());
        }
    }
    private void invokeMethod(Object target, String methodCall) {
        Class<?> clazz;
        boolean isStatic = false;

        if (target instanceof String) {
            // Static method call
            clazz = importedClasses.get(target);
            isStatic = true;
            LOGGER.info("Invoking static method on class: " + target);
        } else {
            // Instance method call
            clazz = target.getClass();
            LOGGER.info("Invoking instance method on class: " + clazz.getName());
        }

        if (clazz == null) {
            LOGGER.error("Class not found for target: " + target);
            return;
        }

        String methodName = methodCall.substring(0, methodCall.indexOf('(')).trim();
        String argsString = methodCall.substring(methodCall.indexOf('(') + 1, methodCall.length() - 1).trim();
        LOGGER.info("Method name: " + methodName + ", Args string: " + argsString);
        Object[] args = parseArgs(argsString);

        try {
            Method method = findMethod(clazz, methodName, args, isStatic);
            if (method != null) {
                LOGGER.info("Found method: " + method);
                if (Modifier.isPublic(method.getModifiers())) {
                    Object result = method.invoke(isStatic ? null : target, args);
                    if (result != null) {
                        LOGGER.info(result.toString());
                    }
                } else {
                    LOGGER.error("Method not accessible: " + methodName);
                }
            } else {
                LOGGER.error("Method not found: " + methodName);
            }
        } catch (Exception e) {
            LOGGER.error("Error invoking method: " + methodName, e);
        }
    }




    private Method findMethod(Class<?> clazz, String methodName, Object[] args, boolean isStatic) {
        LOGGER.info("Finding method: " + methodName + " with args: " + Arrays.toString(args) + " in class: " + clazz.getName());
        for (Method method : clazz.getDeclaredMethods()) {
            LOGGER.info("Checking method: " + method.getName() + " with parameter types: " + Arrays.toString(method.getParameterTypes()));
            if (method.getName().equals(methodName) && Modifier.isStatic(method.getModifiers()) == isStatic) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == args.length) {
                    boolean matches = true;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (!isAssignableFrom(parameterTypes[i], args[i].getClass())) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private boolean isAssignableFrom(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType.isPrimitive()) {
            if (paramType == int.class && argType == Integer.class) return true;
            if (paramType == long.class && argType == Long.class) return true;
            if (paramType == double.class && argType == Double.class) return true;
            if (paramType == float.class && argType == Float.class) return true;
            if (paramType == boolean.class && argType == Boolean.class) return true;
            if (paramType == char.class && argType == Character.class) return true;
            if (paramType == byte.class && argType == Byte.class) return true;
            if (paramType == short.class && argType == Short.class) return true;
        }
        return false;
    }



    private Object[] parseArgs(String argsString) {
        LOGGER.info("Parsing args: " + argsString);
        if (argsString.isEmpty()) {
            return new Object[0];
        }
        String[] parts = argsString.split(",");
        Object[] args = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            args[i] = evaluateExpression(parts[i].trim());
            LOGGER.info("Parsed arg: " + args[i] + " of type: " + args[i].getClass().getName());
        }
        return args;
    }


    private boolean isInRepeatBlock = false;
    private StringBuilder repeatBlockContent = new StringBuilder();
    private int repeatCount = 0;

    private void interpretLine(String line) {
        LOGGER.info("Interpreting line: " + line);

        if (line.startsWith("import ")) {
            String className = line.substring("import ".length()).trim();
            LOGGER.info("Importing class: " + className);
            importClass(className);
        } else if (line.contains(".")) {
            String[] parts = line.split("\\.");
            if (parts.length == 2) {
                String classNameOrInstance = parts[0].trim();
                String methodCall = parts[1].trim();
                LOGGER.info("Class or instance: " + classNameOrInstance + ", Method call: " + methodCall);
                if (classNameOrInstance.endsWith("()")) {
                    // Instance creation and method call
                    String className = classNameOrInstance.substring(0, classNameOrInstance.length() - 2).trim();
                    LOGGER.info("Creating instance of class: " + className);
                    Object instance = createInstance(className);
                    if (instance != null) {
                        invokeMethod(instance, methodCall);
                    }
                } else {
                    // Static method call
                    invokeMethod(classNameOrInstance, methodCall);
                }
            }
        } else {
            // Existing handling for other expressions
            LOGGER.info("Handling other expression: " + line);
            if (isInRepeatBlock) {
                if (line.endsWith("}")) {
                    handleRepeatEnd(line);
                } else {
                    repeatBlockContent.append(line).append("\n");
                }
                return;
            }

            if (line.startsWith("//")) {
                return; // Ignore comments
            }
            if (line.startsWith("print(")) {
                handlePrint(line);
            } else if (line.startsWith("println(")) {
                handlePrintln(line);
            } else if (line.startsWith("readLine()")) {
                handleReadLine();
            } else if (line.startsWith("val ") || line.startsWith("var ")) {
                defineVariable(line);
            } else if (line.startsWith("listOf(") || line.startsWith("mutableListOf(") || line.startsWith("arrayListOf(")) {
                handleList(line);
            } else if (line.startsWith("setOf(") || line.startsWith("mutableSetOf(") || line.startsWith("hashSetOf(")) {
                handleSet(line);
            } else if (line.startsWith("mapOf(") || line.startsWith("mutableMapOf(") || line.startsWith("hashMapOf(")) {
                handleMap(line);
            } else if (line.startsWith("repeat(")) {
                handleRepeatStart(line);
            } else if (line.endsWith("}")) {
                handleRepeatEnd(line);
            } else if (line.startsWith("measureTimeMillis {")) {
                handleMeasureTimeMillis(line);
            } else if (line.startsWith("measureNanoTime {")) {
                handleMeasureNanoTime(line);
            } else if (line.startsWith("require(") || line.startsWith("check(") || line.startsWith("assert(")) {
                handleRequireCheckAssert(line);
            } else if (line.startsWith("executeClass(")) {
                String[] parts = line.substring("executeClass(".length(), line.length() - 1).replace("\"", "").split(",");
                loadAndExecuteClass(parts[0].trim(), parts[1].trim());
            } else if (line.contains(".")) {
                String[] parts = line.split("\\.");
                if (parts.length == 2) {
                    String instanceName = parts[0].trim();
                    String methodCall = parts[1].trim();
                    String methodName = methodCall.substring(0, methodCall.indexOf('(')).trim();
                    String args = methodCall.substring(methodCall.indexOf('(') + 1, methodCall.length() - 1).trim();
                    callInstanceMethod(instanceName, methodName, parseArgs(args));
                }
            } else {
                evaluateExpression(line);
            }
        }
    }

    private void handleRepeatStart(String line) {
        int openParenIndex = line.indexOf('(');
        int closeParenIndex = line.indexOf(')');
        if (openParenIndex == -1 || closeParenIndex == -1) {
            LOGGER.error("Invalid repeat syntax: " + line);
            return;
        }

        String countStr = line.substring(openParenIndex + 1, closeParenIndex).trim();
        Object countValue = getVariableValue(countStr);
        if (!(countValue instanceof Integer)) {
            LOGGER.error("Repeat count must be an integer: " + countStr);
            return;
        }
        repeatCount = (Integer) countValue;
        isInRepeatBlock = true;
        repeatBlockContent.setLength(0); // Clear any previous content
    }


    private void handleRepeatEnd(String line) {
        isInRepeatBlock = false;
        String statement = repeatBlockContent.toString().trim();
        for (int i = 0; i < repeatCount; i++) {
            interpretLine(statement);
        }
    }

    private void handlePrint(String line) {
        String content = line.substring("print(".length(), line.length() - 1).replace("\"", ""); // Remove double quotes
        LOGGER.info(content);
    }

    private void handlePrintln(String line) {
        String content = line.substring("println(".length(), line.length() - 1).replace("\"", ""); // Remove double quotes
        content = interpolateString(content); // Interpolate string
        LOGGER.info(content + "\n");
    }

    private void handleReadLine() {
        String input = scanner.nextLine();
        LOGGER.info("Input: " + input);
    }

    private void handleList(String line) {
        String content = line.substring(line.indexOf("(") + 1, line.length() - 1);
        List<String> list = Arrays.asList(content.split(","));
        LOGGER.info("List: " + list);
    }

    private void handleSet(String line) {
        String content = line.substring(line.indexOf("(") + 1, line.length() - 1);
        Set<String> set = new HashSet<>(Arrays.asList(content.split(",")));
        LOGGER.info("Set: " + set);
    }

    private void handleMap(String line) {
        String content = line.substring(line.indexOf("(") + 1, line.length() - 1);
        Map<String, String> map = new HashMap<>();
        for (String entry : content.split(",")) {
            String[] keyValue = entry.split("=");
            map.put(keyValue[0].trim(), keyValue[1].trim());
        }
        LOGGER.info("Map: " + map);
    }

    private void handleRepeat(String line) {
        int openParenIndex = line.indexOf('(');
        int closeParenIndex = line.indexOf(')');
        int openBraceIndex = line.indexOf('{');
        int closeBraceIndex = line.lastIndexOf('}');

        if (openParenIndex == -1 || closeParenIndex == -1 || openBraceIndex == -1 || closeBraceIndex == -1 || openBraceIndex < closeParenIndex) {
            LOGGER.error("Invalid repeat syntax: " + line);
            return;
        }

        String countStr = line.substring(openParenIndex + 1, closeParenIndex).trim();
        Object countValue = getVariableValue(countStr);
        if (!(countValue instanceof Integer)) {
            LOGGER.error("Repeat count must be an integer: " + countStr);
            return;
        }
        int times = (Integer) countValue;
        String statement = line.substring(openBraceIndex + 1, closeBraceIndex).trim();

        for (int i = 0; i < times; i++) {
            interpretLine(statement);
        }
    }






    private void handleMeasureTimeMillis(String line) {
        String statement = line.substring("measureTimeMillis {".length(), line.length() - 1).trim();
        long startTime = System.currentTimeMillis();
        interpretLine(statement);
        long endTime = System.currentTimeMillis();
        LOGGER.info("Execution time: " + (endTime - startTime) + " ms");
    }

    private void handleMeasureNanoTime(String line) {
        String statement = line.substring("measureNanoTime {".length(), line.length() - 1).trim();
        long startTime = System.nanoTime();
        interpretLine(statement);
        long endTime = System.nanoTime();
        LOGGER.info("Execution time: " + (endTime - startTime) + " ns");
    }

    private void handleRequireCheckAssert(String line) {
        String condition = line.substring(line.indexOf("(") + 1, line.length() - 1);
        if (!Boolean.parseBoolean(condition)) {
            LOGGER.error("Precondition failed: " + line);
            throw new IllegalArgumentException("Precondition failed: " + line);
        }
    }

    private void defineVariable(String declaration) {
        String[] parts = declaration.split("=", 2);
        if (parts.length != 2) {
            LOGGER.error("Invalid variable definition: " + declaration);
            return;
        }

        String varDeclaration = parts[0].trim();
        String valueExpression = parts[1].trim();

        boolean isVal = varDeclaration.startsWith("val ");
        String variableName = varDeclaration.substring(isVal ? 4 : 4); // Skip "val " or "var "

        Object value = evaluateExpression(valueExpression);
        variableContext.put(variableName, value);

        LOGGER.info("Defined variable: " + variableName + " = " + value);
    }




    private Object evaluateExpression(String expression) {
        expression = expression.trim();
        if (expression.isEmpty()) {
            return null; // Handle empty expressions
        }
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            // Handle string literals with interpolation
            String literal = expression.substring(1, expression.length() - 1);
            return interpolateString(literal);
        }
        // Split by space to check for arithmetic operations
        String[] tokens = expression.split(" ");
        if (tokens.length == 1) {
            return getVariableValue(tokens[0]);
        } else if (tokens.length == 3) {
            Object leftValue = getVariableValue(tokens[0]);
            String operator = tokens[1];
            Object rightValue = getVariableValue(tokens[2]);
            return performOperation(leftValue, operator, rightValue);
        }
        LOGGER.warn("Unrecognized or complex expression: " + expression);
        return null;
    }

    private String interpolateString(String literal) {
        // Find all variables in the string and replace them with their values
        StringBuffer result = new StringBuffer();
        Matcher matcher = Pattern.compile("\\$\\w+").matcher(literal);
        while (matcher.find()) {
            String variable = matcher.group();
            String varName = variable.substring(1); // Remove $
            Object varValue = variableContext.getOrDefault(varName, variable);
            matcher.appendReplacement(result, varValue.toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }



    private Object getVariableValue(String value) {
        if (value.isEmpty()) {
            return null; // Handle empty values
        }
        if (variableContext.containsKey(value)) {
            return variableContext.get(value);
        } else if (value.matches("-?\\d+(\\.\\d+)?")) {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1); // String literal
        } else if (value.contains(" ")) {
            return evaluateExpression(value); // Handle expressions
        } else {
            LOGGER.error("Unknown variable or value: " + value);
            throw new NumberFormatException("Unknown variable or value: " + value);
        }
    }


    private Object performOperation(Object leftValue, String operator, Object rightValue) {
        try {
            if (leftValue instanceof Integer && rightValue instanceof Integer) {
                int leftInt = (Integer) leftValue;
                int rightInt = (Integer) rightValue;
                switch (operator) {
                    case "+":
                        return leftInt + rightInt;
                    case "-":
                        return leftInt - rightInt;
                    case "*":
                        return leftInt * rightInt;
                    case "/":
                        return leftInt / rightInt;
                    case "%":
                        return leftInt % rightInt;
                }
            } else if (leftValue instanceof Double && rightValue instanceof Double) {
                double leftDouble = (Double) leftValue;
                double rightDouble = (Double) rightValue;
                switch (operator) {
                    case "+":
                        return leftDouble + rightDouble;
                    case "-":
                        return leftDouble - rightDouble;
                    case "*":
                        return leftDouble * rightDouble;
                    case "/":
                        return leftDouble / rightDouble;
                    case "%":
                        return leftDouble % rightDouble;
                }
            } else {
                LOGGER.error("Operation not supported for types: " + leftValue.getClass().getSimpleName());
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse number: " + rightValue, e);
        }
        return null;
    }
    private void loadAndExecuteClass(String className) {
        try {
            // Load the class dynamically
            Class<?> clazz = Class.forName(className);
            // Create a new instance of the class
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // Find and invoke the execute method
            Method executeMethod = clazz.getMethod("execute");
            executeMethod.invoke(instance);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + className, e);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error executing class: " + className, e);
        }
    }
    private Object createInstance(String className) {
        Class<?> clazz = importedClasses.get(className);
        if (clazz == null) {
            LOGGER.error("Class not imported: " + className);
            return null;
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            classInstances.put(className, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error creating instance of class: " + className, e);
            return null;
        }
    }

}
