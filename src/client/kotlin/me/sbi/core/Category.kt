package me.sbi.core

/**
 * Categories for organizing modules in the ClickGUI.
 * Add new categories here as you add module types.
 */
data class Category(val name: String, val icon: String = "●") {
    companion object {
        private val categories = mutableMapOf<String, Category>()

        val GENERAL = register("General", "◆")
        val MINING = register("Mining", "■")
        val COMBAT = register("Combat", "▲")
        val RENDER = register("Render", "◈")
        val SKYBLOCK = register("Skyblock", "✦")
        val MISC = register("Misc", "○")

        fun register(name: String, icon: String = "●"): Category = categories.getOrPut(name) { Category(name, icon) }

        fun all(): Collection<Category> = categories.values.toList()
    }

    override fun toString(): String = name
}
