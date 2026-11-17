package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancedTest {

    @Test
    fun `builds advanced commands without shell interpolation`() {
        assertEquals(
            listOf("archive", "--format=zip", "--output=/tmp/repo.zip", "HEAD"),
            archiveArgs("HEAD", ArchiveFormat.Zip, "/tmp/repo.zip"),
        )
        assertEquals(
            listOf("bundle", "create", "/tmp/repo.bundle", "--all"),
            bundleCreateArgs("/tmp/repo.bundle"),
        )
        assertEquals(
            listOf("bundle", "verify", "/tmp/repo.bundle"),
            bundleVerifyArgs("/tmp/repo.bundle"),
        )
        assertEquals(
            listOf("bundle", "list-heads", "/tmp/repo.bundle"),
            bundleListHeadsArgs("/tmp/repo.bundle"),
        )
        assertEquals(
            listOf("notes", "add", "-f", "-m", "hello", "abc"),
            notesAddArgs("abc", "hello", append = false),
        )
        assertEquals(
            listOf("notes", "append", "-m", "hello", "abc"),
            notesAddArgs("abc", "hello", append = true),
        )
        assertEquals(
            listOf("notes", "remove", "abc"),
            notesRemoveArgs("abc"),
        )
    }

    @Test
    fun `parses notes and rejects malformed entries`() {
        val note = "a".repeat(40)
        val objectId = "b".repeat(40)
        val entries = parseNotesList(
            "$note $objectId\n" +
                "garbage\n" +
                "${"c".repeat(40)} not-an-object\n",
        )
        assertEquals(1, entries.size)
        assertEquals(objectId, entries.single().objectHash)
        assertEquals(note, entries.single().noteHash)
        assertTrue(isObjectId(objectId))
        assertFalse(isObjectId("abc"))
    }

    @Test
    fun `validates sparse paths and LFS patterns`() {
        assertTrue(validSparsePath("src", cone = true))
        assertTrue(validSparsePath("shared/src@jvm", cone = true))
        assertTrue(validSparsePath("!docs", cone = false))
        assertFalse(validSparsePath("../outside", cone = false))
        assertFalse(validSparsePath("/absolute", cone = true))
        assertFalse(validSparsePath("src/*.kt", cone = true))
        assertFalse(validSparsePath("C:\\repo", cone = true))
        assertFalse(validSparsePath("-x", cone = false))

        assertTrue(validLfsPattern("*.psd"))
        assertTrue(validLfsPattern("assets/**/*.bin"))
        assertFalse(validLfsPattern(""))
        assertFalse(validLfsPattern("--force"))
        assertFalse(validLfsPattern("bad\npattern"))
    }

    @Test
    fun `rejects unsafe advanced revisions`() {
        assertTrue(validAdvancedRef("HEAD"))
        assertTrue(validAdvancedRef("refs/tags/v1"))
        assertFalse(validAdvancedRef(""))
        assertFalse(validAdvancedRef("--help"))
        assertFalse(validAdvancedRef("HEAD\n"))
        assertFalse(validAdvancedRef("two words"))
    }
}
