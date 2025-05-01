package net.liopyu.kotlinscript

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.liopyu.kotlinscript.util.KotlinObject
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

    fun testKotlinSuggestions(suggestions: List<KotlinObject>): List<KotlinObject> = runBlocking {
        suggestions.map { suggestion ->
            async(Dispatchers.Default) {
                val simpleName = suggestion.fullyQualifiedName
                    .substringBefore('(')     // remove any (args)
                    .substringAfterLast('.')  // get just the class name
                if (isValidSuggestion(suggestion.fullyQualifiedName, suggestion.type) ||
                    isValidSuggestion(simpleName, suggestion.type) ||
                    isValidSuggestion(suggestion.source, suggestion.type)
                ) {
                    suggestion
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private val importCache = ConcurrentHashMap<String, Boolean>()
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    fun isValidKotlinScript(suggestion: String?): CompletableFuture<Boolean> {
        if (suggestion.isNullOrBlank()) return CompletableFuture.completedFuture(false)
        val suggestionToEval = "$suggestion"
        return CompletableFuture.supplyAsync({
            try {
                val ks = KS(suggestionToEval)
                val result = ks.eval()

                val isValid = when (result) {
                    is ResultWithDiagnostics.Success -> true
                    is ResultWithDiagnostics.Failure -> {
                        !result.reports.any { it.message.contains("Unresolved reference") }
                    }

                    else -> false
                }
                isValid
            } catch (e: Exception) {
                false
            }
        }, executor)
    }

    fun isValidImport(suggestion: String?, type: String): CompletableFuture<Boolean> {
        if (suggestion.isNullOrBlank()) return CompletableFuture.completedFuture(false)

        // Cache lookup to skip redundant checks
        importCache[suggestion]?.let { return CompletableFuture.completedFuture(it) }

        // Early filtering for known invalid patterns
        if (/*suggestion.startsWith("javafx.") ||
            suggestion.startsWith("com.sun.") ||*/
            suggestion.contains("package-info")) {
            importCache[suggestion] = false
            return CompletableFuture.completedFuture(false)
        }

        val suggestionToEval = "import $suggestion"

        // Async evaluation with caching
        return CompletableFuture.supplyAsync({
            try {
                val ks = KS(suggestionToEval)
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

    private fun isValidSuggestion(suggestion: String?, type: String): Boolean {
        if (suggestion.isNullOrBlank()) return false
        val suggestionToEval = if (type == "method" || type == "lambda") "$suggestion()" else suggestion
        return try {
            val ks = KS(suggestionToEval)
            val result = ks.eval()
            when (result) {
                is ResultWithDiagnostics.Success -> {
                    info("Valid ($type): $suggestionToEval")
                    true
                }

                is ResultWithDiagnostics.Failure -> {
                    val errors = result.reports.map { it.message }
                    val hasFlaggedError = errors.any { error ->
                        error.contains("Unresolved reference") ||
                                error.contains("Expecting an element") ||
                                (error.contains("Cannot access") && error.contains("it is internal in "))
                    }
                    if (hasFlaggedError) {
                        info("Filtered out: $suggestionToEval - ${errors.lastOrNull() ?: "Unknown error"}")
                        false
                    } else {
                        info("Relevant failure: $suggestionToEval - ${errors.lastOrNull() ?: "Unknown error"}")
                        true
                    }
                }

                else -> false
            }
        } catch (e: Exception) {
            logger.error("Error processing suggestion: $suggestionToEval", e)
            false
        }
    }

    fun preInitialize() {
        KotlinScriptLoader.loadScripts()
    }
}


