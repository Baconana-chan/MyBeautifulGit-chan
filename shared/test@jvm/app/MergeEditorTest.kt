package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.MergeSide
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MergeEditorTest {

    @Test
    fun `loads base ours and theirs from index stages`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-merge-editor").toFile()
        try {
            val backend = CliGitBackend()
            val entry = createConflict(dir, backend)
            val controller = openController(dir, backend)
            controller.select(entry, staged = false)

            val state = assertNotNull(controller.mergeEditor)
            assertEquals("base\n", state.base)
            assertEquals("main\n", state.ours)
            assertEquals("side\n", state.theirs)
            assertTrue(controller.mergeDraft.contains("<<<<<<<"))
            assertEquals(1, controller.mergeConflicts.size)
            assertFalse(controller.mergeDirty)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `resolves one line, saves and stages the conflict`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-merge-line").toFile()
        try {
            val backend = CliGitBackend()
            val entry = createConflict(dir, backend)
            val controller = openController(dir, backend)
            controller.select(entry, staged = false)
            val conflict = controller.mergeConflicts.single()

            controller.toggleMergeLine(conflict.id, MergeSide.Ours, 0)
            assertTrue(controller.applyMergeConflict(conflict.id))
            assertFalse(controller.mergeDraft.contains("<<<<<<<"))
            assertEquals("main", controller.mergeDraft.trimEnd('\r', '\n'))
            assertTrue(controller.mergeDirty)

            assertTrue(controller.saveMergeMarkers())
            assertEquals("main", File(dir, "conflict.txt").readText().trimEnd('\r', '\n'))
            assertFalse(controller.mergeDirty)
            assertEquals(1, controller.status?.conflictedCount)

            val current = controller.status!!.entries.single { it.path == "conflict.txt" }
            controller.stage(current)
            assertEquals(0, controller.status?.conflictedCount)
            assertEquals(null, controller.mergeEditor)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `takes all theirs and preserves context outside the block`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-merge-all").toFile()
        try {
            val backend = CliGitBackend()
            val entry = createConflict(dir, backend)
            val controller = openController(dir, backend)
            controller.select(entry, staged = false)

            assertTrue(controller.takeAllMergeSide(MergeSide.Theirs))
            assertEquals("side", controller.mergeDraft.trimEnd('\r', '\n'))
            assertTrue(controller.saveMergeMarkers())
            assertEquals("side", File(dir, "conflict.txt").readText().trimEnd('\r', '\n'))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `manual editing and marker saving keep the exact draft`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-merge-manual").toFile()
        try {
            val backend = CliGitBackend()
            val entry = createConflict(dir, backend)
            val controller = openController(dir, backend)
            controller.select(entry, staged = false)
            val markers = controller.mergeDraft

            controller.editMergeDraft("manual resolution\n")
            assertTrue(controller.saveMergeMarkers())
            assertEquals("manual resolution\n", File(dir, "conflict.txt").readText())
            assertFalse(controller.mergeDirty)

            controller.editMergeDraft(markers)
            assertTrue(controller.saveMergeMarkers())
            assertEquals(markers, File(dir, "conflict.txt").readText())
            assertEquals(1, controller.status?.conflictedCount)
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun openController(dir: File, backend: CliGitBackend): RepoController {
        val repository = backend.open(dir.path)
        return RepoController(repository, backend, backend.version()).also { it.refresh() }
    }

    private fun createConflict(dir: File, backend: CliGitBackend): org.baconana.mybeautifulgit_chan.domain.StatusEntry {
        assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "chan@example.com")).ok)
        File(dir, "conflict.txt").writeText("base\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

        assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
        File(dir, "conflict.txt").writeText("side\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)

        assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
        File(dir, "conflict.txt").writeText("main\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)

        assertFalse(backend.run(dir.path, listOf("merge", "side")).ok)
        val repository = backend.open(dir.path)
        val status = backend.status(repository)
        return status.entries.single { it.path == "conflict.txt" && it.isConflicted }
    }
}
