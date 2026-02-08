package me.sbi.modules.render

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.modules.skyblock.SkyBlockParty
import me.sbi.settings.impl.BooleanSetting
import me.sbi.settings.impl.NumberSetting
import org.lwjgl.glfw.GLFW

/**
 * Displays party members in HUD. Leader with crown.
 * Run /party list or join a party to populate data.
 */
object PartyDisplayModule : Module(
    name = "Party Display",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.RENDER,
    description = "Shows party members (leader with crown)"
) {
    /** Show [Rank] and Hypixel colors like /p l */
    val showHypixelStyle = BooleanSetting("Hypixel ranks & colors", true, "Show [MVP++], colors like /party list").also { registerSetting(it) }
    /** Show background box behind party list */
    val showBackground = BooleanSetting("Show background", true, "Draw background behind party list").also { registerSetting(it) }
    /** Background opacity when shown (0-100%) */
    val backgroundOpacity = NumberSetting("Background opacity", 55.0, 0.0, 100.0, 5.0, 0, "Opacity of background when shown")
        .withDependency { showBackground.value }
        .also { registerSetting(it) }
}
