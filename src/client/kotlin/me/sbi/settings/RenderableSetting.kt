package me.sbi.settings

import me.sbi.gui.ClickGui
import me.sbi.gui.GuiConstants
import me.sbi.utils.ui.HoverHandler
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

/**
 * Settings that can be rendered and interacted with in the ClickGUI.
 */
abstract class RenderableSetting<T>(
    name: String,
    desc: String = ""
) : Setting<T>(name, desc) {

    protected var lastX = 0f
    protected var lastY = 0f

    private val hoverHandler = HoverHandler(1000)

    abstract fun getHeight(): Float
    abstract fun render(graphics: GuiGraphics, x: Float, y: Float, mouseX: Int, mouseY: Int): Float

    /** Call at start of render for tooltip support (1s hover delay) */
    protected fun updateTooltipHover(x: Float, y: Float, mouseX: Int, mouseY: Int) {
        lastX = x
        lastY = y
        val w = GuiConstants.SETTING_WIDTH.toFloat()
        val h = getHeight()
        hoverHandler.handle(x, y, w, h, mouseX, mouseY)
        if (description.isNotBlank() && hoverHandler.isHovered) {
            ClickGui.setTooltip(description, x + w + 10f, y, hoverHandler)
        }
    }
    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false
    open fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {}
    open fun keyTyped(event: CharacterEvent): Boolean = false
    open fun keyPressed(event: KeyEvent): Boolean = false
}
