package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RawCommandRunnerTest {

    @Test
    fun `background command completes without leaving the controller busy`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-background-command").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            val controller = RepoController(git.open(dir.path), git, git.version())

            val completed = async {
                controller.runCommandInBackground("git status --short")
            }

            assertTrue(completed.await(), controller.error ?: "background command failed")
            val job = controller.backgroundCommands.single()
            assertEquals(BackgroundCommandState.Succeeded, job.state)
            assertEquals(0, job.exitCode)
            assertTrue(job.durationMs != null)
            assertTrue(
                controller.log.any {
                    it.kind == LogKind.Info && it.text.contains("started in background")
                },
            )
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `failed background command keeps a failed job and error`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-background-failure").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            val controller = RepoController(git.open(dir.path), git, git.version())

            assertFalse(controller.runCommandInBackground("definitely-not-a-git-command"))
            val job = controller.backgroundCommands.single()
            assertEquals(BackgroundCommandState.Failed, job.state)
            assertNotNull(controller.error)
            assertEquals(false, controller.toast?.success)
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `invalid command does not start a background job`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-background-invalid").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            val controller = RepoController(git.open(dir.path), git, git.version())

            assertFalse(controller.runCommandInBackground("status \"unterminated"))
            assertTrue(controller.backgroundCommands.isEmpty())
            assertEquals(EnglishStrings.repoCommandUnclosedQuote, controller.error)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun initRepository(dir: File, git: CliGitBackend) {
        assertTrue(git.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
    }
}
