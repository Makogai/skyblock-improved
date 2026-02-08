package me.sbi.settings

import me.sbi.core.Module

/**
 * Base class for module settings.
 */
abstract class Setting<T>(
    val name: String,
    var description: String = ""
) {
    abstract val default: T
    abstract var value: T

    protected var hidden = false

    fun hide(): Setting<T> {
        hidden = true
        return this
    }

    protected var visibilityDependency: (() -> Boolean)? = null

    open fun reset() { value = default }

    val isVisible: Boolean
        get() = (visibilityDependency?.invoke() ?: true) && !hidden

    fun withDependency(dependency: () -> Boolean): Setting<T> {
        visibilityDependency = dependency
        return this
    }
}
