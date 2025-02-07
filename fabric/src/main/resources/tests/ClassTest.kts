/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.drop.DropTable
import com.cobblemon.mod.common.api.entity.Despawner
import com.cobblemon.mod.common.api.entity.PokemonSender
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.entity.PokemonEntityLoadEvent
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveEvent
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveToWorldEvent
import com.cobblemon.mod.common.api.events.pokemon.ShoulderMountEvent
import com.cobblemon.mod.common.api.interaction.PokemonEntityInteraction
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addStandardFunctions
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.api.net.serializers.PlatformTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.PoseTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.StringSetDataSerializer
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.api.spawning.BestSpawner
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.battles.BagItems
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.SuccessfulBattleStart
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.entity.PlatformType
import com.cobblemon.mod.common.entity.PosableEntity
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.generic.GenericBedrockEntity
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonMoveControl
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonNavigation
import com.cobblemon.mod.common.entity.pokemon.ai.goals.*
import com.cobblemon.mod.common.entity.pokemon.effects.EffectTracker
import com.cobblemon.mod.common.entity.pokemon.effects.IllusionEffect
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokemonPacket
import com.cobblemon.mod.common.net.messages.client.ui.InteractPokemonUIPacket
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler.SEND_OUT_DURATION
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.InactivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.ShoulderedState
import com.cobblemon.mod.common.pokemon.ai.FormPokemonBehaviour
import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution
import com.cobblemon.mod.common.pokemon.feature.StashHandler
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty
import com.cobblemon.mod.common.util.*
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules
import com.mojang.serialization.Codec
import java.util.EnumSet
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.util.Mth.clamp
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.Shearable
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.goal.EatBlockGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.ShoulderRidingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@Suppress("unused")
open class PokemonEntity(

    world: Level,
    pokemon: Pokemon = Pokemon().apply { isClient = world.isClientSide },
    type: EntityType<out PokemonEntity> = CobblemonEntities.POKEMON,
) : ShoulderRidingEntity(type, world), PosableEntity, Shearable, Schedulable, ScannableEntity {

    companion object {

        @JvmStatic val SPECIES = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.STRING)
        @JvmStatic val NICKNAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.COMPONENT)
        @JvmStatic val NICKNAME_VISIBLE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val SHOULD_RENDER_NAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val MOVING = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val BEHAVIOUR_FLAGS = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BYTE)
        @JvmStatic val PHASING_TARGET_ID = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val PLATFORM_TYPE = SynchedEntityData.defineId(PokemonEntity::class.java, PlatformTypeDataSerializer)
        @JvmStatic val BEAM_MODE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BYTE)
        @JvmStatic val BATTLE_ID = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)
        @JvmStatic val ASPECTS = SynchedEntityData.defineId(PokemonEntity::class.java, StringSetDataSerializer)
        @JvmStatic val DYING_EFFECTS_STARTED = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val POSE_TYPE = SynchedEntityData.defineId(PokemonEntity::class.java, PoseTypeDataSerializer)
        @JvmStatic val LABEL_LEVEL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val HIDE_LABEL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val UNBATTLEABLE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val COUNTS_TOWARDS_SPAWN_CAP = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val SPAWN_DIRECTION = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic val FRIENDSHIP = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val FREEZE_FRAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic val CAUGHT_BALL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.STRING)
        @JvmStatic val EVOLUTION_STARTED = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        const val BATTLE_LOCK = "battle"
        const val EVOLUTION_LOCK = "evolving"

        fun createAttributes(): AttributeSupplier.Builder = LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE)
            .add(Attributes.ATTACK_KNOCKBACK)
    }

    val removalObservable = SimpleObservable<RemovalReason?>()

    /** A list of observable subscriptions related to this entity that need to be cleaned up when the entity is removed. */
    val subscriptions = mutableListOf<ObservableSubscription<*>>()

    override val schedulingTracker = SchedulingTracker()

    val form: FormData
        get() = pokemon.form
    val behaviour: FormPokemonBehaviour
        get() = form.behaviour

    var pokemon: Pokemon = pokemon
        set(value) {
            value.isClient = this.level().isClientSide
            field = value
            delegate.changePokemon(value)

            //This used to be referring to this.updateEyeHeight, I think this is the best conversion
            // We need to update this value every time the Pokémon changes, other eye height related things will be dynamic.
            this.refreshDimensions()
        }

    var despawner: Despawner<PokemonEntity> = Cobblemon.bestSpawner.defaultPokemonDespawner

    /** The player that caused this Pokémon to faint. */
    var killer: ServerPlayer? = null

    val isEvolving: Boolean
        get() = entityData.get(EVOLUTION_STARTED)
    var evolutionEntity: GenericBedrockEntity? = null

    var ticksLived = 0
    val busyLocks = mutableListOf<Any>()
    val isBusy: Boolean
        get() = busyLocks.isNotEmpty()

    val aspects: Set<String>
        get() = entityData.get(ASPECTS)
    var battleId: UUID?
        get() = entityData.get(BATTLE_ID).orElse(null)
        set(value) = entityData.set(BATTLE_ID, Optional.ofNullable(value))
    val isBattling: Boolean
        get() = entityData.get(BATTLE_ID).isPresent
    val friendship: Int
        get() = entityData.get(FRIENDSHIP)

    var drops: DropTable? = null

    var tethering: PokemonPastureBlockEntity.Tethering? = null

    var queuedToDespawn = false

    var enablePoseTypeRecalculation = true

    /**
     * The amount of steps this entity has traveled.
     */
    var blocksTraveled: Double = 0.0
    var countsTowardsSpawnCap = true

    /**
     * 0 is do nothing,
     * 1 is appearing from a pokeball so needs to be small then grows,
     * 2 is 1 without extra animations like ball throwing and particles, used for pastures and wild capture fails
     * 3 is being captured/recalling so starts large and shrinks.
     */
    var beamMode: Int
        get() = entityData.get(BEAM_MODE).toInt()
        set(value) { entityData.set(BEAM_MODE, value.toByte()) }

    var phasingTargetId: Int
        get() = entityData.get(PHASING_TARGET_ID)
        set(value) { entityData.set(PHASING_TARGET_ID, value) }

    /** The [SpawnCause] that created it, if this was the result of the [BestSpawner]. Note: This will be wiped by chunk-unload. */
    var spawnCause: SpawnCause? = null

    // properties like the above are synced and can be subscribed to for changes on either side

    override val delegate = if (world.isClientSide) {
        PokemonClientDelegate()
    } else {
        PokemonServerDelegate()
    }

    /** The effects that are modifying this entity. */
    var effects: EffectTracker = EffectTracker(this)

    /** The species exposed to the client and used on entity spawn. */
    val exposedSpecies: Species get() = this.effects.mockEffect?.exposedSpecies ?: this.pokemon.species

    /** The form exposed to the client and used for calculating hitbox and height. */
    val exposedForm: FormData get() = this.effects.mockEffect?.exposedForm ?: this.pokemon.form

    /** The aspects exposed to the client */
    val exposedAspects: Set<String> get() = this.effects.mockEffect?.exposedForm?.aspects?.toSet() ?: this.pokemon.aspects

    /** The pokeball exposed to the client. Used for sendout animation. */
    val exposedBall: PokeBall get() = this.effects.mockEffect?.exposedBall ?: this.pokemon.caughtBall

    var platform : PlatformType
        get() = entityData.get(PLATFORM_TYPE)
        set(value) { entityData.set(PLATFORM_TYPE, value) }

    override val struct: ObjectValue<PokemonEntity> = ObjectValue(this).also {

        it.addStandardFunctions()
            .addEntityFunctions(this)
            .addPokemonFunctions(pokemon)
            .addPokemonEntityFunctions(this)
    }


    init {
        delegate.initialize(this)
        delegate.changePokemon(pokemon)
        refreshDimensions()
        addPosableFunctions(struct)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(SPECIES, "")
        builder.define(NICKNAME, Component.empty())
        builder.define(NICKNAME_VISIBLE, true)
        builder.define(SHOULD_RENDER_NAME, true)
        builder.define(MOVING, false)
        builder.define(BEHAVIOUR_FLAGS, 0)
        builder.define(BEAM_MODE, 0)
        builder.define(PLATFORM_TYPE, PlatformType.NONE)
        builder.define(PHASING_TARGET_ID, -1)
        builder.define(BATTLE_ID, Optional.empty())
        builder.define(ASPECTS, emptySet())
        builder.define(DYING_EFFECTS_STARTED, false)
        builder.define(POSE_TYPE, PoseType.STAND)
        builder.define(LABEL_LEVEL, 1)
        builder.define(HIDE_LABEL, false)
        builder.define(UNBATTLEABLE, false)
        builder.define(SPAWN_DIRECTION, ((level().random.nextFloat() * 360F - 180F) * 1000).toInt() / 1000F)
        builder.define(COUNTS_TOWARDS_SPAWN_CAP, true)
        builder.define(FRIENDSHIP, 0)
        builder.define(FREEZE_FRAME, -1F)
        builder.define(CAUGHT_BALL, "")
        builder.define(EVOLUTION_STARTED, false)
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        // "But it's imposs-" shut up nerd, it happens during super construction and that's before delegate is assigned by class construction
        if (delegate != null) {
            delegate.onSyncedDataUpdated(data)
        }

        // common SynchedEntityData handling
        when (data) {
            SPECIES -> refreshDimensions()
            POSE_TYPE -> {
                val value = entityData.get(data) as PoseType
                if (value == PoseType.FLY || value == PoseType.HOVER) {
                    setNoGravity(true)
                } else {
                    setNoGravity(false)
                }
            }

            BATTLE_ID -> {
                if (battleId != null) {
                    busyLocks.remove(BATTLE_LOCK) // Remove in case it's hopped across to another battle, don't want extra battle locks
                    busyLocks.add(BATTLE_LOCK)
                } else {
                    busyLocks.remove(BATTLE_LOCK)
                }
            }

            EVOLUTION_STARTED -> {
                if (isEvolving) {
                    busyLocks.remove(EVOLUTION_LOCK)
                    busyLocks.add(EVOLUTION_LOCK)
                } else {
                    busyLocks.remove(EVOLUTION_LOCK)
                }
            }
        }
    }

    override fun canStandOnFluid(state: FluidState): Boolean {
//        val node = navigation.currentPath?.currentNode
//        val targetPos = node?.blockPos
//        if (targetPos == null || world.getBlockState(targetPos.up()).isAir) {
        return if (state.`is`(FluidTags.WATER) && !isEyeInFluid(FluidTags.WATER)) {
            behaviour.moving.swim.canWalkOnWater || platform != PlatformType.NONE
        } else if (state.`is`(FluidTags.LAVA) && !isEyeInFluid(FluidTags.LAVA)) {
            behaviour.moving.swim.canWalkOnLava
        } else {
            super.canStandOnFluid(state)
        }
//        }
//
//        return super.canWalkOnFluid(state)
    }

    override fun handleEntityEvent(status: Byte) {
        delegate.handleStatus(status)
        super.handleEntityEvent(status)
    }

    override fun sendDebugPackets() {
        super.sendDebugPackets()
        DebugPackets.sendEntityBrain(this)
        DebugPackets.sendGoalSelector(level(), this, this.goalSelector)
        DebugPackets.sendPathFindingPacket(level(), this, this.navigation.path, this.navigation.path?.distToTarget ?: 0F)
    }

    override fun tick() {
        /* Addresses watchdog hanging that is completely bloody inexplicable. */
        yBodyRot = Mth.wrapDegrees(yBodyRot)
        yBodyRotO = Mth.wrapDegrees(yBodyRotO)
        yRot = Mth.wrapDegrees(yRot)
        yRotO = Mth.wrapDegrees(yRotO)
        xRot = Mth.wrapDegrees(xRot)
        xRotO = Mth.wrapDegrees(xRotO)
        yHeadRot = Mth.wrapDegrees(yHeadRot)
        yHeadRotO = Mth.wrapDegrees(yHeadRotO)
        /* I'm sure it's not even us but something altering the logic of the loops in LivingEntity */

        super.tick()

        if (isBattling) {
            // Deploy a platform if a non-wild Pokemon is touching water but not underwater.
            // This can't be done in the BattleMovementGoal as the sleep goal will override it.
            // Clients also don't seem to have correct info about behavior
            if (!level().isClientSide && ticksLived > 5 && platform == PlatformType.NONE
                && ownerUUID != null
                && isInWater && !isUnderWater
                && !exposedForm.behaviour.moving.swim.canBreatheUnderwater && !exposedForm.behaviour.moving.swim.canWalkOnWater
                && !getBehaviourFlag(PokemonBehaviourFlag.FLYING)) {
                platform = PlatformType.getPlatformTypeForPokemon((exposedForm))
            }
        } else {
            // Battle clone destruction
            if (this.beamMode == 0 && this.isBattleClone()) {
                discard()
                return
            }
        }

        // We will be handling idle logic ourselves thank you
        this.setNoActionTime(0)
        if (queuedToDespawn) {
            return remove(RemovalReason.DISCARDED)
        }
        if (evolutionEntity != null) {
            evolutionEntity!!.setPos(pokemon.entity!!.x, pokemon.entity!!.y, pokemon.entity!!.z)
            pokemon.entity!!.navigation.stop()
        }
        delegate.tick(this)
        ticksLived++

        if (ticksLived <= 20) {
            clearRestriction()
            val spawnDirection = entityData.get(SPAWN_DIRECTION).takeIf { it.isFinite() } ?: 0F
            yBodyRot = (spawnDirection * 1000F).toInt() / 1000F
        }

        if (this.tethering != null && !this.tethering!!.box.contains(this.x, this.y, this.z)) {
            this.tethering = null
            this.pokemon.recall()
        }
        //This is so that pokemon in the pasture block are ALWAYS in sync with the pokemon box
        //Before, pokemon entities in pastures would hold an old ref to a pokemon obj and changes to that would not appear to the underlying file
        if (this.tethering != null && age % 20 == 0) {
            // Only for online players
            this.ownerUUID?.let { ownerUUID ->
                val player = level().getPlayerByUUID(ownerUUID) as? ServerPlayer
                if (player != null) {
                    val actualPokemon = Cobblemon.storage.getPC(player)[this.pokemon.uuid]
                    actualPokemon?.let {
                        if (it !== pokemon) {
                            pokemon = it
                        }
                    }
                }
            }
        }

        schedulingTracker.update(1 / 20F)
    }

    fun setMoveControl(moveControl: MoveControl) {
        this.moveControl = moveControl
    }

    /**
     * Prevents water type Pokémon from taking drowning damage.
     */
    override fun canBreatheUnderwater(): Boolean {
        return behaviour.moving.swim.canBreatheUnderwater
    }

    /**
     * Prevents fire type Pokémon from taking fire damage.
     */
    override fun fireImmune(): Boolean {
        return pokemon.isFireImmune()
    }

    /**
     * Prevents flying type Pokémon from taking fall damage.
     */
    override fun causeFallDamage(fallDistance: Float, damageMultiplier: Float, damageSource: DamageSource): Boolean {
        return if (ElementalTypes.FLYING in pokemon.types || pokemon.ability.name == "levitate" || pokemon.species.behaviour.moving.fly.canFly) {
            false
        } else {
            super.causeFallDamage(fallDistance, damageMultiplier, damageSource)
        }
    }

    override fun isInvulnerableTo(damageSource: DamageSource): Boolean {
        // If the entity is busy, it cannot be hurt.
        if (busyLocks.isNotEmpty()) {
            return true
        }

        // Don't let Pokémon be hurt during sendout and recall animations
        if (beamMode != 0) {
            return true
        }

        // Owned Pokémon cannot be hurt by players or suffocation
        if (ownerUUID != null && (damageSource.entity is Player || damageSource.`is`(DamageTypes.IN_WALL))) {
            return true
        }

        if (!Cobblemon.config.playerDamagePokemon && damageSource.entity is Player) {
            return true
        }

        return super.isInvulnerableTo(damageSource)
    }

    override fun canRide(vehicle: Entity): Boolean {
        return platform == PlatformType.NONE && super.canRide(vehicle)
    }

    /**
     * A utility method that checks if this Pokémon has the [UncatchableProperty.uncatchable] property.
     *
     * @return If the Pokémon is uncatchable.
     */
    fun isUncatchable() = pokemon.isUncatchable()

    /**
     * A utility method that checks if this Pokémon has the [UncatchableProperty.uncatchable] property.
     *
     * @return If the Pokémon is uncatchable.
     */
    fun isBattleClone() = pokemon.isBattleClone()

    fun recallWithAnimation(): CompletableFuture<Pokemon> {
        val owner = owner ?: pokemon.getOwnerEntity()
        val future = CompletableFuture<Pokemon>()
        if (entityData.get(PHASING_TARGET_ID) == -1 && owner != null) {
            val preamble = if (owner is PokemonSender) {
                owner.recalling(this)
            } else {
                CompletableFuture.completedFuture(Unit)
            }

            preamble.thenAccept {
                owner.level().playSoundServer(position(), CobblemonSounds.POKE_BALL_RECALL, volume = 0.6F)
                entityData.set(PHASING_TARGET_ID, owner.id)
                entityData.set(BEAM_MODE, 3)
                val state = pokemon.state

                // Let the Pokémon be intangible during recall
                noPhysics = true
                // This doesn't appear to actually prevent a livingEntity from falling, but is here as a precaution
                isNoGravity = true

                afterOnServer(seconds = SEND_OUT_DURATION) {
                    // only recall if the Pokémon hasn't been recalled yet for this state
                    if (state == pokemon.state) {
                        pokemon.recall()
                    }
                    if (owner is NPCEntity) {
                        owner.after(seconds = 1F) {
                            future.complete(pokemon)
                        }
                    } else {
                        future.complete(pokemon)
                    }
                }
            }
        } else {
            pokemon.recall()
            future.complete(pokemon)
        }

        return future
    }

    override fun saveWithoutId(nbt: CompoundTag): CompoundTag {
        val tethering = this.tethering
        if (tethering != null) {
            val tetheringNbt = CompoundTag()
            tetheringNbt.putUUID(DataKeys.TETHERING_ID, tethering.tetheringId)
            tetheringNbt.putUUID(DataKeys.POKEMON_UUID, tethering.pokemonId)
            tetheringNbt.putUUID(DataKeys.POKEMON_OWNER_ID, tethering.playerId)
            tetheringNbt.putUUID(DataKeys.PC_ID, tethering.pcId)
            tetheringNbt.put(DataKeys.TETHER_MIN_ROAM_POS, NbtUtils.writeBlockPos(tethering.minRoamPos))
            tetheringNbt.put(DataKeys.TETHER_MAX_ROAM_POS, NbtUtils.writeBlockPos(tethering.maxRoamPos))
            nbt.put(DataKeys.TETHERING, tetheringNbt)
        } else {
            nbt.put(DataKeys.POKEMON, pokemon.saveToNBT(registryAccess()))
        }
        val battleIdToSave = battleId
        if (battleIdToSave != null) {
            nbt.putUUID(DataKeys.POKEMON_BATTLE_ID, battleIdToSave)
        }
        nbt.putString(DataKeys.POKEMON_POSE_TYPE, entityData.get(POSE_TYPE).name)
        nbt.putByte(DataKeys.POKEMON_BEHAVIOUR_FLAGS, entityData.get(BEHAVIOUR_FLAGS))

        if (entityData.get(HIDE_LABEL)) {
            nbt.putBoolean(DataKeys.POKEMON_HIDE_LABEL, true)
        }
        if (entityData.get(UNBATTLEABLE)) {
            nbt.putBoolean(DataKeys.POKEMON_UNBATTLEABLE, true)
        }
        if (!countsTowardsSpawnCap) {
            nbt.putBoolean(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP, false)
        }
        if (entityData.get(FREEZE_FRAME) != -1F) {
            nbt.putFloat(DataKeys.POKEMON_FREEZE_FRAME, entityData.get(FREEZE_FRAME))
        }
        if (!enablePoseTypeRecalculation) {
            nbt.putBoolean(DataKeys.POKEMON_RECALCULATE_POSE, enablePoseTypeRecalculation)
        }

        // save active effects
        nbt.put(DataKeys.ENTITY_EFFECTS, effects.saveToNbt(this.level().registryAccess()))

        CobblemonEvents.POKEMON_ENTITY_SAVE.post(PokemonEntitySaveEvent(this, nbt))

        return super.saveWithoutId(nbt)
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(DataKeys.TETHERING)) {
            val tetheringNBT = nbt.getCompound(DataKeys.TETHERING)
            val tetheringId = tetheringNBT.getUUID(DataKeys.TETHERING_ID)
            val pcId = tetheringNBT.getUUID(DataKeys.PC_ID)
            val pokemonId = tetheringNBT.getUUID(DataKeys.POKEMON_UUID)
            val playerId = tetheringNBT.getUUID(DataKeys.POKEMON_OWNER_ID)
            val minRoamPos = NbtUtils.readBlockPos(tetheringNBT, DataKeys.TETHER_MIN_ROAM_POS).get()
            val maxRoamPos = NbtUtils.readBlockPos(tetheringNBT, DataKeys.TETHER_MAX_ROAM_POS).get()

            val loadedPokemon = Cobblemon.storage.getPC(pcId, registryAccess())[pokemonId]
            if (loadedPokemon != null && loadedPokemon.tetheringId == tetheringId) {
                pokemon = loadedPokemon
                tethering = PokemonPastureBlockEntity.Tethering(
                    minRoamPos = minRoamPos,
                    maxRoamPos = maxRoamPos,
                    playerId = playerId,
                    playerName = "",
                    tetheringId = tetheringId,
                    pokemonId = pokemonId,
                    pcId = pcId,
                    entityId = id // Doesn't really matter on the entity
                )
            } else {
                pokemon = this.createSidedPokemon()
                health = 0F
            }
        } else {
            val ops = registryAccess().createSerializationContext(NbtOps.INSTANCE)
            pokemon = try {
                this.sidedCodec().decode(ops, nbt.getCompound(DataKeys.POKEMON)).orThrow.first
            } catch (_: IllegalStateException) {
                health = 0F
                this.createSidedPokemon()
            }
        }

        val savedBattleId =
            if (nbt.hasUUID(DataKeys.POKEMON_BATTLE_ID)) nbt.getUUID(DataKeys.POKEMON_BATTLE_ID) else null
        if (savedBattleId != null) {
            val battle = BattleRegistry.getBattle(savedBattleId)
            if (battle != null) {
                battleId = savedBattleId
            }
        }

        // apply active effects
        if (nbt.contains(DataKeys.ENTITY_EFFECTS)) effects.loadFromNBT(
            nbt.getCompound(DataKeys.ENTITY_EFFECTS),
            this.level().registryAccess()
        )

        // init SynchedEntityData
        entityData.set(SPECIES, effects.mockEffect?.mock?.species ?: pokemon.species.resourceIdentifier.toString())
        entityData.set(NICKNAME, pokemon.nickname ?: Component.empty())
        entityData.set(LABEL_LEVEL, pokemon.level)
        entityData.set(POSE_TYPE, PoseType.valueOf(nbt.getString(DataKeys.POKEMON_POSE_TYPE)))
        entityData.set(BEHAVIOUR_FLAGS, nbt.getByte(DataKeys.POKEMON_BEHAVIOUR_FLAGS))
        if (nbt.contains(DataKeys.POKEMON_FREEZE_FRAME)) {
            entityData.set(FREEZE_FRAME, nbt.getFloat(DataKeys.POKEMON_FREEZE_FRAME))
        }

        if (nbt.contains(DataKeys.POKEMON_HIDE_LABEL)) {
            entityData.set(HIDE_LABEL, nbt.getBoolean(DataKeys.POKEMON_HIDE_LABEL))
        }
        if (nbt.contains(DataKeys.POKEMON_UNBATTLEABLE)) {
            entityData.set(UNBATTLEABLE, nbt.getBoolean(DataKeys.POKEMON_UNBATTLEABLE))
        }
        if (nbt.contains(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP)) {
            countsTowardsSpawnCap = nbt.getBoolean(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP)
        }
        if (nbt.contains(DataKeys.POKEMON_RECALCULATE_POSE)) {
            enablePoseTypeRecalculation = nbt.getBoolean(DataKeys.POKEMON_RECALCULATE_POSE)
        }

        if (nbt.contains(DataKeys.POKEMON_PLATFORM_TYPE)) {
            entityData.set(PLATFORM_TYPE, PlatformType.valueOf(nbt.getString(DataKeys.POKEMON_PLATFORM_TYPE)))
        }

        CobblemonEvents.POKEMON_ENTITY_LOAD.postThen(
            event = PokemonEntityLoadEvent(this, nbt),
            ifSucceeded = {},
            ifCanceled = { this.discard() }
        )
    }

    override fun getAddEntityPacket(entityTrackerEntry: ServerEntity): Packet<ClientGamePacketListener> =
        ClientboundCustomPayloadPacket(
            SpawnPokemonPacket(
                this,
                super.getAddEntityPacket(entityTrackerEntry) as ClientboundAddEntityPacket
            )
        ) as Packet<ClientGamePacketListener>

    override fun getPathfindingMalus(nodeType: PathType): Float {
        return if (nodeType == PathType.OPEN) 2F else super.getPathfindingMalus(nodeType)
    }

    override fun getNavigation() = navigation as PokemonNavigation
    override fun createNavigation(world: Level) = PokemonNavigation(world, this)

    @Suppress("SENSELESS_COMPARISON")
    public override fun registerGoals() {
        // DO NOT REMOVE
        // LivingEntity#getActiveEyeHeight is called in the constructor of Entity
        // Pokémon param is not available yet
        if (this.pokemon == null) {
            return
        }
        moveControl = PokemonMoveControl(this)
        goalSelector.removeAllGoals { true }
        goalSelector.addGoal(0, PokemonInBattleMovementGoal(this, 10))
        goalSelector.addGoal(0, object : Goal() {
            override fun canUse() =
                this@PokemonEntity.entityData.get(PHASING_TARGET_ID) != -1 ||
                        pokemon.status?.status == Statuses.SLEEP ||
                        entityData.get(DYING_EFFECTS_STARTED) ||
                        evolutionEntity != null

            override fun canContinueToUse(): Boolean {
                if (pokemon.status?.status == Statuses.SLEEP && !canSleep() && !isBusy) {
                    return false
                } else if (pokemon.status?.status == Statuses.SLEEP || isBusy) {
                    return true
                }
                return false
            }

            override fun getFlags() = EnumSet.allOf(Flag::class.java)
        })

        goalSelector.addGoal(1, PokemonBreatheAirGoal(this))
        goalSelector.addGoal(2, PokemonFloatToSurfaceGoal(this))
        goalSelector.addGoal(3, PokemonFollowOwnerGoal(this, 1.0, 8F, 2F))
        goalSelector.addGoal(4, PokemonMoveIntoFluidGoal(this))
        goalSelector.addGoal(5, SleepOnTrainerGoal(this))
        goalSelector.addGoal(5, WildRestGoal(this))

        if (pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED) != null) {
            goalSelector.addGoal(5, EatBlockGoal(this))
        }

        goalSelector.addGoal(6, PokemonWanderAroundGoal(this))
        goalSelector.addGoal(7, PokemonLookAtEntityGoal(this, ServerPlayer::class.java, 5F))
        goalSelector.addGoal(8, PokemonPointAtSpawnGoal(this))
    }

    fun canSleep(): Boolean {
        val rest = behaviour.resting
        val worldTime = (level().dayTime % 24000).toInt()
        val light = level().getMaxLocalRawBrightness(blockPosition())
        val block = level().getBlockState(blockPosition()).block
        val biome = level().getBiome(blockPosition()).value()

        return rest.canSleep &&
                !this.getBehaviourFlag(PokemonBehaviourFlag.EXCITED) &&
                worldTime in this.behaviour.resting.times &&
                light in rest.light &&
                (rest.blocks.isEmpty() || rest.blocks.any {
                    it.fits(
                        block,
                        this.level().registryAccess().registryOrThrow(Registries.BLOCK)
                    )
                }) &&
                (rest.biomes.isEmpty() || rest.biomes.any {
                    it.fits(
                        biome,
                        this.level().registryAccess().registryOrThrow(Registries.BIOME)
                    )
                })
    }

    override fun getBreedOffspring(serverLevel: ServerLevel, ageableMob: AgeableMob) = null

    override fun canSitOnShoulder(): Boolean {
        return pokemon.form.shoulderMountable
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (!this.isBattling && this.isBattleClone()) {
            return InteractionResult.FAIL
        }
        val itemStack = player.getItemInHand(hand)
        val colorFeatureType = SpeciesFeatures.getFeaturesFor(pokemon.species)
            .find { it is ChoiceSpeciesFeatureProvider && DataKeys.CAN_BE_COLORED in it.keys }
        val colorFeature = pokemon.getFeature<StringSpeciesFeature>(DataKeys.CAN_BE_COLORED)

        if (ownerUUID == player.uuid || ownerUUID == null) {
            if (itemStack.`is`(Items.SHEARS) && this.readyForShearing()) {
                this.shear(SoundSource.PLAYERS)
                this.gameEvent(GameEvent.SHEAR, player)
                itemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
                return InteractionResult.SUCCESS
            } else if (itemStack.`is`(Items.BUCKET)) {
                if (pokemon.aspects.any { it.contains(DataKeys.CAN_BE_MILKED) }) {
                    player.playSound(SoundEvents.GOAT_MILK, 1.0f, 1.0f)
                    val milkBucket = ItemUtils.createFilledResult(itemStack, player, Items.MILK_BUCKET.defaultInstance)
                    player.setItemInHand(hand, milkBucket)
                    return InteractionResult.sidedSuccess(level().isClientSide)
                }
            } else if (itemStack.`is`(Items.BOWL)) {
                if (pokemon.aspects.any { it.contains("mooshtank") }) {
                    player.playSound(SoundEvents.MOOSHROOM_MILK, 1.0f, 1.0f)
                    // if the Mooshtank ate a Flower beforehand
                    if (pokemon.lastFlowerFed != ItemStack.EMPTY && pokemon.aspects.any { it.contains("mooshtank-brown") }) {
                        when (pokemon.lastFlowerFed.item) {
                            Items.ALLIUM -> MobEffects.FIRE_RESISTANCE to 80
                            Items.AZURE_BLUET -> MobEffects.BLINDNESS to 160
                            Items.BLUE_ORCHID, Items.DANDELION -> MobEffects.SATURATION to 7
                            Items.CORNFLOWER -> MobEffects.JUMP to 120
                            Items.LILY_OF_THE_VALLEY -> MobEffects.POISON to 240
                            Items.OXEYE_DAISY -> MobEffects.REGENERATION to 160
                            Items.POPPY, Items.TORCHFLOWER -> MobEffects.NIGHT_VISION to 100
                            Items.PINK_TULIP, Items.RED_TULIP, Items.WHITE_TULIP, Items.ORANGE_TULIP -> MobEffects.WEAKNESS to 180
                            Items.WITHER_ROSE -> MobEffects.WITHER to 160
                            CobblemonItems.PEP_UP_FLOWER -> MobEffects.LEVITATION to 160
                            else -> null
                        }?.let {
                            // modify the suspicious stew with the effect
                            val susStewStack = Items.SUSPICIOUS_STEW.defaultInstance
                            //SuspiciousStewItem.addEffectsToStew(susStewStack, listOf(StewEffect(it.first, it.second)))
                            val susStewEffect = ItemUtils.createFilledResult(itemStack, player, susStewStack)
                            //give player modified Suspicious Stew
                            player.setItemInHand(hand, susStewEffect)
                            // reset the flower fed state
                            pokemon.lastFlowerFed = ItemStack.EMPTY
                        }
                        return InteractionResult.sidedSuccess(level().isClientSide)
                    } else {
                        val mushroomStew = ItemUtils.createFilledResult(itemStack, player, Items.MUSHROOM_STEW.defaultInstance)
                        player.setItemInHand(hand, mushroomStew)
                        return InteractionResult.sidedSuccess(level().isClientSide)
                    }
                }
            }
            // Flowers used on brown MooshTanks
            else if (itemStack.`is`(Items.ALLIUM) ||
                itemStack.`is`(Items.AZURE_BLUET) ||
                itemStack.`is`(Items.BLUE_ORCHID) ||
                itemStack.`is`(Items.DANDELION) ||
                itemStack.`is`(Items.CORNFLOWER) ||
                itemStack.`is`(Items.LILY_OF_THE_VALLEY) ||
                itemStack.`is`(Items.OXEYE_DAISY) ||
                itemStack.`is`(Items.POPPY) ||
                itemStack.`is`(Items.TORCHFLOWER) ||
                itemStack.`is`(Items.PINK_TULIP) ||
                itemStack.`is`(Items.RED_TULIP) ||
                itemStack.`is`(Items.WHITE_TULIP) ||
                itemStack.`is`(Items.ORANGE_TULIP) ||
                itemStack.`is`(Items.WITHER_ROSE) ||
                itemStack.`is`(CobblemonItems.PEP_UP_FLOWER)
            ) {
                if (pokemon.aspects.any { it.contains("mooshtank") }) {
                    player.playSound(SoundEvents.MOOSHROOM_EAT, 1.0f, 1.0f)
                    pokemon.lastFlowerFed = itemStack
                    return InteractionResult.sidedSuccess(level().isClientSide)
                }
            } else if (!player.isShiftKeyDown && StashHandler.interactMob(player, pokemon, itemStack)) {
                return InteractionResult.SUCCESS
            } else if (itemStack.item is DyeItem && colorFeatureType != null) {
                val currentColor = colorFeature?.value ?: ""
                val item = itemStack.item as DyeItem
                if (!item.dyeColor.name.equals(currentColor, ignoreCase = true)) {
                    if (player is ServerPlayer) {
                        if (colorFeature != null) {
                            colorFeature.value = item.dyeColor.name.lowercase()
                            this.pokemon.markFeatureDirty(colorFeature)
                        } else {
                            val newColorFeature =
                                StringSpeciesFeature(DataKeys.CAN_BE_COLORED, item.dyeColor.name.lowercase())
                            this.pokemon.features.add(newColorFeature)
                            this.pokemon.anyChangeObservable.emit(pokemon)
                        }

                        this.pokemon.updateAspects()
                        if (!player.isCreative) {
                            itemStack.shrink(1)
                        }
                    }
                    return InteractionResult.sidedSuccess(level().isClientSide)
                }
            } else if (itemStack.item.equals(Items.WATER_BUCKET) && colorFeatureType != null) {
                if (player is ServerPlayer) {
                    if (colorFeature != null) {
                        if (!player.isCreative) {
                            itemStack.shrink(1)
                            player.giveOrDropItemStack(Items.BUCKET.defaultInstance)
                        }
                        colorFeature.value = ""
                        this.pokemon.markFeatureDirty(colorFeature)
                        this.pokemon.updateAspects()
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
        }

        if (hand == InteractionHand.MAIN_HAND && player is ServerPlayer && pokemon.getOwnerPlayer() == player) {
            if (player.isShiftKeyDown) {
                InteractPokemonUIPacket(this.getUUID(), canSitOnShoulder() && pokemon in player.party()).sendToPlayer(
                    player
                )
            } else {
                // TODO #105
                if (this.attemptItemInteraction(player, player.getItemInHand(hand))) return InteractionResult.SUCCESS
            }
        }

        return super.mobInteract(player, hand)
    }

    override fun getDimensions(pose: Pose): EntityDimensions {
        val scale = effects.mockEffect?.scale ?: (form.baseScale * pokemon.scaleModifier)
        var result = this.exposedForm.hitbox.scale(scale)
        result = result.withEyeHeight(this.exposedForm.eyeHeight(this) * result.height)
        return result
    }

    override fun canBeSeenAsEnemy() = super.canBeSeenAsEnemy() && !isBusy

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        return if (super.hurt(source, amount)) {
            effects.mockEffect?.takeIf { it is IllusionEffect && this.battleId == null }?.end(this)
            if (this.health == 0F) {
                pokemon.currentHealth = 0
            } else {
//                pokemon.currentHealth = this.health.toInt()
            }
            true
        } else false
    }

    override fun shouldBeSaved(): Boolean {
        if (ownerUUID == null && !pokemon.isNPCOwned() && (Cobblemon.config.savePokemonToWorld || isPersistenceRequired)) {
            CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.postThen(PokemonEntitySaveToWorldEvent(this)) {
                return true
            }
        }
        return tethering != null
    }

    override fun checkDespawn() {
        if (pokemon.getOwnerUUID() == null && !isPersistenceRequired && despawner.shouldDespawn(this)) {
            discard()
        }
    }

    fun setBehaviourFlag(flag: PokemonBehaviourFlag, on: Boolean) {
        entityData.set(BEHAVIOUR_FLAGS, setBitForByte(entityData.get(BEHAVIOUR_FLAGS), flag.bit, on))
    }

    fun getBehaviourFlag(flag: PokemonBehaviourFlag): Boolean =
        getBitForByte(entityData.get(BEHAVIOUR_FLAGS), flag.bit)

    @Suppress("UNUSED_PARAMETER")
    fun canBattle(player: Player): Boolean {
        if (entityData.get(UNBATTLEABLE)) {
            return false
        } else if (isBusy) {
            return false
        } else if (battleId != null) {
            return false
        } else if (ownerUUID != null) {
            return false
        } else if (health <= 0F || isDeadOrDying) {
            return false
        } else if (player.isPartyBusy()) {
            return false
        }

        return true
    }

    /**
     * The level this entity should display.
     *
     * @return The level that should be displayed, if equal or lesser than 0 the level is not intended to be displayed.
     */
    fun labelLevel() = entityData.get(LABEL_LEVEL)

    override fun playAmbientSound() {
        if (!this.isSilent || this.busyLocks.filterIsInstance<EmptyPokeBallEntity>().isEmpty()) {
            val sound = ResourceLocation.fromNamespaceAndPath(
                this.pokemon.species.resourceIdentifier.namespace,
                "pokemon.${this.pokemon.showdownId()}.ambient"
            )
            // ToDo distance to travel is currently hardcoded to default we can maybe find a way to work around this down the line
            UnvalidatedPlaySoundS2CPacket(
                sound,
                this.soundSource,
                this.x,
                this.y,
                this.z,
                this.soundVolume,
                this.voicePitch
            ).sendToPlayersAround(this.x, this.y, this.z, 16.0, this.level().dimension())
        }
    }

    // We never want to allow an actual sound event here, we do not register our sounds to the sound registry as species are loaded by the time the registry is frozen.
    // Super call would do the same but might as well future-proof.
    override fun getAmbientSound() = null

    override fun getAmbientSoundInterval() = Cobblemon.config.ambientPokemonCryTicks

    private fun attemptItemInteraction(player: Player, stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }

        if (player is ServerPlayer && isBattling) {
            val battle = battleId?.let(BattleRegistry::getBattle) ?: return false

            val bagItemLike = BagItems.getConvertibleForStack(stack) ?: return false

            val battlePokemon =
                battle.actors.flatMap { it.pokemonList }.find { it.effectedPokemon.uuid == pokemon.uuid }
                    ?: return false // Shouldn't be possible but anyway
            if (battlePokemon.actor.getSide().actors.none { it.isForPlayer(player) }) {
                return true
            }

            return bagItemLike.handleInteraction(player, battlePokemon, stack)
        }
        if (player !is ServerPlayer || this.isBusy) {
            return false
        }

        // Check evolution item interaction
        if (pokemon.getOwnerPlayer() == player) {
            val context = ItemInteractionEvolution.ItemInteractionContext(stack, player.level())
            pokemon.lockedEvolutions
                .filterIsInstance<ItemInteractionEvolution>()
                .forEach { evolution ->
                    if (evolution.attemptEvolution(pokemon, context)) {
                        if (!player.isCreative) {
                            stack.shrink(1)
                        }
                        this.level().playSoundServer(
                            position = this.position(),
                            sound = CobblemonSounds.ITEM_USE,
                            volume = 1F,
                            pitch = 1F
                        )
                        return true
                    }
                }
        }

        (stack.item as? PokemonEntityInteraction)?.let {
            if (it.onInteraction(player, this, stack)) {
                it.sound?.let {
                    this.level().playSoundServer(
                        position = this.position(),
                        sound = it,
                        volume = 1F,
                        pitch = 1F
                    )
                }
                return true
            }
        }
        return false
    }

    override fun getOwner(): LivingEntity? {
        return pokemon.getOwnerEntity()
    }

    fun offerHeldItem(player: Player, stack: ItemStack): Boolean {
        if (player !is ServerPlayer || this.isBusy || this.pokemon.getOwnerPlayer() != player) {
            return false
        }
        // We want the count of 1 in order to match the ItemStack#areEqual
        val giving = stack.copy().apply { count = 1 }
        val possibleReturn = this.pokemon.heldItemNoCopy()
        if (stack.isEmpty && possibleReturn.isEmpty) {
            return false
        }
        if (ItemStack.isSameItem(giving, possibleReturn)) {
            player.sendSystemMessage(lang("held_item.already_holding", this.pokemon.getDisplayName(), stack.hoverName))
            return true
        }
        val returned = this.pokemon.swapHeldItem(stack = stack, decrement = !player.isCreative)
        val text = when {
            giving.isEmpty -> lang("held_item.take", returned.hoverName, this.pokemon.getDisplayName())
            returned.isEmpty -> lang("held_item.give", this.pokemon.getDisplayName(), giving.hoverName)
            else -> lang("held_item.replace", returned.hoverName, this.pokemon.getDisplayName(), giving.hoverName)
        }
        player.giveOrDropItemStack(returned)
        player.sendSystemMessage(text)
        this.level().playSoundServer(
            position = this.position(),
            sound = SoundEvents.ITEM_PICKUP,
            volume = 0.6F,
            pitch = 1.4F
        )
        return true
    }

    fun tryMountingShoulder(player: ServerPlayer): Boolean {
        if (this.pokemon.belongsTo(player) && this.hasRoomToMount(player)) {
            CobblemonEvents.SHOULDER_MOUNT.postThen(
                ShoulderMountEvent(
                    player,
                    pokemon,
                    isLeft = player.shoulderEntityLeft.isEmpty
                )
            ) {
                val dirToPlayer = player.eyePosition.subtract(position()).multiply(1.0, 0.0, 1.0).normalize()
                deltaMovement = dirToPlayer.scale(0.8).add(0.0, 0.5, 0.0)
                val lock = Any()
                busyLocks.add(lock)
                after(seconds = 0.5F) {
                    busyLocks.remove(lock)
                    if (!isBusy && isAlive) {
                        val isLeft = player.shoulderEntityLeft.isEmpty
                        if (isLeft || player.shoulderEntityRight.isEmpty) {
                            pokemon.state = ShoulderedState(player.uuid, isLeft, pokemon.uuid)
                            this.setEntityOnShoulder(player)
                            this.pokemon.form.shoulderEffects.forEach { it.applyEffect(this.pokemon, player, isLeft) }
                            this.level().playSoundServer(
                                position = this.position(),
                                sound = SoundEvents.ITEM_PICKUP,
                                volume = 0.7F,
                                pitch = 1.4F
                            )
                            discard()
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    override fun setEntityOnShoulder(player: ServerPlayer): Boolean {
        if (!super.setEntityOnShoulder(player)) {
            return false
        }
        val nbt = when {
            player.shoulderEntityRight.isPokemonEntity() && player.shoulderEntityRight.getCompound(DataKeys.POKEMON)
                .getUUID(DataKeys.POKEMON_UUID) == this.pokemon.uuid -> player.shoulderEntityRight

            player.shoulderEntityLeft.isPokemonEntity() && player.shoulderEntityLeft.getCompound(DataKeys.POKEMON)
                .getUUID(DataKeys.POKEMON_UUID) == this.pokemon.uuid -> player.shoulderEntityLeft

            else -> return true
        }
        nbt.putUUID(DataKeys.SHOULDER_UUID, this.pokemon.uuid)
        nbt.putString(DataKeys.SHOULDER_SPECIES, this.pokemon.species.resourceIdentifier.toString())
        nbt.putString(DataKeys.SHOULDER_FORM, this.pokemon.form.name)
        nbt.put(DataKeys.SHOULDER_ASPECTS, this.pokemon.aspects.map(StringTag::valueOf).toNbtList())
        nbt.putFloat(DataKeys.SHOULDER_SCALE_MODIFIER, this.pokemon.scaleModifier)
        return true
    }

    /**
     * Adjusts a given sent out position based on the local environment.
     * Returns the new position and a PlatformType if the pokemon should be placed on one.
     */
    fun getAjustedSendoutPosition(pos: Vec3) : Vec3 {
        var platform = PlatformType.NONE
        var blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
        var blockLookCount = 5
        var foundSurface = false
        val form = this.exposedForm
        var result = pos
        if (this.level().isWaterAt(blockPos)) {
            // look upward for a water surface
            var testPos = blockPos
            if (!form.behaviour.moving.swim.canBreatheUnderwater || form.behaviour.moving.fly.canFly) {
                // move sendout pos to surface if it's near
                for (i in 0..blockLookCount) {
                    // Try to find a surface...
                    val blockState = this.level().getBlockState(testPos)
                    if (blockState.fluidState.isEmpty) {
                        if (blockState.getCollisionShape(this.level(), testPos).isEmpty) {
                            foundSurface = true
                        }
                        // No space above the water surface
                        break
                    }
                    testPos = testPos.above()
                }
                if (foundSurface) {
                    val hasHeadRoom = !collidesWithBlock(Vec3(blockPos.x.toDouble(), (blockPos.y).toDouble(), (blockPos.z).toDouble()))
                    if (hasHeadRoom) {
                        result = Vec3(result.x, testPos.y.toDouble(), result.z)
                    }
                } else {
                    foundSurface = false
                }
            }
        } else if (this.level().isEmptyBlock(blockPos)) {
            // look downward for a water surface
            blockLookCount = 64 // Higher because the pokemon can fall down to water below
            var testPos =  BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            for (i in 0..blockLookCount) {
                // Try to find a surface...
                val blockState = this.level().getBlockState(testPos)
                if (!blockState.fluidState.isEmpty) {
                    foundSurface = true
                    break
                }
                if (!blockState.getCollisionShape(this.level(), testPos).isEmpty) {
                    break
                }
                testPos = testPos.below()
            }
        }
        if (foundSurface) {
            val canFly = form.behaviour.moving.fly.canFly
            if (canFly) {
                val hasHeadRoom = !collidesWithBlock(Vec3(blockPos.x.toDouble(), (result.y + 1), (blockPos.z).toDouble()))
                if (hasHeadRoom) {
                    result = Vec3(result.x, result.y + 1.0, result.z)
                }
            } else if (form.behaviour.moving.swim.canBreatheUnderwater && !form.behaviour.moving.swim.canWalkOnWater) {
                // Use half hitbox height for swimmers
                val halfHeight = form.hitbox.height * form.baseScale / 2.0
                for (i in 1..halfHeight.toInt()) {
                    blockPos = blockPos.below()
                    if (!this.level().isWaterAt(blockPos) || !this.level().getBlockState(blockPos).getCollisionShape(this.level(), blockPos).isEmpty) {
                        break
                    }
                }
                result = Vec3(result.x, result.y + halfHeight - halfHeight.toInt(), result.z)
            } else {
                platform = if (form.behaviour.moving.swim.canWalkOnWater || collidesWithBlock(Vec3(result.x, result.y, result.z))) PlatformType.NONE else PlatformType.getPlatformTypeForPokemon(form)
            }
        }
        this.platform = platform

        return result
    }

    private fun Entity.collidesWithBlock(pos: Vec3) : Boolean {
        return level().getBlockCollisions(this, boundingBox.move(pos)).iterator().hasNext()
    }

    override fun remove(reason: RemovalReason) {
        val stateEntity = (pokemon.state as? ActivePokemonState)?.entity
        super.remove(reason)
        if (stateEntity == this) {
            pokemon.state = InactivePokemonState()
        }
        subscriptions.forEach(ObservableSubscription<*>::unsubscribe)
        removalObservable.emit(reason)

        if (reason.shouldDestroy() && pokemon.tetheringId != null) {
            pokemon.tetheringId = null
        }
        if (evolutionEntity != null) {
            evolutionEntity!!.kill()
            pokemon.entity?.evolutionEntity = null
        }
    }

    // Copy and paste of how vanilla checks it, unfortunately no util method you can only add then wait for the result
    fun hasRoomToMount(player: Player): Boolean {
        return (player.shoulderEntityLeft.isEmpty || player.shoulderEntityRight.isEmpty)
                && !player.isPassenger()
                && player.onGround()
                && !player.isInWater
                && !player.isInPowderSnow
    }

    fun cry() {
        if (this.isSilent) return
        val pkt = PlayPosableAnimationPacket(id, setOf("cry"), emptyList())
        level().getEntitiesOfClass(ServerPlayer::class.java, AABB.ofSize(position(), 64.0, 64.0, 64.0)) { true }.forEach {
            it.sendPacket(pkt)
        }
    }

    override fun dropAllDeathLoot(world: ServerLevel, source: DamageSource) {
        if (pokemon.isWild()) {
            super.dropAllDeathLoot(world, source)
            delegate.drop(source)
        }
    }

    override fun dropExperience(attacker: Entity?) {
        // Copied over the entire function because it's the simplest way to switch out the gamerule check
        if (
            level() is ServerLevel && !this.wasExperienceConsumed() &&
            (isAlwaysExperienceDropper ||
                    lastHurtByPlayerTime > 0 &&
                    shouldDropExperience() &&
                    level().gameRules.getBoolean(CobblemonGameRules.DO_POKEMON_LOOT
                    ))
        ) {
            ExperienceOrb.award(level() as ServerLevel, position(), baseExperienceReward)
        }
    }

    override fun tickDeath() {
        // Do not invoke super we need to keep a tight lid on this due to the Thorium mod forcing the ticks to a max of 20 on server side if we invoke a field update here
        // Client delegate is mimicking expected behavior on client end.
        delegate.updatePostDeath()
    }

    override fun ate() {
        super.ate()

        val feature = pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED)
        if (feature != null) {
            feature.enabled = false
            pokemon.markFeatureDirty(feature)
            pokemon.updateAspects()
        }
    }

    override fun travel(movementInput: Vec3) {
        val prevBlockPos = this.blockPosition()
        if (beamMode != 3) { // Don't let Pokémon move during recall
            super.travel(movementInput)
            this.updateBlocksTraveled(prevBlockPos)
        }
        if (isBattling && this.isInWater) {
            // Prevent swimmers from sinking in battle
            this.deltaMovement = Vec3(deltaMovement.x, 0.0, deltaMovement.z)
        }
    }

    private fun updateBlocksTraveled(fromBp: BlockPos) {
        // Riding or falling shouldn't count, other movement sources are fine
        if (this.isPassenger() || this.isFalling()) {
            return
        }
        val blocksTaken = this.blockPosition().distSqr(fromBp)
        if (blocksTaken > 0) this.blocksTraveled += blocksTaken
    }

    override fun pushEntities() {
        // Don't collide with other entities when being recalled
        if (beamMode != 3) super.pushEntities()
    }

    override fun isPushable(): Boolean {
        return beamMode != 3 && super.isPushable()
    }

    /*
        private fun updateEyeHeight() {
            @Suppress("CAST_NEVER_SUCCEEDS")
            (this as com.cobblemon.mod.common.mixin.accessor.AccessorEntity).standingEyeHeight(this.getActiveEyeHeight(EntityPose.STANDING, this.type.dimensions))
        }

    */

    fun isFlying() = this.getBehaviourFlag(PokemonBehaviourFlag.FLYING)

    fun isFalling() = this.fallDistance > 0 && this.level().getBlockState(this.blockPosition().below()).isAir && !this.isFlying()

    fun couldStopFlying() = isFlying() && !behaviour.moving.walk.avoidsLand && behaviour.moving.walk.canWalk

    override fun getCurrentPoseType(): PoseType = this.entityData.get(POSE_TYPE)

    /**
     * Returns the [Species.translatedName] of the backing [pokemon].
     *
     * @return The [Species.translatedName] of the backing [pokemon].
     */
    override fun getTypeName(): Component = this.pokemon.species.translatedName

    /**
     * If this Pokémon has a nickname, then the nickname is returned.
     * Otherwise, [getDefaultName] is returned
     *
     * @return The current display name of this entity.
     */
    override fun getName(): Component {
        if (!entityData.get(NICKNAME_VISIBLE)) return typeName
        return entityData.get(NICKNAME).takeIf { it.contents != PlainTextContents.EMPTY }
            ?: pokemon.getDisplayName()
    }

    /**
     * Returns the custom name of this entity, in the context of Cobblemon it is the [Pokemon.nickname].
     *
     * @return The nickname of the backing [pokemon].
     */
    override fun getCustomName(): Component? = pokemon.nickname

    /**
     * Sets the custom name of this entity.
     * In the context of a Pokémon entity this affects the [Pokemon.nickname].
     *
     * @param name The new name being set, if null the [Pokemon.nickname] is removed.
     */
    override fun setCustomName(name: Component?) {
        // We do this as a compromise to keep as much compatibility as possible with other mods expecting this entity to act like a vanilla one
        this.pokemon.nickname = Component.literal(name?.string)
    }

    /**
     * Checks if the backing [pokemon] has a non-null [Pokemon.nickname].
     *
     * @return If the backing [pokemon] has a non-null [Pokemon.nickname].
     */
    override fun hasCustomName(): Boolean =
        pokemon.nickname != null && pokemon.nickname?.contents != PlainTextContents.EMPTY

    /**
     * This method toggles the visibility of the entity name,
     * Unlike the vanilla implementation in our context it changes between displaying the species name or nickname of the Pokémon.
     *
     * @param visible The state of custom name visibility.
     */
    override fun setCustomNameVisible(visible: Boolean) {
        // We do this as a compromise to keep as much compatibility as possible with other mods expecting this entity to act like a vanilla one
        entityData.set(NICKNAME_VISIBLE, visible)
    }

    /**
     * Attempts to force initiate a battle with this Pokémon.
     *
     * @param player The player to attempt a battle with.
     * @return Whether the battle was successfully started.
     */
    fun forceBattle(player: ServerPlayer): Boolean {
        if (!canBattle(player)) {
            return false
        }
        val lead = player.party().firstOrNull { it.entity != null }?.uuid
        return BattleBuilder.pve(player, this, lead) is SuccessfulBattleStart
    }

    /**
     * In the context of a Pokémon entity this checks if the Pokémon is currently set to displaying its nickname.
     *
     * @return If the custom name of this entity should display, in this case the [getCustomName] is the nickname but if null the [getDefaultName] will be used.
     */
    override fun isCustomNameVisible(): Boolean = entityData.get(NICKNAME_VISIBLE)

    /**
     * Returns whether the entity is currently set to having its name displayed.
     *
     * @return If this entity should render the name label.
     */
    override fun shouldShowName(): Boolean = entityData.get(SHOULD_RENDER_NAME)

    /**
     * Sets the entity to having its name hidden.
     */
    fun hideNameRendering() {
        entityData.set(SHOULD_RENDER_NAME, false)
    }

    override fun isFood(stack: ItemStack): Boolean = false

    override fun canMate(other: Animal): Boolean = false

    override fun spawnChildFromBreeding(world: ServerLevel, other: Animal) {}

    override fun shear(shearedSoundCategory: SoundSource) {
        this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F)
        val feature = this.pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED) ?: return
        feature.enabled = true
        this.pokemon.markFeatureDirty(feature)
        this.pokemon.updateAspects()
        val i = this.random.nextInt(3) + 1
        for (j in 0..i) {
            val color = this.pokemon.getFeature<StringSpeciesFeature>(DataKeys.CAN_BE_COLORED)?.value ?: "white"
            val woolItem = when (color) {
                "black" -> Items.BLACK_WOOL
                "blue" -> Items.BLUE_WOOL
                "brown" -> Items.BROWN_WOOL
                "cyan" -> Items.CYAN_WOOL
                "gray" -> Items.GRAY_WOOL
                "green" -> Items.GREEN_WOOL
                "light_blue" -> Items.LIGHT_BLUE_WOOL
                "light_gray" -> Items.LIGHT_GRAY_WOOL
                "lime" -> Items.LIME_WOOL
                "magenta" -> Items.MAGENTA_WOOL
                "orange" -> Items.ORANGE_WOOL
                "purple" -> Items.PURPLE_WOOL
                "red" -> Items.RED_WOOL
                "yellow" -> Items.YELLOW_WOOL
                "pink" -> Items.PINK_WOOL
                else -> Items.WHITE_WOOL
            }
            val itemEntity = this.spawnAtLocation(woolItem, 1) ?: return
            itemEntity.deltaMovement = itemEntity.deltaMovement.add(
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.1f).toDouble(),
                (this.random.nextFloat() * 0.05f).toDouble(),
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.1f).toDouble()
            )
        }
    }

    override fun readyForShearing(): Boolean {
        val feature = this.pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED) ?: return false
        return !this.isBusy && !this.pokemon.isFainted() && !feature.enabled
    }

    override fun canUsePortal(allowsVehicles: Boolean) = false

    override fun setAirSupply(air: Int) {
        if (this.isBattling) {
            this.entityData.set(DATA_AIR_SUPPLY_ID, 300)
            return
        }
        super.setAirSupply(air)
    }

    override fun stopSeenByPlayer(player: ServerPlayer) {
        if (player == null) {
            return
        }

        if (this.ownerUUID == player.uuid && tethering == null) {
            queuedToDespawn = true
            return
        }
//
//            val chunkPos = ChunkPos(BlockPos(x.toInt(), y.toInt(), z.toInt()))
//            (world as ServerWorld).chunkManager
//                .addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 0, id)
//            this.goalSelector.tick()
//            if(distanceTo(player.blockPos) > 100) pokemon.recall()
//        }
    }

    override fun canBeLeashed() = true

    override fun setLeashedTo(entity: Entity, bl: Boolean) {
        super.setLeashedTo(entity, bl)
        if (this.ownerUUID != null && this.ownerUUID != entity.uuid ) {
            dropLeash(true, true)
        }
    }

    /** Retrieves the battle theme associated with this Pokemon's Species/Form, or the default PVW theme if not found. */
    fun getBattleTheme() = BuiltInRegistries.SOUND_EVENT.get(this.form.battleTheme) ?: CobblemonSounds.PVW_BATTLE

    /**
     * A utility method to instance a [Pokemon] aware if the [world] is client sided or not.
     *
     * @return The side safe [Pokemon] with the [Pokemon.isClient] set.
     */
    private fun createSidedPokemon(): Pokemon = Pokemon().apply { isClient = this@PokemonEntity.level().isClientSide }

    /**
     * A utility method to resolve the [Codec] of [Pokemon] aware if the [world] is client sided or not.
     *
     * @return The [Codec].
     */
    private fun sidedCodec(): Codec<Pokemon> = if (this.level().isClientSide) Pokemon.CLIENT_CODEC else Pokemon.CODEC

    override fun resolvePokemonScan(): PokedexEntityData? {
        return PokemonSpecies.getByIdentifier(this.exposedSpecies.resourceIdentifier)?.let { species ->
            PokedexEntityData(
                species = species,
                form = this.form,
                gender = this.pokemon.gender,
                aspects = this.aspects,
                shiny = this.pokemon.shiny,
                level = this.labelLevel(),
                ownerUUID = this.ownerUUID
            )
        }
    }

    override fun resolveEntityScan(): LivingEntity {
        return this
    }
}

