package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class KotlinScriptInterpreter {
    public static final Logger LOGGER = LogUtils.getLogger();

    public KotlinScriptInterpreter() {

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
            interpretLine(line);
        }
    }

    private void interpretLine(String line) {
        // Basic implementation to interpret print statements
        if (line.trim().startsWith("print(")) {
            String content = line.trim().substring("print(".length(), line.trim().length() - 1);
            content = content.replace("\"", ""); // Remove double quotes
            LOGGER.info(content);
        } else {
            LOGGER.warn("Unrecognized line: " + line);
        }
    }
}