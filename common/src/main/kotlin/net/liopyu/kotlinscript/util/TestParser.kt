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
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import java.io.File
import java.io.InputStreamReader

val instanceDir = File(System.getProperty("user.dir"))
val sourcesDir = File(instanceDir, "kotlinsources")
fun some() {
    val tree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings()
    val namespaces = listOf(tree.srcNamespace) + tree.dstNamespaces
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

        if (names.size > 1 && names[1] != null && names[1]!!.contains("Minecraft")) {
            val intermediaryClass = names[0]?.replace('/', '.')
            val namedClass = names[1]?.replace('/', '.')
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
        "deobfFieldTypeMap" to deobfFieldTypeMap,
        "obfFieldDescriptorMap" to obfFieldDescriptorMap
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

    obfFieldDescriptorMap.clear()
    parseMap<Map<String, Map<String, String>>>("obfFieldDescriptorMap", methodMapType)
        .forEach { (k, v) -> obfFieldDescriptorMap[k] = v.toMutableMap() }


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
    return mappingFile
}


fun buildMappingsIfNeeded(tree: MappingTree, obfNamespace: Int, deobfNamespace: Int) {
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

fun isDevEnvironment(): Boolean {
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

private fun compatible(paramPieces: List<String>, argKs: List<Class<*>?>): Boolean {
    for ((i, pp) in paramPieces.withIndex()) {
        val ac = argKs.getOrNull(i) ?: return false
        val pc = jvmPieceToClass(pp) ?: return false

        if (pc.isPrimitive) {
            val boxed = when (pc) {
                java.lang.Byte.TYPE -> java.lang.Byte::class.java
                java.lang.Character.TYPE -> java.lang.Character::class.java
                java.lang.Double.TYPE -> java.lang.Double::class.java
                java.lang.Float.TYPE -> java.lang.Float::class.java
                java.lang.Integer.TYPE -> java.lang.Integer::class.java
                java.lang.Long.TYPE -> java.lang.Long::class.java
                java.lang.Short.TYPE -> java.lang.Short::class.java
                java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
                else -> pc
            }
            if (ac == pc || ac == boxed) continue
            return false
        } else {
            if (pc.isAssignableFrom(ac)) continue
            return false
        }
    }
    return true
}

private fun deobfMethodReturnType(
    ownerDeobfInput: String,
    methodName: String,
    arity: Int,
    hints: List<String>
): String? {
    val ownerDeobf = normalizeOwnerForLookup(ownerDeobfInput)
    val list = deobfMethodOverloads[ownerDeobf]?.get(methodName) ?: run {
        println("[KS-ret] deobfMethodReturnType: no overloads for owner='$ownerDeobf' meth='$methodName'")
        return null
    }

    var bestScore = -1
    var bestRetJvm: String? = null

    println(
        "[KS-ret] deobf pick owner='$ownerDeobf' meth='$methodName' arity=$arity hints(raw)=$hints hints(jvm)=${
            hints.map {
                toJvmHint(
                    it
                )
            }
        }"
    )

    for ((desc, _) in list) {
        val ps = paramTypes(desc)
        if (ps.size != arity) continue

        var score = 0
        val upto = minOf(hints.size, ps.size)
        for (i in 0 until upto) {
            val hJvm = toJvmHint(hints[i])
            if (hJvm.isNotEmpty()) {
                val pBare = bareJvm(ps[i])
                val hBare = bareJvm(hJvm)
                if (pBare == hBare) score += 12
                else if (pBare.contains(hBare)) score += 3
            }
        }
        println("[KS-ret]   cand desc='$desc' ps=$ps score=$score")

        if (score > bestScore) {
            bestScore = score
            bestRetJvm = jvmMethodReturnToClassName(desc)
        }
    }

    val mapped = bestRetJvm?.let(::canonicalDeobfClass)
    val preferred = preferOwnerInner(ownerDeobf, mapped)
    println("[KS-ret] -> bestScore=$bestScore retJvm='$bestRetJvm' mapped='$mapped' preferred='$preferred'")
    return preferred
}

private fun obfMethodReturnType(
    ownerDeobfInput: String,
    obfMethod: String,
    arity: Int,
    hints: List<String>
): String? {
    val ownerDeobf = normalizeOwnerForLookup(ownerDeobfInput)
    val obfOwner = deobfToObfClassMap[ownerDeobf] ?: run {
        println("[KS-ret] obfMethodReturnType: no obf owner for '$ownerDeobf'")
        return null
    }
    val table = obfToDeobfMethodMap[obfOwner] ?: run {
        println("[KS-ret] obfMethodReturnType: no table for obfOwner='$obfOwner'")
        return null
    }

    var bestScore = -1
    var bestRetDeobf: String? = null

    println(
        "[KS-ret] obf pick owner='$ownerDeobf'($obfOwner) meth='$obfMethod' arity=$arity hints(raw)=$hints hints(jvm)=${
            hints.map {
                toJvmHint(
                    it
                )
            }
        }"
    )

    for ((key, _) in table) {
        if (!key.startsWith("$obfMethod(")) continue
        val desc = key.substring(obfMethod.length)
        if (countParams(desc) != arity) continue

        val ps = paramTypes(desc)
        var score = 0
        val upto = minOf(hints.size, ps.size)
        for (i in 0 until upto) {
            val hJvm = toJvmHint(hints[i])
            if (hJvm.isNotEmpty()) {
                val pBare = bareJvm(ps[i])
                val hBare = bareJvm(hJvm)
                if (pBare == hBare) score += 12
                else if (pBare.contains(hBare)) score += 3
            }
        }
        println("[KS-ret]   cand key='$key' desc='$desc' ps=$ps score=$score")

        if (score >= bestScore) {
            bestScore = score
            val retObf = jvmMethodReturnToClassName(desc)
            val namedDeobf = when (retObf) {
                null -> null
                else -> {
                    val d = obfToDeobfClassMap[retObf]
                        ?: obfToDeobfClassMap[retObf.replace('$', '.')]
                        ?: retObf
                    canonicalDeobfClass(d)
                }
            }
            bestRetDeobf = namedDeobf
        }
    }

    val preferred = preferOwnerInner(ownerDeobf, bestRetDeobf)
    println("[KS-ret] -> bestScore=$bestScore retDeobf='$bestRetDeobf' preferred='$preferred'")
    return preferred
}

fun hintFromTypeFq(typeFq: String?): String {
    if (typeFq == null) return ""
    val simple = typeFq.substringAfterLast('.')
    return when (simple) {
        "boolean" -> "Z"
        "byte" -> "B"
        "short" -> "S"
        "char" -> "C"
        "int", "Integer" -> "I"
        "long", "Long" -> "J"
        "float", "Float" -> "F"
        "double", "Double" -> "D"
        else -> canonicalDeobfClass(typeFq).replace('.', '/')
    }
}

fun hintForArgExpr(expr: KtExpression?, env: TypeEnv, imports: Map<String, String>): String {
    if (expr == null) return ""
    val resolved = resolveExprType(expr, env, imports)
    if (resolved != null) {
        val h = hintFromTypeFq(resolved)
        println("[KS-type] arg='${expr.text}' kind=resolved type=$resolved hint=$h")
        return h
    }
    when (expr) {
        is KtStringTemplateExpression -> {
            println("[KS-type] arg='${expr.text}' kind=StringLiteral hint=java/lang/String")
            return "java/lang/String"
        }

        is KtConstantExpression -> {
            val s = expr.text.lowercase()
            val k = when {
                s == "true" || s == "false" -> "Z"
                s.endsWith("f") -> "F"
                s.contains('.') -> "D"
                else -> "I"
            }
            println("[KS-type] arg='${expr.text}' kind=NumberLiteral hint=$k")
            return k
        }
    }
    println("[KS-type] arg='${expr.text}' kind=unknown hint=")
    return ""
}

private fun argHints(
    call: KtCallExpression,
    imports: Map<String, String>,
    env: TypeEnv
): List<String> {


    fun numericHint(textRaw: String): String {
        val s = textRaw.trim().lowercase()
        return when {
            s.endsWith("f") -> "F"
            s.contains('.') -> "D"
            s == "true" || s == "false" -> "Z"
            else -> "I"
        }
    }

    fun typeOf(e: KtExpression?): String? {
        if (e == null) return null

        resolveExprType(e, env, imports)?.let { return canonicalDeobfClass(it) }

        return when (e) {
            is KtStringTemplateExpression -> "java.lang.String"
            is KtConstantExpression -> when (numericHint(e.text)) {
                "F" -> "java.lang.Float"
                "D" -> "java.lang.Double"
                "I" -> "java.lang.Integer"
                "Z" -> "java.lang.Boolean"
                else -> null
            }

            is KtDotQualifiedExpression -> typeOf(e.selectorExpression)

            is KtCallExpression -> resolveCtorType(e, imports)?.let(::canonicalDeobfClass)

            is KtNameReferenceExpression -> {
                val rn = e.getReferencedName()
                canonicalDeobfClass(env[rn] ?: resolveTypeRef(rn, imports))
            }

            else -> null
        }
    }

    return call.valueArguments.map { arg ->
        hintFromTypeFq(typeOf(arg.getArgumentExpression()))
    }
}

private fun argHints(
    call: KtCallExpression,
    imports: Map<String, String>,
    env: Map<String, String>
): List<String> {

    fun hintFromType(deobfFq: String?): String =
        if (deobfFq.isNullOrBlank()) "" else toJvmHint(deobfFq.substringAfterLast('.'))

    fun numericHint(textRaw: String): String {
        val s = textRaw.trim().lowercase()
        return when {
            s.endsWith("f") -> "F"
            s.contains('.') -> "D"
            s == "true" || s == "false" -> "Z"
            else -> "I"
        }
    }

    fun resolveExprTypeForHint(e: KtExpression?): String? {
        if (e == null) return null
        return when (e) {
            is KtStringTemplateExpression -> "java.lang.String"
            is KtConstantExpression -> return when (numericHint(e.text)) {
                "F" -> "java.lang.Float"
                "D" -> "java.lang.Double"
                "I" -> "java.lang.Integer"
                "Z" -> "java.lang.Boolean"
                else -> null
            }

            is KtNameReferenceExpression -> {
                val rn = e.getReferencedName()
                canonicalDeobfClass(env[rn] ?: resolveTypeRef(rn, imports))
            }

            is KtCallExpression -> resolveCtorType(e, imports)?.let(::canonicalDeobfClass)

            is KtDotQualifiedExpression -> {
                val ownerT = resolveExprTypeForHint(e.receiverExpression)
                val sel = e.selectorExpression
                if (ownerT == null || sel == null) return null

                when (sel) {
                    is KtCallExpression -> {
                        val m = (sel.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                        val childHints = argHints(sel, imports, env)
                        if (m != null) {
                            val strong = strongPickOnOwner(ownerT, m, childHints.size, childHints)
                            val mapPick = mappingPickOnOwner(ownerT, m, childHints.size, childHints)
                            val selPick = reflectSelectMethod(ownerT, m, childHints.size, childHints)
                            val reflectRet = reflectMethodReturnType(ownerT, m, childHints.size, childHints)

                            println(
                                "[KS-type]   HINT call '$m' owner=$ownerT arity=${childHints.size} childHints=$childHints " +
                                        "-> strong.obf=${strong.obfName} strong.ret=${strong.retDeobf} " +
                                        "map.ret=${mapPick?.retDeobf} sel.obf=${selPick?.obfName} sel.ret=${selPick?.retDeobf} " +
                                        "reflect.ret=$reflectRet"
                            )

                            val ret = strong.retDeobf
                                ?: mapPick?.retDeobf
                                ?: selPick?.retDeobf
                                ?: reflectRet

                            preferOwnerInner(ownerT, ret)?.let(::canonicalDeobfClass)
                        } else null
                    }

                    is KtSimpleNameExpression -> {
                        val field = sel.getReferencedName()
                        val next = nextTypeAfterField(ownerT, field)
                            ?: reflectDeobfFieldType(ownerT, field)
                        next?.let(::canonicalDeobfClass)
                    }

                    else -> null
                }
            }

            else -> null
        }
    }

    return call.valueArguments.map { arg ->
        hintFromType(resolveExprTypeForHint(arg.getArgumentExpression()))
    }
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
            if (expr.parent is KtUserType || expr.parent?.parent is KtUserType) return
            val whole = canonicalDeobfClass(expr.text)
            if (deobfToObfClassMap.containsKey(whole) || obfToDeobfClassMap.containsKey(whole)) return

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
                        val hints = argHints(call, importSimple, varTypes)

                        val arity = hints.size

                        val pick = strongPickOnOwner(curDeobfType, mn, arity, hints)
                        outParts += call.text

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
                    if (next == null) break
                    curDeobfType = canonicalDeobfClass(next)
                    i++
                    continue
                }

                break
            }

            if (changed) {
                val from = expr.text
                val to = outParts.joinToString(".")
                val lineIdx = lineOf(expr.textRange.startOffset)
                val list = rewritesByLine.getOrPut(lineIdx) { mutableListOf() }
                list.add(from to to)
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
                val canon = normalizeOwnerForLookup(t, importSimple)
                vars[p.name ?: return@forEach] = canon
            }
        }

        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)

            fun putVar(name: String?, typeFq: String?) {
                if (name == null || typeFq == null) return
                vars[name] = canonicalDeobfClass(typeFq)
            }

            property.typeReference?.text?.let { t ->
                putVar(property.name, fq(t))
                return
            }

            val init = property.initializer ?: return

            when (init) {
                is KtCallExpression -> {
                    val callee = init.calleeExpression
                    if (callee is KtNameReferenceExpression || callee is KtDotQualifiedExpression) {
                        resolveCtorType(init, importSimple)?.let { putVar(property.name, it) }
                    } else {
                        val dot = init.parent as? KtDotQualifiedExpression
                        if (dot != null) {
                            val ownerExpr = dot.receiverExpression
                            val ownerDeobf: String? = when (ownerExpr) {
                                is KtNameReferenceExpression -> {
                                    val rn = ownerExpr.getReferencedName()
                                    vars[rn] ?: fq(rn)
                                }

                                is KtDotQualifiedExpression -> canonicalDeobfClass(ownerExpr.text)
                                is KtCallExpression -> {
                                    val c = ownerExpr.calleeExpression
                                    if (c is KtNameReferenceExpression || c is KtDotQualifiedExpression) {
                                        resolveCtorType(ownerExpr, importSimple)
                                    } else {
                                        null
                                    }
                                }

                                else -> null
                            }

                            val mn = (init.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                            val hints = argHints(init, importSimple, vars)

                            val arity = hints.size

                            val ret = if (ownerDeobf != null && mn != null)
                                strongPickOnOwner(ownerDeobf, mn, arity, hints).retDeobf
                            else null

                            putVar(property.name, ret)
                        }
                    }
                }

                is KtNameReferenceExpression -> {
                    val rn = init.getReferencedName()
                    putVar(property.name, vars[rn] ?: fq(rn))
                }

                is KtDotQualifiedExpression -> {
                    val sel = init.selectorExpression as? KtSimpleNameExpression
                    val fieldName = sel?.getReferencedName()
                    val ownerExpr = init.receiverExpression
                    val ownerDeobf = when (ownerExpr) {
                        is KtNameReferenceExpression -> {
                            val rn = ownerExpr.getReferencedName()
                            vars[rn] ?: fq(rn)
                        }

                        is KtDotQualifiedExpression -> canonicalDeobfClass(ownerExpr.text)
                        else -> null
                    }
                    val next = if (ownerDeobf != null && fieldName != null)
                        nextTypeAfterField(ownerDeobf, fieldName) else null
                    putVar(property.name, next)
                }
            }
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
val obfFieldDescriptorMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

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

data class TextEdit(val start: Int, val end: Int, val newText: String)

private fun applyTextEdits(src: String, edits: List<TextEdit>): String {
    if (edits.isEmpty()) return src
    val sb = StringBuilder(src)
    for (e in edits.sortedByDescending { it.start }) sb.replace(e.start, e.end, e.newText)
    return sb.toString()
}

private fun lambdaProducesValue(lambda: KtLambdaExpression): Boolean {
    val body = lambda.bodyExpression ?: return false
    if (body.anyDescendantOfType<KtReturnExpression>()) return true
    val last = body.statements.lastOrNull() ?: return false
    return when (last) {
        is KtTryExpression -> false
        is KtBinaryExpression -> true
        is KtCallExpression, is KtNameReferenceExpression, is KtQualifiedExpression -> true
        else -> last.text.isNotBlank()
    }
}

fun fixVoidLambdaFunctionTypeReturns(project: Project, source: String): String {
    val psiFileFactory = PsiFileFactory.getInstance(project)
    val ktFile = psiFileFactory.createFileFromText("after.kts", KotlinLanguage.INSTANCE, source) as KtFile
    val edits = mutableListOf<TextEdit>()

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)
            val ftype = property.typeReference?.typeElement as? KtFunctionType ?: return
            val lambda = property.initializer as? KtLambdaExpression ?: return
            if (lambdaProducesValue(lambda)) return

            val retRef = ftype.returnTypeReference
            if (retRef != null && retRef.text != "Unit" && retRef.text != "kotlin.Unit") {
                edits += TextEdit(retRef.textRange.startOffset, retRef.textRange.endOffset, "Unit")
            }
        }
    })

    return applyTextEdits(source, edits)
}

