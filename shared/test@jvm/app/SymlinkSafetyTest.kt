package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.StatusEntry
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SymlinkSafetyTest {

    @Test
    fun `untracked diff does not follow a symlinked file`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-diff").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            val outside = File(root, "outside-diff")
            outside.writeText("outside-secret\n")
            if (!symlink(outside, File(repo, "leak.txt"))) return@runBlocking

            val controller = openController(repo, backend)
            val entry = controller.status!!.entries.single { it.path == "leak.txt" }
            controller.select(entry, staged = false)

            assertEquals(EnglishStrings.repoPathUnsafe, controller.diff)
            assertFalse(controller.diff.contains("outside-secret"))
            assertEquals("outside-secret\n", outside.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `image diff does not read a symlinked file`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-image").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            val outside = File(root, "outside-image")
            outside.writeText("outside-secret\n")
            if (!symlink(outside, File(repo, "leak.png"))) return@runBlocking

            val controller = openController(repo, backend)
            val entry = controller.status!!.entries.single { it.path == "leak.png" }
            controller.select(entry, staged = false)

            assertTrue(controller.diffFileInfo?.image == true)
            assertNull(controller.diffFileInfo?.new?.bytes)
            assertFalse(controller.diff.contains("outside-secret"))
            assertEquals("outside-secret\n", outside.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `merge editor does not read a symlinked conflict file`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-merge-read").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            val entry = createConflict(repo, backend)
            val outside = File(root, "outside-merge-read")
            outside.writeText("outside-secret\n")
            Files.delete(File(repo, "conflict.txt").toPath())
            if (!symlink(outside, File(repo, "conflict.txt"))) return@runBlocking

            // Replace the file before opening: opening may auto-select and cache the editor.
            val controller = openController(repo, backend)
            controller.select(entry, staged = false)

            assertEquals(EnglishStrings.repoPathUnsafe, controller.mergeEditor?.error)
            assertEquals("", controller.mergeEditor?.original)
            assertEquals("outside-secret\n", outside.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `ignore does not follow a symlinked gitignore`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-ignore").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            File(repo, "new.txt").writeText("new\n")
            val outside = File(root, "outside-ignore")
            outside.writeText("outside\n")
            if (!symlink(outside, File(repo, ".gitignore"))) return@runBlocking

            val controller = openController(repo, backend)
            val entry = controller.status!!.entries.single { it.path == "new.txt" }

            assertFalse(controller.ignore(entry))
            assertEquals("outside\n", outside.readText())
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `merge save does not follow a symlinked conflict file`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-merge").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            val entry = createConflict(repo, backend)
            val controller = openController(repo, backend)
            controller.select(entry, staged = false)
            val outside = File(root, "outside-merge")
            outside.writeText("outside\n")
            Files.delete(File(repo, "conflict.txt").toPath())
            if (!symlink(outside, File(repo, "conflict.txt"))) return@runBlocking

            controller.editMergeDraft("attacker\n")
            assertFalse(controller.saveMergeMarkers())
            assertEquals("outside\n", outside.readText())
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `hook save rejects a symlinked hook file`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-hook").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            val outside = File(root, "outside-hook")
            outside.writeText("outside\n")
            if (!symlink(outside, File(repo, ".git/hooks/pre-commit"))) return@runBlocking

            val controller = openController(repo, backend)
            assertFalse(controller.saveHook("pre-commit", "#!/bin/sh\necho attacker\n"))
            assertEquals("outside\n", outside.readText())
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `hooks path rejects a symlink in an intermediate segment`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-hooks-path").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            val outside = File(root, "outside-hooks")
            assertTrue(File(outside, "hooks").mkdirs())
            if (!symlink(outside, File(repo, "custom-hooks"))) return@runBlocking

            val controller = openController(repo, backend)
            assertFalse(
                controller.setHooksPath("custom-hooks/hooks", allowExternal = true),
            )
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `rerere clear unlinks a symlinked cache without deleting its target`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-rerere").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            val outside = File(root, "outside-rerere")
            assertTrue(outside.mkdirs())
            val keep = File(outside, "keep.txt")
            keep.writeText("keep\n")
            val link = File(repo, ".git/rr-cache")
            if (!symlink(outside, link)) return@runBlocking

            val controller = openController(repo, backend)
            assertTrue(controller.clearRerereCache())
            assertFalse(link.exists())
            assertTrue(keep.isFile)
            assertEquals("keep\n", keep.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `rebase workspace refuses a symlinked directory`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-rebase").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            File(repo, "one.txt").writeText("one\n")
            assertTrue(backend.run(repo.path, listOf("add", "one.txt")).ok)
            assertTrue(backend.run(repo.path, listOf("commit", "-m", "one")).ok)

            val outside = File(root, "outside-rebase")
            assertTrue(outside.mkdirs())
            val keep = File(outside, "keep.txt")
            keep.writeText("keep\n")
            val workspace = File(repo, ".git/mybeautifulgit/rebase-todo")
            workspace.parentFile.mkdirs()
            if (!symlink(outside, workspace)) return@runBlocking

            val controller = openController(repo, backend)
            assertNull(controller.loadInteractiveRebasePlan(base = null, root = true))
            assertEquals("keep\n", keep.readText())
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `patch backup refuses a symlinked chan-backups directory`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-symlink-patch").toFile()
        try {
            val backend = CliGitBackend()
            val repo = File(root, "repo")
            assertTrue(repo.mkdirs())
            initRepository(repo, backend)
            File(repo, "tracked.txt").writeText("one\n")
            assertTrue(backend.run(repo.path, listOf("add", "tracked.txt")).ok)
            assertTrue(backend.run(repo.path, listOf("commit", "-m", "base")).ok)
            File(repo, "tracked.txt").writeText("one\ntwo\n")

            val outside = File(root, "outside-backups")
            assertTrue(outside.mkdirs())
            if (!symlink(outside, File(repo, ".git/chan-backups"))) return@runBlocking

            val controller = openController(repo, backend)
            val entry = controller.status!!.entries.single { it.path == "tracked.txt" }
            assertFalse(
                controller.discard(
                    DiscardTarget(entry, DiscardScope.File),
                    DiscardBackup.Patch,
                ),
            )
            assertTrue(outside.listFiles().orEmpty().isEmpty())
            assertEquals("one\ntwo\n", File(repo, "tracked.txt").readText().replace("\r\n", "\n"))
            assertEquals(EnglishStrings.repoPathUnsafe, controller.error)
        } finally {
            root.deleteRecursively()
        }
    }

    private suspend fun openController(repo: File, backend: CliGitBackend): RepoController {
        val repository = backend.open(repo.path)
        return RepoController(repository, backend, backend.version()).also { it.refresh() }
    }

    private fun initRepository(dir: File, backend: CliGitBackend) {
        assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "chan@example.com")).ok)
    }

    private fun createConflict(dir: File, backend: CliGitBackend): StatusEntry {
        initRepository(dir, backend)
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

    private fun symlink(target: File, link: File): Boolean =
        symlink(target.toPath(), link.toPath())

    private fun symlink(target: Path, link: Path): Boolean =
        runCatching { Files.createSymbolicLink(link, target) }.isSuccess
}
