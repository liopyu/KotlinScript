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
        "deobfFieldTypeMap" to deobfFieldTypeMap
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

    fun <T> parseMap(key: String, token: java.lang.reflect.Type): T {
        val jsonObj = gson.toJson(data[key] ?: emptyMap<String, Any>())
        return gson.fromJson(jsonObj, token) ?: throw IllegalStateException("Failed to parse $key from mappings.json")
    }

    val classMapType = object : TypeToken<Map<String, String>>() {}.type
    val methodMapType = object : TypeToken<Map<String, Map<String, String>>>() {}.type

    deobfFieldTypeMap.clear()
    runCatching {
        parseMap<Map<String, Map<String, String>>>("deobfFieldTypeMap", methodMapType)
            .forEach { (k, v) -> deobfFieldTypeMap[k] = v.toMutableMap() }
    }.onFailure {
        logger.info("deobfFieldTypeMap missing in mappings.json (ok for legacy).")
    }
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

private fun descToNamedTypeOrNull(desc: String): String? {
    var d = desc
    while (d.startsWith("[")) d = d.substring(1)
    return if (d.startsWith("L") && d.endsWith(";"))
        d.substring(1, d.length - 1).replace('/', '.')
    else
        null
}

data class ChainRewrite(val line: Int, val from: String, val to: String)

private fun asChain(e: KtExpression): List<KtExpression> {
    val out = mutableListOf<KtExpression>()
    fun rec(x: KtExpression) {
        if (x is KtDotQualifiedExpression) {
            rec(x.receiverExpression)
            x.selectorExpression?.let { rec(it) }
        } else out += x
    }
    rec(e)
    return out
}

private fun nextTypeAfterField(ownerDeobf: String, deobfField: String): String? {
    deobfFieldTypeMap[ownerDeobf]?.get(deobfField)?.let { return it }
    return deobfFieldTypeMap[canonicalDeobfClass(ownerDeobf)]?.get(deobfField)
}

val debugChains = true
var debugMethods = true

private fun mdbg(msg: () -> String) {
    if (debugMethods) logger.info("[KS-method] ${msg()}")
}

private fun dbg(msg: () -> String) {
    if (debugChains) logger.info("[KS-chain] ${msg()}")
}

private fun tryLoadAny(name: String): Class<*>? {
    val cl = TestParser::class.java.classLoader
    var c: Class<*>? = null
    if (c == null) c = runCatching { Class.forName(name, false, cl) }.getOrNull()
    if (c == null) c = runCatching { Class.forName(name.replace('.', '$'), false, cl) }.getOrNull()
    if (c == null) c = runCatching { Class.forName(name.replace('$', '.'), false, cl) }.getOrNull()
    return c
}

private fun findFieldInHierarchy(c0: Class<*>, name: String): java.lang.reflect.Field? {
    var c: Class<*>? = c0
    while (c != null) {
        val clazz = c
        val f = runCatching { clazz.getDeclaredField(name) }.getOrNull()
        if (f != null) return f
        c = clazz.superclass
    }
    return null
}

/** Discover the next deobf type for ownerDeobf.deobfField using reflection on the obf class. */
private fun reflectDeobfFieldType(ownerDeobf: String, deobfField: String): String? {
    val obfOwner = deobfToObfClassMap[ownerDeobf] ?: return null
    val obfField = deobfToObfFieldMap[ownerDeobf]?.get(deobfField) ?: return null

    obfFieldTypeDeobfMap[obfOwner]?.get(obfField)?.let { return it }

    val cls = tryLoadAny(obfOwner) ?: return null
    val fld = findFieldInHierarchy(cls, obfField) ?: return null

    val obfType = fld.type.name
    val deobfNonNull: String =
        obfToDeobfClassMap[obfType]
            ?: obfToDeobfClassMap[obfType.replace('$', '.')]
            ?: obfType

    val canon = canonicalDeobfClass(deobfNonNull)

    deobfFieldTypeMap.getOrPut(ownerDeobf) { mutableMapOf() }[deobfField] = canon
    obfFieldTypeDeobfMap.getOrPut(obfOwner) { mutableMapOf() }[obfField] = canon

    return canon
}

private fun jvmMethodReturnToClassName(desc: String): String? {
    val rp = desc.indexOf(')')
    if (rp < 0 || rp + 1 >= desc.length) return null
    var i = rp + 1
    while (i < desc.length && desc[i] == '[') i++
    return if (i < desc.length && desc[i] == 'L') {
        val end = desc.indexOf(';', i)
        if (end > i) desc.substring(i + 1, end).replace('/', '.') else null
    } else null
}

private fun isObfMethodName(n: String): Boolean = n.startsWith("method_")

private fun paramCount(desc: String): Int = countParams(desc)

private fun deobfRetFromDeobfDesc(desc: String): String? =
    jvmMethodReturnToClassName(desc)?.let(::canonicalDeobfClass)

private fun deobfRetFromObfDesc(desc: String): String? {
    val obf = jvmMethodReturnToClassName(desc) ?: return null
    val named = obfToDeobfClassMap[obf] ?: obfToDeobfClassMap[obf.replace('$', '.')] ?: obf
    return canonicalDeobfClass(named)
}


private fun deobfMethodReturnType(ownerDeobf: String, methodName: String, arity: Int, hints: List<String>): String? {
    val list = deobfMethodOverloads[ownerDeobf]?.get(methodName) ?: return null
    var best: Pair<Int, String?> = -1 to null
    for ((desc, _) in list) {
        val ps = paramTypes(desc)
        if (ps.size != arity) continue
        var score = 0
        for (i in 0 until minOf(hints.size, ps.size)) {
            val h = hints[i]
            if (h.isNotEmpty() && ps[i].contains(h)) score += 3
        }
        if (score > best.first) best = score to jvmMethodReturnToClassName(desc)
    }
    return best.second?.let(::canonicalDeobfClass)
}

