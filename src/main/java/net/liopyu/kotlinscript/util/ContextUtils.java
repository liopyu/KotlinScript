package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;

import java.util.List;

public class ContextUtils {
    public enum VariableType {
        STRING,
        OBJECT,
        NUMBER,
        CLAZZ
    }
    public static class Variable{
        public final String name;
        public final VariableType type;

        public Variable(String name, VariableType type) {
            this.name = name;
            this.type = type;
        }
    }
    public static class Parameter {
        private String name;
        private String type;

        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
    public static class Function {
        private List<Parameter> parameters;
        private ASTNode body;
        private Scope closureScope;

        public Function(List<Parameter> parameters, ASTNode body, Scope closureScope) {
            this.parameters = parameters;
            this.body = body;
            this.closureScope = closureScope;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public ASTNode getBody() {
            return body;
        }

        public Scope getClosureScope() {
            return closureScope;
        }

        public Object call(List<Object> arguments, Executor executor) {
            if (arguments.size() != parameters.size()) {
                throw new RuntimeException("Argument count mismatch");
            }

            // Create a new function scope using the closure scope
            Scope functionScope = new Scope(closureScope);

            // Declare each parameter in the new function scope with its corresponding argument
            for (int i = 0; i < parameters.size(); i++) {
                ContextUtils.Parameter parameter = parameters.get(i);
                Object argument = arguments.get(i);

                // Use parameter.getName() to get the String name of the parameter
                functionScope.declareVariable(parameter.getName(), argument);
            }

            // Push the new scope, execute the body, and pop the scope
            executor.pushScope(functionScope);
            body.accept(executor);
            executor.popScope();

            // Return the last evaluated value from the body execution
            return executor.getLastEvaluatedValue();
        }
    }

    public static class FunctionDefinition {
        List<String> parameters;
        ASTNode body;

        public FunctionDefinition(List<String> parameters, ASTNode body) {
            this.parameters = parameters;
            this.body = body;
        }
    }
}
