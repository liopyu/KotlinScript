package net.liopyu.kotlinscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class LambdaContext {
    private String body;
    private Map<String, String> variables = new HashMap<>();

    public LambdaContext(String body) {
        this.body = body;
        KotlinScriptInterpreter.LOGGER.info("[LambdaContext] Initialized with body: " + body);
    }

    public void addVariable(String name, String value) {
        variables.put(name, value);
        KotlinScriptInterpreter.LOGGER.info("[LambdaContext] Variable added: " + name + " = " + value);
    }

    public String processInput(String inputVariableName, String input) {
        String processed = body.replace("$" + inputVariableName, input);
        KotlinScriptInterpreter.LOGGER.info("[LambdaContext] Processing input: " + inputVariableName + " = " + input);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            processed = processed.replaceAll("\\$" + entry.getKey(), Matcher.quoteReplacement(entry.getValue()));
            KotlinScriptInterpreter.LOGGER.info("[LambdaContext] Replacing $" + entry.getKey() + " with " + entry.getValue());
        }

        KotlinScriptInterpreter.LOGGER.info("[LambdaContext] Processed body: " + processed);
        return processed;
    }
}
