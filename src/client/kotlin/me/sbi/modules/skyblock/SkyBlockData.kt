package me.sbi.modules.skyblock

import me.sbi.SkyblockImproved
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

/**
 * Singleton providing parsed Hypixel SkyBlock data from tab list and scoreboard.
 * Updated every tick when on SkyBlock. Other modules read from here.
 *
 * Tab list requires "Player List Info": SkyBlock Menu → Settings → Personal → User Interface.
 * Configurable via /tablist and /widgets. If unavailable, tabListAvailable = false.
 */
object SkyBlockData {

    /** Main area name (e.g. "Village", "Spider's Den") */
    var area: String = ""
        private set

    /** Sub-area (e.g. "High Level", "Hub") - often from scoreboard */
    var subArea: String = ""
        private set

    /** True if tab list contains SkyBlock-style data */
    var tabListAvailable: Boolean = false
        private set

    /** True if scoreboard has SkyBlock-style sidebar */
    var scoreboardAvailable: Boolean = false
        private set

    /** Raw scoreboard sidebar lines (for modules that need more parsing) */
    var scoreboardLines: List<String> = emptyList()
        private set

    /** Raw tab list lines from player entries */
    var tabListLines: List<String> = emptyList()
        private set

    /** Captured from tab list packet (header/footer) - most reliable source for "Area: X" */
    private var lastTabHeader: Component? = null
    private var lastTabFooter: Component? = null

    /** Called by mixin when tab list packet is received */
    @JvmStatic
    fun onTabListPacket(header: Component?, footer: Component?) {
        lastTabHeader = header
        lastTabFooter = footer
    }

    private const val HYPIXEL = "hypixel.net"
    /** Tab list: "Area: Deep Caverns", "Area: Hub" - capture until newline or end */
    private val TAB_AREA_PATTERN = Regex("Area:\\s*([^\n]+)")
    /** Scoreboard: ⏣ Village, ⏣ High Level (sub-area/location) */
    private val SCOREBOARD_LOCATION_PATTERN = Regex("⏣\\s*(.+)")
    private val CLEAN = Regex("§[0-9a-fk-or]")

    fun update() {
        val mc = SkyblockImproved.mc
        val player = mc.player ?: return
        val connection = player.connection ?: return

        area = ""
        subArea = ""
        tabListAvailable = false
        scoreboardAvailable = false
        scoreboardLines = emptyList()
        tabListLines = emptyList()

        if (!isOnHypixel(connection)) return

        val level = mc.level ?: return
        parseTabList(connection)
        parseScoreboard(level.scoreboard)

        // Main area: from tab list ("Area: Hub", "Area: Spider's Den")
        for (line in tabListLines) {
            val m = TAB_AREA_PATTERN.find(stripColor(line))
            if (m != null) {
                area = m.groupValues[1].trim()
                break
            }
        }

        // Sub-area: from scoreboard (⏣ Village, ⏣ High Level, etc.)
        for (line in scoreboardLines) {
            val m = SCOREBOARD_LOCATION_PATTERN.find(stripColor(line))
            if (m != null) {
                subArea = m.groupValues[1].trim()
                break
            }
        }
    }

    private fun isOnHypixel(connection: net.minecraft.client.multiplayer.ClientPacketListener): Boolean {
        val addr = connection.connection?.remoteAddress?.toString() ?: return false
        return addr.contains(HYPIXEL, ignoreCase = true)
    }

    private fun parseTabList(connection: net.minecraft.client.multiplayer.ClientPacketListener) {
        val lines = mutableListOf<String>()

        // 1. Tab list header/footer - prefer mixin-captured (from packet), fallback to reflection
        val header = lastTabHeader ?: getTabListHeader(SkyblockImproved.mc.gui.tabList)
        val footer = lastTabFooter ?: getTabListFooter(SkyblockImproved.mc.gui.tabList)
        for (component in listOf(header, footer)) {
            if (component != null) {
                val s = stripColor(component.string)
                if (s.isNotEmpty()) lines.add(s)
            }
        }

        // 2. Tab list scoreboard (DisplaySlot.LIST) - Hypixel Info column can come from here
        val scoreboard = SkyblockImproved.mc.level?.scoreboard
        val listObj = scoreboard?.getDisplayObjective(DisplaySlot.LIST)
        if (listObj != null && scoreboard != null) {
            for (entry in scoreboard.listPlayerScores(listObj)) {
                if (entry.isHidden) continue
                val team = scoreboard.getPlayersTeam(entry.owner())
                val text = if (team != null) {
                    PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).string
                } else entry.owner()
                val raw = stripColor(text)
                if (raw.isNotEmpty()) lines.add(raw)
            }
        }

        // 3. Player entries - Hypixel uses fake players for "Area: Deep Caverns" etc.
        val seen = mutableSetOf<String>()
        for (info in connection.listedOnlinePlayers) {
            val name = info.tabListDisplayName ?: info.profile?.name?.let { Component.literal(it) }
            if (name != null) {
                val raw = stripColor(name.string)
                if (raw.isNotEmpty() && seen.add(raw)) lines.add(raw)
            }
        }
        for (info in connection.onlinePlayers) {
            val name = info.tabListDisplayName ?: info.profile?.name?.let { Component.literal(it) }
            if (name != null) {
                val raw = stripColor(name.string)
                if (raw.isNotEmpty() && seen.add(raw)) lines.add(raw)
            }
        }

        tabListLines = lines
        tabListAvailable = lines.isNotEmpty() && hasSkyBlockContent(lines)
    }

    private fun getTabListHeader(overlay: PlayerTabOverlay): Component? =
        getReflectedField(overlay, "header", "field_2153", "f_94522")

    private fun getTabListFooter(overlay: PlayerTabOverlay): Component? =
        getReflectedField(overlay, "footer", "field_2154", "f_94521")

    private fun getReflectedField(overlay: PlayerTabOverlay, vararg names: String): Component? {
        for (name in names) {
            try {
                val f = PlayerTabOverlay::class.java.getDeclaredField(name)
                f.trySetAccessible()
                return f.get(overlay) as? Component
            } catch (_: Exception) { }
        }
        return null
    }

    private fun parseScoreboard(scoreboard: Scoreboard) {
        val obj = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return
        val entries = scoreboard.listPlayerScores(obj)
        val lines = mutableListOf<String>()
        for (entry in entries) {
            if (entry.isHidden) continue
            val team = scoreboard.getPlayersTeam(entry.owner())
            val text = if (team != null) {
                PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).string
            } else entry.owner()
            lines.add(text)
        }
        scoreboardLines = lines.reversed()
        if (scoreboardLines.isNotEmpty()) scoreboardAvailable = hasSkyBlockContent(scoreboardLines)
    }

    private fun stripColor(input: String): String = CLEAN.replace(input, "")

    private fun hasSkyBlockContent(lines: List<String>): Boolean {
        val joined = lines.joinToString(" ")
        return joined.contains("⏣") ||
            joined.contains("SkyBlock", ignoreCase = true) ||
            joined.contains("Skills", ignoreCase = true) ||
            joined.contains("Purse", ignoreCase = true)
    }
}
