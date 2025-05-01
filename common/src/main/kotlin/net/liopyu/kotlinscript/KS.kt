package net.liopyu.kotlinscript

import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

data class KS(val script: String) {
    val defaultImports = listOf(
        //Kotlin Packages
        "net.liopyu.kotlinscript.EventHandlers",
        "kotlin.reflect.*",
        "kotlin.reflect.jvm.*",
        "org.apache.logging.log4j.Logger",
        "com.mojang.logging.LogUtils",
        "net.liopyu.kotlinscript.util.console"
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
            dependenciesFromCurrentContext(
                wholeClasspath = true
            )
        }
    }

    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        val result = BasicJvmScriptingHost().eval(compile(), configuration, null)
        return result
    }

    private fun compile(): SourceCode {
        return script.toScriptSource()
    }


}