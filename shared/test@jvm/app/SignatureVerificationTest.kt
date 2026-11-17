package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.runBlocking
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.SignatureCheckKind
import org.baconana.mybeautifulgit_chan.domain.SignatureTarget
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignatureVerificationTest {

    @Test
    fun `verifies an unsigned commit and exposes the result`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-signature-commit").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            val controller = RepoController(git.open(dir.path), git, git.version())
            controller.refresh()
            val hash = controller.commits.first().hash

            assertFalse(controller.verifyCommitSignature(hash))
            val report = assertNotNull(controller.signatureVerification)
            assertEquals(hash, report.target)
            assertEquals(SignatureTarget.Commit, report.targetKind)
            assertFalse(report.verified)
            val check = report.checks.single()
            assertEquals(SignatureCheckKind.Commit, check.kind)
            assertEquals(CommitSignatureStatus.NoSignature, check.status)
            assertFalse(check.ok)
            assertTrue(check.command.contains("verify-commit"))
            assertFalse(controller.signatureVerificationLoading)
            assertFalse(controller.busy)
            assertEquals(EnglishStrings.toastSignatureFailed, controller.toast?.message)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `verifies an unsigned annotated tag`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-signature-tag").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            assertTrue(git.run(dir.path, listOf("commit", "--allow-empty", "-m", "base")).ok)
            assertTrue(git.run(dir.path, listOf("tag", "-a", "v1", "-m", "release")).ok)
            val controller = RepoController(git.open(dir.path), git, git.version())
            controller.refresh()

            assertFalse(controller.verifyTagSignature("v1"))
            val report = assertNotNull(controller.signatureVerification)
            assertEquals("refs/tags/v1", report.target)
            assertEquals(SignatureTarget.Tag, report.targetKind)
            val check = report.checks.single()
            assertEquals(SignatureCheckKind.Tag, check.kind)
            assertEquals(CommitSignatureStatus.NoSignature, check.status)
            assertTrue(check.command.contains("verify-tag"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `verifies ssh signed commit tag and embedded merge tag`() = runBlocking {
        if (!hasSshKeygen()) return@runBlocking
        val dir = Files.createTempDirectory("git-chan-signature-ssh").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            if (!configureSshSigning(dir, git)) return@runBlocking

            assertTrue(git.run(dir.path, listOf("commit", "-S", "--allow-empty", "-m", "base")).ok)
            val base = git.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            assertTrue(git.run(dir.path, listOf("checkout", "-b", "side")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-S", "--allow-empty", "-m", "side")).ok)
            assertTrue(
                git.run(dir.path, listOf("tag", "-s", "-m", "side tag", "signed-side")).ok,
            )
            assertTrue(git.run(dir.path, listOf("checkout", "main")).ok)
            assertTrue(
                git.run(
                    dir.path,
                    listOf("merge", "--no-ff", "signed-side", "-m", "merge signed tag"),
                ).ok,
            )

            val controller = RepoController(git.open(dir.path), git, git.version())
            controller.refresh()
            assertTrue(controller.verifyCommitSignature(base))
            assertEquals(CommitSignatureStatus.Good, controller.signatureVerification?.checks?.single()?.status)

            assertTrue(controller.verifyTagSignature("signed-side"))
            assertEquals(
                CommitSignatureStatus.Good,
                controller.signatureVerification?.checks?.single()?.status,
            )

            val merge = controller.commits.first { it.isMerge }
            assertTrue(controller.verifyCommitSignature(merge.hash, merge = true))
            val report = assertNotNull(controller.signatureVerification)
            assertTrue(report.verified)
            assertEquals(2, report.checks.size)
            assertEquals(SignatureCheckKind.MergeTag, report.checks[1].kind)
            assertEquals(CommitSignatureStatus.Good, report.checks[1].status)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `verifies a merge commit through the merge path`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-signature-merge").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(git.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(git.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "side.txt").writeText("side\n")
            assertTrue(git.run(dir.path, listOf("add", "side.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "side")).ok)
            assertTrue(git.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "main.txt").writeText("main\n")
            assertTrue(git.run(dir.path, listOf("add", "main.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "main")).ok)
            assertTrue(git.run(dir.path, listOf("merge", "--no-ff", "side", "-m", "merge side")).ok)

            val controller = RepoController(git.open(dir.path), git, git.version())
            controller.refresh()
            val merge = controller.commits.first { it.isMerge }

            assertFalse(controller.verifyCommitSignature(merge.hash, merge = true))
            val report = assertNotNull(controller.signatureVerification)
            assertEquals(SignatureTarget.Merge, report.targetKind)
            assertEquals(SignatureCheckKind.MergeCommit, report.checks.single().kind)
            assertEquals(CommitSignatureStatus.NoSignature, report.checks.single().status)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun initRepository(dir: File, git: CliGitBackend) {
        assertTrue(git.run(dir.path, listOf("init", "-b", "main")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(git.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(git.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
        assertTrue(git.run(dir.path, listOf("config", "tag.gpgsign", "false")).ok)
    }

    /** Signing tests use an ephemeral SSH key, so they do not need a GPG keyring. */
    private fun hasSshKeygen(): Boolean = runCatching {
        ProcessBuilder("ssh-keygen", "-?")
            .redirectErrorStream(true)
            .start()
            .waitFor()
        true
    }.getOrDefault(false)

    private fun configureSshSigning(dir: File, git: CliGitBackend): Boolean {
        val key = File(dir, "signing-key")
        val allowedSigners = File(dir, "allowed-signers")
        val generated = runCatching {
            ProcessBuilder(
                "ssh-keygen",
                "-q",
                "-t",
                "ed25519",
                "-N",
                "",
                "-f",
                key.path,
            ).redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)
        if (!generated) return false
        val publicKey = File("${key.path}.pub").readText().trim()
        allowedSigners.writeText("test@example.com $publicKey\n")
        return git.run(dir.path, listOf("config", "gpg.format", "ssh")).ok &&
            git.run(
                dir.path,
                listOf("config", "user.signingkey", key.path.replace('\\', '/')),
            ).ok &&
            git.run(
                dir.path,
                listOf(
                    "config",
                    "gpg.ssh.allowedSignersFile",
                    allowedSigners.path.replace('\\', '/'),
                ),
            ).ok
    }
}
