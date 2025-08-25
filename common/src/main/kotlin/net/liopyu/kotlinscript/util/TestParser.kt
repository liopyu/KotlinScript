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
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiRecursiveElementWalkingVisitor
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


private fun canonicalDeobfClass(name: String): String {
    val n = name.replace('$', '.')
    obfToDeobfClassMap[n]?.let { return it }
    if (deobfToObfClassMap.containsKey(n)) return n
    val short = n.substringAfterLast('.')
    obfToDeobfClassMap[short]?.let { return it }
    deobfToObfClassMap[short]?.let { obf -> return obfToDeobfClassMap[obf] ?: short }
    return n
}

private fun resolveObfOverrideMethodDirect(
    superType: String,
    methodName: String,
    arity: Int,
    hints: List<String>
): String? {
    val named = canonicalDeobfClass(superType)
    var obf = deobfToObfMethodByArity[named]?.get(methodName)?.get(arity)
    if (obf.isNullOrEmpty()) obf = chooseOverloadByHints(named, methodName, arity, hints)
    if (!obf.isNullOrEmpty()) return obf
    val obfClass = deobfToObfClassMap[named]
    if (obfClass != null) {
        val retryNamed = obfToDeobfClassMap[obfClass] ?: named
        obf = deobfToObfMethodByArity[retryNamed]?.get(methodName)?.get(arity)
        if (obf.isNullOrEmpty()) obf = chooseOverloadByHints(retryNamed, methodName, arity, hints)
        if (!obf.isNullOrEmpty()) return obf
    }
    val g = resolveObfMethodGlobalByNameAndHints(methodName, arity, hints)
    return if (g != null && g.first == named) g.second else null
}


private fun normalizeHint(t: String): String {
    val x = t.removeSuffix("?")
    return when (x) {
        "Byte" -> "B"
        "Char" -> "C"
        "Double" -> "D"
        "Float" -> "F"
        "Int" -> "I"
        "Long" -> "J"
        "Short" -> "S"
        "Boolean" -> "Z"
        else -> x.replace('.', '/')
    }
}

private fun resolveObfMethodGlobalByNameAndHints(
    methodName: String,
    arity: Int,
    hints: List<String>
): Pair<String, String>? {
    var bestScore = -1
    var bestOwner: String? = null
    var bestObf: String? = null
    var tie = false
    deobfMethodOverloads.forEach { (owner, methods) ->
        val list = methods[methodName] ?: return@forEach
        for ((desc, obf) in list) {
            val ps = paramTypes(desc)
            if (ps.size != arity) continue
            var score = 0
            for (i in ps.indices) {
                val h = if (i < hints.size) hints[i] else ""
                if (h.isNotEmpty() && ps[i].contains(h)) score += 3
            }
            if (score > bestScore) {
                bestScore = score
                bestOwner = owner
                bestObf = obf
                tie = false
            } else if (score == bestScore && (bestOwner != owner || bestObf != obf)) {
                tie = true
            }
        }
    }
    if (bestScore <= 0 || tie) return null
    return bestOwner!! to bestObf!!
}

fun findOverrideRenames(ktFile: KtFile): Map<String, String> {
    val importSimple = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val ip = it.importPath?.pathStr ?: return@forEach
        importSimple[ip.substringAfterLast('.')] = ip
    }
    val out = mutableMapOf<String, String>()
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)
            if (!function.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD)) return
            val owner =
                generateSequence(function.parent) { it.parent }.firstOrNull { it is KtClassOrObject } as? KtClassOrObject
                    ?: return
            val directSupers =
                owner.superTypeListEntries.mapNotNull { it.typeReference?.text }.map { importSimple[it] ?: it }
            val superChain = LinkedHashSet<String>().apply {
                directSupers.forEach { addAll(collectSuperChainNamed(it)) }
            }
            val name = function.name ?: return
            val arity = function.valueParameters.size
            val hints = function.valueParameters.map { p ->
                val t = p.typeReference?.text ?: ""
                val fq = importSimple[t] ?: obfToDeobfClassMap[t] ?: t
                normalizeHint(fq)
            }
            val obf = resolveObfOverrideByChain(superChain.map(::canonicalDeobfClass).toSet(), name, arity, hints)
            if (!obf.isNullOrEmpty()) out[name] = obf
        }
    })
    return out
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
private fun literalHint(expr: KtExpression): String {
    return when (expr) {
        is KtStringTemplateExpression -> "java/lang/String"
        is KtConstantExpression -> when (expr.text.lowercase()) {
            "true", "false" -> "Z"
            else -> if (expr.text.contains('.')) "D" else "I"
        }

        is KtNameReferenceExpression -> expr.getReferencedName()
        else -> ""
    }
}

