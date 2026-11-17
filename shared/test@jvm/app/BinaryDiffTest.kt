package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BinaryDiffTest {

    @Test
    fun `reads blobs with a hard size limit`() {
        val dir = Files.createTempDirectory("git-chan-blob").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val bytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
            File(dir, "sample.bin").writeBytes(bytes)
            assertTrue(backend.run(dir.path, listOf("add", "sample.bin")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "binary")).ok)
            val repository = backend.open(dir.path)

            val head = backend.readBlob(repository, "HEAD:sample.bin", 1024)
            assertEquals(8L, head.size)
            assertContentEquals(bytes, head.bytes)

            val limited = backend.readBlob(repository, "HEAD:sample.bin", 4)
            assertEquals(8L, limited.size)
            assertTrue(limited.tooLarge)
            assertEquals(null, limited.bytes)

            val missing = backend.readBlob(repository, "HEAD:absent.bin", 1024)
            assertTrue(missing.missing)
            assertEquals(null, missing.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `shows binary metadata without attempting a text diff`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-binary").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val before = byteArrayOf(0, 1, 2, 3, 4, 5)
            File(dir, "archive.bin").writeBytes(before)
            assertTrue(backend.run(dir.path, listOf("add", "archive.bin")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "binary")).ok)
            File(dir, "archive.bin").writeBytes(byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9, 9))

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "archive.bin" }
            controller.select(entry, staged = false)

            val info = assertNotNull(controller.diffFileInfo)
            assertTrue(info.binary)
            assertFalse(info.image)
            assertEquals(6L, info.old.size)
            assertEquals(9L, info.new.size)
            assertEquals("index:archive.bin", info.old.label)
            assertEquals("worktree:archive.bin", info.new.label)
            assertFalse(info.tooLarge)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads old and new bytes for image diff`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-image").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val before = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)
            val after = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 9, 8, 7, 6)
            File(dir, "hero.png").writeBytes(before)
            assertTrue(backend.run(dir.path, listOf("add", "hero.png")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "image")).ok)
            File(dir, "hero.png").writeBytes(after)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "hero.png" }
            controller.select(entry, staged = false)

            val info = assertNotNull(controller.diffFileInfo)
            assertTrue(info.image)
            assertTrue(info.binary)
            assertContentEquals(before, info.old.bytes)
            assertContentEquals(after, info.new.bytes)
            assertEquals(7L, info.old.size)
            assertEquals(8L, info.new.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `warns instead of loading a large text file`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-large").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val large = ByteArray(3 * 1024 * 1024) { 'a'.code.toByte() }
            File(dir, "large.txt").writeBytes(large)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "large.txt" }
            controller.select(entry, staged = false)

            val info = assertNotNull(controller.diffFileInfo)
            assertTrue(info.tooLarge)
            assertFalse(info.image)
            assertFalse(info.binary)
            assertEquals(large.size.toLong(), info.new.size)
            assertEquals(2L * 1024 * 1024, info.limitBytes)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun initRepository(dir: File, backend: CliGitBackend) {
        assertTrue(backend.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "chan@example.com")).ok)
        File(dir, "seed.txt").writeText("seed\n")
        assertTrue(backend.run(dir.path, listOf("add", "seed.txt")).ok)
        assertTrue(backend.run(dir.path, listOf("commit", "-m", "seed")).ok)
    }
}
