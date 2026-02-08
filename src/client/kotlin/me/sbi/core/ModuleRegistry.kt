package me.sbi.core

/**
 * Build-time module filter. Edit before building to exclude modules from the JAR.
 * Excluded modules won't appear in ClickGui and won't run.
 *
 * Usage:
 * - Empty set = include all modules
 * - Add module names (lowercase, as in ClickGui) to exclude them
 *
 * Example minimal build (no ESP, no Party GUI, no HUD Flair):
 *   setOf("esp", "party gui", "party display", "hud flair")
 *
 * Note: Excluding ChatParser, SkyBlockData, or ClickGui may break other features.
 */
object ModuleRegistry {
    /** Module names to exclude. Empty = include all. */
    val DISABLED_MODULES: Set<String> = setOf(
        // "esp",
        "party gui",
        // "party display",
        // "area display",
        // "coords display",
        "name changer",
        "example mining",
    )

    fun isEnabled(module: Module): Boolean =
        module.name.lowercase() !in DISABLED_MODULES
}
