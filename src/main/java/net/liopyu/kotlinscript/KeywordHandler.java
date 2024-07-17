package net.liopyu.kotlinscript;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordHandler {
    private final ScopeChain scopeChain;

    public KeywordHandler(ScopeChain scopeChain) {
        this.scopeChain = scopeChain;
    }

    public void handleDeclaration(String line) {
        String[] parts = line.split("=", 2);
        String declarationPart = parts[0].trim();
        String valuePart = parts[1].trim();
        String[] declarationParts = declarationPart.split("\\s+");

        String variableName = declarationParts[1].trim();
        Object value;
        Class<?> valueType; // Type of the value
        try {
            if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                value = valuePart.substring(1, valuePart.length() - 1); // String literal
                valueType = String.class;
            } else {
                value = evaluateExpression(valuePart);
                valueType = value.getClass(); // Determine the class of the value
            }
        } catch (RuntimeException e) {
            KotlinScript.LOGGER.error("Error evaluating expression: " + valuePart, e);
            return;
        }

        boolean isImmutable = declarationParts[0].equals("val");
        scopeChain.currentScope().defineVariable(variableName, value, isImmutable, valueType);
    }

    public void handlePrint(String line) {
        // Remove the 'print(' prefix and the ')' suffix
        String content = line.substring("print(".length(), line.length() - 1).trim();

        // Check for string concatenation or interpolation
        if (content.contains("+") || content.contains("$")) {
            content = evaluateComplexExpression(content);
        } else if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);  // Remove quotes for direct string literals
        } else if (content.contains(".")) { // This checks if it's likely a method call
            try {
                content = String.valueOf(evaluateMethodCall(content));
            } catch (RuntimeException e) {
                KotlinScript.LOGGER.error("Error processing method call: " + content, e);
                return;
            }
        } else {
            // Handle as a variable name or direct string if not quoted
            Object value = scopeChain.currentScope().getVariable(content);
            if (value == null) {
                content = content; // If no variable found, treat as a string
            } else {
                content = String.valueOf(value);
            }
        }

        KotlinScript.LOGGER.info(content);
    }
    private Object evaluateMethodCall(String methodCall) {
        try {
            String[] parts = methodCall.split("\\.");
            String className = parts[0];
            String methodName = parts[1].substring(0, parts[1].indexOf('('));
            String args = parts[1].substring(parts[1].indexOf('(') + 1, parts[1].indexOf(')'));
            Object[] arguments = parseArguments(args); // You need to implement this based on your method signature needs

            ContextUtils.ClassContext context = importedClasses.get(className);
            if (context == null) {
                throw new RuntimeException("Class not found for alias: " + className);
            }

            Method method = context.clazz.getMethod(methodName, getParameterTypes(arguments));
            return method.invoke(null, arguments);  // Handle static method invocation with arguments
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute method: " + methodCall, e);
        }
    }
    private Object[] parseArguments(String args) {
        if (args.isEmpty()) {
            return new Object[0]; // No arguments
        }

        // Split arguments by commas. Note: This simple split can fail with complex arguments like strings containing commas.
        String[] parts = args.split(",");
        Object[] arguments = new Object[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String arg = parts[i].trim();
            // Determine the type of each argument and parse it accordingly
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arguments[i] = arg.substring(1, arg.length() - 1); // Remove quotes for strings
            } else if (arg.matches("\\d+")) {
                arguments[i] = Integer.parseInt(arg); // Parse integers
            } else if (arg.matches("\\d*\\.\\d+")) {
                arguments[i] = Double.parseDouble(arg); // Parse doubles
            } else {
                // Add more parsing logic here for other types as needed
                arguments[i] = arg; // Default to string if not sure
            }
        }
        return arguments;
    }
    private Class<?>[] getParameterTypes(Object[] arguments) {
        Class<?>[] parameterTypes = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Integer) {
                parameterTypes[i] = int.class; // or Integer.class for objects
            } else if (arguments[i] instanceof Double) {
                parameterTypes[i] = double.class; // or Double.class for objects
            } else if (arguments[i] instanceof String) {
                parameterTypes[i] = String.class;
            } else {
                // Handle other types
                parameterTypes[i] = arguments[i].getClass();
            }
        }
        return parameterTypes;
    }


    private String evaluateComplexExpression(String expr) {
        // Handle interpolation with regular expressions
        Pattern pattern = Pattern.compile("\\$(\\w+)");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = scopeChain.currentScope().getVariable(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        expr = sb.toString();

        // Strip surrounding quotes after interpolation
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            expr = expr.substring(1, expr.length() - 1);
        }

        // Handle string concatenation
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    result.append(part, 1, part.length() - 1); // Remove quotes for direct string literals
                } else {
                    Object value = scopeChain.currentScope().getVariable(part);
                    result.append(String.valueOf(value));
                }
            }
            expr = result.toString();
        }

        return expr;
    }


    public static final Map<String, ContextUtils.ClassContext> importedClasses = new HashMap<>();
    public void importClass(String importStatement) {
        String className;
        String alias;

        // Trim and remove the 'import' keyword
        importStatement = importStatement.substring("import ".length()).trim();

        // Check if there is an 'as' to define an alias
        if (importStatement.contains(" as ")) {
            String[] parts = importStatement.split(" as ");
            className = parts[0].trim();
            alias = parts[1].trim();
        } else {
            className = importStatement;
            // Extract simple name as alias if no alias is explicitly provided
            alias = className.substring(className.lastIndexOf('.') + 1);
        }

        try {
            Class<?> clazz = Class.forName(className);
            String simpleName = clazz.getSimpleName();
            ContextUtils.ClassContext classContext = new ContextUtils.ClassContext(clazz, simpleName,alias, className);
            importedClasses.put(alias, classContext);
            KotlinScript.LOGGER.info("Class imported successfully: " + className + " as " + alias);
        } catch (ClassNotFoundException e) {
            KotlinScript.LOGGER.error("Class not found: " + importStatement, e);
        }
    }



    private Object evaluateExpression(String expr) {
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);  // Remove quotes for string literals
        } else if (expr.contains(".")) {  // Likely a method call
            return evaluateMethodCall(expr);
        } else {
            return scopeChain.currentScope().getVariable(expr);  // Treat as a variable name
        }
    }

}
