package net.liopyu.kotlinscript;

import kotlin.script.experimental.api.EvaluationResult;
import kotlin.script.experimental.api.ResultWithDiagnostics;
import kotlin.script.experimental.api.SourceCode;
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost;
import org.example.CustomScript;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class KotlinScriptInterpreter {

    private static final Logger logger = Logger.getLogger(KotlinScript.class.getName());
    private static final File SCRIPT_DIRECTORY = new File("config/kotlinscript/scripts");

    private final List<File> scriptFiles = new ArrayList<>();
    private final BasicJvmScriptingHost scriptingHost = new BasicJvmScriptingHost();

    public KotlinScriptInterpreter() {
        if (!SCRIPT_DIRECTORY.exists()) {
            SCRIPT_DIRECTORY.mkdirs();
            logger.info("Created script directory at: " + SCRIPT_DIRECTORY.getAbsolutePath());
        }
    }

    public void loadScriptsFromFolder() {
        logger.info("Loading scripts from folder: " + SCRIPT_DIRECTORY.getAbsolutePath());

        File[] files = SCRIPT_DIRECTORY.listFiles((dir, name) -> name.endsWith(".kts"));
        if (files == null) {
            logger.severe("Failed to list files in folder: " + SCRIPT_DIRECTORY.getAbsolutePath());
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                logger.info("Adding script file: " + file.getName());
                scriptFiles.add(file);
            }
        }
        logger.info("Total scripts loaded: " + scriptFiles.size());
    }

    public void evaluateScripts() {
        logger.info("Starting script evaluation.");
        var compilationConfig = CustomScript.createScriptCompilationConfiguration();

        for (File file : scriptFiles) {
            try {
                logger.info("Reading script file: " + file.getName());
                String script = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                logger.info("Evaluating script file: " + file.getName());

                SourceCode source = CustomScript.compile(script);
                ResultWithDiagnostics<EvaluationResult> result = scriptingHost.eval(source, compilationConfig, null);

                if (result.getReports().isEmpty()) {
                    logger.info("Script " + file.getName() + " evaluated successfully.");
                } else {
                    result.getReports().forEach(report -> logger.severe(report.toString()));
                }
            } catch (IOException e) {
                logger.severe("Error reading script file " + file.getName() + ": " + e.getLocalizedMessage());
            }
        }
    }
}