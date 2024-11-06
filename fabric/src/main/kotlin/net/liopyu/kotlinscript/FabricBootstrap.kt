
package net.liopyu.kotlinscript

import net.fabricmc.api.ModInitializer
class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        KotlinScriptInit.preInitialize()
    }


}