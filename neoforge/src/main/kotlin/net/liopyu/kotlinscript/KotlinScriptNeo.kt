package net.liopyu.kotlinscript

import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager

@Mod("kotlinscript")
object KotlinScriptNeo {
    private val logger = LogManager.getLogger("kotlinscript")

    init {
        logger.info("Loading kotlinscript")
        KotlinScriptInit.preInitialize()
    }
}
