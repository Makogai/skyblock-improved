package me.sbi.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.sbi.gui.GuiConstants
import me.sbi.settings.RenderableSetting
import me.sbi.settings.Saving
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class DropdownSetting(
    name: String,
    override val default: String,
    val options: List<String>,
    desc: String = ""
) : RenderableSetting<String>(name, desc), Saving {

    override var value: String = default
        set(v) { field = if (v in options) v else default }

    private var expanded = false

    override fun getHeight(): Float {
        return if (expanded) 28f + options.size * 22f else 28f
    }

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val w = GuiConstants.SETTING_WIDTH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + 28).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + 27).toInt(), (x + w).toInt(), (y + 28).toInt(), 0xFF2A2A2A.toInt())

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + 8).toInt(), 0xFFE8E8E8.toInt(), true)
        graphics.drawString(mc.font, value, (x + w - 8 - mc.font.width(value)).toInt(), (y + 8).toInt(), 0xFFB0B0B0.toInt(), true)
        graphics.drawString(mc.font, if (expanded) "v" else ">", (x + w - 16).toInt(), (y + 8).toInt(), 0xFF808080.toInt(), true)

        var optY = y + 28
        if (expanded) {
            for (opt in options) {
                val optColor = if (opt == value) me.sbi.gui.GuiStyle.accentDark else 0xFF1A1A1A.toInt()
                graphics.fill(x.toInt(), optY.toInt(), (x + w).toInt(), (optY + 22).toInt(), optColor)
                graphics.drawString(mc.font, opt, (x + 12).toInt(), (optY + 6).toInt(), 0xFFE8E8E8.toInt(), true)
                optY += 22
            }
        }

        return optY - y
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()

        if (mouseY >= lastY && mouseY < lastY + 28) {
            if (mouseX >= lastX && mouseX <= lastX + w) {
                expanded = !expanded
                return true
            }
        }

        if (expanded) {
            var optY = lastY + 28
            for (opt in options) {
                if (mouseX >= lastX && mouseX <= lastX + w && mouseY >= optY && mouseY < optY + 22) {
                    value = opt
                    expanded = false
                    return true
                }
                optY += 22
            }
        }
        return false
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement, gson: Gson) {
        element.asString?.let { if (it in options) value = it }
    }
}
