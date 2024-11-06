

package net.liopyu.kotlinscript


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.*
import java.nio.charset.StandardCharsets
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException


val scriptFiles: MutableList<File> = mutableListOf()

val engine by lazy {
    KotlinScriptInit.LOGGER.info("Initializing the Kotlin script engine.")
    val eng = ScriptEngineManager().getEngineByExtension("kts")
    if (eng == null) {
        KotlinScriptInit.LOGGER.error("Kotlin scripting engine not found.")
        throw IllegalStateException("Kotlin scripting engine not found")
    }
    eng
}
class KotlinScript {
    companion object {
        private var instance: KotlinScript = KotlinScript()
        @JvmStatic
        val getInstance: KotlinScript
            get() {
                return instance
            }
    }
    fun loadScriptsFromFolder(folderPath: String) {
        if (scriptFiles.isNotEmpty()){
            scriptFiles.clear()
        }
        KotlinScriptInit.LOGGER.info("Loading scripts from folder: $folderPath")
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            KotlinScriptInit.LOGGER.error("Invalid folder path: $folderPath")
            return
        }

        val files = folder.listFiles { _, name -> name.endsWith(".kts") }
        if (files == null) {
            KotlinScriptInit.LOGGER.error("Failed to list files in folder: $folderPath")
            return
        }

        files.forEach { file ->
            if (file.isFile) {  // Ensure it's a file not a directory
                KotlinScriptInit.LOGGER.info("Adding script file: ${file.name}")
                scriptFiles.add(file)
            }
        }
        KotlinScriptInit.LOGGER.info("Total scripts loaded: ${scriptFiles.size}")
    }
    private fun createNewEngine(): ScriptEngine {
        KotlinScriptInit.LOGGER.info("Creating a new Kotlin script engine instance.")
        return ScriptEngineManager().getEngineByExtension("kts") ?: throw IllegalStateException("Kotlin scripting engine not found")
    }

    fun evaluateScripts() {
        KotlinScriptInit.LOGGER.info("Starting script evaluation.")
        scriptFiles.forEach { file ->
            val engine = createNewEngine() // New engine instance for each script
            try {
                KotlinScriptInit.LOGGER.info("Reading script file: ${file.name}")
                val script = file.readText(StandardCharsets.UTF_8)
                KotlinScriptInit.LOGGER.info("Evaluating script file: ${file.name}")
                val result = engine.eval(script)
                KotlinScriptInit.LOGGER.info("Script ${file.name} evaluated with result: $result")
            } catch (e: ScriptException) {
                KotlinScriptInit.LOGGER.error("Error evaluating script ${file.name}: ${e.localizedMessage}")
            } catch (e: IOException) {
                KotlinScriptInit.LOGGER.error("Error reading script file ${file.name}: ${e.localizedMessage}")
            }
        }
    }
}


object KotlinScriptInit {
    val LOGGER: Logger = LogManager.getLogger()

    fun main() {
        val kotlinscript = KotlinScript.getInstance
        kotlinscript.loadScriptsFromFolder("config/scripts")
        kotlinscript.evaluateScripts()
    }


    fun preInitialize() {
        main()
    }
}


