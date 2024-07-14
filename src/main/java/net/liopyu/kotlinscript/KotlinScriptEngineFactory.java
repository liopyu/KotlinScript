package net.liopyu.kotlinscript;


import kotlin.KotlinVersion;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.List;

public class KotlinScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
        return "Custom Kotlin Script Engine";
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        return List.of("kts");
    }

    @Override
    public List<String> getMimeTypes() {
        return List.of("text/x-kotlin");
    }

    @Override
    public List<String> getNames() {
        return List.of("KotlinScriptEngine", "kotlin", "Kotlin");
    }

    @Override
    public String getLanguageName() {
        return "Kotlin";
    }

    @Override
    public String getLanguageVersion() {
        return KotlinVersion.CURRENT.toString();
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.NAME:
                return getLanguageName();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            default:
                return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        String argsList = String.join(", ", args);
        return obj + "." + m + "(" + argsList + ")";
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "println(\"" + toDisplay.replace("\"", "\\\"") + "\")";
    }

    @Override
    public String getProgram(String... statements) {
        return "fun main() {\n" + String.join("\n", statements) + "\n}";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new KotlinScriptEngine(this);
    }
}
