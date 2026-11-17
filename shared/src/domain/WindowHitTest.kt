package org.baconana.mybeautifulgit_chan.domain

/** Normalized client-area rectangle (0..1) used by the native window hit-test. */
data class WindowHitRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val valid: Boolean
        get() = left >= 0f && top >= 0f && right <= 1f && bottom <= 1f &&
            left < right && top < bottom
}

enum class WindowHitTarget {
    Client,
    Left,
    Right,
    Top,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    MaximizeButton,
}

/**
 * Pure geometry for the Windows WndProc. Borders take precedence over the
 * maximize button while the window is normal; a maximized window cannot be
 * resized from its edges.
 */
fun windowHitTarget(
    clientWidth: Int,
    clientHeight: Int,
    x: Int,
    y: Int,
    maximized: Boolean,
    border: Int,
    maximizeBounds: WindowHitRect?,
): WindowHitTarget {
    if (clientWidth <= 0 || clientHeight <= 0) return WindowHitTarget.Client
    val edge = border.coerceAtLeast(0)
    val inLeft = x < edge
    val inRight = x >= clientWidth - edge
    val inTop = y < edge
    val inBottom = y >= clientHeight - edge

    if (!maximized) {
        when {
            inLeft && inTop -> return WindowHitTarget.TopLeft
            inRight && inTop -> return WindowHitTarget.TopRight
            inLeft && inBottom -> return WindowHitTarget.BottomLeft
            inRight && inBottom -> return WindowHitTarget.BottomRight
            inLeft -> return WindowHitTarget.Left
            inRight -> return WindowHitTarget.Right
            inTop -> return WindowHitTarget.Top
            inBottom -> return WindowHitTarget.Bottom
        }
    }

    val bounds = maximizeBounds?.takeIf { it.valid } ?: return WindowHitTarget.Client
    val left = (bounds.left * clientWidth).toInt()
    val top = (bounds.top * clientHeight).toInt()
    val right = (bounds.right * clientWidth).toInt()
    val bottom = (bounds.bottom * clientHeight).toInt()
    return if (x >= left && x < right && y >= top && y < bottom) {
        WindowHitTarget.MaximizeButton
    } else {
        WindowHitTarget.Client
    }
}
