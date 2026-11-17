package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepositoryTabsTest {

    @Test
    fun `repository roots compare without a trailing separator`() {
        val root = Files.createTempDirectory("git-chan-tabs").toFile().canonicalPath

        assertTrue(sameRepositoryRoot(root, root + File.separator))
        assertTrue(sameRepositoryRoot(root, "  $root  "))
        assertFalse(sameRepositoryRoot(root, root + "-other"))
        assertFalse(sameRepositoryRoot(null, root))
    }
}