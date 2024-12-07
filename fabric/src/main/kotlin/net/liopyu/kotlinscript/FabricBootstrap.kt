
package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile

class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        KotlinScriptInit.preInitialize()
        val suggestions = listKotlinDotSuggestions()

        suggestions.forEach { suggestion ->
            LogUtils.getLogger().info("Testing method: $suggestion")
        }
        val validAndRelevantSuggestions = KotlinScriptInit.testKotlinSuggestions(suggestions)

        validAndRelevantSuggestions.forEach { suggestion ->
            LogUtils.getLogger().info("Valid or relevant: $suggestion")
        }
    }
    val allowedPackages = mapOf(
        "kotlin." to { className: String -> className.startsWith("kotlin.") && !className.substringAfter("kotlin.").contains(".") },
        "kotlin.collections." to { className: String -> className.startsWith("kotlin.collections.") }
    )
    fun listKotlinDotSuggestions(): List<String> {
        val classLoader = ClassLoader.getSystemClassLoader()
        val urls = mutableListOf<URL>()

        if (classLoader is java.net.URLClassLoader) {
            urls.addAll(classLoader.urLs)
        } else {
            val classPath = System.getProperty("java.class.path")
            urls.addAll(classPath.split(File.pathSeparator).map { File(it).toURI().toURL() })
        }

        val suggestions = mutableListOf<String>()

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

                        // Check against allowed packages and their logic
                        allowedPackages.forEach { (packagePrefix, filterLogic) ->
                            if (filterLogic(className)) {
                                suggestions.addAll(getKotlinDotSuggestionsFromClass(className, packagePrefix))
                            }
                        }
                    }
                }
            } else if (file.isFile && file.extension == "jar") {
                JarFile(file).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .forEach { entry ->
                            val className = entry.name.replace("/", ".").removeSuffix(".class")

                            // Check against allowed packages and their logic
                            allowedPackages.forEach { (packagePrefix, filterLogic) ->
                                if (filterLogic(className)) {
                                    suggestions.addAll(getKotlinDotSuggestionsFromClass(className, packagePrefix))
                                }
                            }
                        }
                }
            }
        }

        return suggestions.distinct().sorted()
    }
    fun getKotlinDotSuggestionsFromClass(className: String, allowedPackage: String): List<String> {
        try {
            // Filter logic moved here for clarity
            if (!className.startsWith(allowedPackage) || className.substringAfter(allowedPackage).contains(".")) {
                return emptyList()
            }

            val clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader())
            if (!java.lang.reflect.Modifier.isPublic(clazz.modifiers)) {
                return emptyList()
            }

            val suggestions = mutableListOf<String>()

            clazz.declaredMethods.filter { method ->
                java.lang.reflect.Modifier.isPublic(method.modifiers) && java.lang.reflect.Modifier.isStatic(method.modifiers)
            }.forEach { method ->
                val sanitizedMethodName = method.name.substringBefore("-").substringBefore('$')
                if (allowedPackage == "kotlin.") {
                    suggestions.add("kotlin.$sanitizedMethodName")
                } else {
                    // For non-base kotlin packages, use only the simple name of the method
                    suggestions.add("${className.substringAfterLast('.')}.$sanitizedMethodName")
                }
            }

            // Companion object handling
            clazz.declaredClasses.firstOrNull { it.simpleName == "Companion" }?.let { companion ->
                companion.declaredMethods.filter { method ->
                    java.lang.reflect.Modifier.isPublic(method.modifiers)
                }.forEach { method ->
                    val sanitizedMethodName = method.name.substringBefore("-").substringBefore('$')
                    suggestions.add("${clazz.simpleName}.$sanitizedMethodName")
                }
            }

            return suggestions.distinct()
        } catch (e: Throwable) {
            return emptyList()
        }
    }


}