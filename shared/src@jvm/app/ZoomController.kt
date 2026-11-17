package org.baconana.mybeautifulgit_chan.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.baconana.mybeautifulgit_chan.platform.AppSettings
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * UI scale shared by the main and worktree windows. Persisting the factor means
 * a restart restores the same zoom, while the fixed step list keeps shortcuts
 * and title-bar buttons consistent.
 */
class ZoomController(
    private val settings: AppSettings = AppSettings(),
) {
    var index by mutableStateOf(nearestIndex(settings.loadZoom()))
        private set

    val scale: Float get() = STEPS[index]

    val label: String get() = "${(scale * 100).roundToInt()}%"

    fun step(delta: Int) {
        changeIndex(index + delta)
    }

    /** Ctrl+0 always returns to the neutral 100% step. */
    fun reset() {
        changeIndex(STEPS.indexOf(1f).takeIf { it >= 0 } ?: nearestIndex(1f))
    }

    private fun changeIndex(value: Int) {
        val clamped = value.coerceIn(0, STEPS.lastIndex)
        if (clamped == index) return
        index = clamped
        settings.saveZoom(STEPS[clamped])
    }

    private fun nearestIndex(scale: Float): Int =
        STEPS.indices.minByOrNull { abs(STEPS[it] - scale) } ?: 0

    companion object {
        val STEPS = listOf(0.9f, 1f, 1.1f, 1.25f, 1.5f, 2f)
    }
}
