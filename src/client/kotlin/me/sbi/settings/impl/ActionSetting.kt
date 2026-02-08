package me.sbi.settings.impl

import me.sbi.gui.GuiConstants
import me.sbi.settings.RenderableSetting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class ActionSetting(
    name: String,
    val buttonText: String = "Click",
    desc: String = "",
    val action: () -> Unit
) : RenderableSetting<Unit>(name, desc) {

    override val default: Unit get() = Unit
    override var value: Unit
        get() = Unit
        set(_) {}

    override fun getHeight(): Float = 30f

    override fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float {
        updateTooltipHover(x, y, mouseX, mouseY)
        val mc = Minecraft.getInstance()
        val height = getHeight()
        val w = GuiConstants.SETTING_WIDTH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF151515.toInt())
        graphics.fill(x.toInt(), (y + height - 1).toInt(), (x + w).toInt(), (y + height).toInt(), 0xFF2A2A2A.toInt())

        val btnW = mc.font.width(buttonText) + 16
        val btnX = (x + w - btnW - 8).toInt()
        val btnY = (y + height / 2 - 10).toInt()
        graphics.fill(btnX, btnY, btnX + btnW, btnY + 20, me.sbi.gui.GuiStyle.accent)
        graphics.drawString(mc.font, buttonText, btnX + 8, btnY + 6, 0xFFFFFFFF.toInt(), true)

        graphics.drawString(mc.font, name, (x + 8).toInt(), (y + 8).toInt(), 0xFFE8E8E8.toInt(), true)

        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false
        val mc = Minecraft.getInstance()
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val btnW = mc.font.width(buttonText) + 16
        val btnX = lastX + w - btnW - 8
        val btnY = lastY + getHeight() / 2 - 10

        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 20) {
            action()
            return true
        }
        return false
    }
}
