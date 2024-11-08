
package net.liopyu.kotlinscript

import net.fabricmc.api.ModInitializer
import net.liopyu.kotlinscript.util.TypingsDumper

class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        TypingsDumper.dumpTypingsToJSONFile("net")
        //KotlinScriptInit.preInitialize()

    }


}