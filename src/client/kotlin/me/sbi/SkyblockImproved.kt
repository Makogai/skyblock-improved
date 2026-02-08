package me.sbi

import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Main mod reference - provides access to Minecraft instance and config directory.
 * Only initialized on client.
 */
object SkyblockImproved {
    val logger = LoggerFactory.getLogger("skyblock-improved")

    val mc: Minecraft get() = Minecraft.getInstance()

    val configDir: File = File(mc.gameDirectory, "config/skyblock-improved/").apply {
        if (!exists()) mkdirs()
    }
}
