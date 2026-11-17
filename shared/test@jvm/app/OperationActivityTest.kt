package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.baconana.mybeautifulgit_chan.git.GitBackend
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitRunControl
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OperationActivityTest {

    @Test
    fun `reports progress and cancels a foreground operation`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-operation-progress").toFile()
        try {
            val cli = CliGitBackend()
            initRepository(dir, cli)
            val backend = ControlledBackend(cli).apply {
                progress = "remote: Receiving objects:  42% (42/100), 1.2 MiB\r"
            }
            val controller = RepoController(cli.open(dir.path), backend, cli.version())

            val job = async(Dispatchers.Default) { controller.runCommand("fetch origin") }
            withTimeout(5_000) {
                while (!backend.started) delay(10)
            }
            withTimeout(5_000) {
                while (controller.operationActivity?.progress?.percent != 42) delay(10)
            }
            val running = assertNotNull(controller.operationActivity)
            assertTrue(running.running)
            assertEquals("Receiving objects", running.progress?.stage)

            controller.cancelOperationActivity()
            assertTrue(controller.operationActivity?.cancelling == true)
            assertFalse(job.await())

            val finished = assertNotNull(controller.operationActivity)
            assertEquals(OperationOutcome.Cancelled, finished.outcome)
            assertNotNull(finished.durationMs)
            assertTrue(finished.completedAtMs != null)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `cancels only the selected background command`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-background-cancel").toFile()
        try {
            val cli = CliGitBackend()
            initRepository(dir, cli)
            val backend = ControlledBackend(cli)
            val controller = RepoController(cli.open(dir.path), backend, cli.version())

            val job = async(Dispatchers.Default) { controller.runCommandInBackground("fetch origin") }
            withTimeout(5_000) {
                while (controller.backgroundCommands.none {
                        it.state == BackgroundCommandState.Running
                    }
                ) {
                    delay(10)
                }
            }
            val running = controller.backgroundCommands.single {
                it.state == BackgroundCommandState.Running
            }
            controller.cancelBackgroundCommand(running.id)
            assertTrue(controller.backgroundCommands.single().cancelling)
            assertFalse(job.await())

            val finished = controller.backgroundCommands.single()
            assertEquals(BackgroundCommandState.Cancelled, finished.state)
            assertFalse(finished.cancelling)
        } finally {
            dir.deleteRecursively()
        }
    }

    private class ControlledBackend(
        private val delegate: GitBackend,
    ) : GitBackend by delegate {
        @Volatile
        var started = false

        @Volatile
        var progress: String? = null

        override fun runControlled(
            cwd: String?,
            args: List<String>,
            stdin: String?,
            timeoutSeconds: Long,
            environment: Map<String, String>,
            control: GitRunControl,
        ): GitResult {
            started = true
            progress?.let(control::reportProgress)
            val start = System.nanoTime()
            while (!control.isCancelled() && System.nanoTime() - start < 5_000_000_000L) {
                Thread.sleep(15)
            }
            return GitResult(
                args = args,
                cwd = cwd,
                exitCode = -1,
                stdout = "",
                stderr = "",
                durationMs = (System.nanoTime() - start) / 1_000_000,
                cancelled = control.isCancelled(),
            )
        }
    }

    private fun initRepository(dir: File, backend: CliGitBackend) {
        val init = backend.run(dir.path, listOf("init", "-b", "main"))
        assertTrue(init.ok, init.stderr)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
    }
}