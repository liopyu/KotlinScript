package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
import net.liopyu.kotlinscript.token.Tokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeGenerator {
    private StringBuilder javaCode;
    private int indentationLevel;
    private Scope currentScope;
    private Map<String, String> customOperators;

    public CodeGenerator() {
        javaCode = new StringBuilder();
        indentationLevel = 0;
        currentScope = new Scope(null); // Global scope
        customOperators = new HashMap<>();
    }

    public String generate(ASTNode node) {
        try {
            if (node instanceof Program) {
                generateProgram((Program) node);
            } else if (node instanceof VariableDeclaration) {
                generateVariableDeclaration((VariableDeclaration) node);
            } else if (node instanceof FunctionDeclaration) {
                generateFunctionDeclaration((FunctionDeclaration) node);
            } else if (node instanceof PrintStatement) {
                generatePrintStatement((PrintStatement) node);
            } else if (node instanceof BinaryOperation) {
                generateBinaryOperation((BinaryOperation) node);
            } else if (node instanceof FunctionCall) {
                generateFunctionCall((FunctionCall) node);
            } else if (node instanceof Identifier) {
                generateIdentifier((Identifier) node);
            } else if (node instanceof Literal) {
                generateLiteral((Literal) node);
            } else if (node instanceof IfStatement) {
                generateIfStatement((IfStatement) node);
            } else if (node instanceof WhileStatement) {
                generateWhileStatement((WhileStatement) node);
            } else if (node instanceof ReturnStatement) {
                generateReturnStatement((ReturnStatement) node);
            } else if (node instanceof ForStatement) {
                generateForStatement((ForStatement) node);
            } else if (node instanceof SwitchStatement) {
                generateSwitchStatement((SwitchStatement) node);
            } else if (node instanceof CaseClause) {
                generateCaseClause((CaseClause) node);
            } else if (node instanceof ClassDeclaration) {
                generateClassDeclaration((ClassDeclaration) node);
            } else if (node instanceof ConstructorDeclaration) {
                generateConstructorDeclaration((ConstructorDeclaration) node);
            } else if (node instanceof AccessModifier) {
                generateAccessModifier((AccessModifier) node);
            } else if (node instanceof StaticDeclaration) {
                generateStaticDeclaration((StaticDeclaration) node);
            } else if (node instanceof AbstractMethodDeclaration) {
                generateAbstractMethodDeclaration((AbstractMethodDeclaration) node);
            } else if (node instanceof InterfaceDeclaration) {
                generateInterfaceDeclaration((InterfaceDeclaration) node);
            } else if (node instanceof Annotation) {
                generateAnnotation((Annotation) node);
            } else if (node instanceof TernaryOperation) {
                generateTernaryOperation((TernaryOperation) node);
            } else if (node instanceof LambdaExpression) {
                generateLambdaExpression((LambdaExpression) node);
            } else if (node instanceof TryCatchFinally) {
                generateTryCatchFinally((TryCatchFinally) node);
            } else if (node instanceof EnumDeclaration) {
                generateEnumDeclaration((EnumDeclaration) node);
            } else if (node instanceof GenericDeclaration) {
                generateGenericDeclaration((GenericDeclaration) node);
            } else if (node instanceof InlineFunction) {
                generateInlineFunction((InlineFunction) node);
            } else if (node instanceof CustomOperator) {
                generateCustomOperator((CustomOperator) node);
            } else if (node instanceof AnnotationWithParameters) {
                generateAnnotationWithParameters((AnnotationWithParameters) node);
            } else if (node instanceof DoWhileStatement) {
                generateDoWhileStatement((DoWhileStatement) node);
            } else if (node instanceof LabeledStatement) {
                generateLabeledStatement((LabeledStatement) node);
            } else if (node instanceof BreakStatement) {
                generateBreakStatement((BreakStatement) node);
            } else if (node instanceof ContinueStatement) {
                generateContinueStatement((ContinueStatement) node);
            } else if (node instanceof ArrayDeclaration) {
                generateArrayDeclaration((ArrayDeclaration) node);
            } else if (node instanceof ListDeclaration) {
                generateListDeclaration((ListDeclaration) node);
            } else if (node instanceof MapDeclaration) {
                generateMapDeclaration((MapDeclaration) node);
            } else if (node instanceof DefaultParameter) {
                generateDefaultParameter((DefaultParameter) node);
            } else if (node instanceof Closure) {
                generateClosure((Closure) node);
            } else if (node instanceof SynchronizedBlock) {
                generateSynchronizedBlock((SynchronizedBlock) node);
            } else if (node instanceof ThreadCreation) {
                generateThreadCreation((ThreadCreation) node);
            } else if (node instanceof Reflection) {
                generateReflection((Reflection) node);
            } else {
                throw new RuntimeException("Unknown AST node type: " + node.getClass());
            }
        } catch (RuntimeException e) {
            EnhancedErrorReporter.reportError("Error generating code for node", node);
            e.printStackTrace();
        }
        return javaCode.toString();
    }

    private void generateProgram(Program program) {
        javaCode.append("public class GeneratedProgram {\n");
        increaseIndentation();
        javaCode.append(getIndentation()).append("public static void main(String[] args) {\n");
        increaseIndentation();
        for (ASTNode statement : program.statements) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
        decreaseIndentation();
        javaCode.append("}\n");
    }

    private void generateVariableDeclaration(VariableDeclaration variableDeclaration) {
        currentScope.declareVariable(variableDeclaration.name, "var");
        javaCode.append(getIndentation()).append("var ");
        javaCode.append(variableDeclaration.name);
        javaCode.append(" = ");
        generate(variableDeclaration.initializer);
        javaCode.append(";\n");
    }

    private void generateFunctionDeclaration(FunctionDeclaration functionDeclaration) {
        String parameters = functionDeclaration.parameters.stream()
                .map(param -> "var " + param)
                .collect(Collectors.joining(", "));
        currentScope.declareFunction(functionDeclaration.name, parameters);

        javaCode.append(getIndentation()).append("public void ");
        javaCode.append(functionDeclaration.name);
        javaCode.append("(");
        javaCode.append(parameters);
        javaCode.append(") {\n");

        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the function

        for (ASTNode statement : functionDeclaration.body) {
            generate(statement);
        }

        // Include annotation processing after function body is generated
        generateAnnotationProcessing(functionDeclaration.name);

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generatePrintStatement(PrintStatement printStatement) {
        javaCode.append(getIndentation()).append("System.out.println(");
        generate(printStatement.expression);
        javaCode.append(");\n");
    }

    private void generateBinaryOperation(BinaryOperation binaryOperation) {
        if (customOperators.containsKey(binaryOperation.operator)) {
            javaCode.append(customOperators.get(binaryOperation.operator)).append("(");
            generate(binaryOperation.left);
            javaCode.append(", ");
            generate(binaryOperation.right);
            javaCode.append(")");
        } else {
            generate(binaryOperation.left);
            javaCode.append(" ").append(binaryOperation.operator).append(" ");
            generate(binaryOperation.right);
        }
    }

    private void generateFunctionCall(FunctionCall functionCall) {
        String functionSignature = currentScope.resolveFunction(functionCall.name);
        if (functionSignature == null) {
            EnhancedErrorReporter.reportError("Function not declared", functionCall);
        }
        javaCode.append(functionCall.name);
        javaCode.append("(");
        String arguments = functionCall.arguments.stream()
                .map(this::generateExpression)
                .collect(Collectors.joining(", "));
        javaCode.append(arguments);
        javaCode.append(");\n");
    }

    private void generateIdentifier(Identifier identifier) {
        String variableType = currentScope.resolveVariable(identifier.name);
        if (variableType == null) {
            EnhancedErrorReporter.reportError("Variable not declared", identifier);
        }
        javaCode.append(identifier.name);
    }

    private void generateLiteral(Literal literal) {
        javaCode.append(literal.value);
    }

    private void generateIfStatement(IfStatement ifStatement) {
        javaCode.append(getIndentation()).append("if (");
        generate(ifStatement.condition);
        javaCode.append(") {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the if statement

        for (ASTNode statement : ifStatement.thenBranch) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
        if (!ifStatement.elseBranch.isEmpty()) {
            javaCode.append(getIndentation()).append("else {\n");
            increaseIndentation();
            currentScope = new Scope(previousScope); // Create a new scope for the else statement

            for (ASTNode statement : ifStatement.elseBranch) {
                generate(statement);
            }

            currentScope = previousScope; // Restore the previous scope
            decreaseIndentation();
            javaCode.append(getIndentation()).append("}\n");
        }
    }

    private void generateWhileStatement(WhileStatement whileStatement) {
        javaCode.append(getIndentation()).append("while (");
        generate(whileStatement.condition);
        javaCode.append(") {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the while statement

        for (ASTNode statement : whileStatement.body) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateDoWhileStatement(DoWhileStatement doWhileStatement) {
        javaCode.append(getIndentation()).append("do {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the do-while statement

        for (ASTNode statement : doWhileStatement.body) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("} while (");
        generate(doWhileStatement.condition);
        javaCode.append(");\n");
    }

    private void generateLabeledStatement(LabeledStatement labeledStatement) {
        javaCode.append(getIndentation()).append(labeledStatement.label).append(": ");
        generate(labeledStatement.statement);
    }

    private void generateBreakStatement(BreakStatement breakStatement) {
        javaCode.append(getIndentation()).append("break");
        if (breakStatement.label != null) {
            javaCode.append(" ").append(breakStatement.label);
        }
        javaCode.append(";\n");
    }

    private void generateContinueStatement(ContinueStatement continueStatement) {
        javaCode.append(getIndentation()).append("continue");
        if (continueStatement.label != null) {
            javaCode.append(" ").append(continueStatement.label);
        }
        javaCode.append(";\n");
    }

    private void generateReturnStatement(ReturnStatement returnStatement) {
        javaCode.append(getIndentation()).append("return ");
        if (returnStatement.value != null) {
            generate(returnStatement.value);
        }
        javaCode.append(";\n");
    }

    private void generateForStatement(ForStatement forStatement) {
        javaCode.append(getIndentation()).append("for (");
        if (forStatement.initializer != null) {
            generate(forStatement.initializer);
        }
        javaCode.append(" ; ");
        if (forStatement.condition != null) {
            generate(forStatement.condition);
        }
        javaCode.append(" ; ");
        if (forStatement.increment != null) {
            generate(forStatement.increment);
        }
        javaCode.append(") {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the for statement

        for (ASTNode statement : forStatement.body) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateSwitchStatement(SwitchStatement switchStatement) {
        javaCode.append(getIndentation()).append("switch (");
        generate(switchStatement.expression);
        javaCode.append(") {\n");
        increaseIndentation();
        for (CaseClause caseClause : switchStatement.cases) {
            generate(caseClause);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateCaseClause(CaseClause caseClause) {
        if (caseClause.value != null) {
            javaCode.append(getIndentation()).append("case ");
            generate(caseClause.value);
            javaCode.append(": \n");
        } else {
            javaCode.append(getIndentation()).append("default: \n");
        }
        increaseIndentation();
        for (ASTNode statement : caseClause.statements) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("break;\n");
    }

    private void generateClassDeclaration(ClassDeclaration classDeclaration) {
        javaCode.append(getIndentation()).append("public class ").append(classDeclaration.name);
        if (classDeclaration.parentClass != null) {
            javaCode.append(" extends ").append(classDeclaration.parentClass);
        }
        javaCode.append(" {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the class

        for (ASTNode member : classDeclaration.members) {
            generate(member);
        }

        // Include annotation processing after class members are generated
        generateAnnotationProcessing(classDeclaration.name);

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateConstructorDeclaration(ConstructorDeclaration constructorDeclaration) {
        javaCode.append(getIndentation()).append("public ").append(currentScope.resolveClass()).append("(");
        String parameters = constructorDeclaration.parameters.stream()
                .map(param -> "var " + param)
                .collect(Collectors.joining(", "));
        javaCode.append(parameters);
        javaCode.append(") {\n");

        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the constructor

        for (ASTNode statement : constructorDeclaration.body) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateAccessModifier(AccessModifier accessModifier) {
        javaCode.append(getIndentation()).append(accessModifier.modifier).append(" ");
    }

    private void generateStaticDeclaration(StaticDeclaration staticDeclaration) {
        javaCode.append(getIndentation()).append("static ");
        generate(staticDeclaration.member);
    }

    private void generateAbstractMethodDeclaration(AbstractMethodDeclaration abstractMethodDeclaration) {
        javaCode.append(getIndentation()).append("public abstract void ");
        javaCode.append(abstractMethodDeclaration.name);
        javaCode.append("(");
        String parameters = abstractMethodDeclaration.parameters.stream()
                .map(param -> "var " + param)
                .collect(Collectors.joining(", "));
        javaCode.append(parameters);
        javaCode.append(");\n");
    }

    private void generateInterfaceDeclaration(InterfaceDeclaration interfaceDeclaration) {
        javaCode.append(getIndentation()).append("public interface ").append(interfaceDeclaration.name).append(" {\n");
        increaseIndentation();
        for (ASTNode method : interfaceDeclaration.methods) {
            generate(method);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateAnnotation(Annotation annotation) {
        javaCode.append(getIndentation()).append("@").append(annotation.name).append("\n");
    }

    private void generateAnnotationWithParameters(AnnotationWithParameters annotation) {
        javaCode.append(getIndentation()).append("@").append(annotation.name).append("(");
        String parameters = annotation.parameters.entrySet().stream()
                .map(entry -> entry.getKey() + " = \"" + entry.getValue() + "\"")
                .collect(Collectors.joining(", "));
        javaCode.append(parameters).append(")\n");
    }

    private void generateTernaryOperation(TernaryOperation ternaryOperation) {
        generate(ternaryOperation.condition);
        javaCode.append(" ? ");
        generate(ternaryOperation.trueExpr);
        javaCode.append(" : ");
        generate(ternaryOperation.falseExpr);
    }

    private void generateLambdaExpression(LambdaExpression lambdaExpression) {
        javaCode.append("(");
        String parameters = lambdaExpression.parameters.stream()
                .collect(Collectors.joining(", "));
        javaCode.append(parameters);
        javaCode.append(") -> ");
        generate(lambdaExpression.body);
    }

    private void generateTryCatchFinally(TryCatchFinally tryCatchFinally) {
        javaCode.append(getIndentation()).append("try {\n");
        increaseIndentation();
        for (ASTNode statement : tryCatchFinally.tryBlock) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("} catch (Exception e) {\n");
        increaseIndentation();
        for (ASTNode statement : tryCatchFinally.catchBlock) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("} finally {\n");
        increaseIndentation();
        for (ASTNode statement : tryCatchFinally.finallyBlock) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateEnumDeclaration(EnumDeclaration enumDeclaration) {
        javaCode.append(getIndentation()).append("public enum ").append(enumDeclaration.name).append(" {\n");
        increaseIndentation();
        String values = enumDeclaration.values.stream()
                .collect(Collectors.joining(", "));
        javaCode.append(getIndentation()).append(values).append(";\n");
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateGenericDeclaration(GenericDeclaration genericDeclaration) {
        javaCode.append(getIndentation()).append("public class ").append(genericDeclaration.name);
        javaCode.append("<");
        String typeParameters = genericDeclaration.typeParameters.stream()
                .collect(Collectors.joining(", "));
        javaCode.append(typeParameters);
        javaCode.append("> {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the class

        for (ASTNode member : genericDeclaration.members) {
            generate(member);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateInlineFunction(InlineFunction inlineFunction) {
        javaCode.append(getIndentation()).append("public inline void ");
        javaCode.append(inlineFunction.name);
        javaCode.append("(");
        String parameters = inlineFunction.parameters.stream()
                .map(param -> "var " + param)
                .collect(Collectors.joining(", "));
        javaCode.append(parameters);
        javaCode.append(") {\n");

        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the function

        generate(inlineFunction.body);

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateCustomOperator(CustomOperator customOperator) {
        customOperators.put(customOperator.symbol, customOperator.functionName);
    }

    private void generateArrayDeclaration(ArrayDeclaration arrayDeclaration) {
        currentScope.declareVariable(arrayDeclaration.name, "Array");
        javaCode.append(getIndentation()).append(arrayDeclaration.elementType).append("[] ");
        javaCode.append(arrayDeclaration.name).append(" = new ").append(arrayDeclaration.elementType).append("[");
        generate(arrayDeclaration.size);
        javaCode.append("];\n");
    }

    private void generateListDeclaration(ListDeclaration listDeclaration) {
        currentScope.declareVariable(listDeclaration.name, "List");
        javaCode.append(getIndentation()).append("List<").append(listDeclaration.elementType).append("> ");
        javaCode.append(listDeclaration.name).append(" = new ArrayList<>();\n");
    }

    private void generateMapDeclaration(MapDeclaration mapDeclaration) {
        currentScope.declareVariable(mapDeclaration.name, "Map");
        javaCode.append(getIndentation()).append("Map<").append(mapDeclaration.keyType).append(", ").append(mapDeclaration.valueType).append("> ");
        javaCode.append(mapDeclaration.name).append(" = new HashMap<>();\n");
    }

    private void generateDefaultParameter(DefaultParameter defaultParameter) {
        currentScope.declareVariable(defaultParameter.name, "var");
        javaCode.append(getIndentation()).append("var ").append(defaultParameter.name).append(" = ");
        generate(defaultParameter.defaultValue);
        javaCode.append(";\n");
    }

    private void generateClosure(Closure closure) {
        javaCode.append("(");
        String parameters = closure.parameters.stream()
                .collect(Collectors.joining(", "));
        javaCode.append(parameters);
        javaCode.append(") -> {\n");
        increaseIndentation();
        Scope previousScope = currentScope;
        currentScope = new Scope(previousScope); // Create a new scope for the closure

        for (ASTNode statement : closure.body) {
            generate(statement);
        }

        currentScope = previousScope; // Restore the previous scope
        decreaseIndentation();
        javaCode.append(getIndentation()).append("};\n");
    }

    private void generateSynchronizedBlock(SynchronizedBlock synchronizedBlock) {
        javaCode.append(getIndentation()).append("synchronized (");
        generate(synchronizedBlock.expression);
        javaCode.append(") {\n");
        increaseIndentation();
        for (ASTNode statement : synchronizedBlock.body) {
            generate(statement);
        }
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    private void generateThreadCreation(ThreadCreation threadCreation) {
        javaCode.append(getIndentation()).append("new Thread(");
        generate(threadCreation.runnable);
        javaCode.append(").start();\n");
    }

    private void generateReflection(Reflection reflection) {
        javaCode.append(getIndentation()).append("Class<?> clazz = Class.forName(").append(reflection.target).append(");\n");
        switch (reflection.operation) {
            case "getMethods":
                javaCode.append(getIndentation()).append("java.lang.reflect.Method[] methods = clazz.getMethods();\n");
                break;
            case "getFields":
                javaCode.append(getIndentation()).append("java.lang.reflect.Field[] fields = clazz.getFields();\n");
                break;
            case "invokeMethod":
                javaCode.append(getIndentation()).append("java.lang.reflect.Method method = clazz.getMethod(\"methodName\", paramTypes);\n");
                javaCode.append(getIndentation()).append("Object result = method.invoke(instance, args);\n");
                break;
            case "getField":
                javaCode.append(getIndentation()).append("java.lang.reflect.Field field = clazz.getField(\"fieldName\");\n");
                javaCode.append(getIndentation()).append("Object value = field.get(instance);\n");
                break;
            default:
                throw new RuntimeException("Unknown reflection operation: " + reflection.operation);
        }
    }

    private String generateExpression(ASTNode node) {
        StringBuilder expression = new StringBuilder();
        if (node instanceof Identifier) {
            expression.append(((Identifier) node).name);
        } else if (node instanceof Literal) {
            expression.append(((Literal) node).value);
        } else if (node instanceof BinaryOperation) {
            expression.append(generateExpression(((BinaryOperation) node).left));
            expression.append(" ").append(((BinaryOperation) node).operator).append(" ");
            expression.append(generateExpression(((BinaryOperation) node).right));
        } else if (node instanceof TernaryOperation) {
            expression.append(generateExpression(((TernaryOperation) node).condition));
            expression.append(" ? ");
            expression.append(generateExpression(((TernaryOperation) node).trueExpr));
            expression.append(" : ");
            expression.append(generateExpression(((TernaryOperation) node).falseExpr));
        } else if (node instanceof LambdaExpression) {
            expression.append("(");
            String parameters = ((LambdaExpression) node).parameters.stream()
                    .collect(Collectors.joining(", "));
            expression.append(parameters);
            expression.append(") -> ");
            expression.append(generateExpression(((LambdaExpression) node).body));
        } else if (node instanceof FunctionCall) {
            expression.append(((FunctionCall) node).name);
            expression.append("(");
            String arguments = ((FunctionCall) node).arguments.stream()
                    .map(this::generateExpression)
                    .collect(Collectors.joining(", "));
            expression.append(arguments);
            expression.append(")");
        } else {
            throw new RuntimeException("Unknown expression node type: " + node.getClass());
        }
        return expression.toString();
    }

    private void increaseIndentation() {
        indentationLevel++;
    }

    private void decreaseIndentation() {
        if (indentationLevel > 0) {
            indentationLevel--;
        }
    }

    private String getIndentation() {
        return "    ".repeat(indentationLevel);
    }

    private void generateAnnotationProcessing(String classNameOrMethodName) {
        javaCode.append(getIndentation()).append("try {\n");
        increaseIndentation();
        javaCode.append(getIndentation()).append("Class<?> clazz = Class.forName(\"").append(classNameOrMethodName).append("\");\n");
        javaCode.append(getIndentation()).append("Annotation[] annotations = clazz.getAnnotations();\n");
        javaCode.append(getIndentation()).append("for (Annotation annotation : annotations) {\n");
        increaseIndentation();
        javaCode.append(getIndentation()).append("if (annotation instanceof MyAnnotation) {\n");
        increaseIndentation();
        javaCode.append(getIndentation()).append("MyAnnotation myAnnotation = (MyAnnotation) annotation;\n");
        javaCode.append(getIndentation()).append("System.out.println(\"key1: \" + myAnnotation.key1());\n");
        javaCode.append(getIndentation()).append("System.out.println(\"key2: \" + myAnnotation.key2());\n");
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
        decreaseIndentation();
        javaCode.append(getIndentation()).append("} catch (ClassNotFoundException e) {\n");
        increaseIndentation();
        javaCode.append(getIndentation()).append("e.printStackTrace();\n");
        decreaseIndentation();
        javaCode.append(getIndentation()).append("}\n");
    }

    public static void main(String[] args) {
        Tokenizer tokenizer = new Tokenizer();
        List<Tokenizer.Token> tokens = tokenizer.tokenize("val x = 10; fun main() { if (x > 5) { print(\"Greater\"); } else { print(\"Lesser\"); } for (var i = 0; i < 10; i = i + 1) { print(i); } switch(x) { case 1: print(\"One\"); break; case 10: print(\"Ten\"); break; default: print(\"Other\"); } } fun main(y: int) { print(y); } class MyClass { val a = 5; fun method() { print(a); } constructor() { a = 10; } } interface MyInterface { void doSomething(); } @MyAnnotation class AnnotatedClass { } @MyAnnotationWithParams(key1 = \"value1\", key2 = \"value2\") class AnnotatedClassWithParams { } enum MyEnum { ONE, TWO, THREE } val z = (x > 5) ? \"Greater\" : \"Lesser\"; val lambda = (a, b) -> a + b; try { print(x); } catch { print(\"Error\"); } finally { print(\"Finally\"); } class MyGenericClass<T> { fun genericMethod(param: T) { print(param); } } inline fun inlineFunction(a: int, b: int) { return a + b; } operator + = addFunction; val arr = int[5]; val list = List<String>(); val map = Map<String, Integer>(); do { print(x); x = x - 1; } while (x > 0); label: while (true) { if (x == 5) break label; if (x == 10) continue label; x = x - 1; } fun overloaded(a: int) { print(a); } fun overloaded(a: int, b: int) { print(a + b); } fun defaultParam(a: int, b: int = 5) { print(a + b); } { print(\"Closure\"); } synchronized (x) { print(x); } new Thread({ print(\"Thread\"); }); reflection getMethods(\"com.example.MyClass\"); reflection getFields(\"com.example.MyClass\");");
        tokens.add(new Tokenizer.Token("EOF", ""));
        Parser parser = new Parser(tokens);
        ASTNode program = parser.parse();
        EnhancedSemanticAnalyzer analyzer = new EnhancedSemanticAnalyzer();
        analyzer.analyze(program);
        CodeGenerator generator = new CodeGenerator();
        String javaCode = generator.generate(program);
        System.out.println(javaCode);
    }
}