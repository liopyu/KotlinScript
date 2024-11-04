package me.zodd

import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

internal data class KotlinScript(val script: String) {
    private val defaultImports = listOf(
        //Kotlin Packages
        "kotlin.reflect.*",
        "kotlin.reflect.jvm.*",
        "org.apache.logging.log4j.Logger",
        "me.zodd.*",
    )
    private fun mergeImports(): List<String> {
        val imports = mutableListOf<String>()
        imports.addAll(defaultImports)
        return imports
    }
    private val configuration = createJvmCompilationConfigurationFromTemplate<PluginScript> {
        compilerOptions("-jvm-target", "17")
        defaultImports(*mergeImports().toTypedArray())
        jvm {
            dependenciesFromClassloader(wholeClasspath = true)
        }
    }

    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        return BasicJvmScriptingHost().eval(compile(), configuration, null)
    }
    private fun compile(): SourceCode {
        return script.toScriptSource()
    }
}