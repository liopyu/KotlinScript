package me.zodd

import com.mojang.logging.LogUtils
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess

internal object KotlinScriptLoader {
    private const val DIR = "config/scripting-host/scripts/"
    private val scriptFileDir = File(DIR)
    private val logger = LogUtils.getLogger()

    fun loadScripts() {

        try {
            scriptFileDir.mkdirs()
            scriptFileDir.listFiles()?.forEach { file ->
                logger.info("Loading script : ${file.name}...")
                KotlinScript(file.readText()).eval()/*.logResult(file.name)*/
            }
        }catch (e: Exception) {
            logger.error("Error loading scripts", e)
        }

    }

    private fun ResultWithDiagnostics<EvaluationResult>.logResult(name: String) {
        onFailure {
            LogInfo(name, it.reports).printLog()
        }.onSuccess {
           /* if (Host.config.extraLogging) {
                Logger.info("Script: $name successfully loaded!")
            }*/
            asSuccess()
        }
    }
}