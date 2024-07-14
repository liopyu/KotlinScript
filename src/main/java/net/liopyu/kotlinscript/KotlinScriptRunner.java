package net.liopyu.kotlinscript;

import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory;


import org.slf4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
public class KotlinScriptRunner {
    private final Logger logger;

    public KotlinScriptRunner(Logger logger) {
        this.logger = logger;
    }

    public void executeKotlinScript(File scriptFile) {
        try {
            // Create a Kotlin script engine
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByExtension("main.kts");

            if (engine == null) {
                logger.error("Kotlin script engine not found");
                return;
            }

            // Read the script file content
            String script = new String(Files.readAllBytes(Paths.get(scriptFile.toURI())));

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
        } catch (IOException e) {
            logger.error("Error reading Kotlin script", e);
        }
    }
}