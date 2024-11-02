package me.zodd.dsl.command

import org.spongepowered.api.command.parameter.managed.Flag

sealed interface CommandFlag {
    infix fun String.buildFlag(permission: String): Flag {
        return asFlag(permission).build()
    }

    infix fun String.asFlag(permission: String): Flag.Builder {
        return Flag.builder().aliases(this.split(",")).setPermission(permission)
    }
}