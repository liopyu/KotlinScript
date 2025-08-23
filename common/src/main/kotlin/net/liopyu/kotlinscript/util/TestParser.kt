package net.liopyu.kotlinscript.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mojang.logging.LogUtils
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.io.InputStreamReader

val instanceDir = File(System.getProperty("user.dir"))
val sourcesDir = File(instanceDir, "kotlinsources")
fun some() {
    val tree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings()
    val namespaces = listOf(tree.srcNamespace) + tree.dstNamespaces
    println("Namespace count: ${namespaces.size}")
    for ((i, ns) in namespaces.withIndex()) {
        println("Namespace $i: $ns")
    }

    for (classDef in tree.classes) {
        val names = mutableListOf<String?>()
        for (i in namespaces.indices) {
            val name = try {
                classDef.getName(i)
            } catch (e: Exception) {
                null
            }
            names.add(name)
        }
        println("Class names: ${names.withIndex().joinToString { "${it.index}=${it.value}" }}")

        if (names.size > 1 && names[1] != null && names[1]!!.contains("Minecraft")) {
            val intermediaryClass = names[0]?.replace('/', '.')
            val namedClass = names[1]?.replace('/', '.')
            println("=== Class: $namedClass (intermediary: $intermediaryClass) ===")
            for (methodDef in classDef.methods) {
                val intermediaryMethod = try {
                    methodDef.getName(0)
                } catch (e: Exception) {
                    null
                }
                val namedMethod = try {
                    methodDef.getName(1)
                } catch (e: Exception) {
                    null
                }
                println("  method: intermediary=$intermediaryMethod, named=$namedMethod")
                if (namedMethod == "getInstance") {
                    println("getInstance method found: named=$namedMethod, intermediary=$intermediaryMethod")
                }
            }
        }
    }
}

fun saveMappingsToJson() {
    val gson = Gson()
    val data = mapOf(
        "deobfToObfClassMap" to deobfToObfClassMap,
        "deobfToObfMethodMap" to deobfToObfMethodMap,
        "deobfToObfFieldMap" to deobfToObfFieldMap,
        "obfToDeobfClassMap" to obfToDeobfClassMap,
        "obfToDeobfMethodMap" to obfToDeobfMethodMap,
        "obfToDeobfFieldMap" to obfToDeobfFieldMap,
    )
    val file = getDevMappingFile()
    file.writeText(gson.toJson(data))
}

fun loadMappingsFromResource() {
    val resourcePath = "/mappings/mappings.json"
    val resourceStream = TestParser.Companion::class.java.getResourceAsStream(resourcePath)
    val url = TestParser.Companion::class.java.getResource(resourcePath)
    logger.info("Trying to load resource as URL: $url")
    val gson = Gson()
    val mapType = object : TypeToken<Map<String, Any>>() {}.type

    val json = InputStreamReader(resourceStream).readText()
    val data: Map<String, Any> = gson.fromJson(json, mapType) ?: emptyMap()

    for (key in listOf(
        "deobfToObfClassMap", "deobfToObfMethodMap", "deobfToObfFieldMap",
        "obfToDeobfClassMap", "obfToDeobfMethodMap", "obfToDeobfFieldMap"
    )) {
        if (!data.containsKey(key)) logger.warn("$key missing from mappings.json!")
    }

    // Helper
    fun <T> parseMap(key: String, token: java.lang.reflect.Type): T {
        val jsonObj = gson.toJson(data[key] ?: emptyMap<String, Any>())
        return gson.fromJson(jsonObj, token) ?: throw IllegalStateException("Failed to parse $key from mappings.json")
    }

    // Parse all the maps
    val classMapType = object : TypeToken<Map<String, String>>() {}.type
    val methodMapType = object : TypeToken<Map<String, Map<String, String>>>() {}.type

    deobfToObfClassMap.clear()
    deobfToObfClassMap.putAll(parseMap("deobfToObfClassMap", classMapType))

    deobfToObfMethodMap.clear()
    parseMap<Map<String, Map<String, String>>>("deobfToObfMethodMap", methodMapType)
        .forEach { (k, v) -> deobfToObfMethodMap[k] = v.toMutableMap() }

    deobfToObfFieldMap.clear()
    parseMap<Map<String, Map<String, String>>>("deobfToObfFieldMap", methodMapType)
        .forEach { (k, v) -> deobfToObfFieldMap[k] = v.toMutableMap() }

    obfToDeobfClassMap.clear()
    obfToDeobfClassMap.putAll(parseMap("obfToDeobfClassMap", classMapType))

    obfToDeobfMethodMap.clear()
    parseMap<Map<String, Map<String, String>>>("obfToDeobfMethodMap", methodMapType)
        .forEach { (k, v) -> obfToDeobfMethodMap[k] = v.toMutableMap() }

    obfToDeobfFieldMap.clear()
    parseMap<Map<String, Map<String, String>>>("obfToDeobfFieldMap", methodMapType)
        .forEach { (k, v) -> obfToDeobfFieldMap[k] = v.toMutableMap() }
    rebuildMethodArityIndex()
    rebuildOverloadsFromFlatMap()
}

