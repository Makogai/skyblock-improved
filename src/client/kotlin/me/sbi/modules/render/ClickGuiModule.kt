package me.sbi.modules.render

import me.sbi.SkyblockImproved
import me.sbi.core.Category
import me.sbi.util.SbiLog
import me.sbi.util.SbiMessage
import me.sbi.core.Module
import me.sbi.gui.ClickGui
import me.sbi.gui.Panel
import me.sbi.settings.impl.ActionSetting
import me.sbi.settings.impl.BooleanSetting
import me.sbi.settings.impl.DropdownSetting
import org.lwjgl.glfw.GLFW

/**
 * Module that opens the overlay config GUI. Right Shift by default.
 */
object ClickGuiModule : Module(
    name = "Click GUI",
    key = GLFW.GLFW_KEY_RIGHT_SHIFT,
    category = Category.RENDER,
    description = "Opens the module configuration overlay"
) {
    val overlayOpacity = me.sbi.settings.impl.NumberSetting("Overlay opacity", 0.75, 0.0, 1.0, 0.05, 2, "Config background opacity").also { registerSetting(it) }
    val chatNotifications = BooleanSetting("Chat notifications", true, "Notify when toggling modules via keybind").also { registerSetting(it) }
    val debugMode = BooleanSetting("Debug mode", false, "Enable debug logs and chat parser tracing") { enabled ->
        SbiLog.setDebug(enabled)
        SbiMessage.setDebug(enabled)
    }.also { registerSetting(it) }
    val theme = DropdownSetting("Theme", "Blue", listOf("Blue", "Cyan"), "Accent color theme").also { registerSetting(it) }
    val guiStyle = DropdownSetting("GUI style", "Minimal", listOf("Minimal", "RPG"), "Minimal = clean flat, RPG = stylized for fantasy games").also { registerSetting(it) }
    val resetBtn = ActionSetting("Reset panels", "Reset", "Reset all panel positions") { resetPanelPositions() }.also { registerSetting(it) }
    val editHudBtn = ActionSetting("Edit GUI", "Edit HUD", "Drag to reposition HUD elements") {
        mc.setScreen(me.sbi.gui.EditHudScreen())
        SbiMessage.send("Config", "Edit HUD - drag elements, ESC to save")
    }.also { registerSetting(it) }

    val panelPositions = mutableMapOf<String, PanelData>()
    data class PanelData(var x: Float = 10f, var y: Float = 10f, var extended: Boolean = true)

    fun resetPanelPositions() {
        val mc = SkyblockImproved.mc
        val w: Int
        val h: Int
        when {
            mc.screen != null -> { w = mc.screen!!.width; h = mc.screen!!.height }
            mc.window != null -> {
                val scale = mc.window!!.guiScale.toDouble().coerceAtLeast(1.0)
                w = (mc.window!!.width / scale).toInt().coerceAtLeast(320)
                h = (mc.window!!.height / scale).toInt().coerceAtLeast(240)
            }
            else -> { w = 960; h = 540 } // fallback during early init (window not created yet)
        }
        val pw = Panel.WIDTH.toInt() + Panel.GAP
        val ph = 120
        val cols = ((w - 20) / pw).coerceAtLeast(1)
        Category.all().forEachIndexed { index, category ->
            val col = index % cols
            val row = index / cols
            panelPositions[category.name] = PanelData(10f + col * pw, 10f + row * ph, true)
        }
    }

    override fun onKeybind() {
        toggle()
    }

    override fun onEnable() {
        mc.setScreen(ClickGui)
        super.onEnable()
        toggle() // Close immediately - screen is now open
    }
}
