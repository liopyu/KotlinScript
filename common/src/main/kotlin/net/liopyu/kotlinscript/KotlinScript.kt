

package net.liopyu.kotlinscript


import com.mojang.logging.LogUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.script.experimental.api.ResultWithDiagnostics

object KotlinScriptInit {
    val LOGGER: Logger = LogManager.getLogger()
    fun testKotlinSuggestions(suggestions: List<String>, basePackage: String = "kotlin."): List<String> {
        val validAndRelevantFailures = mutableListOf<String>()
        val logger = LogUtils.getLogger()

        suggestions.forEach { suggestion ->
            try {
                val baseSuggestion = suggestion.removePrefix(basePackage)
                val ksBase = KS(baseSuggestion)
                val ksFull = KS(suggestion)

                val resultBase = ksBase.eval()
                val resultFull = ksFull.eval()

                if (resultBase is ResultWithDiagnostics.Success || resultFull is ResultWithDiagnostics.Success) {
                    validAndRelevantFailures.add(suggestion)
                    logger.info("Valid: $suggestion")
                } else if (resultBase is ResultWithDiagnostics.Failure && resultFull is ResultWithDiagnostics.Failure) {
                    val errorsBase = resultBase.reports.map { it.message }
                    val errorsFull = resultFull.reports.map { it.message }

                    val isUnresolvedReferenceBase = errorsBase.any { it.contains("Unresolved reference") }
                    val isUnresolvedReferenceFull = errorsFull.any { it.contains("Unresolved reference") }
                    val isExpectingElementBase = errorsBase.any { it.contains("Expecting an element") }
                    val isExpectingElementFull = errorsFull.any { it.contains("Expecting an element") }
                    val isInternalAccessBase = errorsBase.any { it.contains("Cannot access") && it.contains("it is internal in") }
                    val isInternalAccessFull = errorsFull.any { it.contains("Cannot access") && it.contains("it is internal in") }

                    if (isUnresolvedReferenceBase || isExpectingElementBase || isInternalAccessBase ||
                        isUnresolvedReferenceFull || isExpectingElementFull || isInternalAccessFull
                    ) {
                        logger.info("Filtered out: $suggestion")
                    } else {
                        validAndRelevantFailures.add(suggestion)
                        logger.info("Relevant failure: $suggestion")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing suggestion: $suggestion", e)
            }
        }

        return validAndRelevantFailures
    }


    fun preInitialize() {
        KotlinScriptLoader.loadScripts()
    }
}


