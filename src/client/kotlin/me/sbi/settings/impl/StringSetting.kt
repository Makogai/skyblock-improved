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

class StringSetting(
    name: String,
    override val default: String = "",
    val maxLength: Int = 32,
    desc: String = ""
) : RenderableSetting<String>(name, desc), Saving {

    override var value: String = default
    var listening: Boolean = false

    override fun getHeight(): Float = 30f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = getHeight()
        val w = GuiConstants.SETTING_WIDTH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill((x + 4).toInt(), (y + 4).toInt(), (x + w - 4).toInt(), (y + height - 4).toInt(), 0xFF1E1E1E.toInt())
        if (listening) {
            graphics.fill((x + 3).toInt(), (y + 3).toInt(), (x + w - 3).toInt(), (y + 4).toInt(), me.sbi.gui.GuiStyle.accent)
        }

        val displayText = if (listening) value + "_" else value.ifEmpty { "Click to type..." }
        val textColor = if (value.isEmpty() && !listening) 0xFF666666.toInt() else 0xFFE8E8E8.toInt()
        val labelColor = 0xFFE0E0E0.toInt()

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + 6).toInt(), labelColor, true)
        val maxTextW = w - 24
        val showText = if (mc.font.width(displayText) > maxTextW) {
            mc.font.plainSubstrByWidth(displayText, maxTextW - mc.font.width("...")) + "..."
        } else displayText
        graphics.drawString(mc.font, showText, (x + 8).toInt(), (y + 18).toInt(), textColor, true)

        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        if (mouseX >= lastX && mouseX <= lastX + w && mouseY >= lastY && mouseY <= lastY + getHeight()) {
            listening = true
            return true
        }
        return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!listening) return false
        when (event.key()) {
            GLFW.GLFW_KEY_ESCAPE -> { listening = false; return true }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { listening = false; return true }
            GLFW.GLFW_KEY_BACKSPACE -> { value = value.dropLast(1); return true }
            else -> return false
        }
    }

    override fun keyTyped(event: CharacterEvent): Boolean {
        if (!listening) return false
        val cp = event.codepoint()
        if (value.length < maxLength && Character.isBmpCodePoint(cp) && cp >= 32) {
            value += cp.toChar()
        }
        return true
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement, gson: Gson) { value = element.asString }
}
