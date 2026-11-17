package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.OperationAction
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OperationActionsTest {

    @Test
    fun `continues a resolved merge without opening an editor`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-continue-merge").toFile()
        try {
            val backend = CliGitBackend()
            createMergeConflict(dir, backend)
            File(dir, "conflict.txt").writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)

            val controller = openController(dir, backend)
            assertEquals(GitOperationKind.Merge, controller.operation?.kind)

            assertTrue(controller.operationAction(OperationAction.Continue))
            assertNull(controller.operation)
            assertTrue(controller.status?.isClean == true)
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertTrue(toast.command.contains("merge --continue"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `aborts a merge and restores the worktree`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-abort-merge").toFile()
        try {
            val backend = CliGitBackend()
            createMergeConflict(dir, backend)
            val controller = openController(dir, backend)
            assertEquals(GitOperationKind.Merge, controller.operation?.kind)

            assertTrue(controller.operationAction(OperationAction.Abort))
            assertNull(controller.operation)
            assertTrue(controller.status?.entries?.none { it.isConflicted } == true)
            assertEquals("main", File(dir, "conflict.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `merge skip is rejected without changing the operation`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-skip-merge").toFile()
        try {
            val backend = CliGitBackend()
            createMergeConflict(dir, backend)
            val controller = openController(dir, backend)

            assertFalse(controller.operationAction(OperationAction.Skip))
            assertEquals(GitOperationKind.Merge, controller.operation?.kind)
            assertTrue(controller.error?.contains("--skip") == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `aborts a rebase`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-abort-rebase").toFile()
        try {
            val backend = CliGitBackend()
            createRebaseConflict(dir, backend)
            val controller = openController(dir, backend)
            assertEquals(GitOperationKind.Rebase, controller.operation?.kind)

            assertTrue(controller.operationAction(OperationAction.Abort))
            assertNull(controller.operation)
            assertEquals("side", File(dir, "conflict.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `continues a resolved rebase without an editor`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-continue-rebase").toFile()
        try {
            val backend = CliGitBackend()
            createRebaseConflict(dir, backend)
            File(dir, "conflict.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)

            val controller = openController(dir, backend)
            assertEquals(GitOperationKind.Rebase, controller.operation?.kind)

            assertTrue(controller.operationAction(OperationAction.Continue))
            assertNull(controller.operation)
            assertEquals("side", File(dir, "conflict.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun openController(dir: File, backend: CliGitBackend): RepoController {
        val repository = backend.open(dir.path)
        return RepoController(repository, backend, backend.version()).also { it.refresh() }
    }

    private fun createMergeConflict(dir: File, backend: CliGitBackend) {
        initRepository(dir, backend)
        assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
        File(dir, "conflict.txt").writeText("side\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)

        assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
        File(dir, "conflict.txt").writeText("main\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)

        assertFalse(backend.run(dir.path, listOf("merge", "side")).ok)
    }

    private fun createRebaseConflict(dir: File, backend: CliGitBackend) {
        initRepository(dir, backend)
        assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
        File(dir, "conflict.txt").writeText("side\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)

        assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
        File(dir, "conflict.txt").writeText("main\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)

        assertTrue(backend.run(dir.path, listOf("checkout", "side")).ok)
        assertFalse(backend.run(dir.path, listOf("rebase", "main")).ok)
    }

    private fun initRepository(dir: File, backend: CliGitBackend) {
        assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "chan@example.com")).ok)
        File(dir, "conflict.txt").writeText("base\n")
        assertTrue(backend.run(dir.path, listOf("add", "conflict.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
    }
}