fun rebuildOverloadsFromFlatMap() {
    deobfMethodOverloads.clear()
    deobfToObfMethodMap.forEach { (cls, map) ->
        val into = deobfMethodOverloads.getOrPut(cls) { mutableMapOf() }
        map.forEach { (k, v) ->
            val p = k.indexOf('(')
            if (p > 0) {
                val name = k.substring(0, p)
                val desc = k.substring(p)
                into.getOrPut(name) { mutableListOf() }.add(desc to v)
            }
        }
    }
}


fun getDevMappingFile(): File {
    val resource = TestParser.Companion::class.java.getResource("/mappings/")
        ?: error("Could not find /mappings/ in resources! Make sure src/main/resources/mappings exists.")
    val mappingsDir = File(resource.toURI())
    if (!mappingsDir.exists()) mappingsDir.mkdirs()
    val mappingFile = File(mappingsDir, "mappings.json")
    logger.info("getDevMappingFile(): Using dev mappings file: ${mappingFile.absolutePath}")
    return mappingFile
}


fun buildMappingsIfNeeded(tree: MappingTree, obfNamespace: Int, deobfNamespace: Int) {
    if (!isDevEnvironment()) {
        loadMappingsFromResource()
    } else {
        val mappingFile = getDevMappingFile()
        if (!mappingFile.exists()) {
            buildDeobfToObfMapFromTree(tree, obfNamespace, deobfNamespace)
            saveMappingsToJson()
            logger.info("Generated mapping file at: ${mappingFile.absolutePath}")
        } else {
            loadMappingsFromResource()
            logger.info("Loaded existing mapping file at: ${mappingFile.absolutePath}")
        }
    }
}

fun isDevEnvironment(): Boolean {
    if (!FabricLoader.getInstance().isDevelopmentEnvironment)
        LogUtils.getLogger().info("Im inside a production environment!")
    else
        LogUtils.getLogger().info("Im inside a dev environment!")
    return FabricLoader.getInstance().isDevelopmentEnvironment
}

val logger = LogUtils.getLogger()

val deobfToObfClassMap: MutableMap<String, String> = mutableMapOf()
val deobfToObfMethodMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
val deobfToObfFieldMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

val obfToDeobfClassMap: MutableMap<String, String> = mutableMapOf()
val obfToDeobfMethodMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
val obfToDeobfFieldMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
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
        for (fieldDef in classDef.fields) {
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
            if (obfField != null && deobfField != null) {
                fieldMap[deobfField] = obfField
                obfFieldMap[obfField] = deobfField
            }
        }

        classCount++
    }

    logger.info("buildDeobfToObfMapFromTree: Finished! Total mapped classes: $classCount, skipped classes: $skippedClassCount")
    logger.info("  deobfToObfClassMap size: ${deobfToObfClassMap.size}")
    logger.info("  deobfToObfMethodMap size: ${deobfToObfMethodMap.size}")
    logger.info("  deobfToObfFieldMap size: ${deobfToObfFieldMap.size}")
}


