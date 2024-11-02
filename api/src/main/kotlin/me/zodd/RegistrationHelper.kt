package me.zodd

import io.leangen.geantyref.TypeToken
import me.zodd.dsl.command.DslCommand
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.Command
import org.spongepowered.api.command.Command.Parameterized
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.EventListenerRegistration
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.plugin.PluginContainer

object RegistrationHelper {
    //Used internally for registration of Events from scripts
    fun <T : Event> registerListener(eventClass: TypeToken<T>, executor: (T) -> Unit) {
        Sponge.eventManager().registerListener(
            EventListenerRegistration.builder(eventClass)
                .order(Order.DEFAULT)
                .listener(executor::invoke)
                .plugin(API.container)
                .build()
        )
    }

    inline fun <reified T : Event> registerListener(noinline executor: T.() -> Unit) =
        registerListener(typeToken<T>(), executor)

    //Allows easy creation of typeToken
    inline fun <reified T : Event> typeToken() = object : TypeToken<T>() {}

    fun RegisterCommandEvent<Command.Parameterized>.register(container: PluginContainer, dslCommand: DslCommand) {
        register(container, dslCommand.command, dslCommand.baseAlias, *dslCommand.remainingAliases)
    }

    fun RegisterCommandEvent<Command.Parameterized>.register(
        container: PluginContainer,
        dslCommands: List<DslCommand>,
    ) {
        dslCommands.forEach { this.register(container, it) }
    }
}