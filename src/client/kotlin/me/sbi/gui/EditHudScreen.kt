package me.sbi.gui

import me.sbi.core.ModuleManager
import me.sbi.modules.general.CoordsDisplayModule
import me.sbi.modules.general.NameChangerModule
import me.sbi.modules.render.PartyDisplayModule
import me.sbi.modules.skyblock.AreaDisplayModule
import me.sbi.modules.skyblock.SkyBlockData
import me.sbi.modules.skyblock.SkyBlockParty
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Overlay to drag HUD elements. Opened via "Edit GUI" in ClickGui.
 */
class EditHudScreen : Screen(Component.literal("Edit HUD")) {

    private var dragging: HudLayout.Element? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Skip default - we draw our own overlay to avoid NPE when level is loading etc.
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val mc = Minecraft.getInstance()
        val w = graphics.guiWidth()
        val h = graphics.guiHeight()

        GuiStyle.current.overlayColor(graphics, 0, 0, w, h)
        val hintColor = if (GuiStyle.current == GuiStyle.RPG) 0xFFC49420.toInt() else 0xFFFFFFFF.toInt()
        graphics.drawCenteredString(font, "Drag elements to move. ESC to save and close.", w / 2, 8, hintColor)

        val player = mc.player
        val level = mc.level

        for (element in HudLayout.Element.entries) {
            val (content, visible) = getContent(element, player != null, level != null)
            var x = HudLayout.x(element)
            var y = HudLayout.y(element)

            if (element == dragging) {
                x = (mouseX - dragOffsetX).coerceIn(0, w - 50)
                y = (mouseY - dragOffsetY).coerceIn(0, h - 20)
                HudLayout.set(element, x, y)
            }

            val boxW = (font.width(content).coerceAtLeast(40) + 8)
            val boxH = 20

            val (borderColor, innerColor) = when (GuiStyle.current) {
                GuiStyle.MINIMAL -> if (visible) 0x8800FF00.toInt() to 0xCC1A1A1A.toInt() else 0x88FF6600.toInt() to 0xCC1A1A1A.toInt()
                GuiStyle.RPG -> if (visible) 0xDDC49420.toInt() to 0xCC1A1410.toInt() else 0xAA5A4020.toInt() to 0xCC1A1410.toInt()
            }
            graphics.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, borderColor)
            graphics.fill(x, y, x + boxW, y + boxH, innerColor)
            graphics.drawString(font, content, x + 4, y + 6, 0xFFFFFFFF.toInt(), true)
        }

        super.render(graphics, mouseX, mouseY, delta)
    }

    private fun getContent(element: HudLayout.Element, hasPlayer: Boolean, hasLevel: Boolean): Pair<String, Boolean> {
        return when (element) {
            HudLayout.Element.COORDS -> {
                val visible = CoordsDisplayModule.enabled
                val content = if (hasPlayer && hasLevel) {
                    val pos = Minecraft.getInstance().player!!.blockPosition()
                    "XYZ: ${pos.x}, ${pos.y}, ${pos.z}"
                } else "Coords"
                content to visible
            }
            HudLayout.Element.NICK -> {
                val visible = NameChangerModule.enabled && NameChangerModule.customName.value.isNotEmpty()
                "Nick: ${NameChangerModule.customName.value.ifEmpty { "..." }}" to visible
            }
            HudLayout.Element.AREA -> {
                val visible = AreaDisplayModule.enabled
                val area = SkyBlockData.area
                val sub = SkyBlockData.subArea
                val text = when {
                    area.isNotEmpty() -> "Area: $area"
                    sub.isNotEmpty() -> sub
                    else -> "Area"
                }
                text to visible
            }
            HudLayout.Element.PARTY -> {
                val visible = PartyDisplayModule.enabled && SkyBlockParty.hasParty
                val text = if (SkyBlockParty.hasParty) {
                    val l = SkyBlockParty.partyLeader
                    val m = SkyBlockParty.partyMembers
                    if (l.name.isNotEmpty()) "♔ ${l.displayText}" + if (m.isNotEmpty()) " +${m.size}" else ""
                    else "Party: ${SkyBlockParty.allMembers.size} members"
                } else "Party (run /p l)"
                text to visible
            }
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, consume: Boolean): Boolean {
        if (mouseButtonEvent.button() != 0) return super.mouseClicked(mouseButtonEvent, consume)
        val mx = mouseButtonEvent.x().toInt()
        val my = mouseButtonEvent.y().toInt()

        for (element in HudLayout.Element.entries.reversed()) {
            val x = HudLayout.x(element)
            val y = HudLayout.y(element)
            val (content, _) = getContent(element, true, true)
            val boxW = (font.width(content).coerceAtLeast(40) + 8)
            val boxH = 20
            if (mx >= x && mx <= x + boxW && my >= y && my <= y + boxH) {
                dragging = element
                dragOffsetX = mx - x
                dragOffsetY = my - y
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, consume)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        if (mouseButtonEvent.button() == 0) dragging = null
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
        if (keyEvent.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            HudLayout.save()
            onClose()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun onClose() {
        HudLayout.save()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}
