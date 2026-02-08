package me.sbi.gui

import me.sbi.SkyblockImproved
import me.sbi.gui.GuiStyle
import me.sbi.modules.skyblock.PartyGuiModule
import me.sbi.modules.skyblock.PartyState
import me.sbi.modules.skyblock.SkyBlockParty
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Full party management GUI - RPG style.
 * All action buttons drawn explicitly for visibility.
 */
class PartyGuiScreen : Screen(Component.literal("Party")) {

    private var inviteBox: EditBox? = null

    companion object {
        const val PANEL_W = 380
        const val PANEL_H = 420
        const val SLOT_H = 52
        const val MARGIN = 12
        const val BTN_W = 58
        const val BTN_H = 22
        const val BTN_GAP = 4
        const val ACCENT = 0xFFC49420.toInt()
        const val BG_DARK = 0xFF1A1410.toInt()
        const val BG_SLOT = 0xFF2A2018.toInt()
        const val BORDER = 0xFF5A4020.toInt()
        const val TEXT = 0xFFE8E0C0.toInt()
        const val TEXT_DIM = 0xFFA09070.toInt()
        val BTN_BG = 0xFF3A3525.toInt()
        val BTN_HOVER = 0xFF4A4535.toInt()
        val KICK_BG = 0xFF4A2520.toInt()
        val KICK_HOVER = 0xFF5A3025.toInt()
        val TRANSFER_BG = 0xFF3A4020.toInt()
        val TRANSFER_HOVER = 0xFF4A5030.toInt()
    }

