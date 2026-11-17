package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmodulesTest {

    @Test
    fun `builds init update sync add and remove commands`() {
        assertEquals(
            listOf("submodule", "init", "--", "libs/foo"),
            submoduleInitArgs("libs/foo"),
        )
        assertEquals(
            listOf("submodule", "update", "--init", "--recursive", "--checkout", "--", "libs/foo"),
            submoduleUpdateArgs("libs/foo"),
        )
        assertEquals(
            listOf("submodule", "update", "--checkout"),
            submoduleUpdateArgs(
                null,
                SubmoduleUpdateOptions(init = false, recursive = false),
            ),
        )
        assertEquals(
            listOf("submodule", "update"),
            submoduleUpdateArgs(
                null,
                SubmoduleUpdateOptions(init = false, recursive = false, checkout = false),
            ),
        )
        assertEquals(
            listOf("submodule", "sync", "--recursive", "--", "libs/foo"),
            submoduleSyncArgs("libs/foo", recursive = true),
        )
        assertEquals(
            listOf("submodule", "add", "--", "https://example.com/foo.git", "libs/foo"),
            submoduleAddArgs("https://example.com/foo.git", "libs/foo"),
        )
        assertEquals(
            listOf("submodule", "deinit", "-f", "--", "libs/foo"),
            submoduleDeinitArgs("libs/foo"),
        )
        assertEquals(
            listOf("rm", "-f", "--", "libs/foo"),
            submoduleRemoveArgs("libs/foo"),
        )
    }

    @Test
    fun `rejects unsafe relative paths`() {
        assertTrue(validSubmodulePath("libs/foo"))
        assertTrue(validSubmodulePath("libs/foo bar"))
        assertTrue(validSubmodulePath("nested/libs/foo"))

        assertFalse(validSubmodulePath(""))
        assertFalse(validSubmodulePath("-libs/foo"))
        assertFalse(validSubmodulePath("/libs/foo"))
        assertFalse(validSubmodulePath("C:/libs/foo"))
        assertFalse(validSubmodulePath("libs/../foo"))
        assertFalse(validSubmodulePath("libs//foo"))
        assertFalse(validSubmodulePath("libs/foo/"))
        assertFalse(validSubmodulePath("libs\\.git"))
    }

    @Test
    fun `validates submodule URLs`() {
        assertTrue(validSubmoduleUrl("https://example.com/foo.git"))
        assertTrue(validSubmoduleUrl("ssh://git@example.com/foo.git"))
        assertTrue(validSubmoduleUrl("git@example.com:foo.git"))
        assertTrue(validSubmoduleUrl("https://[::1]/foo.git"))
        assertFalse(validSubmoduleUrl(""))
        assertFalse(validSubmoduleUrl("https://::1/foo.git"))
        assertFalse(validSubmoduleUrl("https://2001:db8::1/foo.git"))
        assertFalse(validSubmoduleUrl("https://host:22:33/foo.git"))
        assertFalse(validSubmoduleUrl("-rf"))
        assertFalse(validSubmoduleUrl("http://example.com/foo.git"))
        assertFalse(validSubmoduleUrl("file:///tmp/foo.git"))
        assertFalse(validSubmoduleUrl("ext::sh -c 'touch /tmp/pwned'"))
    }
}
