package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectoryBrowserTest {

    @Test
    fun `lists directories hides dotfiles and puts repositories first`() {
        val dir = Files.createTempDirectory("git-chan-browser").toFile()
        try {
            File(dir, "alpha").mkdirs()
            File(dir, ".hidden").mkdirs()
            File(dir, "repoc").mkdirs()
            File(dir, "repoc/.git").mkdirs()
            File(dir, "barec").mkdirs()
            File(dir, "barec/HEAD").writeText("ref: refs/heads/main\n")
            File(dir, "barec/objects").mkdirs()
            File(dir, "barec/refs").mkdirs()

            val listing = DirectoryBrowser.list(dir.path)

            assertEquals(dir.canonicalPath, listing.directory)
            assertNull(listing.repositoryRoot)
            assertEquals(listOf("barec", "repoc", "alpha"), listing.entries.map { it.name })
            assertTrue(listing.entries.first { it.name == "barec" }.isBareRepository)
            assertTrue(listing.entries.first { it.name == "repoc" }.isRepository)
            assertTrue(listing.entries.none { it.name == ".hidden" })

            val withHidden = DirectoryBrowser.list(dir.path, showHidden = true)
            assertTrue(withHidden.entries.any { it.name == ".hidden" })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `finds repository root from a nested directory`() {
        val dir = Files.createTempDirectory("git-chan-browser").toFile()
        try {
            val repo = File(dir, "project").apply { mkdirs() }
            File(repo, ".git").mkdirs()
            val nested = File(repo, "src/main").apply { mkdirs() }

            val listing = DirectoryBrowser.list(nested.path)

            assertEquals(repo.canonicalPath, assertNotNull(listing.repositoryRoot))
            assertEquals(File(repo, "src").canonicalPath, listing.parent)
            assertTrue(listing.entries.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `missing path is reported instead of silently listing its parent`() {
        val dir = Files.createTempDirectory("git-chan-browser").toFile()
        try {
            val missing = File(dir, "missing").path

            val listing = DirectoryBrowser.list(missing)

            assertNotNull(listing.error)
            assertTrue(listing.entries.isEmpty())
            assertEquals(dir.canonicalPath, listing.parent)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `file path resolves to its parent directory`() {
        val dir = Files.createTempDirectory("git-chan-browser").toFile()
        try {
            val file = File(dir, "note.txt").apply { writeText("hello") }

            val listing = DirectoryBrowser.list(file.path)

            assertEquals(dir.canonicalPath, listing.directory)
            assertNull(listing.error)
            assertTrue(listing.entries.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
