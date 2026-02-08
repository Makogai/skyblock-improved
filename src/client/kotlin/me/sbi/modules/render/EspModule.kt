package me.sbi.modules.render

import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.BooleanSetting
import me.sbi.settings.impl.ColorSetting
import me.sbi.settings.impl.DropdownSetting
import me.sbi.settings.impl.NumberSetting
import me.sbi.settings.impl.SectionHeaderSetting
import org.lwjgl.glfw.GLFW

/**
 * ESP module - highlights Invisibugs (Hunting) and party members.
 */
object EspModule : Module(
    name = "ESP",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.RENDER,
    description = "Highlight Invisibugs and party members"
) {

    val invisibugs = BooleanSetting("Invisibugs", true, "ESP for Invisibugs in Galatea")
    val partyMembers = BooleanSetting("Party members", true, "ESP for party members from /p l")
    val partyChunkRadius = NumberSetting("Scan chunks", 12.0, 4.0, 12.0, 1.0, 0, "Chunks (12 = SkyBlock max)")
    val espShowNames = BooleanSetting("Show names", true, "Show names above party members")

    val espDisplayStyle = DropdownSetting("Display style", "Standard", listOf("Standard", "Minecraft"), "Minecraft = white glow + black outline")
    val espColor = ColorSetting("Color", 0xFFFFAA00.toInt(), desc = "Fill/glow color")
    val espOutlineColor = ColorSetting("Outline color", 0xFF000000.toInt(), desc = "Minecraft style outline (black recommended)")
    val espRainbow = BooleanSetting("Rainbow", false, "Cycle colors over time")
    val espStyle = DropdownSetting("Box style", "Outline", listOf("Outline", "Filled"), "When using Standard display")
    val espSize = NumberSetting("Box size", 1.0, 0.3, 3.0, 0.1, 1, "Size multiplier")
    val espOpacity = NumberSetting("Opacity", 100.0, 0.0, 100.0, 5.0, 0, "Opacity %")

    private fun anyEspEnabled() = invisibugs.value || partyMembers.value

    init {
        registerSetting(SectionHeaderSetting("Hunting", "✦"))
        registerSetting(invisibugs)

        registerSetting(SectionHeaderSetting("Party", "◆"))
        registerSetting(partyMembers)
        registerSetting(partyChunkRadius.withDependency { partyMembers.value })
        registerSetting(espShowNames.withDependency { partyMembers.value })

        registerSetting(SectionHeaderSetting("Display", "◈"))
        registerSetting(espDisplayStyle)
        registerSetting(espColor.withDependency { anyEspEnabled() })
        registerSetting(espOutlineColor.withDependency { anyEspEnabled() && espDisplayStyle.value == "Minecraft" })
        registerSetting(espRainbow.withDependency { anyEspEnabled() })
        registerSetting(espStyle.withDependency { anyEspEnabled() && espDisplayStyle.value != "Minecraft" })
        registerSetting(espSize.withDependency { anyEspEnabled() })
        registerSetting(espOpacity.withDependency { anyEspEnabled() })
    }

    override fun onTick() {
        me.sbi.modules.skyblock.InvisibugScan.tick()
    }
    override fun onKeybind() {}
}
