package some
.thing

import net.liopyu.kotlinscript.FabricBootstrap
import net.minecraft.client.Minecraft
import net.minecraft.sounds.Musics
import net.minecraft.world.entity.EntityType.Builder
import net.minecraft.network.protocol.Packet
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.core.Registry
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.entity.EntityType
import kotlin.internal.PureReifiable
import net.liopyu.kotlinscript.util.ClassScanner

PureReifiable
// Flying Entity Class
Entity().ELEMENT_NODE
class FlyingEntity(
    entityType: EntityType<out Entity>,
    world: Level
) : Entity(entityType, world) {

    var altitude = 100.0
    var speed = 1.5

    init {
        noClip = true // Allows this entity to pass through solid objects
    }

    override fun tick() {
        super.tick()
        if (!world.isClient) {
            moveInAir()
        }
    }

    private fun moveInAir() {
        velocity = velocity.add(0.0, 0.02, 0.0) // Ascending movement
        if (altitude > 200) {
            velocity = velocity.multiply(1.0, 0.0, 1.0) // Cap the altitude
        }
    }

    override fun onPlayerCollision(player: Player) {
        player.damage(damageSources.magic(), 2.0f) // Deal light damage to players on contact
    }

    override fun interact(
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        if (!world.isClient) {
            player.sendMessage("You feel a gust of wind as the entity soars past!", true)
        }
        return InteractionResult.SUCCESS
    }

    override fun initDataTracker() {
        // Initialize entity data here
    }

    override fun readCustomDataFromNbt(nbt: CompoundTag) {
        altitude = nbt.getDouble("Altitude")
        speed = nbt.getDouble("Speed")
    }

    override fun writeCustomDataToNbt(nbt: CompoundTag) {
        nbt.putDouble("Altitude", altitude)
        nbt.putDouble("Speed", speed)
    }

    override fun createSpawnPacket(): Packet<*>? {
        return super.createSpawnPacket()
    }

    override fun playSpawnEffects() {
        world.playSound(
            null,
            this.blockPos,
            SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
            SoundSource.HOSTILE,
            1.0f,
            1.0f
        )
    }

    companion object {
        fun registerEntity() {
            Registry.register(
                Registry.ENTITY_TYPE,
                "flying_entity",
                Builder.create(::FlyingEntity, SpawnGroupData.CREATURE)
                    .setDimensions(1.0f, 1.0f)
                    .build("flying_entity")
            )
        }
    }
}

fun calculateSum(numbers: List<Int>): Unit {
    var sum = 0 // `sum` is highlighted as a scoped variable
    for (number in numbers) {
        sum += number
// Both `sum` and `number` are highlighted as scoped variables
    }
    return  // `return` is highlighted as a keyword
}
net.liopyu.kotlinscript.util.ClassScanner

class SomeClass {
    companion object {
        fun someStaticMethod() = "Hello from Companion Object!"
        val value = 10
    }

    val someMethod = SomeClass // Store a reference to the companion object
}


fun main() {
    val instance: Unit = ClassScanner()()
    val instance2: FabricBootstrap.Companion = FabricBootstrap
    instance2.getOtherValue()

}
kotlin.io.Console
var something =
    something.someStaticMethod()