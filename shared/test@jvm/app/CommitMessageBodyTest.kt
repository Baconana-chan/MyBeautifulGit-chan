package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend

class CommitMessageBodyTest {

    @Test
    fun `commits a message with subject and body`() = runBlocking {
        withRepository { _, backend, controller ->
            controller.commitMessage = "Add body support\n\nKeep the second line\nand the third."
            assertTrue(controller.commit())

            val commit = controller.commits.first()
            assertEquals("Add body support", commit.subject)
            assertEquals(
                "Add body support\n\nKeep the second line\nand the third.",
                backend.commitMessage(controller.repository, commit.hash).trim(),
            )
        }
    }

    @Test
    fun `edited template body is committed unchanged`() = runBlocking {
        withRepository { dir, backend, controller ->
            File(dir, "template.txt").writeText("Template subject\n\nBody\n# dropped comment\n")
            controller.commitTemplatePath = "template.txt"
            assertTrue(controller.loadCommitTemplate())

            controller.commitMessage = controller.commitMessage.replace("Body", "Edited body")
            assertTrue(controller.commit())

            assertEquals(
                "Template subject\n\nEdited body",
                backend.commitMessage(controller.repository, controller.commits.first().hash).trim(),
            )
        }
    }

    private suspend fun withRepository(
        block: suspend (File, CliGitBackend, RepoController) -> Unit,
    ) {
        val dir = Files.createTempDirectory("git-chan-message").toFile()
        try {
            val backend = CliGitBackend()
            assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            block(dir, backend, controller)
        } finally {
            dir.deleteRecursively()
        }
    }
}
