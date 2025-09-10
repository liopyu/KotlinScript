package some

import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

var onItemUse: (net.minecraft.world.level.Level, net.minecraft.world.entity.player.Player, net.minecraft.world.InteractionHand) -> Unit =
    { _, _, _ ->
        try {
            Minecraft.getInstance().execute {
                Minecraft.getInstance().setScreen(SimpleScreen())
            }
        } catch (t: Throwable) {
            LogUtils.getLogger().error("Error opening screen", t)
        }
    }

class SomeEnderEyeItem(settings: net.minecraft.world.item.Item.Properties) : net.minecraft.world.item.Item(settings) {
    override fun use(
        level: net.minecraft.world.level.Level,
        player: net.minecraft.world.entity.player.Player,
        hand: net.minecraft.world.InteractionHand
    ): net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> {
        onItemUse(level, player, hand)
        return super.use(level, player, hand)
    }
}

class SimpleScreen : Screen(Component.literal("Simple Screen")) {
    override fun isPauseScreen(): Boolean = true
}

val myItem = SomeEnderEyeItem(Item.Properties())

Registry.register(
    BuiltInRegistries.ITEM,
    ResourceLocation.fromNamespaceAndPath("kotlinscript", "some_ender_eye_item"),
    myItem
)