package me.sbi.settings

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Interface for settings that can be saved/loaded from config.
 */
interface Saving {
    fun read(element: JsonElement, gson: Gson)
    fun write(gson: Gson): JsonElement
}
