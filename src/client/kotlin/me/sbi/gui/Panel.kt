package me.sbi.gui

import me.sbi.core.Category
import me.sbi.core.ModuleManager
import me.sbi.gui.GuiStyle
import me.sbi.modules.render.ClickGuiModule
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

class Panel(private val category: Category) {

    private val panelData: ClickGuiModule.PanelData
        get() = ClickGuiModule.panelPositions.getOrPut(category.name) {
            ClickGuiModule.PanelData(10f, 10f, true)
        }

    private val allModuleButtons = ModuleManager.modulesByCategory[category]
        ?.filter { !it.internal }
        ?.sortedBy { it.name }
        ?.map { ModuleButton(it) }
        ?: emptyList()

    var searchQuery: String = ""
        set(value) { field = value.trim() }

    private fun moduleButtons(): List<ModuleButton> =
        if (searchQuery.isEmpty()) allModuleButtons
        else allModuleButtons.filter { it.matchesSearch(searchQuery) }

    private var contentHeight = 0f
    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    fun containsPoint(mx: Int, my: Int): Boolean {
        val data = panelData
        val maxH = if (panelData.extended) contentHeight else HEADER_HEIGHT
        return mx >= data.x && mx <= data.x + WIDTH &&
               my >= data.y && my <= data.y + maxH
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val data = panelData
        val mc = Minecraft.getInstance()

        if (dragging) {
            data.x = (mouseX - dragOffsetX).coerceIn(0f, graphics.guiWidth() - WIDTH)
            data.y = (mouseY - dragOffsetY).coerceIn(0f, graphics.guiHeight() - HEADER_HEIGHT)
        }

        val x = data.x.toInt()
        val y = data.y.toInt()

        // 1. HEADER - Category icon + title (style-dependent)
        GuiStyle.current.panelHeaderColor(graphics, x, y, WIDTH.toInt(), HEADER_HEIGHT.toInt())
        val title = "${category.icon} ${category.name}"
        val titleX = x + (WIDTH.toInt() - mc.font.width(title)) / 2
        val titleY = y + (HEADER_HEIGHT.toInt() - 9) / 2
        graphics.drawString(mc.font, title, titleX, titleY, 0xFFFFFFFF.toInt(), true)

        contentHeight = HEADER_HEIGHT
        if (data.extended) {
            var moduleY = data.y + HEADER_HEIGHT
            for (button in moduleButtons()) {
                contentHeight += button.render(graphics, data.x, moduleY, mouseX, mouseY)
                moduleY += button.getHeight()
            }
        }

        if (data.extended && moduleButtons().isNotEmpty()) {
            graphics.fill(x, (data.y + contentHeight - 1).toInt(), x + WIDTH.toInt(), (data.y + contentHeight).toInt(), 0xFF0D0D0D.toInt())
        }
    }

    fun mouseClicked(mx: Int, my: Int, button: Int): Boolean {
        val data = panelData
        val x = data.x
        val y = data.y

        // Header
        if (mx >= x && mx <= x + WIDTH && my >= y && my < y + HEADER_HEIGHT) {
            when (button) {
                0 -> {
                    dragOffsetX = mx - x
                    dragOffsetY = my - y
                    dragging = true
                }
                1 -> data.extended = !data.extended
            }
            return true
        }

        if (!data.extended) return false
        if (mx < x || mx > x + WIDTH) return false

        var moduleY = y + HEADER_HEIGHT
        for (mb in moduleButtons()) {
            val h = mb.getHeight()
            if (my >= moduleY && my < moduleY + h) {
                return mb.mouseClicked(mx, my, x.toInt(), moduleY.toInt(), button)
            }
            moduleY += h
        }
        return false
    }

    fun mouseReleased(mx: Int, my: Int, button: Int) {
        if (button == 0) dragging = false
        if (panelData.extended) {
            var moduleY = panelData.y + HEADER_HEIGHT
            for (mb in moduleButtons()) {
                mb.mouseReleased(mx, my, button)
                moduleY += mb.getHeight()
            }
        }
    }

    fun mouseScrolled(mx: Int, my: Int, amount: Double): Boolean = panelData.extended

    fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (!panelData.extended) return false
        return moduleButtons().any { it.keyPressed(event) }
    }

    fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (!panelData.extended) return false
        return moduleButtons().any { it.charTyped(event) }
    }

    companion object {
        const val WIDTH = 200f
        const val HEADER_HEIGHT = 24f
        const val GAP = 6
    }
}
