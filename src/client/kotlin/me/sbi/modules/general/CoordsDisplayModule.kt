package me.sbi.modules.general

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.BooleanSetting
import me.sbi.settings.impl.NumberSetting

/**
 * Coords Display - shows your XYZ coordinates on screen when enabled.
 */
object CoordsDisplayModule : Module(
    name = "Coords Display",
    key = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
    category = Category.GENERAL,
    description = "Shows your XYZ coordinates on screen"
) {
    val showDimension = BooleanSetting("Show dimension", true, "Include dimension name").also { registerSetting(it) }
    val scale = NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1, 1, "Text size multiplier").also { registerSetting(it) }
}
