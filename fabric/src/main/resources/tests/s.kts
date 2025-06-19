

import java.lang.reflect.Modifier
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import net.liopyu.kotlinscript.FabricBootstrap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
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

var onItemUse: (Level, Player, InteractionHand) -> Unit = { level, player, hand ->
    try {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().setScreen(SimpleScreen())
        }
    } catch (exception: Exception) {
        LogUtils.getLogger().error("Error using custom ender eye.", exception)
    }
}

class SomeEnderEyeItem(settings: Item.Properties) : EnderEyeItem(settings) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        onItemUse(level, player, hand)
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
/*EventHandlers.register(EndTick, { event ->
    println("EndTick event triggered")
})*/

// Modify grammar.js to make this syntax error
/* EventHandlers.register(ClientEvents.END_TICK) { minecraft ->
    println("End client tick! Player = ${minecraft.player?.name?.string}")
} */
class SimpleScreen : Screen(Component.literal("Simple Screen")) {
    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Completely replace default blur+panorama
        // guiGraphics.fill(0, 0, width, height, 0xFF000000.toInt())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawRoundedRect(
            guiGraphics,
            width / 2 - 50,
            height / 2 - 20,
            100,
            40,
            8,
            0xAA000000.toInt()
        ) // Semi-transparent black
        guiGraphics.drawCenteredString(
            font,
            "Hello from KotlinScript Screen!",
            width / 2,
            height / 2 - font.lineHeight / 2,
            0xFFFFFF
        )
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }


    override fun shouldCloseOnEsc(): Boolean = true
}

fun drawRoundedRect(guiGraphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
    val pose = guiGraphics.pose()
    val segments = 12

    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    RenderSystem.disableDepthTest()

    val tesselator = Tesselator.getInstance()
    val buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)

    fun vertex(xf: Float, yf: Float) {
        buffer.addVertex(pose.last().pose(), xf, yf, 0f).setColor(
            color shr 16 and 0xFF,
            color shr 8 and 0xFF,
            color and 0xFF,
            color ushr 24 and 0xFF
        )
    }

    val cx = x + width / 2f
    val cy = y + height / 2f
    vertex(cx, cy) // center of rectangle

    // Top-left corner
    for (i in 0..segments) {
        val angle = Math.PI + Math.PI / 2 * (i.toDouble() / segments)
        vertex((x + radius + Math.cos(angle) * radius).toFloat(), (y + radius + Math.sin(angle) * radius).toFloat())
    }

    // Top-right corner
    for (i in 0..segments) {
        val angle = -Math.PI / 2 + Math.PI / 2 * (i.toDouble() / segments)
        vertex(
            (x + width - radius + Math.cos(angle) * radius).toFloat(),
            (y + radius + Math.sin(angle) * radius).toFloat()
        )
    }

    // Bottom-right corner
    for (i in 0..segments) {
        val angle = 0.0 + Math.PI / 2 * (i.toDouble() / segments)
        vertex(
            (x + width - radius + Math.cos(angle) * radius).toFloat(),
            (y + height - radius + Math.sin(angle) * radius).toFloat()
        )
    }

    // Bottom-left corner
    for (i in 0..segments) {
        val angle = Math.PI / 2 + Math.PI / 2 * (i.toDouble() / segments)
        vertex(
            (x + radius + Math.cos(angle) * radius).toFloat(),
            (y + height - radius + Math.sin(angle) * radius).toFloat()
        )
    }

    val mesh = buffer.buildOrThrow()
    val vertexBuffer = VertexBuffer(VertexBuffer.Usage.STATIC)
    vertexBuffer.upload(mesh)

    val shader = GameRenderer.getPositionColorShader()
    val modelViewMatrix = pose.last().pose()
    val projectionMatrix = RenderSystem.getProjectionMatrix()

    vertexBuffer.bind()
    if (shader != null) {
        vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader)
    }
    VertexBuffer.unbind()

    RenderSystem.enableDepthTest()
    RenderSystem.disableBlend()
}
FabricBootstrap.s
val bootstrap: FabricBootstrap = FabricBootstrap()
bootstrap.invoke()
class MyClass private constructor() {

    companion object {
        @JvmStatic
        operator fun invoke(): MyClass {
            println("From companion invoke")
            return MyClass()
        }
    }
}

kotlin.jvm.internal.PrimitiveSpreadBuilder<?>()

fun main() {
    val instance = MyClass() // Calls companion's invoke()
}
Boolean.TRUE

open class s {
    final fun some() {

    }
    protected final val minecraft: Minecraft
        get() {
            TODO()
        }
}

interface Nameable {
    fun getName(): Component
    fun hasCustomName(): Boolean {return true}
    fun getDisplayName(): Component {}
    fun getCustomName(): Component {}
}

