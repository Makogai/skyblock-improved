package me.sbi.core

import me.sbi.SkyblockImproved
import me.sbi.config.ModuleConfig
import me.sbi.modules.general.CoordsDisplayModule
import me.sbi.modules.general.NameChangerModule
import me.sbi.modules.mining.ExampleMiningModule
import me.sbi.modules.render.ClickGuiModule
import me.sbi.modules.render.EspModule
import me.sbi.modules.render.PartyDisplayModule
import me.sbi.modules.skyblock.AreaDisplayModule
import me.sbi.modules.skyblock.ChatParserModule
import me.sbi.modules.skyblock.ModSyncModule
import me.sbi.modules.skyblock.PartyGuiModule
import me.sbi.modules.skyblock.SkyBlockDataModule
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import java.io.File

/**
 * Central manager for all modules. Handles registration, tick events, and config.
 */
object ModuleManager {

    val modules = linkedMapOf<String, Module>()
    val modulesByCategory = hashMapOf<Category, MutableList<Module>>()
    private val configs = mutableListOf<ModuleConfig>()
    private val keybindCache = mutableListOf<me.sbi.settings.impl.KeybindSetting>()

    private val configFile = File(SkyblockImproved.configDir, "config.json")

    init {
        val config = ModuleConfig(configFile)

        // Register modules - add your modules here. Exclude via ModuleRegistry.DISABLED_MODULES
        val allModules = listOf(
            ClickGuiModule, NameChangerModule, CoordsDisplayModule, ExampleMiningModule,
            SkyBlockDataModule, AreaDisplayModule, ChatParserModule, PartyDisplayModule,
            PartyGuiModule, ModSyncModule, EspModule, me.sbi.modules.general.TrollModule
        )
        val active = allModules.filter { ModuleRegistry.isEnabled(it) }
        registerModules(config, *active.toTypedArray())

        if (ClickGuiModule.panelPositions.isEmpty()) {
            ClickGuiModule.resetPanelPositions()
        }

        config.load()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level == null || client.player == null) return@register

            for (module in modules.values) {
                if (module.enabled) module.onTick()
            }

            // Keybind handling - only fire on key press (not hold)
            for (setting in keybindCache) {
                if (setting.value.value == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) continue
                val isDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(client.window, setting.value.value)
                if (isDown && !setting.wasKeyDown) {
                    setting.onPress?.invoke()
                }
                setting.wasKeyDown = isDown
            }
        }
    }

    fun registerModules(config: ModuleConfig, vararg modules: Module) {
        for (module in modules) {
            val key = module.name.lowercase()
            config.modules[key] = module
            this.modules[key] = module
            modulesByCategory.getOrPut(module.category) { mutableListOf() }.add(module)

            if (module.key != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
                val keybindSetting = me.sbi.settings.impl.KeybindSetting(
                    "Keybind",
                    module.key,
                    "Toggle this module"
                )
                keybindSetting.onPress = module::onKeybind
                module.registerSetting(keybindSetting)
                keybindCache.add(keybindSetting)
            }

            for ((_, setting) in module.settings) {
                if (setting is me.sbi.settings.impl.KeybindSetting) {
                    if (setting !in keybindCache) keybindCache.add(setting)
                }
            }
        }
        configs.add(config)
    }

    fun loadConfigurations() = configs.forEach { it.load() }
    fun saveConfigurations() = configs.forEach { it.save() }
}
