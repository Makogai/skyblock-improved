package me.sbi.modules.mining

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.BooleanSetting

/**
 * Example mining module - template for adding mining features.
 */
object ExampleMiningModule : Module(
    name = "Example Mining",
    key = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
    category = Category.MINING,
    description = "Template for mining module features"
) {
    val showOreInfo = BooleanSetting("Show ore info", false, "Display ore information when looking at ores").also { registerSetting(it) }

    override fun onTick() {
        // Add your mining logic here
    }
}
