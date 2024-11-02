package me.zodd.dsl.command

import org.spongepowered.api.command.parameter.Parameter

sealed interface CommandParameter {
    infix fun <T> String.withType(type: Parameter.Value.Builder<T>): Parameter.Value<T> {
        return type.key(this).build()
    }

    infix fun <T> Parameter.Value.Builder<T>.keyedWith(key: String): Parameter.Value<T> {
        return this.key(key).build()
    }
}