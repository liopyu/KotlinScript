package net.liopyu.kotlinscript;

import javax.script.*;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public class KotlinScriptEngine extends AbstractScriptEngine {

    private final KotlinScriptEngineFactory factory;

    public KotlinScriptEngine(KotlinScriptEngineFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        // Implementation of script evaluation using the Kotlin compiler
        return eval(new StringReader(script), context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        // Convert Reader to String and evaluate using the Kotlin compiler
        // This is a simplified version; actual implementation should handle errors and compilation
        return "Evaluated Kotlin Script";
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return this.factory;
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }
}

