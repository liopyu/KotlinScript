package advanced

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KProperty

// ---------- Logging ----------
private val logger = LogUtils.getLogger()

// ---------- Property delegate (for fun state) ----------
class IntBox(initial: Int = 0) {
    private var v: Int = initial
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: Int) {
        v = value
    }
}

var clicks by IntBox(0)

// ---------- Sealed action model ----------
sealed class Action {
    object OpenScreen : Action()
    object PlayPling : Action()
    object None : Action()
}

// ---------- Extension + utilities ----------
inline val ItemStack.registryName: ResourceLocation?
    get() = BuiltInRegistries.ITEM.getKey(this.item)

operator fun ResourceLocation.plus(suffix: String): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(this.namespace, this.path + suffix)

private inline fun <reified T : Any> logType(tag: String, value: Any?) {
    logger.info("[KS:$tag] T=${T::class.qualifiedName}, value=$value")
}

fun decideAction(stack: ItemStack): Action = when {
    stack.isEmpty -> Action.None
    stack.hasFoil() -> Action.PlayPling
    else -> Action.OpenScreen
}

// ---------- Item ----------
class DebugWandItem(props: Item.Properties = Item.Properties().stacksTo(1)) : Item(props) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        // Show some registry/typed logging
        logType<Item>("use.registry", stack.registryName)
        logger.info("Using ${stack.registryName ?: ResourceLocation.parse("unknown:unknown")} with $hand")

        return try {
            when (decideAction(stack)) {
                is Action.OpenScreen -> {
                    Minecraft.getInstance().execute {
                        Minecraft.getInstance().setScreen(DebugScreen(stack.registryName?.toString() ?: "unknown"))
                    }
                }

                is Action.PlayPling -> {
                    level.playLocalSound(
                        player.x, player.y, player.z,
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                        1.0f, 1.0f, false
                    )
                }

                is Action.None -> logger.info("No action for empty stack.")
            }
            InteractionResultHolder.success(stack)
        } catch (e: Exception) {
            logger.error("Error using DebugWandItem", e)
            InteractionResultHolder.fail(stack)
        }
    }
}

// Register item with a slightly different ResourceLocation construction than your previous test
val DEBUG_WAND_ID: ResourceLocation = ResourceLocation.parse("kotlinscript:debug_wand")
val debugWandItem = DebugWandItem().also {
    logger.info("Registered class: ${it::class.qualifiedName}")
}

@Suppress("UNUSED_VARIABLE")
val REGISTRATION_HANDLE = Registry.register(BuiltInRegistries.ITEM, DEBUG_WAND_ID, debugWandItem)

// ---------- Screen ----------
class DebugScreen(private val info: String) : Screen(Component.literal("KotlinScript Debug UI")) {

    private var btn: Button? = null
    private var hue: Float = 0f

    companion object {
        private const val PAD = 10
        private const val BG = 0xAA151515.toInt()
        private const val ACCENT = 0xFF3FA7FF.toInt()
    }

    override fun init() {
        // Use builder-style API with named args for variety
        btn = addRenderableWidget(
            Button.builder(Component.literal("Clicks: $clicks")) { _ ->
                clicks++
                btn?.message = Component.literal("Clicks: $clicks")
                // Tiny visual ping: sound + hue shift
                minecraft?.level?.playLocalSound(
                    minecraft!!.player!!.x, minecraft!!.player!!.y, minecraft!!.player!!.z,
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                    0.5f, 0.8f + (clicks % 5) * 0.05f, false
                )
                hue = (hue + 0.07f) % 1f
            }.pos(width / 2 - 50, height / 2 + 30).size(100, 20).build()
        )
        super.init()
    }

