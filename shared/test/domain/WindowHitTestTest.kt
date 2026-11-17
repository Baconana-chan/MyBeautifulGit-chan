package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowHitTestTest {

    private val maximize = WindowHitRect(
        left = 0.85f,
        top = 0f,
        right = 0.92f,
        bottom = 0.05f,
    )

    @Test
    fun `detects edges and corners before client content`() {
        assertEquals(
            WindowHitTarget.TopLeft,
            windowHitTarget(1000, 800, 2, 2, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.TopRight,
            windowHitTarget(1000, 800, 995, 2, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.BottomLeft,
            windowHitTarget(1000, 800, 2, 796, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.BottomRight,
            windowHitTarget(1000, 800, 995, 796, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Left,
            windowHitTarget(1000, 800, 1, 400, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Right,
            windowHitTarget(1000, 800, 999, 400, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Top,
            windowHitTarget(1000, 800, 500, 1, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Bottom,
            windowHitTarget(1000, 800, 500, 799, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Client,
            windowHitTarget(1000, 800, 500, 400, false, 8, maximize),
        )
    }

    @Test
    fun `maximize button hit area is normalized and ignored while invalid`() {
        assertEquals(
            WindowHitTarget.MaximizeButton,
            windowHitTarget(1000, 800, 880, 20, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Client,
            windowHitTarget(1000, 800, 950, 20, false, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.Client,
            windowHitTarget(1000, 800, 880, 20, false, 8, null),
        )
        assertEquals(
            WindowHitTarget.Client,
            windowHitTarget(
                1000,
                800,
                880,
                20,
                false,
                8,
                WindowHitRect(0.9f, 0f, 0.8f, 0.05f),
            ),
        )
    }

    @Test
    fun `maximized window keeps only the maximize button hot zone`() {
        assertEquals(
            WindowHitTarget.Client,
            windowHitTarget(1000, 800, 1, 1, true, 8, maximize),
        )
        assertEquals(
            WindowHitTarget.MaximizeButton,
            windowHitTarget(1000, 800, 880, 20, true, 8, maximize),
        )
    }
}