fun resolveClassName(obfClass: String): String? {
    return obfToDeobfClassMap[obfClass]
}

fun resolveFieldName(obfClass: String, obfField: String): String? {
    return obfToDeobfFieldMap[obfClass]?.get(obfField)
}

fun resolveMethodName(className: String, methodName: String, args: List<String>): String? {
    val key = "$methodName(${args.joinToString(",")})"
    return obfToDeobfMethodMap[className]?.get(key)
}


fun resolveObfMethodName(className: String, methodName: String, args: List<String>): String? {
    val key = "$methodName(${args.joinToString(",")})"
    return deobfToObfMethodMap[className]?.get(key)
}

val deobfToObfMethodByArity: MutableMap<String, MutableMap<String, MutableMap<Int, String>>> = mutableMapOf()

private fun countParams(desc: String): Int {
    var i = desc.indexOf('(') + 1
    var c = 0
    while (i < desc.length && desc[i] != ')') {
        when (desc[i]) {
            'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> {
                c++; i++
            }

            'L' -> {
                c++; while (i < desc.length && desc[i] != ';') i++; i++
            }

            '[' -> {
                while (i < desc.length && desc[i] == '[') i++
                if (i < desc.length && desc[i] == 'L') {
                    while (i < desc.length && desc[i] != ';') i++; i++
                } else i++
                c++
            }

            else -> i++
        }
    }
    return c
}

fun rebuildMethodArityIndex() {
    deobfToObfMethodByArity.clear()
    deobfToObfMethodMap.forEach { (cls, map) ->
        val into = deobfToObfMethodByArity.getOrPut(cls) { mutableMapOf() }
        map.forEach { (key, obfName) ->
            val p = key.indexOf('(')
            if (p > 0) {
                val m = key.substring(0, p)
                val desc = key.substring(p)
                val ar = countParams(desc)
                val byArity = into.getOrPut(m) { mutableMapOf() }
                val prev = byArity.putIfAbsent(ar, obfName)
                if (prev != null && prev != obfName) byArity[ar] = ""
            }
        }
    }
}

private fun toSourceFqcn(name: String) = name.replace('$', '.')
data class MethodUse(val className: String, val methodName: String, val arity: Int, val hints: List<String>)

val deobfMethodOverloads: MutableMap<String, MutableMap<String, MutableList<Pair<String, String>>>> = mutableMapOf()
private fun paramTypes(desc: String): List<String> {
    val out = mutableListOf<String>()
    var i = desc.indexOf('(') + 1
    while (i < desc.length && desc[i] != ')') {
        when (desc[i]) {
            'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> {
                out += desc[i].toString(); i++
            }

            'L' -> {
                val s = i + 1; while (i < desc.length && desc[i] != ';') i++; out += desc.substring(s, i); i++
            }

            '[' -> {
                var a = 1; i++
                while (i < desc.length && desc[i] == '[') {
                    a++; i++
                }
                if (i < desc.length && desc[i] == 'L') {
                    val s = i + 1; while (i < desc.length && desc[i] != ';') i++; out += desc.substring(s, i); i++
                } else {
                    out += "[]"; i++
                }
            }

            else -> i++
        }
    }
    return out
}

private fun classifyArg(raw: String): String {
    val r = raw.trim()
    return when {
        r.startsWith("\"") -> "java/lang/String"
        "BuiltInRegistries" in r -> "net/minecraft/core/Registry"
        r.startsWith("ResourceLocation") || ".fromNamespaceAndPath(" in r -> "net/minecraft/resources/ResourceLocation"
        else -> ""
    }
}

