package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.ArchiveFormat
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvancedRepositoryTest {

    @Test
    fun `runs storage checks and explicit prune preview`() = runBlocking {
        val dir = newRepository("git-chan-advanced-storage")
        try {
            val controller = controller(dir)
            controller.countObjects()
            assertTrue(assertNotNull(controller.advancedResult).ok)
            assertTrue(controller.advancedResult!!.command.contains("count-objects"))

            controller.fsckRepository()
            assertTrue(assertNotNull(controller.advancedResult).ok)

            controller.gcRepository()
            assertTrue(assertNotNull(controller.advancedResult).ok)
            controller.repackRepository()
            assertTrue(assertNotNull(controller.advancedResult).ok)

            assertTrue(controller.previewPrune())
            assertNotNull(controller.prunePreview)
            assertTrue(controller.pruneRepository())
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `applies and disables sparse checkout`() = runBlocking {
        val dir = newRepository("git-chan-advanced-sparse")
        try {
            File(dir, "src").mkdirs()
            File(dir, "docs").mkdirs()
            File(dir, "src/a.txt").writeText("a\n")
            File(dir, "docs/b.txt").writeText("b\n")
            val git = CliGitBackend()
            assertTrue(git.run(dir.path, listOf("add", ".")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "layout")).ok)

            val controller = controller(dir)
            controller.loadSparseState()
            assertFalse(controller.sparseState.enabled)

            assertTrue(controller.applySparseCheckout("src", cone = true))
            assertTrue(controller.sparseState.enabled)
            assertEquals(listOf("src"), controller.sparseState.patterns)
            assertTrue(File(dir, "src/a.txt").isFile)
            assertFalse(File(dir, "docs/b.txt").exists())

            assertTrue(controller.disableSparseCheckout())
            assertFalse(controller.sparseState.enabled)
            assertTrue(File(dir, "docs/b.txt").isFile)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `adds appends shows and removes notes`() = runBlocking {
        val dir = newRepository("git-chan-advanced-notes")
        try {
            val controller = controller(dir)
            controller.loadNotes()
            assertTrue(controller.gitNotes.isEmpty())

            assertTrue(controller.saveNote("HEAD", "first note", append = false))
            controller.loadNotes()
            assertEquals(1, controller.gitNotes.size)
            assertTrue(controller.showNote("HEAD"))
            assertTrue(controller.gitNoteText.contains("first note"))

            assertTrue(controller.saveNote("HEAD", "second paragraph", append = true))
            assertTrue(controller.showNote("HEAD"))
            assertTrue(controller.gitNoteText.contains("first note"))
            assertTrue(controller.gitNoteText.contains("second paragraph"))

            assertTrue(controller.removeNote("HEAD"))
            controller.loadNotes()
            assertTrue(controller.gitNotes.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `creates archive and bundle and verifies bundle`() = runBlocking {
        val dir = newRepository("git-chan-advanced-export")
        try {
            val controller = controller(dir)
            val archives = File(dir, "exports").apply { mkdirs() }
            val archive = File(archives, "head.zip")
            assertTrue(controller.createArchive("HEAD", ArchiveFormat.Zip, archive.path))
            assertTrue(archive.isFile)
            assertTrue(assertNotNull(controller.advancedResult).note?.contains("head.zip") == true)

            val bundle = File(archives, "repo.bundle")
            assertTrue(controller.createBundle(bundle.path))
            assertTrue(bundle.isFile)
            assertTrue(controller.verifyBundle(bundle.path))
            assertTrue(controller.listBundleHeads(bundle.path))
            assertTrue(assertNotNull(controller.advancedResult).ok)

            assertFalse(controller.createBundle(bundle.path))
            assertTrue(controller.error?.contains("already exists") == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `installs and tracks lfs when the extension is available`() = runBlocking {
        val dir = newRepository("git-chan-advanced-lfs")
        try {
            val git = CliGitBackend()
            if (!git.run(dir.path, listOf("lfs", "version")).ok) return@runBlocking
            val controller = controller(dir)

            assertTrue(controller.installLfs())
            assertTrue(controller.trackLfs("*.bin"))
            assertTrue(File(dir, ".gitattributes").readText().contains("*.bin"))
            controller.loadLfsState()
            assertTrue(controller.lfsState.available)
            assertTrue(controller.lfsState.version.contains("git-lfs"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `detects partial clone promisor configuration`() = runBlocking {
        val dir = newRepository("git-chan-advanced-partial")
        try {
            val git = CliGitBackend()
            assertTrue(git.run(dir.path, listOf("config", "extensions.partialClone", "origin")).ok)
            val controller = controller(dir)
            controller.loadPartialCloneState()
            assertTrue(controller.partialCloneState.enabled)
            assertTrue(
                controller.partialCloneState.details.any {
                    it.contains("extensions.partialclone", ignoreCase = true)
                },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reports fsmonitor config without requiring platform support`() = runBlocking {
        val dir = newRepository("git-chan-advanced-fsmonitor")
        try {
            val controller = controller(dir)
            controller.loadFsmonitorConfig()
            assertTrue(controller.fsmonitorConfig.isNotEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun controller(dir: File): RepoController {
        val git = CliGitBackend()
        val controller = RepoController(git.open(dir.path), git, git.version())
        controller.refresh()
        return controller
    }

    private fun newRepository(prefix: String): File {
        val dir = Files.createTempDirectory(prefix).toFile()
        val git = CliGitBackend()
        assertTrue(git.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(git.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
        File(dir, "file.txt").writeText("hello\n")
        assertTrue(git.run(dir.path, listOf("add", "file.txt")).ok)
        assertTrue(git.run(dir.path, listOf("commit", "-m", "initial")).ok)
        return dir
    }
}
