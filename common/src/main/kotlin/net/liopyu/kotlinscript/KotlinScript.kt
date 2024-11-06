

package net.liopyu.kotlinscript


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.*
import java.nio.charset.StandardCharsets
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

object KotlinScriptInit {
    val LOGGER: Logger = LogManager.getLogger()
    fun preInitialize() {
        KotlinScriptLoader.loadScripts()
    }
}


