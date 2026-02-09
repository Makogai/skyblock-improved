package me.sbi.modules.skyblock

import com.google.gson.JsonObject
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ConnectionEvent
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Message
import me.sbi.SkyblockImproved
import me.sbi.util.AdminMessage
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Ably-based implementation. Loaded lazily via reflection from ModSyncModule
 * to avoid Ably classes at startup (prevents Minecraft crash reporter NPE).
 */
class ModSyncAblyHandler : ModSyncHandler {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "mod-sync-ably").apply { isDaemon = true }
    }
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val ablyRef = AtomicReference<AblyRealtime?>(null)
    private val playersChannelRef = AtomicReference<Channel?>(null)

    override fun connect(baseUrl: String, clientId: String) {
        executor.execute {
            try {
                val token = fetchToken(baseUrl, clientId)
                if (token == null) {
                    SkyblockImproved.mc.execute {
                        me.sbi.util.SbiMessage.warn("Mod Sync", "Cannot reach backend at $baseUrl - check API URL and that backend is running")
                    }
                    return@execute
                }
                val options = ClientOptions().apply {
                    this.token = token
                    this.clientId = clientId
                    autoConnect = true
                }
                val realtime = AblyRealtime(options)
                ablyRef.set(realtime)

                realtime.connection.on(ConnectionEvent.connected) {
                    val adminCh = realtime.channels.get("skyblock:admin-messages")
                    adminCh.attach()
                    adminCh.subscribe { msg: Message ->
                        if (msg.name != "admin-message") return@subscribe
                        val data = msg.data
                        val text = when (data) {
                            is Map<*, *> -> data["message"] as? String
                            is com.google.gson.JsonObject -> data.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                            is String -> data
                            else -> try {
                                val json = com.google.gson.Gson().toJson(data)
                                com.google.gson.JsonParser.parseString(json).asJsonObject.get("message")?.asString
                            } catch (_: Exception) { null }
                        }
                        text?.let { SkyblockImproved.mc.execute { AdminMessage.sendAdminMessage(it) } }
                    }

                    val commandsCh = realtime.channels.get("skyblock:player-commands")
                    commandsCh.attach()
                    commandsCh.subscribe { msg: Message ->
                        if (msg.name != "player-command") return@subscribe
                        val data = msg.data
                        val target = when (data) {
                            is Map<*, *> -> (data["targetPlayer"] as? String)?.trim()
                            is com.google.gson.JsonObject -> data.get("targetPlayer")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                            else -> try {
                                val json = com.google.gson.Gson().toJson(data)
                                com.google.gson.JsonParser.parseString(json).asJsonObject.get("targetPlayer")?.asString?.trim()
                            } catch (_: Exception) { null }
                        } ?: return@subscribe
                        if (!target.equals(clientId, ignoreCase = true)) return@subscribe
                        val cmd = when (data) {
                            is Map<*, *> -> (data["command"] as? String)?.trim()
                            is com.google.gson.JsonObject -> data.get("command")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                            else -> try {
                                val json = com.google.gson.Gson().toJson(data)
                                com.google.gson.JsonParser.parseString(json).asJsonObject.get("command")?.asString?.trim()
                            } catch (_: Exception) { null }
                        } ?: return@subscribe
                        if (cmd.isEmpty()) return@subscribe
                        val c = if (cmd.startsWith("/")) cmd.drop(1) else cmd
                        SkyblockImproved.mc.execute {
                            SkyblockImproved.mc.player?.connection?.sendCommand(c)
                        }
                    }

                    val playersCh = realtime.channels.get("skyblock:players")
                    playersChannelRef.set(playersCh)
                    playersCh.attach()
                    playersCh.presence.enter(mapOf("playerName" to clientId), null)
                }
                realtime.connection.on(ConnectionEvent.closed) {
                    playersChannelRef.set(null)
                }
                realtime.connection.on(ConnectionEvent.failed) { playersChannelRef.set(null) }
            } catch (e: AblyException) {
                SkyblockImproved.logger.warn("Mod sync Ably error: {}", e.message)
            } catch (e: Exception) {
                SkyblockImproved.logger.debug("Mod sync connect error: {}", e.message)
            }
        }
    }

    override fun disconnect() {
        executor.execute {
            try {
                val ch = playersChannelRef.getAndSet(null)
                val ably = ablyRef.getAndSet(null)
                if (ch != null && ably != null) {
                    ch.presence.leave(null, object : io.ably.lib.realtime.CompletionListener {
                        override fun onSuccess() {
                            try { ably.close() } catch (_: Exception) {}
                        }
                        override fun onError(err: io.ably.lib.types.ErrorInfo?) {
                            try { ably.close() } catch (_: Exception) {}
                        }
                    })
                } else if (ably != null) {
                    try { ably.close() } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                ablyRef.getAndSet(null)?.close()
            }
        }
    }

    override fun publish(payload: JsonObject): Boolean {
        val ch = playersChannelRef.get() ?: return false
        executor.execute {
            try {
                ch.publish("update", com.google.gson.Gson().toJson(payload))
            } catch (e: Exception) {
                SkyblockImproved.logger.debug("Mod sync publish error: {}", e.message)
            }
        }
        return true
    }

    override fun publishChat(playerName: String, message: String, timestamp: String): Boolean {
        val ch = playersChannelRef.get() ?: return false
        executor.execute {
            try {
                val payload = mapOf(
                    "playerName" to playerName,
                    "message" to message,
                    "timestamp" to timestamp,
                )
                ch.publish("chat", com.google.gson.Gson().toJson(payload))
            } catch (e: Exception) {
                SkyblockImproved.logger.debug("Mod sync chat publish error: {}", e.message)
            }
        }
        return true
    }

    override fun isConnected(): Boolean {
        val ably = ablyRef.get() ?: return false
        return ably.connection.state == ConnectionState.connected
    }

    private fun fetchToken(baseUrl: String, clientId: String): String? {
        return try {
            val req = HttpRequest.newBuilder(
                URI.create("$baseUrl/mod/token?clientId=${java.net.URLEncoder.encode(clientId, Charsets.UTF_8)}")
            )
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() !in 200..299) return null
            val json = com.google.gson.JsonParser.parseString(resp.body()).asJsonObject
            json.get("token")?.asString
        } catch (e: Exception) {
            SkyblockImproved.logger.debug("Token fetch error: {}", e.message)
            null
        }
    }
}
