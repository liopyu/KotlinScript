package me.zodd

import org.apache.logging.log4j.Logger
import org.spongepowered.plugin.PluginContainer

class API(container: PluginContainer, logger: Logger) {
    companion object {
        lateinit var container: PluginContainer
        lateinit var logger: Logger
    }

    init {
        Companion.container = container
        Companion.logger = logger
    }
}