private fun lambdaHasValueResult(lambda: KtLambdaExpression): Boolean {
    val body = lambda.bodyExpression ?: return false
    val last = body.statements.lastOrNull() ?: return false
    return last !is KtReturnExpression &&
            last !is KtDeclaration &&
            last.text.isNotBlank() &&
            (last !is KtCallExpression || lambda.valueParameters.isNotEmpty())
}

fun normalizeLambdaFunctionTypeReturns(ktFile: KtFile, source: String): String {
    val edits = mutableListOf<TextEdit>()

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)

            val typeRef = property.typeReference?.typeElement as? KtFunctionType ?: return
            val lambda = property.initializer as? KtLambdaExpression ?: return

            if (!lambdaHasValueResult(lambda)) {
                val retRef = typeRef.returnTypeReference ?: return
                if (retRef.text != "Unit" && retRef.text != "kotlin.Unit") {
                    edits += TextEdit(
                        start = retRef.textRange.startOffset,
                        end = retRef.textRange.endOffset,
                        newText = "Unit"
                    )
                }
            }
        }
    })

    return applyTextEdits(source, edits)
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

private fun classClosureDeobf(ownerDeobf: String): Set<String> {
    val root = tryLoadEither(ownerDeobf) ?: return setOf(canonicalDeobfClass(ownerDeobf))
    val seen = LinkedHashSet<Class<*>>()
    val q = ArrayDeque<Class<*>>()
    q.add(root)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        if (!seen.add(c)) continue
        c.superclass?.let(q::add)
        c.interfaces?.forEach(q::add)
    }
    val out = LinkedHashSet<String>()
    for (c in seen) {
        val jvm = c.name
        val deobf = obfToDeobfClassMap[jvm]
            ?: obfToDeobfClassMap[jvm.replace('$', '.')]
            ?: jvm
        out += canonicalDeobfClass(deobf)
    }
    return out
}

fun debugCheckDimensionType() {
    val owner = "net.minecraft.client.multiplayer.ClientLevel"
    val method = "dimensionType"
    val owners = buildList {
        add(owner)
        addAll(classClosureDeobf(owner))
    }
}

private val simpleNameIndex: Map<String, List<String>> by lazy {
    deobfToObfClassMap.keys.groupBy { it.substringAfterLast('.').substringAfterLast('$') }
}

private fun guessFqFromSimple(simple: String, debug: Boolean = true): String? {
    val hits = simpleNameIndex[simple] ?: return null
    if (hits.size == 1) return hits.first()
    if (debug) println("[KS-own] ambiguous simple '$simple' -> candidates=$hits")
    return null
}

private fun normalizeOwnerForLookup(
    ownerDeobf: String,
    imports: Map<String, String>? = null
): String {
    var o = ownerDeobf
    if ('.' !in o) {
        val byImport = imports?.get(o)
        if (byImport != null) {
            println("[KS-own] import maps '$o' -> '$byImport'")
            o = byImport
        } else {
            val guess = guessFqFromSimple(o)
            if (guess != null) {
                println("[KS-own] guess simple '$o' -> '$guess'")
                o = guess
            } else {
                println("[KS-own] keep simple '$o' (ambiguous/no import)")
            }
        }
    }
    val canon = canonicalDeobfClass(o)
    if (canon != o) println("[KS-own] canonical '$o' -> '$canon'")
    return canon
}

private fun normalizeOwnerForLookup(
    ownerDeobf: String,
    imports: Map<String, String>?,
    contextMethod: String? = null
): String {
    if (deobfToObfClassMap.containsKey(ownerDeobf) ||
        deobfToObfClassMap.containsKey(ownerDeobf.replace('$', '.'))
    ) {
        return ownerDeobf
    }

    if ('.' in ownerDeobf || '$' in ownerDeobf) {
        val canon = canonicalDeobfClassPreserveInner(ownerDeobf)
        if (deobfToObfClassMap.containsKey(canon) ||
            deobfToObfClassMap.containsKey(canon.replace('$', '.'))
        ) {
            return canon
        }
    }

    imports?.get(ownerDeobf)?.let { imp ->
        println("[KS-own] import maps '$ownerDeobf' -> '$imp'")
        return canonicalDeobfClassPreserveInner(imp)
    }

    val cands = simpleNameIndex[ownerDeobf].orEmpty()
    if (cands.isNotEmpty()) {
        var filtered = cands
        if (!contextMethod.isNullOrEmpty()) {
            val withMethod = filtered.filter { deobfMethodOverloads[it]?.containsKey(contextMethod) == true }
            if (withMethod.size == 1) {
                val hit = withMethod.first()
                println("[KS-own] method '$contextMethod' disambiguates '$ownerDeobf' -> '$hit'")
                return canonicalDeobfClassPreserveInner(hit)
            }
            if (withMethod.isNotEmpty()) filtered = withMethod
        }
        val withPkg = filtered.filter { '.' in it }
        if (withPkg.size == 1) {
            val hit = withPkg.first()
            println("[KS-own] pkg tie-break '$ownerDeobf' -> '$hit'")
            return canonicalDeobfClassPreserveInner(hit)
        }
        println("[KS-own] ambiguous simple '$ownerDeobf' -> candidates=$filtered (keeping simple)")
    }

    return ownerDeobf
}


/**
 * Mapping-based owner/method resolver that also searches supertypes/interfaces.
 * Returns the obf method name to use, plus the *deobf* return type (if known).
 *
 * Requires:
 *  - deobfMethodOverloads: Map<OwnerDeobf, Map<MethodNameDeobf, Map<DeobfDesc, ObfName>>>
 *  - deobfToObfClassMap, obfToDeobfMethodMap
 *  - helpers: isObfMethodName, paramCount, paramTypes, deobfRetFromDeobfDesc, deobfRetFromObfDesc
 *  - canonicalDeobfClass(...)
 */
