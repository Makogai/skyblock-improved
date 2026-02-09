package me.sbi.util

import me.sbi.SkyblockImproved
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Styled admin message display - "[SBI] Admin: message"
 */
object AdminMessage {

    private const val PREFIX = "[SBI]"

    @JvmStatic
    fun sendAdminMessage(message: String) {
        val mc = SkyblockImproved.mc
        val component = buildAdminMessage(message)
        mc.gui.chat.addMessage(component)
    }

    @JvmStatic
    fun buildAdminMessage(message: String): Component {
        val prefix = Component.literal("$PREFIX ")
            .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD))
        val adminPart = Component.literal("Admin")
            .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.RED))
        val colon = Component.literal(": ")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
        val msg = Component.literal(message)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))
        return prefix.append(adminPart).append(colon).append(msg)
    }
}