private fun obfMethodReturnType(ownerDeobf: String, obfMethod: String, arity: Int, hints: List<String>): String? {
    val obfOwner = deobfToObfClassMap[ownerDeobf] ?: return null
    val table = obfToDeobfMethodMap[obfOwner] ?: return null

    var bestScore = -1
    var bestRet: String? = null

    for ((key, _) in table) {
        if (!key.startsWith("$obfMethod(")) continue
        val desc = key.substring(obfMethod.length)
        if (countParams(desc) != arity) continue

        val ps = paramTypes(desc)
        var score = 0
        for (i in 0 until minOf(hints.size, ps.size)) {
            val h = hints[i]
            if (h.isNotEmpty() && ps[i].contains(h)) score += 3
        }

        if (score >= bestScore) {
            bestScore = score
            val retObf = jvmMethodReturnToClassName(desc)
            bestRet = when {
                retObf == null -> null
                else -> {
                    val named = obfToDeobfClassMap[retObf]
                        ?: obfToDeobfClassMap[retObf.replace('$', '.')]
                        ?: retObf
                    canonicalDeobfClass(named)
                }
            }
        }
    }
    return bestRet
}

private fun argHints(call: KtCallExpression, imports: Map<String, String>): List<String> {
    val out = ArrayList<String>(call.valueArguments.size)
    var i = 0
    while (i < call.valueArguments.size) {
        val ex = call.valueArguments[i].getArgumentExpression()
        val h = when (ex) {
            is KtStringTemplateExpression -> "java/lang/String"
            is KtConstantExpression -> when (ex.text.lowercase()) {
                "true", "false" -> "Z"
                else -> if (ex.text.contains('.')) "D" else "I"
            }

            is KtNameReferenceExpression -> ex.getReferencedName()
            is KtCallExpression -> resolveCtorType(ex, imports)?.replace('.', '/')
            else -> ""
        } ?: ""
        out += h
        i++
    }
    return out
}

fun findChainedFieldRewrites(ktFile: KtFile, source: String): Map<Int, List<Pair<String, String>>> {
    val starts = lineStartsOf(source)
    fun lineOf(offset: Int) = lineIndexOf(offset, starts)

    val importSimple = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val ip = it.importPath?.pathStr ?: return@forEach
        importSimple[ip.substringAfterLast('.')] = ip
    }
    fun fq(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val n = importSimple[text] ?: text
        return canonicalDeobfClass(n)
    }

    val varTypes = findVariableTypes(ktFile)
    val rewritesByLine = mutableMapOf<Int, MutableList<Pair<String, String>>>()

    fun asChain(e: KtExpression): List<KtExpression> {
        val out = mutableListOf<KtExpression>()
        fun rec(x: KtExpression) {
            if (x is KtDotQualifiedExpression) {
                rec(x.receiverExpression)
                x.selectorExpression?.let { rec(it) }
            } else out += x
        }
        rec(e)
        return out
    }

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitDotQualifiedExpression(expr: KtDotQualifiedExpression) {
            super.visitDotQualifiedExpression(expr)

            val parts = asChain(expr)
            if (parts.isEmpty()) return

            val headExpr = parts[0]
            var outHeadText: String? = null
            var startType: String? = null

            when (headExpr) {
                is KtNameReferenceExpression -> {
                    val rootName = headExpr.getReferencedName()
                    val t = varTypes[rootName] ?: fq(rootName)
                    startType = t
                    outHeadText = rootName
                }

                is KtCallExpression -> {
                    val t = resolveCtorType(headExpr, importSimple)
                    startType = t
                    outHeadText = headExpr.text
                }

                is KtDotQualifiedExpression -> {
                    val t = canonicalDeobfClass(headExpr.text)
                    startType = t
                    outHeadText = headExpr.text
                }

                else -> return
            }

            if (startType == null || outHeadText == null) return

            var curDeobfType = canonicalDeobfClass(startType)
            val curObfType = { deobfToObfClassMap[curDeobfType] }

            dbg { "expr='${expr.text}' head='${headExpr.text}' curDeobfType='$curDeobfType' obfType='${curObfType()}'" }

            val outParts = ArrayList<String>()
            outParts.add(outHeadText!!)
            var changed = false

            var i = 1
            while (i < parts.size) {
                val nameRef = parts[i] as? KtNameReferenceExpression
                if (nameRef == null) {
                    val call = parts[i] as? KtCallExpression
                    if (call != null) {
                        val callee = call.calleeExpression as? KtSimpleNameExpression ?: break
                        val mn = callee.getReferencedName()
                        val hints = argHints(call, importSimple)
                        val arity = hints.size

                        val pick = strongPickOnOwner(curDeobfType, mn, arity, hints)
                        outParts += call.text
                        dbg { " step[$i]: call '$mn/$arity' owner='$curDeobfType' -> next='${pick.retDeobf ?: "void"}'" }

                        if (pick.retDeobf == null) {
                            var j = i + 1
                            while (j < parts.size) {
                                outParts += parts[j].text; j++
                            }
                            break
                        }

                        curDeobfType = canonicalDeobfClass(pick.retDeobf)
                        i++
                        continue
                    }
                    break
                }

                val token = nameRef.getReferencedName()

                val m = deobfToObfFieldMap[curDeobfType]
                val obfFromDeobf = m?.get(token)
                if (obfFromDeobf != null) {
                    outParts.add(obfFromDeobf)
                    changed = true

                    var next = nextTypeAfterField(curDeobfType, token)
                    if (next == null) next = reflectDeobfFieldType(curDeobfType, token)

                    dbg { " step[$i]: deobf field '$token' -> obf '$obfFromDeobf', nextDeobfType='${next ?: "null"}'" }

                    if (next == null) {
                        var j = i + 1
                        while (j < parts.size) {
                            outParts += parts[j].text; j++
                        }
                        break
                    } else {
                        curDeobfType = canonicalDeobfClass(next)
                        i++
                        continue
                    }
                }

                val obfOwner = curObfType()
                var deobfField: String? = null
                if (obfOwner != null) {
                    val back = obfToDeobfFieldMap[obfOwner]
                    if (back != null) deobfField = back[token]
                }
                if (deobfField != null) {
                    outParts.add(token)
                    var next = deobfFieldTypeMap[curDeobfType]?.get(deobfField)
                    if (next == null) next = reflectDeobfFieldType(curDeobfType, deobfField)
                    dbg { " step[$i]: obf field '$token' (deobf='$deobfField'), nextDeobfType='${next ?: "null"}'" }
                    if (next == null) break
                    curDeobfType = canonicalDeobfClass(next)
                    i++
                    continue
                }

                dbg { " step[$i]: token '$token' did not match fields of '$curDeobfType' (or '${obfOwner}')" }
                break
            }

            if (changed) {
                val from = expr.text
                val to = outParts.joinToString(".")
                val lineIdx = lineOf(expr.textRange.startOffset)
                val list = rewritesByLine.getOrPut(lineIdx) { mutableListOf() }
                list.add(from to to)
                dbg { " rewrite: '$from' -> '$to'" }
            } else {
                dbg { " no rewrite for '${expr.text}'" }
            }
        }
    })
    return rewritesByLine
}

