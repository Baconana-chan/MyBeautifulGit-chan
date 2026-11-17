package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.Repository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryRuntimeProbeTest {

    @Test
    fun `detects single operation markers`() {
        withRepository { _, gitDir, repository ->
            val markers = linkedMapOf(
                "MERGE_HEAD" to GitOperationKind.Merge,
                "CHERRY_PICK_HEAD" to GitOperationKind.CherryPick,
                "REVERT_HEAD" to GitOperationKind.Revert,
                "BISECT_LOG" to GitOperationKind.Bisect,
                "BISECT_START" to GitOperationKind.Bisect,
            )
            markers.forEach { (name, kind) ->
                Files.writeString(gitDir.resolve(name), "marker\n")
                assertEquals(kind, RepositoryRuntimeProbe.read(repository).operation?.kind)
                Files.delete(gitDir.resolve(name))
            }
        }
    }

    @Test
    fun `rebase marker wins over cherry-pick marker and exposes progress`() {
        withRepository { _, gitDir, repository ->
            val rebase = Files.createDirectories(gitDir.resolve("rebase-merge"))
            Files.writeString(rebase.resolve("msgnum"), "2\n")
            Files.writeString(rebase.resolve("end"), "5\n")
            Files.writeString(gitDir.resolve("CHERRY_PICK_HEAD"), "marker\n")

            val operation = assertNotNull(RepositoryRuntimeProbe.read(repository).operation)

            assertEquals(GitOperationKind.Rebase, operation.kind)
            assertEquals("2/5", operation.progress)
        }
    }

    @Test
    fun `distinguishes apply rebase from am`() {
        withRepository { _, gitDir, repository ->
            val apply = Files.createDirectories(gitDir.resolve("rebase-apply"))
            Files.writeString(apply.resolve("next"), "1\n")
            Files.writeString(apply.resolve("last"), "3\n")
            Files.writeString(apply.resolve("rebasing"), "")

            val rebase = assertNotNull(RepositoryRuntimeProbe.read(repository).operation)
            assertEquals(GitOperationKind.Rebase, rebase.kind)
            assertEquals("1/3", rebase.progress)

            Files.delete(apply.resolve("rebasing"))
            Files.writeString(apply.resolve("applying"), "")

            val am = assertNotNull(RepositoryRuntimeProbe.read(repository).operation)
            assertEquals(GitOperationKind.Am, am.kind)
            assertEquals("1/3", am.progress)
        }
    }

    @Test
    fun `detects and removes index lock`() {
        withRepository { _, gitDir, repository ->
            val lock = gitDir.resolve("index.lock")
            Files.writeString(lock, "")

            val runtime = RepositoryRuntimeProbe.read(repository)
            val indexLock = assertNotNull(runtime.indexLock)
            assertEquals(lock.toString(), indexLock.path)

            assertTrue(RepositoryRuntimeProbe.deleteIndexLock(repository))
            assertFalse(Files.exists(lock))
            assertNull(RepositoryRuntimeProbe.read(repository).indexLock)
        }
    }

    @Test
    fun `bare repository has no index lock`() {
        withRepository { root, gitDir, _ ->
            Files.writeString(gitDir.resolve("index.lock"), "")
            val bare = Repository(
                root = root.toString(),
                gitDir = gitDir.toString(),
                commonDir = gitDir.toString(),
                name = "bare",
                isBare = true,
                isWorktree = false,
                workTree = null,
            )

            assertNull(RepositoryRuntimeProbe.read(bare).indexLock)
            assertFalse(RepositoryRuntimeProbe.deleteIndexLock(bare))
        }
    }

    private fun withRepository(block: (Path, Path, Repository) -> Unit) {
        val root = Files.createTempDirectory("git-chan-runtime").toFile()
        val gitDir = Files.createDirectories(Path.of(root.path, ".git"))
        val repository = Repository(
            root = root.path,
            gitDir = gitDir.toString(),
            commonDir = gitDir.toString(),
            name = "repo",
            isBare = false,
            isWorktree = false,
            workTree = root.path,
        )
        try {
            block(Path.of(root.path), gitDir, repository)
        } finally {
            root.deleteRecursively()
        }
    }
}
