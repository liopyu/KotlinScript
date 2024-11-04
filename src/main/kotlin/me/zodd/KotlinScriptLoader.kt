package me.zodd

import java.io.File
import kotlin.script.experimental.api.*

internal object KotlinScriptLoader {
    private const val DIR = "config/scripting-host/scripts/"
    private val scriptFileDir = File(DIR)
    fun loadScripts() {
        scriptFileDir.mkdirs()
        scriptFileDir.listFiles()?.forEach { file ->
            Host.logger.info("Loading script : ${file.name}...")
            KotlinScript(file.readText()).eval()/*.logResult(file.name)*/
        }
    }
    private fun ResultWithDiagnostics<EvaluationResult>.logResult(name: String) {
        onFailure {
            Host.logger.error(name+ " "+ it.reports)
        }.onSuccess {
            Host.logger.info("Script: $name successfully loaded!")
            asSuccess()
        }
    }
}
