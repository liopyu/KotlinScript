package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import org.openjdk.nashorn.internal.runtime.ScriptFunction;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotlinScriptInterpreter {
    public static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, Object> variableContext = new HashMap<>();
    private final Map<String, Class<?>> importedClasses = new HashMap<>();
    private boolean isLambdaBlock = false;
    private StringBuilder lambdaBuilder = new StringBuilder();
    private InterpreterContext context;
    public KotlinScriptInterpreter() {
        this.context = new InterpreterContext();
    }
    private void processRegularLine(String line) {
        // Skip comments and empty lines
        if (line.trim().isEmpty() || line.startsWith("//")) {
            return;
        }

        // Handle import statements to dynamically load classes
        if (line.startsWith("import ")) {
            String className = line.substring("import ".length()).trim();
            importClass(className);
            return;
        }

        // Check for method calls or property accesses
        if (line.contains(".")) {
            String[] parts = line.split("\\.");
            String objectOrClassName = parts[0].trim();

            // Check if the class is imported or if it is an instance method call
            if (Character.isUpperCase(objectOrClassName.charAt(0))) { // Assuming class names start with an uppercase letter
                if (!importedClasses.containsKey(objectOrClassName)) {
                    LOGGER.error("Class not imported or instance not found: " + objectOrClassName);
                    return;
                }
            }
            handleMethodOrPropertyAccess(line);
            return;
        }

        // Handle variable definitions and assignments
        if (line.matches("^(var|val)\\s+\\w+\\s*=.*")) {
            defineVariable(line);
            return;
        }

        // Handle print and println commands for basic output
        if (line.startsWith("print(") || line.startsWith("println(")) {
            handlePrintCommands(line);
            return;
        }

        // For all other cases, attempt to evaluate the line as an expression
        evaluateExpression(line);
    }

    private String extractClassNameFromLine(String line) {
        // Assume the line format "ClassName.methodName()" or "ClassName().methodName()"
        int dotIndex = line.indexOf('.');
        if (dotIndex == -1) return null;  // Early return if no dot present

        String beforeDot = line.substring(0, dotIndex);
        if (beforeDot.contains("(")) {
            // This handles "ClassName().methodName"
            return beforeDot.substring(0, beforeDot.indexOf('(')).trim();
        }
        return beforeDot.trim();  // Return "ClassName" part
    }

    private void handlePrintCommands(String line) {
        // Extracting the message from print/println commands
        String message = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
        if (line.startsWith("println(")) {
            LOGGER.info(message);
        } else if (line.startsWith("print(")) {
            LOGGER.info(message.replace("\"", "")); // Assuming removing quotes for simplicity
        }
    }


    private void importClass(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            String simpleName = clazz.getSimpleName();
            importedClasses.put(simpleName, clazz);  // Map simple name to class
            LOGGER.info("Class imported successfully: " + fullClassName + " as " + simpleName);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + fullClassName, e);
        }
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

    private void interpretLine(String line, Context context) {
        if (line.isEmpty() || line.startsWith("//")) {
            // Skip empty lines and comments
            return;
        }

        if (line.startsWith("if") || line.startsWith("for") || line.startsWith("while")) {
            // Handle control structures which might involve creating new scopes
            handleControlStructure(line, context);
        } else if (line.contains("=")) {
            // Handle assignments or declarations
            handleAssignment(line, context.currentScope);
        } else if (line.startsWith("return")) {
            // Handle return statements, potentially breaking out of the function
            handleReturn(line, context);
        } else {
            // Evaluate expressions (could be function calls, operations, etc.)
            evaluateExpression(line, context.currentScope);
        }
    }
    private void processLambdaExpression(String lambdaExpression) {
        LOGGER.info("Processing complete lambda expression: " + lambdaExpression);

        int dotIndex = lambdaExpression.lastIndexOf(".");
        int openBraceIndex = lambdaExpression.indexOf("{");
        int arrowIndex = lambdaExpression.indexOf("->");

        // Check for basic parsing errors
        if (dotIndex == -1 || openBraceIndex == -1 || arrowIndex == -1 || dotIndex >= openBraceIndex || arrowIndex < openBraceIndex) {
            LOGGER.error("Invalid lambda syntax: " + lambdaExpression);
            return;
        }

        // Ensure that dotIndex and openBraceIndex are properly placed for substring operation
        if (dotIndex + 1 >= openBraceIndex) {
            LOGGER.error("Method name extraction bounds are incorrect in: " + lambdaExpression);
            return;
        }

        String header = lambdaExpression.substring(0, openBraceIndex).trim();
        String methodName = header.substring(dotIndex + 1).trim();

        // Extract className ensuring it ends with '()' which indicates instance creation
        String className = header.substring(0, dotIndex);
        if (!className.endsWith("()")) {
            LOGGER.error("Expected class instantiation syntax with '()': " + className);
            return;
        }

        className = className.substring(0, className.length() - 2); // Remove '()'

        // Extract the lambda variable name and body
        String variableName = lambdaExpression.substring(openBraceIndex + 1, arrowIndex).trim();
        String body = lambdaExpression.substring(arrowIndex + 2, lambdaExpression.lastIndexOf('}')).trim();

        Object instance = createInstance(className);
        if (instance == null) {
            LOGGER.error("Failed to create instance for class: " + className);
            return;
        }

        invokeMethodWithConsumer(instance, methodName, body, variableName);
    }
    private void processLambda(Object instance, String methodName, String lambda) {
        LOGGER.info("[processLambda] Processing lambda for method: " + methodName + " with lambda: " + lambda);

        int arrowIndex = lambda.indexOf("->");
        String variableName = lambda.substring(lambda.indexOf('{') + 1, arrowIndex).trim();
        String body = lambda.substring(arrowIndex + 2, lambda.lastIndexOf('}')).trim();

        LambdaContext context = new LambdaContext(body);

        // Extract variables
        Pattern varPattern = Pattern.compile("val\\s+(\\w+)\\s*=\\s*\"([^\"]*)\"");
        Matcher matcher = varPattern.matcher(body);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = matcher.group(2);
            context.addVariable(varName, varValue);
        }

        Consumer<String> consumer = input -> {
            LOGGER.info("[processLambda] Executing lambda with input: " + input);
            String result = context.processInput(variableName, input);
            LOGGER.info("[processLambda] Result: " + result);
        };

        try {
            Method method = instance.getClass().getMethod(methodName, Consumer.class);
            method.invoke(instance, consumer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("[processLambda] Error invoking method with lambda: " + methodName, e);
        }
    }

    private boolean invokeMethodWithConsumer(Object instance, String methodName, String body, String variableName) {
        try {
            Method method = Arrays.stream(instance.getClass().getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName) && m.getParameterCount() == 1 &&
                            Consumer.class.isAssignableFrom(m.getParameterTypes()[0]))
                    .findFirst()
                    .orElse(null);

            if (method == null) {
                LOGGER.error("No suitable method found for lambda: " + methodName);
                return false;
            }

            Consumer<String> consumer = input -> LOGGER.info(body.replace("$" + variableName, input));
            method.invoke(instance, consumer);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Error invoking method with lambda: " + methodName, e);
            return false;
        }
    }
    private void defineFunction(String declaration, String body) {
        String functionName = extractFunctionName(declaration);
        List<String> parameters = extractParameters(declaration);

        // Create a new function object
        ScriptFunction function = new ScriptFunction(functionName, parameters, body, context.currentScope);
        context.currentScope.storeFunction(functionName, function);

        LOGGER.info("Function defined: " + functionName + " with params " + parameters);
    }
    private void executeFunctionBody(String body, Scope functionScope) {
        // Split the body into lines assuming each statement ends with a newline
        String[] lines = body.split("\\r?\\n");

        // Context class to manage current state, including scope
        Context context = new Context(functionScope);

        for (String line : lines) {
            interpretLine(line.trim(), context);
        }
    }
    private void callFunction(String functionName, List<Object> arguments) {
        ScriptFunction function = context.currentScope.lookupFunction(functionName);
        if (function == null) {
            LOGGER.error("Function not found: " + functionName);
            return;
        }

        // Create a new scope for function execution
        Scope functionScope = new Scope(function.getDeclarationScope());
        for (int i = 0; i < function.getParameters().size(); i++) {
            functionScope.declareVariable(function.getParameters().get(i), arguments.get(i));
        }

        // Execute the function body
        executeFunctionBody(function.getBody(), functionScope);
    }


    private void defineVariable(String declaration) {
        String[] parts = declaration.split("=", 2);
        if (parts.length != 2) {
            LOGGER.error("Invalid variable definition: " + declaration);
            return;
        }

        // Trim and determine whether the declaration is using "var" (mutable) or "val" (immutable)
        String varDeclaration = parts[0].trim();
        String valueExpression = parts[1].trim();

        boolean isVal = varDeclaration.startsWith("val ");
        String variableName = varDeclaration.substring(isVal ? "val ".length() : "var ".length()).trim();

        // Evaluate the expression to get the value to be stored in the variable
        Object value = evaluateExpression(valueExpression);

        // Store the variable in the current scope instead of a global context
        try {
            context.currentScope.declareVariable(variableName, value);
            LOGGER.info("Defined variable: " + variableName + " = " + value);
        } catch (Exception e) {
            LOGGER.error("Error declaring variable '" + variableName + "': " + e.getMessage());
        }
    }

    private Object evaluateExpression(String expression) {
        expression = expression.trim();

        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1); // Handle string literals
        }

        if (expression.contains("->")) {
            return handleLambdaExpression(expression); // Handle lambda expressions
        }

        return processDirectExpression(expression); // Process other types of expressions
    }
    private void handleMethodOrPropertyAccess(String line) {
        int dotIndex = line.indexOf('.');
        int openBraceIndex = line.indexOf('{');

        // Check for basic syntax errors before proceeding
        if (dotIndex == -1) {
            LOGGER.error("No method call or property access found in line: " + line);
            return;
        }

        // Determine if the access is a method call potentially with a lambda
        String objectAndMethod = line.substring(0, dotIndex);
        String methodName = openBraceIndex != -1 ?
                line.substring(dotIndex + 1, openBraceIndex).trim() :
                line.substring(dotIndex + 1).trim();

        if (openBraceIndex != -1) {
            // Handle lambda expressions
            if (!line.endsWith("}")) {
                LOGGER.error("Lambda block is not properly closed: " + line);
                return;
            }

            String lambda = line.substring(openBraceIndex);
            if (!objectAndMethod.endsWith("()")) {
                LOGGER.error("Method call for lambda should include object instantiation '()': " + objectAndMethod);
                return;
            }

            String className = objectAndMethod.substring(0, objectAndMethod.length() - 2);
            Object instance = createInstance(className);
            if (instance == null) {
                LOGGER.error("Failed to create instance for class: " + className);
                return;
            }
            processLambda(instance, methodName, lambda);
        } else {
            // Handle regular method calls or property accesses
            if (!objectAndMethod.contains("()")) {
                LOGGER.error("Expected method call to include '()' for instance creation: " + objectAndMethod);
                return;
            }

            String className = objectAndMethod.substring(0, objectAndMethod.indexOf('('));
            Object instance = createInstance(className);
            if (instance == null) {
                LOGGER.error("Failed to create instance for class: " + className);
                return;
            }
            invokeMethod(instance, methodName);
        }
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
    private Object createInstance(String className) {
        Class<?> clazz = importedClasses.get(className);
        if (clazz == null) {
            LOGGER.error("Class not imported: " + className);
            return null;
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error creating instance of class: " + className, e);
            return null;
        }
    }


}
