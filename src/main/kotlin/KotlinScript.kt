package org.example

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.logging.Logger
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class KotlinScript {
    private val logger: Logger = Logger.getLogger(KotlinScript::class.java.name)
    val scriptFiles: MutableList<File> = mutableListOf()

    private val engine by lazy {
        logger.info("Initializing the Kotlin script engine.")
        val eng = ScriptEngineManager().getEngineByExtension("kts")
        if (eng == null) {
            logger.severe("Kotlin scripting engine not found.")
            throw IllegalStateException("Kotlin scripting engine not found")
        }
        eng
    }

    fun loadScriptsFromFolder(folderPath: String) {
        logger.info("Loading scripts from folder: $folderPath")
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            logger.severe("Invalid folder path: $folderPath")
            return
        }

        val files = folder.listFiles { _, name -> name.endsWith(".kts") }
        if (files == null) {
            logger.severe("Failed to list files in folder: $folderPath")
            return
        }

        files.forEach { file ->
            if (file.isFile) {  // Ensure it's a file not a directory
                logger.info("Adding script file: ${file.name}")
                scriptFiles.add(file)
            }
        }
        logger.info("Total scripts loaded: ${scriptFiles.size}")
    }

    fun evaluateScripts() {
        logger.info("Starting script evaluation.")
        scriptFiles.forEach { file ->
            try {
                logger.info("Reading script file: ${file.name}")
                val script = file.readText(StandardCharsets.UTF_8)
                logger.info("Evaluating script file: ${file.name}")
                val result = engine.eval(script)
                logger.info("Script ${file.name} evaluated with result: $result")
            } catch (e: ScriptException) {
                logger.severe("Error evaluating script ${file.name}: ${e.localizedMessage}")
            } catch (e: IOException) {
                logger.severe("Error reading script file ${file.name}: ${e.localizedMessage}")
            }
        }
    }
}