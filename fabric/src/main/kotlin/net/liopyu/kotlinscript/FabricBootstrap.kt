package net.liopyu.kotlinscript

import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import io.github.classgraph.AnnotationInfoList
import io.github.classgraph.ClassGraph
import io.github.classgraph.FieldInfo
import kotlinx.coroutines.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree
import net.liopyu.kotlinscript.util.*
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.Opcodes.ASM9
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.system.measureTimeMillis


@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Deprecated

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class MyAnno(val description: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NotEmpty

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Size(val max: Int)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Tag(val value: Array<String>)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SuppressWarnings(vararg val value: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Named(val value: String)


@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class AnnotationWithDefault(val value: String = "", val flag: Boolean = false)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class JsonProperty(val required: Boolean)


val logger = LogUtils.getLogger()
/*
val instanceDir = File(System.getProperty("user.dir"))
val sourcesDir = File(instanceDir, "kotlinsources")

fun main() {
    val otherFile = File(sourcesDir, "mappings.tiny")
    val mappingsFile = File.createTempFile("mappings", ".tiny").apply {
        outputStream().use { out ->
            otherFile.inputStream().use { input ->
                input.copyTo(out)
            }
        }
        deleteOnExit()
    }
    tinyToJson(mappingsFile)
    println(resolveClassName("net.minecraft.class_310")) // → com.mojang.math.Axis
    println(resolveFieldName("field_35642"))              // → REMOTE_ADDRESS
    println(resolveMethodName("method_39494"))            // → commitEvent

}

val runtimeTinyMappings: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
val fieldIndex: MutableMap<String, String> = mutableMapOf()
val methodIndex: MutableMap<String, String> = mutableMapOf()
val fieldIndexBuckets: MutableMap<Int, MutableMap<Int, MutableMap<String, String>>> = mutableMapOf()
val methodIndexBuckets: MutableMap<Int, MutableMap<Int, MutableMap<String, String>>> = mutableMapOf()

fun resolveClassName(obfClass: String): String? {
    return runtimeTinyMappings[obfClass]
        ?.get("deobf")
        ?.toString()
        ?.replace('/', '.')
}

fun resolveFieldName(obfField: String): String? {
    return fieldIndex[obfField] ?: run {
        fieldIndexBuckets[
            obfField.substringAfter('_').toIntOrNull()?.div(1000) ?: return null
        ]?.get(
            (obfField.substringAfter('_').toIntOrNull()?.div(10))?.rem(100) ?: return null
        )?.get(obfField)
    }
}

fun resolveMethodName(obfMethod: String): String? {
    return methodIndex[obfMethod] ?: run {
        methodIndexBuckets[
            obfMethod.substringAfter('_').toIntOrNull()?.div(1000) ?: return null
        ]?.get(
            (obfMethod.substringAfter('_').toIntOrNull()?.div(10))?.rem(100) ?: return null
        )?.get(obfMethod)
    }
}


fun tinyToJson(inputFile: File) {
    var currentClass: String? = null

    inputFile.forEachLine { rawLine ->
        val line = rawLine.trimEnd()
        if (line.isEmpty()) return@forEachLine

        val tokens = line.split("\t")
        val keyword = tokens.firstOrNull { it.isNotBlank() } ?: return@forEachLine

        when (keyword) {
            "c" -> if (tokens.size >= 4) {
                val obfName = tokens[2].replace('/', '.')
                val deobfName = tokens[3]
                currentClass = obfName
                runtimeTinyMappings[currentClass!!] = mutableMapOf(
                    "deobf" to deobfName,
                    "methods" to mutableMapOf<String, String>(),
                    "fields" to mutableMapOf<String, String>()
                )
            }

            "f" -> if (tokens.size >= 6 && currentClass != null) {
                val obfField = tokens[4]
                val deobfField = tokens[5]
                val fields = runtimeTinyMappings[currentClass]?.get("fields") as MutableMap<String, String>
                fields[obfField] = deobfField
                fieldIndex[obfField] = deobfField

                val num = obfField.substringAfter('_').toIntOrNull()
                if (num != null) {
                    val tens = num / 1000
                    val ones = (num / 10) % 100
                    val tier1 = fieldIndexBuckets.getOrPut(tens) { mutableMapOf() }
                    val tier2 = tier1.getOrPut(ones) { mutableMapOf() }
                    tier2[obfField] = deobfField
                }
            }

            "m" -> if (tokens.size >= 6 && currentClass != null) {
                val obfMethod = tokens[4]
                val deobfMethod = tokens[5]
                val methods = runtimeTinyMappings[currentClass]?.get("methods") as MutableMap<String, String>
                methods[obfMethod] = deobfMethod
                methodIndex[obfMethod] = deobfMethod

                val num = obfMethod.substringAfter('_').toIntOrNull()
                if (num != null) {
                    val tens = num / 1000
                    val ones = (num / 10) % 100
                    val tier1 = methodIndexBuckets.getOrPut(tens) { mutableMapOf() }
                    val tier2 = tier1.getOrPut(ones) { mutableMapOf() }
                    tier2[obfMethod] = deobfMethod
                }
            }
        }
    }
}*/

@Retention(AnnotationRetention.RUNTIME)
annotation class KDoc(val value: String)
class FabricBootstrap : ModInitializer {


    @KDoc(
        """
    Entry point for KotlinScript mod initialization.

    This method sets up the necessary directory structure, scans for Kotlin source files, and generates suggestion metadata
    for code completion and documentation tools. It also extracts class and companion object information from the loaded
    environment using ClassGraph.

    Key actions:
    - Validates presence of the `kotlinsources` folder
    - Extracts top-level Kotlin functions
    - Deduplicates and enriches function metadata
    - Dumps class and companion object data into JSON format for later use

    ```kt
    // Example usage during mod bootstrap:
    onInitialize()
    ```

    @see extractTopLevelFunctions
    @see dumpClassesToFile
    @see dumpCompanionObjectsToFile
    """
    )
    override fun onInitialize() {
        val tree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings()
        buildMappingsIfNeeded(tree, 0, 1)

        KotlinScriptInit.preInitialize()
        if (!sourcesDir.exists()) {
            LogUtils.getLogger()
                .warn("Kotlin sources folder not found at ${sourcesDir.absolutePath}, creating it now...")
            sourcesDir.mkdirs()
        }

        if (!sourcesDir.isDirectory) {
            LogUtils.getLogger().error("Failed to create Kotlin sources directory at ${sourcesDir.absolutePath}")
            return
        }
        /* val functions = extractTopLevelFunctions(sourcesDir.absolutePath)
         val uniqueFunctions = functions.distinctBy { it.fullyQualifiedName }
         val occurrenceMap = mutableMapOf<String, Int>()
         val enrichedSuggestions = uniqueFunctions.map { obj ->
             val pureSimpleName = obj.fullyQualifiedName
                 .substringBefore('(')
                 .substringAfterLast('.')
             val currentCount = occurrenceMap.getOrDefault(pureSimpleName, 0)
             occurrenceMap[pureSimpleName] = currentCount + 1
             val newFullyQualifiedName = if (currentCount > 0) obj.fullyQualifiedName else pureSimpleName
             obj.copy(
                 fullyQualifiedName = newFullyQualifiedName,
             )
         }
         saveSuggestionsToJson(enrichedSuggestions, File(sourcesDir, "kotlin_suggestions.json").absolutePath)
        */
        val l = listOf(
            "net.liopyu.kotlinscript.Utils",
        )


        val list = l//dumpClassesToFile(sourcesDir)
        //dumpClassesToFile(sourcesDir, list, "available_members.json")
        //dumpCompanionObjectsToFile(sourcesDir, list, "companion_objects.json")

    }

    fun isDevEnvironment(): Boolean {
        return FabricLoader.getInstance().isDevelopmentEnvironment
    }

    fun buildDeobfToObfMapFromTree(tree: MappingTree, obfNamespace: Int, deobfNamespace: Int) {
        val namespaces = listOf(tree.srcNamespace) + tree.dstNamespaces
        var classCount = 0
        var skippedClassCount = 0

        for (classDef in tree.classes) {
            val names = mutableListOf<String?>()
            for (i in namespaces.indices) {
                val name = try {
                    classDef.getName(i)
                } catch (_: Exception) {
                    null
                }
                names.add(name)
            }

            val obfClass = if (obfNamespace < names.size) names[obfNamespace]?.replace('/', '.') else null
            val deobfClass = if (deobfNamespace < names.size) names[deobfNamespace]?.replace('/', '.') else null

            if (obfClass == null || deobfClass == null) {
                skippedClassCount++
                continue
            }

            deobfToObfClassMap[deobfClass] = obfClass
            deobfToObfClassMap[deobfClass.substringAfterLast('.')] = obfClass
            if ('$' in deobfClass) {
                deobfToObfClassMap[deobfClass.replace('$', '.')] = obfClass
                deobfToObfClassMap[deobfClass.substringAfterLast('$')] = obfClass
            }
            obfToDeobfClassMap[obfClass] = deobfClass
            if ('$' in obfClass) {
                obfToDeobfClassMap[obfClass.replace('$', '.')] = deobfClass
            }
            val methodMap = deobfToObfMethodMap.getOrPut(deobfClass) { mutableMapOf() }
            val obfMethodMap = obfToDeobfMethodMap.getOrPut(obfClass) { mutableMapOf() }
            val arityIndex = deobfToObfMethodByArity.getOrPut(deobfClass) { mutableMapOf() }
            val overloads = deobfMethodOverloads.getOrPut(deobfClass) { mutableMapOf() }

            for (methodDef in classDef.methods) {
                val methodNames = mutableListOf<String?>()
                for (i in namespaces.indices) {
                    val n = try {
                        methodDef.getName(i)
                    } catch (_: Exception) {
                        null
                    }
                    methodNames.add(n)
                }
                val obfMethod = if (obfNamespace < methodNames.size) methodNames[obfNamespace] else null
                val deobfMethod = if (deobfNamespace < methodNames.size) methodNames[deobfNamespace] else null
                val obfDesc = try {
                    methodDef.getDesc(obfNamespace)
                } catch (_: Exception) {
                    null
                }
                val deobfDesc = try {
                    methodDef.getDesc(deobfNamespace)
                } catch (_: Exception) {
                    null
                }
                if (obfMethod != null && deobfMethod != null && obfDesc != null && deobfDesc != null) {
                    val deobfKey = "$deobfMethod$deobfDesc"
                    val obfKey = "$obfMethod$obfDesc"
                    methodMap[deobfKey] = obfMethod
                    obfMethodMap[obfKey] = deobfMethod
                    val ar = countParams(deobfDesc)
                    val byArity = arityIndex.getOrPut(deobfMethod) { mutableMapOf() }
                    val prev = byArity.putIfAbsent(ar, obfMethod)
                    if (prev != null && prev != obfMethod) byArity[ar] = ""
                    overloads.getOrPut(deobfMethod) { mutableListOf() }.add(deobfDesc to obfMethod)
                }
            }

            val fieldMap = deobfToObfFieldMap.getOrPut(deobfClass) { mutableMapOf() }
            val obfFieldMap = obfToDeobfFieldMap.getOrPut(obfClass) { mutableMapOf() }
            val fieldTypesForOwner = deobfFieldTypeMap.getOrPut(deobfClass) { mutableMapOf() }


            for (fieldDef in classDef.fields) {
                val obfDesc = runCatching { fieldDef.getDesc(obfNamespace) }.getOrNull()
                val fieldNames = mutableListOf<String?>()
                for (i in namespaces.indices) {
                    val n = try {
                        fieldDef.getName(i)
                    } catch (_: Exception) {
                        null
                    }
                    fieldNames.add(n)
                }
                val obfField = if (obfNamespace < fieldNames.size) fieldNames[obfNamespace] else null
                val deobfField = if (deobfNamespace < fieldNames.size) fieldNames[deobfNamespace] else null

                var namedType: String? = runCatching { fieldDef.getDesc(deobfNamespace) }.getOrNull()?.let { d ->
                    jvmFieldDescToClassName(d)
                }

                if (namedType == null) {
                    val obfDesc = runCatching { fieldDef.getDesc(obfNamespace) }.getOrNull()
                    if (obfDesc != null) {
                        val obfType =
                            jvmFieldDescToClassName(obfDesc)
                        if (obfType != null) {
                            namedType =
                                obfToDeobfClassMap[obfType]
                                    ?: obfToDeobfClassMap[obfType.replace('$', '.')]
                                            ?: obfToDeobfClassMap[obfType.replace(
                                        '.',
                                        '$'
                                    )]
                        }
                    }
                }

                if (deobfField != null && namedType != null) {
                    val canon = canonicalDeobfClass(namedType)
                    fieldTypesForOwner[deobfField] = canon
                    obfFieldTypeDeobfMap.getOrPut(obfClass) { mutableMapOf() }[obfField!!] = canon
                }


                if (obfField != null && deobfField != null) {
                    fieldMap[deobfField] = obfField
                    obfFieldMap[obfField] = deobfField
                }

                if (obfField != null && obfDesc != null) {
                    obfFieldDescriptorMap.getOrPut(obfClass) { mutableMapOf() }[obfField] = obfDesc
                }
            }

            classCount++
        }
    }

    fun buildMappingsIfNeeded(
        tree: net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree,
        obfNamespace: Int,
        deobfNamespace: Int
    ) {
        if (!isDevEnvironment()) {
            loadMappingsFromResource()
        } else {
            val mappingFile = getDevMappingFile()
            if (!mappingFile.exists()) {
                buildDeobfToObfMapFromTree(tree, obfNamespace, deobfNamespace)
            } else {
                loadMappingsFromResource()
            }
            saveMappingsToJson()
        }
    }

    fun extractKDocFromAnnotations(annotations: List<Annotation>): String? {
        return annotations
            .find { it.annotationClass.qualifiedName == "net.liopyu.kotlinscript.KDoc" }
            ?.let { annotation ->
                val raw = annotation.annotationClass.members
                    .find { it.name == "value" }
                    ?.call(annotation) as? String
                raw?.trimMargin()
            }
    }

    fun extractKDocFromAnnotations(annotations: AnnotationInfoList?): String? {
        return annotations
            ?.find {
                /*  if (it.name == "net.liopyu.kotlinscript.KDoc")
                      LogUtils.getLogger().info("Found annotation: " + it.name)*/
                it.name == "net.liopyu.kotlinscript.KDoc"
            }
            ?.parameterValues
            ?.get("value")
            ?.value
            ?.toString()
            ?.trimMargin()
    }


    fun dumpCompanionObjectsToFile(sourcesDir: File, filteredClasses: List<String>, fileName: String) {
        val jsonOutputPath = File(sourcesDir, fileName).apply {
            parentFile.mkdirs()
        }
        val objectClassMethods = setOf("toString", "hashCode", "equals")
        val companionMap = mutableMapOf<String, MutableMap<String, Any>>()
        val scanResult = ClassGraph()
            .enableClassInfo()
            .enableMethodInfo()
            .enableFieldInfo()
            .enableAnnotationInfo()
            .enableSystemJarsAndModules()
            .scan()
        scanResult.allClasses
            .filter { classInfo ->
                val deobf = resolveClassName(classInfo.name) ?: classInfo.name
                deobf in filteredClasses &&
                        deobf.endsWith("\$Companion") &&
                        classInfo.constructorInfo.toString().contains("synthetic")
            }
            .forEach { clazz ->
                val deobf = resolveClassName(clazz.name) ?: clazz.name
                val baseOuterName = deobf
                    .removeSuffix("\$Companion")
                    .substringBefore('$')
                val outerClass = scanResult.getClassInfo(baseOuterName)
                if ((outerClass == null || !outerClass.isPublic)) {
                    return@forEach
                }
                try {
                    val companionEntry = mutableMapOf<String, Any>()
                    val someClass = Class.forName(clazz.name, false, ClassLoader.getSystemClassLoader())


                    // Safely inspect functions
                    someClass.kotlin.functions
                        .filter {
                            val filteredParams = if (
                                it.parameters.isNotEmpty() &&
                                deepResolveType(it.parameters.first().type.toString()) == someClass.kotlin.qualifiedName
                            ) {
                                it.parameters.drop(1)
                            } else {
                                it.parameters
                            }
                            val resolvedArgs = filteredParams.map { deepResolveType(it.type.toString()) }
                            it.visibility == KVisibility.PUBLIC && (resolveMethodName(
                                clazz.name,
                                it.name,
                                resolvedArgs
                            )?.split("(")
                                ?.get(0)
                                ?: it.name) !in objectClassMethods
                        }
                        .forEach { method ->
                            val filteredParams = if (
                                method.parameters.isNotEmpty() &&
                                deepResolveType(method.parameters.first().type.toString()) == someClass.kotlin.qualifiedName
                            ) {
                                method.parameters.drop(1)
                            } else {
                                method.parameters
                            }
                            val resolvedArgs = filteredParams.map { deepResolveType(it.type.toString()) }
                            val methodName = resolveMethodName(clazz.name, method.name, resolvedArgs)?.split("(")
                                ?.get(0) ?: method.name
                            val returnType = deepResolveType(method.returnType.toString())

                            val methodEntry = mutableMapOf<String, Any>()

                            val isOperator = resolveMethodName(clazz.name, method.name, resolvedArgs)?.split("(")
                                ?.get(0) == "invoke" &&
                                    method.parameters.count { it.kind == KParameter.Kind.VALUE } == 0 &&
                                    method.isOperator

                            if (isOperator) {
                                methodEntry["isInvokeOperator"] = true
                            }

                            methodEntry["returns"] = returnType


                            if (filteredParams.isNotEmpty()) {
                                methodEntry["args"] = filteredParams.map { deepResolveType(it.type.toString()) }
                            }

                            val description = extractKDocFromAnnotations(method.annotations)
                            if (!description.isNullOrBlank()) {
                                methodEntry["description"] = description
                            }

                            companionEntry["$methodName()"] = methodEntry
                        }


                    // Safely inspect properties
                    someClass.kotlin.memberProperties
                        .filter { it.visibility == KVisibility.PUBLIC }
                        .forEach { field ->
                            val fieldEntry = mutableMapOf<String, Any>()

                            val resolvedFieldName = resolveFieldName(clazz.name, field.name) ?: field.name
                            val resolvedType = deepResolveType(field.returnType.toString())

                            fieldEntry["type"] = resolvedType
                            companionEntry[resolvedFieldName] = fieldEntry

                            val description = extractKDocFromAnnotations(field.annotations)
                            if (!description.isNullOrBlank()) {
                                fieldEntry["description"] = description
                            }
                        }


                    if (companionEntry.isNotEmpty()) {
                        companionMap[deobf.removeSuffix("\$Companion")] = companionEntry
                    }
                } catch (e: Throwable) {
                    logger.warn("⚠️ Skipped class due to error (${deobf}): ${e::class.simpleName}: ${e.message}")
                }
            }

        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

        val jsonOutput = gson.toJson(companionMap)
        jsonOutputPath.writeText(jsonOutput)
        logger.info(" Dumped companion object data to ${jsonOutputPath.absolutePath}")
    }

    fun dumpClassesWithFernFlower(sourcesDir: File) {
        runBlocking {
            val sourcesOutputPath = File(sourcesDir, "sources").apply { mkdirs() }

            val classList = ClassGraph()
                .enableClassInfo()
                //.acceptPackages("net") // Filter by "net" only for debugging
                .enableSystemJarsAndModules()
                .scan()
                .allClasses
                .filter { it.isPublic }
                .filter { !it.name.matches(Regex(".*\\$\\d+")) } // Exclude inner/anonymous classes

            val batchSize = 500  // Tune this value based on system performance
            val classBatches = classList.chunked(batchSize)

            val totalTime = measureTimeMillis {
                classBatches.mapIndexed { index, batch ->
                    async(Dispatchers.IO) {
                        processBatchWithFernFlower(batch, sourcesOutputPath, "batch_$index")
                    }
                }.awaitAll()
            }

            //  println("[INFO] Decompilation completed in ${totalTime}ms")
        }
    }

    suspend fun processBatchWithFernFlower(
        classBatch: List<io.github.classgraph.ClassInfo>,
        sourcesOutputPath: File,
        batchName: String
    ) {
        val tempClassDir = Files.createTempDirectory("fernflower_temp_classes_$batchName").toFile()
        val fernflowerOutputDir = Files.createTempDirectory("fernflower_output_$batchName").toFile()

        // Extract `.class` files concurrently
        withContext(Dispatchers.IO) {
            classBatch.forEach { classInfo ->
                try {
                    val className = classInfo.name
                    val classPath = className.replace('.', '/') + ".class"
                    val resource = classInfo.resource?.load()

                    if (resource != null) {
                        val tempClassFile = File(tempClassDir, classPath).apply {
                            parentFile.mkdirs()
                            writeBytes(resource)
                        }
                    }
                } catch (e: Exception) {
                    println("[ERROR] Failed to extract class: ${classInfo.name}")
                }
            }
        }

        try {
            val fernflowerArgs = arrayOf(
                "-dgs=1",                 // Decompile generic signatures
                "-rsy=1",                 // Remove synthetic classes
                tempClassDir.absolutePath, // Input: temp class folder
                fernflowerOutputDir.absolutePath // Output: Decompiled source folder
            )

            ConsoleDecompiler.main(fernflowerArgs)

            fernflowerOutputDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "java") {
                    val relativePath = file.relativeTo(fernflowerOutputDir)
                    val finalOutputPath = File(sourcesOutputPath, relativePath.path)

                    finalOutputPath.parentFile.mkdirs()
                    file.copyTo(finalOutputPath, overwrite = true)
                }
            }
        } catch (e: Exception) {
            println("[ERROR] FernFlower decompilation failed for batch: $batchName - ${e.message}")
        }
    }

    fun dumpClassesAsSources(sourcesDir: File) {
        val sourcesOutputPath = File(sourcesDir, "sources").apply { mkdirs() }

        val classList = ClassGraph()
            .enableClassInfo()
            .enableSystemJarsAndModules()
            .acceptPackages(
                "java",
                "javax",
                "kotlin",
                "com",
                "org",
                "net",
                "io"
            ) // Exclude macOS-specific and native bindings
            .scan()
            .allClasses
            .filter { it.isPublic }
            .filter { !it.name.matches(Regex(".*\\$\\d+")) }

        classList.forEach { classInfo ->
            try {
                val className = classInfo.name
                val classPath = className.replace('.', '/')
                val outputPath = File(sourcesOutputPath, "$classPath.java")

                outputPath.parentFile.mkdirs()

                val classBytes = classInfo.resource.load()
                if (classBytes != null) {
                    PrintWriter(outputPath).use { writer ->
                        writer.println("package ${className.substringBeforeLast('.')};")
                        writer.println()
                        writer.println(decompileClassFromBytes(classBytes, className))
                    }
                }
            } catch (_: Exception) {
                // Swallow errors silently to avoid logging
            }
        }
    }

    fun decompileClassFromBytes(classBytes: ByteArray, className: String): String {
        val classVisitor = StringBuilder()

        val cw = object : ClassVisitor(ASM9) {
            override fun visit(
                version: Int, access: Int, name: String,
                signature: String?, superName: String?, interfaces: Array<out String>?
            ) {
                val modifiers = Modifier.toString(access)
                classVisitor.append("$modifiers class ${className.substringAfterLast('.')}")

                if (superName != null && superName != "java/lang/Object") {
                    classVisitor.append(" extends ${superName.replace('/', '.')}")
                }

                interfaces?.let {
                    if (it.isNotEmpty()) {
                        classVisitor.append(" implements ${it.joinToString { iface -> iface.replace('/', '.') }}")
                    }
                }

                classVisitor.append(" {\n")
            }

            override fun visitField(
                access: Int, name: String, descriptor: String?,
                signature: String?, value: Any?
            ): FieldVisitor? {
                if (access and ACC_SYNTHETIC != 0) return null

                val modifiers = Modifier.toString(access)
                classVisitor.append("    $modifiers ${Type.getType(descriptor).className} $name;\n")
                return super.visitField(access, name, descriptor, signature, value)
            }

            override fun visitMethod(
                access: Int, name: String, descriptor: String?,
                signature: String?, exceptions: Array<out String>?
            ): MethodVisitor? {
                if (access and ACC_SYNTHETIC != 0) return null

                val modifiers = Modifier.toString(access)
                val returnType = Type.getReturnType(descriptor).className
                val parameters = Type.getArgumentTypes(descriptor)
                    .mapIndexed { index, paramType -> "${paramType.className} param$index" }
                    .joinToString(", ")

                if (name == "<init>") {
                    classVisitor.append("    $modifiers ${className.substringAfterLast('.')}($parameters) {}\n")
                } else {
                    classVisitor.append("    $modifiers $returnType $name($parameters)")

                    exceptions?.let {
                        if (it.isNotEmpty()) {
                            classVisitor.append(" throws ${it.joinToString { ex -> ex.replace('/', '.') }}")
                        }
                    }

                    classVisitor.append(" {}\n")
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }

            override fun visitEnd() {
                classVisitor.append("}")
            }
        }

        val cr = ClassReader(classBytes)
        cr.accept(cw, 0)

        return """
        package ${className.substringBeforeLast('.')};
        
        ${classVisitor.toString().trim()}
    """.trimIndent()
    }

    fun inspectClassDetails(name: String) {
        val classInfo = ClassGraph()
            .enableClassInfo()
            .enableMethodInfo()
            .enableFieldInfo()
            .enableAnnotationInfo()
            .enableExternalClasses()
            .enableSystemJarsAndModules()
            .scan()
            .allClasses
            .firstOrNull { it.name == name }

        if (classInfo != null) {
            LogUtils.getLogger().info("Class Details for ${classInfo.name}")

            // Core Class Information
            LogUtils.getLogger().info(" - Is Public: ${classInfo.isPublic}")
            LogUtils.getLogger().info(" - Is Private: ${classInfo.isPrivate}")
            LogUtils.getLogger().info(" - Is Protected: ${classInfo.isProtected}")
            LogUtils.getLogger().info(" - Is Abstract: ${classInfo.isAbstract}")
            LogUtils.getLogger().info(" - Is Synthetic: ${classInfo.isSynthetic}")
            LogUtils.getLogger().info(" - Is Final: ${classInfo.isFinal}")
            LogUtils.getLogger().info(" - Is Static: ${classInfo.isStatic}")
            LogUtils.getLogger().info(" - Is Interface: ${classInfo.isInterface}")
            LogUtils.getLogger().info(" - Is Enum: ${classInfo.isEnum}")
            LogUtils.getLogger().info(" - Is Record: ${classInfo.isRecord}")
            LogUtils.getLogger().info(" - Is Standard Class: ${classInfo.isStandardClass()}")
            LogUtils.getLogger().info(" - Is Inner Class: ${classInfo.isInnerClass}")
            LogUtils.getLogger().info(" - Is Outer Class: ${classInfo.isOuterClass}")
            LogUtils.getLogger().info(" - Is Anonymous Inner Class: ${classInfo.isAnonymousInnerClass}")
            LogUtils.getLogger().info(" - Is External Class: ${classInfo.isExternalClass()}")

            // Classfile Info
            LogUtils.getLogger()
                .info(" - Classfile Version: ${classInfo.getClassfileMajorVersion()}.${classInfo.getClassfileMinorVersion()}")

            // Package and Path Information
            LogUtils.getLogger().info(" - Package Name: ${classInfo.getPackageName()}")
            LogUtils.getLogger().info(" - Class Path: ${classInfo.getClasspathElementFile()}")

            // Superclass & Interface Information
            LogUtils.getLogger().info(" - Superclasses: ${classInfo.getSuperclasses().map { it.name }}")
            LogUtils.getLogger().info(" - Interfaces: ${classInfo.getInterfaces().map { it.name }}")
            LogUtils.getLogger().info(" - Implemented Interfaces: ${classInfo.getInterfaces().map { it.name }}")

            // Outer & Inner Classes
            LogUtils.getLogger().info(" - Outer Classes: ${classInfo.getOuterClasses().map { it.name }}")
            LogUtils.getLogger().info(" - Inner Classes: ${classInfo.getInnerClasses().map { it.name }}")

            // Field Information
            val fields = classInfo.fieldInfo
                .joinToString("\n") { fieldInfo ->
                    "   - ${fieldInfo.name}: ${fieldInfo.typeSignatureOrTypeDescriptor}"
                }
            if (fields.isNotEmpty()) {
                LogUtils.getLogger().info("Fields:\n$fields")
            } else {
                LogUtils.getLogger().info(" - No Fields Found")
            }

            // Method Information
            val methods = classInfo.methodInfo
                .joinToString("\n") { methodInfo ->
                    val params = methodInfo.parameterInfo.joinToString(", ") { param ->
                        "${param.typeSignatureOrTypeDescriptor} ${param.name ?: ""}".trim()
                    }
                    "   - ${methodInfo.name}(${params})"
                }
            if (methods.isNotEmpty()) {
                LogUtils.getLogger().info("Methods:\n$methods")
            } else {
                LogUtils.getLogger().info(" - No Methods Found")
            }

            // Annotation Information
            val annotations = classInfo.annotationInfo
                .joinToString("\n") { annotationInfo ->
                    "   - ${annotationInfo.name}"
                }
            if (annotations.isNotEmpty()) {
                LogUtils.getLogger().info("Annotations:\n$annotations")
            } else {
                LogUtils.getLogger().info(" - No Annotations Found")
            }

            // Module Information
            val moduleInfo = classInfo.getModuleInfo()
            if (moduleInfo != null) {
                LogUtils.getLogger().info(" - Module Name: ${moduleInfo.name}")
            }

            // Source and Origin Information
            LogUtils.getLogger().info(" - Source Location: ${classInfo.getClasspathElementFile()}")


        } else {
            LogUtils.getLogger().info("Class `$name` not found.")
        }
    }

    fun dumpFilteredKotlinClassesToFile(sourcesDir: File, allowedPackages: List<String>): List<String> {
        val binOutputPath = File(sourcesDir, "available_kotlin_classes.bin").apply {
            parentFile.mkdirs()
        }

        val classList = CopyOnWriteArrayList<String>()
        val futures = ClassGraph()
            .enableClassInfo()
            .enableSystemJarsAndModules()
            .enableAnnotationInfo()
            .scan()
            .allClasses
            .filter { classInfo ->
                val classPackage = classInfo.packageName
                allowedPackages.any { allowed ->
                    classPackage == allowed
                } && classInfo.isPublic && !classInfo.name.matches(Regex(".*\\$\\d+"))
            }
            .map { clazz ->
                if (clazz.hasAnnotation("kotlin.jvm.JvmName") || (clazz.name.startsWith("kotlin") && clazz.name.endsWith(
                        "Kt"
                    ))
                ) {
                    KotlinScriptInit.isValidImport(clazz.name, "import").thenAccept { isValidImport ->
                        if (isValidImport) {
                            classList.add(clazz.name)
                        }
                    }
                } else {
                    classList.add(clazz.name)
                    CompletableFuture.completedFuture(true)
                }
            }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        binOutputPath.writeText(classList.joinToString("\n"))
        LogUtils.getLogger().info(" Dumped ${classList.size} filtered Kotlin classes to ${binOutputPath.absolutePath}")
        return classList
    }

    fun loadMappings(
        file: File,
        fromNamespace: String = "official",
        toNamespace: String = "named"
    ): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        var namespaceOrder: List<String> = emptyList()
        file.useLines { lines ->
            for (line in lines) {
                val parts = line.split('\t')

                if (parts[0] == "tiny") {
                    namespaceOrder = parts.drop(3)
                    continue
                }

                if (parts[0] == "c") {
                    val intermediary = parts.getOrNull(1)?.replace('/', '.')
                    val official = parts.getOrNull(2)?.replace('/', '.')
                    val named = parts.getOrNull(3)?.replace('/', '.')

                    val map = mapOf(
                        "intermediary" to intermediary,
                        "official" to official,
                        "named" to named
                    )

                    val from = map[fromNamespace]
                    val to = map[toNamespace]

                    if (!from.isNullOrEmpty() && !to.isNullOrEmpty()) {
                        mappings[from] = to
                    }
                }
            }
        }

        return mappings
    }

    fun dumpClassesToFile(sourcesDir: File): List<String> {
        val binOutputPath = File(sourcesDir, "available_classes.bin").apply {
            parentFile.mkdirs()
        }

        val classList = CopyOnWriteArrayList<String>()
        val futures = ClassGraph()
            .enableAllInfo()
            .enableSystemJarsAndModules()
            .scan()
            .allClasses
            .filter {
                val resolvedName = resolveClassName(it.name) ?: it.name
                !resolvedName.matches(Regex(".*\\$\\d+"))/*it.name.startsWith("net.minecraft")*/
            }
            .map { clazz ->
                val originalName = clazz.name
                val resolvedName = resolveClassName(originalName) ?: originalName
                if (clazz.hasAnnotation("kotlin.jvm.JvmName") || (originalName.startsWith("kotlin") && originalName.endsWith(
                        "Kt"
                    ))
                ) {
                    KotlinScriptInit.isValidImport(resolvedName, "import").thenAccept { isValidImport ->
                        if (isValidImport) {
                            classList.add(resolvedName)
                        }
                    }
                } else {
                    classList.add(resolvedName)
                    CompletableFuture.completedFuture(true)
                }
            }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        binOutputPath.writeText(classList.joinToString("\n"))
        LogUtils.getLogger().info("Dumped ${classList.size} importable classes to ${binOutputPath.absolutePath}")
        return classList
    }


    private val kotlinCorePackages = setOf(
        "kotlin",
        "kotlin.io",
        "kotlin.text",
        "kotlin.collections",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.comparisons",
        "kotlin.annotation"
    )

    private fun buildCoreKotlinTypeMap(entities: List<KotlinObject>): Map<String, String> {
        return entities
            .filter { it.path in kotlinCorePackages }
            .associate { entity ->
                // Extract just the last part of the fullyQualifiedName without args
                val simpleName = entity.fullyQualifiedName
                    .substringBefore('(') // cut off arguments (if any)
                    .substringAfterLast('.') // then get simple name
                simpleName to entity.fullyQualifiedName
            }
    }


    private val coreKotlinTypeMap: Map<String, String> by lazy {
        val classIndex = mutableMapOf<String, String>()
        ClassGraph()
            .enableClassInfo()
            .scan()
            .allClasses
            .filter { classInfo ->
                classInfo.packageName in kotlinCorePackages
            }
            .forEach { classInfo ->
                classIndex[classInfo.simpleName] = classInfo.name

                classInfo.innerClasses.forEach { inner ->
                    val nestedFqName = "${classInfo.name}.${inner.simpleName}"
                    classIndex[inner.simpleName] = nestedFqName
                }
            }
        classIndex + mapOf(
            "CharSequence" to "kotlin.CharSequence",
            "Array" to "kotlin.Array",
            "ByteArray" to "kotlin.ByteArray",
            "ShortArray" to "kotlin.ShortArray",
            "IntArray" to "kotlin.IntArray",
            "LongArray" to "kotlin.LongArray",
            "FloatArray" to "kotlin.FloatArray",
            "DoubleArray" to "kotlin.DoubleArray",
            "CharArray" to "kotlin.CharArray",
            "BooleanArray" to "kotlin.BooleanArray"
        )
    }

    private fun resolveReturnTypeFqName(
        typeRef: KtTypeReference?,
        file: KtFile,
        currentPackage: String,
        classLookup: Map<String, KotlinObject>,
        coreKotlinTypeMap1: Map<String, String>
    ): String {
        val unwrappedTypeElement = when (val elem = typeRef?.typeElement) {
            is KtNullableType -> elem.innerType
            else -> elem
        }

        val newMap = coreKotlinTypeMap + coreKotlinTypeMap1

        if (unwrappedTypeElement is KtFunctionType) {
            //logger.info("Detected FUNCTION TYPE (Lambda / Predicate / Consumer)")

            val paramTypes = unwrappedTypeElement.parameters.map { param ->
                param.typeReference?.let { paramTypeRef ->
                    val paramResolved =
                        resolveReturnTypeFqName(paramTypeRef, file, currentPackage, classLookup, coreKotlinTypeMap1)
                    // logger.info("    Function Param Type: $paramResolved")
                    paramResolved
                } ?: "Unknown"
            }

            val returnType = unwrappedTypeElement.returnTypeReference?.let { returnTypeRef ->
                resolveReturnTypeFqName(returnTypeRef, file, currentPackage, classLookup, coreKotlinTypeMap1)
            } ?: "kotlin.Unit"

            // logger.info("    Function Return Type: $returnType")

            val allTypes = (paramTypes + returnType).joinToString(",")
            return "kotlin.Function${paramTypes.size}<$allTypes>"
        }

        val typeName = (unwrappedTypeElement as? KtUserType)?.referencedName ?: return "kotlin.Unit"

        val imports = file.importDirectives
            .filter { !it.isAllUnder }
            .mapNotNull { directive ->
                directive.importedFqName?.asString()?.let { fq -> fq.substringAfterLast('.') to fq }
            }
            .toMap()

        val wildcardImports = file.importDirectives
            .filter { it.isAllUnder }
            .mapNotNull { it.importedFqName?.asString() }

        if (typeName in imports) {
            return imports[typeName]!!
        }
        for (pkg in wildcardImports) {
            val candidate = "$pkg.$typeName"
            val fromLookup = classLookup[typeName]
            if (newMap[typeName] == candidate || (fromLookup != null && fromLookup.path == pkg)) {
                return candidate
            }
            try {
                val clazz = Class.forName(candidate)
                if (clazz.`package`?.name == pkg) {
                    return candidate
                }
            } catch (_: ClassNotFoundException) {
            }
        }
        newMap[typeName]?.let { return it }
        if (file.declarations.any { it is KtClassOrObject && it.name == typeName }) {
            return "$currentPackage.$typeName"
        }
        classLookup[typeName]?.let { knownClass ->
            return "${knownClass.path}.$typeName"
        }
        if (typeName.length == 1 && typeName[0].isUpperCase()) {
            //logger.info("Detected GENERIC type (RETURN): $typeName")
            return "RETURN"
        }
        val javaLangClassName = "java.lang.$typeName"
        return try {
            Class.forName(javaLangClassName)
            javaLangClassName
        } catch (e: ClassNotFoundException) {
            "$currentPackage.$typeName"
        }
    }

    fun logModifiers(declaration: KtDeclaration) {
        val modifiers = mutableListOf<String>()

        val modifierTokens = TokenSet.andSet(KtTokens.MODIFIER_KEYWORDS, KtTokens.KEYWORDS).types
            .filterIsInstance<KtModifierKeywordToken>()

        declaration.modifierList?.let { list ->
            for (token in modifierTokens) {
                if (list.hasModifier(token)) {
                    modifiers.add(token.value)
                }
            }
        }

        when (declaration) {
            is KtNamedFunction -> modifiers.add("fun")
            is KtProperty -> modifiers.add(if (declaration.isVar) "var" else "val")
            is KtObjectDeclaration -> modifiers.add("object")
            is KtClass -> {
                if (declaration.isInterface()) {
                    modifiers.add("interface")
                } else {
                    modifiers.add("class")
                }
            }

            is KtEnumEntry -> modifiers.add("enum")
        }

        logger.info("Modifiers: ${modifiers.joinToString(" ")}")
    }

    fun extractTopLevelFunctions(sourcePath: String): List<KotlinObject> {
        val disposable: Disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration().apply {
            put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                org.jetbrains.kotlin.cli.common.messages.MessageCollector.NONE
            )
            put(CommonConfigurationKeys.MODULE_NAME, "extraction")
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
            addKotlinSourceRoot(sourcePath)
        }
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val ktFiles: List<KtFile> = environment.getSourceFiles()
        val entities = mutableListOf<KotlinObject>()
        val classEntities = mutableListOf<KotlinObject>()
        val tempClassEntities = mutableListOf<KotlinObject>()
        val classGraphNames = ClassGraph()
            .enableClassInfo()
            .scan()
            .allClasses
            .map { it.name }
            .toSet()
        val classLookup = classEntities
            .filter { it.fullyQualifiedName in classGraphNames }
            .associateBy {
                it.fullyQualifiedName.substringAfterLast('.').substringBefore('(')
            }

        val coreKotlinTypeMapFromEntities = buildCoreKotlinTypeMap(classEntities)
        val finalCoreKotlinTypeMap = coreKotlinTypeMap + coreKotlinTypeMapFromEntities
        for (file in ktFiles) {
            val pkg = file.packageFqName.asString()
            file.declarations.filterIsInstance<KtClassOrObject>().filter { !it.isPrivate() }
                .forEach { classDeclaration ->
                    val className = classDeclaration.name ?: return@forEach
                    val fqName = if (pkg.isNotEmpty()) "$pkg.$className" else className
                    tempClassEntities.add(
                        KotlinObject(
                            fullyQualifiedName = fqName,
                            source = "",
                            type = "class",
                            path = pkg,
                            parentType = "",
                            requiresImport = pkg.isNotEmpty() && pkg !in kotlinCorePackages,
                            returnType = ""
                        )
                    )
                }
        }
        val fullClassLookup = tempClassEntities
            .associateBy {
                it.fullyQualifiedName.substringAfterLast('.').substringBefore('(')
            }
        val finalCoreKotlinTypeMap2 = coreKotlinTypeMap + buildCoreKotlinTypeMap(tempClassEntities)
        for (file in ktFiles) {
            val pkg = file.packageFqName.asString()
            val sourceName = file.name.substringBeforeLast(".")
            file.declarations.filterIsInstance<KtClassOrObject>().filter { !it.isPrivate() }
                .forEach { classDeclaration ->
                    val className = classDeclaration.name ?: return@forEach
                    if ("kotlin" != pkg) return@forEach
                    val fqName = if (pkg.isNotEmpty()) "$pkg.$className" else className
                    val primaryConstructor = classDeclaration.primaryConstructor
                    val visibility = primaryConstructor?.visibilityModifierType()
                    val constructorArgs = if ((visibility == KtTokens.PUBLIC_KEYWORD || visibility == null) &&
                        !classDeclaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                    ) {
                        primaryConstructor?.valueParameters
                            ?.filter {
                                !it.hasValOrVar() || it.hasModifier(KtTokens.PUBLIC_KEYWORD) || !it.hasModifier(KtTokens.PRIVATE_KEYWORD)
                            }
                            ?.map { param ->
                                val paramTypeRef = param.typeReference
                                if (paramTypeRef != null) {
                                    resolveReturnTypeFqName(
                                        paramTypeRef,
                                        file,
                                        pkg,
                                        fullClassLookup,
                                        finalCoreKotlinTypeMap2
                                    )
                                } else {
                                    "Unknown"
                                }
                            } ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val members = mutableListOf<KotlinObject>()
                    for (declaration in classDeclaration.declarations) {
                        when (declaration) {
                            is KtNamedFunction -> {
                                val memberName = declaration.name ?: continue
                                val returnType = resolveReturnTypeFqName(
                                    declaration.typeReference,
                                    file,
                                    pkg,
                                    fullClassLookup,
                                    finalCoreKotlinTypeMap2
                                )
                                val args = declaration.valueParameters.map { param ->
                                    resolveReturnTypeFqName(
                                        param.typeReference,
                                        file,
                                        pkg,
                                        fullClassLookup,
                                        finalCoreKotlinTypeMap2
                                    )
                                }

                                val fullFqName = if (args.isEmpty()) "$fqName.$memberName"
                                else "$fqName.$memberName(${args.joinToString(",")})"

                                members.add(
                                    KotlinObject(
                                        fullyQualifiedName = fullFqName,
                                        source = memberName,
                                        type = if (declaration.hasModifier(KtTokens.OPERATOR_KEYWORD) && memberName == "invoke") "invoke" else "method",
                                        path = pkg,
                                        parentType = fqName,
                                        requiresImport = false,
                                        returnType = returnType
                                    )
                                )
                            }

                            is KtProperty -> {
                                val memberName = declaration.name ?: continue
                                val typeRef = declaration.typeReference
                                val returnType =
                                    resolveReturnTypeFqName(
                                        typeRef,
                                        file,
                                        pkg,
                                        fullClassLookup,
                                        finalCoreKotlinTypeMap2
                                    )

                                members.add(
                                    KotlinObject(
                                        fullyQualifiedName = "$fqName.$memberName",
                                        source = memberName,
                                        type = "field",
                                        path = pkg,
                                        parentType = fqName,
                                        requiresImport = false,
                                        returnType = returnType
                                    )
                                )
                            }

                            is KtObjectDeclaration -> {
                                // Optional: handle companion objects if needed
                            }
                        }
                    }
                    val typeParams = classDeclaration.typeParameterList
                        ?.parameters
                        ?.map { param ->
                            val name = param.name ?: return@map null
                            val extendsBound = param.extendsBound?.text?.takeIf { it != "Any" }
                            if (extendsBound != null) "$name : $extendsBound" else name
                        }
                        ?.filterNotNull()
                        ?: emptyList()
                    val allKotlinTokens = listOf(
                        KtTokens.PUBLIC_KEYWORD, KtTokens.PRIVATE_KEYWORD, KtTokens.PROTECTED_KEYWORD,
                        KtTokens.ABSTRACT_KEYWORD, KtTokens.FINAL_KEYWORD, KtTokens.OPEN_KEYWORD,
                        KtTokens.SEALED_KEYWORD, KtTokens.ENUM_KEYWORD, KtTokens.DATA_KEYWORD,
                        KtTokens.ANNOTATION_KEYWORD, KtTokens.INNER_KEYWORD, KtTokens.EXTERNAL_KEYWORD,
                        KtTokens.CONST_KEYWORD, KtTokens.LATEINIT_KEYWORD,
                        KtTokens.SUSPEND_KEYWORD, KtTokens.TAILREC_KEYWORD, KtTokens.OPERATOR_KEYWORD,
                        KtTokens.INFIX_KEYWORD, KtTokens.NOINLINE_KEYWORD, KtTokens.CROSSINLINE_KEYWORD,
                        KtTokens.REIFIED_KEYWORD, KtTokens.EXPECT_KEYWORD, KtTokens.ACTUAL_KEYWORD,
                        KtTokens.HEADER_KEYWORD, KtTokens.IMPL_KEYWORD, KtTokens.FUN_KEYWORD
                    )

                    val kotlinMods = allKotlinTokens
                        .filter { classDeclaration.hasModifier(it) }
                        .map { it.value }

                    val javaMods = convertKotlinModifiersToJava(kotlinMods, ModifierContext.CLASS)


                    classEntities.add(
                        KotlinObject(
                            fullyQualifiedName = fqName,
                            source = sourceName,
                            type = "class",
                            path = pkg,
                            parentType = "",
                            requiresImport = pkg.isNotEmpty() && pkg !in kotlinCorePackages,
                            returnType = "",
                            members,
                            typeParameters = typeParams,
                            modifiers = javaMods
                        )
                    )
                }
        }
        for (file in ktFiles) {
            val pkg = file.packageFqName.asString()
            val sourceName = file.name.substringBeforeLast(".")
            file.declarations.filterIsInstance<KtNamedFunction>()
                .filter { !it.hasModifier(KtTokens.PRIVATE_KEYWORD) }
                .forEach { function ->
                    function.name?.let { name ->
                        if ("emptyList" in name)
                            logger.info("found empty list: " + name)
                        val receiverType = resolveReturnTypeFqName(
                            function.receiverTypeReference,
                            file,
                            pkg,
                            classLookup,
                            finalCoreKotlinTypeMap
                        )
                        val baseFqName = if (pkg.isNotEmpty()) "$pkg.$name" else name
                        val parameterTypes = function.valueParameters.map { param ->
                            val typePath = resolveReturnTypeFqName(
                                param.typeReference,
                                file,
                                pkg,
                                classLookup,
                                finalCoreKotlinTypeMap
                            )
                            val isNullable = (param.typeReference?.typeElement as? KtNullableType) != null
                            if (isNullable) "$typePath?" else typePath
                        }
                        val fullFqName = when {
                            parameterTypes.isEmpty() -> baseFqName
                            else -> "$baseFqName(${parameterTypes.joinToString(",")})"
                        }
                        val type = when {
                            function.hasModifier(KtTokens.INFIX_KEYWORD) && receiverType == "RETURN" -> "infix_lambda"
                            function.hasModifier(KtTokens.INFIX_KEYWORD) -> "infix"
                            receiverType == "RETURN" -> "lambda"
                            else -> "method"
                        }
                        val requiresImport = pkg.isNotEmpty() && pkg !in kotlinCorePackages
                        val returnType = resolveReturnTypeFqName(
                            function.typeReference,
                            file,
                            pkg,
                            classLookup,
                            finalCoreKotlinTypeMap
                        )
                        entities.add(
                            KotlinObject(
                                fullyQualifiedName = fullFqName,
                                source = sourceName,
                                type = type,
                                path = pkg,
                                parentType = receiverType,
                                requiresImport = requiresImport,
                                returnType = returnType
                            )
                        )
                    }
                }
        }
        Disposer.dispose(disposable)
        val file = File(sourcesDir, "kotlin_members.json")
        writeAvailableMembersJson(file, classEntities)

        return entities
    }

    enum class ModifierContext {
        CLASS, METHOD, FIELD
    }

    fun formatModifiers(modifiers: Int, context: ModifierContext): String {
        val parts = mutableListOf<String>()

        // Handle visibility first (only one should be present)
        when {
            Modifier.isPublic(modifiers) -> parts.add("public")
            Modifier.isProtected(modifiers) -> parts.add("protected")
            Modifier.isPrivate(modifiers) -> parts.add("private")
        }

        when (context) {
            ModifierContext.CLASS -> {
                if (Modifier.isAbstract(modifiers)) parts.add("abstract")
                if (Modifier.isFinal(modifiers)) parts.add("final")
                if (Modifier.isInterface(modifiers)) parts.add("interface")
                if (Modifier.isStrict(modifiers)) parts.add("strictfp")
            }

            ModifierContext.METHOD -> {
                if (Modifier.isAbstract(modifiers)) parts.add("abstract")
                if (Modifier.isFinal(modifiers)) parts.add("final")
                if (Modifier.isStatic(modifiers)) parts.add("static")
                if (Modifier.isSynchronized(modifiers)) parts.add("synchronized")
                if (Modifier.isNative(modifiers)) parts.add("native")
                if (Modifier.isStrict(modifiers)) parts.add("strictfp")
            }

            ModifierContext.FIELD -> {
                if (Modifier.isStatic(modifiers)) parts.add("static")
                if (Modifier.isFinal(modifiers)) parts.add("final")
                if (Modifier.isTransient(modifiers)) parts.add("transient")
                if (Modifier.isVolatile(modifiers)) parts.add("volatile")
            }
        }

        return parts.joinToString(" ")
    }

    fun convertKotlinModifiersToJava(kotlinModifiers: List<String>, context: ModifierContext): List<String> {
        val mapped = mutableListOf<String>()

        for (mod in kotlinModifiers) {
            when (mod) {
                "public", "private", "protected" -> mapped.add(mod)

                "abstract" -> if (context != ModifierContext.FIELD) mapped.add("abstract")
                "final" -> if (context != ModifierContext.METHOD || !mapped.contains("abstract")) mapped.add("final")
                "open" -> {} // No direct equivalent, ignore
                "sealed" -> mapped.add("sealed") // No Java equivalent, kept as-is
                "enum" -> mapped.add("enum")
                "annotation" -> mapped.add("annotation")
                "static" -> mapped.add("static")
                "native" -> mapped.add("native")
                "strictfp" -> mapped.add("strictfp")
                "synchronized" -> if (context == ModifierContext.METHOD) mapped.add("synchronized")
                "transient" -> if (context == ModifierContext.FIELD) mapped.add("transient")
                "volatile" -> if (context == ModifierContext.FIELD) mapped.add("volatile")

                else -> mapped.add(mod) // fallback for "data", "inline", "suspend", etc.
            }
        }

        return mapped
    }


    fun deepResolveType(type: String): String {
        val pattern = Regex("""[\w.$/]+(?:\$[\w$]+)?""") // matches nested and inner class names
        return pattern.replace(type) { match ->
            resolveClassName(match.value) ?: match.value
        }
    }

    fun dumpClassesToFile(sourcesDir: File, targetClasses: List<String>, fileName: String) {
        val jsonOutputPath = File(sourcesDir, fileName).apply { parentFile.mkdirs() }
        val objectClassMethods = setOf("toString", "hashCode", "equals")
        val classMap = mutableMapOf<String, MutableMap<String, Any>>()
        val excludedPackages = listOf(
            "java.util.jar", "java.util.zip", "java.net", "sun", "com.sun", "io.netty",
            "org.objectweb.asm", "org.spongepowered.asm", "org.openjdk.nashorn", "jdk.nashorn"
        )

        try {
            ClassGraph()
                .enableAllInfo()
                .enableSystemJarsAndModules()
                .scan()
                .allClasses
                .filter { classInfo ->
                    val actualClassName = resolveClassName(classInfo.name) ?: classInfo.name
                    val isCompanion = actualClassName.endsWith("\$Companion") &&
                            classInfo.constructorInfo.toString().contains("synthetic")
                    actualClassName in targetClasses &&
                            !isCompanion &&
                            !excludedPackages.any { actualClassName.startsWith(it) }
                }

                .forEach { clazz ->
                    val deobfClassName = resolveClassName(clazz.name) ?: clazz.name
                    val classEntry = mutableMapOf<String, Any>()

                    val resolvedSuperclass = clazz.superclass?.name?.let { resolveClassName(it) ?: it } ?: ""
                    classEntry["\$superclass"] = resolvedSuperclass

                    val resolvedInterfaces =
                        clazz.interfaces.map { resolveClassName(it.name) ?: it.name }.toTypedArray()
                    classEntry["\$interfaces"] = resolvedInterfaces

                    val classModifiers = formatModifiers(clazz.modifiers, ModifierContext.CLASS)
                    classEntry["\$modifiers"] = classModifiers
                    val genericParams = clazz.typeSignatureOrTypeDescriptor?.typeParameters
                    if (!genericParams.isNullOrEmpty()) {
                        classEntry["\$typeParameters"] = genericParams.map { param ->
                            val allBounds = buildList {
                                param.classBound?.let { add(it) }
                                param.interfaceBounds?.let { addAll(it) }
                            }

                            val resolvedBounds = allBounds
                                .map {
                                    val raw = it.toString()
                                    val cleaned = raw.replace(Regex("^@[^\\s]+\\s*\\(.*?\\)?\\s*"), "")
                                    deepResolveType(cleaned)
                                }
                                .filter { it != "java.lang.Object" }

                            if (resolvedBounds.isEmpty()) {
                                param.name
                            } else {
                                "${param.name} : ${resolvedBounds.joinToString(" & ")}"
                            }
                        }
                    }

                    clazz.methodInfo
                        .filter { method ->
                            val resolvedArgs = method.parameterInfo
                                .mapNotNull { param ->
                                    val raw = param.typeSignatureOrTypeDescriptor.toString()
                                    deepResolveType(raw)
                                }
                                .sorted()
                            val deobfMethod = resolveMethodName(clazz.name, method.name, resolvedArgs)?.split("(")
                                ?.get(0) ?: method.name
                            !deobfMethod.contains("$") && deobfMethod !in objectClassMethods
                        }
                        .forEach { method ->
                            val resolvedArgs = method.parameterInfo
                                .mapNotNull { param ->
                                    val raw = param.typeSignatureOrTypeDescriptor.toString()
                                    deepResolveType(raw)
                                }
                                .sorted()
                            val deobfMethodName =
                                resolveMethodName(clazz.name, method.name, resolvedArgs)?.split("(")
                                    ?.get(0) ?: method.name

                            val methodEntry = mutableMapOf<String, Any>()

                            val resolvedReturnType = method.typeSignatureOrTypeDescriptor?.resultType?.toString()
                                ?.let { deepResolveType(it) } ?: "Unit"


                            if (resolvedArgs.isNotEmpty()) methodEntry["args"] = resolvedArgs

                            val modifiers = formatModifiers(method.modifiers, ModifierContext.METHOD)

                            methodEntry["modifiers"] = modifiers
                            methodEntry["returns"] = resolvedReturnType

                            val description = extractKDocFromAnnotations(method.annotationInfo)
                            if (!description.isNullOrBlank()) methodEntry["description"] = description

                            if (deobfMethodName == "invoke") {
                                try {
                                    val someClass = Class.forName(clazz.name, false, ClassLoader.getSystemClassLoader())
                                    val hasOperatorInvoke = runCatching {
                                        val kclass = someClass.kotlin
                                        kclass.members.any {
                                            it is KFunction<*> && resolveMethodName(
                                                clazz.name,
                                                it.name,
                                                resolvedArgs
                                            )?.split("(")
                                                ?.get(0) == "invoke" && it.isOperator
                                        }
                                    }.getOrElse {
                                        someClass.methods.any {
                                            resolveMethodName(
                                                clazz.name,
                                                it.name,
                                                resolvedArgs
                                            )?.split("(")
                                                ?.get(0) == "invoke" && it.parameterCount >= 0 && !method.isStatic
                                        }
                                    }
                                    if (hasOperatorInvoke) methodEntry["isInvokeOperator"] = true
                                } catch (_: Exception) {
                                }
                            }

                            val signatureKey = "$deobfMethodName(${resolvedArgs.joinToString(",")})"
                            classEntry[signatureKey] = methodEntry
                        }

                    clazz.fieldInfo
                        .filter { field ->
                            val deobfFieldName = resolveFieldName(clazz.name, field.name) ?: field.name
                            val deobfClassNameForField = resolveClassName(field.className) ?: field.className
                            val thisDeobfClassName = resolveClassName(clazz.name) ?: clazz.name
                            !deobfFieldName.contains("$") && deobfClassNameForField == thisDeobfClassName
                        }
                        .forEach { field ->
                            val deobfFieldName = resolveFieldName(clazz.name, field.name) ?: field.name
                            val resolvedFieldType = deepResolveType(extractGenericType(field))

                            val fieldEntry = mutableMapOf<String, Any>()
                            fieldEntry["modifiers"] = formatModifiers(field.modifiers, ModifierContext.FIELD)
                            fieldEntry["type"] = resolvedFieldType

                            val description = extractKDocFromAnnotations(field.annotationInfo)
                            if (!description.isNullOrBlank()) fieldEntry["description"] = description

                            classEntry[deobfFieldName] = fieldEntry
                        }

                    classMap[deobfClassName] = classEntry
                }
        } catch (exception: Exception) {
            logger.error(exception.message)
        }

        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        jsonOutputPath.writeText(gson.toJson(classMap))
        LogUtils.getLogger().info("Dumped class data to ${jsonOutputPath.absolutePath}")
    }


    /*fun dumpClassesToFile(sourcesDir: File, targetClasses: List<String>, fileName: String) {
        val jsonOutputPath = File(sourcesDir, fileName).apply {
            parentFile.mkdirs()
        }
        val objectClassMethods = setOf("toString", "hashCode", "equals")
        val classMap = mutableMapOf<String, MutableMap<String, Any>>()
        val excludedPackages = listOf(
            "net.minecraft",
            "java.util.jar", "java.util.zip",
            "java.net", "sun", "com.sun", "io.netty",
            "org.objectweb.asm", "org.spongepowered.asm",
            "org.openjdk.nashorn", "jdk.nashorn"
        )
        ClassGraph()
            .enableAllInfo()
            .enableSystemJarsAndModules()
            .scan()
            .allClasses
            .filter { classInfo ->
                val isCompanionObject = classInfo.name.endsWith("\$Companion") &&
                        classInfo.constructorInfo.toString().contains("synthetic")
                return@filter classInfo.name in targetClasses &&
                        !isCompanionObject &&
                        !excludedPackages.any {
                            classInfo.name.startsWith(
                                it
                            )
                        }
            }
            .forEach { clazz ->
                val classEntry = mutableMapOf<String, Any>()
                val classModifiers = formatModifiers(
                    clazz.modifiers,
                    ModifierContext.CLASS
                )
                val superclass = if (clazz.superclass != null) clazz.superclass.name else ""
                classEntry["\$superclass"] = superclass
                val interfaceNames = clazz.interfaces
                    .map { it.name }
                    .toTypedArray()
                classEntry["\$interfaces"] = interfaceNames
                classEntry["\$superclass"] = superclass
                classEntry["\$modifiers"] = classModifiers
                clazz.methodInfo
                    .filter { method ->
                        !method.name.contains("$") &&
                                (
                                        method.name !in objectClassMethods
                                        )
                    }
                    .forEach { method ->
                        val args = method.parameterInfo
                            .map { param -> param.typeSignatureOrTypeDescriptor.toString() }
                            .sorted()

                        val modifiers = formatModifiers(
                            method.modifiers,
                            ModifierContext.METHOD
                        )
                        val methodEntry = mutableMapOf<String, Any>()
                        methodEntry["modifiers"] = modifiers
                        methodEntry["returns"] = method.typeSignatureOrTypeDescriptor?.resultType?.toString() ?: "Unit"
                        val description = extractKDocFromAnnotations(method.annotationInfo)
                        if (!description.isNullOrBlank()) {
                            methodEntry["description"] = description
                        }
                        if (args.isNotEmpty()) {
                            methodEntry["args"] = args
                        }
                        if (method.name == "invoke") {
                            try {
                                val someClass = Class.forName(clazz.name, false, ClassLoader.getSystemClassLoader())

                                val hasOperatorInvoke = runCatching {
                                    val kclass = someClass.kotlin
                                    kclass.members.any { member ->
                                        if (member !is KFunction<*>) return@any false
                                        if (member.name != "invoke" || !member.isOperator) return@any false
                                        true
                                    }
                                }.getOrElse {
                                    someClass.methods.any { m ->
                                        m.name == "invoke"
                                                && m.parameterCount >= 0
                                                && !method.isStatic
                                    }
                                }
                                if (hasOperatorInvoke)
                                    methodEntry["isInvokeOperator"] = true
                            } catch (e: Exception) {
                            }
                        }
                        classEntry["${method.name}(${args.joinToString(",")})"
                        ] =
                            methodEntry
                    }
                clazz.fieldInfo
                    .filter { field ->
                        return@filter !field.name.contains("$") &&
                                (field.className == clazz.name)
                    }
                    .forEach { field ->
                        val modifiers = formatModifiers(
                            field.modifiers,
                            ModifierContext.FIELD
                        )
                        val fieldEntry = mutableMapOf<String, Any>()
                        fieldEntry["modifiers"] = modifiers
                        fieldEntry["type"] = extractGenericType(field)
                        val description = extractKDocFromAnnotations(field.annotationInfo)
                        if (!description.isNullOrBlank()) {
                            fieldEntry["description"] = description
                        }
                        classEntry[field.name] = fieldEntry
                    }
                classMap[clazz.name] = classEntry
            }
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
        jsonOutputPath.writeText(gson.toJson(classMap))
        LogUtils.getLogger().info(" Dumped class data to ${jsonOutputPath.absolutePath}")
    }*/

    fun splitGenericAware(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0

        for (char in input) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }

                '>' -> {
                    depth--
                    current.append(char)
                }

                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString().trim())
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }

                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }

        return result
    }

    fun writeAvailableMembersJson(
        outputFile: File,
        classEntities: List<KotlinObject>
    ) {
        val result = mutableMapOf<String, MutableMap<String, Any>>()

        for (clazz in classEntities) {
            val className = clazz.fullyQualifiedName

            val members = mutableMapOf<String, Any>()
            val classMembers = clazz.members

            for (member in classMembers) {
                val argsString = Regex("""\((.*)\)""").find(member.fullyQualifiedName)?.groupValues?.get(1)
                val args = argsString?.takeIf { it.isNotEmpty() }?.let { splitGenericAware(it) } ?: emptyList()

                val memberName = if (args.isNotEmpty()) {
                    "${member.source}(${args.joinToString(",")})"
                } else {
                    member.source + "()"
                }

                val entry = mutableMapOf<String, Any>(
                    "returns" to member.returnType,
                    "isStatic" to false
                )

                if (member.modifiers.isNotEmpty()) {
                    entry["modifiers"] = member.modifiers
                }

                if (args.isNotEmpty()) entry["args"] = args
                if (member.type == "invoke") entry["isInvokeOperator"] = true

                members[memberName] = entry
            }

            if (members.isNotEmpty()) {
                val classEntry = mutableMapOf<String, Any>(
                    "\$modifiers" to clazz.modifiers.joinToString(" "),
                    "\$typeParameters" to clazz.typeParameters
                )

                classEntry.putAll(members)
                result[className] = classEntry
            }
        }

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(gson.toJson(result))
    }


    private fun isStaticConstant(field: FieldInfo): Boolean {
        val modifiers = field.modifiers
        return Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
    }

    private fun extractGenericType(field: FieldInfo): String {
        val signature = field.typeSignatureOrTypeDescriptor.toString()

        return if (signature.contains('<')) {
            signature.replace(" ", "")
        } else {
            signature
        }
    }

    fun saveSuggestionsToJson(entities: List<KotlinObject>, outputPath: String) {
        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
        val json = gson.toJson(entities)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
    }

    operator fun invoke(): String {
        return ""
    }

    companion object {
        @KDoc(
            """
            Returns a greeting message.
        
            This method demonstrates a simple static function that returns a string based on an input parameter.
        
            ```kt
            val message = FabricBootstrap.someStaticMethod(42)
            println(message) // Hello from Companion Object!
            ```
        
            @param something An integer that could modify behavior (currently unused).
            @return A static greeting message.
            """
        )
        fun someStaticMethod(something: Int): String = "Hello from Companion Object!"

        private fun somePrivateStaticMethod(): String = "Hello from Companion Object!"
        val someValue: Int = 10
        private val somePrivateValue: Int = 10
        val SOMETHING = ""
        fun getOtherValue(): Int = 20
        operator fun invoke() {

        }
    }
}

