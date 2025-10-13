package net.liopyu.kotlinscript

import com.mojang.logging.LogUtils
import org.slf4j.Logger
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarFile
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private val logger = LogUtils.getLogger()

private val sharedScriptDir = File("config/scripts")
val instanceDir = File(System.getProperty("user.dir"))
val sourcesDir = File(instanceDir, "kotlinsources")
val mappingsFile = File(sourcesDir, "mappings.tiny")

private const val TARGET_RES = "org/jetbrains/kotlin/cli/jvm/compiler/KotlinCoreEnvironment\$Companion.class"
private const val TARGET_CLASS = "org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment"

class LoggingUrlCl(urls: Array<URL?>?, parent: ClassLoader?) : URLClassLoader(urls, parent) {
    private fun norm(name: String): String = if (name.startsWith("/")) name.substring(1) else name
    private fun logHit(name: String, url: URL) {
        LOGGER.info("KS | getResource('{}') -> {} via {}", name, url, this)
        val st = Thread.currentThread().stackTrace
        if (st.size > 4) LOGGER.info("    at {}", st[4])
    }

    override fun getResource(name: String): URL? {
        val n = norm(name)
        var u = findResource(n)
        if (u != null) {
            logHit(name, u); return u
        }
        u = super.getResource(n)
        if (u != null) logHit(name, u)
        return u
    }

    override fun getResources(name: String): Enumeration<URL> {
        val n = norm(name)
        val all = ArrayList<URL>()
        val child = findResources(n)
        while (child.hasMoreElements()) all.add(child.nextElement())
        val parentEnum = if (parent != null) parent.getResources(n) else getSystemResources(n)
        while (parentEnum.hasMoreElements()) all.add(parentEnum.nextElement())
        return Collections.enumeration(all)
    }

    override fun getResourceAsStream(name: String) = getResource(name)?.openStream() ?: super.getResourceAsStream(name)

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        if (name.startsWith("org.jetbrains.kotlin.")) {
            synchronized(getClassLoadingLock(name)) {
                var c = findLoadedClass(name)
                if (c == null) {
                    try {
                        c = findClass(name)
                    } catch (_: ClassNotFoundException) {
                    }
                    if (c == null) c = super.loadClass(name, false)
                }
                if (resolve) resolveClass(c)
                return c
            }
        }
        return super.loadClass(name, resolve)
    }

    companion object {
        private val LOGGER: Logger = LogUtils.getLogger()
    }
}

private fun normalizeToJarFileUrl(selfUrl: URL?): URL? {
    if (selfUrl == null) return null
    val raw = selfUrl.toString()
    val noBang = raw.removeSuffix("!/")
    return if (noBang.startsWith("union:")) {
        val jarPart = noBang.removePrefix("union:").substringBefore("%23")
        val fileUrl = URL("file:" + jarPart)
        logger.info("KS | normalized union -> file URL: $fileUrl   (from $raw)")
        fileUrl
    } else {
        URL(noBang).also { logger.info("KS | using self URL as-is for compiler: $it") }
    }
}

private fun resName(name: String) = name.removePrefix("/")

private fun clGetResource(cl: ClassLoader, name: String): URL? =
    cl.getResource(resName(name))

private fun clGetResourceAsStream(cl: ClassLoader, name: String) =
    cl.getResourceAsStream(resName(name))

