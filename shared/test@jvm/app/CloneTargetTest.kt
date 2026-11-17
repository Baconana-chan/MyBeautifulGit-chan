package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloneTargetTest {

    @Test
    fun `appends a safe name to an existing directory`() {
        val base = Files.createTempDirectory("git-chan-clone-target").toFile()
        try {
            val target = resolveCloneTarget(base.path, "repo")
            assertEquals(File(base, "repo").path, target)
            assertTrue(File(target!!).parentFile.canonicalFile == base.canonicalFile)
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `keeps a future path and rejects traversal or option-like targets`() {
        val base = Files.createTempDirectory("git-chan-clone-future").toFile()
        try {
            val future = File(base, "not-created-yet").path
            assertEquals(future, resolveCloneTarget(future, "ignored"))

            assertNull(resolveCloneTarget("--bare", "repo"))
            assertNull(resolveCloneTarget(File(base, "../outside").path, "repo"))
            assertNull(resolveCloneTarget(File(base, "sub/../outside").path, "repo"))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `rejects repository names with path separators`() {
        val base = Files.createTempDirectory("git-chan-clone-name").toFile()
        try {
            assertNull(resolveCloneTarget(base.path, "../outside"))
            assertNull(resolveCloneTarget(base.path, "sub/repo"))
            assertNull(resolveCloneTarget(base.path, "sub\\repo"))
            assertNull(resolveCloneTarget(base.path, "-repo"))
        } finally {
            base.deleteRecursively()
        }
    }

    @Test
    fun `rejects an existing non-directory target`() {
        val base = Files.createTempDirectory("git-chan-clone-file").toFile()
        val file = File(base, "existing-file")
        try {
            file.writeText("not a directory")
            assertNull(resolveCloneTarget(file.path, "repo"))
        } finally {
            base.deleteRecursively()
        }
    }
}
