package me.zodd

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = ScriptConfiguration::class
)
abstract class PluginScript

object ScriptConfiguration : ScriptCompilationConfiguration({
    ide.acceptedLocations(ScriptAcceptedLocation.Everywhere)
    compilerOptions("-jvm-target", "17")
    /*val jarUrl = this::class.java.getResource("/${this::class.java.name.replace('.', '/')}.class")
    val jarPath = jarUrl?.toString()?.substringAfter("file:")?.substringBeforeLast("!") ?: error("Unable to determine jar path")
   */ jvm {
        /*updateClasspath(listOf(File(jarPath)))*/
        dependenciesFromClassloader(
            wholeClasspath = true
        )
    }
})

