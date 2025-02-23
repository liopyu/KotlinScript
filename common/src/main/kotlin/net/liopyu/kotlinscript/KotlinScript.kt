


package net.liopyu.kotlinscript

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.liopyu.kotlinscript.util.KotlinObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.script.experimental.api.ResultWithDiagnostics


object KotlinScriptInit {
    val logger: Logger = LogManager.getLogger()
    val debugLogging: Boolean = false
    fun info(i: String){
        if (debugLogging)
        logger.info(i)
    }
    fun testKotlinSuggestions(suggestions: List<KotlinObject>): List<KotlinObject> = runBlocking {
        suggestions.map { suggestion ->
            async(Dispatchers.Default) {
                if (isValidSuggestion(suggestion.fullyQualifiedName, suggestion.type) ||
                    isValidSuggestion(suggestion.simpleName, suggestion.type) ||
                    isValidSuggestion(suggestion.source, suggestion.type)) {
                    suggestion
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
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


