package me.sbi.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants
import me.sbi.gui.GuiConstants
import me.sbi.settings.RenderableSetting
import me.sbi.settings.Saving
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

class KeybindSetting(
    name: String,
    override val default: InputConstants.Key,
    desc: String = ""
) : RenderableSetting<InputConstants.Key>(name, desc), Saving {

    constructor(name: String, defaultKeyCode: Int, desc: String = "") :
        this(name, InputConstants.Type.KEYSYM.getOrCreate(defaultKeyCode), desc)

    override var value: InputConstants.Key = default
    var onPress: (() -> Unit)? = null
    var listening: Boolean = false
    var wasKeyDown: Boolean = false

    override fun getHeight(): Float = 26f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = getHeight()
        val w = GuiConstants.SETTING_WIDTH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + height - 1).toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF2A2A2A.toInt())

        val keyName = if (listening) "Press key..." else value.displayName.string
        val keyColor = if (listening) me.sbi.gui.GuiStyle.accent else 0xFFE8E8E8.toInt()

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + 6).toInt(), 0xFFE0E0E0.toInt(), true)
        graphics.drawString(mc.font, keyName, (x + w - 8 - mc.font.width(keyName)).toInt(), (y + 6).toInt(), keyColor, true)

        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val inBounds = mouseX >= lastX && mouseX <= lastX + w && mouseY >= lastY && mouseY <= lastY + getHeight()

        if (listening && button != 0) {
            value = InputConstants.Type.MOUSE.getOrCreate(button)
            listening = false
            return true
        }
        if (button == 0 && inBounds) {
            listening = true
            return true
        }
        return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!listening) return false
        when (event.key()) {
            GLFW.GLFW_KEY_ESCAPE -> value = InputConstants.UNKNOWN
            GLFW.GLFW_KEY_BACKSPACE -> value = InputConstants.UNKNOWN
            else -> value = InputConstants.Type.KEYSYM.getOrCreate(event.key())
        }
        listening = false
        return true
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value.name)
    override fun read(element: JsonElement, gson: Gson) {
        element.asString?.let { value = InputConstants.getKey(it) }
    }

    override fun reset() { value = default }
}
