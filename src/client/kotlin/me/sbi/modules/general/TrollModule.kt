package me.sbi.modules.general

import me.sbi.SkyblockImproved
import me.sbi.core.Category
import me.sbi.core.Module
import me.sbi.settings.impl.NumberSetting
import org.lwjgl.glfw.GLFW
import org.w3c.dom.Element
import java.io.File
import javax.imageio.ImageIO

/**
 * HUD Flair - occasional on-screen display. Bundled images from assets/.../hud_flair/,
 * or config/skyblock-improved/hud_flair/. Supports PNG and animated GIF.
 * Edit MESSAGE_OPTIONS, FREQUENCY_SEC in code. {name} = player name.
 */
object TrollModule : Module(
    name = "HUD Flair",
    key = GLFW.GLFW_KEY_UNKNOWN,
    category = Category.MISC,
    description = "Occasional on-screen display",
    toggled = true
) {
    /** Messages to randomly show. Use {name} for player name. Add/edit here. */
    private val MESSAGE_OPTIONS = listOf(
        "{name} peder"
    )
    private const val FREQUENCY_SEC = 50
    private const val DURATION_SEC = 5

    val frequencySec = NumberSetting("Interval (sec)", FREQUENCY_SEC.toDouble(), 10.0, 600.0, 15.0, 0, "Display interval").also { registerSetting(it) }
    val durationSec = NumberSetting("Display time (sec)", DURATION_SEC.toDouble(), 2.0, 30.0, 1.0, 0, "Show duration").also { registerSetting(it) }
    val imageScale = NumberSetting("Size %", 100.0, 25.0, 200.0, 25.0, 0, "Display size").also { registerSetting(it) }

    private var lastShowTick = 0
    private var showUntilTick = 0
    private var currentImagePath: String? = null
    private var cachedTextureId: net.minecraft.resources.ResourceLocation? = null
    private var cachedTextureIds: List<net.minecraft.resources.ResourceLocation>? = null
    private var cachedFrameDelays: IntArray? = null
    private var currentGifFrame = 0
    private var lastGifFrameTimeMs = 0L
    private var cachedTexWidth = 256
    private var cachedTexHeight = 256

    private val configFolder: File
        get() = File(SkyblockImproved.configDir, "hud_flair").apply { mkdirs() }

    /** Bundled filenames - extracted to config on first load so users can add their own */
    private val BUNDLED_FILES = listOf(
        "akatsuki.gif", "cry.gif", "dungeon.gif", "emo1.gif", "gothsmall.gif",
        "love.gif", "megumin.gif", "rem.gif", "ueku.gif", "zerotwo1.gif"
    )
    private const val ASSETS_PATH = "assets/skyblock-improved/hud_flair/"
    private var extractedOnce = false

    /** Call on client init to extract bundled images to config folder. */
    fun ensureExtracted() {
        ensureBundledFilesExtracted()
    }

    /** Copy bundled images to config folder on first run. Load only from config after that. */
    private fun ensureBundledFilesExtracted() {
        if (extractedOnce) return
        extractedOnce = true
        val folder = configFolder
        val cl = TrollModule::class.java.classLoader
        var copied = 0
        for (f in BUNDLED_FILES) {
            val dest = File(folder, f)
            if (dest.exists()) continue
            val path = ASSETS_PATH + f
            cl.getResourceAsStream(path)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                    copied++
                }
            }
        }
        if (copied > 0) SkyblockImproved.logger.info("HUD Flair: extracted $copied images to ${folder.absolutePath}")
    }

    /** Returns file paths from config folder only (bundled files are extracted there on load) */
    private fun listImageSources(): List<String> {
        ensureBundledFilesExtracted()
        return configFolder.listFiles()?.filter { f ->
            f.isFile && (f.extension.equals("png", true) || f.extension.equals("gif", true))
        }?.map { it.absolutePath } ?: emptyList()
    }

    override fun onTick() {
        if (!enabled) return
        val player = SkyblockImproved.mc.player ?: return
        val tick = player.tickCount

        if (showUntilTick > 0 && tick < showUntilTick) return
        if (showUntilTick > 0 && tick >= showUntilTick) {
            showUntilTick = 0
            releaseTexture()
        }

        val freq = (frequencySec.value * 20).toInt()
        if (tick - lastShowTick < freq) return

        val images = listImageSources()
        if (images.isEmpty()) return

        lastShowTick = tick
        showUntilTick = tick + (durationSec.value * 20).toInt()
        currentImagePath = images.random()
    }

    fun getDisplayMessage(): String {
        val name = SkyblockImproved.mc.player?.name?.string ?: "Player"
        val template = MESSAGE_OPTIONS.randomOrNull() ?: "{name}"
        return template.replace("{name}", name, ignoreCase = true)
    }

    fun isShowing(): Boolean = showUntilTick > 0 && SkyblockImproved.mc.player?.let {
        it.tickCount < showUntilTick
    } == true

    fun getCurrentImagePath(): String? = currentImagePath

    fun getImageScale(): Float = (imageScale.value / 100f).toFloat()

    fun getTextureDimensions(): Pair<Int, Int> = cachedTexWidth to cachedTexHeight

    fun advanceAnimation(): net.minecraft.resources.ResourceLocation? {
        val ids = cachedTextureIds
        val delays = cachedFrameDelays
        if (ids == null || ids.isEmpty()) return cachedTextureId
        if (ids.size == 1) return ids[0]
        if (delays == null || delays.size != ids.size) return ids.getOrNull(currentGifFrame)
        val now = System.currentTimeMillis()
        val delay = delays[currentGifFrame].coerceAtLeast(50)
        if (now - lastGifFrameTimeMs >= delay) {
            lastGifFrameTimeMs = now
            currentGifFrame = (currentGifFrame + 1) % ids.size
        }
        return ids.getOrNull(currentGifFrame)
    }

    fun releaseTexture() {
        cachedTextureIds?.forEach { id ->
            try { SkyblockImproved.mc.textureManager.release(id) } catch (_: Exception) { }
        }
        cachedTextureIds = null
        cachedTextureId?.let { id ->
            try { SkyblockImproved.mc.textureManager.release(id) } catch (_: Exception) { }
            cachedTextureId = null
        }
        cachedFrameDelays = null
        currentGifFrame = 0
        currentImagePath = null
    }

    fun getOrLoadTexture(path: String): net.minecraft.resources.ResourceLocation? {
        if (currentImagePath != path) {
            releaseTexture()
        } else if (cachedTextureIds != null) {
            return advanceAnimation()
        } else if (cachedTextureId != null) {
            return cachedTextureId
        }

        return try {
            val file = File(path)
            if (!file.exists()) return null
            val name = file.nameWithoutExtension
            val baseId = "skyblock-improved:hflair_${name.hashCode() and 0x7FFFFFFF}"
            if (file.extension.equals("gif", true)) {
                val frames = loadAllGifFrames(file)
                if (frames.isEmpty()) {
                    val first = loadGifFirstFrame(file) ?: return null
                    val tex = net.minecraft.client.renderer.texture.DynamicTexture(java.util.function.Supplier { "troll" }, first)
                    val id = net.minecraft.resources.ResourceLocation.parse(baseId)
                    SkyblockImproved.mc.textureManager.register(id, tex)
                    cachedTextureId = id
                    cachedTexWidth = first.width
                    cachedTexHeight = first.height
                    currentImagePath = path
                    return id
                }
                val ids = mutableListOf<net.minecraft.resources.ResourceLocation>()
                val delays = IntArray(frames.size)
                frames.forEachIndexed { i, (native, delayMs) ->
                    val tex = net.minecraft.client.renderer.texture.DynamicTexture(java.util.function.Supplier { "troll" }, native)
                    val id = net.minecraft.resources.ResourceLocation.parse("${baseId}_$i")
                    SkyblockImproved.mc.textureManager.register(id, tex)
                    ids.add(id)
                    delays[i] = delayMs
                }
                cachedTextureIds = ids
                cachedFrameDelays = delays
                cachedTexWidth = frames[0].first.width
                cachedTexHeight = frames[0].first.height
                currentGifFrame = 0
                lastGifFrameTimeMs = System.currentTimeMillis()
                currentImagePath = path
                advanceAnimation()
            } else {
                val img = com.mojang.blaze3d.platform.NativeImage.read(file.inputStream()) ?: return null
                val texture = net.minecraft.client.renderer.texture.DynamicTexture(java.util.function.Supplier { "troll" }, img)
                val id = net.minecraft.resources.ResourceLocation.parse(baseId)
                SkyblockImproved.mc.textureManager.register(id, texture)
                cachedTextureId = id
                cachedTexWidth = img.width
                cachedTexHeight = img.height
                currentImagePath = path
                id
            }
        } catch (e: Exception) {
            SkyblockImproved.logger.warn("HUD Flair: failed to load image: ${e.message}")
            null
        }
    }

    /** Load all GIF frames - one texture per frame. Reads until no more frames (no getNumImages). */
    private fun loadAllGifFrames(file: File): List<Pair<com.mojang.blaze3d.platform.NativeImage, Int>> {
        val result = mutableListOf<Pair<com.mojang.blaze3d.platform.NativeImage, Int>>()
        try {
            val readers = javax.imageio.ImageIO.getImageReadersByFormatName("gif")
            if (!readers.hasNext()) return emptyList()
            val reader = readers.next()
            try {
                file.inputStream().use { stream ->
                    val iis = javax.imageio.ImageIO.createImageInputStream(stream)
                    reader.setInput(iis, false)
                    var i = 0
                    while (i < 1000) {
                        val buffered = try { reader.read(i) } catch (_: Exception) { break }
                        val img = buffered as? java.awt.image.BufferedImage ?: break
                        val delayMs = getGifFrameDelayMs(reader, i)
                        bufferedToNative(img)?.let { result.add(it to delayMs) } ?: break
                        i++
                    }
                }
            } finally {
                reader.dispose()
            }
        } catch (e: Exception) {
            SkyblockImproved.logger.warn("HUD Flair: could not load GIF frames ${file.name}: ${e.message}")
        }
        return result
    }

    private fun getGifFrameDelayMs(reader: javax.imageio.ImageReader, index: Int): Int = try {
        val meta = reader.getImageMetadata(index) ?: return 100
        val format = meta.nativeMetadataFormatName ?: "javax_imageio_gif_image_1.0"
        val root = meta.getAsTree(format) as? Element ?: return 100
        val list = root.getElementsByTagName("GraphicControlExtension")
        if (list.length == 0) return 100
        val gce = list.item(0) as? Element ?: return 100
        val delay = gce.getAttribute("delayTime").toIntOrNull() ?: return 100
        (delay * 10).coerceIn(20, 1000)
    } catch (_: Exception) { 100 }

    private fun loadGifFirstFrame(file: File): com.mojang.blaze3d.platform.NativeImage? {
        var buffered: java.awt.image.BufferedImage? = null
        try {
            val readers = javax.imageio.ImageIO.getImageReadersByFormatName("gif")
            if (readers.hasNext()) {
                val reader = readers.next()
                try {
                    file.inputStream().use { stream ->
                        val iis = javax.imageio.ImageIO.createImageInputStream(stream)
                        reader.setInput(iis, false)
                        buffered = reader.read(0) as? java.awt.image.BufferedImage
                    }
                } finally {
                    reader.dispose()
                }
            }
            if (buffered == null) buffered = ImageIO.read(file)
        } catch (_: Exception) { }
        return buffered?.let { bufferedToNative(it) }
    }

    private fun bufferedToNative(buffered: java.awt.image.BufferedImage): com.mojang.blaze3d.platform.NativeImage? = try {
        val temp = java.io.File.createTempFile("hflair_gif", ".png")
        try {
            if (!ImageIO.write(buffered, "png", temp)) return null
            com.mojang.blaze3d.platform.NativeImage.read(temp.inputStream())
        } finally {
            temp.delete()
        }
    } catch (e: Exception) {
        SkyblockImproved.logger.warn("HUD Flair: BufferedImage to NativeImage failed: ${e.message}")
        null
    }

    override fun onDisable() {
        releaseTexture()
        showUntilTick = 0
    }

    override fun onKeybind() {}
}
