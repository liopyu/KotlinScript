import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.Level
import  net. minecraft. world. entity. ai.attributes.Attributes
import net.minecraft.world.entity.EntityType
import net.minecraft.  core .Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import  net.minecraft.world.entity.MobCategory
class CustomZombie(type: EntityType<out Zombie>, world: Level) : Zombie(type, world) {

    init {
        // Set custom attributes for the zombie
        this.getAttribute(Attributes.MAX_HEALTH)?.baseValue = 50.0
        this.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue= 10.0
        val bitwiseOr = 0b1010 or 0b0101
        val bitwiseAnd = 0b1010 and 0b0101
        val thing = "${true ? "true" : "false"}"
        val StarterConfig = 10
        val config = StarterConfig()
        this.implementation.registerCommandArgument(cobblemonResource("npc_class"), NPCClassArgumentType::class, SingletonArgumentInfo.contextFree(NPCClassArgumentType::npcClass))

    }


}
class NPCClassArgumentType {
    companion object {
        fun npcClass(className: String): String {
            return "npc class called: $className"
        }
    }
}

val npcFunction = NPCClassArgumentType::npcClass
val result = npcFunction.invoke("warrior")
println(result)

val CUSTOM_ZOMBIE: EntityType<CustomZombie> = EntityType.Builder.of(
    { type, world -> CustomZombie(type, world) }, // Entity factory

    MobCategory.MONSTER // Mob category
)
    .sized(0.6f, 1.95f)
    .build()


 fun onInitialize() {
     /*Registry.register(
         Registries.ENTITY_TYPE,
         ResourceLocation.fromNamespaceAndPath("kotlinscript", "custom_zombie").toString(),
         CUSTOM_ZOMBIE
     )*/

     val MobCategory = 100
     MobCategory.MONSTER
     net.minecraft.world.entity.MobCategory.MONSTER
     println("CustomZombie registered!")
    }

object Cobblemon {
    org.apache.logging.log4j.Logger
}