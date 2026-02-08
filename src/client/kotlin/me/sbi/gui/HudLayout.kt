package me.sbi.gui

import me.sbi.SkyblockImproved
import java.io.File

/**
 * Positions for HUD elements. Each element has x,y. Saved to hud.json.
 */
object HudLayout {

    enum class Element(val key: String, val defaultX: Int, val defaultY: Int) {
        COORDS("coords", 5, 5),
        NICK("nick", 5, 17),
        AREA("area", 5, 29),
        PARTY("party", 5, 41)
    }

    private val positions = mutableMapOf<String, Pair<Int, Int>>()
    private val file = File(SkyblockImproved.configDir, "hud.json")

    fun x(element: Element): Int = positions[element.key]?.first ?: element.defaultX
    fun y(element: Element): Int = positions[element.key]?.second ?: element.defaultY

    fun set(element: Element, x: Int, y: Int) {
        positions[element.key] = Pair(x, y)
    }

    fun load() {
        try {
            if (!file.exists()) return
            val content = file.readText()
            if (content.isBlank()) return
            val obj = com.google.gson.JsonParser.parseString(content).asJsonObject
            for (e in Element.entries) {
                val arr = obj.getAsJsonArray(e.key)
                if (arr != null && arr.size() >= 2) {
                    positions[e.key] = Pair(arr[0].asInt, arr[1].asInt)
                }
            }
        } catch (_: Exception) { }
    }

    fun save() {
        try {
            file.parentFile?.mkdirs()
            val obj = com.google.gson.JsonObject()
            for (e in Element.entries) {
                val (px, py) = positions[e.key] ?: Pair(e.defaultX, e.defaultY)
                obj.add(e.key, com.google.gson.JsonArray().apply {
                    add(px)
                    add(py)
                })
            }
            file.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(obj))
        } catch (e: Exception) {
            SkyblockImproved.logger.error("Failed to save HUD layout", e)
        }
    }
}