private fun chooseOverloadByHints(cls: String, name: String, arity: Int, hints: List<String>): String? {
    val list = deobfMethodOverloads[cls]?.get(name) ?: return null
    var best: Pair<Int, String>? = null
    for ((desc, obf) in list) {
        val ps = paramTypes(desc)
        if (ps.size != arity) continue
        var score = 0
        for (i in hints.indices) {
            val h = hints[i]
            if (h.isEmpty()) continue
            if (i < ps.size && ps[i].contains(h)) score += 3
        }
        if (best == null || score > best!!.first) best = score to obf
    }
    return best?.second
}

class TestParser {
    companion object {

        fun logStaticStatusOfAllFunctions(
            ktFile: KtFile,
            logger: org.slf4j.Logger
        ) {
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    super.visitNamedFunction(function)

                    val isTopLevel = function.isTopLevel
                    val hasJvmStatic = function.annotationEntries.any { it.shortName?.asString() == "JvmStatic" }
                    val isInCompanionOrObject = generateSequence(function.parent) { it.parent }
                        .any { it is KtObjectDeclaration }
                    val isStatic = isTopLevel || (isInCompanionOrObject && hasJvmStatic)

                    val name = function.name ?: "<anonymous>"
                    logger.info("Function $name: static=$isStatic")
                }

            })
        }

        var reobfEnabled = true

        fun main(string: String): String {
            setIdeaIoUseFallback()
            val disposable = Disposer.newDisposable()
            val logger = LogUtils.getLogger()

            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val psiFileFactory = PsiFileFactory.getInstance(environment.project)
            val ktFile = psiFileFactory.createFileFromText(
                "test.kts",
                KotlinLanguage.INSTANCE,
                string
            ) as KtFile

            val importLineMap = buildObfuscatedImports(ktFile, logger)
            val foundClasses = findClassUsages(ktFile, logger)
            val foundMethods = findMethodUsages(ktFile, logger)

            val foundFields = findFieldUsages(ktFile, logger)
            val nested = findNestedQualifiedClassUsages(ktFile, logger)
            val result = replaceObfuscated(string, foundClasses, foundMethods, foundFields, importLineMap, nested)

            logger.info(
                "getInstance map: " + deobfToObfMethodByArity["net.minecraft.client.Minecraft"]?.get("getInstance")
                    ?.get(0)
            )

            Disposer.dispose(disposable)
            return result
        }

        fun findNestedQualifiedClassUsages(ktFile: KtFile, logger: org.slf4j.Logger): Map<String, String> {
            val importSimple = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val ip = it.importPath?.pathStr ?: return@forEach
                importSimple[ip.substringAfterLast('.')] = ip
            }
            val repl = mutableMapOf<String, String>()
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitUserType(type: KtUserType) {
                    super.visitUserType(type)
                    val inner = type.referencedName ?: return
                    val q = type.qualifier as? KtUserType ?: return
                    val outer = q.referencedName ?: return
                    val outerFq = importSimple[outer] ?: outer
                    val deobfNested = "$outerFq$$inner"
                    val obf = deobfToObfClassMap[deobfNested] ?: return
                    repl["$outer.$inner"] = obf.replace('$', '.')
                }

                override fun visitCallExpression(expr: KtCallExpression) {
                    super.visitCallExpression(expr)
                    val calleeName = (expr.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return
                    val dot = expr.parent as? KtDotQualifiedExpression ?: return
                    val outer = dot.receiverExpression.text
                    val outerFq = importSimple[outer] ?: outer
                    val deobfNested = "$outerFq$$calleeName"
                    val obf = deobfToObfClassMap[deobfNested] ?: return
                    repl["$outer.$calleeName"] = obf.replace('$', '.')
                }
            })
            return repl
        }

        fun buildObfuscatedImports(ktFile: KtFile, logger: org.slf4j.Logger): Map<Int, String> {
            val importDirectives = ktFile.importDirectives
            val importLineMap = mutableMapOf<Int, String>()
            importDirectives.forEach {
                val path = it.importPath?.pathStr ?: return@forEach
                val obf = deobfToObfClassMap[path]
                if (obf != null) {
                    importLineMap[it.textRange.startOffset] = "import ${toSourceFqcn(obf)}"
                }
            }
            return importLineMap
        }

        fun findClassUsages(ktFile: KtFile, logger: org.slf4j.Logger): Set<String> {
            val found = mutableSetOf<String>()
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitUserType(type: KtUserType) {
                    super.visitUserType(type)
                    type.referencedName?.let { name ->
                        if (deobfToObfClassMap.containsKey(name) ||
                            deobfToObfClassMap.containsKey(name.substringAfterLast('$'))
                        ) found += name
                    }
                }

                override fun visitCallExpression(expr: KtCallExpression) {
                    super.visitCallExpression(expr)
                    val callee = expr.calleeExpression
                    if (callee is KtSimpleNameExpression) {
                        val name = callee.getReferencedName()
                        if (deobfToObfClassMap.containsKey(name)) found += name
                    }
                }

                override fun visitSimpleNameExpression(expr: KtSimpleNameExpression) {
                    super.visitSimpleNameExpression(expr)
                    val name = expr.getReferencedName()
                    if (deobfToObfClassMap.containsKey(name) ||
                        deobfToObfClassMap.containsKey(name.substringAfterLast('$'))
                    ) found += name
                }
            })
            return found
        }

        fun findMethodUsages(ktFile: KtFile, logger: org.slf4j.Logger): Set<MethodUse> {
            val found = mutableSetOf<MethodUse>()
            val importSimple = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val ip = it.importPath?.pathStr ?: return@forEach
                importSimple[ip.substringAfterLast('.')] = ip
            }
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expr: KtCallExpression) {
                    super.visitCallExpression(expr)
                    val callee = expr.calleeExpression as? KtSimpleNameExpression ?: return
                    val name = callee.getReferencedName()
                    val arity = expr.valueArguments.size
                    val dot = expr.parent as? KtDotQualifiedExpression ?: return
                    val qual = dot.receiverExpression.text
                    val candidates = mutableListOf<String>()
                    importSimple[qual]?.let { candidates += it }
                    obfToDeobfClassMap[qual]?.let { candidates += it }
                    deobfToObfClassMap[qual]?.let { obf -> obfToDeobfClassMap[obf]?.let { candidates += it } }
                    candidates += qual
                    val hints = expr.valueArguments.map { a -> classifyArg(a.getArgumentExpression()?.text ?: "") }
                    for (cn in candidates) {
                        val named = obfToDeobfClassMap[cn] ?: cn
                        val ar = deobfToObfMethodByArity[named]?.get(name)?.get(arity)
                        if (!ar.isNullOrEmpty()) {
                            found += MethodUse(named, name, arity, hints); break
                        } else if ((deobfMethodOverloads[named]?.get(name)?.count { paramTypes(it.first).size == arity }
                                ?: 0) > 1) {
                            found += MethodUse(named, name, arity, hints); break
                        }
                    }
                }
            })
            return found
        }


        fun findFieldUsages(
            ktFile: KtFile,
            logger: org.slf4j.Logger
        ): Set<Pair<String, String>> {
            val foundFields = mutableSetOf<Pair<String, String>>()
            val importSimpleNameMap = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val importPath = it.importPath?.pathStr
                if (importPath != null) {
                    importSimpleNameMap[importPath.substringAfterLast('.')] = importPath
                }
            }
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitDotQualifiedExpression(expr: org.jetbrains.kotlin.psi.KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expr)
                    val selector = expr.selectorExpression as? org.jetbrains.kotlin.psi.KtSimpleNameExpression ?: return
                    val fieldName = selector.getReferencedName()
                    val qualifierText = expr.receiverExpression.text

                    val possibleClassNames = buildList {
                        importSimpleNameMap[qualifierText]?.let { add(it) }
                        deobfToObfClassMap[qualifierText]?.let { add(it) }
                        if (qualifierText.startsWith("net.minecraft.class_")) add(qualifierText)
                        add(qualifierText)
                    }

                    for (className in possibleClassNames) {
                        val fieldMap = deobfToObfFieldMap[className]
                        if (fieldMap?.containsKey(fieldName) == true) {
                            foundFields += className to fieldName
                            break
                        }
                    }
                }
            })
            return foundFields
        }

        fun replaceObfuscated(
            source: String,
            foundClasses: Set<String>,
            foundMethods: Set<MethodUse>,
            foundFields: Set<Pair<String, String>>,
            importLineMap: Map<Int, String>,
            nestedQualifiedClassReplacements: Map<String, String>
        ): String {
            val lines = source.lines().toMutableList()
            val newLines = mutableListOf<String>()
            val obfuscatedImportSimpleNames = mutableSetOf<String>()
            val importPattern = Regex("""^import\s+(.+)$""")

            for (i in lines.indices) {
                val line = lines[i]
                val match = importPattern.matchEntire(line.trim())
                if (match != null) {
                    val importPath = match.groupValues[1].trim()
                    val obf = deobfToObfClassMap[importPath]
                    if (obf != null) {
                        val src = obf.replace('$', '.')
                        obfuscatedImportSimpleNames += src.substringAfterLast('.')
                        newLines += "import $src"
                    } else {
                        newLines += "import $importPath"
                    }
                } else newLines += line
            }

            val processed = newLines.joinToString("\n") { line ->
                if (line.trimStart().startsWith("import ")) line
                else {
                    var replacedLine = line
                    nestedQualifiedClassReplacements.forEach { (from, to) ->
                        replacedLine = replacedLine.replace(Regex("""\b${Regex.escape(from)}\b"""), to)
                    }
                    foundClasses.forEach { deobf ->
                        val key = if (deobfToObfClassMap.containsKey(deobf)) deobf else deobf.substringAfterLast('$')
                        val obf = deobfToObfClassMap[key]
                        if (obf != null) {
                            val src = obf.replace('$', '.')
                            replacedLine = replacedLine.replace(Regex("""\b${Regex.escape(deobf)}\b"""), src)
                        }
                    }
                    foundFields.forEach { (className, deobfField) ->
                        val obfClass0 = deobfToObfClassMap[className] ?: className
                        val obfClass = obfClass0.replace('$', '.')
                        val obf = deobfToObfFieldMap[className]?.get(deobfField)
                        if (obf != null) {
                            replacedLine = replacedLine.replace(
                                Regex(
                                    """(${Regex.escape(obfClass)}|${Regex.escape(className.substringAfterLast('.'))})\s*\.\s*${
                                        Regex.escape(
                                            deobfField
                                        )
                                    }\b"""
                                ),
                                "$1.$obf"
                            )
                            if (obfClass.substringAfterLast('.') !in obfuscatedImportSimpleNames) {
                                replacedLine = replacedLine.replace(Regex("""\b${Regex.escape(deobfField)}\b"""), obf)
                            }
                        }
                    }
                    foundMethods.forEach { (className, deobfMethod, arity, hints) ->
                        val obfClass0 = deobfToObfClassMap[className] ?: className
                        val obfClass = obfClass0.replace('$', '.')
                        var obfMethod = deobfToObfMethodByArity[className]?.get(deobfMethod)?.get(arity)
                        if (obfMethod.isNullOrEmpty()) obfMethod =
                            chooseOverloadByHints(className, deobfMethod, arity, hints)
                        if (!obfMethod.isNullOrEmpty()) {
                            replacedLine = replacedLine.replace(
                                Regex(
                                    """(${Regex.escape(obfClass)}|${Regex.escape(className.substringAfterLast('.'))})\s*\.\s*${
                                        Regex.escape(
                                            deobfMethod
                                        )
                                    }\s*\("""
                                ),
                                "$1.$obfMethod("
                            )
                            if (obfClass.substringAfterLast('.') !in obfuscatedImportSimpleNames) {
                                replacedLine =
                                    replacedLine.replace(Regex("""\b${Regex.escape(deobfMethod)}\b"""), obfMethod)
                            }
                        }
                    }


                    replacedLine
                }
            }
            return processed.replace("net.field_5645.", "net.minecraft.")
        }


    }
}