    override fun shouldCloseOnEsc(): Boolean = true

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks)

        // Panel
        val panelW = 180
        val panelH = 110
        val x = width / 2 - panelW / 2
        val y = height / 2 - panelH / 2 - 10

        // High-level fill + our low-level rounded fan to vary code paths
        guiGraphics.fill(x, y, x + panelW, y + panelH, BG)
        drawRoundedFan(guiGraphics, x, y, panelW, panelH, 8, ACCENT and 0x33FFFFFF)

        // Text + info line
        guiGraphics.drawCenteredString(font, "Debug Wand UI", width / 2, y + PAD, 0xFFFFFF)
        guiGraphics.drawString(font, "Item: $info", x + PAD, y + PAD + font.lineHeight + 2, 0)

        // Fancy ring just to stress Tesselator path
        drawRing(guiGraphics, width / 2f, (y + 62).toFloat(), 24f, 6f, 32, colorFromHue(hue))
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    // Same signature override as before, but intentionally left empty again
    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {}

    // Low-level TRIANGLE_FAN rounded rect (different from your previous exact angles/loops)
    private fun drawRoundedFan(
        gg: GuiGraphics, x: Int, y: Int, w: Int, h: Int, r: Int, color: Int, segments: Int = 10
    ) {
        val pose = gg.pose()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()

        val t = Tesselator.getInstance()
        val buf = t.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)

        fun v(xf: Float, yf: Float) {
            buf.addVertex(pose.last().pose(), xf, yf, 0f).setColor(
                color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, color ushr 24 and 0xFF
            )
        }

        val cx = x + w / 2f
        val cy = y + h / 2f
        v(cx, cy)

        fun arc(cx: Float, cy: Float, start: Double, end: Double) {
            val steps = segments
            for (i in 0..steps) {
                val t0 = start + (end - start) * (i.toDouble() / steps)
                v((cx + cos(t0) * r).toFloat(), (cy + sin(t0) * r).toFloat())
            }
        }

        // Corners: TL -> TR -> BR -> BR arc -> BL arc, etc. (simple path)
        arc((x + r).toFloat(), (y + r).toFloat(), PI, 1.5 * PI)
        arc((x + w - r).toFloat(), (y + r).toFloat(), 1.5 * PI, 2.0 * PI)
        arc((x + w - r).toFloat(), (y + h - r).toFloat(), 0.0, 0.5 * PI)
        arc((x + r).toFloat(), (y + h - r).toFloat(), 0.5 * PI, PI)

        val mesh = buf.buildOrThrow()
        val shader = GameRenderer.getPositionColorShader()
        val model = pose.last().pose()
        val proj = RenderSystem.getProjectionMatrix()

        if (shader != null) {
            com.mojang.blaze3d.vertex.VertexBuffer(VertexBuffer.Usage.STATIC).use { vb ->
                vb.upload(mesh)
                vb.bind()
                vb.drawWithShader(model, proj, shader)
                VertexBuffer.unbind()
            }
        }

        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
    }

    private fun drawRing(
        gg: GuiGraphics,
        cx: Float,
        cy: Float,
        radius: Float,
        thickness: Float,
        segs: Int,
        color: Int
    ) {
        val pose = gg.pose()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()

        val t = Tesselator.getInstance()
        val buf = t.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

        fun v(xf: Float, yf: Float) {
            buf.addVertex(pose.last().pose(), xf, yf, 0f).setColor(
                color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, color ushr 24 and 0xFF
            )
        }

        for (i in 0..segs) {
            val a = (i.toFloat() / segs) * (2f * PI.toFloat())
            val ox = cos(a)
            val oy = sin(a)
            v(cx + ox * (radius - thickness), cy + oy * (radius - thickness))
            v(cx + ox * radius, cy + oy * radius)
        }

        val mesh = buf.buildOrThrow()
        val shader = GameRenderer.getPositionColorShader()
        val model = pose.last().pose()
        val proj = RenderSystem.getProjectionMatrix()
        if (shader != null) {
            com.mojang.blaze3d.vertex.VertexBuffer(VertexBuffer.Usage.STATIC).use { vb ->
                vb.upload(mesh)
                vb.bind()
                vb.drawWithShader(model, proj, shader)
                VertexBuffer.unbind()
            }
        }

        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
    }

    private fun colorFromHue(h: Float): Int {
        // tiny, branchy HSV->RGB-ish to stress when/smart casts
        val i = ((h * 6) % 6).toInt()
        val f = (h * 6) - i
        val q = (1 - f)
        fun c(v: Float) = (v * 255).toInt().coerceIn(0, 255)
        val (r, g, b) = when (i) {
            0 -> Triple(1f, f, 0f)
            1 -> Triple(q, 1f, 0f)
            2 -> Triple(0f, 1f, f)
            3 -> Triple(0f, q, 1f)
            4 -> Triple(f, 0f, 1f)
            else -> Triple(1f, 0f, q)
        }
        return (0xFF shl 24) or (c(r) shl 16) or (c(g) shl 8) or c(b)
    }
}

// ---------- Simple Auto-Open Helper (object singleton + default args) ----------
class DebugWandHelper {
    fun open(info: String = "n/a") {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().setScreen(DebugScreen(info))
        }
    }
}

// Small demo of operator/function references (could be used elsewhere)
val openHelper: (String) -> Unit = DebugWandHelper()::open

@Suppress("unused")
val alsoOpen = { s: String -> openHelper(s) }