data class MethodUse(val className: String, val method: String, val arity: Int, val hints: List<String>)

fun findMethodUsages(
    ktFile: KtFile
): Set<MethodUse> {
    val found = mutableSetOf<MethodUse>()
    val importSimpleNameMap = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val importPath = it.importPath?.pathStr ?: return@forEach
        importSimpleNameMap[importPath.substringAfterLast('.')] = importPath
    }

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expr: KtCallExpression) {
            super.visitCallExpression(expr)
            val callee = expr.calleeExpression as? KtSimpleNameExpression ?: return
            val methodName = callee.getReferencedName()
            val dot = expr.parent as? KtDotQualifiedExpression
            val qualifierText = dot?.receiverExpression?.text

            val possibleClassNames = mutableListOf<String>()
            if (qualifierText != null) {
                deobfToObfClassMap[qualifierText]?.let { possibleClassNames.add(qualifierText) }
                importSimpleNameMap[qualifierText]?.let { possibleClassNames.add(it) }
                possibleClassNames.add(qualifierText)
            }

            val hints = expr.valueArguments.map { a ->
                val ex = a.getArgumentExpression()
                val h = if (ex != null) literalHint(ex) else ""
                val fq = importSimpleNameMap[h] ?: h
                fq.replace('.', '/')
            }
            val arity = hints.size

            var added = false
            for (cn in possibleClassNames) {
                val named = canonicalDeobfClass(cn)
                val hasName = deobfMethodOverloads[named]?.containsKey(methodName) == true
                val hasArity = deobfToObfMethodByArity[named]?.get(methodName)?.isNotEmpty() == true
                if (hasName || hasArity) {
                    found += MethodUse(named, methodName, arity, hints)
                    added = true
                    break
                }
            }
            if (!added && qualifierText != null) {
                val named = canonicalDeobfClass(qualifierText)
                if (deobfMethodOverloads[named]?.containsKey(methodName) == true) {
                    found += MethodUse(named, methodName, arity, hints)
                }
            }
        }
    })
    return found
}


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

fun collectProtectedRanges(ktFile: KtFile): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
            ranges += expression.textRange.startOffset until expression.textRange.endOffset
            super.visitStringTemplateExpression(expression)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            val t = expression.text
            if (t.length >= 2 && t.first() == '\'' && t.last() == '\'') {
                ranges += expression.textRange.startOffset until expression.textRange.endOffset
            }
            super.visitConstantExpression(expression)
        }
    })
    return ranges.sortedBy { it.first }
}

fun collectCommentRanges(ktFile: KtFile): List<IntRange> {
    val out = mutableListOf<IntRange>()
    ktFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is PsiComment) {
                out += element.textRange.startOffset until element.textRange.endOffset
            }
            super.visitElement(element)
        }
    })
    return out.sortedBy { it.first }
}

private fun intersectsProtected(start: Int, end: Int, protected: List<IntRange>): Boolean {
    var lo = 0
    var hi = protected.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val r = protected[mid]
        if (end - 1 < r.first) hi = mid - 1
        else if (start > r.last) lo = mid + 1
        else return true
    }
    return false
}

fun buildImportQualifierMap(ktFile: KtFile): Map<String, String> {
    val m = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val fq = it.importPath?.pathStr ?: return@forEach
        val obf = deobfToObfClassMap[fq] ?: return@forEach
        val simple = fq.substringAfterLast('.')
        m[simple] = obf.replace('$', '.')
    }
    return m
}

var traceVisitors = false

fun traceAll(ktFile: KtFile, logger: org.slf4j.Logger) {
    if (!traceVisitors) return
    ktFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            logger.info(
                "psi ${element.javaClass.simpleName} ${element.textRange.startOffset}-${element.textRange.endOffset} '${
                    element.text.take(
                        120
                    ).replace("\n", "\\n")
                }'"
            )
            super.visitElement(element)
        }
    })
}

private fun lineStartsOf(text: String): IntArray {
    val list = ArrayList<Int>()
    list.add(0)
    var i = 0
    while (i < text.length) {
        if (text[i] == '\n') list.add(i + 1)
        i++
    }
    return list.toIntArray()
}

private fun lineIndexOf(offset: Int, starts: IntArray): Int {
    var lo = 0
    var hi = starts.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val s = starts[mid]
        val e = if (mid + 1 < starts.size) starts[mid + 1] else Int.MAX_VALUE
        if (offset < s) hi = mid - 1 else if (offset >= e) lo = mid + 1 else return mid
    }
    return starts.size - 1
}

