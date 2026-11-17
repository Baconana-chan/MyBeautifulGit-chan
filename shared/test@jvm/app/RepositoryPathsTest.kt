package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepositoryPathsTest {

    @Test
    fun `contains paths and rejects traversal and absolute input`() {
        val root = Files.createTempDirectory("git-chan-paths").toFile()
        try {
            assertFailsWith<RepositoryPathException> {
                RepositoryPaths.resolve(root, "../outside")
            }
            assertFailsWith<RepositoryPathException> {
                RepositoryPaths.resolve(root, "/outside")
            }
            assertFailsWith<RepositoryPathException> {
                RepositoryPaths.resolve(root, "nested/../../outside")
            }

            RepositoryPaths.createDirectories(root, "nested")
            RepositoryPaths.writeText(root, "nested/file.txt", "safe\n")
            assertEquals("safe\n", RepositoryPaths.readText(root, "nested/file.txt"))
            val limited = RepositoryPaths.readTextLimited(root, "nested/file.txt", 2)
            assertTrue(limited.truncated)
            assertEquals(2, limited.text.length)
            assertTrue(RepositoryPaths.exists(root, "nested/file.txt"))
            assertFalse(RepositoryPaths.exists(root, "missing.txt"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `delete tree removes links without following them`() {
        val root = Files.createTempDirectory("git-chan-delete").toFile()
        try {
            val tree = File(root, "tree")
            val outside = File(root, "outside")
            assertTrue(tree.mkdirs())
            assertTrue(outside.mkdirs())
            val victim = File(outside, "keep.txt")
            victim.writeText("keep\n")
            if (!symlink(outside, File(tree, "link"))) return

            assertTrue(RepositoryPaths.deleteTree(root, "tree"))
            assertFalse(tree.exists())
            assertTrue(victim.isFile)
            assertEquals("keep\n", victim.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `final symlink can be unlinked only where the caller allows it`() {
        val root = Files.createTempDirectory("git-chan-final-link").toFile()
        try {
            val outside = File(root, "outside")
            assertTrue(outside.mkdirs())
            val victim = File(outside, "keep.txt")
            victim.writeText("keep\n")
            val link = File(root, "cache")
            if (!symlink(outside, link)) return

            assertFailsWith<RepositoryPathException> {
                RepositoryPaths.deleteTree(root, "cache")
            }
            assertTrue(victim.isFile)
            assertTrue(RepositoryPaths.deleteTree(root, "cache", allowFinalSymlink = true))
            assertFalse(link.exists())
            assertTrue(victim.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `external hook write rejects a symlink target`() {
        val root = Files.createTempDirectory("git-chan-external-link").toFile()
        try {
            val outside = File(root, "outside.txt")
            outside.writeText("outside\n")
            val link = File(root, "hook")
            if (!symlink(outside.toPath(), link.toPath())) return

            assertFailsWith<RepositoryPathException> {
                RepositoryPaths.writeExternalFile(link, "attacker\n")
            }
            assertEquals("outside\n", outside.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun symlink(target: File, link: File): Boolean =
        symlink(target.toPath(), link.toPath())

    private fun symlink(target: Path, link: Path): Boolean =
        runCatching { Files.createSymbolicLink(link, target) }.isSuccess
}
