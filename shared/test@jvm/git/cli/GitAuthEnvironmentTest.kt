package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.GitAuth
import org.baconana.mybeautifulgit_chan.domain.SshAuthMode
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitAuthEnvironmentTest {

    @Test
    fun `token is read from a private credential file instead of the environment`() {
        val auth = GitAuth(username = "octocat", token = "ghp_secret")
        val prepared = auth.prepare()
        val credentialFile = assertNotNull(prepared.credentialFile)
        try {
            assertTrue(prepared.configArgs.any { it.contains("credential.helper=!cat") })
            assertFalse(prepared.configArgs.any { it.contains("ghp_secret") })
            assertFalse(prepared.configArgs.any { it.contains("MBG_GIT_TOKEN") })
            assertTrue(credentialFile.isFile)
            assertTrue(credentialFile.readText().contains("username=octocat"))
            assertTrue(credentialFile.readText().contains("password=ghp_secret"))

            assertFalse(prepared.environment.containsKey("MBG_GIT_TOKEN"))
            assertFalse(prepared.environment.containsKey("MBG_GIT_USERNAME"))
            assertFalse(auth.toString().contains("ghp_secret"))
        } finally {
            prepared.cleanup()
        }
        assertFalse(credentialFile.exists())
    }

    @Test
    fun `key mode passes an explicit ssh identity and agent mode does not`() {
        val keyAuth = GitAuth(
            sshMode = SshAuthMode.Key,
            sshKeyPath = "/home/me/.ssh/id ed25519",
        )
        assertEquals(
            "ssh -i '/home/me/.ssh/id ed25519' -o IdentitiesOnly=yes",
            keyAuth.environment()["GIT_SSH_COMMAND"],
        )
        assertFalse(GitAuth(sshMode = SshAuthMode.Agent).environment().containsKey("GIT_SSH_COMMAND"))
    }

    @Test
    fun `auth is limited to network commands`() {
        assertTrue(authRequired(listOf("clone", "url")))
        assertTrue(authRequired(listOf("fetch", "origin")))
        assertTrue(authRequired(listOf("pull")))
        assertTrue(authRequired(listOf("push", "origin")))
        assertTrue(authRequired(listOf("ls-remote", "origin")))
        assertTrue(authRequired(listOf("archive", "--remote=origin")))

        assertTrue(authRequired(listOf("submodule", "update", "--init")))
        assertTrue(authRequired(listOf("submodule", "add", "url", "path")))
        assertTrue(authRequired(listOf("remote", "show", "origin")))
        assertTrue(authRequired(listOf("remote", "prune", "origin")))
        assertTrue(authRequired(listOf("remote", "update")))

        assertFalse(authRequired(listOf("remote", "-v")))
        assertFalse(authRequired(listOf("remote", "add", "origin", "url")))
        assertFalse(authRequired(listOf("submodule", "status")))
        assertFalse(authRequired(listOf("submodule", "init")))
        assertFalse(authRequired(listOf("submodule", "sync")))
        assertFalse(authRequired(listOf("archive")))
        assertFalse(authRequired(listOf("status")))
    }

    @Test
    fun `git credential fill uses the token file helper`() {
        val dir = Files.createTempDirectory("git-chan-auth")
        try {
            val backend = CliGitBackend(
                initialAuth = GitAuth(username = "git", token = "token-123"),
            )
            val result = backend.run(
                cwd = dir.toString(),
                args = listOf("credential", "fill"),
                stdin = "protocol=https\nhost=example.com\n\n",
            )

            assertTrue(result.ok, result.stderr)
            assertTrue(result.stdout.contains("username=git"), result.stdout)
            assertTrue(result.stdout.contains("password=***"), result.stdout)
            assertFalse(result.stdout.contains("token-123"))
            assertFalse(result.commandLine.contains("token-123"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `configured token is redacted from non-network command output too`() {
        val dir = Files.createTempDirectory("git-chan-token-redaction")
        try {
            val backend = CliGitBackend(
                initialAuth = GitAuth(username = "git", token = "local-token"),
            )
            assertTrue(backend.run(dir.toString(), listOf("init", "-b", "main")).ok)
            assertTrue(
                backend.run(dir.toString(), listOf("config", "local.testSecret", "local-token")).ok,
            )

            val result = backend.run(dir.toString(), listOf("config", "--get", "local.testSecret"))

            assertTrue(result.ok, result.stderr)
            assertEquals("***", result.stdout.trim())
            assertFalse(result.stdout.contains("local-token"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `configured credential helper is passed to git`() {
        val dir = Files.createTempDirectory("git-chan-helper")
        try {
            val backend = CliGitBackend(
                initialAuth = GitAuth(
                    credentialHelper = "!f() { echo username=helper; echo password=secret; }; f",
                ),
            )
            val result = backend.run(
                cwd = dir.toString(),
                args = listOf("credential", "fill"),
                stdin = "protocol=https\nhost=example.com\n\n",
            )

            assertTrue(result.ok, result.stderr)
            assertTrue(result.stdout.contains("username=helper"), result.stdout)
            assertTrue(result.stdout.contains("password=secret"), result.stdout)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `pre-push hook does not receive the token`() {
        val root = Files.createTempDirectory("git-chan-auth-hook").toFile()
        val work = File(root, "work")
        val remote = File(root, "remote.git")
        val hookEnv = File(root, "hook-env.txt")
        try {
            val git = CliGitBackend()
            assertTrue(git.run(null, listOf("init", "--bare", remote.path)).ok)
            assertTrue(git.run(null, listOf("init", "-b", "main", work.path)).ok)
            assertTrue(git.run(work.path, listOf("config", "user.name", "Review")).ok)
            assertTrue(git.run(work.path, listOf("config", "user.email", "review@example.com")).ok)
            File(work, "file.txt").writeText("ok\n")
            assertTrue(git.run(work.path, listOf("add", "file.txt")).ok)
            assertTrue(git.run(work.path, listOf("commit", "-m", "base")).ok)
            assertTrue(git.run(work.path, listOf("remote", "add", "origin", remote.path)).ok)

            val hook = File(work, ".git/hooks/pre-push")
            hook.parentFile.mkdirs()
            val hookEnvPath = hookEnv.absolutePath.replace('\\', '/').replace("'", "'\"'\"'")
            hook.writeText("#!/bin/sh\nenv > '$hookEnvPath'\necho ghp_hook_secret >&2\n")
            hook.setExecutable(true)

            val authBackend = CliGitBackend(
                initialAuth = GitAuth(username = "git", token = "ghp_hook_secret"),
            )
            val push = authBackend.run(work.path, listOf("push", "origin", "main"))
            assertTrue(push.ok, push.stderr)
            assertFalse(push.stderr.contains("ghp_hook_secret"))
            assertTrue(push.stderr.contains("***"), push.stderr)
            assertTrue(hookEnv.isFile, "pre-push hook did not run")
            val text = hookEnv.readText()
            assertFalse(text.contains("ghp_hook_secret"))
            assertFalse(text.contains("MBG_GIT_TOKEN"))
            assertFalse(text.contains("MBG_GIT_USERNAME"))
            assertFalse(text.contains("GIT_ASKPASS"))
        } finally {
            root.deleteRecursively()
        }
    }
}