private fun mappingPickOnOwner(
    ownerDeobf: String,
    nameOrObf: String,
    arity: Int,
    hints: List<String>
): MappingPick? {
    val canonOwner = canonicalDeobfClass(ownerDeobf)
    val owners: List<String> = buildList {
        add(canonOwner)
        addAll(classClosureDeobf(canonOwner))
    }.distinct()

    fun scoreParams(ps: List<String>, hints: List<String>, arity: Int): Int {
        var s = if (ps.size == arity) 1000 else 1000 - kotlin.math.abs(ps.size - arity) * 50
        val n = minOf(ps.size, hints.size)
        var i = 0
        while (i < n) {
            val h = toJvmHint(hints[i])
            if (h.isNotEmpty() && ps[i].contains(h)) s += 3
            i++
        }
        return s
    }


    var bestObf: String? = null
    var bestRet: String? = null
    var bestScore = Int.MIN_VALUE
    var bestWhere: String? = null
    var bestVia: String? = null

    if (!isObfMethodName(nameOrObf)) {
        for (own in owners) {
            val byName = deobfMethodOverloads[own]?.get(nameOrObf) ?: continue
            for ((deobfDesc, obfName) in byName) {
                val ps = paramTypes(deobfDesc)
                val s = scoreParams(ps, hints, arity)
                if (s > bestScore) {
                    bestScore = s
                    bestObf = obfName
                    bestRet = deobfRetFromDeobfDesc(deobfDesc)
                    bestWhere = own
                    bestVia = "overloads"
                }
            }
        }

        if (bestObf == null) {
            for (own in owners) {
                val obfOwner = deobfToObfClassMap[own] ?: continue
                val table = obfToDeobfMethodMap[obfOwner] ?: obfToDeobfMethodMap[obfOwner.replace('$', '.')]
                if (table == null) continue

                for ((key, deobfName) in table) {
                    if (deobfName != nameOrObf) continue
                    val open = key.indexOf('(')
                    if (open <= 0) continue
                    val desc = key.substring(open)
                    val ps = paramTypes(desc)
                    val s = scoreParams(ps, hints, arity)
                    if (s > bestScore) {
                        bestScore = s
                        bestObf = key.substring(0, open)
                        bestRet = deobfRetFromObfDesc(desc)
                        bestWhere = own
                        bestVia = "backmap"
                    }
                }
            }
        }

        if (bestObf != null) {
            return MappingPick(bestObf!!, bestRet)
        }

        return null
    }

    for (own in owners) {
        val obfOwner0 = deobfToObfClassMap[own] ?: continue
        val candidates = listOf(obfOwner0, obfOwner0.replace('$', '.'))
        var table: Map<String, String>? = null
        for (cand in candidates) {
            table = obfToDeobfMethodMap[cand]
            if (table != null) break
        }
        if (table == null) continue

        for ((key, _) in table) {
            if (!key.startsWith("$nameOrObf(")) continue
            val desc = key.substring(nameOrObf.length)
            val ps = paramTypes(desc)
            val s = scoreParams(ps, hints, arity)
            if (s > bestScore) {
                bestScore = s
                bestObf = nameOrObf
                bestRet = deobfRetFromObfDesc(desc)
                bestWhere = own
                bestVia = "obf+backmap"
            }
        }
    }

    if (bestObf != null) {
        return MappingPick(bestObf!!, bestRet)
    }

    return null
}

private fun fullUserTypeName(u: KtUserType): String? {
    val segs = ArrayDeque<String>()
    var cur: KtUserType? = u
    while (cur != null) {
        val name = cur.referencedName ?: return null
        segs.addFirst(name)
        cur = cur.qualifier
    }
    return segs.joinToString(".")
}

fun findQualifiedClassUsages(ktFile: KtFile): Map<String, String> {
    val repl = mutableMapOf<String, String>()
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitUserType(type: KtUserType) {
            super.visitUserType(type)
            val fq = fullUserTypeName(type) ?: return
            val obf = deobfToObfClassMap[fq] ?: return
            repl[fq] = obf.replace('$', '.')
        }
    })
    return repl
}


