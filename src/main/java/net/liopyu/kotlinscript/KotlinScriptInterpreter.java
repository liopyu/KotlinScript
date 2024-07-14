package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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

        // Determine whether it's a static or instance method call
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

        // Improved detection for method name and argument list
        int methodNameEndIndex = methodCall.indexOf("(");
        methodNameEndIndex = methodNameEndIndex == -1 ? methodCall.indexOf("{") : methodNameEndIndex;
        if (methodNameEndIndex == -1) {
            LOGGER.error("Invalid method call syntax: " + methodCall);
            return;
        }

        String methodName = methodCall.substring(0, methodNameEndIndex).trim();
        String argsString = methodCall.substring(methodNameEndIndex).trim(); // Grabs everything after method name

        // Handle different types of argument enclosures
        int openIndex = argsString.startsWith("{") ? argsString.indexOf("{") : argsString.indexOf("(");
        int closeIndex = argsString.startsWith("{") ? argsString.lastIndexOf("}") : argsString.lastIndexOf(")");
        if (openIndex == -1 || closeIndex == -1 || openIndex > closeIndex) {
            LOGGER.error("Invalid method call syntax: " + methodCall);
            return;
        }

        argsString = argsString.substring(openIndex + 1, closeIndex).trim();
        LOGGER.info("Method name: " + methodName + ", Args string: " + argsString);

        // Determine if the arguments suggest a lambda or a consumer
        if (argsString.contains("->")) {
            LOGGER.info("Detected lambda expression in args");
            handleLambdaExpression(clazz, methodName, argsString, isStatic, target);
        } else if (argsString.contains("Consumer<")) {
            LOGGER.info("Detected Consumer expression in args");
            handleConsumerExpression(clazz, methodName, argsString, isStatic, target);
        } else {
            Object[] args = parseArgs(argsString);
            executeMethod(clazz, methodName, args, isStatic, target);
        }
    }

    private String extractArgsString(String methodCall) {
        int start = methodCall.indexOf('{') != -1 ? methodCall.indexOf('{') : methodCall.indexOf('(');
        int end = methodCall.lastIndexOf('}') != -1 ? methodCall.lastIndexOf('}') : methodCall.lastIndexOf(')');
        return methodCall.substring(start + 1, end).trim();
    }
    private void handleConsumerExpression(Class<?> clazz, String methodName, String typeInfo, boolean isStatic, Object target) {
        String typeName = typeInfo.substring(typeInfo.indexOf('<') + 1, typeInfo.indexOf('>')).trim();
        try {
            Class<?> typeClass = Class.forName(typeName);  // Ensure the class is available
            Consumer<?> consumer = createConsumer(typeClass);
            executeMethod(clazz, methodName, new Object[]{consumer}, isStatic, target);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Type not found for Consumer: " + typeName, e);
        }
    }

    private <T> Consumer<T> createConsumer(Class<T> typeClass) {
        return input -> LOGGER.info("Consuming " + input + " of type " + typeClass.getSimpleName());
    }


    private Method findMethodForConsumer(Class<?> clazz, String methodName, Object[] args, boolean isStatic) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == args.length && Modifier.isStatic(method.getModifiers()) == isStatic) {
                if (Consumer.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    return method;
                }
            }
        }
        return null;
    }

    private void handleLambdaExpression(Class<?> clazz, String methodName, String lambdaExpression, boolean isStatic, Object target) {
        LOGGER.info("Handling lambda expression: " + lambdaExpression);
        // Convert the lambda expression to a functional interface
        Object lambda = createLambdaFunction(lambdaExpression);
        executeMethod(clazz, methodName, new Object[]{lambda}, isStatic, target);
    }

    private Object createLambdaFunction(String lambdaExpression) {
        // Placeholder for lambda creation logic
        return (Function<String, String>) s -> {
            // Your lambda execution logic here
            return "Lambda executed with input: " + s;
        };
    }

    private void executeMethod(Class<?> clazz, String methodName, Object[] args, boolean isStatic, Object target) {
        try {
            LOGGER.debug("Attempting to find method: " + methodName);
            Method method = findMethodForGenerics(clazz, methodName, args, isStatic);
            if (method != null && Modifier.isPublic(method.getModifiers())) {
                LOGGER.debug("Invoking method: " + methodName + " on class: " + clazz.getName());
                Object result = method.invoke(isStatic ? null : target, args);
                LOGGER.info("Method result: " + (result != null ? result.toString() : "void"));
            } else {
                LOGGER.error("Method not accessible or not found: " + methodName);
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOGGER.error("Error invoking method: " + methodName, e);
        } catch (InvocationTargetException e) {
            LOGGER.error("Error invoking method: " + methodName, e.getCause());
            e.getCause().printStackTrace();
        }
    }

    private Method findMethodForGenerics(Class<?> clazz, String methodName, Object[] args, boolean isStatic) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length && Modifier.isStatic(method.getModifiers()) == isStatic) {
                if (areParameterTypesCompatible(method.getParameterTypes(), args)) {
                    return method;
                }
            }
        }
        return null;
    }

    private boolean areParameterTypesCompatible(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isInstance(args[i])) {
                // Check for special cases like Consumer or other functional interfaces
                if (Consumer.class.isAssignableFrom(parameterTypes[i]) && args[i] instanceof Consumer) {
                    continue;
                }
                // More special cases (like Function, Supplier, etc.) can be added here
                return false;
            }
        }
        return true;
    }



    private Object handleFunction(String methodName, String argsString, Class<?> targetClass) {
        try {
            // Assume the first part before "(" in argsString specifies the function's input type
            String functionType = argsString.substring(argsString.indexOf(":") + 1, argsString.indexOf("->")).trim();
            Class<?> inputType = Class.forName("java.lang." + functionType);

            // Dynamically determine the return type by inspecting further
            String returnTypePart = argsString.substring(argsString.indexOf("->") + 2).trim();
            Class<?> returnType = returnTypePart.startsWith("\"") ? String.class : Object.class;  // Simplified assumption

            // Find the method with correct parameter types
            Method method = targetClass.getMethod(methodName, Function.class);
            Function<?, ?> function = createFunction(argsString, inputType, returnType);

            return method.invoke(null, function);  // Assuming static for simplicity
        } catch (Exception e) {
            LOGGER.error("Failed to handle function: " + methodName, e);
            return null;
        }
    }

    private Function<?, ?> createFunction(String lambdaBody, Class<?> inputType, Class<?> returnType) {
        // Parsing and dynamic function creation based on inputType and returnType
        // This part is pseudocode and requires specific implementation
        return input -> {
            // Evaluate the lambda body with the given input
            return evaluateExpression(lambdaBody.replace("$s", input.toString()));
        };
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
            String arg = parts[i].trim();
            if (arg.startsWith("{") && arg.endsWith("}")) {
                // Treat as a Kotlin lambda
                String lambdaExpression = arg.substring(1, arg.length() - 1).trim();
                LOGGER.info("Parsed lambda expression: " + lambdaExpression);
                args[i] = (Function<String, Object>) s -> evaluateExpression(lambdaExpression.replace("$s", s));
            } else {
                args[i] = evaluateExpression(arg);
            }
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
            handleImport(line);
        } else if (line.contains(".")) {
            handleMethodCall(line);
        } else {
            handleOtherExpressions(line);
        }
    }

    private void handleImport(String line) {
        String className = line.substring("import ".length()).trim();
        LOGGER.info("Importing class: " + className);
        importClass(className);
    }
    private boolean inLambda = false;
    private StringBuilder lambdaContent = new StringBuilder();

    private void handleMethodCall(String line) {
        if (line.contains("{") && !inLambda) {
            inLambda = true;
            lambdaContent.append(line).append("\n");
            return;
        }
        if (inLambda) {
            lambdaContent.append(line).append("\n");
            if (line.contains("}")) {
                inLambda = false;
                executeLambda(lambdaContent.toString());
                lambdaContent.setLength(0);  // Clear the content after processing
            }
            return;
        }
        String[] parts = line.split("\\.");
        if (parts.length == 2) {
            String classNameOrInstance = parts[0].trim();
            String methodCall = parts[1].trim();
            LOGGER.info("Class or instance: " + classNameOrInstance + ", Method call: " + methodCall);
            if (classNameOrInstance.endsWith("()")) {
                handleInstanceMethodCall(classNameOrInstance, methodCall);
            } else {
                invokeMethod(classNameOrInstance, methodCall);  // Assuming static method call
            }
        }
    }
    private void executeLambda(String lambdaCode) {
        LOGGER.info("Executing lambda block: " + lambdaCode);

        // Extract the method call part and the lambda part
        int openParenIndex = lambdaCode.indexOf('{');
        int closeParenIndex = lambdaCode.lastIndexOf('}');
        if (openParenIndex == -1 || closeParenIndex == -1 || openParenIndex > closeParenIndex) {
            LOGGER.error("Invalid lambda syntax: " + lambdaCode);
            return;
        }

        String methodCallPart = lambdaCode.substring(0, openParenIndex).trim();
        String lambdaPart = lambdaCode.substring(openParenIndex + 1, closeParenIndex).trim();

        LOGGER.info("Method call part: " + methodCallPart);
        LOGGER.info("Lambda part: " + lambdaPart);

        // Parsing the method call part
        String[] methodCallParts = methodCallPart.split("\\.");
        if (methodCallParts.length != 2) {
            LOGGER.error("Invalid method call part: " + methodCallPart);
            return;
        }
        String classNameOrInstance = methodCallParts[0].trim();
        String methodCall = methodCallParts[1].trim();

        // Extracting the lambda parameters and body
        int arrowIndex = lambdaPart.indexOf("->");
        if (arrowIndex == -1) {
            LOGGER.error("Invalid lambda syntax: " + lambdaPart);
            return;
        }
        String lambdaParameters = lambdaPart.substring(0, arrowIndex).trim();
        String lambdaBody = lambdaPart.substring(arrowIndex + 2).trim();

        // Handle different types of argument enclosures
        int methodOpenIndex = methodCall.indexOf("(");
        int methodCloseIndex = methodCall.lastIndexOf(")");
        if (methodOpenIndex == -1 || methodCloseIndex == -1 || methodOpenIndex > methodCloseIndex) {
            LOGGER.error("Invalid method call syntax: " + methodCall);
            return;
        }

        String methodName = methodCall.substring(0, methodOpenIndex).trim();
        String methodArgs = methodCall.substring(methodOpenIndex + 1, methodCloseIndex).trim();

        // Create an instance of the class or use a static method call
        Object target;
        boolean isStatic;
        if (classNameOrInstance.endsWith("()")) {
            String className = classNameOrInstance.substring(0, classNameOrInstance.length() - 2).trim();
            target = createInstance(className);
            isStatic = false;
        } else {
            target = classNameOrInstance;
            isStatic = true;
        }

        if (target == null) {
            LOGGER.error("Failed to create instance or find target for: " + classNameOrInstance);
            return;
        }

        // Determine the type of consumer
        invokeMethodWithLambda(target, methodName, lambdaParameters, lambdaBody, isStatic);
    }
    private void invokeMethodWithLambda(Object target, String methodName, String lambdaParameters, String lambdaBody, boolean isStatic) {
        Class<?> clazz = isStatic ? importedClasses.get(target) : target.getClass();
        if (clazz == null) {
            LOGGER.error("Class not found for target: " + target);
            return;
        }

        try {
            Method method = findMethodForLambda(clazz, methodName, isStatic);
            if (method != null && Modifier.isPublic(method.getModifiers())) {
                Class<?> parameterType = method.getParameterTypes()[0];
                Object consumer = createConsumer(parameterType, lambdaParameters, lambdaBody);
                method.invoke(isStatic ? null : target, consumer);
            } else {
                LOGGER.error("Method not accessible or not found: " + methodName);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Error invoking method: " + methodName, e);
            if (e instanceof InvocationTargetException && e.getCause() != null) {
                LOGGER.error("Invocation target exception: ", e.getCause());
            }
        }
    }

    private Method findMethodForLambda(Class<?> clazz, String methodName, boolean isStatic) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && Modifier.isStatic(method.getModifiers()) == isStatic) {
                if (method.getParameterCount() == 1) {
                    return method;
                }
            }
        }
        return null;
    }

    private Object createConsumer(Class<?> parameterType, String lambdaParameters, String lambdaBody) {
        if (parameterType.equals(Consumer.class)) {
            return (Consumer<String>) input -> {
                String evaluatedBody = interpolateVariables(lambdaBody.replace("$" + lambdaParameters, input));
                LOGGER.info(evaluatedBody);
            };
        } else if (parameterType.equals(BiConsumer.class)) {
            // Assuming BiConsumer for demonstration; adapt as needed
            return (BiConsumer<String, String>) (input1, input2) -> {
                String evaluatedBody = interpolateVariables(lambdaBody.replace("$" + lambdaParameters.split(",")[0].trim(), input1).replace("$" + lambdaParameters.split(",")[1].trim(), input2));
                LOGGER.info(evaluatedBody);
            };
        }
        // Add other types as needed
        throw new IllegalArgumentException("Unsupported consumer type: " + parameterType);
    }
    private void handleInstanceMethodCall(String classNameOrInstance, String methodCall) {
        String className = classNameOrInstance.substring(0, classNameOrInstance.length() - 2).trim();
        LOGGER.info("Creating instance of class: " + className);
        Object instance = createInstance(className);
        if (instance != null) {
            invokeMethod(instance, methodCall);
        }
    }

    private void handleOtherExpressions(String line) {
        if (line.startsWith("//") || line.isEmpty()) {
            return; // Ignore comments and empty lines
        }
        if (inLambda) {
            lambdaContent.append(line).append("\n");
            return;
        }
        // Block control for repeats or multi-line structures
        if (handleBlockControl(line)) {
            return;
        }

        // Direct command handling
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
        } else if (line.startsWith("measureTimeMillis {")) {
            handleMeasureTimeMillis(line);
        } else if (line.startsWith("measureNanoTime {")) {
            handleMeasureNanoTime(line);
        } else if (line.startsWith("require(") || line.startsWith("check(") || line.startsWith("assert(")) {
            handleRequireCheckAssert(line);
        } else if (line.startsWith("executeClass(")) {
            handleExecuteClass(line);
        } else {
            evaluateExpression(line);
        }
    }

    private boolean handleBlockControl(String line) {
        if (isInRepeatBlock) {
            if (line.endsWith("}")) {
                handleRepeatEnd(line);
            } else {
                repeatBlockContent.append(line).append("\n");
            }
            return true;
        }
        return false;
    }

    private void handleExecuteClass(String line) {
        String[] parts = line.substring("executeClass(".length(), line.length() - 1).replace("\"", "").split(",");
        loadAndExecuteClass(parts[0].trim(), parts[1].trim());
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
        String content = extractExpression(line, "println");
        content = interpolateVariables(content); // Assuming this method handles the variable interpolation
        LOGGER.info(content);
    }
    private String extractExpression(String line, String command) {
        int commandLength = command.length();
        int startIndex = line.indexOf(command + "(") + commandLength + 1;  // Start index of the content within the parentheses
        int endIndex = line.lastIndexOf(")");  // End index of the content within the parentheses

        if (startIndex < 0 || endIndex < 0 || endIndex <= startIndex) {
            LOGGER.error("Invalid syntax for " + command + ": " + line);
            return "";
        }

        // Extract the content between the parentheses
        return line.substring(startIndex, endIndex).trim();
    }

    private String interpolateVariables(String content) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variableContext.get(variableName);
            String replacement = value == null ? "null" : value.toString();
            // Properly escape $ signs in the replacement string
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
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

        // Direct return of strings
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }

        // Handling lambda expressions
        if (expression.contains("->")) {
            return handleLambdaExpression(expression);
        }

        return processDirectExpression(expression);
    }

    private Function<String, String> handleLambdaExpression(String expression) {
        // Extract the part after '->' which is the lambda body
        String lambdaBody = expression.substring(expression.indexOf("->") + 2).trim();

        // Return a function that processes this body when called
        return input -> {
            // This should replace the placeholder '$s' with the actual input
            String processedExpression = lambdaBody.replace("$s", input);

            // Evaluate this expression directly to handle replacements and return result
            return (String) evaluateExpression(processedExpression);
        };
    }


    private Object processDirectExpression(String expression) {
        // Remove any surrounding whitespace and normalize the expression
        expression = expression.trim().replaceAll("\\s+", " ");

        // Handle basic arithmetic or direct value access
        if (expression.matches("-?\\d+(\\.\\d+)?")) {  // Numeric values
            return expression.contains(".") ? Double.parseDouble(expression) : Integer.parseInt(expression);
        } else if (expression.startsWith("\"") && expression.endsWith("\"")) {  // String literals
            return expression.substring(1, expression.length() - 1);
        } else if (variableContext.containsKey(expression)) {  // Variables
            return variableContext.get(expression);
        }

        // Split the expression to analyze if it's a function call or complex expression
        String[] tokens = expression.split(" ");
        if (tokens.length == 1) {
            // Single token could be a function call or a variable
            return variableContext.getOrDefault(tokens[0], null);
        } else if (tokens.length == 3) {  // Possible arithmetic operation
            return handleArithmetic(tokens[0], tokens[1], tokens[2]);
        }

        LOGGER.warn("Unhandled direct expression: " + expression);
        return null;
    }

    private Object handleArithmetic(String left, String operator, String right) {
        Object leftVal = getVariableValue(left);
        Object rightVal = getVariableValue(right);

        if (leftVal instanceof Number && rightVal instanceof Number) {
            return calculate((Number) leftVal, operator, (Number) rightVal);
        }

        LOGGER.error("Arithmetic operation not applicable for non-numeric types.");
        return null;
    }

    private Object calculate(Number left, String operator, Number right) {
        switch (operator) {
            case "+": return left.doubleValue() + right.doubleValue();
            case "-": return left.doubleValue() - right.doubleValue();
            case "*": return left.doubleValue() * right.doubleValue();
            case "/":
                if (right.doubleValue() == 0) {
                    LOGGER.error("Division by zero.");
                    return null;
                }
                return left.doubleValue() / right.doubleValue();
            default:
                LOGGER.error("Unknown operator: " + operator);
                return null;
        }
    }


    private String interpolateString(String literal) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = Pattern.compile("\\$\\w+").matcher(literal);
        while (matcher.find()) {
            String variable = matcher.group();
            String varName = variable.substring(1); // Remove the initial $
            Object varValue = variableContext.getOrDefault(varName, variable);
            // Properly escape $ signs in the replacement string
            String replacement = varValue.toString().replace("$", "\\$\\$");
            matcher.appendReplacement(result, replacement);
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
