package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RerereTest {

    @Test
    fun `lists details enables and clears recorded resolutions`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rerere").toFile()
        try {
            val backend = CliGitBackend()
            assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "user.name", "Chan Test")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "user.email", "chan@example.com")).ok)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            val cache = File(File(dir, ".git"), "rr-cache")
            val entry = File(cache, "0123456789abcdef")
            assertTrue(entry.mkdirs())
            File(entry, "preimage").writeText("<<<<<<< HEAD\nours\n=======\ntheirs\n>>>>>>> side\n")
            File(entry, "postimage").writeText("resolved\n")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertFalse(controller.rerere?.enabled ?: true)
            val listed = assertNotNull(controller.rerere?.entries?.singleOrNull())
            assertEquals(entry.name, listed.hash)
            assertTrue(listed.preimageBytes > 0)
            assertTrue(listed.postimageBytes > 0)

            controller.selectRerere(entry.name)
            val detail = assertNotNull(controller.rerereDetail)
            assertEquals("resolved\n", detail.postimage)
            assertTrue(detail.preimage?.contains("<<<<<<< HEAD") == true)

            assertTrue(controller.setRerereEnabled(true))
            assertTrue(controller.rerere?.enabled == true)
            assertEquals(
                "true",
                backend.run(dir.path, listOf("config", "--bool", "--get", "rerere.enabled")).stdout.trim(),
            )

            assertTrue(controller.clearRerereCache())
            assertFalse(cache.exists())
            assertTrue(controller.rerere?.entries?.isEmpty() == true)
            assertNull(controller.rerereDetail)

            assertTrue(controller.setRerereEnabled(false))
            assertFalse(controller.rerere?.enabled ?: true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `empty cache lists nothing and clear is harmless`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rerere-empty").toFile()
        try {
            val backend = CliGitBackend()
            assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertTrue(controller.rerere?.entries?.isEmpty() == true)
            assertFalse(File(File(dir, ".git"), "rr-cache").exists())
            assertTrue(controller.clearRerereCache())
            assertTrue(controller.rerere?.entries?.isEmpty() == true)
        } finally {
            dir.deleteRecursively()
        }
    }
}
