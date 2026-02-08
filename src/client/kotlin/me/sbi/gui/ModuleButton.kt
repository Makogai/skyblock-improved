package me.sbi.gui

import me.sbi.core.Module
import me.sbi.gui.ClickGui
import me.sbi.gui.GuiStyle
import me.sbi.settings.RenderableSetting
import me.sbi.settings.impl.SectionHeaderSetting
import me.sbi.utils.ui.HoverHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class ModuleButton(val module: Module) {

    private var extended = false
    private val moduleRowHover = HoverHandler(1000)
    private val renderableSettings = module.settings.values
        .filterIsInstance<RenderableSetting<*>>()
        .filter { it.isVisible }
        .toList()

    fun matchesSearch(query: String): Boolean {
        if (query.isEmpty()) return true
        val q = query.lowercase()
        if (module.name.lowercase().contains(q)) return true
        for (setting in module.settings.values) {
            if (setting.name.lowercase().contains(q)) return true
        }
        return false
    }

    private fun visibleSettings(): List<RenderableSetting<*>> {
        var sectionExpanded = true
        return renderableSettings.filter { s ->
            when (s) {
                is SectionHeaderSetting -> {
                    sectionExpanded = s.expanded
                    true
                }
                else -> sectionExpanded
            }
        }
    }

    fun getHeight(): Float {
        var h = MODULE_ROW_HEIGHT
        if (extended) {
            for (s in visibleSettings()) h += s.getHeight()
        }
        return h
    }

    fun render(graphics: GuiGraphics, panelX: Float, startY: Float, mouseX: Int, mouseY: Int): Float {
        val mc = Minecraft.getInstance()
        val x = panelX.toInt()
        var y = startY.toInt()

        val rowH = MODULE_ROW_HEIGHT.toInt()
        moduleRowHover.handle(panelX, startY, Panel.WIDTH, MODULE_ROW_HEIGHT, mouseX, mouseY)
        if (module.description.isNotBlank() && moduleRowHover.isHovered) {
            ClickGui.setTooltip(module.description, panelX + Panel.WIDTH + 10f, startY, moduleRowHover)
        }
        GuiStyle.current.moduleRowColor(graphics, x, y, Panel.WIDTH.toInt(), rowH, module.enabled)
        val textY = y + (rowH - 9) / 2
        graphics.drawString(mc.font, module.name, x + 10, textY, 0xFFFFFFFF.toInt(), true)
        if (renderableSettings.isNotEmpty()) {
            val expandIcon = if (extended) "\u25BC" else "\u25B6"
            val iconW = mc.font.width(expandIcon)
            graphics.drawString(mc.font, expandIcon, x + Panel.WIDTH.toInt() - iconW - 6, textY, 0xFF808080.toInt(), false)
        }
        var currentY = y + rowH

        if (extended) {
            for (setting in visibleSettings()) {
                val h = setting.render(graphics, panelX, currentY.toFloat(), mouseX, mouseY)
                currentY += h.toInt()
            }
        }

        return (currentY - startY.toInt()).toFloat()
    }

    fun mouseClicked(mx: Int, my: Int, panelX: Int, buttonTop: Int, button: Int): Boolean {
        val relY = my - buttonTop

        if (relY >= 0 && relY < MODULE_ROW_HEIGHT.toInt()) {
            when (button) {
                0 -> module.toggle()
                1 -> if (renderableSettings.isNotEmpty()) extended = !extended
            }
            return true
        }

        if (!extended) return false

        var settingY = buttonTop + MODULE_ROW_HEIGHT.toInt()
        for (setting in visibleSettings()) {
            val h = setting.getHeight().toInt()
            if (my >= settingY && my < settingY + h) {
                return setting.mouseClicked(mx.toFloat(), my.toFloat(), button)
            }
            settingY += h
        }
        return false
    }

    fun mouseReleased(mx: Int, my: Int, button: Int) {
        if (extended) {
            renderableSettings.forEach { it.mouseReleased(mx.toFloat(), my.toFloat(), button) }
        }
    }

    fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (!extended) return false
        return renderableSettings.any { it.keyPressed(event) }
    }

    fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (!extended) return false
        return renderableSettings.any { it.keyTyped(event) }
    }

    companion object {
        const val MODULE_ROW_HEIGHT = 20f
    }
}
