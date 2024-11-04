package me.zodd

import com.mojang.logging.LogUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

fun main() {
    KotlinScriptLoader.loadScripts()
}
@Mod("kotlinscript")
class Host {
    companion object {
        val logger = LogUtils.getLogger()
    }

    init {
        KotlinScriptLoader.loadScripts()
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus
        modEventBus.addListener(this::setup)

        // Register server events
        MinecraftForge.EVENT_BUS.register(this)

        logger.info("Kotlin Scripting Host initializing...")
    }
    private fun setup(event: FMLCommonSetupEvent) {

    }
    @SubscribeEvent
    fun onServerStart(event: PlayerEvent.ItemPickupEvent) {
        KotlinScriptLoader.loadScripts()
    }
}



