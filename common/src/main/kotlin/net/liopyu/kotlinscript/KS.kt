package net.liopyu.kotlinscript
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

internal data class KS(val script: String) {


    val defaultImports = listOf(
        //Kotlin Packages
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
    fun listAvailableClasses(): List<String> {
        val classLoader = ClassLoader.getSystemClassLoader()
        val urls = mutableListOf<URL>()

        if (classLoader is java.net.URLClassLoader) {
            urls.addAll(classLoader.urLs)
        } else {
            val classPath = System.getProperty("java.class.path")
            urls.addAll(classPath.split(File.pathSeparator).map { File(it).toURI().toURL() })
        }

        val classes = mutableListOf<String>()
        for (url in urls) {
            val file = File(url.toURI())
            if (file.isDirectory) {
                Files.walk(Paths.get(file.toURI())).forEach { path ->
                    if (path.toString().endsWith(".class")) {
                        val className = path.toFile()
                            .relativeTo(file)
                            .path
                            .replace(File.separator, ".")
                            .removeSuffix(".class")
                        classes.add(className)
                    }
                }
            } else if (file.isFile && file.extension == "jar") {
                JarFile(file).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .forEach { entry ->
                            val className = entry.name.replace("/", ".").removeSuffix(".class")
                            classes.add(className)
                        }
                }
            }
        }

        return classes.filter { className ->
            try {
                val clazz = Class.forName(className, false, classLoader)

                if (!java.lang.reflect.Modifier.isPublic(clazz.modifiers)) return@filter false

                val resourcePath = className.replace(".", "/") + ".class"
                val resource = classLoader.getResource(resourcePath)
                resource != null && isImportableInKts(className)
            } catch (e: Throwable) {
                false
            }
        }
    }

    // Test if the class is importable in a KTS script
    fun isImportableInKts(className: String): Boolean {
        return try {
            val clazz = Class.forName(className)
            clazz.declaredMethods.isNotEmpty() || clazz.declaredFields.isNotEmpty()
        } catch (e: Throwable) {
            false
        }
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

    fun writeClassesToFile(classes: List<String>, relativePath: String) {
        val workingDir = File(System.getProperty("user.dir"))
        println("current working directory: ${workingDir.absolutePath}")

        val targetDirectory = File(workingDir, relativePath)

        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
            println("created directory: ${targetDirectory.absolutePath}")
        }

        val jsonContent = classes.joinToString(
            prefix = "[", postfix = "]", separator = ",\n"
        ) { "\"$it\"" }
        val targetFile = File(targetDirectory, "available_classes.json")
        targetFile.writeText(jsonContent)

        println("writing to: ${targetFile.absolutePath}")
    }


    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        val result = BasicJvmScriptingHost().eval(compile(), configuration, null)
        val availableClasses = listAvailableClasses()
        writeClassesToFile(availableClasses, "config/scripts")
        return result
    }
    private fun compile(): SourceCode {
        return script.toScriptSource()
    }

}