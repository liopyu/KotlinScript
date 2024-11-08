package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess
class KotlinScriptLoader {

    companion object{
        private val DIR = "config/scripts/"
        private val scriptFileDir = File(DIR)
        @JvmStatic
        fun loadScripts() {
            scriptFileDir.mkdirs()
            scriptFileDir.listFiles()?.forEach { file ->
                LogUtils.getLogger().info("Loading script : ${file.name}...")
                KS(file.readText()).eval().logResult(file.name)
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