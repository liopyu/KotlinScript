package me.zodd

import kotlin.script.experimental.api.ScriptDiagnostic

internal data class LogInfo(
    val scriptName: String,
    val diagnostic: List<ScriptDiagnostic>,
) {
    private val fileExt = ".plugin.kts"
    private val id = scriptName.removeSuffix(fileExt).uppercase()
    //Known messages deemed of little value to the End User
    private val listOfKnownMessages = listOf(
        "Using JVM IR backend",
        "Using new faster version of JAR FS: it should make your build faster, but the new implementation is experimental",
    )

    private fun handleFailure(string: String): String? {
        //This may be valuable as a configurable option
        if (string.startsWith("Loading modules:")) {
            return null
        }
        listOfKnownMessages.forEach {
            if (string.contains(it)) return null
        }
        return string
    }

    private fun generateReport(): String {
        val messageMerge = mutableListOf<String>()
        diagnostic.forEach {
            val msg = handleFailure(it.message) ?: return@forEach
            messageMerge.add(msg)
        }
        return messageMerge.joinToString("\n")
    }

    fun printLog() {
        val logTitle = "[${id}]"
        Host.logger.info(
            "\n" + """
            ################$logTitle###############
            ${generateReport()}
            ###############################${"#".repeat(logTitle.length)}
            """.trimIndent()
        )
    }
}