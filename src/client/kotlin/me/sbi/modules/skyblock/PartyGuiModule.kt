package me.sbi.modules.skyblock

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.gui.PartyGuiScreen
import me.sbi.settings.impl.BooleanSetting
import me.sbi.settings.impl.KeybindSetting
import org.lwjgl.glfw.GLFW

/**
 * Party management GUI. Keybind opens full RPG-style party screen.
 */
object PartyGuiModule : Module(
    name = "Party GUI",
    key = GLFW.GLFW_KEY_P,
    category = Category.SKYBLOCK,
    description = "Full party management - invite, kick, member details"
) {
    val acceptAllInvites = BooleanSetting("Accept all invites", false, "Auto-accept party invites").also { registerSetting(it) }

    override fun onTick() {}
    override fun onKeybind() {
        me.sbi.SkyblockImproved.mc.setScreen(PartyGuiScreen())
    }
}
