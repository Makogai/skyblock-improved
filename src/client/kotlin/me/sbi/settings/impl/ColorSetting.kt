package me.sbi.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.sbi.gui.GuiConstants
import me.sbi.settings.RenderableSetting
import me.sbi.settings.Saving
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Color setting storing 0xAARRGGBB. Expandable HSB color picker like Odin.
 */
class ColorSetting(
    name: String,
    override val default: Int = 0xFFFFAA00.toInt(),
    private val allowAlpha: Boolean = false,
    desc: String = ""
) : RenderableSetting<Int>(name, desc), Saving {

    override var value: Int = default
        set(v) { field = v and 0xFFFFFFFF.toInt() }

    var expanded: Boolean = false
        private set

    private var draggingSection: Int? = null
    var hexListening: Boolean = false
        private set
    private var hexBuffer: StringBuilder = StringBuilder()

    override fun getHeight(): Float = when {
        expanded && allowAlpha -> 26f + 126f + 18f + 18f + 32f
        expanded -> 26f + 126f + 18f + 32f
        else -> 26f
    }

    fun red(): Int = (value shr 16) and 0xFF
    fun green(): Int = (value shr 8) and 0xFF
    fun blue(): Int = value and 0xFF
    fun alpha(): Int = (value shr 24) and 0xFF

    private fun toAwt(): Color = Color(value, true)
    private fun fromAwt(c: Color) {
        value = (c.alpha shl 24) or (c.red shl 16) or (c.green shl 8) or c.blue
    }

    private fun hue(): Float {
        val c = toAwt()
        return Color.RGBtoHSB(c.red, c.green, c.blue, null)[0]
    }

    private fun saturation(): Float = Color.RGBtoHSB(red(), green(), blue(), null)[1]
    private fun brightness(): Float = Color.RGBtoHSB(red(), green(), blue(), null)[2]
    private fun alphaF(): Float = alpha() / 255f

    private fun setHSB(h: Float, s: Float, b: Float, a: Float = alphaF()) {
        val rgb = Color.HSBtoRGB(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        val ai = (a.coerceIn(0f, 1f) * 255).roundToInt()
        value = (ai shl 24) or (rgb and 0x00FFFFFF)
    }

    private fun hexString(): String = if (allowAlpha) {
        "%02X%02X%02X%02X".format(red(), green(), blue(), alpha())
    } else {
        "%02X%02X%02X".format(red(), green(), blue())
    }

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = 26f
        val w = GuiConstants.SETTING_WIDTH

        if (draggingSection != null) {
            handleColorDrag(mouseX.toFloat(), mouseY.toFloat(), x, y, w.toFloat())
        }

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + height - 1).toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF2A2A2A.toInt())

        val boxSize = 20
        val boxX = (x + w - boxSize - 8).toInt()
        val boxY = (y + height / 2 - boxSize / 2).toInt()
        graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF000000.toInt() or (value and 0x00FFFFFF))
        graphics.fill(boxX, boxY, boxX + boxSize, boxY + 1, 0xFF555555.toInt())
        graphics.fill(boxX, boxY + boxSize - 1, boxX + boxSize, boxY + boxSize, 0xFF333333.toInt())

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + height / 2 - 4).toInt(), 0xFFE8E8E8.toInt(), true)
        graphics.drawString(mc.font, if (expanded) "v" else ">", (x + w - 28).toInt(), (y + height / 2 - 4).toInt(), 0xFF808080.toInt(), false)

        if (!expanded) return height

        var cy = y + height
        val pad = 6f
        val sbSize = 120f
        val barH = 12f

        // Saturation/Brightness square
        for (iy in 0 until 20) {
            for (ix in 0 until 20) {
                val sx = ix / 19f
                val by = 1f - iy / 19f
                val h = hue()
                val c = Color.getHSBColor(h, sx, by)
                val rx = x + pad + (ix * sbSize / 20)
                val ry = cy + (iy * sbSize / 20)
                graphics.fill(rx.toInt(), ry.toInt(), (rx + sbSize / 20 + 1).toInt(), (ry + sbSize / 20 + 1).toInt(), (0xFF000000.toInt() or (c.rgb and 0x00FFFFFF)))
            }
        }
        val sbPointerX = x + pad + saturation() * (sbSize - 6)
        val sbPointerY = cy + (1f - brightness()) * (sbSize - 6)
        graphics.fill(sbPointerX.toInt(), sbPointerY.toInt(), (sbPointerX + 6).toInt(), (sbPointerY + 6).toInt(), 0xFFFFFFFF.toInt())
        graphics.fill((sbPointerX + 1).toInt(), (sbPointerY + 1).toInt(), (sbPointerX + 5).toInt(), (sbPointerY + 5).toInt(), value)

        cy += sbSize + 6

        // Hue bar
        for (i in 0 until 24) {
            val h = i / 23f
            val c = Color.getHSBColor(h, 1f, 1f)
            val segW = (w - pad * 2) / 24
            val rx = x + pad + i * segW
            graphics.fill(rx.toInt(), cy.toInt(), (rx + segW + 1).toInt(), (cy + barH).toInt(), (0xFF000000.toInt() or (c.rgb and 0x00FFFFFF)))
        }
        val huePointerX = x + pad + hue() * (w - pad * 2 - 6)
        graphics.fill(huePointerX.toInt(), (cy - 1).toInt(), (huePointerX + 6).toInt(), (cy + barH + 1).toInt(), 0xFFFFFFFF.toInt())
        cy += barH + 6

        // Alpha bar (if allowAlpha)
        if (allowAlpha) {
            for (i in 0 until 24) {
                val a = i / 23f
                val c = Color(red(), green(), blue(), (a * 255).roundToInt())
                val segW = (w - pad * 2) / 24
                val rx = x + pad + i * segW
                graphics.fill(rx.toInt(), cy.toInt(), (rx + segW + 1).toInt(), (cy + barH).toInt(), c.rgb)
            }
            val alphaPointerX = x + pad + alphaF() * (w - pad * 2 - 6)
            graphics.fill(alphaPointerX.toInt(), (cy - 1).toInt(), (alphaPointerX + 6).toInt(), (cy + barH + 1).toInt(), 0xFFFFFFFF.toInt())
            cy += barH + 6
        }

        // Hex input area
        val hexH = 22f
        graphics.fill((x + pad).toInt(), cy.toInt(), (x + w - pad).toInt(), (cy + hexH).toInt(), 0xFF1A1A1A.toInt())
        if (hexListening) {
            graphics.fill((x + pad).toInt(), (cy + hexH - 1).toInt(), (x + w - pad).toInt(), (cy + hexH).toInt(), me.sbi.gui.GuiStyle.accent)
        }
        val hexStr = if (hexListening) hexBuffer.toString() + "_" else hexString()
        graphics.drawString(mc.font, hexStr.uppercase(), (x + pad + 4).toInt(), (cy + 6).toInt(), 0xFFE8E8E8.toInt(), false)

        return getHeight()
    }

    private fun handleColorDrag(mouseX: Float, mouseY: Float, x: Float, y: Float, w: Float) {
        val pad = 6f
        val sbSize = 120f
        val barH = 12f
        var cy = y + 26f

        when (draggingSection) {
            0 -> {
                val sx = ((mouseX - (x + pad)) / (sbSize - 1)).coerceIn(0f, 1f)
                val by = 1f - ((mouseY - (cy + 1)) / (sbSize - 1)).coerceIn(0f, 1f)
                setHSB(hue(), sx, by)
            }
            1 -> {
                cy += sbSize + 6
                val h = ((mouseX - (x + pad)) / (w - pad * 2 - 1)).coerceIn(0f, 1f)
                setHSB(h, saturation(), brightness())
            }
            2 -> if (allowAlpha) {
                cy += sbSize + 6 + barH + 6
                val a = ((mouseX - (x + pad)) / (w - pad * 2 - 1)).coerceIn(0f, 1f)
                val rgb = value and 0x00FFFFFF
                value = ((a * 255).roundToInt() shl 24) or rgb
            }
            else -> {}
        }
    }

    private fun hexAreaY(): Float {
        var cy = 26f + 120f + 6f + 12f + 6f
        if (allowAlpha) cy += 12f + 6f
        return lastY + cy
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val pad = 6f
        val sbSize = 120f
        val barH = 12f

        val boxSize = 20
        val boxX = lastX + w - boxSize - 8
        val boxY = lastY + 26f / 2 - boxSize / 2
        if (mouseX in boxX..(boxX + boxSize) && mouseY in boxY..(boxY + boxSize)) {
            expanded = !expanded
            if (!expanded) { hexListening = false; hexBuffer = StringBuilder() }
            return true
        }

        if (!expanded) return false

        var cy = lastY + 26f

        if (mouseX >= lastX + pad && mouseX <= lastX + pad + sbSize &&
            mouseY >= cy && mouseY <= cy + sbSize) {
            hexListening = false
            draggingSection = 0
            return true
        }
        cy += sbSize + 6

        if (mouseX >= lastX + pad && mouseX <= lastX + w - pad &&
            mouseY >= cy && mouseY <= cy + barH) {
            hexListening = false
            draggingSection = 1
            return true
        }
        cy += barH + 6

        if (allowAlpha && mouseX >= lastX + pad && mouseX <= lastX + w - pad &&
            mouseY >= cy && mouseY <= cy + barH) {
            hexListening = false
            draggingSection = 2
            return true
        }

        val hexY = hexAreaY()
        if (mouseX >= lastX + pad && mouseX <= lastX + w - pad &&
            mouseY >= hexY && mouseY <= hexY + 22) {
            hexListening = true
            hexBuffer = StringBuilder(hexString().lowercase())
            return true
        }

        hexListening = false
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0) draggingSection = null
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!hexListening) return false
        when (event.key()) {
            GLFW.GLFW_KEY_ESCAPE -> { hexListening = false; return true }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { hexListening = false; return true }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (hexBuffer.isNotEmpty()) { hexBuffer.deleteCharAt(hexBuffer.length - 1); applyHexBuffer() }
                return true
            }
        }
        return false
    }

    override fun keyTyped(event: CharacterEvent): Boolean {
        if (!hexListening) return false
        val c = event.codepoint().toChar()
        if (c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F') {
            val maxLen = if (allowAlpha) 8 else 6
            if (hexBuffer.length < maxLen) {
                hexBuffer.append(c)
                applyHexBuffer()
            }
            return true
        }
        return false
    }

    private fun applyHexBuffer() {
        val hex = hexBuffer.toString().filter { it in "0123456789AaBbCcDdEeFf" }
        val targetLen = if (allowAlpha) 8 else 6
        if (hex.length == targetLen) {
            try {
                val parsed = hex.toLong(16)
                value = if (allowAlpha) parsed.toInt() else 0xFF000000.toInt() or (parsed.toInt() and 0x00FFFFFF)
            } catch (_: Exception) {}
        }
    }

    fun collapse() {
        expanded = false
        hexListening = false
        draggingSection = null
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement, gson: Gson) { value = element.asInt }
}