    override fun init() {
        val cx = width / 2
        val left = cx - PANEL_W / 2
        val top = (height - PANEL_H) / 2

        inviteBox = EditBox(font, left + MARGIN + 2, top + PANEL_H - 48, 200, 20, Component.literal("Player name"))
            .apply {
                setHint(Component.literal("Enter name to invite"))
                setMaxLength(16)
            }
        addRenderableWidget(inviteBox!!)

        addRenderableWidget(Button.builder(Component.literal("Invite")) {
            val name = inviteBox?.value?.trim()
            if (!name.isNullOrEmpty()) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party invite $name")
                PartyState.addPendingInvite(name)
                PartyState.lastInvitedName = name
                inviteBox?.value = ""
            }
        }.bounds(left + 220, top + PANEL_H - 52, 70, 24).build())
    }

    fun isLeader(): Boolean {
        val myName = SkyblockImproved.mc.player?.name?.string ?: return false
        return myName.equals(SkyBlockParty.partyLeader.name, ignoreCase = true)
    }

    /** Members for display - never duplicates by name */
    private fun displayMembers(): List<SkyBlockParty.PartyMember> =
        SkyBlockParty.displayMembers.take(5)

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        GuiStyle.current.overlayColor(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight())
        val cx = width / 2
        val left = cx - PANEL_W / 2
        val top = (height - PANEL_H) / 2

        graphics.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, BORDER)
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, BG_DARK)
        graphics.fill(left, top, left + PANEL_W, top + 40, 0xFF2A1810.toInt())
        graphics.fill(left, top + 38, left + PANEL_W, top + 42, ACCENT)
        graphics.drawString(font, "§6§lParty", left + MARGIN, top + 14, 0xFFFFFFFF.toInt())

        val members = displayMembers()
        val isLeader = isLeader()
        val slotW = PANEL_W - MARGIN * 2

        var y = top + 50

        for ((i, member) in members.withIndex()) {
            val isLead = i == 0 && SkyBlockParty.partyLeader.name.isNotEmpty()
            graphics.fill(left + MARGIN, y, left + PANEL_W - MARGIN, y + SLOT_H - 4, BG_SLOT)
            if (isLead) graphics.fill(left + MARGIN, y, left + MARGIN + 4, y + SLOT_H - 4, ACCENT)
            graphics.fill(left + MARGIN, y + SLOT_H - 5, left + PANEL_W - MARGIN, y + SLOT_H - 4, 0xFF252520.toInt())
            val crown = if (isLead) "§6♔ " else "   "
            graphics.drawString(font, "$crown§f${member.displayText}", left + MARGIN + 10, y + 10, TEXT)

            val btnY = y + 22
            var bx = left + MARGIN + slotW - MARGIN

            val detailsHover = mouseX in bx - BTN_W..bx && mouseY in btnY..btnY + BTN_H
            drawButton(graphics, bx - BTN_W, btnY, BTN_W, BTN_H, "Details", detailsHover, false)
            bx -= BTN_W + BTN_GAP

            if (isLeader && !isLead) {
                val transferHover = mouseX in bx - BTN_W..bx && mouseY in btnY..btnY + BTN_H
                drawButton(graphics, bx - BTN_W, btnY, BTN_W, BTN_H, "§6Transfer", transferHover, true)
                bx -= BTN_W + BTN_GAP
                val kickHover = mouseX in bx - BTN_W..bx && mouseY in btnY..btnY + BTN_H
                drawButton(graphics, bx - BTN_W, btnY, BTN_W, BTN_H, "§cKick", kickHover, false)
            }
            y += SLOT_H
        }

        if (members.isEmpty()) {
            graphics.drawString(font, "No party. Invite or run §e/p list", left + MARGIN, y + 8, TEXT_DIM)
            y += SLOT_H
        }

        if (PartyState.pendingOutgoing.isNotEmpty()) {
            y += 8
            graphics.drawString(font, "§7Pending:", left + MARGIN, y, TEXT_DIM)
            y += 14
            for (name in PartyState.pendingOutgoing.take(5)) {
                graphics.drawString(font, "  §e⏳ $name", left + MARGIN, y, TEXT_DIM)
                y += 12
            }
        }

        if (PartyState.incomingInvites.isNotEmpty()) {
            y += 8
            graphics.drawString(font, "§7Incoming:", left + MARGIN, y, TEXT_DIM)
            y += 14
            for ((name, _) in PartyState.incomingInvites.take(3)) {
                val btnX = left + PANEL_W - MARGIN - 140
                val inAccept = mouseX in btnX..btnX + 60 && mouseY in y - 2..y + 14
                val inDecline = mouseX in btnX + 65..btnX + 125 && mouseY in y - 2..y + 14
                graphics.fill(btnX, y - 2, btnX + 60, y + 14, if (inAccept) BTN_HOVER else BTN_BG)
                graphics.drawString(font, "§aAccept", btnX + 6, y + 2, 0xFF90FF90.toInt())
                graphics.fill(btnX + 65, y - 2, btnX + 125, y + 14, if (inDecline) KICK_HOVER else KICK_BG)
                graphics.drawString(font, "§cDecline", btnX + 72, y + 2, 0xFFFF9090.toInt())
                graphics.drawString(font, "  §f$name", left + MARGIN, y, TEXT)
                y += 16
            }
        }

        var rx = left + PANEL_W - MARGIN
        val headerY = top + 10
        val headerH = 22
        val refreshHover = mouseX in rx - 65..rx && mouseY in headerY..headerY + headerH
        drawButton(graphics, rx - 65, headerY, 65, headerH, "Refresh", refreshHover, false)
        rx -= 69
        val allHover = mouseX in rx - 70..rx && mouseY in headerY..headerY + headerH
        drawButton(graphics, rx - 70, headerY, 70, headerH, "Accept All", allHover, false)
        rx -= 74
        if (SkyBlockParty.hasParty) {
            val leaveHover = mouseX in rx - 55..rx && mouseY in headerY..headerY + headerH
            drawButton(graphics, rx - 55, headerY, 55, headerH, "Leave", leaveHover, false)
            rx -= 59
            if (isLeader) {
                val disbandHover = mouseX in rx - 65..rx && mouseY in headerY..headerY + headerH
                drawButton(graphics, rx - 65, headerY, 65, headerH, "§cDisband", disbandHover, false)
            }
        }

        super.render(graphics, mouseX, mouseY, delta)
    }

    private fun drawButton(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, label: String, hover: Boolean, isTransfer: Boolean) {
        val bg = when {
            isTransfer -> if (hover) TRANSFER_HOVER else TRANSFER_BG
            label.contains("Kick") || label.contains("Disband") || label.contains("Decline") -> if (hover) KICK_HOVER else KICK_BG
            else -> if (hover) BTN_HOVER else BTN_BG
        }
        g.fill(x, y, x + w, y + h, bg)
        g.fill(x, y, x + w, y + 1, 0xFF505050.toInt())
        g.fill(x, y + h - 1, x + w, y + h, 0xFF202020.toInt())
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF.toInt())
    }

    override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
        if (keyEvent.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun onClose() {
        me.sbi.core.ModuleManager.saveConfigurations()
        super.onClose()
    }

    private fun getScaledMouse(event: net.minecraft.client.input.MouseButtonEvent): Pair<Int, Int> {
        val mc = minecraft ?: return event.x().toInt() to event.y().toInt()
        val winW = mc.window.width.toDouble()
        val winH = mc.window.height.toDouble()
        if (winW <= 0 || winH <= 0) return event.x().toInt() to event.y().toInt()
        return (event.x() * width / winW).toInt() to (event.y() * height / winH).toInt()
    }

    override fun mouseClicked(mouseButtonEvent: net.minecraft.client.input.MouseButtonEvent, consume: Boolean): Boolean {
        if (mouseButtonEvent.button() != 0) return super.mouseClicked(mouseButtonEvent, consume)
        val (mx, my) = getScaledMouse(mouseButtonEvent)

        val left = width / 2 - PANEL_W / 2
        val top = (height - PANEL_H) / 2
        val members = displayMembers()
        val isLeader = isLeader()
        val slotW = PANEL_W - MARGIN * 2

        var y = top + 50
        for ((i, member) in members.withIndex()) {
            val isLead = i == 0 && SkyBlockParty.partyLeader.name.isNotEmpty()
            val btnY = y + 22
            var bx = left + MARGIN + slotW - MARGIN

            if (mx in bx - BTN_W..bx && my in btnY..btnY + BTN_H) {
                minecraft?.setScreen(PartyDetailsScreen(member, this))
                return true
            }
            bx -= BTN_W + BTN_GAP
            if (isLeader && !isLead) {
                if (mx in bx - BTN_W..bx && my in btnY..btnY + BTN_H) {
                    SkyblockImproved.mc.player?.connection?.sendCommand("party transfer ${member.name}")
                    return true
                }
                bx -= BTN_W + BTN_GAP
                if (mx in bx - BTN_W..bx && my in btnY..btnY + BTN_H) {
                    SkyblockImproved.mc.player?.connection?.sendCommand("party kick ${member.name}")
                    return true
                }
            }
            y += SLOT_H
        }

        val headerY = top + 10
        val headerH = 22
        var rx = left + PANEL_W - MARGIN
        if (mx in rx - 65..rx && my in headerY..headerY + headerH) {
            SkyblockImproved.mc.player?.connection?.sendCommand("party list")
            return true
        }
        rx -= 69
        if (mx in rx - 70..rx && my in headerY..headerY + headerH) {
            PartyGuiModule.acceptAllInvites.value = !PartyGuiModule.acceptAllInvites.value
            return true
        }
        rx -= 74
        if (SkyBlockParty.hasParty) {
            if (mx in rx - 55..rx && my in headerY..headerY + headerH) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party leave")
                minecraft?.setScreen(null)
                return true
            }
            rx -= 59
            if (isLeader && mx in rx - 65..rx && my in headerY..headerY + headerH) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party disband")
                minecraft?.setScreen(null)
                return true
            }
        }

        var iy = top + 50 + members.size * SLOT_H + 8
        if (PartyState.pendingOutgoing.isNotEmpty()) iy += 14 + PartyState.pendingOutgoing.take(5).size * 12 + 8
        iy += 14
        for ((name, _) in PartyState.incomingInvites.take(3)) {
            val btnX = left + PANEL_W - MARGIN - 140
            if (mx in btnX..btnX + 60 && my in iy - 2..iy + 14) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party accept $name")
                PartyState.removeIncomingInvite(name)
                SkyblockImproved.mc.player?.connection?.sendCommand("party list")
                return true
            }
            if (mx in btnX + 65..btnX + 125 && my in iy - 2..iy + 14) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party deny $name")
                PartyState.removeIncomingInvite(name)
                return true
            }
            iy += 16
        }

        return super.mouseClicked(mouseButtonEvent, consume)
    }

    override fun isPauseScreen(): Boolean = false
}

