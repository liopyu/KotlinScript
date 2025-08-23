import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

val myItem = Item(Item.Properties())
Registry.register(
    BuiltInRegistries.ITEM,
    ResourceLocation.fromNamespaceAndPath("kotlinscript", "some_ender_eye_item"),
    myItem
)