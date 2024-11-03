package me.zodd

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ScriptingConfig(
    @field:Comment("Set true for a placebo")
    val placeholderSetting: Boolean = false,
    @field:Comment("Enables debug logging")
    val extraLogging: Boolean = false,
)