fun collectProtectedRangesByLine(ktFile: KtFile, source: String): Map<Int, List<IntRange>> {
    val starts = lineStartsOf(source)
    val map = HashMap<Int, MutableList<IntRange>>()
    fun addRange(absStart: Int, absEnd: Int) {
        var sOff = absStart
        val end = absEnd
        while (sOff < end) {
            val line = lineIndexOf(sOff, starts)
            val lineStart = starts[line]
            val lineEnd = if (line + 1 < starts.size) starts[line + 1] else source.length + 1
            val segStart = sOff - lineStart
            val segEnd = minOf(end, lineEnd) - lineStart
            if (segEnd > segStart) {
                map.getOrPut(line) { ArrayList() }.add(segStart until segEnd)
            }
            sOff = lineEnd
        }
    }
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
            addRange(expression.textRange.startOffset, expression.textRange.endOffset)
            super.visitStringTemplateExpression(expression)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            val t = expression.text
            if (t.length >= 2 && t.first() == '\'' && t.last() == '\'') {
                addRange(expression.textRange.startOffset, expression.textRange.endOffset)
            }
            super.visitConstantExpression(expression)
        }
    })
    ktFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is PsiComment) {
                addRange(element.textRange.startOffset, element.textRange.endOffset)
            }
            super.visitElement(element)
        }
    })
    map.keys.forEach { line ->
        val segs = map[line]!!
        segs.sortBy { it.first }
        val merged = ArrayList<IntRange>()
        var cur: IntRange? = null
        for (r in segs) {
            if (cur == null) cur = r
            else if (r.first <= cur!!.last) cur = cur!!.first until maxOf(cur!!.last + 1, r.last + 1)
            else {
                merged.add(cur!!); cur = r
            }
        }
        if (cur != null) {
            merged.add(cur!!)
            map[line]!!.clear()
            map[line]!!.addAll(merged)
        }
    }
    return map
}

private fun intersects(lineStart: Int, lineEnd: Int, protected: List<IntRange>): Boolean {
    var i = 0
    val n = protected.size
    while (i < n) {
        val r = protected[i]
        if (lineEnd <= r.first) return false
        if (lineStart < r.last + 1 && lineEnd > r.first) return true
        i++
    }
    return false
}

private fun safeReplaceLine(
    line: String,
    protected: List<IntRange>,
    pattern: Regex,
    replacement: String
): String {
    val matches = pattern.findAll(line).toList()
    if (matches.isEmpty()) return line
    val sb = StringBuilder(line)
    for (m in matches.asReversed()) {
        val s = m.range.first
        val e = m.range.last + 1
        if (intersects(s, e, protected)) continue
        sb.replace(s, e, replacement)
    }
    return sb.toString()
}

private fun safeReplaceLine(
    line: String,
    protected: List<IntRange>,
    pattern: Regex,
    transform: (MatchResult) -> String
): String {
    val matches = pattern.findAll(line).toList()
    if (matches.isEmpty()) return line
    val sb = StringBuilder(line)
    for (m in matches.asReversed()) {
        val s = m.range.first
        val e = m.range.last + 1
        if (intersects(s, e, protected)) continue
        sb.replace(s, e, transform(m))
    }
    return sb.toString()
}

private fun tryLoadNamed(fq: String): Class<*>? = try {
    Class.forName(fq, false, TestParser::class.java.classLoader)
} catch (_: Throwable) {
    null
}

private fun collectSuperChainNamed(startFq: String): Set<String> {
    val seen = LinkedHashSet<String>()
    val q: ArrayDeque<Class<*>> = ArrayDeque()
    val start = tryLoadNamed(startFq) ?: return setOf(startFq)
    q.add(start)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        if (!seen.add(c.name)) continue
        c.superclass?.let { q.add(it) }
        c.interfaces?.forEach { q.add(it) }
    }
    return seen
}

private fun resolveObfOverrideByChain(
    chain: Set<String>,
    methodName: String,
    arity: Int,
    hints: List<String>
): String? {
    for (s in chain) {
        val r = resolveObfOverrideMethodDirect(s, methodName, arity, hints)
        if (!r.isNullOrEmpty()) return r
    }
    val g = resolveObfMethodGlobalByNameAndHints(methodName, arity, hints)
    return if (g != null && canonicalDeobfClass(g.first) in chain) g.second else null
}

