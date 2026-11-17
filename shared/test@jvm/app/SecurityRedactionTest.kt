package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityRedactionTest {

    @Test
    fun `redacts remote credentials from toast log and hook output`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-redaction")
        try {
            val git = CliGitBackend()
            assertTrue(git.run(dir.toString(), listOf("init", "-b", "main")).ok)
            val controller = RepoController(git.open(dir.toString()), git, git.version())
            controller.refresh()

            assertTrue(
                controller.runCommand(
                    "remote add origin https://user:secret@example.com/team/repo.git",
                ),
                controller.error ?: "remote add failed",
            )
            assertFalse(controller.toast?.command.orEmpty().contains("secret"))
            assertTrue(controller.toast?.command.orEmpty().contains("***@example.com"))

            assertTrue(controller.runCommand("remote -v"), controller.error ?: "remote -v failed")
            assertTrue(controller.log.none { "secret" in it.text })

            val hookOutput = assertNotNull(controller.hookOutput)
            assertFalse(hookOutput.text.contains("secret"))
            assertTrue(hookOutput.text.contains("***@example.com"))
            assertFalse(hookOutput.command.contains("secret"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}