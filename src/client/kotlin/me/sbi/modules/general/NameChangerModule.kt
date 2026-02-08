package me.sbi.modules.general

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.KeybindSetting
import me.sbi.settings.impl.StringSetting
import org.lwjgl.glfw.GLFW

/**
 * Name Changer - set a custom display name (client-side).
 * When enabled, shows your custom name in the HUD.
 */
object NameChangerModule : Module(
    name = "Name Changer",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.GENERAL,
    description = "Display a custom name client-side"
) {
    val customName = StringSetting("Display name", "", 16, "The name to display when enabled").also { registerSetting(it) }
    val toggleKey = KeybindSetting("Keybind", GLFW.GLFW_KEY_N, "Toggle Name Changer").also {
        it.onPress = { toggle() }
        registerSetting(it)
    }
}