private const val GUARD_PREFIX = "__KS_GUARD__"

private fun guardLine(line: String, protected: List<IntRange>): Pair<String, List<String>> {
    if (protected.isEmpty()) return line to emptyList()
    var out = line
    val saved = ArrayList<String>(protected.size)
    val ranges = protected.sortedByDescending { it.first }
    ranges.forEachIndexed { idx, r ->
        val start = r.first
        val endExclusive = r.last + 1
        val token = "$GUARD_PREFIX${idx}__"
        saved.add(line.substring(start, endExclusive))
        out = out.substring(0, start) + token + out.substring(endExclusive)
    }
    return out to saved
}

private fun restoreLine(line: String, saved: List<String>): String {
    var out = line
    saved.forEachIndexed { idx, orig ->
        out = out.replace("$GUARD_PREFIX${idx}__", orig)
    }
    return out
}

class TestParser {
    companion object {

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

            val importLineMap = buildObfuscatedImports(ktFile)
            val foundClasses = findClassUsages(ktFile)
            val foundMethods = findMethodUsages(ktFile)

            val foundFields = findFieldUsages(ktFile)
            val nested = findNestedQualifiedClassUsages(ktFile)
            val overrideRenames = findOverrideRenames(ktFile)
            traceAll(ktFile, logger)
            val qualifierMap = buildImportQualifierMap(ktFile)
            val protectedByLine = collectProtectedRangesByLine(ktFile, string)
            val result = replaceObfuscated(
                string,
                foundClasses,
                foundMethods,
                foundFields,
                nested,
                overrideRenames,
                protectedByLine,
                qualifierMap
            )

            logger.info(
                "getInstance map: " + deobfToObfMethodByArity["net.minecraft.client.Minecraft"]?.get("getInstance")
                    ?.get(0)
            )

            Disposer.dispose(disposable)
            return result
        }

        fun findNestedQualifiedClassUsages(ktFile: KtFile): Map<String, String> {
            val importSimple = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val ip = it.importPath?.pathStr ?: return@forEach
                importSimple[ip.substringAfterLast('.')] = ip
            }

            fun resolveObfNestedFromAnyOuter(anyOuterText: String, innerSimple: String): String? {
                val outerNamed = importSimple[anyOuterText] ?: anyOuterText
                val deobfKey1 = "$outerNamed$$innerSimple"
                deobfToObfClassMap[deobfKey1]?.let { return it }

                val maybeDeobfOuter =
                    obfToDeobfClassMap[outerNamed] ?: obfToDeobfClassMap[anyOuterText]
                if (maybeDeobfOuter != null) {
                    val deobfKey2 = "$maybeDeobfOuter$$innerSimple"
                    deobfToObfClassMap[deobfKey2]?.let { return it }
                }
                return null
            }

            fun addAllSpellings(
                repl: MutableMap<String, String>,
                sourceOuterText: String,
                innerSimple: String,
                obfNestedFq: String
            ) {
                val to = obfNestedFq.replace('$', '.')
                repl["$sourceOuterText.$innerSimple"] = to
                val outerNamed = importSimple[sourceOuterText] ?: sourceOuterText
                if (outerNamed.contains('.')) {
                    repl["$outerNamed.$innerSimple"] = to
                }
                val obfOuter = deobfToObfClassMap[outerNamed]
                if (obfOuter != null) {
                    repl["${obfOuter.replace('$', '.')}.$innerSimple"] = to
                }
            }

            val repl = mutableMapOf<String, String>()

            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitUserType(type: KtUserType) {
                    super.visitUserType(type)
                    val inner = type.referencedName ?: return
                    val q = type.qualifier as? KtUserType ?: return
                    val outer = q.text
                    val obfNested = resolveObfNestedFromAnyOuter(outer, inner) ?: return
                    addAllSpellings(repl, outer, inner, obfNested)
                }

