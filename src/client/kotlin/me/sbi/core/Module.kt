package me.sbi.core

import me.sbi.SkyblockImproved
import me.sbi.settings.Setting
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * Base class for all feature modules.
 * Modules can be toggled on/off and have configurable settings.
 */
abstract class Module(
    val name: String,
    val key: Int = GLFW.GLFW_KEY_UNKNOWN,
    val category: Category,
    val description: String = "",
    toggled: Boolean = false,
    /** If true, module is hidden from ClickGui (for internal use like SkyBlockData) */
    val internal: Boolean = false,
    /** If true, module cannot be disabled by the player */
    val alwaysEnabled: Boolean = false
) {
    val settings = linkedMapOf<String, Setting<*>>()

    var enabled: Boolean = toggled
        private set

    protected val mc: Minecraft get() = SkyblockImproved.mc

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onTick() {}
    open fun onKeybind() { if (!alwaysEnabled) toggle() }

    fun toggle() {
        if (alwaysEnabled && enabled) return
        enabled = !enabled
        if (enabled) onEnable() else onDisable()
    }

    fun registerSetting(setting: Setting<*>): Setting<*> {
        settings[setting.name] = setting
        return setting
    }

    operator fun <T, S : Setting<T>> S.unaryPlus(): S {
        registerSetting(this)
        return this
    }
}
