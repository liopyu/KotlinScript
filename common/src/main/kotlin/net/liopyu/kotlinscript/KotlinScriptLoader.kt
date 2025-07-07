package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import java.io.File
import kotlin.script.experimental.api.*

class KotlinScriptLoader {

    companion object {

        private val DIR = "config/scripts/"
        private val scriptFileDir = File(DIR)

        @JvmStatic
        fun loadScripts() {
            scriptFileDir.mkdirs()
            val logger = LogUtils.getLogger()

            val evalContext = mutableMapOf<String, Any>()
            scriptFileDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "kts") {
                    //  logger.info("Loading script: ${file.relativeTo(scriptFileDir)}...")
                    val result = KS().eval(file, evalContext)
                    if (result is ResultWithDiagnostics.Success) {
                        /* val evaluationResult = result.value
                         val returnVal = (evaluationResult.returnValue as? ResultValue.Value)?.value
 */
                        /*   if (returnVal != null) {
                               evalContext["x"] = returnVal
                               logger.info("ReturnVal: " + returnVal)
                           }
   */
                        result.reports.forEach {
                            if (it.isError()) {
                                logger.error(it.message)
                            }
                        }
                    } else {
                        // logger.error("Script evaluation failed for file: ${file.name}")
                        result.reports.forEach {
                            if (it.isError()) {
                                logger.error(it.message)
                            }
                        }
                    }
                } else if (file.isDirectory) {
                    // logger.info("Scanning directory: ${file.relativeTo(scriptFileDir)}")
                }
            }
        }


        private fun ResultWithDiagnostics<EvaluationResult>.logResult(name: String) {
            onFailure {
                it.reports.forEach {
                    if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
                        println("$name : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
                    }
                }
            }.onSuccess {
                LogUtils.getLogger().info("Script: $name successfully loaded!")
                asSuccess()
            }
        }
    }

}
