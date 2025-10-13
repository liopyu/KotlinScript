package net.liopyu.kotlinscript

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.script.experimental.api.ResultWithDiagnostics

object KotlinScriptInit {
    val logger: Logger = LogManager.getLogger()
    val debugLogging: Boolean = false
    fun info(i: String) {
        if (debugLogging)
            logger.info(i)
    }

    fun preInitialize() {
        KotlinScriptLoader.loadScripts()
    }

    private val importCache = ConcurrentHashMap<String, Boolean>()
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())


    fun isValidImport(suggestion: String?, type: String): CompletableFuture<Boolean> {
        if (suggestion.isNullOrBlank()) return CompletableFuture.completedFuture(false)

        importCache[suggestion]?.let { return CompletableFuture.completedFuture(it) }

        if (
            suggestion.contains("package-info")) {
            importCache[suggestion] = false
            return CompletableFuture.completedFuture(false)
        }

        val suggestionToEval = "import $suggestion"

        return CompletableFuture.supplyAsync({
            try {
                val ks = KSText(suggestionToEval)
                val result = ks.eval()

                val isValid = when (result) {
                    is ResultWithDiagnostics.Success -> true
                    is ResultWithDiagnostics.Failure -> {
                        !result.reports.any { it.message.contains("Unresolved reference") }
                    }

                    else -> false
                }

                importCache[suggestion] = isValid
                isValid
            } catch (e: Exception) {
                importCache[suggestion] = false
                false
            }
        }, executor)
    }


}


