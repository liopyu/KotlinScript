package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import net.liopyu.kotlinscript.util.TestParser
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
val instanceDir = File(System.getProperty("user.dir"))
val sourcesDir = File(instanceDir, "kotlinsources")
val mappingsFile = File(sourcesDir, "mappings.tiny")


val sharedClassLoader = URLClassLoader(
    arrayOf(sharedScriptDir.toURI().toURL()),
    KS::class.java.classLoader
)


val sharedCompilationConfig = createJvmCompilationConfigurationFromTemplate<PluginScript> {
    compilerOptions("-jvm-target", "17")
    jvm {
        dependenciesFromClassloader(
            classLoader = sharedClassLoader,
            wholeClasspath = true
        )
    }
}
val sharedEvalConfig = ScriptEvaluationConfiguration {
    jvm {
        baseClassLoader(sharedClassLoader)
    }
    providedProperties("x" to "Hello")
}

data class KS(val scriptFile: File) {
    companion object {
        var globalBindings: Map<String, Any> = emptyMap()
        val defaultImports = listOf(
            "net.liopyu.kotlinscript.util.KUtils",
            "com.mojang.logging.LogUtils"
        )
    }

    fun eval(context: Map<String, Any> = emptyMap()): ResultWithDiagnostics<EvaluationResult> {
        val obfScript = TestParser.main(
            scriptFile.absoluteFile.readText().trimIndent()
        )

        val config = sharedEvalConfig.with {
            providedProperties(context)
        }
        val compilationConfig = sharedCompilationConfig.with {
            if (context.isNotEmpty()) {
                set(providedProperties, context.mapValues { KotlinType(it.value::class) })
            }
        }
        LogUtils.getLogger().info("Obfuscated Script: ${obfScript}")
        return BasicJvmScriptingHost().eval(
            obfScript.toScriptSource(),
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