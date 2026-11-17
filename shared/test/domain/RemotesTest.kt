package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemotesTest {
    @Test
    fun `builds fetch flags in stable order`() {
        val args = fetchArgs(
            " origin ",
            FetchOptions(
                prune = true,
                tags = FetchTags.Tags,
                depth = "1",
                filter = " blob:none ",
            ),
        )

        assertEquals(
            listOf(
                "fetch",
                "--prune",
                "--tags",
                "--depth=1",
                "--filter=blob:none",
                "origin",
            ),
            args,
        )
    }

    @Test
    fun `keeps default and no-tags fetch modes distinct`() {
        assertEquals(listOf("fetch", "origin"), fetchArgs("origin", FetchOptions()))
        assertEquals(
            listOf("fetch", "--no-tags", "origin"),
            fetchArgs("origin", FetchOptions(tags = FetchTags.NoTags)),
        )
    }

    @Test
    fun `allows only safe remote transports`() {
        assertTrue(validRemoteUrl("https://example.com/repo.git"))
        assertTrue(validRemoteUrl("ssh://git@example.com/repo.git"))
        assertTrue(validRemoteUrl("git://example.com/repo.git"))
        assertTrue(validRemoteUrl("git@example.com:repo.git"))
        assertTrue(validRemoteUrl("https://[::1]/repo.git"))
        assertTrue(validRemoteUrl("https://[2001:db8::1]:8443/repo.git"))
        assertTrue(validRemoteUrl("./local/repo"))

        assertFalse(validRemoteUrl("http://example.com/repo.git"))
        assertFalse(validRemoteUrl("file:///tmp/repo.git"))
        assertFalse(validRemoteUrl("ext::sh -c 'touch /tmp/pwned'"))
        assertFalse(validRemoteUrl("ftp://example.com/repo.git"))
        assertFalse(validRemoteUrl("https://"))
        assertFalse(validRemoteUrl("https:///repo.git"))
        assertFalse(validRemoteUrl("https://host:bad/repo.git"))
        assertFalse(validRemoteUrl("https://host:/repo.git"))
        assertFalse(validRemoteUrl("https://host:22:33/repo.git"))
        assertFalse(validRemoteUrl("https://::1/repo.git"))
        assertFalse(validRemoteUrl("https://2001:db8::1/repo.git"))
        assertFalse(validRemoteUrl("https://[::1]:bad/repo.git"))
        assertFalse(validRemoteUrl("https://-host/repo.git"))
        assertFalse(validRemoteUrl("https://host/repo with space"))
    }

    @Test
    fun `redacts remote credentials for display`() {
        val url = "https://user:secret@example.com/team/repo.git"
        assertEquals("https://***@example.com/team/repo.git", redactUrlCredentials(url))
        assertTrue(urlContainsCredentials(url))
        assertEquals("https://example.com/team/repo.git", redactUrlCredentials("https://example.com/team/repo.git"))
        assertFalse(urlContainsCredentials("https://example.com/team/repo.git"))
        assertEquals(
            "ssh://git@example.com/team/repo.git",
            redactUrlCredentials("ssh://git@example.com/team/repo.git"),
        )
        assertFalse(urlContainsCredentials("ssh://git@example.com/team/repo.git"))
        assertEquals(
            "ssh://***@example.com/team/repo.git",
            redactUrlCredentials("ssh://git:secret@example.com/team/repo.git"),
        )
        assertEquals(
            "https://***@one.example/repo https://***@two.example/repo git@three.example:repo.git",
            redactUrlCredentials(
                "https://a:one@one.example/repo https://b:two@two.example/repo git@three.example:repo.git",
            ),
        )
        assertTrue(
            urlContainsCredentials(
                "https://a:one@one.example/repo https://b:two@two.example/repo",
            ),
        )
    }

    @Test
    fun `validates remote and fetch options`() {
        assertTrue(validRemoteName("origin"))
        assertTrue(validRemoteName("team/fork"))
        assertFalse(validRemoteName(""))
        assertFalse(validRemoteName("-origin"))
        assertFalse(validRemoteName("bad name"))
        assertFalse(validRemoteName(".."))

        assertTrue(validRemoteUrl("https://example.com/repo.git"))
        assertTrue(validRemoteUrl("C:\\repos\\repo.git"))
        assertFalse(validRemoteUrl(""))
        assertFalse(validRemoteUrl("-upload-pack=evil"))

        assertTrue(validFetchDepth(""))
        assertTrue(validFetchDepth("1"))
        assertFalse(validFetchDepth("0"))
        assertFalse(validFetchDepth("1.5"))
        assertFalse(validFetchDepth("--depth=1"))

        assertTrue(validFetchFilter(""))
        assertTrue(validFetchFilter("blob:none"))
        assertTrue(validFetchFilter("blob:limit=1m"))
        assertFalse(validFetchFilter("blob none"))
        assertFalse(validFetchFilter("-filter"))
    }
}
