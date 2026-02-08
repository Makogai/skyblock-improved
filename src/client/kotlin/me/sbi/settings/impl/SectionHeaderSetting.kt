package me.sbi.settings.impl

import me.sbi.gui.GuiConstants
import me.sbi.gui.GuiStyle
import me.sbi.settings.RenderableSetting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * Expandable section header. Right-click to expand/collapse.
 * Not saved to config.
 */
class SectionHeaderSetting(
    name: String,
    private val icon: String = "»"
) : RenderableSetting<Unit>(name, "") {

    override val default: Unit get() = Unit
    override var value: Unit
        get() = Unit
        set(_) {}

    var expanded: Boolean = true

    override fun getHeight(): Float = 18f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val w = GuiConstants.SETTING_WIDTH.toInt()
        val h = getHeight().toInt()

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt(), 0xFF0D0D12.toInt())
        graphics.fill(x.toInt(), (y + h - 1).toInt(), (x + w).toInt(), (y + h).toInt(), 0xFF2A2A35.toInt())

        val accentColor = when (GuiStyle.current) {
            GuiStyle.MINIMAL -> GuiStyle.accent
            GuiStyle.RPG -> GuiStyle.accent
        }
        graphics.drawString(mc.font, "$icon $name", (x + 6).toInt(), (y + h / 2 - 4).toInt(), accentColor, false)

        val expandIcon = if (expanded) "\u25BC" else "\u25B6"
        val iconW = mc.font.width(expandIcon)
        graphics.drawString(mc.font, expandIcon, (x + w - iconW - 6).toInt(), (y + h / 2 - 4).toInt(), 0xFF808080.toInt(), false)

        return getHeight()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 1) return false
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        if (mouseX >= lastX && mouseX <= lastX + w && mouseY >= lastY && mouseY <= lastY + getHeight()) {
            expanded = !expanded
            return true
        }
        return false
    }
}
