package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import java.io.File
import kotlin.script.experimental.api.*

class KotlinScriptLoader {

    companion object{

        private val DIR = "config/scripts/"
        private val scriptFileDir = File(DIR)
        @JvmStatic
        fun loadScripts() {
            scriptFileDir.mkdirs()
            scriptFileDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "kts") {
                    LogUtils.getLogger().info("Loading script: ${file.relativeTo(scriptFileDir)}...")
                    val scriptContent = file.readText()
                    KS(scriptContent).eval().logResult(file.name)
                } else if (file.isDirectory) {
                    LogUtils.getLogger().info("Scanning directory: ${file.relativeTo(scriptFileDir)}")
                }
            }
        }



        private fun ResultWithDiagnostics<EvaluationResult>.logResult(name: String) {
            onFailure {
                LogUtils.getLogger().error(name+""+ it.reports)
            }.onSuccess {
                LogUtils.getLogger().info("Script: $name successfully loaded!")
                asSuccess()
            }
        }
    }

}