package me.sbi.util

import me.sbi.SkyblockImproved
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors

/**
 * Checks GitHub releases for updates, downloads the new JAR, and replaces the old one on next restart.
 */
object UpdateChecker {

    private const val GITHUB_REPO = "Makogai/skyblock-improved"
    private const val MOD_ID = "skyblock-improved"

    private val executor = Executors.newSingleThreadExecutor()
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    data class UpdateInfo(val version: String, val jarUrl: String, val releaseUrl: String)

    fun checkAsync() {
        executor.execute {
            try {
                val current = currentVersion()
                val update = fetchLatestRelease() ?: return@execute
                if (!isNewer(update.version, current)) return@execute

                val gameDir = FabricLoader.getInstance().gameDir
                val updateDir = gameDir.resolve("skyblock-improved-update")
                val newJar = updateDir.resolve("skyblock-improved-${update.version}.jar")

                if (!downloadJar(update.jarUrl, newJar)) {
                    notifyDownloadFailed(update)
                    return@execute
                }

                val currentJarPath = getCurrentJarPath() ?: run {
                    notifyDownloadedManual(update.version, newJar)
                    return@execute
                }

                if (scheduleReplaceOnExit(currentJarPath, newJar)) {
                    notifyUpdateScheduled(update.version)
                } else {
                    notifyDownloadedManual(update.version, newJar)
                }
            } catch (e: Exception) {
                SbiLog.debug("Update check failed: ${e.message}")
            }
        }
    }

    private fun getCurrentJarPath(): Path? {
        val roots = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().rootPaths
        if (roots.isEmpty()) return null
        val uri = roots[0].toUri()
        return when (uri.scheme) {
            "jar" -> {
                val spec = uri.rawSchemeSpecificPart
                val fileUriStr = spec.substringBefore("!/")
                try {
                    Paths.get(URI.create(fileUriStr))
                } catch (_: Exception) {
                    null
                }
            }
            "file" -> try { Paths.get(uri) } catch (_: Exception) { null }
            else -> null
        }?.takeIf { Files.isRegularFile(it) }
    }

    private fun scheduleReplaceOnExit(currentJar: Path, newJar: Path): Boolean {
        val pid = ProcessHandle.current().pid()
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> createWindowsScript(pid, currentJar, newJar)
            else -> createUnixScript(pid, currentJar, newJar)
        }
    }

    private fun createWindowsScript(pid: Long, currentJar: Path, newJar: Path): Boolean {
        return try {
            val gameDir = FabricLoader.getInstance().gameDir
            val script = gameDir.resolve("skyblock-improved-replace.bat")
            val content = """
                @echo off
                setlocal
                :wait
                tasklist /FI "PID eq $pid" 2>NUL | find /I "java" >NUL
                if %ERRORLEVEL%==0 (timeout /t 2 /nobreak >NUL & goto wait)
                copy /Y "${newJar.toAbsolutePath().toString().replace("/", "\\")}" "${currentJar.toAbsolutePath().toString().replace("/", "\\")}"
                del "${newJar.toAbsolutePath().toString().replace("/", "\\")}"
                rmdir "${newJar.parent.toAbsolutePath().toString().replace("/", "\\")}" 2>NUL
                del "%~f0"
            """.trimIndent()
            Files.writeString(script, content)
            ProcessBuilder("cmd", "/c", "start", "/min", "cmd", "/c", script.toString())
                .directory(gameDir.toFile())
                .start()
            true
        } catch (e: Exception) {
            SbiLog.debug("Could not create replace script: ${e.message}")
            false
        }
    }

    private fun createUnixScript(pid: Long, currentJar: Path, newJar: Path): Boolean {
        return try {
            val gameDir = FabricLoader.getInstance().gameDir
            val script = gameDir.resolve("skyblock-improved-replace.sh")
            val content = """
                #!/bin/sh
                while kill -0 $pid 2>/dev/null; do sleep 2; done
                cp -f "${newJar}" "${currentJar}"
                rm -f "${newJar}"
                rmdir "${newJar.parent}" 2>/dev/null || true
                rm -f "$0"
            """.trimIndent()
            Files.writeString(script, content)
            script.toFile().setExecutable(true)
            ProcessBuilder("sh", script.toString())
                .directory(gameDir.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            true
        } catch (e: Exception) {
            SbiLog.debug("Could not create replace script: ${e.message}")
            false
        }
    }

    private fun notifyUpdateScheduled(version: String) {
        SkyblockImproved.mc.execute {
            val msg = Component.literal("[SBI] ")
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA))
                .append(Component.literal("Update").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal(": Version "))
                .append(Component.literal(version).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                .append(Component.literal(" downloaded. Restart Minecraft to complete the update."))
            SkyblockImproved.mc.gui.chat.addMessage(msg)
        }
    }

    private fun notifyDownloadedManual(version: String, path: Path) {
        SkyblockImproved.mc.execute {
            val msg = Component.literal("[SBI] ")
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA))
                .append(Component.literal("Update").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal(": Version "))
                .append(Component.literal(version).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                .append(Component.literal(" downloaded to "))
                .append(Component.literal(path.toString()).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                .append(Component.literal(". Replace the old JAR in mods/ and restart."))
            SkyblockImproved.mc.gui.chat.addMessage(msg)
        }
    }

    private fun notifyDownloadFailed(update: UpdateInfo) {
        SkyblockImproved.mc.execute {
            val msg = Component.literal("[SBI] ")
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA))
                .append(Component.literal("Update").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                .append(Component.literal(": New version "))
                .append(Component.literal(update.version).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
                .append(Component.literal(" available! Download: "))
                .append(Component.literal(update.releaseUrl).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
            SkyblockImproved.mc.gui.chat.addMessage(msg)
        }
    }

    private fun currentVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow()
            .metadata
            .version
            .toString()
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        val req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
            .header("Accept", "application/vnd.github.v3+json")
            .GET()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() != 200) return null
        val json = res.body()
        val tagMatch = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(json) ?: return null
        val version = tagMatch.groupValues[1].removePrefix("v")
        val htmlUrlMatch = Regex(""""html_url"\s*:\s*"([^"]+)"""").find(json) ?: return null
        val releaseUrl = htmlUrlMatch.groupValues[1]
        val assetMatch = Regex(""""browser_download_url"\s*:\s*"([^"]+\.jar)"""").findAll(json)
            .map { it.groupValues[1] }
            .firstOrNull { !it.contains("-sources") } ?: return null
        return UpdateInfo(version, assetMatch, releaseUrl)
    }

    private fun isNewer(new: String, current: String): Boolean {
        fun parse(v: String): List<Int> = v.split('.').mapNotNull { it.toIntOrNull() }
        val n = parse(new)
        val c = parse(current)
        for (i in 0 until maxOf(n.size, c.size)) {
            val nn = n.getOrElse(i) { 0 }
            val cc = c.getOrElse(i) { 0 }
            if (nn > cc) return true
            if (nn < cc) return false
        }
        return false
    }

    private fun downloadJar(url: String, target: java.nio.file.Path): Boolean {
        return try {
            Files.createDirectories(target.parent)
            val req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
            val res = http.send(req, HttpResponse.BodyHandlers.ofInputStream())
            if (res.statusCode() != 200) return false
            Files.copy(res.body(), target, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            SbiLog.debug("Download failed: ${e.message}")
            false
        }
    }
}
