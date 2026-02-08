package me.sbi

import me.sbi.core.ModuleManager
import me.sbi.gui.ClickGui
import me.sbi.gui.GuiStyle
import me.sbi.gui.HudLayout
import me.sbi.modules.general.CoordsDisplayModule
import me.sbi.modules.general.NameChangerModule
import me.sbi.modules.render.Esp3DRenderer
import me.sbi.modules.render.EspModule
import me.sbi.modules.general.TrollModule
import me.sbi.modules.render.PartyDisplayModule
import me.sbi.modules.skyblock.AreaDisplayModule
import me.sbi.modules.skyblock.ChatParserModule
import me.sbi.modules.render.ClickGuiModule
import me.sbi.modules.skyblock.SkyBlockData
import me.sbi.modules.skyblock.SkyBlockParty
import me.sbi.util.SbiLog
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object SkyblockimprovedClient : ClientModInitializer {
    private lateinit var openGuiKey: KeyMapping

    override fun onInitializeClient() {
        // Initialize module system (registers modules, loads config, sets up tick)
        ModuleManager
        TrollModule.ensureExtracted()
        ChatParserModule.init()
        HudLayout.load()
        Esp3DRenderer.init()
        SbiLog.setDebug(ClickGuiModule.debugMode.value)

        HudRenderCallback.EVENT.register { context, _ ->
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return@register
            val level = mc.level ?: return@register
            if (mc.options.hideGui) return@register

            if (CoordsDisplayModule.enabled) {
                val pos = player.blockPosition()
                var text = "XYZ: ${pos.x}, ${pos.y}, ${pos.z}"
                if (CoordsDisplayModule.showDimension.value) {
                    val dimStr = level.dimension().toString()
                    val match = Regex(".*?([^/\\]]+)\\]").find(dimStr)
                    text += " (${match?.groupValues?.get(1) ?: "?"})"
                }
                context.drawString(mc.font, text, HudLayout.x(HudLayout.Element.COORDS), HudLayout.y(HudLayout.Element.COORDS), 0xFFFFFFFF.toInt(), true)
            }
            if (NameChangerModule.enabled && NameChangerModule.customName.value.isNotEmpty()) {
                context.drawString(mc.font, "Nick: ${NameChangerModule.customName.value}", HudLayout.x(HudLayout.Element.NICK), HudLayout.y(HudLayout.Element.NICK), 0xFFAAAAFF.toInt(), true)
            }
            if (AreaDisplayModule.enabled) {
                val area = SkyBlockData.area
                val subArea = SkyBlockData.subArea
                val parts = mutableListOf<String>()
                if (area.isNotEmpty()) parts.add("Area: $area")
                if (AreaDisplayModule.showSubArea.value && subArea.isNotEmpty()) parts.add(subArea)
                if (AreaDisplayModule.showAvailability.value) {
                    if (!SkyBlockData.tabListAvailable) parts.add("[Tab list off]")
                    if (!SkyBlockData.scoreboardAvailable && area.isEmpty()) parts.add("[Scoreboard n/a]")
                }
                val text = parts.joinToString(" | ").ifEmpty { "No SkyBlock data" }
                context.drawString(mc.font, text, HudLayout.x(HudLayout.Element.AREA), HudLayout.y(HudLayout.Element.AREA), 0xFF55FF55.toInt(), true)
            }
            if (PartyDisplayModule.enabled && SkyBlockParty.hasParty) {
                val allMembers = SkyBlockParty.displayMembers
                var py = HudLayout.y(HudLayout.Element.PARTY)
                val px = HudLayout.x(HudLayout.Element.PARTY)
                val useStyle = PartyDisplayModule.showHypixelStyle.value
                val lineCount = allMembers.size
                val boxW = 120
                val boxH = lineCount * 12 + 6
                if (PartyDisplayModule.showBackground.value) {
                    val opacity = (PartyDisplayModule.backgroundOpacity.value / 100.0 * 255).toInt().coerceIn(0, 255)
                    val (baseBg, baseHeader) = GuiStyle.current.partyBoxColors()
                    val bgColor = (opacity shl 24) or (baseBg and 0x00FFFFFF)
                    val headerColor = (opacity shl 24) or (baseHeader and 0x00FFFFFF)
                    context.fill(px - 4, py - 2, px + boxW, py + boxH, bgColor)
                    context.fill(px - 4, py - 2, px + boxW, py, headerColor)
                }
                for ((idx, m) in allMembers.withIndex()) {
                    val isLeader = idx == 0 && SkyBlockParty.partyLeader.name.isNotEmpty()
                    if (useStyle) {
                        val prefix = if (isLeader) "♔ " else "  "
                        val comp = net.minecraft.network.chat.Component.literal(prefix).withStyle(if (isLeader) net.minecraft.ChatFormatting.GOLD else net.minecraft.ChatFormatting.RESET)
                            .append(SkyBlockParty.toComponent(m, isLeader))
                        context.drawString(mc.font, comp, px, py + 2, 0xFFFFFFFF.toInt(), true)
                    } else {
                        val prefix = if (isLeader) "♔ " else "  "
                        var comp = net.minecraft.network.chat.Component.literal(prefix + m.displayText)
                            .withStyle(if (isLeader) net.minecraft.ChatFormatting.GOLD else net.minecraft.ChatFormatting.GRAY)
                        if (isLeader) comp = comp.withStyle(net.minecraft.ChatFormatting.BOLD)
                        context.drawString(mc.font, comp, px, py + 2, 0xFFFFFFFF.toInt(), true)
                    }
                    py += 12
                }
            }
            if (TrollModule.enabled && TrollModule.isShowing()) {
                val path = TrollModule.getCurrentImagePath()
                val w = context.guiWidth()
                val h = context.guiHeight()
                val msg = TrollModule.getDisplayMessage()
                val scale = TrollModule.getImageScale()
                if (path != null) {
                    val tex = TrollModule.getOrLoadTexture(path)
                    val (tw, th) = TrollModule.getTextureDimensions()
                    if (tex != null && tw > 0 && th > 0) {
                        val maxDim = (256 * scale).toInt().coerceIn(64, 512)
                        val (imgW, imgH) = if (tw >= th) {
                            maxDim to (maxDim * th / tw).coerceAtLeast(1)
                        } else {
                            (maxDim * tw / th).coerceAtLeast(1) to maxDim
                        }
                        val x = (w - imgW) / 2
                        val y = (h - imgH) / 2 - 30
                        // Use (x1,y1,x2,y2,u1,u2,v1,v2) - NOT (x,y,0,0,...) which draws wrong rect to 0,0
                        context.blit(tex, x, y, x + imgW, y + imgH, 0f, 1f, 0f, 1f)
                    }
                }
                val textY = h / 2 + 20
                val comp = net.minecraft.network.chat.Component.literal(msg).withStyle(net.minecraft.ChatFormatting.BOLD)
                for (dx in -1..1) for (dy in -1..1) if (dx != 0 || dy != 0) {
                    context.drawCenteredString(mc.font, comp, w / 2 + dx, textY + dy, 0x80000000.toInt())
                }
                context.drawCenteredString(mc.font, comp, w / 2, textY, 0xFFFFFF00.toInt())
            }
        }

        openGuiKey = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.skyblock-improved.open_gui",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
        ))

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (openGuiKey.consumeClick() && client.screen == null) {
                client.setScreen(ClickGui)
            }
        }
    }
}