fun findVariableTypes(ktFile: KtFile): Map<String, String> {
    val importSimple = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val ip = it.importPath?.pathStr ?: return@forEach
        importSimple[ip.substringAfterLast('.')] = ip
    }
    val vars = mutableMapOf<String, String>()

    fun fq(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val n = importSimple[text] ?: text
        return canonicalDeobfClass(n)
    }

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)
            function.valueParameters.forEach { p ->
                val t = p.typeReference?.text?.let(::fq) ?: return@forEach
                vars[p.name ?: return@forEach] = t
            }
        }

        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)
            val t = property.typeReference?.text?.let(::fq) ?: return
            property.name?.let { vars[it] = t }
        }
    })
    return vars
}


val logger = LogUtils.getLogger()

val deobfToObfClassMap: MutableMap<String, String> = mutableMapOf()
val deobfToObfMethodMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
val deobfToObfFieldMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

val obfToDeobfClassMap: MutableMap<String, String> = mutableMapOf()
val obfToDeobfMethodMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
val obfToDeobfFieldMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
val obfFieldTypeDeobfMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
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
        }

        classCount++
    }

    logger.info("buildDeobfToObfMapFromTree: Finished! Total mapped classes: $classCount, skipped classes: $skippedClassCount")
    logger.info("  deobfToObfClassMap size: ${deobfToObfClassMap.size}")
    logger.info("  deobfToObfMethodMap size: ${deobfToObfMethodMap.size}")
    logger.info("  deobfToObfFieldMap size: ${deobfToObfFieldMap.size}")
}

data class StrongPick(val obfName: String?, val retDeobf: String?)

private fun strongPickOnOwner(
    ownerDeobf: String,
    nameOrObf: String,
    arity: Int,
    hints: List<String>
): StrongPick {
    val mp = mappingPickOnOwner(ownerDeobf, nameOrObf, arity, hints)
    if (mp != null) return StrongPick(mp.obfName, mp.retDeobf)

    val rp = reflectPickOnOwner(ownerDeobf, nameOrObf, arity, hints)
    if (rp != null) return StrongPick(rp.obfName, rp.retDeobf)

    return StrongPick(null, null)
}


private fun reflectPickOnOwner(
    ownerDeobf: String,
    nameOrObf: String,
    arity: Int,
    hints: List<String>
): ReflectPick? {
    val cls = tryLoadEither(ownerDeobf) ?: return null

    val cands = LinkedHashMap<String, java.lang.reflect.Method>()
    fun accept(m: java.lang.reflect.Method) {
        val k = methodKey(m)
        if (!cands.containsKey(k)) cands[k] = m
    }

    var arr = cls.methods
    var i = 0
    while (i < arr.size) {
        val m = arr[i]; accept(m); i++
    }
    arr = cls.declaredMethods
    i = 0
    while (i < arr.size) {
        val m = arr[i]; accept(m); i++
    }

    val filtered = ArrayList<java.lang.reflect.Method>()
    val backOwner = deobfToObfClassMap[ownerDeobf]?.let(::normalizeOwnerForBackMap)
    val backMap = if (backOwner != null) obfToDeobfMethodMap[backOwner] else null

    for (m in cands.values) {
        val n = m.name
        var ok =
            (n == nameOrObf) || (!isObfMethodName(nameOrObf) && backMap != null && backMap[methodKey(m)] == nameOrObf)
        if (ok) filtered += m
    }
    if (filtered.isEmpty()) return null

    var best: java.lang.reflect.Method? = null
    var bestScore = Int.MIN_VALUE
    i = 0
    while (i < filtered.size) {
        val m = filtered[i]
        val pc = m.parameterCount
        var s = if (pc == arity) 1000 else 1000 - kotlin.math.abs(pc - arity) * 50
        val pts = m.parameterTypes
        val n = minOf(pts.size, hints.size)
        var j = 0
        while (j < n) {
            val h = hints[j]
            if (h.isNotEmpty()) {
                val atom = classToJvmAtom(pts[j])
                if (atom.contains(h)) s += 3
            }
            j++
        }
        if (s > bestScore) {
            bestScore = s; best = m
        }
        i++
    }

    val chosen = best ?: return null
    val ret = chosen.returnType
    val retDeobf = if (ret == java.lang.Void.TYPE) null else {
        val obf = ret.name
        val named = obfToDeobfClassMap[obf] ?: obfToDeobfClassMap[obf.replace('$', '.')] ?: obf
        canonicalDeobfClass(named)
    }

    return ReflectPick(chosen.name, retDeobf)
}

data class MappingPick(val obfName: String, val retDeobf: String?)

