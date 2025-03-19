package tests
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.client.Minecraft
String
String(StringBuilder())
class Test {

    fun a(x: Any, y: Any) {
        val some: Minecraft
    }
    inline fun hello() = println("Hello from script1.kts!")
}
try {
    val propertyName by lazy {

    }
} catch (e: Exception) {
    TODO("Not yet implemented")
}
class SomeEntity(entityType: EntityType<out LivingEntity>, level: Level) : LivingEntity(entityType, level) {
    override fun getArmorSlots(): MutableIterable<ItemStack> {
        TODO("Not yet implemented")
    }

    override fun getItemBySlot(slot: EquipmentSlot): ItemStack {
        TODO("Not yet implemented")
    }

    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
        TODO("Not yet implemented")
    }

    override fun getMainArm(): HumanoidArm {
        TODO("Not yet implemented")
    }

    override fun spawnAtLocation(stack: ItemStack, offsetY: Float): ItemEntity? {
        stack.item.isFoil(stack)
        moveDist
        return super.spawnAtLocation(stack, offsetY)
    }
    override fun updateSwimming() {
        //super.updateSwimming()

    }
}