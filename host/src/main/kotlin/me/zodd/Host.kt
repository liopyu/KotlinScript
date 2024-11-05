package me.zodd

import com.mojang.logging.LogUtils
import net.minecraftforge.fml.common.Mod
import java.io.File

@Mod("kotlinscript")
class Host {
companion object{

    val logger = LogUtils.getLogger()
}

    init {
       /* val jarUrl = this::class.java.getResource("/${this::class.java.name.replace('.', '/')}.class")
        val jarPath = jarUrl?.toString()?.substringAfter("file:")?.substringBeforeLast("!") ?: error("Unable to determine jar path")
        System.setProperty("kotlin.java.stdlib.jar", jarPath)
*/
        logger.info("loading kotlinscript")
        KotlinScriptLoader.loadScripts()
    }
}

fun main() {
    KotlinScriptLoader.loadScripts()
}