                override fun visitCallExpression(expr: KtCallExpression) {
                    super.visitCallExpression(expr)
                    val calleeName = (expr.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return
                    val dot = expr.parent as? KtDotQualifiedExpression ?: return
                    val outer = dot.receiverExpression.text
                    val obfNested = resolveObfNestedFromAnyOuter(outer, calleeName) ?: return
                    addAllSpellings(repl, outer, calleeName, obfNested)
                }
            })
            return repl
        }


        fun buildObfuscatedImports(ktFile: KtFile): Map<Int, String> {
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

        fun findClassUsages(ktFile: KtFile): Set<String> {
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


        fun findFieldUsages(
            ktFile: KtFile
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
                override fun visitDotQualifiedExpression(expr: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expr)
                    val selector = expr.selectorExpression as? KtSimpleNameExpression ?: return
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
            nestedQualifiedClassReplacements: Map<String, String>,
            overrideRenameMap: Map<String, String>,
            protectedByLine: Map<Int, List<IntRange>>,
            importQualifierMap: Map<String, String>
        ): String {
            val lines = source.lines().toMutableList()
            for (i in lines.indices) {
                val prot = protectedByLine[i] ?: emptyList()
                var line = lines[i]

                val importMatch = Regex("""^\s*import\s+(.+)$""").matchEntire(line.trim())
                if (importMatch != null) {
                    val importPath = importMatch.groupValues[1].trim()
                    val obf = deobfToObfClassMap[importPath]
                    if (obf != null) line = "import ${obf.replace('$', '.')}"
                    lines[i] = line
                    continue
                }

                val (guarded, saved) = guardLine(line, prot)
                line = guarded
                nestedQualifiedClassReplacements.forEach { (from, to) ->
                    line = safeReplaceLine(line, prot, Regex("""\b${Regex.escape(from)}\b"""), to)
                }

                importQualifierMap.forEach { (simple, fq) ->
                    line = safeReplaceLine(line, prot, Regex("""(?<!\w)${Regex.escape(simple)}(?=\s*\.)"""), fq)
                }

                overrideRenameMap.forEach { (from, to) ->
                    line = safeReplaceLine(
                        line, emptyList(),
                        Regex("""\boverride\s+fun\s+${Regex.escape(from)}\s*\("""), "override fun $to("
                    )
                    line = safeReplaceLine(
                        line, emptyList(),
                        Regex("""\bsuper\s*\.\s*${Regex.escape(from)}\s*\("""), "super.$to("
                    )
                }

                foundClasses.forEach { deobf ->
                    val key = if (deobfToObfClassMap.containsKey(deobf)) deobf else deobf.substringAfterLast('$')
                    val obf = deobfToObfClassMap[key]
                    if (obf != null) {
                        val src = obf.replace('$', '.')
                        line = safeReplaceLine(line, emptyList(), Regex("""\b${Regex.escape(deobf)}\b"""), src)
                    }
                }

                foundFields.forEach { (className, deobfField) ->
                    val obfClass0 = deobfToObfClassMap[className] ?: className
                    val obfClass = obfClass0.replace('$', '.')
                    val obf = deobfToObfFieldMap[className]?.get(deobfField)
                    if (obf != null) {
                        line = safeReplaceLine(
                            line, emptyList(),
                            Regex(
                                """(${Regex.escape(obfClass)}|${Regex.escape(className.substringAfterLast('.'))})\s*\.\s*${
                                    Regex.escape(
                                        deobfField
                                    )
                                }\b"""
                            )
                        ) { mr -> "${mr.groupValues[1]}.$obf" }
                        line = safeReplaceLine(line, emptyList(), Regex("""\b${Regex.escape(deobfField)}\b"""), obf)
                    }
                }

                foundMethods.forEach { (className, deobfMethod, arity, hints) ->
                    val obfClass0 = deobfToObfClassMap[className] ?: className
                    val obfClass = obfClass0.replace('$', '.')
                    var obfMethod = deobfToObfMethodByArity[className]?.get(deobfMethod)?.get(arity)
                    if (obfMethod.isNullOrEmpty()) obfMethod =
                        chooseOverloadByHints(className, deobfMethod, arity, hints)
                    if (obfMethod.isNullOrEmpty()) {
                        val g = resolveObfMethodGlobalByNameAndHints(deobfMethod, arity, hints)
                        if (g != null && g.first == canonicalDeobfClass(className)) obfMethod = g.second
                    }
                    if (!obfMethod.isNullOrEmpty()) {
                        line = safeReplaceLine(
                            line, emptyList(),
                            Regex(
                                """(${Regex.escape(obfClass)}|${Regex.escape(className.substringAfterLast('.'))})\s*\.\s*${
                                    Regex.escape(
                                        deobfMethod
                                    )
                                }\s*\("""
                            )
                        ) { mr -> "${mr.groupValues[1]}.$obfMethod(" }
                        line =
                            safeReplaceLine(line, emptyList(), Regex("""\b${Regex.escape(deobfMethod)}\b"""), obfMethod)
                    }
                }

                line = restoreLine(line, saved)
                lines[i] = line
            }

            return lines.joinToString("\n").replace("net.field_5645.", "net.minecraft.")
        }

    }
}
