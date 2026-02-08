package me.sbi.gui

import me.sbi.core.Category
import me.sbi.core.ModuleManager
import me.sbi.gui.GuiStyle
import me.sbi.modules.render.ClickGuiModule
import me.sbi.utils.ui.HoverHandler
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Overlay config GUI - no scaling, raw screen coordinates for reliability.
 */
object ClickGui : Screen(Component.literal("Skyblock Improved")) {

    private val panels = Category.all()
        .filter { it in ModuleManager.modulesByCategory }
        .map { Panel(it) }

    private var searchBox: EditBox? = null

    private val helpText = "Left click: toggle module | Right click: expand panel/section | Drag header: move panel"

    private var tooltipText: String = ""
    private var tooltipX: Float = 0f
    private var tooltipY: Float = 0f
    private var tooltipHover: HoverHandler? = null

    fun setTooltip(text: String, x: Float, y: Float, hoverHandler: HoverHandler) {
        tooltipText = text
        tooltipX = x
        tooltipY = y
        tooltipHover = hoverHandler
    }

    init {
        if (ClickGuiModule.panelPositions.isEmpty()) {
            ClickGuiModule.resetPanelPositions()
        }
    }

    private var searchBuffer = ""

    override fun init() {
        super.init()
        val w = width
        val h = height
        val searchH = 20
        val searchY = h - searchH - 8
        val searchX = 8
        val searchW = (w - 24).coerceAtLeast(100)

        searchBox = EditBox(font, searchX, searchY, searchW, searchH, Component.literal("Search"))
            .apply {
                setHint(Component.literal("Filter modules..."))
                setMaxLength(64)
                setValue(searchBuffer)
            }
        addRenderableWidget(searchBox!!)
        setInitialFocus(searchBox!!)
    }

    private fun isAnySettingListening(): Boolean {
        for (module in ModuleManager.modules.values) {
            for (setting in module.settings.values) {
                when (setting) {
                    is me.sbi.settings.impl.StringSetting -> if (setting.listening) return true
                    is me.sbi.settings.impl.KeybindSetting -> if (setting.listening) return true
                    is me.sbi.settings.impl.ColorSetting -> if (setting.hexListening) return true
                    else -> {}
                }
            }
        }
        return false
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        searchBox?.let { box ->
            val boxVal = box.value
            if (boxVal != searchBuffer) searchBuffer = boxVal
            box.setValue(searchBuffer)
        }
        val searchQuery = searchBuffer.trim()
        panels.forEach { it.searchQuery = searchQuery }
        val w = graphics.guiWidth()
        val h = graphics.guiHeight()
        GuiStyle.current.overlayColor(graphics, 0, 0, w, h)

        // Clamp panels that are off-screen (e.g. from different resolution)
        val maxY = (h - FOOTER_HEIGHT - Panel.HEADER_HEIGHT - 10).coerceAtLeast(0f)
        for (data in ClickGuiModule.panelPositions.values) {
            data.x = data.x.coerceIn(0f, (w - Panel.WIDTH - 10).coerceAtLeast(0f))
            data.y = data.y.coerceIn(0f, maxY)
        }

        for (panel in panels) {
            panel.render(graphics, mouseX, mouseY)
        }

        val helpY = h - FOOTER_HEIGHT + 6
        graphics.drawString(font, helpText, 8, helpY, 0xFF808080.toInt(), false)

        if (tooltipText.isNotBlank() && tooltipHover?.percent() ?: 0f >= 100f) {
            renderTooltipBox(graphics, tooltipText, tooltipX.toInt(), tooltipY.toInt())
        }

        super.render(graphics, mouseX, mouseY, delta)
    }

    private fun renderTooltipBox(g: GuiGraphics, text: String, x: Int, y: Int) {
        val maxWidth = 260
        val lines = font.split(Component.literal(text), maxWidth)
        var boxW = 0
        for (line in lines) {
            boxW = maxOf(boxW, font.width(line))
        }
        val pad = 6
        val boxH = lines.size * (font.lineHeight + 2) + pad * 2
        boxW += pad * 2

        var tx = x
        var ty = y
        val screenW = g.guiWidth()
        val screenH = g.guiHeight()
        if (tx + boxW > screenW - 8) tx = screenW - boxW - 8
        if (ty + boxH > screenH - 8) ty = screenH - boxH - 8
        if (tx < 8) tx = 8
        if (ty < 8) ty = 8

        g.fill(tx, ty, tx + boxW, ty + boxH, 0xFF262626.toInt())
        g.fill(tx, ty, tx + boxW, ty + 1, 0xFF505050.toInt())
        g.fill(tx, ty + boxH - 1, tx + boxW, ty + boxH, 0xFF1A1A1A.toInt())
        g.fill(tx, ty, tx + 1, ty + boxH, 0xFF505050.toInt())
        g.fill(tx + boxW - 1, ty, tx + boxW, ty + boxH, 0xFF1A1A1A.toInt())

        for ((i, line) in lines.withIndex()) {
            g.drawString(font, line, tx + pad, ty + pad + i * (font.lineHeight + 2), 0xFFE8E8E8.toInt(), false)
        }
    }

    private val FOOTER_HEIGHT = 44

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, consume: Boolean): Boolean {
        val mx = mouseButtonEvent.x().toInt()
        val my = mouseButtonEvent.y().toInt()
        val h = height

        if (my >= h - FOOTER_HEIGHT) {
            return super.mouseClicked(mouseButtonEvent, consume)
        }

        for (panel in panels.reversed()) {
            if (panel.containsPoint(mx, my) && panel.mouseClicked(mx, my, mouseButtonEvent.button())) {
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, consume)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        val mx = mouseButtonEvent.x().toInt()
        val my = mouseButtonEvent.y().toInt()

        for (panel in panels) {
            panel.mouseReleased(mx, my, mouseButtonEvent.button())
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        for (panel in panels) {
            if (panel.containsPoint(mx, my) && panel.mouseScrolled(mx, my, scrollY)) {
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (super.keyPressed(keyEvent)) return true
        if (keyEvent.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        if (!isAnySettingListening()) {
            when (keyEvent.key()) {
                org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> {
                    if (searchBuffer.isNotEmpty()) {
                        searchBuffer = searchBuffer.dropLast(1)
                        return true
                    }
                }
            }
        }
        return panels.any { it.keyPressed(keyEvent) }
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        if (isAnySettingListening()) {
            if (super.charTyped(characterEvent)) return true
            return panels.any { it.charTyped(characterEvent) }
        }
        if (characterEvent.codepoint() in 32..126 && searchBuffer.length < 64) {
            searchBuffer += characterEvent.codepoint().toChar()
            return true
        }
        if (super.charTyped(characterEvent)) return true
        return panels.any { it.charTyped(characterEvent) }
    }

    override fun onClose() {
        for (module in ModuleManager.modules.values) {
            for (setting in module.settings.values) {
                when (setting) {
                    is me.sbi.settings.impl.StringSetting -> setting.listening = false
                    is me.sbi.settings.impl.KeybindSetting -> setting.listening = false
                    is me.sbi.settings.impl.ColorSetting -> setting.collapse()
                    else -> {}
                }
            }
        }
        ModuleManager.saveConfigurations()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}
