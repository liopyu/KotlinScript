package me.zodd.dsl.command

import net.kyori.adventure.text.Component
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.parameter.Parameter
import org.spongepowered.api.command.parameter.managed.Flag
import org.spongepowered.api.command.parameter.CommandContext as SpongeContext

sealed interface CommandContext {
    infix fun <T> SpongeContext.requireOne(param: Parameter.Value<T>): T = this.requireOne(param)

    fun SpongeContext.success(): CommandResult = CommandResult.success()

    infix fun SpongeContext.error(errorMessage: Component): CommandResult = CommandResult.error(errorMessage)

    infix fun SpongeContext.hasFlag(flag: Flag) = this.hasFlag(flag)
}