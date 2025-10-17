package net.liopyu.kotlinscript

import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager

@Mod("kotlinscript")
object KotlinScriptNeo {
    private val logger = LogManager.getLogger("kotlinscript")

    init {
        logger.info("Loading kotlinscript")
        //val loader = KotlinCompilerBootstrap.loader
        //LogUtils.getLogger().info("Bootstrapped Kotlin: kotlin.home=${System.getProperty("kotlin.home")}")


        try {
            KotlinScriptInit.preInitialize()
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }
}
