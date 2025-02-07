
package net.liopyu.kotlinscript

import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.liopyu.kotlinscript.KotlinScriptInit.enrichSuggestionsWithMetadata
import net.liopyu.kotlinscript.util.KotlinObject
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile

class FabricBootstrap : ModInitializer {
    val nonBaseKotlinPackages = mapOf(
        "kotlin.collections." to { className: String -> className.startsWith("kotlin.collections.") }
    )
    val basePackage = mapOf(
        "kotlin." to { className: String -> className.startsWith("kotlin.") && !className.substringAfter("kotlin.").contains(".") }
    )
    val additionalClasses = mutableListOf<String>()

    fun addAdditionalClass(className: String) {
        additionalClasses.add(className)
    }

    override fun onInitialize() {
        KotlinScriptInit.preInitialize()
        addAdditionalClass("kotlin.collections.CollectionsKt")

        val suggestions = listBaseKotlinDotSuggestions()/* + listAdditionalClassSuggestions()*/

        suggestions.forEach { suggestion ->
            LogUtils.getLogger().info("Testing method: $suggestion")
        }

        val validAndRelevantSuggestions = KotlinScriptInit.testKotlinSuggestions(suggestions)

        val enrichedSuggestions = enrichSuggestionsWithMetadata(validAndRelevantSuggestions)

        saveSuggestionsToJson(enrichedSuggestions, "kotlin_suggestions.json")
    }

    fun extractMethodsFromClass(className: String): List<Triple<String, String, String?>> {
        val suggestions = mutableListOf<Triple<String, String, String?>>()

        // **Known Kotlin synthetic multifile classes**
        val syntheticClassNames = listOf(
            "${className}__ListsKt",
            "${className}__IteratorsKt",
            "${className}__SortingKt",
            "${className}__GroupingKt",
            "${className}__SetsKt",
            "${className}__MapsKt",
            "${className}___CollectionsJvmKt"
        )

        val possibleClasses = mutableListOf(className) + syntheticClassNames

        for (targetClass in possibleClasses) {
            try {

                val clazz = try {
                    Class.forName(targetClass, false, ClassLoader.getSystemClassLoader())
                } catch (e: ClassNotFoundException) {
                    KotlinScriptInit.logger.warn("Class not found: $targetClass")
                    continue
                }

                val allMethods = clazz.declaredMethods
                if (allMethods.isEmpty()) {
                    continue
                }

                val publicMethods = allMethods.filter { method ->
                    java.lang.reflect.Modifier.isPublic(method.modifiers)
                }

                if (publicMethods.isEmpty()) {
                    KotlinScriptInit.logger.warn("⚠ No public methods in: $targetClass")
                    continue
                }

                val staticMethods = publicMethods.filter { method ->
                    java.lang.reflect.Modifier.isStatic(method.modifiers)
                }

                if (staticMethods.isEmpty()) {
                    KotlinScriptInit.logger.warn("⚠ No public static methods in: $targetClass")
                    continue
                }

                staticMethods.forEach { method ->
                    val sanitizedMethodName = method.name.substringBefore("-").substringBefore('$')
                    suggestions.add(Triple("$targetClass.$sanitizedMethodName", sanitizedMethodName, "${clazz.simpleName}.$sanitizedMethodName"))
                }

                if (suggestions.isNotEmpty()) break

            } catch (e: Throwable) {
                KotlinScriptInit.logger.error("❌ Error extracting methods from class: $targetClass", e)
            }
        }

        return suggestions.distinct()
    }







