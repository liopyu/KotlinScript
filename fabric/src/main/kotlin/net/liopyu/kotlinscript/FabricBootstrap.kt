
package net.liopyu.kotlinscript

import net.fabricmc.api.ModInitializer
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile

class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        //TypingsDumper.dumpTypingsToJSONFile("net")
        KotlinScriptInit.preInitialize()
        val availableClasses = listAvailableClasses()
        writeClassesToFile(availableClasses, "config/scripts")
    }
    fun writeClassesToFile(classes: List<String>, relativePath: String) {
        val workingDir = File(System.getProperty("user.dir"))
        val targetDirectory = File(workingDir, relativePath)
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        val jsonContent = classes.joinToString(
            prefix = "[", postfix = "]", separator = ",\n"
        ) { "\"$it\"" }
        val targetFile = File(targetDirectory, "available_classes.json")
        targetFile.writeText(jsonContent)
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
}