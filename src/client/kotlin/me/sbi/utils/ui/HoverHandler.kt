package me.sbi.utils.ui

/**
 * Tracks hover state and delay before "ready" (e.g. for tooltips).
 * After hovering for [delayMs], [ready] returns true.
 */
class HoverHandler(private val delayMs: Long = 1000) {

    var isHovered = false
        private set

    private var hoverStartTime = 0L

    fun handle(x: Float, y: Float, w: Float, h: Float, mouseX: Int, mouseY: Int) {
        val hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h
        if (hovered != isHovered) {
            isHovered = hovered
            if (hovered) hoverStartTime = System.currentTimeMillis()
        }
    }

    /** True when hovered for at least [delayMs] */
    fun ready(): Boolean = isHovered && (System.currentTimeMillis() - hoverStartTime) >= delayMs

    /** 0-100, reaches 100 after [delayMs] of hovering */
    fun percent(): Float = when {
        !isHovered -> 0f
        else -> ((System.currentTimeMillis() - hoverStartTime) / delayMs.toFloat()).coerceIn(0f, 1f) * 100f
    }
}
