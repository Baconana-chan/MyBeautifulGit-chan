package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.RemoteHostKind
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemoteProfileTest {

    @Test
    fun `profile reads all fetch URLs and applies insteadOf without network`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-remote-profile").toFile()
        try {
            val git = CliGitBackend()
            assertTrue(git.run(dir.path, listOf("init", "-b", "main")).ok)
            assertTrue(
                git.run(
                    dir.path,
                    listOf("remote", "add", "origin", "https://github.com/team/repo.git"),
                ).ok,
            )
            assertTrue(
                git.run(
                    dir.path,
                    listOf("remote", "set-url", "--add", "origin", "git@example.com:team/repo.git"),
                ).ok,
            )
            assertTrue(
                git.run(
                    dir.path,
                    listOf("remote", "set-url", "--add", "origin", "gh:team/repo.git"),
                ).ok,
            )
            assertTrue(
                git.run(
                    dir.path,
                    listOf("remote", "set-url", "--push", "origin", "ssh://git@example.com/team/repo.git"),
                ).ok,
            )
            assertTrue(
                git.run(
                    dir.path,
                    listOf("config", "url.https://github.com/.insteadOf", "gh:"),
                ).ok,
            )

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()

            val profile = assertNotNull(controller.loadRemoteProfile("origin"))
            assertEquals("origin", profile.name)
            assertEquals(2, profile.fetchUrls.size)
            assertEquals(RemoteHostKind.GitHub, profile.fetchUrls[0].hostKind)
            assertEquals("github.com/team/repo", profile.fetchUrls[0].fullName)
            assertEquals("example.com/team/repo", profile.fetchUrls[1].fullName)
            assertEquals(1, profile.pushUrls.size)
            assertEquals("example.com/team/repo", profile.pushUrls.single().fullName)
        } finally {
            dir.deleteRecursively()
        }
    }
}
