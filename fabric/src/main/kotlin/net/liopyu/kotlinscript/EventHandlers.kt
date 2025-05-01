package net.liopyu.kotlinscript

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

object EventHandlers {
    fun <T> register(event: Event<T>, handler: (T) -> Unit) {
        event.register(handler)
    }
}

interface Event<T> {
    fun register(callback: (T) -> Unit)
}

object ClientEvents {
    val END_TICK = object : Event<Minecraft> {
        override fun register(callback: (Minecraft) -> Unit) {
            ClientTickEvents.END_CLIENT_TICK.register(callback)
        }
    }
}

