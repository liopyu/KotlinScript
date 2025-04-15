package net.liopyu.kotlinscript

import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import io.github.classgraph.AnnotationInfoList
import io.github.classgraph.ClassGraph
import io.github.classgraph.FieldInfo
import kotlinx.coroutines.*
import net.fabricmc.api.ModInitializer
import net.liopyu.kotlinscript.util.KotlinObject
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
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

@Retention(AnnotationRetention.RUNTIME)
annotation class KDoc(val value: String)

class FabricBootstrap : ModInitializer {
    operator fun invoke() {

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
        KotlinScriptInit.preInitialize()
        val instanceDir = File(System.getProperty("user.dir"))
        val sourcesDir = File(instanceDir, "kotlinsources")
        val modsDir = File(instanceDir, "mods")
        if (!sourcesDir.exists() || !sourcesDir.isDirectory) {
            LogUtils.getLogger().warn("Kotlin sources folder not found at ${sourcesDir.absolutePath}")
            return
        }
        val functions = extractTopLevelFunctions(sourcesDir.absolutePath)
        val uniqueFunctions = functions.distinctBy { it.fullyQualifiedName }
        val validAndRelevantSuggestions = uniqueFunctions
        val occurrenceMap = mutableMapOf<String, Int>()
        val enrichedSuggestions = validAndRelevantSuggestions.map { obj ->
            val currentCount = occurrenceMap.getOrDefault(obj.simpleName, 0)
            occurrenceMap[obj.simpleName] = currentCount + 1
            val newSimpleName = if (currentCount > 0) obj.fullyQualifiedName else obj.simpleName
            obj.copy(simpleName = newSimpleName)
        }
        //saveSuggestionsToJson(enrichedSuggestions, "kotlin_suggestions.json")
        val l = listOf(
            /* "net.minecraft.world.entity.Entity",
             "net.minecraft.world.entity.LivingEntity",*/
            "net.liopyu.kotlinscript.FabricBootstrap",
            "net.liopyu.kotlinscript.util.ClassScanner"
        )
        val list = dumpClassesToFile(sourcesDir)
        dumpClassesToFile(sourcesDir, list)
        dumpCompanionObjectsToFile(sourcesDir, list)
        //inspectClassDetails("org.jetbrains.kotlin.codegen.CommonVariableAsmNameManglingUtils")
        //dumpClassesWithFernFlower(sourcesDir)
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


    fun dumpCompanionObjectsToFile(sourcesDir: File, filteredClasses: List<String>) {
        // forceExportPackage()
        val logger = LogUtils.getLogger()
        val jsonOutputPath = File(sourcesDir, "companion_objects.json").apply {
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
                classInfo.name in filteredClasses &&
                        classInfo.name.endsWith("\$Companion") &&
                        classInfo.constructorInfo.toString().contains("synthetic")
            }
            .forEach { clazz ->
                val logger = LogUtils.getLogger()
                val baseOuterName = clazz.name
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
                        .filter { it.visibility == KVisibility.PUBLIC && it.name !in objectClassMethods }
                        .forEach { method ->
                            val methodName = method.name
                            val returnType = method.returnType.toString()
                            val methodEntry = mutableMapOf<String, Any>()
                            val isOperator = method.name == "invoke" &&
                                    method.parameters.count { it.kind == KParameter.Kind.VALUE } == 0 &&
                                    method.isOperator
                            if (isOperator) {
                                methodEntry["isInvokeOperator"] = true
                            }
                            methodEntry["returns"] = returnType
                            val filteredParams = if (
                                method.parameters.isNotEmpty() &&
                                method.parameters.first().type.toString() == someClass.kotlin.qualifiedName
                            ) {
                                method.parameters.drop(1)
                            } else {
                                method.parameters
                            }

                            if (filteredParams.isNotEmpty()) {
                                methodEntry["args"] = filteredParams.map { it.type.toString() }
                            }
                            val description = extractKDocFromAnnotations(method.annotations)

                            if (!description.isNullOrBlank()) {
                                methodEntry["description"] = description
                            }


                            companionEntry[("$methodName()")] = methodEntry
                        }

                    // Safely inspect properties
                    someClass.kotlin.memberProperties
                        .filter { it.visibility == KVisibility.PUBLIC }
                        .forEach { field ->
                            val fieldEntry = mutableMapOf<String, Any>()
                            fieldEntry["type"] = field.returnType.toString()
                            companionEntry[field.name] = fieldEntry
                            val description = extractKDocFromAnnotations(field.annotations)

                            if (!description.isNullOrBlank()) {
                                fieldEntry["description"] = description
                            }

                        }

                    if (companionEntry.isNotEmpty()) {
                        companionMap[clazz.name.removeSuffix("\$Companion")] = companionEntry
                    }
                } catch (e: Throwable) {
                    logger.warn("⚠️ Skipped class due to error (${clazz.name}): ${e::class.simpleName}: ${e.message}")
                }
            }

        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

        val jsonOutput = gson.toJson(companionMap)
        jsonOutputPath.writeText(jsonOutput)
        logger.info("✅ Dumped companion object data to ${jsonOutputPath.absolutePath}")
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


    fun dumpClassesToFile(sourcesDir: File): List<String> {
        val binOutputPath = File(sourcesDir, "available_classes.bin").apply {
            parentFile.mkdirs()
        }
        val excludedPackages = listOf(
            "java.lang", "java.io", "java.nio", "java.util.jar", "java.util.zip",
            "java.net", "sun", "com.sun", "io.netty",
            "org.objectweb.asm", "org.spongepowered.asm",
            "org.openjdk.nashorn", "jdk.nashorn"
        )
        val classList = CopyOnWriteArrayList<String>()
        val futures = ClassGraph()
            .enableClassInfo()
            .enableSystemJarsAndModules()
            .enableAnnotationInfo()
            .scan()
            .allClasses
            .filter { classInfo ->
                classInfo.isPublic && !classInfo.name.matches(Regex(".*\\$\\d+")) && !excludedPackages.any {
                    classInfo.name.startsWith(
                        it
                    )
                }
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
        LogUtils.getLogger().info("✅ Dumped ${classList.size} importable classes to ${binOutputPath.absolutePath}")
        return classList
    }

    fun extractTopLevelFunctions(sourcePath: String): List<KotlinObject> {
        val disposable: Disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration().apply {
            put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                org.jetbrains.kotlin.cli.common.messages.MessageCollector.NONE
            )
            put(CommonConfigurationKeys.MODULE_NAME, "extraction")
            addKotlinSourceRoot(sourcePath)
        }
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val ktFiles: List<KtFile> = environment.getSourceFiles()
        val entities = mutableListOf<KotlinObject>()

        val kotlinCorePackages = setOf(
            "kotlin",
            "kotlin.io",
            "kotlin.text",
            "kotlin.collections",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.comparisons"
        )

        for (file in ktFiles) {
            val pkg = file.packageFqName.asString()
            val sourceName = file.name.substringBeforeLast(".")

            file.declarations.filterIsInstance<KtClassOrObject>().forEach { classDeclaration ->
                val className = classDeclaration.name ?: return@forEach
                val fqName = if (pkg.isNotEmpty()) "$pkg.$className" else className

                entities.add(
                    KotlinObject(
                        fullyQualifiedName = fqName,
                        simpleName = className,
                        source = sourceName,
                        type = "class",
                        path = pkg,
                        parentType = null,
                        requiresImport = pkg.isNotEmpty() && pkg !in kotlinCorePackages,
                        isClass = true
                    )
                )
            }


            file.declarations.filterIsInstance<KtNamedFunction>()
                .filter { !it.hasModifier(KtTokens.PRIVATE_KEYWORD) }
                .forEach { function ->
                    function.name?.let { name ->
                        val receiverType = function.receiverTypeReference?.text
                        val fqName =
                            if (pkg == "kotlin" && receiverType != null && receiverType.matches(Regex("^[A-Z]$"))) {
                                name
                            } else {
                                if (pkg.isNotEmpty()) "$pkg.$name" else name
                            }
                        val type = when {
                            function.hasModifier(KtTokens.INFIX_KEYWORD) &&
                                    pkg == "kotlin" && fqName == name &&
                                    receiverType != null && receiverType.matches(Regex("^[A-Z]$")) -> "infix_lambda"

                            function.hasModifier(KtTokens.INFIX_KEYWORD) -> "infix"
                            pkg == "kotlin" && fqName == name &&
                                    receiverType != null && receiverType.matches(Regex("^[A-Z]$")) -> "lambda"

                            function.valueParameters.isNotEmpty() -> "method"
                            else -> "property"
                        }

                        val requiresImport = pkg.isNotEmpty() && pkg !in kotlinCorePackages

                        entities.add(
                            KotlinObject(
                                fullyQualifiedName = fqName,
                                simpleName = name,
                                source = sourceName,
                                type = type,
                                path = pkg,
                                parentType = receiverType,
                                requiresImport = requiresImport,
                                isClass = false
                            )
                        )
                    }
                }
        }

        Disposer.dispose(disposable)
        return entities
    }

    fun dumpClassesToFile(sourcesDir: File, targetClasses: List<String>) {
        val jsonOutputPath = File(sourcesDir, "available_members.json").apply {
            parentFile.mkdirs()
        }

        val objectClassMethods = setOf("toString", "hashCode", "equals")
        val classMap = mutableMapOf<String, MutableMap<String, Any>>()

        ClassGraph()
            .enableClassInfo()
            .enableMethodInfo()
            .enableFieldInfo()
            .enableAnnotationInfo()
            .enableSystemJarsAndModules()
            .scan()
            .allClasses
            .filter { classInfo ->
                val isCompanionObject = classInfo.name.endsWith("\$Companion") &&
                        classInfo.constructorInfo.toString().contains("synthetic")
                return@filter classInfo.name in targetClasses && !isCompanionObject
            }
            .forEach { clazz ->
                val classEntry = mutableMapOf<String, Any>()
                val isJavaLangObject = clazz.name == "java.lang.Object"
                val isKotlinAny = clazz.name == "kotlin.Any"

                val declaredMethodNames = clazz.methodInfo.map { it.name }.toSet()
                clazz.methodInfo
                    .filter { method ->
                        method.isPublic &&
                                !method.name.contains("$") &&
                                method.name in declaredMethodNames &&
                                (
                                        method.name !in objectClassMethods ||
                                                isJavaLangObject ||
                                                isKotlinAny
                                        )
                    }
                    .forEach { method ->
                        val args = method.parameterInfo
                            .map { param -> param.typeSignatureOrTypeDescriptor.toString() }
                            .sorted()

                        val methodEntry = mutableMapOf<String, Any>()

                        methodEntry["returns"] = method.typeSignatureOrTypeDescriptor?.resultType?.toString() ?: "Unit"
                        methodEntry["isStatic"] = method.isStatic
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
                                        val filteredParams = member.parameters.dropWhile {
                                            it.kind == KParameter.Kind.INSTANCE || it.type.classifier == kclass
                                        }


                                        /* LogUtils.getLogger()
                                             .info("Function: ${member.name}, isOperator: ${member.isOperator}, filteredParamCount: ${filteredParams.size}")
                                         */true
                                    }
                                }.getOrElse {
                                    // Fallback: try to detect `invoke` manually from declared methods
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
                        classEntry["${method.name}()"] = methodEntry
                    }

                // Extract fields
                val declaredFieldNames = clazz.fieldInfo.map { it.name }.toSet()

                clazz.fieldInfo
                    .filter { field ->
                        return@filter field.isPublic &&
                                !field.name.contains("$") &&
                                field.name in declaredFieldNames &&
                                (field.className == clazz.name)
                    }
                    .forEach { field ->
                        val fieldEntry = mutableMapOf<String, Any>()
                        fieldEntry["type"] = extractGenericType(field)
                        fieldEntry["isStatic"] = field.isStatic
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

        LogUtils.getLogger().info("✅ Dumped class data to ${jsonOutputPath.absolutePath}")
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
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(entities)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

