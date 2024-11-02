package me.zodd

import me.zodd.RegistrationHelper.registerListener
import org.spongepowered.api.Engine
import org.spongepowered.api.command.Command.Parameterized
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.block.ChangeBlockEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.event.entity.DestructEntityEvent
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.entity.MoveEntityEvent
import org.spongepowered.api.event.entity.SpawnEntityEvent
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StartingEngineEvent
import org.spongepowered.api.event.lifecycle.StoppedGameEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent

// === EVENTS ===

// INTERACTION

fun onBlockLeftClick(executor: InteractBlockEvent.Primary.() -> Unit) = registerListener(executor)

fun onBlockRightClick(executor: InteractBlockEvent.Secondary.() -> Unit) = registerListener(executor)

fun onEntityLeftClick(executor: InteractEntityEvent.Primary.() -> Unit) = registerListener(executor)

fun onEntityRightClick(executor: InteractEntityEvent.Secondary.() -> Unit) = registerListener(executor)

// MOVE

fun onEntityMove(executor: MoveEntityEvent.() -> Unit) = registerListener(executor)

fun onPlayerMove(executor: MoveEntityEvent.() -> Unit) =
    onEntityMove { if (entity().type() == EntityTypes.PLAYER) executor() }

// BLOCKS

fun onBlockChange(executor: ChangeBlockEvent.() -> Unit) = registerListener(executor)

// ENTITIES

fun onEntitySpawn(executor: SpawnEntityEvent.() -> Unit) = registerListener(executor)

fun onEntityDamage(executor: DamageEntityEvent.() -> Unit) = registerListener(executor)

fun onEntityRemove(executor: DestructEntityEvent.() -> Unit) = registerListener(executor)

fun onEntityDeath(executor: DestructEntityEvent.Death.() -> Unit) = registerListener(executor)

// PLAYERS

fun onPlayerLogin(executor: ServerSideConnectionEvent.Login.() -> Unit) = registerListener(executor)

fun onPlayerJoin(executor: ServerSideConnectionEvent.Join.() -> Unit) = registerListener(executor)

fun onPlayerLeave(executor: ServerSideConnectionEvent.Disconnect.() -> Unit) = registerListener(executor)

fun onPlayerDamage(executor: DamageEntityEvent.() -> Unit) = onEntityDamage { if (entity() is Player) executor() }

fun onPlayerDeath(executor: DestructEntityEvent.Death.() -> Unit) =
    onEntityDeath { if (entity() is Player) executor() }

// SERVER

fun onServerStarting(executor: StartingEngineEvent<Engine>.() -> Unit) = registerListener(executor)

fun onServerStarted(executor: StartedEngineEvent<Engine>.() -> Unit) = registerListener(executor)

fun onServerStopping(executor: StoppingEngineEvent<Engine>.() -> Unit) = registerListener(executor)

fun onServerStopped(executor: StoppedGameEvent.() -> Unit) = registerListener(executor)

// COMMANDS

fun onRegisterCommandsParameterized(executor: RegisterCommandEvent<Parameterized>.() -> Unit) =
    registerListener(executor)

// OTHERS

//fun onScriptsUnload(executor: UnloadScriptsEvent.() -> Unit) = registerListener(executor)

