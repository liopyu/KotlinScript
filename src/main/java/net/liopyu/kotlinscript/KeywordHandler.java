package net.liopyu.kotlinscript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.liopyu.kotlinscript.KotlinScript.interpreterMap;

public class KeywordHandler {
    private final ScopeChain scopeChain;

    public KeywordHandler(ScopeChain scopeChain) {
        this.scopeChain = scopeChain;
    }

    public void handleNewScope(Scanner scanner,String line) throws FileNotFoundException {
        scopeChain.enterScope();
        try {
            int firstBraceIndex = line.indexOf('{');

            // Handle any inline code immediately after the opening brace
            if (firstBraceIndex != -1) {
                String inlineCodeAfterBrace = line.substring(firstBraceIndex + 1).trim();
                if (!inlineCodeAfterBrace.isEmpty()) {
                    // There might be executable code directly after '{'
                    interpretInlineCode(inlineCodeAfterBrace, scanner);
                }
            }

            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine().trim();
                if (nextLine.equals("}")) {
                    break; // Exit the loop on a standalone closing bracket
                } else if (nextLine.startsWith("}")) {
                    // Handle inline code before a closing brace
                    String inlineCodeBeforeBrace = nextLine.substring(1).trim();
                    if (!inlineCodeBeforeBrace.isEmpty()) {
                        interpretLine(inlineCodeBeforeBrace, scanner);
                    }
                    break;
                } else if (!nextLine.isEmpty() && !nextLine.startsWith("//")) {
                    interpretLine(nextLine, scanner); // Recursively process each line within the new scope
                }
            }
        } finally {
            scopeChain.exitScope(); // Ensure the scope is always exited
        }
    }

    private void interpretInlineCode(String code, Scanner scanner) throws FileNotFoundException {
        // Split by semicolons if multiple statements are on one line after the opening brace
        String[] statements = code.split(";");
        for (String statement : statements) {
            if (!statement.trim().isEmpty()) {
                interpretLine(statement.trim(), scanner);
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
                    this.handleDeclaration(line);
                    break;
                case "print":
                    this.handlePrint(line);
                    break;
                case "import":
                    this.importClass(line);
                    break;
                case "fun":
                    this.handleFunctionDefinition(line, scanner);
                    break;
                default:
                    KotlinScript.LOGGER.error("Unhandled keyword: " + keyword);
            }
        }  else if (line.matches("\\w+\\(\\)")) { // Detects function calls with ()
            String functionName = line.substring(0, line.indexOf('('));
            executeFunction(functionName);
        } else if (line.matches("\\w+")) {
            if (!executeVariable(line)) {  // Attempt to execute if it's a single word that might be a variable/method.
                KotlinScript.LOGGER.info("Command or variable not executed: " + line);
            }
        } else if (line.startsWith("{")) {
            this.handleNewScope(scanner,line); // Recursive handling of new scope
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
                Object value = evaluateExpression(valueExpression,scopeChain.currentScope());
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
    private Object evaluateExpression(String expr, Scope scope) {
        try {
            // Check if the expression is a method call
            if (expr.matches("[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\(.*\\)")) {
                // This pattern assumes a simple method call like ClassName.methodName()
                String className = expr.substring(0, expr.indexOf('.'));
                String methodName = expr.substring(expr.indexOf('.') + 1, expr.indexOf('('));
                ContextUtils.ClassContext classContext = importedClasses.get(className);
                if (classContext != null) {
                    Method method = classContext.clazz.getMethod(methodName);
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        Object result = method.invoke(null);
                        if (method.getReturnType().equals(Void.TYPE)) {
                            // For void methods, return a placeholder or null
                            return "Void Method Placeholder";
                        }
                        return result;
                    }
                }
            } else if (expr.startsWith("\"") && expr.endsWith("\"")) {
                return expr.substring(1, expr.length() - 1);  // Handle string literals
            } else if (expr.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(expr);  // Handle numeric values
            } else if (scope.hasVariable(expr)) {
                return scope.getVariable(expr);  // Retrieve value of an existing variable
            }
        } catch (Exception e) {
            KotlinScript.LOGGER.error("Failed to evaluate expression: " + expr, e);
        }
        throw new IllegalArgumentException("Expression could not be evaluated: " + expr);
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

    public void handleDeclaration(String line) {
        String[] parts = line.split("=", 2);
        String declarationPart = parts[0].trim();
        String valuePart = parts.length > 1 ? parts[1].trim() : "";
        String[] declarationParts = declarationPart.split("\\s+");

        String variableName = declarationParts[1].trim();
        Object value;

        try {
            if (valuePart.matches("[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\(\\)")) {  // Check for method call pattern
                String className = valuePart.substring(0, valuePart.indexOf('.'));
                String methodName = valuePart.substring(valuePart.indexOf('.') + 1, valuePart.indexOf('('));
                Class<?> clazz = importedClasses.get(className).clazz;
                value = new ContextUtils.MethodReferenceContext(clazz, methodName);
            } else {
                value = evaluateExpression(valuePart, scopeChain.currentScope());
            }
            scopeChain.currentScope().defineVariable(variableName, value, declarationParts[0].equals("val"), value.getClass());
        } catch (RuntimeException e) {
            KotlinScript.LOGGER.error("Error handling declaration: " + line, e);
        }
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
            ContextUtils.ClassContext classContext = new ContextUtils.ClassContext(clazz, simpleName, alias, className);
            importedClasses.put(alias, classContext);

            // Store a placeholder or reference in the scope
            scopeChain.currentScope().defineVariable(alias, classContext, false, ContextUtils.ClassContext.class);

            KotlinScript.LOGGER.info("Class imported successfully: " + className + " as " + alias);
        } catch (ClassNotFoundException e) {
            KotlinScript.LOGGER.error("Class not found: " + importStatement, e);
        }
    }
    public void handleFunctionDefinition(String line, Scanner scanner) {
        StringBuilder functionDeclaration = new StringBuilder(line.trim());
        while (!functionDeclaration.toString().contains("{") && scanner.hasNextLine()) {
            functionDeclaration.append(" ").append(scanner.nextLine().trim());
        }

        int lastBraceIndex = functionDeclaration.lastIndexOf("{");
        String declarationUpToBrace = functionDeclaration.substring(0, lastBraceIndex).trim();
        String inlineCodeAfterBrace = functionDeclaration.substring(lastBraceIndex + 1).trim();

        int functionNameStart = 4; // Skip the "fun " part
        int functionNameEnd = declarationUpToBrace.indexOf('(');
        if (functionNameEnd == -1) {
            throw new RuntimeException("Syntax error: Function definition missing '('.");
        }
        String functionName = declarationUpToBrace.substring(functionNameStart, functionNameEnd).trim();

        StringBuilder functionBody = new StringBuilder();
        int braceDepth = 1; // Starts with 1 due to the initial '{'
        boolean inString = false;
        char stringChar = '\0';

        // Add any inline code immediately after the '{' to the function body.
        functionBody.append(inlineCodeAfterBrace).append("\n");

        while (scanner.hasNext() && braceDepth > 0) {
            String nextLine = scanner.nextLine();
            int firstBraceIndex = nextLine.indexOf('}');
            if (firstBraceIndex != -1 && firstBraceIndex != 0) {
                // There's inline code before the closing '}' on this line
                String codeBeforeBrace = nextLine.substring(0, firstBraceIndex).trim();
                for (char nextChar : codeBeforeBrace.toCharArray()) {
                    functionBody.append(nextChar);
                    if (nextChar == '{') {
                        braceDepth++;
                    } else if (nextChar == '"' || nextChar == '\'') {
                        if (inString && stringChar == nextChar) {
                            inString = false; // Closing the string
                        } else {
                            inString = true;
                            stringChar = nextChar; // Opening a new string
                        }
                    }
                }
                functionBody.append("\n");
            }

            // Continue processing the rest of the line after handling the code before '}'
            for (int i = (firstBraceIndex == -1 ? 0 : firstBraceIndex); i < nextLine.length(); i++) {
                char nextChar = nextLine.charAt(i);
                functionBody.append(nextChar);
                if (nextChar == '{') {
                    braceDepth++;
                } else if (nextChar == '}') {
                    braceDepth--;
                    if (braceDepth == 0 && i != nextLine.length() - 1) {
                        // Break out of the loop if the function body is properly closed
                        break;
                    }
                }
            }

            functionBody.append("\n");
            if (braceDepth == 0) break; // Stop processing if all braces are matched
        }

        if (braceDepth != 0) {
            throw new RuntimeException("Syntax error: Mismatched braces in function definition.");
        }

        Consumer<Scope> function = currentScope -> {
            try {
                interpretFunctionBody(functionBody.toString().trim(), line);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        scopeChain.currentScope().defineFunction(functionName, function);
    }


    public void executeFunction(String functionName) {
        Consumer<Scope> function = scopeChain.currentScope().getFunction(functionName);
        if (function != null) {
            // Execute the function in a new scope
            scopeChain.enterScope();
            try {
                function.accept(scopeChain.currentScope());
            } finally {
                scopeChain.exitScope(); // Ensure the new function execution scope is exited
            }
        } else {
            KotlinScript.LOGGER.error("Function not found: " + functionName);
        }
    }

    public void interpretFunctionBody(String functionBody, String line) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(functionBody)) {
            this.handleNewScope(scanner,line);
        }
    }

}
