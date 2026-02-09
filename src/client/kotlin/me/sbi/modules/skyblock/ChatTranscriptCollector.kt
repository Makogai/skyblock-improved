package me.sbi.modules.skyblock

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Collects chat messages for Mod Sync. Populated by ChatComponentMixin.
 * ModSyncModule drains pending messages and publishes to Ably.
 */
object ChatTranscriptCollector {

    private const val MAX_MESSAGES = 100

    private val messages = CopyOnWriteArrayList<ChatEntry>()
    private val pendingIndex = AtomicInteger(0)

    data class ChatEntry(
        val text: String,
        val timestamp: String,
    )

    /** Called from ChatComponentMixin when a chat message is received. */
    @JvmStatic
    fun add(text: String) {
        if (text.isBlank()) return
        val entry = ChatEntry(text.trim(), java.time.Instant.now().toString())
        synchronized(messages) {
            messages.add(entry)
            while (messages.size > MAX_MESSAGES) messages.removeAt(0)
        }
    }

    /** Returns messages not yet published, and advances the pending index. */
    fun drainPendingForPublish(): List<ChatEntry> {
        val idx = pendingIndex.get()
        val list = messages.toList()
        if (idx >= list.size) return emptyList()
        val toPublish = list.subList(idx, list.size).toList()
        pendingIndex.set(list.size)
        return toPublish
    }

    /** All stored messages (for initial load - not used for real-time). */
    fun getAll(): List<ChatEntry> = messages.toList()

    /** Reset when Mod Sync disconnects. */
    fun clear() {
        messages.clear()
        pendingIndex.set(0)
    }
}