private fun canonicalDeobfClass(name: String): String {
    if (deobfToObfClassMap.containsKey(name)) return name

    val dotted = name.replace('$', '.')
    val dollar = name.replace('.', '$')
    if (deobfToObfClassMap.containsKey(dotted)) return dotted
    if (deobfToObfClassMap.containsKey(dollar)) return dollar

    obfToDeobfClassMap[name]?.let { return it }
    obfToDeobfClassMap[dotted]?.let { return it }
    obfToDeobfClassMap[dollar]?.let { return it }

    val hasQualifier = name.contains('.') || name.contains('$')
    if (!hasQualifier) {
        val simple = name
        obfToDeobfClassMap[simple]?.let { return it }
        if (deobfToObfClassMap.containsKey(simple)) return simple
    }

    return name
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
                if (h.isNotEmpty()) {
                    if (ps[i] == h) score += 12
                    else if (ps[i].contains(h)) score += 3
                }
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
                toJvmHint(fq)
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
class TypeEnv {
    private val stack = ArrayDeque<MutableMap<String, String>>()
        .apply { addLast(mutableMapOf()) }

    fun push() {
        stack.addLast(mutableMapOf())
    }

    fun pop() {
        require(stack.size > 1) { "TypeEnv: cannot pop the root scope" }
        stack.removeLast()
    }

    operator fun get(name: String): String? =
        stack.asReversed().firstNotNullOfOrNull { it[name] }

    operator fun set(name: String, deobfFqcn: String) {
        stack.last()[name] = deobfFqcn
    }

    fun put(name: String, deobfFqcn: String) {
        this[name] = deobfFqcn
    }

    fun assign(name: String, deobfFqcn: String) {
        for (frame in stack.asReversed()) {
            if (name in frame) {
                frame[name] = deobfFqcn; return
            }
        }
        stack.last()[name] = deobfFqcn
    }

    inline fun <T> withScope(block: () -> T): T {
        push()
        return try {
            block()
        } finally {
            pop()
        }
    }

    fun lookup(name: String): String? = this[name]
}

private fun descriptorToDeobfType(desc: String): String? {
    fun prim(t: Char) = when (t) {
        'V' -> "void"
        'Z' -> "boolean"
        'B' -> "byte"
        'S' -> "short"
        'C' -> "char"
        'I' -> "int"
        'J' -> "long"
        'F' -> "float"
        'D' -> "double"
        else -> null
    }

    var i = 0
    var arrayDepth = 0
    while (i < desc.length && desc[i] == '[') {
        arrayDepth++; i++
    }
    val base = when (desc[i]) {
        'L' -> {
            val semi = desc.indexOf(';', i)
            if (semi < 0) return null
            val internal = desc.substring(i + 1, semi)
            val deobf = obfToDeobfClassMap[internal] ?: internal.replace('/', '.')
            i = semi + 1
            deobf
        }

        else -> {
            val p = prim(desc[i]) ?: return null
            i++
            p
        }
    }
    return if (arrayDepth == 0) base else Array(arrayDepth) { "[]" }.joinToString("", prefix = base)
}

private fun nextTypeAfterField(ownerDeobf: String, deobfField: String): String? {
    deobfFieldTypeMap[ownerDeobf]?.get(deobfField)?.let { return it }
    val ownerCanon = canonicalDeobfClass(ownerDeobf)
    deobfFieldTypeMap[ownerCanon]?.get(deobfField)?.let { return it }

    val obfOwner = deobfToObfClassMap[ownerCanon]
    val obfField = deobfToObfFieldMap[ownerCanon]?.get(deobfField)
    if (obfOwner != null && obfField != null) {
        val desc = obfFieldDescriptorMap[obfOwner]?.get(obfField)
        if (!desc.isNullOrBlank()) {
            val obfTypeDot = descToNamedTypeOrNull(desc)
            if (!obfTypeDot.isNullOrBlank()) {
                val namedType =
                    obfToDeobfClassMap[obfTypeDot]
                        ?: obfToDeobfClassMap[obfTypeDot.replace('$', '.')]
                        ?: obfToDeobfClassMap[obfTypeDot.replace('.', '$')]
                        ?: obfTypeDot
                val canon = canonicalDeobfClass(namedType)
                deobfFieldTypeMap.getOrPut(ownerCanon) { mutableMapOf() }[deobfField] = canon
                return canon
            }
        }
    }

    return reflectDeobfFieldType(ownerCanon, deobfField)
}

private fun recordMethodUse(
    ownerDeobf: String,
    methodName: String,
    arity: Int,
    hints: List<String>,
    found: MutableList<MethodUse>
) {
    val owners = buildList {
        val canon = canonicalDeobfClass(ownerDeobf)
        add(canon)
        addAll(classClosureDeobf(canon))
    }.distinct()

    val hasName = owners.any { deobfMethodOverloads[it]?.containsKey(methodName) == true }
    val hasArity = owners.any {
        deobfToObfMethodByArity[it]?.get(methodName)?.isNotEmpty() == true
    }


    if (hasName || hasArity) {
        found += MethodUse(ownerDeobf, methodName, arity, hints)
        return
    }

    val pick = mappingPickOnOwner(ownerDeobf, methodName, arity, hints)
        ?: run {
            val ret = reflectMethodReturnType(ownerDeobf, methodName, arity, hints)
            if (ret != null) MappingPick(methodName, ret) else null
        }

    if (pick != null) {
        found += MethodUse(ownerDeobf, methodName, arity, hints)
    }
}

private fun argHead(expr: KtExpression?): String? = when (expr) {
    is KtStringTemplateExpression -> "kotlin.String"
    is KtNameReferenceExpression -> expr.getReferencedName()
    is KtCallExpression -> {
        val callee = expr.calleeExpression
        when (callee) {
            is KtNameReferenceExpression -> callee.getReferencedName()
            is KtDotQualifiedExpression -> argHead(callee.receiverExpression)
            else -> null
        }
    }

    is KtDotQualifiedExpression -> argHead(expr.receiverExpression)
    else -> null
}

fun resolveExprType(
    expr: KtExpression,
    env: TypeEnv,
    imports: Map<String, String>
): String? {
    fun canonical(s: String) = canonicalDeobfClass(s)

    fun numericClassOfLiteral(text: String): String {
        val s = text.lowercase()
        val isHexOrBin = s.startsWith("0x") || s.startsWith("0b")
        return when {
            s.endsWith("f") -> "F"
            !isHexOrBin && s.contains('.') -> "D"
            else -> "I"
        }
    }

    fun hintFromType(deobfFq: String?): String =
        if (deobfFq == null) "" else toJvmHint(deobfFq.substringAfterLast('.'))


    fun hintForArg(a: KtExpression?): String {
        if (a == null) return ""
        val resolved = resolveExprType(a, env, imports)
        if (resolved != null) {
            val h = hintFromType(resolved)
            println("[KS-type] arg='${a.text}' kind=resolved type=$resolved hint=$h")
            return h
        }
        when (a) {
            is KtBinaryExpression -> {
                val opName = (a.operationReference as? KtSimpleNameExpression)
                    ?.getReferencedName() ?: a.operationReference.text
                if (opName in setOf("shl", "shr", "ushr", "and", "or", "xor")) {
                    println("[KS-type] arg='${a.text}' kind=bitwise op=$opName hint=I")
                    return "I"
                }
            }

            is KtStringTemplateExpression -> {
                println("[KS-type] arg='${a.text}' kind=StringLiteral hint=java/lang/String")
                return "java/lang/String"
            }

            is KtConstantExpression -> {
                val k = numericClassOfLiteral(a.text)
                println("[KS-type] arg='${a.text}' kind=NumberLiteral hint=$k")
                return k
            }
        }
        println("[KS-type] arg='${a.text}' kind=unknown hint=")
        return ""
    }

    return when (expr) {
        is KtBinaryExpression -> {
            val opName = (expr.operationReference as? KtSimpleNameExpression)
                ?.getReferencedName() ?: expr.operationReference.text

            if (opName in setOf("shl", "shr", "ushr", "and", "or", "xor")) {
                println("[KS-type] bitwise '${expr.text}' op=$opName -> kotlin.Int")
                "kotlin.Int"
            } else {
                val lt = resolveExprType(expr.left ?: return null, env, imports)
                val rt = resolveExprType(expr.right ?: return null, env, imports)
                val t = when {
                    lt == "kotlin.Double" || rt == "kotlin.Double" -> "kotlin.Double"
                    lt == "kotlin.Float" || rt == "kotlin.Float" -> "kotlin.Float"
                    else -> "kotlin.Int"
                }
                println("[KS-type] binary '${expr.text}' op=$opName -> $t")
                t
            }
        }

        is KtThisExpression -> {
            val t = env["this"]?.let(::canonical)
            println("[KS-type] this -> $t")
            t
        }

        is KtStringTemplateExpression -> {
            println("[KS-type] String literal '${expr.text}' -> java.lang.String")
            "java.lang.String"
        }

        is KtConstantExpression -> {
            val k = numericClassOfLiteral(expr.text)
            println("[KS-type] number literal '${expr.text}' -> <primitive $k> (returning null type, using hint)")
            null
        }

        is KtNameReferenceExpression -> {
            val rn = expr.getReferencedName()
            val t = (env[rn] ?: resolveTypeRef(rn, imports))?.let(::canonical)
            println("[KS-type] name '$rn' -> $t")
            t
        }

        is KtDotQualifiedExpression -> {
            val owner0 = resolveExprType(expr.receiverExpression, env, imports)
            val sel = expr.selectorExpression
            println("[KS-type] dot recv='${expr.receiverExpression.text}' owner=$owner0 sel='${sel?.text}'")

            val ctxName: String? = when (sel) {
                is KtCallExpression -> (sel.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                is KtSimpleNameExpression -> sel.getReferencedName()
                else -> null
            }

            val owner = owner0?.let { normalizeOwnerForLookup(it, imports, ctxName) }

            when (sel) {
                is KtSimpleNameExpression -> {
                    val field = sel.getReferencedName()
                    val viaNext = owner?.let { nextTypeAfterField(it, field) }
                    val viaMap = owner?.let { deobfFieldTypeMap[it]?.get(field) }
                    val ret = (viaNext ?: viaMap)
                    val retFq = ret?.let { normalizeOwnerForLookup(it, imports, null) }
                    println("[KS-type]   field '$field' -> viaNext=$viaNext viaMap=$viaMap ret=$retFq")
                    retFq
                }

                is KtCallExpression -> {
                    val m = (sel.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()

                    fun hintForArg(a: KtExpression?): String {
                        if (a == null) return ""
                        val t = resolveExprType(a, env, imports)
                        return if (t != null) toJvmHint(t.substringAfterLast('.')) else when (a) {
                            is KtStringTemplateExpression -> "java/lang/String"
                            is KtConstantExpression -> {
                                val s = a.text.lowercase()
                                when {
                                    s.endsWith("f") -> "F"
                                    s.contains('.') -> "D"
                                    s == "true" || s == "false" -> "Z"
                                    else -> "I"
                                }
                            }

                            else -> ""
                        }
                    }

                    val hints = sel.valueArguments.map { a -> hintForArg(a.getArgumentExpression()) }

                    if (owner != null && m != null) {
                        val pickStrong = strongPickOnOwner(owner, m, hints.size, hints)
                        val mapPick = mappingPickOnOwner(owner, m, hints.size, hints)
                        val selPick = reflectSelectMethod(owner, m, hints.size, hints)
                        val reflectRet = reflectMethodReturnType(owner, m, hints.size, hints)

                        val pickedRet = pickStrong.retDeobf
                            ?: mapPick?.retDeobf
                            ?: selPick?.retDeobf
                            ?: reflectRet

                        val retRaw = pickedRet
                        val retPreferred = preferOwnerInner(owner, retRaw)
                        val retFq = retPreferred?.let { normalizeOwnerForLookup(it, imports, null) }

                        println(
                            "[KS-type]   call '$m' owner=$owner arity=${hints.size} hints=$hints " +
                                    "-> strong.obf=${pickStrong.obfName} strong.ret=${pickStrong.retDeobf} " +
                                    "map.ret=${mapPick?.retDeobf} sel.obf=${selPick?.obfName} sel.ret=${selPick?.retDeobf} " +
                                    "reflect.ret=$reflectRet => retRaw=$retRaw retPreferred=$retPreferred retFq=$retFq"
                        )

                        retFq
                    } else {
                        println("[KS-type]   call '$m' unresolved owner or name (owner=$owner)")
                        null
                    }
                }

                else -> null
            }
        }


        is KtSafeQualifiedExpression -> {
            val rebuilt = KtPsiFactory(expr.project)
                .createExpression("${expr.receiverExpression.text}.${expr.selectorExpression?.text}") as KtExpression
            val t = resolveExprType(rebuilt, env, imports)?.let(::canonical)
            println("[KS-type] safe-dot '${expr.text}' -> $t")
            t
        }

        is KtCallExpression -> {
            val ctor = resolveCtorType(expr, imports)?.let(::canonical)
            println("[KS-type] ctor/static '${expr.text}' -> $ctor")
            ctor
        }

        else -> {
            println("[KS-type] expr '${expr.text}' kind=${expr::class.simpleName} -> <unknown>")
            null
        }
    }
}

private fun jvmPieceToClass(piece: String, cl: ClassLoader = Thread.currentThread().contextClassLoader): Class<*>? {
    return when (piece) {
        "I" -> Int::class.javaPrimitiveType
        "F" -> Float::class.javaPrimitiveType
        "D" -> Double::class.javaPrimitiveType
        "J" -> Long::class.javaPrimitiveType
        "S" -> Short::class.javaPrimitiveType
        "B" -> Byte::class.javaPrimitiveType
        "C" -> Char::class.javaPrimitiveType
        "Z" -> Boolean::class.javaPrimitiveType
        "V" -> Void.TYPE
        else -> {
            val fq = when {
                piece.startsWith("L") && piece.endsWith(";") ->
                    piece.substring(1, piece.length - 1).replace('/', '.')

                else -> piece.replace('/', '.')
            }
            try {
                Class.forName(fq, false, cl)
            } catch (_: Throwable) {
                null
            }
        }
    }
}

private fun deobfNameToRuntimeClass(deobfFq: String): Class<*>? {
    val obf = deobfToObfClassMap[deobfFq] ?: deobfFq
    val fq = obf.replace('/', '.')
    return try {
        Class.forName(fq, false, Thread.currentThread().contextClassLoader)
    } catch (_: Throwable) {
        null
    }
}

private fun argExprToRuntimeClass(arg: KtExpression?, env: TypeEnv, imports: Map<String, String>): Class<*>? {
    if (arg == null) return null

    when (arg) {
        is KtStringTemplateExpression -> return String::class.java
        is KtConstantExpression -> {
            val s = arg.text.trim().lowercase()
            return when {
                s == "true" || s == "false" -> java.lang.Boolean.TYPE
                s.endsWith("f") -> java.lang.Float.TYPE
                s.contains('.') -> java.lang.Double.TYPE
                else -> java.lang.Integer.TYPE
            }
        }
    }

    val deobf = resolveExprType(arg, env, imports)
    if (deobf != null) {
        when (deobf.substringAfterLast('.')) {
            "boolean" -> return java.lang.Boolean.TYPE
            "byte" -> return java.lang.Byte.TYPE
            "short" -> return java.lang.Short.TYPE
            "char" -> return java.lang.Character.TYPE
            "int", "Integer" -> return java.lang.Integer.TYPE
            "long", "Long" -> return java.lang.Long.TYPE
            "float", "Float" -> return java.lang.Float.TYPE
            "double", "Double" -> return java.lang.Double.TYPE
        }
        if ('.' !in deobf && '$' !in deobf) {
            runCatching { return Class.forName("java.lang.$deobf", false, Thread.currentThread().contextClassLoader) }
        }

        val obf = deobfToObfClassMap[deobf] ?: deobf
        val fq = obf.replace('/', '.')
        return runCatching {
            val k = Class.forName(fq, false, Thread.currentThread().contextClassLoader)
            println("[KS-argK] expr='${arg.text}' deobf='$deobf' obf='$obf' fq='$fq' -> $k")
            k
        }.onFailure {
            println("[KS-argK] expr='${arg.text}' deobf='$deobf' obf='$obf' fq='$fq' -> <FAILED> $it")
        }.getOrNull()
    } else {
        println("[KS-argK] expr='${arg.text}' deobf=<null>")
    }

    if (arg is KtDotQualifiedExpression) {
        val ownerT = resolveExprType(arg.receiverExpression, env, imports)
        val sel = arg.selectorExpression as? KtCallExpression
        val m = (sel?.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
        if (ownerT != null && m != null) {
            val hints = sel.valueArguments.map { va ->
                hintForArgExpr(va.getArgumentExpression(), env, imports)
            }

            val pick = reflectSelectMethod(ownerT, m, hints.size, hints)
            val ret = pick?.retDeobf
            if (ret != null) {
                val obf = deobfToObfClassMap[ret] ?: ret
                val fq = obf.replace('/', '.')
                return runCatching {
                    val k = Class.forName(fq, false, Thread.currentThread().contextClassLoader)
                    println("[KS-argK] (fallback) expr='${arg.text}' owner='$ownerT' m='$m' ret='$ret' fq='$fq' -> $k")
                    k
                }.getOrNull()
            }
        }
    }

    return null
}

private fun reflectionPickOverload(
    ownerDeobf: String,
    methodName: String,
    candidates: List<Pair<String, String>>,
    argExprs: List<KtExpression?>,
    env: TypeEnv,
    imports: Map<String, String>
): String? {
    val ownerObf = deobfToObfClassMap[ownerDeobf] ?: return null
    val ownerK = try {
        Class.forName(ownerObf.replace('/', '.'), false, Thread.currentThread().contextClassLoader)
    } catch (_: Throwable) {
        return null
    }

    val argKs: List<Class<*>?> = argExprs.map { argExprToRuntimeClass(it, env, imports) }
    println("[KS-reflect] owner='$ownerDeobf' ($ownerObf) method='$methodName' argK=$argKs")

    fun compatible(paramPieces: List<String>): Pair<Boolean, Array<Class<*>>?> {
        if (paramPieces.size != argKs.size) return false to null
        val pcs = arrayOfNulls<Class<*>>(paramPieces.size)
        for (i in paramPieces.indices) {
            val pc = jvmPieceToClass(paramPieces[i]) ?: return false to null
            val ac = argKs[i] ?: return false to null
            val ok = when {
                pc.isPrimitive -> {
                    val boxed = when (pc) {
                        java.lang.Integer.TYPE -> java.lang.Integer::class.java
                        java.lang.Float.TYPE -> java.lang.Float::class.java
                        java.lang.Double.TYPE -> java.lang.Double::class.java
                        java.lang.Long.TYPE -> java.lang.Long::class.java
                        java.lang.Short.TYPE -> java.lang.Short::class.java
                        java.lang.Byte.TYPE -> java.lang.Byte::class.java
                        java.lang.Character.TYPE -> java.lang.Character::class.java
                        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
                        else -> null
                    }
                    ac == boxed
                }

                else -> pc.isAssignableFrom(ac)
            }
            if (!ok) return false to null
            pcs[i] = pc
        }
        @Suppress("UNCHECKED_CAST")
        return true to (pcs as Array<Class<*>>)
    }

    val ordered = candidates
    for ((desc, obfName) in ordered) {
        val ps = paramTypes(desc)
        val (ok, pcs) = compatible(ps)
        println("[KS-reflect]   cand name=$obfName desc=$desc ps=$ps compat=$ok")
        if (!ok || pcs == null) continue
        try {
            ownerK.getMethod(obfName, *pcs)
            println("[KS-reflect]   -> choose '$obfName' by reflection")
            return obfName
        } catch (_: Throwable) {
        }
    }
    return null
}

private fun canonicalDeobfClassPreserveInner(s: String): String {
    val k = s.replace('$', '.')
    if (deobfToObfClassMap.containsKey(s) ||
        deobfToObfClassMap.containsKey(s.replace('$', '.')) ||
        deobfToObfClassMap.containsKey(k)
    ) {
        return s
    }
    return canonicalDeobfClass(s)
}

fun collectMethodCallRewritesByLine(ktFile: KtFile, source: String): Map<Int, List<Pair<String, String>>> {
    val out = mutableMapOf<Int, MutableList<Pair<String, String>>>()

    val importSimpleNameMap = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val p = it.importPath?.pathStr ?: return@forEach
        importSimpleNameMap[p.substringAfterLast('.')] = p
    }

    fun resolveTypeRefOrNull(t: String?): String? =
        t?.let { resolveTypeRef(it, importSimpleNameMap) }?.let(::canonicalDeobfClass)

    fun hintFromType(deobfFq: String?): String =
        if (deobfFq == null) "" else toJvmHint(deobfFq.substringAfterLast('.'))


    fun numericHintFromLiteral(text: String): String {
        val s = text.lowercase()
        val isHexOrBin = s.startsWith("0x") || s.startsWith("0b")
        if (s.endsWith("f")) return "F"
        if (!isHexOrBin && s.contains('.')) return "D"
        return "I"
    }

    val lineStarts = IntArray(source.count { it == '\n' } + 1).also { arr ->
        var idx = 0
        var pos = 0
        arr[idx++] = 0
        while (true) {
            val n = source.indexOf('\n', pos)
            if (n < 0) break
            pos = n + 1
            arr[idx++] = pos
        }
    }

    fun offsetToLine(off: Int): Int {
        var lo = 0
        var hi = lineStarts.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val v = lineStarts[mid]
            when {
                v == off -> return mid
                v < off -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return hi.coerceAtLeast(0)
    }

    val env = TypeEnv().apply { push() }

    fun resolveLocal(expr: KtExpression?): String? =
        expr?.let { resolveExprType(it, env, importSimpleNameMap) }?.let(::canonicalDeobfClass)

    fun hintForArgOrFallback(e: KtExpression?, env: TypeEnv): String {
        if (e == null) return ""
        resolveLocal(e)?.let { return hintFromType(it) }
        return when (e) {
            is KtStringTemplateExpression -> "java/lang/String"
            is KtConstantExpression -> numericHintFromLiteral(e.text)
            is KtDotQualifiedExpression -> when {
                e.text.endsWith(".toInt()") -> "I"
                e.text.endsWith(".toFloat()") -> "F"
                e.text.endsWith(".toDouble()") -> "D"
                else -> ""
            }

            else -> ""
        }
    }

    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            env.withScope {
                function.receiverTypeReference?.text
                    ?.let { resolveTypeRefOrNull(it) }
                    ?.let { normalizeOwnerForLookup(it, importSimpleNameMap, null) }
                    ?.let { env["this"] = it }

                function.valueParameters.forEach { p ->
                    val t = p.typeReference?.text ?: return@forEach
                    resolveTypeRefOrNull(t)?.let { fq0 ->
                        val fq = normalizeOwnerForLookup(fq0, importSimpleNameMap, null)
                        p.name?.let { name ->
                            env[name] = fq
                            println("[KS-env] param '$name': typeText='$t' -> '$fq'")
                        }
                    }
                }
                super.visitNamedFunction(function)
            }
        }


        override fun visitProperty(property: KtProperty) {
            val name = property.name
            var stored = false

            if (name != null) {
                property.typeReference?.text?.let { typeText ->
                    resolveTypeRefOrNull(typeText)?.let { fq ->
                        env[name] = fq
                        stored = true
                    }
                }

                if (!stored) {
                    val init = property.initializer
                    val inferred = when (init) {
                        is KtCallExpression -> {
                            val parent = init.parent
                            if (parent is KtDotQualifiedExpression) {
                                val ownerType = resolveLocal(parent.receiverExpression)
                                val method = (init.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                                val hints = init.valueArguments.map { a ->
                                    val t = resolveLocal(a.getArgumentExpression())
                                    if (t != null) hintFromType(t) else hintForArgOrFallback(
                                        a.getArgumentExpression(),
                                        env
                                    )
                                }
                                if (ownerType != null && method != null) {
                                    val pick = strongPickOnOwner(ownerType, method, hints.size, hints)
                                    val ret =
                                        pick.retDeobf ?: reflectMethodReturnType(ownerType, method, hints.size, hints)
                                    val canon = ret?.let(::canonicalDeobfClass)
                                    canon
                                } else null
                            } else {
                                resolveCtorType(init, importSimpleNameMap)?.let(::canonicalDeobfClass)
                            }
                        }

                        is KtDotQualifiedExpression -> {
                            val ownerType = resolveLocal(init.receiverExpression)
                            when (val sel = init.selectorExpression) {
                                is KtCallExpression -> {
                                    val method = (sel.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                                    val hints = sel.valueArguments.map { a ->
                                        val t = resolveLocal(a.getArgumentExpression())
                                        if (t != null) hintFromType(t) else hintForArgOrFallback(
                                            a.getArgumentExpression(),
                                            env
                                        )
                                    }
                                    if (ownerType != null && method != null) {
                                        val pick = strongPickOnOwner(ownerType, method, hints.size, hints)
                                        val ret = pick.retDeobf ?: reflectMethodReturnType(
                                            ownerType,
                                            method,
                                            hints.size,
                                            hints
                                        )
                                        val canon = ret?.let(::canonicalDeobfClass)
                                        canon
                                    } else null
                                }

                                is KtSimpleNameExpression -> {
                                    val fieldName = sel.getReferencedName()
                                    val ret = if (ownerType != null)
                                        deobfFieldTypeMap[canonicalDeobfClass(ownerType)]?.get(fieldName)
                                    else null
                                    ret?.let(::canonicalDeobfClass)
                                }

                                else -> null
                            }
                        }

                        is KtNameReferenceExpression ->
                            (env[init.getReferencedName()] ?: resolveTypeRef(
                                init.getReferencedName(),
                                importSimpleNameMap
                            ))?.let(::canonicalDeobfClass)

                        else -> null
                    }
                    if (inferred != null) {
                        env[name] = inferred
                    }
                }
            }

            super.visitProperty(property)
        }

        override fun visitCallExpression(expr: KtCallExpression) {
            super.visitCallExpression(expr)

            val dot = expr.parent as? KtDotQualifiedExpression ?: return
            val methodName = (expr.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return

            val ownerResolved = resolveExprType(dot.receiverExpression, env, importSimpleNameMap)
            val ownerRaw = ownerResolved ?: run {
                val head = (dot.receiverExpression as? KtNameReferenceExpression)?.getReferencedName()
                val headType = head?.let { env[it] ?: resolveTypeRef(it, importSimpleNameMap) }
                if (headType == null) return else headType
            }

            val ownerCanon = normalizeOwnerForLookup(
                canonicalDeobfClassPreserveInner(ownerRaw),
                importSimpleNameMap,
                methodName
            )
            val ownerObf = deobfToObfClassMap[ownerCanon]

            val hints = expr.valueArguments.map { a -> hintForArgOrFallback(a.getArgumentExpression(), env) }
            val normHints = hints.map { toJvmHint(it) }
            val argExprs = expr.valueArguments.map { it.getArgumentExpression() }
            val arity = hints.size
            val line = offsetToLine(expr.textRange.startOffset)

            logger.info(
                "[KS-pick] recv='${dot.receiverExpression.text}' ownerRaw='$ownerRaw' ownerCanon='$ownerCanon' ownerObf='${ownerObf ?: "-"}' " +
                        "meth=$methodName arity=$arity rawHints=$hints normHints=$normHints"
            )

            val ownersForLookup = buildList {
                add(ownerCanon)
                addAll(classClosureDeobf(ownerCanon))
            }.distinct()
            val candPairs: List<Pair<String, String>> =
                ownersForLookup.flatMap { deobfMethodOverloads[it]?.get(methodName)?.toList().orEmpty() }

            run {
                val pick = strongPickOnOwner(ownerCanon, methodName, arity, hints)
                val strong = pick.obfName
                if (!strong.isNullOrEmpty()) {
                    val finalObf = if (candPairs.isNotEmpty()) {
                        val descForStrong = candPairs.firstOrNull { it.second == strong }?.first
                        val verified = if (descForStrong != null) {
                            reflectionPickOverload(
                                ownerCanon,
                                methodName,
                                listOf(descForStrong to strong),
                                argExprs,
                                env,
                                importSimpleNameMap
                            )
                        } else null
                        verified ?: reflectionPickOverload(
                            ownerCanon,
                            methodName,
                            candPairs,
                            argExprs,
                            env,
                            importSimpleNameMap
                        ) ?: strong
                    } else strong
                    out.getOrPut(line) { mutableListOf() }.add(methodName to finalObf)
                    logger.info("[KS-pick]  strongPick -> '$strong' (final='$finalObf')")
                    return
                }
            }

            run {
                val mp = mappingPickOnOwner(ownerCanon, methodName, arity, hints)
                val obf2 = mp?.obfName
                if (!obf2.isNullOrEmpty()) {
                    val finalObf = if (candPairs.isNotEmpty()) {
                        val descForMap = candPairs.firstOrNull { it.second == obf2 }?.first
                        val verified = if (descForMap != null) {
                            reflectionPickOverload(
                                ownerCanon,
                                methodName,
                                listOf(descForMap to obf2),
                                argExprs,
                                env,
                                importSimpleNameMap
                            )
                        } else null
                        verified ?: reflectionPickOverload(
                            ownerCanon,
                            methodName,
                            candPairs,
                            argExprs,
                            env,
                            importSimpleNameMap
                        ) ?: obf2
                    } else obf2
                    out.getOrPut(line) { mutableListOf() }.add(methodName to finalObf)
                    val candDbg = candPairs.map { (d, o) -> "$d:$o" }
                    logger.info("[KS-pick]  candidates=$candDbg")
                    logger.info("[KS-pick]  -> chose obf='$finalObf' for $ownerCanon.$methodName($normHints)")
                    return
                }
            }

            run {
                if (candPairs.isNotEmpty()) {
                    val picked =
                        reflectionPickOverload(ownerCanon, methodName, candPairs, argExprs, env, importSimpleNameMap)
                    if (!picked.isNullOrEmpty()) {
                        out.getOrPut(line) { mutableListOf() }.add(methodName to picked)
                        logger.info("[KS-pick]  -> reflection chose obf='$picked' for $ownerCanon.$methodName")
                        return
                    }
                }
            }

            logger.warn("[KS-pick]  FAILED to pick for $ownerCanon.$methodName arity=$arity; hints=$normHints; recv='${dot.receiverExpression.text}'")
        }


    })

    env.pop()

    return out
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


    fun hintFromType(deobfFq: String?): String =
        if (deobfFq == null) "" else canonicalDeobfClass(deobfFq).replace('.', '/')

    fun resolveExprType(
        expr: KtExpression,
        env: TypeEnv,
        imports: Map<String, String>
    ): String? = when (expr) {
        is KtThisExpression -> env["this"]

        is KtNameReferenceExpression -> {
            val rn = expr.getReferencedName()
            env[rn] ?: resolveTypeRef(rn, imports)
        }

        is KtDotQualifiedExpression -> {
            val ownerType = resolveExprType(expr.receiverExpression, env, imports)
            when (val sel = expr.selectorExpression) {
                is KtSimpleNameExpression -> {
                    val field = sel.getReferencedName()
                    if (ownerType != null) nextTypeAfterField(ownerType, field) else null
                }

                is KtCallExpression -> {
                    val method = (sel.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                    val hints = sel.valueArguments.map { a ->
                        val t = a.getArgumentExpression()?.let { resolveExprType(it, env, imports) }
                        if (t != null) hintFromType(t) else ""
                    }
                    if (ownerType != null && method != null) {
                        val strong = strongPickOnOwner(ownerType, method, hints.size, hints)
                        val mapPick = mappingPickOnOwner(ownerType, method, hints.size, hints)
                        val selPick = reflectSelectMethod(ownerType, method, hints.size, hints)
                        val reflectRet = reflectMethodReturnType(ownerType, method, hints.size, hints)

                        println(
                            "[KS-type]   HINT call '$method' owner=$ownerType arity=${hints.size} childHints=$hints " +
                                    "-> strong.obf=${strong.obfName} strong.ret=${strong.retDeobf} " +
                                    "map.ret=${mapPick?.retDeobf} sel.obf=${selPick?.obfName} sel.ret=${selPick?.retDeobf} " +
                                    "reflect.ret=$reflectRet"
                        )
                        val retRaw = strong.retDeobf
                            ?: mapPick?.retDeobf
                            ?: selPick?.retDeobf
                            ?: reflectRet
                        val retPreferred = preferOwnerInner(ownerType, retRaw)
                        println("[KS-type]   call '$method' owner=$ownerType arity=${hints.size} hints=$hints -> strong.ret=${strong.retDeobf} map.ret=${mapPick?.retDeobf} sel.ret=${selPick?.retDeobf} reflect.ret=$reflectRet => retPreferred=$retPreferred")

                        retPreferred?.let(::canonicalDeobfClass)

                    } else null
                }


                else -> null
            }
        }

        is KtSafeQualifiedExpression -> {
            val recv = expr.receiverExpression
            val sel = expr.selectorExpression
            resolveExprType(
                KtPsiFactory(expr.project).createExpression("${recv.text}.${sel?.text}") as KtExpression,
                env,
                imports
            )
        }

        is KtCallExpression -> resolveCtorType(expr, imports)

        else -> null
    }

    fun hintForArg(a: KtExpression?): String {
        if (a == null) return ""
        val t = resolveExprType(a, env, importSimpleNameMap)
        return if (t != null) toJvmHint(t.substringAfterLast('.')) else when (a) {
            is KtStringTemplateExpression -> "java/lang/String"
            is KtConstantExpression -> {
                val s = a.text.lowercase()
                when {
                    s.endsWith("f") -> "F"
                    s.contains('.') -> "D"
                    s == "true" || s == "false" -> "Z"
                    else -> "I"
                }
            }

            else -> ""
        }
    }

    ktFile.accept(object : KtTreeVisitorVoid() {

        override fun visitNamedFunction(function: KtNamedFunction) {
            env.withScope {
                function.receiverTypeReference?.text
                    ?.let { resolveTypeRef(it, importSimpleNameMap) }
                    ?.let { env["this"] = it }

                function.valueParameters.forEach { p ->
                    val t = p.typeReference?.text ?: return@forEach
                    val fq = resolveTypeRef(t, importSimpleNameMap)
                    p.name?.let { env[it] = fq }
                }

                super.visitNamedFunction(function)
            }
        }

        override fun visitLambdaExpression(expr: KtLambdaExpression) {
            env.withScope {
                expr.valueParameters.forEach { p ->
                    val t = p.typeReference?.text ?: return@forEach
                    val fq = resolveTypeRef(t, importSimpleNameMap)
                    p.name?.let { env[it] = fq }
                }
                super.visitLambdaExpression(expr)
            }
        }

        override fun visitForExpression(expr: KtForExpression) {
            env.withScope {
                val name = expr.loopParameter?.name
                val range = expr.loopRange
                if (name != null && range != null) {
                    resolveExprType(range, env, importSimpleNameMap)?.let { env[name] = it }
                }
                super.visitForExpression(expr)
            }
        }

        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)

            val name = property.name ?: return

            property.typeReference?.text?.let { typeText ->
                env[name] = resolveTypeRef(typeText, importSimpleNameMap)
                return
            }

            val init = property.initializer ?: return
            var inferred: String? = null
            when (init) {
                is KtCallExpression -> {
                    val parent = init.parent
                    if (parent is KtDotQualifiedExpression) {
                        val ownerType = resolveExprType(parent.receiverExpression, env, importSimpleNameMap)
                        val method = (init.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()
                        val hints = init.valueArguments.map { a -> hintForArg(a.getArgumentExpression()) }
                        if (ownerType != null && method != null) {
                            inferred = strongPickOnOwner(ownerType, method, hints.size, hints).retDeobf
                        }
                    } else {
                        inferred = resolveCtorType(init, importSimpleNameMap)
                    }
                }

                is KtNameReferenceExpression -> {
                    val rn = init.getReferencedName()
                    inferred = env[rn] ?: resolveTypeRef(rn, importSimpleNameMap)
                }

                is KtDotQualifiedExpression -> {
                    val ownerType = resolveExprType(init.receiverExpression, env, importSimpleNameMap)
                    val fieldName = (init.selectorExpression as? KtSimpleNameExpression)?.getReferencedName()
                    if (ownerType != null && fieldName != null) {
                        inferred = deobfFieldTypeMap[ownerType]?.get(fieldName)
                    }
                }
            }
            if (inferred != null) env[name] = canonicalDeobfClass(inferred!!)
        }

        override fun visitCallExpression(expr: KtCallExpression) {
            super.visitCallExpression(expr)

            val methodName = (expr.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return
            val dot = expr.parent as? KtDotQualifiedExpression ?: return

            val ownerDeobf = resolveExprType(dot.receiverExpression, env, importSimpleNameMap) ?: run {
                val head = (dot.receiverExpression as? KtNameReferenceExpression)?.getReferencedName()
                val headType = head?.let { env[it] ?: resolveTypeRef(it, importSimpleNameMap) }
                if (headType == null) return else canonicalDeobfClass(headType)
            }

            val ownerCanon = normalizeOwnerForLookup(canonicalDeobfClass(ownerDeobf), importSimpleNameMap, methodName)

            val hints = expr.valueArguments.map { a -> hintForArg(a.getArgumentExpression()) }
            val arity = hints.size

            val owners = buildList {
                add(ownerCanon)
                addAll(classClosureDeobf(ownerCanon))
            }.distinct()

            val hasName = owners.any { deobfMethodOverloads[it]?.containsKey(methodName) == true }
            val hasArity = owners.any { deobfToObfMethodByArity[it]?.get(methodName)?.isNotEmpty() == true }

            if (hasName || hasArity) {
                found += MethodUse(ownerCanon, methodName, arity, hints)
                return
            }

            val pick = mappingPickOnOwner(ownerDeobf, methodName, arity, hints)
                ?: run {
                    val ret = reflectMethodReturnType(ownerDeobf, methodName, arity, hints)
                    if (ret != null) MappingPick(methodName, ret) else null
                }

            if (pick != null) {
                found += MethodUse(ownerDeobf, methodName, arity, hints)
            }
        }
    })

    env.pop()
    return found.distinctBy { Triple(it.className, it.method, it.arity) }.toSet()
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

private fun preferOwnerInner(ownerDeobf: String?, retDeobf: String?): String? {
    if (ownerDeobf == null || retDeobf == null) return retDeobf

    val ownerFq = normalizeOwnerForLookup(ownerDeobf)
    val retFqOrSimple = if ('.' in retDeobf) canonicalDeobfClass(retDeobf)
    else guessFqFromSimple(retDeobf) ?: retDeobf
    val simple = retFqOrSimple.substringAfterLast('.')

    val dot = "$ownerFq.$simple"
    val dollar = "$ownerFq$$simple"

    val hit = when {
        deobfToObfClassMap.containsKey(dot) -> dot
        deobfToObfClassMap.containsKey(dollar) -> dollar
        else -> null
    }
    println("[KS-inner] owner='$ownerDeobf'→'$ownerFq' retIn='$retDeobf'→'$retFqOrSimple' simple='$simple' try='$dot'|'$dollar' hit='${hit ?: "-"}'")
    return (hit ?: retFqOrSimple)
}

private fun chooseOverloadByHints(cls: String, name: String, arity: Int, hints: List<String>): String? {
    val list = deobfMethodOverloads[cls]?.get(name) ?: return null
    var best: Pair<Int, String>? = null
    for ((desc, obf) in list) {
        val ps = paramTypes(desc)
        if (ps.size != arity) continue
        var score = 0
        for (i in hints.indices) {
            val h = toJvmHint(hints[i])
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
    val map = mutableMapOf<String, String>()
    ktFile.importDirectives.forEach {
        val fq = it.importPath?.pathStr ?: return@forEach
        val simple = fq.substringAfterLast('.')
        val obf = deobfToObfClassMap[fq]
        if (obf != null) {
            val to = obf.replace('$', '.')
            map[simple] = to
        }
    }
    return map
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

private fun bareJvm(s: String): String =
    s.trim().removePrefix("L").removeSuffix(";")

private fun toJvmHint(s: String): String {
    val t = s.removeSuffix("?")
    return when (t) {
        "boolean", "kotlin.Boolean", "java.lang.Boolean" -> "Z"
        "byte", "kotlin.Byte", "java.lang.Byte" -> "B"
        "char", "kotlin.Char", "java.lang.Character" -> "C"
        "short", "kotlin.Short", "java.lang.Short" -> "S"
        "int", "kotlin.Int", "java.lang.Integer" -> "I"
        "long", "kotlin.Long", "java.lang.Long" -> "J"
        "float", "kotlin.Float", "java.lang.Float" -> "F"
        "double", "kotlin.Double", "java.lang.Double" -> "D"
        "void", "kotlin.Unit", "java.lang.Void" -> "V"
        "String", "kotlin.String", "java.lang.String" -> "java/lang/String"
        else -> t.replace('.', '/')
    }
}


private fun scoreByHints(paramTypes: Array<Class<*>>, hints: List<String>): Int {
    var score = 0
    val n = minOf(paramTypes.size, hints.size)
    var i = 0
    while (i < n) {
        val h = hints[i]
        if (h.isNotEmpty()) {
            val ph = classToJvmAtom(paramTypes[i])
            if (ph.contains(h)) score += 3
        }
        i++
    }
    return score
}

private fun reflectMethodReturnType(
    ownerDeobf: String,
    methodNameOrObf: String,
    arity: Int,
    hints: List<String>
): String? {
    println("[KS-reflect-ret] owner=$ownerDeobf method=$methodNameOrObf arity=$arity hints=$hints")

    val root = tryLoadEither(ownerDeobf) ?: run {
        println("[KS-reflect-ret]   !! could not load owner class for '$ownerDeobf'")
        return null
    }

    val seen = LinkedHashSet<Class<*>>()
    val q = ArrayDeque<Class<*>>()
    q.add(root)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        if (!seen.add(c)) continue
        c.superclass?.let(q::add)
        c.interfaces?.forEach(q::add)
    }

    fun backOwner(c: Class<*>) = c.name.replace('.', '/')
    fun keyOf(m: java.lang.reflect.Method): String {
        return methodKey(m)
    }

    val strict = ArrayList<java.lang.reflect.Method>()
    val reasons = mutableListOf<String>()
    for (c in seen) {
        for (m in c.declaredMethods + c.methods) {
            if (m.parameterCount != arity) continue
            var accept = (m.name == methodNameOrObf)
            if (!accept) {
                val ownerKey = backOwner(m.declaringClass)
                val back = obfToDeobfMethodMap[ownerKey]
                val k = keyOf(m)
                val deobf = back?.get(k)
                accept = (deobf == methodNameOrObf)
                if (!accept) reasons += "reject ${m.declaringClass.name}.${m.name}${keyOf(m)}: name/backmap mismatch (wanted '$methodNameOrObf', got '$deobf')"
            }
            if (accept) strict += m
        }
    }
    println("[KS-reflect-ret]   strictCandidates=${strict.size}")
    reasons.take(5).forEach { println("[KS-reflect-ret]     $it") }

    val candidates = if (strict.isNotEmpty()) strict else buildList {
        for (c in seen) for (m in c.declaredMethods + c.methods) {
            if (m.parameterCount == arity) add(m)
        }
    }
    println("[KS-reflect-ret]   candidates=${candidates.size} -> ${
        candidates.joinToString { it.name + keyOf(it) }
    }")

    if (candidates.isEmpty()) return null

    var best: java.lang.reflect.Method? = null
    var bestScore = Int.MIN_VALUE
    var tie = false
    for (m in candidates) {
        val s = scoreByHints(m.parameterTypes, hints, arity)
        if (s > bestScore) {
            best = m; bestScore = s; tie = false
        } else if (s == bestScore) {
            tie = true
        }
    }
    if (tie && best != null) {
        for (m in candidates) {
            if (scoreByHints(m.parameterTypes, hints, arity) == bestScore &&
                m.declaringClass == root
            ) {
                best = m; break
            }
        }
    }

    val chosen = best!!
    val ret = chosen.returnType
    if (ret == java.lang.Void.TYPE) return null

    val obfRet = ret.name
    println("[KS-reflect-ret]   choose name=${chosen.name} ret=$obfRet tie=$tie")

    val deobfRaw =
        obfToDeobfClassMap[obfRet]
            ?: obfToDeobfClassMap[obfRet.replace('$', '.')]
            ?: obfRet

    val named = canonicalDeobfClass(deobfRaw)
    val preferred = preferOwnerInner(ownerDeobf, named)
    println("[KS-reflect-ret]   deobfRet=$named -> preferred=$preferred (from obf=$obfRet)")
    return preferred
}


private fun classToJvmAtom(c: Class<*>): String {
    if (c.isArray) {
        var t = c
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
            java.lang.Void.TYPE -> sb.append('V').toString()
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

fun tryLoadEither(name: String): Class<*>? {
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


private data class ReflectPick(val obfName: String, val retDeobf: String?)

private fun reflectSelectMethod(
    ownerDeobf: String,
    methodNameOrObf: String,
    arity: Int,
    hints: List<String>
): ReflectPick? {
    println("[KS-reflect-sel] owner=$ownerDeobf method=$methodNameOrObf arity=$arity hints=$hints")

    val root = tryLoadEither(ownerDeobf) ?: run {
        println("[KS-reflect-sel]   !! could not load owner class for '$ownerDeobf'")
        return null
    }
    val seen = LinkedHashSet<Class<*>>()
    val q = ArrayDeque<Class<*>>()
    q.add(root)
    while (q.isNotEmpty()) {
        val c = q.removeFirst()
        if (!seen.add(c)) continue
        c.superclass?.let(q::add)
        c.interfaces?.forEach(q::add)
    }

    fun backOwner(c: Class<*>) = c.name.replace('.', '/')
    fun keyOf(m: java.lang.reflect.Method) = methodKey(m)

    val strict = ArrayList<java.lang.reflect.Method>()
    for (c in seen) for (m in c.declaredMethods + c.methods) {
        if (m.parameterCount != arity) continue
        var accept = (m.name == methodNameOrObf)
        if (!accept) {
            val back = obfToDeobfMethodMap[backOwner(m.declaringClass)]
            if (back != null && back[keyOf(m)] == methodNameOrObf) accept = true
        }
        if (accept) strict += m
    }

    val candidates = if (strict.isNotEmpty()) strict else buildList {
        for (c in seen) for (m in c.declaredMethods + c.methods) {
            if (m.parameterCount == arity) add(m)
        }
    }
    println("[KS-reflect-sel]   candidates=${candidates.size}")

    if (candidates.isEmpty()) return null

    var best: java.lang.reflect.Method? = null
    var bestScore = Int.MIN_VALUE
    for (m in candidates) {
        val s = scoreByHints(m.parameterTypes, hints, arity)
        if (s > bestScore) {
            best = m; bestScore = s
        }
    }
    val chosen = best!!
    val retObf = chosen.returnType.name
    val retDeobf =
        obfToDeobfClassMap[retObf] ?: obfToDeobfClassMap[retObf.replace('$', '.')] ?: retObf
    val retPreferred = preferOwnerInner(ownerDeobf, canonicalDeobfClass(retDeobf))
    val obfName = chosen.name
    println("[KS-reflect-sel]   choose obf=$obfName ret=$retPreferred")
    return ReflectPick(obfName, retPreferred)
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

fun collectDeclarationRanges(ktFile: KtFile): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(f: KtNamedFunction) {
            f.nameIdentifier?.let { ranges += it.textRange.startOffset until it.textRange.endOffset }
            super.visitNamedFunction(f)
        }

        override fun visitParameter(p: KtParameter) {
            p.nameIdentifier?.let { ranges += it.textRange.startOffset until it.textRange.endOffset }
            super.visitParameter(p)
        }

        override fun visitProperty(p: KtProperty) {
            p.nameIdentifier?.let { ranges += it.textRange.startOffset until it.textRange.endOffset }
            super.visitProperty(p)
        }

        override fun visitClassOrObject(c: KtClassOrObject) {
            c.nameIdentifier?.let { ranges += it.textRange.startOffset until it.textRange.endOffset }
            super.visitClassOrObject(c)
        }
    })
    return ranges.sortedBy { it.first }
}

class TestParser {
    companion object {

        fun main(string: String): String {
            setIdeaIoUseFallback()
            val disposable = Disposer.newDisposable()

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

            val normalizedSource = normalizeLambdaFunctionTypeReturns(ktFile, string)

            val ktFile2 = psiFileFactory.createFileFromText(
                "test.kts",
                KotlinLanguage.INSTANCE,
                normalizedSource
            ) as KtFile

            val importLineMap = buildObfuscatedImports(ktFile2)
            val foundClasses = findClassUsages(ktFile2)
            val foundMethods = findMethodUsages(ktFile2)
            val foundFields = findFieldUsages(ktFile2)
            val nested = findNestedQualifiedClassUsages(ktFile2)
            val overrideRenames = findOverrideRenames(ktFile2)
            traceAll(ktFile2, logger)
            val qualifierMap = buildImportQualifierMap(ktFile2)

            val protectedByLine = collectProtectedRangesByLine(ktFile2, normalizedSource)
            val chained = findChainedFieldRewrites(ktFile2, normalizedSource)

            val qualified = findQualifiedClassUsages(ktFile2)
            val methodCalls = collectMethodCallRewritesByLine(ktFile, normalizedSource)

            val result = replaceObfuscated(
                normalizedSource,
                foundClasses,
                foundMethods,
                foundFields,
                nested,
                overrideRenames,
                protectedByLine,
                qualifierMap,
                chained,
                qualified,
                methodCalls
            )


            val fixed = fixVoidLambdaFunctionTypeReturns(environment.project, result)
            Disposer.dispose(disposable)
            return fixed
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

                override fun visitDotQualifiedExpression(expr: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expr)

                    fun tryAdd(outerText: String, innerSimple: String) {
                        val obfNested = resolveObfNestedFromAnyOuter(outerText, innerSimple) ?: return
                        addAllSpellings(repl, outerText, innerSimple, obfNested)
                    }

                    val recv = expr.receiverExpression
                    val selName = (expr.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()

                    if (recv is KtNameReferenceExpression && selName != null) {
                        tryAdd(recv.getReferencedName(), selName)
                    }

                    if (recv is KtDotQualifiedExpression) {
                        val inner = (recv.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
                        val outer = recv.receiverExpression.text
                        if (inner != null) tryAdd(outer, inner)
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
                    val parent = type.parent
                    val isFunctionTypeReturn = parent is KtTypeReference && parent.parent is KtFunctionType
                    if (isFunctionTypeReturn) return
                    val name = type.referencedName ?: return
                    if (deobfToObfClassMap.containsKey(name) ||
                        deobfToObfClassMap.containsKey(name.substringAfterLast('$'))
                    ) {
                        found += name
                    }
                }

                override fun visitCallExpression(expr: KtCallExpression) {
                    super.visitCallExpression(expr)
                    val callee = expr.calleeExpression as? KtSimpleNameExpression ?: return
                    val name = callee.getReferencedName()
                    if (deobfToObfClassMap.containsKey(name)) {
                        found += name
                    }
                }

                override fun visitSimpleNameExpression(expr: KtSimpleNameExpression) {
                    super.visitSimpleNameExpression(expr)
                    val name = expr.getReferencedName()
                    if (deobfToObfClassMap.containsKey(name) ||
                        deobfToObfClassMap.containsKey(name.substringAfterLast('$'))
                    ) {
                        found += name
                    }
                }
            })
            return found
        }

        fun findFieldUsages(ktFile: KtFile): Set<Pair<String, String>> {
            val foundFields = mutableSetOf<Pair<String, String>>()

            fun lastDotToDollar(s: String): String {
                val i = s.lastIndexOf('.'); return if (i >= 0) s.substring(0, i) + '$' + s.substring(i + 1) else s
            }

            val importSimpleNameMap = mutableMapOf<String, String>()
            ktFile.importDirectives.forEach {
                val ip = it.importPath?.pathStr ?: return@forEach
                importSimpleNameMap[ip.substringAfterLast('.')] = ip
            }

            fun resolveSimpleType(t: String): String = importSimpleNameMap[t] ?: t

            fun ownerCandidatesForQualifier(qual: String): List<String> {
                val parts = qual.split('.').filter { it.isNotEmpty() }
                if (parts.isEmpty()) return emptyList()
                val headFq = resolveSimpleType(parts.first())
                val dotted = buildString {
                    append(headFq); for (p in parts.drop(1)) {
                    append('.'); append(p)
                }
                }
                val dollared = buildString {
                    append(headFq)
                    if (parts.size > 1) {
                        append('$'); append(parts.drop(1).joinToString("$"))
                    }
                }
                val rawCanon = canonicalDeobfClass(qual)
                val dottedCanon = canonicalDeobfClass(dotted)
                return listOf(dollared, dotted, dottedCanon, rawCanon).distinct().filter { it.isNotEmpty() }
            }

            ktFile.accept(object : KtTreeVisitorVoid() {
                private var paramEnv: Map<String, String> = emptyMap()
                private var thisChain: List<String> = emptyList()

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

                override fun visitClassOrObject(co: KtClassOrObject) {
                    val saved = thisChain
                    val supers = co.superTypeListEntries.mapNotNull { it.typeReference?.text }
                    val base = supers.firstOrNull()?.let { importSimpleNameMap[it] ?: it }
                        ?: run {
                            val pkg = (co.containingFile as? KtFile)?.packageFqName?.asString().orEmpty()
                            val nm = co.name ?: ""
                            if (nm.isNotEmpty()) "$pkg.$nm" else null
                        }
                    val canon = base?.let(::canonicalDeobfClass)
                    thisChain = buildList {
                        canon?.let { add(it) }
                        if (canon != null) addAll(collectSuperChainNamed(canon))
                    }
                    super.visitClassOrObject(co)
                    thisChain = saved
                }

                fun resolveReceiverType(expr: KtExpression): String? {
                    return when (expr) {
                        is KtNameReferenceExpression -> {
                            val nm = expr.getReferencedName()
                            paramEnv[nm] ?: run {
                                for (owner in thisChain) {
                                    val fmap = deobfToObfFieldMap[owner] ?: continue
                                    if (fmap.containsKey(nm)) {
                                        val t = nextTypeAfterField(owner, nm)
                                        if (t != null) return t
                                    }
                                }
                                null
                            }
                        }

                        is KtDotQualifiedExpression -> {
                            val ownerType = resolveReceiverType(expr.receiverExpression) ?: return null
                            when (val sel = expr.selectorExpression) {
                                is KtSimpleNameExpression -> {
                                    val field = sel.getReferencedName()
                                    val t = nextTypeAfterField(ownerType, field)
                                    t
                                }

                                else -> null
                            }
                        }

                        else -> null
                    }
                }

                override fun visitSimpleNameExpression(expr: KtSimpleNameExpression) {
                    super.visitSimpleNameExpression(expr)
                    val n = (expr as? KtNameReferenceExpression)?.getReferencedName() ?: return
                    if (paramEnv.containsKey(n)) return
                    if (deobfToObfClassMap.containsKey(n)) return
                    val owner =
                        thisChain.firstOrNull { owner -> deobfToObfFieldMap[owner]?.containsKey(n) == true } ?: return
                    foundFields += owner to n
                }

                override fun visitDotQualifiedExpression(expr: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expr)
                    val selector = expr.selectorExpression as? KtSimpleNameExpression ?: return
                    val fieldName = selector.getReferencedName()
                    val recvText = expr.receiverExpression.text

                    val ownerType = resolveReceiverType(expr.receiverExpression)
                    if (ownerType != null) {
                        val fm = deobfToObfFieldMap[ownerType]
                            ?: deobfToObfFieldMap[lastDotToDollar(ownerType)]
                        if (fm?.containsKey(fieldName) == true) {
                            foundFields += ownerType to fieldName
                            return
                        }
                    }

                    for (owner in ownerCandidatesForQualifier(recvText)) {
                        val fmap = deobfToObfFieldMap[owner] ?: deobfToObfFieldMap[lastDotToDollar(owner)]
                        if (fmap?.containsKey(fieldName) == true) {
                            foundFields += owner to fieldName
                            return
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
            importQualifierMap: Map<String, String>,
            chainedFieldRewritesByLine: Map<Int, List<Pair<String, String>>>,
            qualifiedClassReplacements: Map<String, String>,
            methodCallRewritesByLine: Map<Int, List<Pair<String, String>>>
        ): String {
            fun isDeclarationLine(line: String, name: String): Boolean {
                val r = Regex("""\b(class|object|fun|val|var)\s+${Regex.escape(name)}\b""")
                return r.containsMatchIn(line)
            }

            fun ownerVariants(owner: String): List<String> {
                val dotted = owner.replace('/', '.')
                val dollar =
                    dotted.replace(Regex("""\.(?=[A-Z][\w$]*$)"""), java.util.regex.Matcher.quoteReplacement("$"))
                val withDotInner = dotted.replace('$', '.')
                val canonical = canonicalDeobfClass(dotted)
                return listOf(owner, dotted, dollar, withDotInner, canonical).distinct()
            }

            fun pickObfMethodName(owner: String, method: String, arity: Int, hints: List<String>): String? {
                val variants = ownerVariants(owner)
                for (ov in variants) {
                    val s = strongPickOnOwner(ov, method, arity, hints).obfName
                    if (!s.isNullOrEmpty()) {
                        return s
                    }
                    val m = mappingPickOnOwner(ov, method, arity, hints)?.obfName
                    if (!m.isNullOrEmpty()) {
                        return m
                    }
                }
                return null
            }

            val poseStackOwner = "com.mojang.blaze3d.vertex.PoseStack"
            val obfLast = pickObfMethodName(poseStackOwner, "last", 0, emptyList()) ?: "last"

            val poseInnerOwner = "com.mojang.blaze3d.vertex.PoseStack\$Pose"
            var obfPoseOnInner = pickObfMethodName(poseInnerOwner, "pose", 0, emptyList())
            if (obfPoseOnInner.isNullOrEmpty() || obfPoseOnInner == "pose") {
                val obfInnerA = deobfToObfClassMap[poseInnerOwner]?.replace('$', '.')
                val obfInnerB = deobfToObfClassMap["com.mojang.blaze3d.vertex.PoseStack.Pose"]?.replace('$', '.')
                obfPoseOnInner = obfPoseOnInner
                    ?: obfInnerA?.let { cls ->
                        try {
                            val k = Class.forName(cls)
                            k.declaredMethods.firstOrNull { it.parameterCount == 0 && (it.returnType.name == "org.joml.Matrix4f" || it.returnType.name == "com.mojang.math.Matrix4f") }?.name
                        } catch (_: Throwable) {
                            null
                        }
                    }
                            ?: obfInnerB?.let { cls ->
                        try {
                            val k = Class.forName(cls)
                            k.declaredMethods.firstOrNull { it.parameterCount == 0 && (it.returnType.name == "org.joml.Matrix4f" || it.returnType.name == "com.mojang.math.Matrix4f") }?.name
                        } catch (_: Throwable) {
                            null
                        }
                    }
                            ?: run {
                        val obfOuter = deobfToObfClassMap[poseStackOwner]?.replace('$', '.')
                        try {
                            val outerCls = if (obfOuter != null) Class.forName(obfOuter) else null
                            outerCls?.declaredClasses?.firstNotNullOfOrNull { inner ->
                                inner.declaredMethods.firstOrNull { it.parameterCount == 0 && (it.returnType.name == "org.joml.Matrix4f" || it.returnType.name == "com.mojang.math.Matrix4f") }?.name
                            }
                        } catch (_: Throwable) {
                            null
                        }
                    }
            }

            val lines = source.lines().toMutableList()
            for (i in lines.indices) {
                val prot = protectedByLine[i] ?: emptyList()
                var line = lines[i]

                chainedFieldRewritesByLine[i]
                    ?.sortedByDescending { it.first.length }
                    ?.forEach { (from, to) ->
                        line = safeReplaceLine(line, prot, Regex("""\b${Regex.escape(from)}\b"""), to)
                    }

                methodCallRewritesByLine[i]
                    ?.sortedByDescending { it.first.length }
                    ?.forEach { (from, to) ->
                        val before = line
                        line = safeReplaceLine(
                            line, prot,
                            Regex("""(\?\.|\.)\s*${Regex.escape(from)}\s*\(""")
                        ) { mr -> "${mr.groupValues[1]}$to(" }
                    }

                methodCallRewritesByLine[i]
                    ?.forEach { (from, to) ->
                        line = safeReplaceLine(
                            line, prot,
                            Regex("""\b${Regex.escape(from)}\s*\(""")
                        ) { _ -> "$to(" }
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
                    val rx = Regex("""\b(?:[A-Za-z_]\w*\.)*${Regex.escape(from)}\b""")
                    line = safeReplaceLine(line, prot, rx, to)
                }

                qualifiedClassReplacements.keys
                    .sortedByDescending { it.length }
                    .forEach { from ->
                        val to = qualifiedClassReplacements[from]!!
                        line = safeReplaceLine(line, prot, Regex("""\b${Regex.escape(from)}\b"""), to)
                    }

                run {
                    importQualifierMap.forEach { (simple, fq) ->
                        line = safeReplaceLine(line, prot, Regex("""(?<!\w)${Regex.escape(simple)}(?=\s*\.)"""), fq)
                    }
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

                run {
                    foundClasses.forEach { deobf ->
                        val key = if (deobfToObfClassMap.containsKey(deobf)) deobf else deobf.substringAfterLast('$')
                        val obf = deobfToObfClassMap[key]
                        if (obf != null) {
                            val to = obf.replace('$', '.')
                            val rx = Regex("""(?<!\.)\b${Regex.escape(deobf)}\b""")
                            line = safeReplaceLine(line, emptyList(), rx, to)
                        }
                    }
                }

                foundFields.forEach { (className, deobfField) ->
                    val obfClass0 = deobfToObfClassMap[className] ?: className
                    val obfClass = obfClass0.replace('$', '.')
                    val obf = deobfToObfFieldMap[className]?.get(deobfField)
                    if (obf != null) {
                        line = safeReplaceLine(
                            line, prot,
                            Regex(
                                """(${Regex.escape(obfClass)}|${Regex.escape(className.substringAfterLast('.'))})\s*\.\s*${
                                    Regex.escape(
                                        deobfField
                                    )
                                }\b"""
                            )
                        ) { mr -> "${mr.groupValues[1]}.$obf" }
                        if (!isDeclarationLine(line, deobfField)) {
                            line = safeReplaceLine(line, prot, Regex("""\b${Regex.escape(deobfField)}\b"""), obf)
                        }
                    }
                }

                val ambiguousMethodNames: Set<String> = foundMethods
                    .groupBy { it.method }
                    .filter { entry -> entry.value.map { it.className }.distinct().size > 1 }
                    .keys

                foundMethods.forEach { (className, deobfMethod, arity, hints) ->
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
                        if (deobfMethod !in ambiguousMethodNames && !isDeclarationLine(line, deobfMethod)) {
                            line = safeReplaceLine(
                                line, prot,
                                Regex("""(?<![\w.])${Regex.escape(deobfMethod)}\s*\("""),
                                "$obfMethod("
                            )
                        }
                    }
                }

                if (!obfPoseOnInner.isNullOrEmpty() && obfPoseOnInner != "pose") {
                    val before = line
                    val chainRx = Regex("""(${Regex.escape(obfLast)}\s*\(\))\s*(\?\.|\.)\s*pose\s*\(""")
                    line = safeReplaceLine(line, prot, chainRx) { mr ->
                        "${mr.groupValues[1]}${mr.groupValues[2]}$obfPoseOnInner("
                    }
                }

                run {
                    importQualifierMap.forEach { (simple, fq) ->
                        if (line.contains("$simple.")) {
                            line = safeReplaceLine(line, prot, Regex("""(?<!\w)${Regex.escape(simple)}(?=\s*\.)"""), fq)
                        }
                    }
                }

                line = restoreLine(line, saved)
                lines[i] = line
            }

            return lines.joinToString("\n").replace("net.field_5645.", "net.minecraft.")
        }

    }
}