private fun mappingPickOnOwner(
    ownerDeobf: String,
    nameOrObf: String,
    arity: Int,
    hints: List<String>
): MappingPick? {
    if (!isObfMethodName(nameOrObf)) {
        val list = deobfMethodOverloads[ownerDeobf]?.get(nameOrObf) ?: return null
        var bestObf: String? = null
        var bestRet: String? = null
        var bestScore = Int.MIN_VALUE
        for ((deobfDesc, obf) in list) {
            val pc = paramCount(deobfDesc)
            val base = if (pc == arity) 1000 else 1000 - kotlin.math.abs(pc - arity) * 50
            val ps = paramTypes(deobfDesc)
            var s = base
            val n = minOf(ps.size, hints.size)
            var i = 0
            while (i < n) {
                val h = hints[i]
                if (h.isNotEmpty() && ps[i].contains(h)) s += 3
                i++
            }
            if (s > bestScore) {
                bestScore = s
                bestObf = obf
                bestRet = deobfRetFromDeobfDesc(deobfDesc)
            }
        }
        if (bestObf != null) return MappingPick(bestObf!!, bestRet)
        return null
    }

    val obfOwner = deobfToObfClassMap[ownerDeobf] ?: return null
    val table = obfToDeobfMethodMap[obfOwner] ?: return null
    var bestObf: String? = null
    var bestRet: String? = null
    var bestScore = Int.MIN_VALUE

    for ((key, _) in table) {
        if (!key.startsWith("$nameOrObf(")) continue
        val desc = key.substring(nameOrObf.length)
        val pc = paramCount(desc)
        val base = if (pc == arity) 1000 else 1000 - kotlin.math.abs(pc - arity) * 50
        val ps = paramTypes(desc)
        var s = base
        val n = minOf(ps.size, hints.size)
        var i = 0
        while (i < n) {
            val h = hints[i]
            if (h.isNotEmpty() && ps[i].contains(h)) s += 3
            i++
        }
        if (s > bestScore) {
            bestScore = s
            bestObf = nameOrObf
            bestRet = deobfRetFromObfDesc(desc)
        }
    }
    return if (bestObf != null) MappingPick(bestObf!!, bestRet) else null
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
val deobfFieldTypeMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
private fun jvmFieldDescToClassName(desc: String): String? {
    var i = 0
    while (i < desc.length && desc[i] == '[') i++
    return if (i < desc.length && desc[i] == 'L') {
        val end = desc.indexOf(';', i)
        if (end > i) desc.substring(i + 1, end).replace('/', '.') else null
    } else null
}


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

private fun resolveTypeRef(text: String, imports: Map<String, String>): String {
    val parts = text.split('.')
    if (parts.size == 1) {
        val fq = imports[text] ?: text
        return canonicalDeobfClass(fq)
    }
    val outerSimple = parts.first()
    val outerFq = imports[outerSimple] ?: outerSimple
    val nested = buildString {
        append(outerFq)
        for (i in 1 until parts.size) append('$').append(parts[i])
    }
    return canonicalDeobfClass(nested)
}

private fun resolveCtorType(expr: KtCallExpression, imports: Map<String, String>): String? {
    val callee = expr.calleeExpression ?: return null
    return when (callee) {
        is KtNameReferenceExpression -> {
            val t = callee.getReferencedName()
            val fq = imports[t] ?: t
            canonicalDeobfClass(fq)
        }

        is KtDotQualifiedExpression -> canonicalDeobfClass(callee.text)
        else -> null
    }
}

data class MethodUse(val className: String, val method: String, val arity: Int, val hints: List<String>)
private class TypeEnv {
    private val stack = ArrayDeque<MutableMap<String, String>>()
    fun push() = stack.addLast(mutableMapOf())
    fun pop() {
        stack.removeLast()
    }

    fun put(name: String, deobfFqcn: String) {
        stack.last()[name] = deobfFqcn
    }

    fun get(name: String): String? = stack.asReversed().firstNotNullOfOrNull { it[name] }
}

fun findMethodUsages(ktFile: KtFile): Set<MethodUse> {
    val found = mutableSetOf<MethodUse>()
    val importSimpleNameMap = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val p = it.importPath?.pathStr ?: return@forEach
        importSimpleNameMap[p.substringAfterLast('.')] = p
    }

    val env = TypeEnv()
    env.push()

    ktFile.accept(object : KtTreeVisitorVoid() {

        override fun visitNamedFunction(function: KtNamedFunction) {
            env.push()
            function.valueParameters.forEach { p ->
                val t = p.typeReference?.text ?: return@forEach
                val fq = resolveTypeRef(t, importSimpleNameMap)
                p.name?.let { env.put(it, fq) }
            }
            super.visitNamedFunction(function)
            env.pop()
        }

        override fun visitProperty(property: KtProperty) {
            property.typeReference?.text?.let { t ->
                val fq = resolveTypeRef(t, importSimpleNameMap)
                property.name?.let { env.put(it, fq) }
            } ?: run {
                val init = property.initializer as? KtCallExpression
                val fq = init?.let { resolveCtorType(it, importSimpleNameMap) }
                if (fq != null) property.name?.let { env.put(it, fq) }
            }
            super.visitProperty(property)
        }

        override fun visitCallExpression(expr: KtCallExpression) {
            super.visitCallExpression(expr)
            val callee = expr.calleeExpression as? KtSimpleNameExpression ?: return
            val methodName = callee.getReferencedName()
            val dot = expr.parent as? KtDotQualifiedExpression ?: return
            val qualifierExpr = dot.receiverExpression

            val candidates = mutableListOf<String>()

            when (val q = qualifierExpr) {

                is KtNameReferenceExpression -> {
                    val qn = q.getReferencedName()
                    importSimpleNameMap[qn]?.let { candidates += canonicalDeobfClass(it) }
                    if (deobfToObfClassMap.containsKey(qn)) candidates += canonicalDeobfClass(qn)
                }

                is KtDotQualifiedExpression -> {
                    val innerCall = q.selectorExpression as? KtCallExpression
                    if (innerCall != null) {
                        val ownerExpr = q.receiverExpression
                        val innerOwnerDeobf = when (ownerExpr) {
                            is KtNameReferenceExpression -> {
                                val qn = ownerExpr.getReferencedName()
                                canonicalDeobfClass(importSimpleNameMap[qn] ?: qn)
                            }

                            is KtDotQualifiedExpression -> canonicalDeobfClass(ownerExpr.text)
                            else -> null
                        }

                        val innerName = (innerCall.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                        val innerArity = innerCall.valueArguments.size
                        val innerHints = innerCall.valueArguments.map { a ->
                            val ex = a.getArgumentExpression()
                            when (ex) {
                                is KtStringTemplateExpression -> "java/lang/String"
                                is KtConstantExpression -> when (ex.text.lowercase()) {
                                    "true", "false" -> "Z"
                                    else -> if (ex.text.contains('.')) "D" else "I"
                                }

                                is KtNameReferenceExpression -> ex.getReferencedName()
                                is KtCallExpression -> resolveCtorType(ex, importSimpleNameMap)?.replace('.', '/')
                                else -> ""
                            } ?: ""
                        }

                        val ret = if (innerOwnerDeobf != null && innerName != null)
                            deobfMethodReturnType(innerOwnerDeobf, innerName, innerArity, innerHints)
                                ?: obfMethodReturnType(innerOwnerDeobf, innerName, innerArity, innerHints)
                                ?: reflectMethodReturnType(innerOwnerDeobf, innerName, innerArity, innerHints)
                        else null

                        if (ret != null) candidates += canonicalDeobfClass(ret)
                    } else {
                        candidates += canonicalDeobfClass(q.text)
                    }
                }

                is KtCallExpression -> {
                    val innerDot = qualifierExpr.parent as? KtDotQualifiedExpression
                    val ownerExpr = innerDot?.receiverExpression
                    val ownerDeobf = when (ownerExpr) {
                        is KtNameReferenceExpression -> {
                            val qn = ownerExpr.getReferencedName()
                            canonicalDeobfClass(importSimpleNameMap[qn] ?: qn)
                        }

                        is KtDotQualifiedExpression -> canonicalDeobfClass(ownerExpr.text)
                        else -> null
                    }

                    var innerName: String? = null
                    val innerCallee = q.calleeExpression as? KtSimpleNameExpression
                    if (innerCallee != null) innerName = innerCallee.getReferencedName()
                    val innerArity = q.valueArguments.size
                    val innerHints = q.valueArguments.map { a ->
                        val ex = a.getArgumentExpression()
                        val h = when (ex) {
                            is KtStringTemplateExpression -> "java/lang/String"
                            is KtConstantExpression -> when (ex.text.lowercase()) {
                                "true", "false" -> "Z"
                                else -> if (ex.text.contains('.')) "D" else "I"
                            }

                            is KtNameReferenceExpression -> ex.getReferencedName()
                            is KtCallExpression -> resolveCtorType(ex, importSimpleNameMap)?.replace('.', '/')
                            else -> ""
                        } ?: ""
                        h
                    }

                    val ret = if (ownerDeobf != null && innerName != null)
                        deobfMethodReturnType(ownerDeobf, innerName, innerArity, innerHints)
                            ?: obfMethodReturnType(ownerDeobf, innerName, innerArity, innerHints)
                            ?: reflectMethodReturnType(ownerDeobf, innerName, innerArity, innerHints)
                    else null

                    if (ret != null) candidates += canonicalDeobfClass(ret)

                    mdbg {
                        "usage-scan recv=CALL innerOwner=${ownerDeobf ?: "?"} innerName=${innerName ?: "?"}/$innerArity " +
                                "innerRet=${ret ?: "null"} → candidates=${candidates}"
                    }
                }
            }

            val hints = expr.valueArguments.map { a ->
                val ex = a.getArgumentExpression()
                val h = when (ex) {
                    is KtStringTemplateExpression -> "java/lang/String"
                    is KtConstantExpression -> when (ex.text.lowercase()) {
                        "true", "false" -> "Z"
                        else -> if (ex.text.contains('.')) "D" else "I"
                    }

                    is KtNameReferenceExpression -> ex.getReferencedName()
                    is KtCallExpression -> resolveCtorType(ex, importSimpleNameMap)?.replace('.', '/')
                    else -> ""
                } ?: ""
                h
            }
            val arity = hints.size

            var added = false
            for (owner in candidates) {
                val hasName = deobfMethodOverloads[owner]?.containsKey(methodName) == true
                val hasArity = deobfToObfMethodByArity[owner]?.get(methodName)?.isNotEmpty() == true
                mdbg { "probe owner=$owner meth=$methodName/$arity hasName=$hasName hasArity=$hasArity" }
                if (hasName || hasArity) {
                    found += MethodUse(owner, methodName, arity, hints)
                    mdbg { "ADD use owner=$owner meth=$methodName/$arity hints=$hints" }
                    added = true
                    break
                }
            }
            if (!added && candidates.isNotEmpty()) {
                found += MethodUse(candidates.first(), methodName, arity, hints)
                mdbg { "ADD fallback use owner=${candidates.first()} meth=$methodName/$arity (no table match)" }
            }
        }


    })

    env.pop()
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
            val lineEnd = if (line + 1 < starts.size) starts[line + 1] else source.length
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
            else if (r.first <= cur!!.last) {
                cur = cur!!.first until maxOf(cur!!.last + 1, r.last + 1)
            } else {
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

private fun hintOfClass(c: Class<*>): String {
    if (c.isArray) {
        var t = c
        var dims = ""
        while (t.isArray) {
            dims += "["; t = t.componentType
        }
        return when (t) {
            java.lang.Byte.TYPE -> dims + "B"
            java.lang.Character.TYPE -> dims + "C"
            java.lang.Double.TYPE -> dims + "D"
            java.lang.Float.TYPE -> dims + "F"
            java.lang.Integer.TYPE -> dims + "I"
            java.lang.Long.TYPE -> dims + "J"
            java.lang.Short.TYPE -> dims + "S"
            java.lang.Boolean.TYPE -> dims + "Z"
            else -> dims + "L" + t.name.replace('.', '/')
        }
    }
    if (c.isPrimitive) {
        return when (c) {
            java.lang.Byte.TYPE -> "B"
            java.lang.Character.TYPE -> "C"
            java.lang.Double.TYPE -> "D"
            java.lang.Float.TYPE -> "F"
            java.lang.Integer.TYPE -> "I"
            java.lang.Long.TYPE -> "J"
            java.lang.Short.TYPE -> "S"
            java.lang.Boolean.TYPE -> "Z"
            java.lang.Void.TYPE -> "V"
            else -> ""
        }
    }
    return "L" + c.name.replace('.', '/')
}

private fun scoreByHints(paramTypes: Array<Class<*>>, hints: List<String>): Int {
    var score = 0
    val n = minOf(paramTypes.size, hints.size)
    var i = 0
    while (i < n) {
        val h = hints[i]
        if (h.isNotEmpty()) {
            val ph = hintOfClass(paramTypes[i])
            if (ph.contains(h)) score += 3
        }
        i++
    }
    return score
}

/** Reflect the return type of a method on ownerDeobf. Works for deobf or obf method names. */
private fun reflectMethodReturnType(
    ownerDeobf: String,
    methodNameOrObf: String,
    arity: Int,
    hints: List<String>
): String? {
    val cls = tryLoadEither(ownerDeobf) ?: return null

    var obfGuess: String? = deobfToObfMethodByArity[ownerDeobf]?.get(methodNameOrObf)?.get(arity)
    if (obfGuess.isNullOrEmpty()) {
        obfGuess = chooseOverloadByHints(ownerDeobf, methodNameOrObf, arity, hints)
    }

    val candidates = ArrayList<java.lang.reflect.Method>()
    for (m in cls.methods) {
        val pn = m.parameterCount
        if (pn != arity) continue
        val name = m.name
        if (name == methodNameOrObf || (obfGuess != null && name == obfGuess)) {
            candidates += m
        }
    }
    if (candidates.isEmpty()) {
        for (m in cls.declaredMethods) {
            val pn = m.parameterCount
            if (pn != arity) continue
            val name = m.name
            if (name == methodNameOrObf || (obfGuess != null && name == obfGuess)) {
                candidates += m
            }
        }
    }
    if (candidates.isEmpty()) {
        val obfOwner = deobfToObfClassMap[ownerDeobf]
        val back = if (obfOwner != null) obfToDeobfMethodMap[obfOwner] else null
        if (back != null) {
            for (m in cls.methods) {
                if (m.parameterCount != arity) continue
                val name = m.name
                val key = buildString {
                    append(name).append('(')
                    val pts = m.parameterTypes
                    for (t in pts) append(hintOfClass(t))
                    append(')')
                    append(hintOfClass(m.returnType))
                }
                val deobfName = back[key]
                if (deobfName == methodNameOrObf) candidates += m
            }
            if (candidates.isEmpty()) {
                for (m in cls.declaredMethods) {
                    if (m.parameterCount != arity) continue
                    val name = m.name
                    val key = buildString {
                        append(name).append('(')
                        val pts = m.parameterTypes
                        for (t in pts) append(hintOfClass(t))
                        append(')')
                        append(hintOfClass(m.returnType))
                    }
                    val deobfName = back[key]
                    if (deobfName == methodNameOrObf) candidates += m
                }
            }
        }
    }

    if (candidates.isEmpty()) return null

    var best: java.lang.reflect.Method? = null
    var bestScore = Int.MIN_VALUE
    for (m in candidates) {
        val s = scoreByHints(m.parameterTypes, hints)
        if (s > bestScore) {
            bestScore = s; best = m
        }
    }
    val chosen = best ?: candidates[0]
    val ret = chosen.returnType
    if (ret == java.lang.Void.TYPE) return null

    val obfRet = ret.name
    val deobf =
        obfToDeobfClassMap[obfRet]
            ?: obfToDeobfClassMap[obfRet.replace('$', '.')]
            ?: obfRet

    return canonicalDeobfClass(deobf)
}

private fun classToJvmAtom(c: Class<*>): String {
    if (c.isArray) {
        var t = c;
        val sb = StringBuilder()
        while (t.isArray) {
            sb.append('['); t = t.componentType
        }
        return when (t) {
            java.lang.Byte.TYPE -> sb.append('B').toString()
            java.lang.Character.TYPE -> sb.append('C').toString()
            java.lang.Double.TYPE -> sb.append('D').toString()
            java.lang.Float.TYPE -> sb.append('F').toString()
            java.lang.Integer.TYPE -> sb.append('I').toString()
            java.lang.Long.TYPE -> sb.append('J').toString()
            java.lang.Short.TYPE -> sb.append('S').toString()
            java.lang.Boolean.TYPE -> sb.append('Z').toString()
            else -> sb.append('L').append(t.name.replace('.', '/')).append(';').toString()
        }
    }
    if (c.isPrimitive) {
        return when (c) {
            java.lang.Byte.TYPE -> "B"
            java.lang.Character.TYPE -> "C"
            java.lang.Double.TYPE -> "D"
            java.lang.Float.TYPE -> "F"
            java.lang.Integer.TYPE -> "I"
            java.lang.Long.TYPE -> "J"
            java.lang.Short.TYPE -> "S"
            java.lang.Boolean.TYPE -> "Z"
            java.lang.Void.TYPE -> "V"
            else -> ""
        }
    }
    return "L" + c.name.replace('.', '/') + ";"
}

private fun methodKey(m: java.lang.reflect.Method): String {
    val pts = m.parameterTypes
    val sb = StringBuilder(m.name).append('(')
    var i = 0
    while (i < pts.size) {
        sb.append(classToJvmAtom(pts[i])); i++
    }
    sb.append(')').append(classToJvmAtom(m.returnType))
    return sb.toString()
}

private fun scoreByHints(params: Array<Class<*>>, hints: List<String>, arity: Int): Int {
    var score = if (params.size == arity) 1000 else (1000 - kotlin.math.abs(params.size - arity) * 50)
    val n = minOf(params.size, hints.size)
    var i = 0
    while (i < n) {
        val h = hints[i]
        if (h.isNotEmpty()) {
            val a = classToJvmAtom(params[i])
            if (a.contains(h)) score += 3
        }
        i++
    }
    return score
}

private fun normalizeOwnerForBackMap(obfOwner: String): String {
    if (obfToDeobfClassMap.containsKey(obfOwner)) return obfOwner
    val dot = obfOwner.replace('$', '.')
    if (obfToDeobfClassMap.containsKey(dot)) return dot
    return obfOwner
}

private fun tryLoadEither(name: String): Class<*>? {
    val cl = TestParser::class.java.classLoader
    val deobf = canonicalDeobfClass(name)
    val candidates = arrayOf(
        deobf, deobf.replace('$', '.'), deobf.replace('.', '$'),
        deobfToObfClassMap[deobf],
        deobfToObfClassMap[deobf]?.replace('$', '.'),
        deobfToObfClassMap[deobf]?.replace('.', '$')
    )
    var i = 0
    while (i < candidates.size) {
        val n = candidates[i]
        if (n != null) {
            val c = runCatching { Class.forName(n, false, cl) }.getOrNull()
            if (c != null) return c
        }
        i++
    }
    return null
}

data class ReflectPick(val obfName: String, val retDeobf: String?)

private fun reflectSelectMethod(
    ownerDeobf: String,
    desiredNameOrObf: String,
    arity: Int,
    hints: List<String>
): ReflectPick? {
    val cls = tryLoadEither(ownerDeobf) ?: run {
        mdbg { "reflect owner load FAIL: $ownerDeobf" }
        return null
    }
    mdbg { "reflect owner=${cls.name} query=$desiredNameOrObf/$arity" }

    val cands = ArrayList<java.lang.reflect.Method>()
    fun collect(ms: Array<java.lang.reflect.Method>) {
        var i = 0
        while (i < ms.size) {
            val m = ms[i]
            val name = m.name
            var accepts = false
            if (name == desiredNameOrObf) {
                accepts = true
            } else {
                val declObfOwner = normalizeOwnerForBackMap(m.declaringClass.name)
                val backMap = obfToDeobfMethodMap[declObfOwner]
                if (backMap != null) {
                    val key = methodKey(m)
                    val deobfName = backMap[key]
                    if (deobfName == desiredNameOrObf) accepts = true
                }
            }
            if (accepts) cands.add(m)
            i++
        }
    }
    collect(cls.methods)
    collect(cls.declaredMethods)

    mdbg { "reflect candidates=${cands.size} for $desiredNameOrObf (owner=${cls.name})" }
    if (cands.isEmpty()) return null

    var best: java.lang.reflect.Method? = null
    var bestScore = Int.MIN_VALUE
    var unique = true

    var i = 0
    while (i < cands.size) {
        val m = cands[i]
        val s = scoreByHints(m.parameterTypes, hints, arity)
        mdbg { "  cand ${m.declaringClass.name}.${m.name}/${m.parameterCount} score=$s sig=${methodKey(m)}" }
        if (s > bestScore) {
            best = m; bestScore = s; unique = true
        } else if (s == bestScore) unique = false
        i++
    }

    if (!unique && best != null) {
        val ownerC = cls
        i = 0
        while (i < cands.size) {
            val m = cands[i]
            val s = scoreByHints(m.parameterTypes, hints, arity)
            if (s == bestScore && m.declaringClass == ownerC) {
                best = m; break
            }
            i++
        }
    }

    val chosen = best!!
    val ret = chosen.returnType
    val retDeobf = if (ret == java.lang.Void.TYPE) null else {
        val obfRet = ret.name
        val named = obfToDeobfClassMap[obfRet]
            ?: obfToDeobfClassMap[obfRet.replace('$', '.')]
            ?: obfRet
        canonicalDeobfClass(named)
    }
    mdbg { "reflect PICK ${chosen.declaringClass.name}.${chosen.name}/${chosen.parameterCount} ret=${retDeobf ?: "void"}" }
    return ReflectPick(chosen.name, retDeobf)
}


private fun tryLoadNamed(fq: String): Class<*>? = try {
    Class.forName(fq, false, TestParser::class.java.classLoader)
} catch (_: Throwable) {
    null
}

private fun isAssignableFromNamed(superNamed: String, subNamed: String): Boolean {
    val superC = tryLoadEither(superNamed) ?: return false
    val subC = tryLoadEither(subNamed) ?: return false
    return superC.isAssignableFrom(subC)
}

private fun collectSuperChainNamed(startFq: String): Set<String> {
    val seenClasses = LinkedHashSet<Class<*>>()
    val q: ArrayDeque<Class<*>> = ArrayDeque()
    val start = tryLoadEither(startFq) ?: return setOf(startFq)

    q.add(start)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        if (!seenClasses.add(c)) continue
        c.superclass?.let { q.add(it) }
        c.interfaces?.forEach { q.add(it) }
    }

    val out = LinkedHashSet<String>()
    for (c in seenClasses) {
        val jvm = c.name
        val deobf =
            obfToDeobfClassMap[jvm]
                ?: obfToDeobfClassMap[jvm.replace('$', '.')]
                ?: jvm
        out.add(canonicalDeobfClass(deobf))
    }
    return out
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
    if (g != null) {
        val candOwner = canonicalDeobfClass(g.first)
        if (candOwner in chain) return g.second
        if (chain.any { isAssignableFromNamed(candOwner, it) }) return g.second
    }

    return null
}


private const val GUARD_PREFIX = "__KS_GUARD__"
private fun guardLine(line: String, protected: List<IntRange>): Pair<String, List<String>> {
    if (protected.isEmpty()) return line to emptyList()

    var out = line
    val saved = ArrayList<String>(protected.size)

    val ranges = protected.sortedByDescending { it.first }

    for ((idx, r) in ranges.withIndex()) {
        val start = r.first.coerceIn(0, line.length)
        val endExclusiveRaw = (r.last + 1)
        val endExclusive = endExclusiveRaw.coerceIn(start, line.length)

        if (start >= endExclusive) continue

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
            val chained = findChainedFieldRewrites(ktFile, string)

            val result = replaceObfuscated(
                string,
                foundClasses,
                foundMethods,
                foundFields,
                nested,
                overrideRenames,
                protectedByLine,
                qualifierMap,
                chained
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
                    val outer = q.referencedName ?: return
                    val outerFq = importSimple[outer] ?: outer
                    val deobfNested = "$outerFq$$inner"
                    val obf = deobfToObfClassMap[deobfNested]
                    if (obf != null) {
                        repl["$outer.$inner"] = obf.replace('$', '.')
                        repl[inner] = obf.replace('$', '.')
                    }
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

        fun findFieldUsages(ktFile: KtFile): Set<Pair<String, String>> {
            val foundFields = mutableSetOf<Pair<String, String>>()

            val importSimpleNameMap = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val ip = it.importPath?.pathStr ?: return@forEach
                importSimpleNameMap[ip.substringAfterLast('.')] = ip
            }

            fun resolveSimpleType(t: String): String = importSimpleNameMap[t] ?: t
            fun resolveExprDeobfType(expr: KtExpression, env: Map<String, String>): String? {
                return when (expr) {
                    is KtNameReferenceExpression -> {
                        val nm = expr.getReferencedName()
                        env[nm] ?: resolveSimpleType(nm).let { canonicalDeobfClass(it) }
                            .takeIf { deobfToObfClassMap.containsKey(it) || obfToDeobfClassMap.containsKey(it) }
                    }

                    is KtDotQualifiedExpression -> {
                        val ownerType = resolveExprDeobfType(expr.receiverExpression, env) ?: return null
                        val sel = expr.selectorExpression
                        when (sel) {
                            is KtSimpleNameExpression -> {
                                val fieldName = sel.getReferencedName()
                                val nextType = deobfFieldTypeMap[ownerType]?.get(fieldName)
                                nextType?.let(::canonicalDeobfClass)
                            }

                            is KtCallExpression -> {
                                null
                            }

                            else -> null
                        }
                    }

                    else -> null
                }
            }

            ktFile.accept(object : KtTreeVisitorVoid() {
                private var paramEnv: Map<String, String> = emptyMap()

                override fun visitNamedFunction(function: KtNamedFunction) {
                    val saved = paramEnv
                    paramEnv = buildMap {
                        function.valueParameters.forEach { p ->
                            val t = p.typeReference?.text ?: return@forEach
                            val fq = canonicalDeobfClass(resolveSimpleType(t))
                            val name = p.name ?: return@forEach
                            put(name, fq)
                        }
                    }
                    super.visitNamedFunction(function)
                    paramEnv = saved
                }

                override fun visitDotQualifiedExpression(expr: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expr)

                    val selector = expr.selectorExpression as? KtSimpleNameExpression ?: return
                    val fieldName = selector.getReferencedName()

                    run {
                        val qualifierText = expr.receiverExpression.text
                        val possibleOwners = buildList {
                            importSimpleNameMap[qualifierText]?.let { add(it) }
                            deobfToObfClassMap[qualifierText]?.let { add(it) }
                            if (qualifierText.startsWith("net.minecraft.class_")) add(qualifierText)
                            add(qualifierText)
                        }
                        for (owner in possibleOwners) {
                            val fieldMap = deobfToObfFieldMap[owner]
                            if (fieldMap?.containsKey(fieldName) == true) {
                                foundFields += owner to fieldName
                                return
                            }
                        }
                    }

                    val ownerType = resolveExprDeobfType(expr.receiverExpression, paramEnv) ?: return
                    val fieldMap = deobfToObfFieldMap[ownerType]
                    if (fieldMap?.containsKey(fieldName) == true) {
                        foundFields += ownerType to fieldName
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
            importQualifierMap: Map<String, String>,
            chainedFieldRewritesByLine: Map<Int, List<Pair<String, String>>>
        ): String {
            val lines = source.lines().toMutableList()
            for (i in lines.indices) {
                val prot = protectedByLine[i] ?: emptyList()
                var line = lines[i]
                chainedFieldRewritesByLine[i]
                    ?.sortedByDescending { it.first.length }
                    ?.forEach { (from, to) ->
                        dbg { "apply line[$i]: '$from' -> '$to'  (before='${line.trim()}')" }
                        line = safeReplaceLine(line, prot, Regex("""\b${Regex.escape(from)}\b"""), to)
                        dbg { "apply line[$i]: after='${line.trim()}'" }
                    }
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
                val ambiguousMethodNames: Set<String> = foundMethods
                    .groupBy { it.method }
                    .filter { entry -> entry.value.map { it.className }.distinct().size > 1 }
                    .keys
                foundMethods.forEach { (className, deobfMethod, arity, hints) ->
                    mdbg { "rename try owner=$className meth=$deobfMethod/$arity hints=$hints" }

                    val pick = strongPickOnOwner(className, deobfMethod, arity, hints)
                    val obfMethod = pick.obfName

                    if (!obfMethod.isNullOrEmpty()) {
                        val obfClass0 = deobfToObfClassMap[className] ?: className
                        val obfClass = obfClass0.replace('$', '.')
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

                        if (deobfMethod !in ambiguousMethodNames) {
                            line = safeReplaceLine(
                                line, emptyList(),
                                Regex("""\b${Regex.escape(deobfMethod)}\b"""),
                                obfMethod
                            )
                        }
                        mdbg { "  DONE rename $className.$deobfMethod/$arity -> $obfMethod" }
                    } else {
                        mdbg { "  FAIL rename $className.$deobfMethod/$arity (no owner-scoped pick)" }
                    }
                }



                line = restoreLine(line, saved)
                lines[i] = line
            }

            return lines.joinToString("\n").replace("net.field_5645.", "net.minecraft.")
        }

    }
}
