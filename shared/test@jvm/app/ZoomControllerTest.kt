package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.platform.AppSettings
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoomControllerTest {

    @Test
    fun `steps clamp reset and persist across controllers`() {
        val dir = Files.createTempDirectory("git-chan-zoom")
        try {
            val file = dir.resolve("settings.properties")
            val zoom = ZoomController(AppSettings(file))

            assertEquals(1f, zoom.scale, 0.0001f)
            assertEquals("100%", zoom.label)

            zoom.step(1)
            assertEquals(1.1f, zoom.scale, 0.0001f)
            repeat(20) { zoom.step(1) }
            assertEquals(2f, zoom.scale, 0.0001f)

            zoom.reset()
            assertEquals(1f, zoom.scale, 0.0001f)

            zoom.step(-1)
            assertEquals(0.9f, zoom.scale, 0.0001f)
            repeat(20) { zoom.step(-1) }
            assertEquals(0.9f, zoom.scale, 0.0001f)

            val restored = ZoomController(AppSettings(file))
            assertEquals(0.9f, restored.scale, 0.0001f)
            assertEquals("90%", restored.label)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `invalid saved zoom falls back to neutral`() {
        val dir = Files.createTempDirectory("git-chan-zoom-invalid")
        try {
            val file = dir.resolve("settings.properties")
            AppSettings(file).saveZoom(Float.NaN)
            assertEquals(1f, ZoomController(AppSettings(file)).scale, 0.0001f)

            AppSettings(file).saveZoom(9f)
            assertEquals(1f, ZoomController(AppSettings(file)).scale, 0.0001f)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
