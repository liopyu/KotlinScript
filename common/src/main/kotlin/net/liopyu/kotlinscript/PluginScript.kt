package net.liopyu.kotlinscript

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = ScriptConfiguration::class
)

abstract class PluginScript

object ScriptConfiguration : ScriptCompilationConfiguration({
    ide.acceptedLocations(ScriptAcceptedLocation.Everywhere)
    compilerOptions("-jvm-target", "17")
    defaultImports(KS.defaultImports)
    jvm {
        dependenciesFromClassloader(
            classLoader = sharedClassLoader,
            wholeClasspath = true
        )

    }
})

