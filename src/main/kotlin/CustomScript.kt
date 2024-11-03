package org.example

// Kotlin file
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = CustomScriptConfiguration::class
)
abstract class CustomScript {
    companion object {
        @JvmStatic
        fun createScriptCompilationConfiguration(): ScriptCompilationConfiguration {
            return createJvmCompilationConfigurationFromTemplate<CustomScript> {
                defaultImports("kotlin.math.*")
                jvm {
                    dependenciesFromCurrentContext(wholeClasspath = true)
                }
            }
        }
        @JvmStatic
        fun compile(script: String): SourceCode {
            return script.toScriptSource()
        }
    }
}


object CustomScriptConfiguration : ScriptCompilationConfiguration({
    ide.acceptedLocations(ScriptAcceptedLocation.Everywhere)
    compilerOptions("-jvm-target", "17")
    jvm {
        dependenciesFromClassloader(
            classLoader = CustomScript::class.java.classLoader,
            wholeClasspath = true
        )

    }


})
