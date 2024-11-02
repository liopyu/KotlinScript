package me.zodd.dsl.command

import me.zodd.API
import me.zodd.RegistrationHelper.register
import me.zodd.annotations.ScriptDsl
import me.zodd.onRegisterCommandsParameterized

@ScriptDsl
object CommandManager : CommandArgument, CommandContext {

    operator fun invoke(initializer: CommandManager.() -> Unit) = onRegisterCommandsParameterized {
        this@CommandManager.initializer()
        register(API.container, CommandBuilder.builtCommands)
    }

    fun command(name: String, initializer: CommandBuilder.(name: String) -> Unit): DslCommand {
        val builder = CommandBuilder()
        builder.aliases += name
        builder.initializer(name)
        return builder.buildCommand()
    }
}