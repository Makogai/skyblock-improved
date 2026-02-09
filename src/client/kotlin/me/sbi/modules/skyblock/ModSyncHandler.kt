package me.sbi.modules.skyblock

import com.google.gson.JsonObject

/**
 * Interface for Mod Sync backend. Implementations are lazy-loaded to avoid
 * pulling in Ably (or other deps) at startup - prevents Minecraft crash
 * reporter NPE when StackTraceElement.getFileName() is null.
 */
interface ModSyncHandler {

    fun connect(baseUrl: String, clientId: String, syncPayload: String? = null)

    fun disconnect()

    fun publish(payload: JsonObject): Boolean

    fun publishChat(playerName: String, message: String, timestamp: String): Boolean

    fun pushSyncPayload(baseUrl: String, clientId: String, syncPayload: String?)

    fun isConnected(): Boolean
}
