package me.sbi.modules.skyblock

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.BooleanSetting
import org.lwjgl.glfw.GLFW

/**
 * Displays current SkyBlock area and sub-area (from tab list + scoreboard).
 * For testing SkyBlockData parsing.
 */
object AreaDisplayModule : Module(
    name = "Area Display",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.SKYBLOCK,
    description = "Shows current area and sub-area from SkyBlock data"
) {
    val showSubArea = BooleanSetting("Show sub-area", true).also { registerSetting(it) }
    val showAvailability = BooleanSetting("Show tab/scoreboard status", false).also { registerSetting(it) }
}
