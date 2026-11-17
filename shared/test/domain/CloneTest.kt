package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloneTest {

    @Test
    fun `separates clone and init arguments from options`() {
        assertEquals(
            listOf("clone", "--progress", "--", "https://example.com/repo.git", "target"),
            cloneArgs("https://example.com/repo.git", "target"),
        )
        assertEquals(
            listOf(
                "clone",
                "--progress",
                "--filter=blob:none",
                "--",
                "https://example.com/repo.git",
                "target",
            ),
            cloneArgs("https://example.com/repo.git", "target", PartialCloneFilter.BlobNone),
        )
        assertEquals(
            listOf("init", "-b", "main", "--", "new-repo"),
            initArgs("main", "new-repo"),
        )
    }

    @Test
    fun `rejects option-like and unsupported clone URLs`() {
        assertTrue(validCloneUrl("https://example.com/user/repo.git"))
        assertTrue(validCloneUrl("http://example.com/repo"))
        assertTrue(validCloneUrl("ssh://git@example.com/repo.git"))
        assertTrue(validCloneUrl("git://example.com/repo.git"))
        assertTrue(validCloneUrl("git@example.com:org/repo.git"))
        assertTrue(validCloneUrl("C:\\Users\\VIC\\repo"))
        assertTrue(validCloneUrl("../local-repo"))

        assertFalse(validCloneUrl(""))
        assertFalse(validCloneUrl("--upload-pack=sh -c 'touch pwned'"))
        assertFalse(validCloneUrl("-uhttps://example.com/repo"))
        assertFalse(validCloneUrl("ext::sh -c 'touch pwned'"))
        assertFalse(validCloneUrl("file:///tmp/repo"))
        assertFalse(validCloneUrl("ftp://example.com/repo"))
        assertFalse(validCloneUrl("https://"))
        assertFalse(validCloneUrl("https://--upload-pack=bad"))
        assertFalse(validCloneUrl("https://example.com/repo\n"))
    }

    @Test
    fun `derives a single safe repository name from a URL`() {
        assertEquals("repo", repositoryNameFromUrl("https://example.com/user/repo.git"))
        assertEquals("repo", repositoryNameFromUrl("https://example.com/user/repo.git/"))
        assertEquals("repo", repositoryNameFromUrl("git@example.com:repo.git"))
        assertEquals("repo", repositoryNameFromUrl("C:\\Users\\VIC\\repo.git"))
        assertEquals("repo", repositoryNameFromUrl("repo"))

        assertEquals("repository", repositoryNameFromUrl(""))
        assertEquals("repository", repositoryNameFromUrl("https://example.com/user/.."))
        assertEquals("repository", repositoryNameFromUrl("https://example.com/user/-repo"))
        assertEquals("repository", repositoryNameFromUrl("--upload-pack=sh -c bad"))
        assertEquals("repository", repositoryNameFromUrl("https://example.com/user/repo:bad.git"))
    }

    @Test
    fun `validates clone targets and default branches`() {
        assertTrue(validCloneTarget("C:\\Users\\VIC\\repos"))
        assertTrue(validCloneTarget("repos/new"))
        assertFalse(validCloneTarget(""))
        assertFalse(validCloneTarget("--bare"))
        assertFalse(validCloneTarget("../repos"))
        assertFalse(validCloneTarget("C:\\repos\\..\\outside"))
        assertFalse(validCloneTarget("repos\n"))

        assertTrue(validInitBranch("main"))
        assertTrue(validInitBranch("feature/new-ui"))
        assertFalse(validInitBranch(""))
        assertFalse(validInitBranch("-main"))
        assertFalse(validInitBranch("bad branch"))
        assertFalse(validInitBranch("feature..new"))
        assertFalse(validInitBranch("main.lock"))
    }
}
