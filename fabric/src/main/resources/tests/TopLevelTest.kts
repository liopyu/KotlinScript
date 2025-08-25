import com.mojang.logging.LogUtils
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.EnderEyeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class SimpleScreen : Screen(Component.literal("Simple Screen")) {
    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.pose()
    }

    override fun shouldCloseOnEsc(): Boolean = true
}

class SomeEnderEyeItem(settings: Item.Properties) : EnderEyeItem(settings) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return super.use(level, player, hand)
    }
}

val myItem = SomeEnderEyeItem(Item.Properties())
LogUtils.getLogger().info(myItem::class.qualifiedName)
Registry.register(
    BuiltInRegistries.ITEM,
    ResourceLocation.fromNamespaceAndPath("kotlinscript", "some_ender_eye_item"),
    myItem
)