package me.sbi.gui

import me.sbi.modules.render.ClickGuiModule
import net.minecraft.client.gui.GuiGraphics

/**
 * GUI style: Minimal (clean, flat) or RPG (stylized for fantasy/RPG games).
 */
enum class GuiStyle {
    MINIMAL,
    RPG;

    companion object {
        val current: GuiStyle
            get() = when (ClickGuiModule.guiStyle.value) {
                "RPG" -> RPG
                else -> MINIMAL
            }

        /** Accent color from theme (Blue / Cyan) */
        val accent: Int
            get() = when (ClickGuiModule.theme.value) {
                "Cyan" -> 0xFF5AC8FA.toInt()
                else -> 0xFF3296FA.toInt()
            }

        /** Slightly darker accent for selected/dropdown items */
        val accentDark: Int
            get() = when (ClickGuiModule.theme.value) {
                "Cyan" -> 0xFF2A6A8A.toInt()
                else -> 0xFF2A4A6A.toInt()
            }

        /** Module row background when enabled */
        val moduleRowEnabled: Int
            get() = when (ClickGuiModule.theme.value) {
                "Cyan" -> 0xFF1E3A4A.toInt()
                else -> 0xFF1E3A5F.toInt()
            }

        // RPG palette: parchment, dark wood (RPG uses theme accent for header bar)
        private const val RPG_HEADER_TOP = 0xFF2A1810
        private const val RPG_HEADER_BOT = 0xFF4A2818
        private val RPG_ACCENT: Int get() = accent
        private const val RPG_BG = 0xFF1A1410
        private const val RPG_ROW_ENABLED = 0xFF2A2018
        private const val RPG_ROW_DISABLED = 0xFF181410
        private const val RPG_BORDER = 0xFF5A4020
    }

    fun panelHeaderColor(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        when (this) {
    MINIMAL -> {
                g.fill(x, y, x + w, y + h, 0xFF1A1A1A.toInt())
                g.fill(x, y + h - 2, x + w, y + h, accent)
            }
    RPG -> {
                g.fillGradient(x, y, x + w, y + h, RPG_HEADER_TOP.toInt(), RPG_HEADER_BOT.toInt())
                g.fill(x, y + h - 2, x + w, y + h, RPG_ACCENT)
                g.fill(x, y, x + 2, y + h, RPG_BORDER.toInt())
                g.fill(x + w - 2, y, x + w, y + h, RPG_BORDER.toInt())
            }
        }
    }

    fun moduleRowColor(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, enabled: Boolean) {
        when (this) {
            MINIMAL -> {
                val c = if (enabled) moduleRowEnabled else 0xFF1C1C1C.toInt()
                g.fill(x, y, x + w, y + h, c)
                g.fill(x, y + h - 1, x + w, y + h, 0xFF252525.toInt())
            }
            RPG -> {
                val c = if (enabled) RPG_ROW_ENABLED else RPG_ROW_DISABLED
                g.fill(x, y, x + w, y + h, c.toInt())
                if (enabled) g.fill(x, y, x + 3, y + h, RPG_ACCENT)
                g.fill(x, y + h - 1, x + w, y + h, 0xFF252520.toInt())
            }
        }
    }

    fun overlayColor(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        val alpha = (ClickGuiModule.overlayOpacity.value * 255).toInt().coerceIn(0, 255)
        when (this) {
            MINIMAL -> g.fill(x, y, x + w, y + h, (alpha shl 24) or 0x000000)
            RPG -> g.fill(x, y, x + w, y + h, (alpha shl 24) or 0x0A0806)
        }
    }

    fun partyBoxColors(): Pair<Int, Int> = when (this) {
        MINIMAL -> 0x88000000.toInt() to 0xAA2A5080.toInt()
        RPG -> 0xCC1A1410.toInt() to 0xDDC49420.toInt()
    }
}
