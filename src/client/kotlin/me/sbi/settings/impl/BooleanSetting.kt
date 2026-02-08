package me.sbi.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.sbi.gui.GuiConstants
import me.sbi.gui.GuiStyle
import me.sbi.settings.RenderableSetting
import me.sbi.settings.Saving
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class BooleanSetting(
    name: String,
    override val default: Boolean = false,
    desc: String = "",
    /** Called when value changes (e.g. from GUI toggle) */
    var onChange: ((Boolean) -> Unit)? = null
) : RenderableSetting<Boolean>(name, desc), Saving {

    override var value: Boolean = default

    override fun getHeight(): Float = 26f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = getHeight()
        val w = GuiConstants.SETTING_WIDTH

        // Setting row
        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + height - 1).toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF2A2A2A.toInt())

        // Standard toggle: OFF = gray track, knob left | ON = colored track, knob right
        val trackW = 38
        val trackH = 14
        val knobSize = 12
        val pad = 2
        val toggleX = (x + w - trackW - 8).toInt()
        val toggleY = (y + height / 2 - trackH / 2).toInt()
        val knobY = toggleY + (trackH - knobSize) / 2
        val knobX = if (value) toggleX + trackW - knobSize - pad else toggleX + pad

        val hovered = mouseX >= toggleX && mouseX <= toggleX + trackW && mouseY >= toggleY && mouseY <= toggleY + trackH

        if (value) {
            // ON: colored track (accent), knob on right
            graphics.fill(toggleX, toggleY, toggleX + trackW, toggleY + trackH, GuiStyle.accent)
            graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFFFFFFF.toInt())
        } else {
            // OFF: grayish track, knob on left
            val trackGray = if (hovered) 0xFF4A4A4A.toInt() else 0xFF404040.toInt()
            graphics.fill(toggleX, toggleY, toggleX + trackW, toggleY + trackH, trackGray)
            graphics.fill(toggleX, toggleY + trackH - 1, toggleX + trackW, toggleY + trackH, 0xFF2A2A2A.toInt())
            graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFE8E8E8.toInt())
            graphics.fill(knobX, knobY + knobSize - 1, knobX + knobSize, knobY + knobSize, 0xFFC0C0C0.toInt())
        }

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + height / 2 - 4).toInt(), 0xFFE8E8E8.toInt(), true)

        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val trackW = 38
        val trackH = 14
        val toggleX = lastX + w - trackW - 8
        val toggleY = lastY + getHeight() / 2 - trackH / 2
        if (mouseX >= toggleX && mouseX <= toggleX + trackW && mouseY >= toggleY && mouseY <= toggleY + trackH) {
            value = !value
            onChange?.invoke(value)
            return true
        }
        return false
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement, gson: Gson) { value = element.asBoolean }
}
