package me.sbi.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.sbi.gui.GuiConstants
import me.sbi.settings.RenderableSetting
import me.sbi.settings.Saving
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class NumberSetting(
    name: String,
    override val default: Double,
    val min: Double,
    val max: Double,
    val step: Double = 1.0,
    val decimals: Int = 1,
    desc: String = ""
) : RenderableSetting<Double>(name, desc), Saving {

    override var value: Double = default
        set(v) { field = v.coerceIn(min, max) }

    private var dragging = false

    override fun getHeight(): Float = 30f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = getHeight()
        val w = GuiConstants.SETTING_WIDTH

        if (dragging) {
            val sliderX = x + 8
            val sliderW = w - 16
            val pct = ((mouseX - sliderX) / sliderW).coerceIn(0f, 1f)
            value = min + (max - min) * pct
        }

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + height - 1).toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF2A2A2A.toInt())

        val sliderX = x + 8
        val sliderY = y + 18
        val sliderW = w - 16
        val sliderH = 6f

        graphics.fill(sliderX.toInt(), sliderY.toInt(), (sliderX + sliderW).toInt(), (sliderY + sliderH).toInt(), 0xFF333333.toInt())
        val pct = ((value - min) / (max - min)).toFloat().coerceIn(0f, 1f)
        graphics.fill(sliderX.toInt(), sliderY.toInt(), (sliderX + sliderW * pct).toInt(), (sliderY + sliderH).toInt(), me.sbi.gui.GuiStyle.accent)

        val valueStr = if (decimals == 0) value.toInt().toString() else "%.${decimals}f".format(value)
        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + 4).toInt(), 0xFFE8E8E8.toInt(), true)
        graphics.drawString(mc.font, valueStr, (x + w - 8 - mc.font.width(valueStr)).toInt(), (y + 4).toInt(), 0xFFB0B0B0.toInt(), true)

        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val sliderY = lastY + 18
        if (mouseX >= lastX && mouseX <= lastX + w && mouseY >= lastY && mouseY <= lastY + getHeight()) {
            dragging = true
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        if (button == 0) dragging = false
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement, gson: Gson) { value = element.asDouble }
}
