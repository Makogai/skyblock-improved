package me.sbi.util

import me.sbi.SkyblockImproved
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Sends formatted messages to the player's chat.
 * Format: [SBI] (bold, colored) |module_name|: Message
 */
object SbiMessage {

    private const val PREFIX = "[SBI]"

    /**
     * Send a message to the player's chat in the standard format.
     * @param moduleName e.g. "Party", "Area", "ESP"
     * @param message The message text
     * @param prefixFormatting ChatFormatting for [SBI] (default AQUA)
     */
    @JvmStatic
    fun send(moduleName: String, message: String, prefixFormatting: ChatFormatting = ChatFormatting.AQUA) {
        val mc = SkyblockImproved.mc
        val component = buildMessage(moduleName, message, prefixFormatting)
        mc.gui.chat.addMessage(component)
    }

    /**
     * Build the Component without sending. Useful for HUD or other display.
     */
    @JvmStatic
    fun buildMessage(moduleName: String, message: String, prefixFormatting: ChatFormatting = ChatFormatting.AQUA): Component {
        val prefix = Component.literal("$PREFIX ")
            .withStyle(Style.EMPTY.withBold(true).withColor(prefixFormatting))
        val modulePart = Component.literal("|$moduleName|")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
        val colon = Component.literal(": ")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
        val msg = Component.literal(message)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))

        return prefix.append(modulePart).append(colon).append(msg)
    }

    /** Send an info-level message (also logs). */
    @JvmStatic
    fun info(moduleName: String, message: String) {
        SbiLog.info("[$moduleName] $message")
        send(moduleName, message)
    }

    /** Send a warning (also logs). */
    @JvmStatic
    fun warn(moduleName: String, message: String) {
        SbiLog.warn("[$moduleName] $message")
        send(moduleName, message, ChatFormatting.GOLD)
    }

    /** Send an error (also logs). */
    @JvmStatic
    fun error(moduleName: String, message: String) {
        SbiLog.error("[$moduleName] $message")
        send(moduleName, message, ChatFormatting.RED)
    }

    /** Send a debug message - only when debug mode is on. */
    @JvmStatic
    fun debug(moduleName: String, message: String) {
        SbiLog.debug("[$moduleName] $message")
        if (SbiLog.debugEnabled) {
            send(moduleName, message, ChatFormatting.GRAY)
        }
    }

    /** Toggle debug mode (for dev console / future command). */
    @JvmStatic
    fun setDebug(enabled: Boolean) {
        SbiLog.setDebug(enabled)
    }
}