private class PartyDetailsScreen(val member: SkyBlockParty.PartyMember, val parent: PartyGuiScreen) : Screen(Component.literal("Details")) {
    private fun getScaledMouse(event: net.minecraft.client.input.MouseButtonEvent): Pair<Int, Int> {
        val mc = minecraft ?: return event.x().toInt() to event.y().toInt()
        val w = mc.window
        val winW = w.width.toDouble()
        val winH = w.height.toDouble()
        if (winW <= 0 || winH <= 0) return event.x().toInt() to event.y().toInt()
        return (event.x() * width / winW).toInt() to (event.y() * height / winH).toInt()
    }
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {}
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        GuiStyle.current.overlayColor(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight())
        val popupW = 220
        val popupH = 160
        val px = (width - popupW) / 2
        val py = (height - popupH) / 2
        val isMeLeader = parent.isLeader()
        val isThisLeader = SkyBlockParty.partyLeader.name.equals(member.name, ignoreCase = true)

        graphics.fill(px - 1, py - 1, px + popupW + 1, py + popupH + 1, 0xFF5A4020.toInt())
        graphics.fill(px, py, px + popupW, py + popupH, 0xFF1A1410.toInt())
        graphics.fill(px, py, px + popupW, py + 28, 0xFF2A1810.toInt())
        graphics.drawString(font, "§6${member.displayText}", px + 8, py + 10, 0xFFFFFFFF.toInt())
        graphics.drawString(font, "§7Dungeon: §fCata 42", px + 8, py + 40, 0xFFE8E0C0.toInt())
        graphics.drawString(font, "§7Class: §fMage", px + 8, py + 54, 0xFFE8E0C0.toInt())
        graphics.drawString(font, "§8(TODO: API)", px + 8, py + 88, 0xFFA09070.toInt())

