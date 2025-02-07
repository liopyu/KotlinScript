

package net.liopyu.kotlinscript


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
    fun testKotlinSuggestions(suggestions: List<Triple<String, String, String?>>): List<String> {
        val validAndRelevantFailures = mutableListOf<String>()

        suggestions.forEach { (qualifiedMethod, directMethod, classMethod) ->
            evaluateSuggestion(qualifiedMethod, validAndRelevantFailures, "Qualified Method")
            evaluateSuggestion(directMethod, validAndRelevantFailures, "Direct Method")
            evaluateSuggestion(classMethod, validAndRelevantFailures, "Class Method")
        }

        return validAndRelevantFailures.distinct()
    }
    fun enrichSuggestionsWithMetadata(suggestions: List<String>): List<KotlinObject> {
        return suggestions
            .filter { it.startsWith("kotlin.") } // Filter only suggestions starting with "kotlin."
            .map { suggestion ->
                val (simpleName, path) = analyzeSuggestion(suggestion)
                KotlinObject(
                    fullyQualifiedName = suggestion,
                    simpleName = simpleName,
                    source = suggestion, // Add source if applicable
                    type = deriveType(suggestion), // Determine type
                    path = path
                )
            }
    }


    // Analyze the suggestion to extract the simple name and path
    private fun analyzeSuggestion(suggestion: String): Pair<String, String?> {
        val simpleName = suggestion.substringAfterLast(".") // Extract simple name
        val path = suggestion.substringBeforeLast(".", missingDelimiterValue = "") // Extract path
        return Pair(simpleName, if (path.isBlank()) null else path)
    }

    // Derive the type of the suggestion (Class, Method, Property, etc.)
    private fun deriveType(suggestion: String): String {
        return when {
            suggestion.contains("(") -> "Method" // Heuristic: If it has parentheses, it's likely a method
            suggestion.contains(".") -> "Property" // Heuristic: If it has a dot, it's likely a property
            else -> "Class" // Default to class
        }
    }

    private fun evaluateSuggestion(
        suggestion: String?,
        validAndRelevantFailures: MutableList<String>,
        type: String
    ) {
        if (!suggestion.isNullOrBlank()) {
            try {
                val ks = KS(suggestion)
                val result = ks.eval()

                if (result is ResultWithDiagnostics.Success) {
                    validAndRelevantFailures.add(suggestion)
                   info("Valid ($type): $suggestion")
                } else if (result is ResultWithDiagnostics.Failure) {
                    processFailure(result, suggestion, validAndRelevantFailures)
                }
            } catch (e: Exception) {
                logger.error("Error processing $type suggestion: $suggestion", e)
            }
        }
    }

    private fun processFailure(
        result: ResultWithDiagnostics<*>,
        suggestion: String,
        validAndRelevantFailures: MutableList<String>
    ) {
        val errors = result.reports.map { it.message }

        // Check for flagged errors (irrelevant suggestions)
        val hasFlaggedError = errors.any { error ->
            error.contains("Unresolved reference") ||
                    error.contains("Expecting an element") ||
                    (error.contains("Cannot access") && error.contains("it is internal in "))
        }

        if (hasFlaggedError) {
           info("Filtered out: $suggestion- ${errors.lastOrNull() ?: "Unknown error"}")
            return // Skip further checks if a flagged error is found
        }

        // If no flagged errors, add suggestion as relevant failure
        validAndRelevantFailures.add(suggestion)
        info("Relevant failure: $suggestion - ${errors.lastOrNull() ?: "Unknown error"}")
    }

    fun preInitialize() {
        KotlinScriptLoader.loadScripts()
    }
}


