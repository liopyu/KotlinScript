package me.zodd

import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess

internal object KotlinScriptLoader {
    private const val DIR = "config/scripting-host/scripts/"
    private val scriptFileDir = File(DIR)

    fun loadScripts() {
        scriptFileDir.mkdirs()
        scriptFileDir.listFiles()?.forEach { file ->
            Logger.info("Loading script : ${file.name}...")
            KotlinScript(file.readText()).eval().logResult(file.name)
        }
    }

    private fun ResultWithDiagnostics<EvaluationResult>.logResult(name: String) {
        onFailure {
            LogInfo(name, it.reports).printLog()
        }.onSuccess {
            if (Host.config.extraLogging) {
                Logger.info("Script: $name successfully loaded!")
            }
            asSuccess()
        }
    }
}