        if (isMeLeader && !isThisLeader) {
            val tx = px + 8
            val ty = py + 105
            val transferHover = mouseX in tx..tx + 65 && mouseY in ty..ty + 22
            val kickHover = mouseX in tx + 70..tx + 130 && mouseY in ty..ty + 22
            graphics.fill(tx, ty, tx + 65, ty + 22, if (transferHover) 0xFF4A5030.toInt() else 0xFF3A4020.toInt())
            graphics.drawCenteredString(font, "§6Transfer", tx + 32, ty + 7, 0xFFC4C420.toInt())
            graphics.fill(tx + 70, ty, tx + 130, ty + 22, if (kickHover) 0xFF5A3025.toInt() else 0xFF4A2520.toInt())
            graphics.drawCenteredString(font, "§cKick", tx + 100, ty + 7, 0xFFFF9090.toInt())
        }
        val closeX = px + popupW - 55
        val closeHover = mouseX in closeX..closeX + 45 && mouseY in py + 6..py + 24
        graphics.fill(closeX, py + 6, closeX + 45, py + 24, if (closeHover) 0xFF4A4535.toInt() else 0xFF3A3525.toInt())
        graphics.drawCenteredString(font, "Close", closeX + 22, py + 10, 0xFFFFFFFF.toInt())

        super.render(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseButtonEvent: net.minecraft.client.input.MouseButtonEvent, consume: Boolean): Boolean {
        if (mouseButtonEvent.button() != 0) return super.mouseClicked(mouseButtonEvent, consume)
        val (mx, my) = getScaledMouse(mouseButtonEvent)
        val popupW = 220
        val popupH = 160
        val px = (width - popupW) / 2
        val py = (height - popupH) / 2
        val isMeLeader = parent.isLeader()
        val isThisLeader = SkyBlockParty.partyLeader.name.equals(member.name, ignoreCase = true)

        if (mx in px + popupW - 55..px + popupW - 10 && my in py + 6..py + 24) {
            minecraft?.setScreen(parent)
            return true
        }
        if (isMeLeader && !isThisLeader) {
            val tx = px + 8
            val ty = py + 105
            if (mx in tx..tx + 65 && my in ty..ty + 22) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party transfer ${member.name}")
                minecraft?.setScreen(parent)
                return true
            }
            if (mx in tx + 70..tx + 130 && my in ty..ty + 22) {
                SkyblockImproved.mc.player?.connection?.sendCommand("party kick ${member.name}")
                minecraft?.setScreen(parent)
                return true
            }
        }
        if (mx !in px..px + popupW || my !in py..py + popupH) {
            minecraft?.setScreen(parent)
        }
        return true
    }

    override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
        if (keyEvent.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            minecraft?.setScreen(parent)
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun isPauseScreen(): Boolean = false
}
