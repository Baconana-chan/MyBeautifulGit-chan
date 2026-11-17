package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OperationJournalTest {

    @Test
    fun `undoes and redoes a commit through the journal`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-journal-commit").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = head(dir, backend)

            val controller = openController(dir, backend)
            File(dir, "a.txt").writeText("two\n")
            controller.refresh()
            val entry = assertNotNull(controller.status).entries.single { it.path == "a.txt" }
            controller.stage(entry)
            controller.commitMessage = "second"
            assertTrue(controller.commit(), controller.error ?: "commit failed")
            val committed = head(dir, backend)
            assertTrue(committed != base)

            val journalEntry = assertNotNull(controller.lastJournalEntry)
            assertTrue(journalEntry.action is JournalAction.CommitReset)
            assertTrue(controller.canUndoJournal)
            assertTrue(controller.undoJournal(), controller.error ?: "journal undo failed")
            assertEquals(base, head(dir, backend))
            assertEquals("two", File(dir, "a.txt").readText().trim())
            assertTrue(controller.canRedoJournal)

            assertTrue(controller.redoJournal(), controller.error ?: "journal redo failed")
            assertEquals(committed, head(dir, backend))
            assertTrue(controller.status?.isClean == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `undoes and redoes checkout through the journal`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-journal-checkout").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = head(dir, backend)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            val tip = head(dir, backend)

            val controller = openController(dir, backend)
            assertTrue(controller.checkoutCommit(base), controller.error ?: "checkout failed")
            assertEquals(base, head(dir, backend))
            assertTrue(controller.canUndoJournal)
            assertTrue(controller.undoJournal(), controller.error ?: "journal undo failed")
            assertEquals(tip, head(dir, backend))
            assertEquals(
                "main",
                backend.run(dir.path, listOf("symbolic-ref", "--short", "HEAD")).stdout.trim(),
            )
            assertTrue(controller.canRedoJournal)

            assertTrue(controller.redoJournal(), controller.error ?: "journal redo failed")
            assertEquals(base, head(dir, backend))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `undoes and redoes a reset through the journal backup ref`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-journal-reset").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = head(dir, backend)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            val tip = head(dir, backend)

            val controller = openController(dir, backend)
            assertTrue(controller.resetCommit(base, ResetMode.Hard), controller.error ?: "reset failed")
            assertEquals(base, head(dir, backend))
            assertTrue(controller.lastJournalEntry?.action is JournalAction.Rewrite)
            assertTrue(controller.canUndoJournal)

            assertTrue(controller.undoJournal(), controller.error ?: "journal undo failed")
            assertEquals(tip, head(dir, backend))
            assertTrue(controller.canRedoJournal)
            assertTrue(controller.redoJournal(), controller.error ?: "journal redo failed")
            assertEquals(base, head(dir, backend))
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun openController(dir: File, backend: CliGitBackend): RepoController {
        val repository = backend.open(dir.path)
        return RepoController(repository, backend, backend.version()).also { it.refresh() }
    }

    private fun head(dir: File, backend: CliGitBackend): String =
        backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

    private fun initRepository(dir: File, backend: CliGitBackend) {
        val init = backend.run(dir.path, listOf("init", "-b", "main"))
        assertTrue(init.ok, init.stderr)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
    }
}