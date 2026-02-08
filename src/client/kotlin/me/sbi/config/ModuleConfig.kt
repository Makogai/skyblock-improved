package me.sbi.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.sbi.SkyblockImproved
import me.sbi.settings.Saving
import me.sbi.core.Module
import java.io.File

/**
 * Handles saving/loading module states and settings to JSON.
 * Odin-style config persistence.
 */
class ModuleConfig(file: File) {

    internal val modules = hashMapOf<String, Module>()

    private val file = file.apply {
        try {
            parentFile?.mkdirs()
            createNewFile()
        } catch (e: Exception) {
            SkyblockImproved.logger.error("Error initializing config file", e)
        }
    }

    fun load() {
        try {
            val content = file.bufferedReader().use { it.readText() }
            if (content.isBlank()) return

            val jsonArray = JsonParser.parseString(content).asJsonArray
            for (element in jsonArray) {
                val obj = element?.asJsonObject ?: continue
                val module = modules[obj.get("name")?.asString?.lowercase()] ?: continue

                if (obj.get("enabled")?.asBoolean != module.enabled) {
                    module.toggle()
                }

                obj.get("settings")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (key, value) ->
                    (module.settings[key] as? Saving)?.read(value ?: return@forEach, gson)
                }
            }
        } catch (e: Exception) {
            SkyblockImproved.logger.error("Error loading config", e)
        }
    }

    fun save() {
        try {
            val jsonArray = JsonArray().apply {
                for ((_, module) in modules) {
                    add(JsonObject().apply {
                        addProperty("name", module.name)
                        addProperty("enabled", module.enabled)
                        add("settings", JsonObject().apply {
                            for ((name, setting) in module.settings) {
                                if (setting is Saving) add(name, setting.write(gson))
                            }
                        })
                    })
                }
            }
            file.bufferedWriter().use { it.write(gson.toJson(jsonArray)) }
        } catch (e: Exception) {
            SkyblockImproved.logger.error("Error saving config", e)
        }
    }

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
    }
}
