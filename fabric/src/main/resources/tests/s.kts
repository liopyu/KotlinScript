package some

import java.lang.Math
import kotlin.Unit
import java.lang.Exception
import kotlin.Float
import kotlin.Int

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.class_290
import net.minecraft.class_289
import net.minecraft.class_291
import net.minecraft.class_293
import com.mojang.logging.LogUtils
import net.minecraft.class_310
import net.minecraft.class_332
import net.minecraft.class_437
import net.minecraft.class_757
import net.minecraft.class_2378
import net.minecraft.class_7923
import net.minecraft.class_2561
import net.minecraft.class_2960
import net.minecraft.class_1268
import net.minecraft.class_1271
import net.minecraft.class_1657
import net.minecraft.class_1777
import net.minecraft.class_1792
import net.minecraft.class_1799
import net.minecraft.class_1937

var onItemUse: (net.minecraft.class_1937, net.minecraft.class_1657, net.minecraft.class_1268) -> net.minecraft.class_4428 =
    { level, player, hand ->
        try {
            net.minecraft.class_310.method_1551().execute {
                net.minecraft.class_310.method_1551().setScreen(SimpleScreen())
            }
            net.minecraft.class_310.method_1551().play
        } catch (exception: Exception) {
            LogUtils.getLogger().error("Error using custom ender eye.", exception)
        }
    }

class SomeEnderEyeItem(settings: net.minecraft.class_1792.Properties) : net.minecraft.class_1777(settings) {
    override fun use(
        level: net.minecraft.class_1937,
        player: net.minecraft.class_1657,
        hand: net.minecraft.class_1268
    ): net.minecraft.class_1271<net.minecraft.class_1799> {
        onItemUse(level, player, hand)
        return super.use(level, player, hand)
    }
}

val myItem = SomeEnderEyeItem(net.minecraft.class_1792.Properties())
LogUtils.getLogger().info(myItem::class.qualifiedName)
net.minecraft.class_2378.method_10226(
    net.minecraft.class_7923.field_41178,
    net.minecraft.class_2960.method_60655("kotlinscript", "some_ender_eye_item"),
    myItem
)
net.minecraft.class_2378.method_10230(
    net.minecraft.class_7923.field_41178,
    net.minecraft.class_2960.method_60655("kotlinscript", "some_ender_eye_item"),
    myItem
)
class SimpleScreen : net.minecraft.class_437(net.minecraft.class_2561.method_43470("Simple net.minecraft.class_437")) {
    override fun renderBackground(guiGraphics: net.minecraft.class_332, mouseX: Int, mouseY: Int, partialTicks: Float) {
    }

    override fun render(guiGraphics: net.minecraft.class_332, mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawRoundedRect(
            guiGraphics,
            width / 2 - 50,
            height / 2 - 20,
            100,
            40,
            8,
            0xAA000000.toInt()
        )
        guiGraphics.drawCenteredString(
            font,
            "Hello from KotlinScript net.minecraft.class_437!",
            width / 2,
            height / 2 - font.lineHeight / 2,
            0xFFFFFF
        )
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }


    override fun shouldCloseOnEsc(): Boolean = true
}

fun drawRoundedRect(
    guiGraphics: net.minecraft.class_332,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    radius: Int,
    color: Int
) {
    val pose = guiGraphics.pose()
    val segments = 12

    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    RenderSystem.disableDepthTest()

    val tesselator = net.minecraft.class_289.method_1348()
    val buffer = tesselator.begin(net.minecraft.class_293.Mode.TRIANGLE_FAN, net.minecraft.class_290.field_1576)

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
    vertex(cx, cy)

    for (i in 0..segments) {
        val angle = Math.PI + Math.PI / 2 * (i.toDouble() / segments)
        vertex((x + radius + Math.cos(angle) * radius).toFloat(), (y + radius + Math.sin(angle) * radius).toFloat())
    }

    /*  for (i in 0..segments) {
         val angle = -Math.PI / 2 + Math.PI / 2 * (i.toDouble() / segments)
         vertex(
             (x + width - radius + Math.cos(angle) * radius).toFloat(),
             (y + radius + Math.sin(angle) * radius).toFloat()
         )
     }

     for (i in 0..segments) {
         val angle = 0.0 + Math.PI / 2 * (i.toDouble() / segments)
         vertex(
             (x + width - radius + Math.cos(angle) * radius).toFloat(),
             (y + height - radius + Math.sin(angle) * radius).toFloat()
         )
     } */

    /*  for (i in 0..segments) {
         val angle = Math.PI / 2 + Math.PI / 2 * (i.toDouble() / segments)
         vertex(
             (x + radius + Math.cos(angle) * radius).toFloat(),
             (y + height - radius + Math.sin(angle) * radius).toFloat()
         )
     } */

    val mesh = buffer.buildOrThrow()
    val vertexBuffer = net.minecraft.class_291(net.minecraft.class_291.Usage.STATIC)
    vertexBuffer.upload(mesh)

    val shader = net.minecraft.class_757.method_34540()
    val modelViewMatrix = pose.last().pose()
    val projectionMatrix = RenderSystem.getProjectionMatrix()

    vertexBuffer.bind()
    if (shader != null) {
        vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader)
    }
    net.minecraft.class_291.method_1354()

    RenderSystem.enableDepthTest()
    RenderSystem.disableBlend()
}