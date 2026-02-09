package me.sbi.modules.skyblock

import com.mojang.blaze3d.platform.NativeImage
import me.sbi.SkyblockImproved
import net.minecraft.client.Screenshot
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Captures screenshots from the game and uploads them to the admin backend.
 * Uses a safe downscale factor (computed from framebuffer size) for any resolution.
 */
object ScreenshotUploader {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sbi-screenshot-upload").apply { isDaemon = true }
    }

    /** Interval in seconds between screenshot captures. */
    const val INTERVAL_SEC = 10

    /**
     * Returns a downscale factor that divides both width and height, so takeScreenshot never throws.
     * Uses 8, 4, 2, or 1 depending on framebuffer dimensions. Works for any resolution.
     */
    private fun safeDownscale(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        return when {
            width % 8 == 0 && height % 8 == 0 -> 8
            width % 4 == 0 && height % 4 == 0 -> 4
            width % 2 == 0 && height % 2 == 0 -> 2
            else -> 1
        }
    }

    fun captureAndUpload(baseUrl: String, playerName: String) {
        val mc = SkyblockImproved.mc
        val target = mc.mainRenderTarget ?: return
        val downscale = safeDownscale(target.width, target.height)
        Screenshot.takeScreenshot(target, downscale) { img ->
            try {
                img.use { nativeImage ->
                    val tempFile = File.createTempFile("sbi_ss_", ".png")
                    try {
                        nativeImage.writeToFile(tempFile.toPath())
                        val bytes = Files.readAllBytes(tempFile.toPath())
                        uploadBytes(bytes, baseUrl, playerName)
                    } finally {
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                SkyblockImproved.logger.warn("Screenshot failed: {}", e.message)
            }
        }
    }

    private fun uploadBytes(imageBytes: ByteArray, baseUrl: String, playerName: String) {
        executor.execute {
            try {
                val url = "$baseUrl/mod/screenshot?clientId=${URLEncoder.encode(playerName, StandardCharsets.UTF_8)}"
                val boundary = "----SBIFormBoundary${System.currentTimeMillis()}"
                val body = buildMultipartBody(boundary, playerName, imageBytes)
                val req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build()
                val resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding())
                if (resp.statusCode() !in 200..299) {
                    SkyblockImproved.logger.warn("Screenshot upload returned {}", resp.statusCode())
                }
            } catch (e: Exception) {
                SkyblockImproved.logger.warn("Screenshot upload error: {}", e.message)
            }
        }
    }

    private fun buildMultipartBody(boundary: String, playerName: String, imageBytes: ByteArray): ByteArray {
        val crlf = "\r\n"
        val part1 = "--$boundary$crlf" +
            "Content-Disposition: form-data; name=\"clientId\"$crlf$crlf" +
            "$playerName$crlf" +
            "--$boundary$crlf" +
            "Content-Disposition: form-data; name=\"image\"; filename=\"screenshot.png\"$crlf" +
            "Content-Type: image/png$crlf$crlf"
        val part2 = "$crlf--$boundary--$crlf"
        return part1.toByteArray(StandardCharsets.UTF_8) + imageBytes + part2.toByteArray(StandardCharsets.UTF_8)
    }
}
