package net.liopyu.kotlinscript;

import java.nio.charset.StandardCharsets;

import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory;
import org.slf4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
public class KotlinScriptEvaluator {
    private final Logger logger;

    public KotlinScriptEvaluator(Logger logger) {
        this.logger = logger;
    }

    public void executeKotlinScript(String scriptFileName) {
        try {
            // Create a Kotlin script engine
            ScriptEngine engine = new KotlinJsr223JvmLocalScriptEngineFactory().getScriptEngine();

            // Read the script file content
            InputStream scriptStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
            if (scriptStream == null) {
                logger.error("Script file not found: " + scriptFileName);
                return;
            }

            String script = new Scanner(scriptStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();

            // Redirect the script output to the logger
            engine.getContext().setWriter(new java.io.PrintWriter(new java.io.StringWriter() {
                @Override
                public void write(String str) {
                    logger.info(str);
                }
            }));

            // Evaluate the script
            engine.eval(script);
        } catch (ScriptException e) {
            logger.error("Error executing Kotlin script", e);
        } catch (Exception e) {
            logger.error("Error reading Kotlin script", e);
        }
    }
}