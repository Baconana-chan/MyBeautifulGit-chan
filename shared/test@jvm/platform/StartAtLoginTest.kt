package org.baconana.mybeautifulgit_chan.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class StartAtLoginTest {

    @Test
    fun `windows command arguments are quoted only when needed`() {
        assertEquals(
            "C:\\Tools\\git-chan.exe",
            StartAtLogin.quoteWindowsArgument("C:\\Tools\\git-chan.exe"),
        )
        assertEquals(
            "\"C:\\Program Files\\Git-chan\\git-chan.exe\"",
            StartAtLogin.quoteWindowsArgument("C:\\Program Files\\Git-chan\\git-chan.exe"),
        )
        assertEquals(
            "C:\\Tools\\git-chan.exe \"--config=C:\\My Files\\config\"",
            listOf(
                "C:\\Tools\\git-chan.exe",
                "--config=C:\\My Files\\config",
            ).joinToString(" ") { StartAtLogin.quoteWindowsArgument(it) },
        )
    }

    @Test
    fun `linux desktop arguments escape spaces and field codes`() {
        assertEquals(
            "\"/opt/My Git/app\"",
            StartAtLogin.quoteDesktopArgument("/opt/My Git/app"),
        )
        assertEquals(
            "100%%ready",
            StartAtLogin.quoteDesktopArgument("100%ready"),
        )
    }
}
