package me.sbi.modules.skyblock

import me.sbi.core.Category
import me.sbi.core.Module
import org.lwjgl.glfw.GLFW

/**
 * Invisible module that always updates SkyBlockData every tick when on Hypixel.
 * Other modules read from SkyBlockData; this just keeps it fresh.
 */
object SkyBlockDataModule : Module(
    name = "SkyBlock Data",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.SKYBLOCK,
    description = "Internal: parses tab list and scoreboard",
    toggled = true,
    internal = true
) {
    override fun onTick() {
        SkyBlockData.update()
    }

    override fun onKeybind() {}
}
