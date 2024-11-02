package me.zodd

import com.mojang.logging.LogUtils
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

//import thedarkcolour.kotlinforforge.forge.MOD_CONTEXT

@Mod("kotlinscript")
class Host {

    companion object {
        val logger = LogUtils.getLogger()
    }

    init {
        //val type = KotlinType
        //KotlinScriptLoader.loadScripts()
        // Register mod setup events
        /*val modEventBus = MOD_CONTEXT.getKEventBus()
        modEventBus.addListener(this::setup)

        // Register server events
        MinecraftForge.EVENT_BUS.register(this)

        logger.info("Kotlin Scripting Host initializing...")*/
    }

    private fun setup(event: FMLCommonSetupEvent) {
        // Register any common setup tasks here
        logger.info("Setting up common mod components...")
    }

    @SubscribeEvent
    fun onServerStart(event: ServerStartingEvent) {
        logger.info("Starting Kotlin Scripting Host server...")
        KotlinScriptLoader.loadScripts()
        logger.info("Finished loading scripts...")
    }

    @SubscribeEvent
    fun onServerStop(event: ServerStoppedEvent) {
        // Handle server shutdown if needed
        logger.info("Stopping Kotlin Scripting Host server...")
    }

    // Additional event listeners can be added as needed
}
