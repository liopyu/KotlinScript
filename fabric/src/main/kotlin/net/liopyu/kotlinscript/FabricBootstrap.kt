
package net.liopyu.kotlinscript
import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import io.github.classgraph.ClassGraph
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
import kotlin.system.measureTimeMillis

class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        KotlinScriptInit.preInitialize()
        val instanceDir = File(System.getProperty("user.dir"))
        val modsDir = File(instanceDir,"mods")
        val sourcesDir = File(instanceDir, "kotlinsources")
        if (!sourcesDir.exists() || !sourcesDir.isDirectory) {
            LogUtils.getLogger().warn("Kotlin sources folder not found at ${sourcesDir.absolutePath}")
            return
        }
        val functions = extractTopLevelFunctions(sourcesDir.absolutePath)
        val uniqueFunctions = functions.distinctBy { it.fullyQualifiedName }
       /* uniqueFunctions.forEach { suggestion ->
            LogUtils.getLogger().info("Testing method: ${suggestion.fullyQualifiedName}, source: ${suggestion.source}, path: ${suggestion.path}")
        }*/
        val validAndRelevantSuggestions = uniqueFunctions/*KotlinScriptInit.testKotlinSuggestions(uniqueFunctions)*/
        val occurrenceMap = mutableMapOf<String, Int>()
        val enrichedSuggestions = validAndRelevantSuggestions.map { obj ->
            val currentCount = occurrenceMap.getOrDefault(obj.simpleName, 0)
            occurrenceMap[obj.simpleName] = currentCount + 1
            val newSimpleName = if (currentCount > 0) obj.fullyQualifiedName else obj.simpleName
            obj.copy(simpleName = newSimpleName)
        }
        saveSuggestionsToJson(enrichedSuggestions, "kotlin_suggestions.json")
        dumpClassesToFile(sourcesDir)
       // dumpClassesWithFernFlower(sourcesDir)
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

    suspend fun processBatchWithFernFlower(classBatch: List<io.github.classgraph.ClassInfo>, sourcesOutputPath: File, batchName: String) {
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
            .acceptPackages("java", "javax", "kotlin", "com", "org", "net", "io") // Exclude macOS-specific and native bindings
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


    fun dumpClassesToFile(sourcesDir: File) {
        val binOutputPath = File(sourcesDir, "available_classes.bin").apply {
            parentFile.mkdirs()
        }
        val classList = ClassGraph()
            .enableClassInfo()
            .enableSystemJarsAndModules()
            .scan()
            .allClasses
            .filter { it.isPublic }
            .names
            .filter { !it.matches(Regex(".*\\$\\d+")) }
        binOutputPath.writeText(classList.joinToString("\n"))
        LogUtils.getLogger().info("Dumped ${classList.size} classes to ${binOutputPath.absolutePath}")
    }
    fun extractTopLevelFunctions(sourcePath: String): List<KotlinObject> {
        val disposable: Disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, org.jetbrains.kotlin.cli.common.messages.MessageCollector.NONE)
            put(CommonConfigurationKeys.MODULE_NAME, "extraction")
            addKotlinSourceRoot(sourcePath)
        }
        val environment = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val ktFiles: List<KtFile> = environment.getSourceFiles()
        val entities = mutableListOf<KotlinObject>()

        val kotlinCorePackages = setOf(
            "kotlin", "kotlin.io", "kotlin.text", "kotlin.collections", "kotlin.ranges", "kotlin.sequences", "kotlin.comparisons"
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
                        val fqName = if (pkg == "kotlin" && receiverType != null && receiverType.matches(Regex("^[A-Z]$"))) {
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



    fun saveSuggestionsToJson(entities: List<KotlinObject>, outputPath: String) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(entities)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}