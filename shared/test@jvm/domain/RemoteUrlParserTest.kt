package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteUrlParserTest {

    @Test
    fun `scp-like and ssh remotes expose host owner and repository`() {
        val scp = parseRemoteUrl("git@github.com:Baconana-chan/MyBeautifulGit-chan.git")
        assertEquals(RemoteTransport.Ssh, scp.transport)
        assertEquals(RemoteHostKind.GitHub, scp.hostKind)
        assertEquals("github.com", scp.host)
        assertEquals("Baconana-chan", scp.owner)
        assertEquals("MyBeautifulGit-chan", scp.repository)
        assertEquals("github.com/Baconana-chan/MyBeautifulGit-chan", scp.fullName)
        assertTrue(scp.isComplete)

        val ssh = parseRemoteUrl("ssh://git@git.example.com:2222/group/sub/repo.git")
        assertEquals(RemoteTransport.Ssh, ssh.transport)
        assertEquals("git.example.com", ssh.host)
        assertEquals(2222, ssh.port)
        assertEquals("group/sub", ssh.owner)
        assertEquals("repo", ssh.repository)
        assertEquals(RemoteHostKind.Custom, ssh.hostKind)
    }

    @Test
    fun `https ports and query strings are parsed without credentials`() {
        val identity = parseRemoteUrl("https://user:secret@GitHub.com:8443/Team/Repo.git?ref=main")
        assertEquals(RemoteTransport.Https, identity.transport)
        assertEquals("github.com", identity.host)
        assertEquals(8443, identity.port)
        assertEquals("Team", identity.owner)
        assertEquals("Repo", identity.repository)
        assertEquals(RemoteHostKind.GitHub, identity.hostKind)
        assertFalse(identity.originalUrl.contains("secret"))
        assertFalse(identity.resolvedUrl.contains("secret"))
        assertTrue(identity.originalUrl.contains("***@"))

        val scp = parseRemoteUrl("user:secret@example.com:team/repo.git")
        assertFalse(scp.originalUrl.contains("secret"))
        assertTrue(scp.originalUrl.contains("***@"))
        assertEquals("example.com/team/repo", scp.fullName)
        assertTrue(urlContainsCredentials("user:secret@example.com:team/repo.git"))
        assertFalse(urlContainsCredentials("git@example.com:team/repo.git"))
        assertEquals("git@example.com:team/repo.git", redactRemoteUrl("git@example.com:team/repo.git"))
    }

    @Test
    fun `git protocol and local paths stay provider agnostic`() {
        val git = parseRemoteUrl("git://example.com/team/repo.git")
        assertEquals(RemoteTransport.Git, git.transport)
        assertEquals("example.com/team/repo", git.fullName)

        listOf(
            "/tmp/repo",
            "./repo",
            "../repo",
            "~/src/repo",
            """C:\src\repo""",
            """\\server\share\repo""",
            "repo",
        ).forEach { path ->
            val local = parseRemoteUrl(path)
            assertTrue(local.isLocal, path)
            assertEquals(RemoteTransport.Local, local.transport)
            assertNull(local.host, path)
            assertNull(local.owner, path)
            assertNull(local.repository, path)
            assertNull(local.fullName, path)
        }
    }

    @Test
    fun `missing owner and unsupported transports are reported as unknown`() {
        val noOwner = parseRemoteUrl("https://example.com/repo.git")
        assertEquals(RemoteIdentityIssue.MissingOwner, noOwner.issue)
        assertFalse(noOwner.isComplete)
        assertNull(noOwner.fullName)

        val unsupported = parseRemoteUrl("ftp://example.com/team/repo.git")
        assertEquals(RemoteIdentityIssue.UnsupportedTransport, unsupported.issue)
        assertEquals(RemoteTransport.Unknown, unsupported.transport)

        val http = parseRemoteUrl("http://example.com/team/repo.git")
        assertEquals(RemoteIdentityIssue.UnsupportedTransport, http.issue)

        val ext = parseRemoteUrl("ext::sh -c echo")
        assertEquals(RemoteIdentityIssue.UnsupportedTransport, ext.issue)

        val invalid = parseRemoteUrl("https:///team/repo.git")
        assertEquals(RemoteIdentityIssue.MissingHost, invalid.issue)

        val empty = parseRemoteUrl("   ")
        assertEquals(RemoteIdentityIssue.InvalidUrl, empty.issue)
    }

    @Test
    fun `insteadOf is applied longest-prefix first`() {
        val rules = gitInsteadOfRules(
            listOf(
                GitConfigEntry(
                    key = "url.https://github.com/.insteadOf",
                    value = "gh:",
                    scope = GitConfigScope.Global,
                ),
                GitConfigEntry(
                    key = "url.ssh://git@example.com/.insteadOf",
                    value = "work:",
                    scope = GitConfigScope.Global,
                ),
            ),
        )
        val github = parseRemoteUrl("gh:Baconana-chan/repo.git", rules)
        assertNotNull(github.host)
        assertEquals("github.com", github.host)
        assertEquals("https://github.com/Baconana-chan/repo.git", github.resolvedUrl)
        assertEquals("github.com/Baconana-chan/repo", github.fullName)

        val work = parseRemoteUrl("work:team/repo.git", rules)
        assertEquals(RemoteTransport.Ssh, work.transport)
        assertEquals("example.com", work.host)
        assertEquals("example.com/team/repo", work.fullName)
    }

    @Test
    fun `remote name is never treated as an owner guess`() {
        val origin = parseRemoteUrl("origin")
        assertTrue(origin.isLocal)
        assertNull(origin.owner)
        assertNull(origin.repository)
        assertNull(origin.fullName)

        val custom = parseRemoteUrl("https://git.self-hosted.example/team/repo.git")
        assertEquals(RemoteHostKind.Custom, custom.hostKind)
        assertEquals("team", custom.owner)
        assertEquals("repo", custom.repository)
    }

    @Test
    fun `multiple URLs are parsed and deduplicated by resolved value`() {
        val urls = parseRemoteUrls(
            listOf(
                "https://example.com/team/repo.git",
                "https://example.com/team/repo.git",
                "git@example.com:team/repo.git",
            ),
        )
        assertEquals(2, urls.size)
        assertEquals(listOf("https://example.com/team/repo.git", "git@example.com:team/repo.git"), urls.map { it.resolvedUrl })
    }
}
