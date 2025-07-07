package net.liopyu.kotlinscript

import java.io.File
import java.net.URLClassLoader
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private val sharedScriptDir = File("config/scripts")

private val sharedClassLoader = URLClassLoader(
    arrayOf(sharedScriptDir.toURI().toURL()),
    KS::class.java.classLoader
)
private val sharedCompilationConfig = createJvmCompilationConfigurationFromTemplate<PluginScript> {
    compilerOptions("-jvm-target", "17")
    //set(providedProperties, mapOf("x" to KotlinType(String::class)))
    jvm {
        dependenciesFromClassloader(
            classLoader = sharedClassLoader,
            wholeClasspath = true
        )
    }
}
public val sharedEvalConfig = ScriptEvaluationConfiguration {

    jvm {
        baseClassLoader(sharedClassLoader)
    }
    providedProperties("x" to "Hello")
}

class KS {
    companion object {
        var globalBindings: Map<String, Any> = emptyMap()
        val defaultImports = listOf(
            "net.liopyu.kotlinscript.util.KUtils",
            "com.mojang.logging.LogUtils",
            "net.liopyu.kotlinscript.util.console"
        )
    }

    val utilityScript = "val y = 10".toScriptSource()

    fun eval(scriptFile: File, context: Map<String, Any> = emptyMap()): ResultWithDiagnostics<EvaluationResult> {
        val config = sharedEvalConfig.with {
            providedProperties(context)
        }
        val compilationConfig = sharedCompilationConfig.with {
            if (context.isNotEmpty()) {
                set(providedProperties, context.mapValues { KotlinType(it.value::class) })
            }
        }

        return BasicJvmScriptingHost().eval(
            scriptFile.toScriptSource(),
            compilationConfig,
            config
        )
    }

}

data class KSText(val script: String) {
    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        return BasicJvmScriptingHost().eval(
            script.toScriptSource(),
            sharedCompilationConfig,
            sharedEvalConfig
        )
    }
}