    fun saveSuggestionsToJson(entities: List<KotlinObject>, outputPath: String) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(entities)
        File(outputPath).writeText(json)
    }

    fun listBaseKotlinDotSuggestions(): List<Triple<String, String, String?>> {
        val classLoader = ClassLoader.getSystemClassLoader()
        val urls = mutableListOf<URL>()

        if (classLoader is java.net.URLClassLoader) {
            urls.addAll(classLoader.urLs)
        } else {
            val classPath = System.getProperty("java.class.path")
            urls.addAll(classPath.split(File.pathSeparator).map { File(it).toURI().toURL() })
        }

        val suggestions = mutableListOf<Triple<String, String, String?>>()

        val allowedPackages = listOf("kotlin.", "kotlin.collections.")

        val filterLogic = { className: String ->
            allowedPackages.any { pkg -> className.startsWith(pkg) && !className.substringAfter(pkg).contains(".") }
        }

        for (url in urls) {
            val file = File(url.toURI())

            if (file.isDirectory) {
                Files.walk(Paths.get(file.toURI()))
                    .parallel()
                    .filter { it.toString().endsWith(".class") }
                    .forEach { path ->
                        val className = path.toFile().relativeTo(file).path.replace(File.separator, ".").removeSuffix(".class")
                        if (filterLogic(className)) {
                            val packagePrefix = allowedPackages.find { className.startsWith(it) } ?: "kotlin."
                            suggestions.addAll(getKotlinDotSuggestionsWithDirectAccess(className, packagePrefix))
                        }
                    }
            } else if (file.isFile && file.extension == "jar") {
                JarFile(file).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .forEach { entry ->
                            val className = entry.name.replace("/", ".").removeSuffix(".class")
                            if (filterLogic(className)) {
                                val packagePrefix = allowedPackages.find { className.startsWith(it) } ?: "kotlin."
                                suggestions.addAll(getKotlinDotSuggestionsWithDirectAccess(className, packagePrefix))
                            }
                        }
                }
            }
        }

        return suggestions.distinct()
    }

    fun getKotlinDotSuggestionsWithDirectAccess(className: String, allowedPackage: String): List<Triple<String, String, String?>> {
        try {
           // KotlinScriptInit.logger.info("Attempting to process class: $className")

            val clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader())
            if (!java.lang.reflect.Modifier.isPublic(clazz.modifiers)) {
               // KotlinScriptInit.logger.warn("Skipping non-public class: $className")
                return emptyList()
            }

            val suggestions = mutableListOf<Triple<String, String, String?>>()

            // Add the class itself
            suggestions.add(Triple(className, className.substringAfterLast('.'), null))
           // KotlinScriptInit.logger.info("Added class itself: $className")

            val packagesToCheck = listOf("kotlin.", "kotlin.collections.")

            for (pkg in packagesToCheck) {
                if (className.startsWith(pkg)) {
                    val packageName = className.substringBeforeLast(".")
                    val classPrefix = className.substringAfterLast(".")

                    val syntheticClasses = listAvailableClasses().filter {
                        it.startsWith("$packageName.${classPrefix}__") && !it.contains("$")
                    }


                    val classesToScan = listOf(className) + syntheticClasses
                    for (syntheticClass in classesToScan) {
                        try {
                            val synthClazz = Class.forName(syntheticClass, false, ClassLoader.getSystemClassLoader())

                            val staticMethods = synthClazz.declaredMethods.filter { method ->
                                java.lang.reflect.Modifier.isPublic(method.modifiers) && java.lang.reflect.Modifier.isStatic(method.modifiers)
                            }


                            staticMethods.forEach { method ->
                                val sanitizedMethodName = method.name.substringBefore("-").substringBefore('$')
                                suggestions.add(Triple("$className.$sanitizedMethodName", sanitizedMethodName, "${clazz.simpleName}.$sanitizedMethodName"))
                               // KotlinScriptInit.logger.info("Added method: $sanitizedMethodName from $syntheticClass")
                            }
                        } catch (e: Exception) {
                            KotlinScriptInit.logger.warn("Failed to process synthetic class: $syntheticClass", e)
                        }
                    }
                }
            }

            return suggestions.distinct()
        } catch (e: Throwable) {
            KotlinScriptInit.logger.error("Error processing class: $className", e)
            return emptyList()
        }
    }

    fun listAvailableClasses(): List<String> {
        val classLoader = ClassLoader.getSystemClassLoader()
        val urls = mutableListOf<URL>()

        val classPath = System.getProperty("java.class.path")
        urls.addAll(classPath.split(File.pathSeparator).map { File(it).toURI().toURL() })

        val allowedPackages = listOf("kotlin.", "kotlin.collections.")

        val classes = mutableListOf<String>()
        for (url in urls) {
            val file = File(url.toURI())
            if (file.isDirectory) {
                Files.walk(Paths.get(file.toURI()))
                    .filter { it.toString().endsWith(".class") }
                    .forEach { path ->
                        val className = path.toFile()
                            .relativeTo(file)
                            .path
                            .replace(File.separator, ".")
                            .removeSuffix(".class")

                        if (allowedPackages.any { className.startsWith(it) } &&
                            className.count { it == '.' } <= 2) {
                            classes.add(className)
                        }
                    }
            } else if (file.isFile && file.extension == "jar") {
                JarFile(file).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") }
                        .forEach { entry ->
                            val className = entry.name.replace("/", ".").removeSuffix(".class")

                            if (allowedPackages.any { className.startsWith(it) } &&
                                className.count { it == '.' } <= 2) {
                                classes.add(className)
                            }
                        }
                }
            }
        }

        return classes
    }

    fun decodeModifiers(modifiers: Int): String {
        val flags = mutableListOf<String>()

        if (java.lang.reflect.Modifier.isPublic(modifiers)) flags.add("public")
        if (java.lang.reflect.Modifier.isPrivate(modifiers)) flags.add("private")
        if (java.lang.reflect.Modifier.isProtected(modifiers)) flags.add("protected")
        if (java.lang.reflect.Modifier.isStatic(modifiers)) flags.add("static")
        if (java.lang.reflect.Modifier.isFinal(modifiers)) flags.add("final")
        if (java.lang.reflect.Modifier.isSynchronized(modifiers)) flags.add("synchronized")
        if (java.lang.reflect.Modifier.isVolatile(modifiers)) flags.add("volatile")
        if (java.lang.reflect.Modifier.isTransient(modifiers)) flags.add("transient")
        if (java.lang.reflect.Modifier.isNative(modifiers)) flags.add("native")
        if (java.lang.reflect.Modifier.isInterface(modifiers)) flags.add("interface")
        if (java.lang.reflect.Modifier.isAbstract(modifiers)) flags.add("abstract")
        if (java.lang.reflect.Modifier.isStrict(modifiers)) flags.add("strictfp")

        return flags.joinToString(" | ")
    }





}