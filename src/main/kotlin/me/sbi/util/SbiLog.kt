package me.sbi.util

import org.slf4j.LoggerFactory

/**
 * Central logging for the mod. Use SbiLog.info/warn/error for normal logs.
 * SbiLog.debug() only logs when debug mode is enabled (dev/debug build).
 */
object SbiLog {

    private val log = LoggerFactory.getLogger("skyblock-improved")

    /** When true, debug() calls are logged. Toggle via SbiMessage or config. */
    @Volatile
    var debugEnabled: Boolean = false
        private set

    @JvmStatic
    fun isDebugEnabled(): Boolean = debugEnabled

    fun setDebug(enabled: Boolean) {
        debugEnabled = enabled
    }

    fun info(msg: String) = log.info(msg)
    fun info(msg: String, t: Throwable) = log.info(msg, t)
    fun warn(msg: String) = log.warn(msg)
    fun warn(msg: String, t: Throwable) = log.warn(msg, t)
    fun error(msg: String) = log.error(msg)
    fun error(msg: String, t: Throwable) = log.error(msg, t)

    /** Only logs when debugEnabled is true. */
    fun debug(msg: String) {
        if (debugEnabled) log.info("[DEBUG] $msg")
    }

    /** Only logs when debugEnabled is true. */
    fun debug(msg: String, t: Throwable) {
        if (debugEnabled) log.info("[DEBUG] $msg", t)
    }
}
