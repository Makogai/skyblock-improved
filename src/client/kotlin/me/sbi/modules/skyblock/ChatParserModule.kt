package me.sbi.modules.skyblock

import me.sbi.SkyblockImproved
import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.core.ModuleManager
import me.sbi.modules.skyblock.PartyGuiModule
import me.sbi.modules.skyblock.PartyState
import me.sbi.util.SbiLog
import me.sbi.util.SbiMessage
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Internal module that parses chat messages for SkyBlock data (party, etc.).
 * Other modules read from SkyBlockParty, SkyBlockData, etc.
 */
object ChatParserModule : Module(
    name = "Chat Parser",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.SKYBLOCK,
    description = "Internal: parses chat for party and other SkyBlock info",
    toggled = true,
    internal = true
) {
    private var ticksUntilPartyList = 0

    override fun onTick() {
        if (ticksUntilPartyList > 0) {
            ticksUntilPartyList--
            if (ticksUntilPartyList == 0) {
                val needsPartyData = needsPartyData()
                if (needsPartyData) {
                    val player = SkyblockImproved.mc.player ?: return
                    player.connection?.sendCommand("party list")
                }
            }
        }
    }

    override fun onKeybind() {}

    private fun needsPartyData(): Boolean =
        ModuleManager.modules["party display"]?.enabled == true

    fun init() {
        ClientReceiveMessageEvents.GAME.register(::onGameMessage)
        ClientReceiveMessageEvents.CHAT.register { msg, _, _, _, _ -> processMessage(getFullString(msg)) }
        ClientReceiveMessageEvents.MODIFY_GAME.register { msg, overlay ->
            if (!overlay) processMessage(getFullString(msg))
            msg
        }
        ClientPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val addr = handler.connection?.remoteAddress?.toString() ?: return@register
            if (addr.contains("hypixel.net", ignoreCase = true)) {
                val needsPartyData = needsPartyData()
                if (needsPartyData) ticksUntilPartyList = 60 // 3 seconds after join
            }
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> InvisibugScan.clear() }
    }

    private fun onGameMessage(message: Component, overlay: Boolean) {
        if (overlay) return
        processMessage(getFullString(message))
    }

    private fun getFullString(component: Component): String {
        return try {
            component.string
        } catch (e: Exception) {
            component.toString()
        }
    }

    private fun processMessage(text: String) {
        val clean = Regex("§[0-9a-fk-or]").replace(text, "")
        if (text.contains("Party", ignoreCase = true) && SbiLog.debugEnabled) {
            SbiLog.debug("ChatParser received: ${text.take(200)}${if (text.length > 200) "..." else ""}")
        }
        processInviteMessages(text)
        if (text.contains("Party Members (") &&
            (text.contains("Party Leader:") || text.contains("Party Members:") || text.contains("Party Moderators:"))) {
            SkyBlockParty.parseFromChat(text)
            if (SbiLog.debugEnabled) {
                SbiMessage.debug("Party", "Parsed: leader=${SkyBlockParty.partyLeader}, members=${SkyBlockParty.partyMembers}")
            }
        } else if (text.contains("left the party", ignoreCase = true) ||
            text.contains("party was disbanded", ignoreCase = true) ||
            text.contains("kicked from the party", ignoreCase = true)) {
            SkyBlockParty.clear()
            PartyState.clearPending()
        }
    }

    private fun processInviteMessages(text: String) {
        val clean = Regex("§[0-9a-fk-or]").replace(text, "")
        when {
            clean.contains("You have invited") && clean.contains("to your party") -> {
                val name = Regex("You have invited (?:\\[[^\\]]+\\]\\s*)?([^\\s!]+) to your party").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let { PartyState.addPendingInvite(it) }
                PartyState.lastInvitedName = null
            }
            (clean.contains("cannot invite") || clean.contains("can't invite")) && clean.contains("not online") -> {
                PartyState.lastInvitedName?.let { PartyState.removePendingInvite(it) }
                PartyState.lastInvitedName = null
            }
            clean.contains("has accepted your party invite") -> {
                val name = Regex("(?:\\[[^\\]]+\\]\\s*)?([^\\s!]+) has accepted your party invite").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let { PartyState.removePendingInvite(it) }
            }
            (clean.contains("party invite") || clean.contains("invite to")) && clean.contains("expired") -> {
                val name = Regex("(?:invite to|invite) (?:\\[[^\\]]+\\]\\s*)?([^\\s.,!]+)").find(clean)?.groupValues?.getOrNull(1)?.trim()
                    ?: Regex("(?:\\[[^\\]]+\\]\\s*)?([^\\s.,!]+)\\s+has expired").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let { PartyState.removePendingInvite(it) }
            }
            clean.contains("has declined your party invite") -> {
                val name = Regex("(?:\\[[^\\]]+\\]\\s*)?([^\\s!]+) has declined your party invite").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let { PartyState.removePendingInvite(it) }
            }
            clean.contains("has invited you to join their party") -> {
                val name = Regex("(?:\\[[^\\]]+\\]\\s*)?([^\\s!]+) has invited you to join their party").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let {
                    PartyState.addIncomingInvite(it)
                    if (PartyGuiModule.acceptAllInvites.value) {
                        SkyblockImproved.mc.player?.connection?.sendCommand("party accept $it")
                    }
                }
            }
            clean.contains("You joined") && clean.contains("party") -> {
                val name = Regex("You joined (?:\\[[^\\]]+\\]\\s*)?([^'\\s]+)'s party").find(clean)?.groupValues?.getOrNull(1)?.trim()
                name?.let { PartyState.removeIncomingInvite(it) }
            }
            // X joined the party - they accepted our invite, remove from pending
            clean.contains("joined the party") && !clean.contains("You joined") -> {
                val whoPart = Regex("(.+?)\\s+joined the party").find(clean)?.groupValues?.getOrNull(1)?.trim()
                whoPart?.let {
                    val name = Regex("\\[[^\\]]+\\]").replace(it, "").trim().ifEmpty { it }
                    if (name.isNotEmpty()) PartyState.removePendingInvite(name)
                }
            }
        }
    }
}