private fun extractNestedJars(selfJar: File): List<File> {
    val cacheBase = File(instanceDir, "config/kotlinscript/jij-cache")
    val stamp = "${selfJar.nameWithoutExtension}_${selfJar.length()}_${selfJar.lastModified()}"
    val cacheDir = File(cacheBase, stamp)
    cacheDir.mkdirs()
    val out = mutableListOf<File>()
    JarFile(selfJar).use { jf ->
        val en = jf.entries()
        while (en.hasMoreElements()) {
            val e = en.nextElement()
            val n = e.name
            if (!e.isDirectory && (n.startsWith("META-INF/jars/") || n.startsWith("META-INF/libraries/") || n.startsWith(
                    "META-INF/jarjar/"
                )) && n.endsWith(".jar")
            ) {
                val target = File(cacheDir, n.substringAfterLast('/'))
                if (!target.exists() || target.length() != e.size) {
                    jf.getInputStream(e).use { ins ->
                        Files.copy(ins, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                out += target
            }
        }
    }
    return out
}

private data class CompilerEnv(val cl: URLClassLoader, val classpath: List<File>)

// --- buildCompilerEnv(): only the probe line changed to use clGetResource ---
private fun buildCompilerEnv(): CompilerEnv {
    val self = KS::class.java.protectionDomain?.codeSource?.location
    val jarFileUrl = normalizeToJarFileUrl(self)
        ?: File("config/scripts").toURI().toURL()
            .also { logger.warn("KS | could not resolve self code-source; falling back to $it") }
    val selfFile = File(jarFileUrl.toURI())
    val cacheBase = File(instanceDir, "config/kotlinscript/jij-cache")
    System.setProperty("kotlin.home", cacheBase.absolutePath)
    System.setProperty("kotlin.compiler.jar", selfFile.absolutePath)
    System.setProperty("kotlin.java.stdlib.jar", selfFile.absolutePath)
    System.setProperty("kotlin.java.reflect.jar", selfFile.absolutePath)
    System.setProperty("kotlin.script.runtime.jar", selfFile.absolutePath)

    val extracted = extractNestedJars(selfFile)
    val urls = ArrayList<URL>()
    urls += selfFile.toURI().toURL()
    extracted.forEach { urls += it.toURI().toURL() }
    val parent = KS::class.java.classLoader
    val cl = LoggingUrlCl(urls.toTypedArray(), parent)
    logger.info("KS | compilerCl URLs:"); cl.urLs.forEach { logger.info("  -> $it") }
    logger.info("KS | CL[compiler]: $cl (parent=$parent)")
    // changed:
    logger.info("KS | compilerCl.getResource('$TARGET_RES'): ${clGetResource(cl, TARGET_RES)}")
    try {
        val kce = Class.forName(TARGET_CLASS, false, cl)
        logger.info("KS | loaded ${kce.name} via $cl")
        kce.declaredClasses.firstOrNull { it.simpleName == "Companion" }
            ?.let { logger.info("KS | found inner: ${it.name} via ${it.classLoader}") }
        try {
            val pu = Class.forName("org.jetbrains.kotlin.utils.PathUtil", true, cl)
            val m = pu.getMethod("getResourcePathForClass", Class::class.java)
            val jarFile = m.invoke(null, kce) as File
            logger.info("KS | PathUtil.getResourcePathForClass(KCE) -> $jarFile (exists=${jarFile.exists()})")
        } catch (pt: Throwable) {
            logger.error("KS | PathUtil probe failed: ${pt::class.java.name}: ${pt.message}")
        }
    } catch (t: Throwable) {
        logger.error("KS | Class.forName failed: ${t::class.java.name}: ${t.message}")
    }
    return CompilerEnv(cl, extracted + selfFile)
}

// --- chainProbe(): now uses the wrappers everywhere ---
private fun chainProbe(tag: String, cl: ClassLoader, res: String) {
    fun p(s: String) = logger.info("KS | $tag $s")
    val segments = res.split('/').filter { it.isNotEmpty() }
    var prefix = ""
    for ((i, seg) in segments.withIndex()) {
        prefix += "$seg/"
        val key = prefix.removeSuffix("/")
        val hit = clGetResource(cl, key)
        p("chain[$i] '$prefix' -> $hit")
        clGetResourceAsStream(cl, key)?.use { p("chain[$i] read=32") }
    }
    val exactNoSlash = clGetResource(cl, res)
    p("exact(noSlash) -> $exactNoSlash")
    val withSlash = if (res.startsWith("/")) res else "/$res"
    val exactSlash = clGetResource(cl, withSlash)
    p("exact(withSlashNormalized) -> $exactSlash")
}


private val compilerEnv = buildCompilerEnv()
val compilerCl: URLClassLoader = compilerEnv.cl
private val compilerClasspathFiles: List<File> = compilerEnv.classpath
private val sharedCompilationConfig =
    createJvmCompilationConfigurationFromTemplate<PluginScript> {
        compilerOptions("-jvm-target", "21")

        jvm {
            // keep explicit files you built from the JIJ cache + self jar
            dependencies(JvmDependency(compilerClasspathFiles))

            // NEW: also lift the *entire* classpath from the URLClassLoader we created
            // (this mirrors what kotlinc expects internally and fixes the “Resource not found …$Companion.class”)
            dependenciesFromClassloader(
                classLoader = compilerCl,
                wholeClasspath = true
            )
        }
    }

// 2) makeHost(): no functional change, but keep it here for completeness
private fun makeHost(): ScriptingHostConfiguration = ScriptingHostConfiguration {
    jvm { baseClassLoader(compilerCl) }
}

// 3) makeEvalConfig(): ensure eval also uses the same base loader
private fun makeEvalConfig(context: Map<String, Any>) = ScriptEvaluationConfiguration {
    jvm { baseClassLoader(compilerCl) }
    if (context.isNotEmpty()) providedProperties(context)
}


data class KS(val scriptFile: File) {
    companion object {
        var globalBindings: Map<String, Any> = emptyMap()
        val defaultImports = listOf("net.liopyu.kotlinscript.util.KUtils", "com.mojang.logging.LogUtils")
    }

    fun eval(context: Map<String, Any> = emptyMap()): ResultWithDiagnostics<EvaluationResult> {
        val rawScript = scriptFile.absoluteFile.readText().trimIndent()
        val compilationConfig = sharedCompilationConfig.with {
            if (context.isNotEmpty()) set(providedProperties, context.mapValues { KotlinType(it.value::class) })
        }
        val evalConfig = makeEvalConfig(context)
        val prev = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = compilerCl
        logger.info("KS | TCCL -> compilerCl: $compilerCl")
        chainProbe("chain[pre-eval:compilerCl]", compilerCl, TARGET_RES)
        chainProbe(
            "chain[pre-eval:definingCl(${KS::class.java.classLoader::class.java.simpleName})]",
            KS::class.java.classLoader,
            TARGET_RES
        )
        chainProbe(
            "chain[pre-eval:TCCL(${Thread.currentThread().contextClassLoader::class.java.simpleName})]",
            Thread.currentThread().contextClassLoader,
            TARGET_RES
        )
        chainProbe("chain[pre-eval:SystemCL]", ClassLoader.getSystemClassLoader(), TARGET_RES)
        return try {
            val result =
                BasicJvmScriptingHost(makeHost()).eval(rawScript.toScriptSource(), compilationConfig, evalConfig)
            result.reports.forEach { r -> logger.info("KS | report [${r.severity}] ${r.message} @ ${r.location}") }
            result
        } finally {
            Thread.currentThread().contextClassLoader = prev
            logger.info("KS | TCCL restored: $prev")
        }
    }
}

data class KSText(val script: String) {
    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        val prev = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = compilerCl
        logger.info("KS | TCCL -> compilerCl for text-eval: $compilerCl")
        return try {
            val host = makeHost()
            BasicJvmScriptingHost(host).eval(
                script.toScriptSource(),
                sharedCompilationConfig,
                ScriptEvaluationConfiguration { jvm { baseClassLoader(compilerCl) } }
            )
        } finally {
            Thread.currentThread().contextClassLoader = prev
            logger.info("KS | TCCL restored: $prev")
        }
    }
}
