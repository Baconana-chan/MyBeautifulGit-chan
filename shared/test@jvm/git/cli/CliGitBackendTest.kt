package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.domain.RepositoryMetadataException
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitRunControl
import org.baconana.mybeautifulgit_chan.git.GitVersion
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CliGitBackendTest {
    private val backend = CliGitBackend()

    @Test
    fun `opens repository, reads status, log and refs`() {
        val dir = Files.createTempDirectory("git-chan-status").toFile()
        try {
            initRepository(dir)

            val file = File(dir, "a.txt")
            file.writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            file.writeText("two\n")
            File(dir, "b.txt").writeText("new\n")

            val repository = backend.open(dir.path)
            assertEquals(dir.canonicalPath, repository.root)
            assertEquals(dir.name, repository.name)
            assertFalse(repository.isBare)
            assertFalse(repository.isWorktree)
            assertEquals(repository.root, repository.workTree)

            val status = backend.status(repository)
            assertEquals("main", status.branch?.head)
            assertEquals(40, status.branch?.oid?.length)
            assertFalse(status.isClean)

            val modified = status.entries.single { it.path == "a.txt" }
            assertTrue(modified.isUnstaged)
            assertEquals(StatusKind.Changed, modified.kind)

            val untracked = status.entries.single { it.path == "b.txt" }
            assertEquals(StatusKind.Untracked, untracked.kind)
            assertTrue(untracked.isUnstaged)

            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            val staged = backend.status(repository).entries.single { it.path == "b.txt" }
            assertTrue(staged.isStaged)
            assertFalse(staged.isUnstaged)

            val log = backend.log(repository, maxCount = 10)
            assertEquals(1, log.size)
            assertEquals("initial", log.single().subject)
            assertEquals("main", log.single().refs.firstOrNull { it.startsWith("HEAD ->") }?.removePrefix("HEAD -> "))

            val refs = backend.refs(repository)
            val main = refs.single { it.shortName == "main" }
            assertTrue(main.isHead)
            assertTrue(refs.none { it.kind == RefKind.RemoteBranch })

            val worktrees = backend.worktrees(repository)
            assertEquals(1, worktrees.size)
            assertEquals(dir.canonicalPath, File(worktrees.single().path).canonicalPath)
            assertEquals("main", worktrees.single().branchName)
            assertTrue(backend.submodules(repository).isEmpty())

            val lock = File(File(dir, ".git"), "index.lock")
            lock.writeText("")
            assertNotNull(backend.runtimeState(repository).indexLock)
            assertTrue(backend.removeIndexLock(repository))
            assertNull(backend.runtimeState(repository).indexLock)

            val nested = File(dir, "src/deep").apply { mkdirs() }
            assertEquals(repository.root, backend.open(nested.path).root)
            assertTrue(backend.version() >= GitVersion(2, 30, 0, "2.30"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reports missing git objects instead of a raw rev-parse failure`() {
        val dir = Files.createTempDirectory("git-chan-missing-objects").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            assertTrue(File(dir, ".git/objects").renameTo(File(dir, ".git/objects-removed")))

            val error = assertFailsWith<RepositoryMetadataException> {
                backend.open(dir.path)
            }
            assertEquals(dir.canonicalPath, error.repositoryPath)
            assertEquals(listOf(".git/objects"), error.missingMetadata)
            assertTrue(error.cause is GitCommandException)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `previews commits and local changes affected by reset`() {
        val dir = Files.createTempDirectory("git-chan-reset-preview").toFile()
        try {
            initRepository(dir)
            val file = File(dir, "a.txt")
            file.writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val first = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            file.writeText("second\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            file.writeText("local\n")
            File(dir, "staged.txt").writeText("staged\n")
            assertTrue(backend.run(dir.path, listOf("add", "staged.txt")).ok)
            File(dir, "new.txt").writeText("new\n")

            val preview = backend.resetPreview(backend.open(dir.path), first)
            assertEquals(1, preview.lostCommits)
            assertEquals(listOf("second"), preview.lostCommitSubjects)
            assertEquals(listOf("staged.txt"), preview.stagedPaths)
            assertEquals(listOf("a.txt"), preview.unstagedPaths)
            assertEquals(listOf("new.txt"), preview.untrackedPaths)
            assertTrue(preview.conflictedPaths.isEmpty())
            assertEquals(listOf("a.txt"), preview.keepConflicts)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `plans unreachable commits objects and existing fsck garbage`() {
        val dir = Files.createTempDirectory("git-chan-unreachable-plan").toFile()
        try {
            initRepository(dir)
            val file = File(dir, "a.txt")
            file.writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            file.writeText("second\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            val second = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            file.writeText("third\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "third")).ok)
            val third = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val repository = backend.open(dir.path)
            val plan = backend.unreachablePlan(repository, listOf(second))
            assertEquals(1, plan.commitCount)
            assertEquals(listOf("third"), plan.commits.map { it.subject })
            assertTrue(plan.objectCount > 0)
            assertNull(plan.fsckError)

            assertTrue(backend.run(dir.path, listOf("branch", "keep", third)).ok)
            val kept = backend.unreachablePlan(repository, listOf(second))
            assertEquals(0, kept.commitCount)
            assertEquals(0, kept.objectCount)

            file.writeText("dangling\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "dangling")).ok)
            assertTrue(backend.run(dir.path, listOf("reset", "--hard", third)).ok)
            val dangling = backend.unreachablePlan(repository, listOf(third))
            assertEquals(0, dangling.commitCount)
            assertTrue(dangling.fsckUnreachableCommits >= 1)
            assertNull(dangling.fsckError)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads a full commit message including body`() {
        val dir = Files.createTempDirectory("git-chan-commit-message").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(
                backend.run(
                    dir.path,
                    listOf("commit", "-m", "subject", "-m", "body line one\nbody line two"),
                ).ok,
            )
            val repository = backend.open(dir.path)
            val message = backend.commitMessage(
                repository,
                backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim(),
            )
            assertEquals("subject\n\nbody line one\nbody line two", message)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `empty repository has empty log and no refs`() {
        val dir = Files.createTempDirectory("git-chan-empty").toFile()
        try {
            initRepository(dir)
            val repository = backend.open(dir.path)

            assertTrue(backend.log(repository).isEmpty())
            assertTrue(backend.refs(repository).isEmpty())

            val status = backend.status(repository)
            assertEquals("main", status.branch?.head)
            assertNull(status.branch?.oid)
            assertTrue(status.branch?.unborn ?: false)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `lists an added worktree`() {
        val dir = Files.createTempDirectory("git-chan-worktree").toFile()
        val worktreeDir = File(dir.parentFile, "${dir.name}-linked")
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val add = backend.run(
                dir.path,
                listOf("worktree", "add", worktreeDir.path, "-b", "side"),
            )
            assertTrue(add.ok, add.stderr)

            val worktrees = backend.worktrees(backend.open(dir.path))
            assertEquals(2, worktrees.size)
            val linked = worktrees.single { it.branchName == "side" }
            assertEquals(worktreeDir.canonicalPath, File(linked.path).canonicalPath)
            assertFalse(linked.isDetached)
        } finally {
            worktreeDir.deleteRecursively()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `detects a real merge conflict and clears it after abort`() {
        val dir = Files.createTempDirectory("git-chan-merge").toFile()
        try {
            initRepository(dir)
            val file = File(dir, "a.txt")
            file.writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            file.writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            file.writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)

            assertFalse(backend.run(dir.path, listOf("merge", "side")).ok)
            val repository = backend.open(dir.path)
            val operation = assertNotNull(backend.runtimeState(repository).operation)
            assertEquals(GitOperationKind.Merge, operation.kind)

            assertTrue(backend.run(dir.path, listOf("merge", "--abort")).ok)
            assertNull(backend.runtimeState(repository).operation)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads commit diff stats and unsigned signature`() {
        val dir = Files.createTempDirectory("git-chan-inspection").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            File(dir, "b.txt").writeText("keep\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            File(dir, "a.txt").writeText("one\ntwo\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "inspect me")).ok)

            val repository = backend.open(dir.path)
            val hash = backend.log(repository, maxCount = 1).single().hash
            val inspection = backend.commitInspection(repository, hash)

            assertEquals(hash, inspection.hash)
            assertTrue(inspection.diff.contains("diff --git a/a.txt b/a.txt"))
            assertTrue(inspection.diff.contains("+two"))
            assertFalse(inspection.diff.contains("+keep"))
            assertEquals(1, inspection.stats.files.size)
            assertEquals("a.txt", inspection.stats.files.single().path)
            assertEquals(1, inspection.stats.additions)
            assertEquals(0, inspection.stats.deletions)
            assertEquals(CommitSignatureStatus.NoSignature, inspection.signature.status)
            assertFalse(inspection.diffTruncated)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `uses first-parent diff for a merge commit`() {
        val dir = Files.createTempDirectory("git-chan-merge-inspection").toFile()
        try {
            initRepository(dir)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "side.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "side.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "main.txt").writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("add", "main.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)
            val merge = backend.run(dir.path, listOf("merge", "--no-ff", "side", "-m", "merge"))
            assertTrue(merge.ok, merge.stderr)

            val repository = backend.open(dir.path)
            val inspection = backend.commitInspection(repository, backend.log(repository, maxCount = 1).single().hash)
            assertTrue(inspection.diff.contains("diff --git a/side.txt b/side.txt"))
            assertTrue(inspection.diff.contains("+side"))
            assertEquals("side.txt", inspection.stats.files.single().path)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `marks a commit diff as truncated when it exceeds the limit`() {
        val dir = Files.createTempDirectory("git-chan-inspection-limit").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("one\ntwo\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "big enough")).ok)

            val repository = backend.open(dir.path)
            val hash = backend.log(repository, maxCount = 1).single().hash
            val inspection = backend.commitInspection(repository, hash, maxDiffBytes = 20)
            assertTrue(inspection.diffTruncated)
            assertTrue(inspection.diff.toByteArray().size <= 20)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `marks a single huge diff line as truncated without throwing`() {
        val dir = Files.createTempDirectory("git-chan-inspection-huge-line").toFile()
        try {
            initRepository(dir)
            File(dir, "huge-line.txt").writeText("x".repeat(8 * 1024 * 1024 + 100_000))
            assertTrue(backend.run(dir.path, listOf("add", "huge-line.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "huge line")).ok)

            val repository = backend.open(dir.path)
            val hash = backend.log(repository, maxCount = 1).single().hash
            val inspection = backend.commitInspection(repository, hash)

            assertTrue(inspection.diffTruncated)
            assertTrue(inspection.diff.toByteArray().size <= 2 * 1024 * 1024)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `skips the full patch for a commit with too many changed lines`() {
        val dir = Files.createTempDirectory("git-chan-inspection-large").toFile()
        try {
            initRepository(dir)
            File(dir, "large.txt").writeText((1..25_000).joinToString("\n"))
            assertTrue(backend.run(dir.path, listOf("add", "large.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "many lines")).ok)

            val repository = backend.open(dir.path)
            val hash = backend.log(repository, maxCount = 1).single().hash
            val inspection = backend.commitInspection(repository, hash)

            assertTrue(inspection.diffTruncated)
            assertTrue(inspection.diff.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads file history across renames, blame and grep`() {
        val dir = Files.createTempDirectory("git-chan-file-search").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "first")).ok)

            assertTrue(backend.run(dir.path, listOf("mv", "a.txt", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "rename")).ok)
            File(dir, "b.txt").writeText("one\nTWO\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "change")).ok)

            val repository = backend.open(dir.path)
            val history = backend.fileHistory(repository, "b.txt")
            assertEquals(listOf("change", "rename", "first"), history.map { it.subject })

            val blame = backend.blame(repository, "b.txt", maxLines = 10)
            assertEquals(2, blame.lines.size)
            assertFalse(blame.truncated)
            assertEquals("one", blame.lines[0].text)
            assertEquals("TWO", blame.lines[1].text)
            assertEquals("change", blame.lines[1].summary)

            val limited = backend.blame(repository, "b.txt", maxLines = 1)
            assertEquals(1, limited.lines.size)
            assertTrue(limited.truncated)

            val matches = backend.grep(repository, "TWO")
            assertEquals(1, matches.matches.size)
            assertEquals("b.txt", matches.matches.single().path)
            assertEquals(2, matches.matches.single().lineNumber)
            assertEquals("TWO", matches.matches.single().text)
            assertTrue(backend.grep(repository, "TWO", path = "missing.txt").matches.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads reflog entries for the current refs`() {
        val dir = Files.createTempDirectory("git-chan-reflog").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            val first = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            val second = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val entries = backend.reflog(backend.open(dir.path), maxCount = 20)
            assertTrue(entries.any { it.hash == first && it.selector.contains("HEAD") })
            assertTrue(entries.any { it.hash == second && it.subject.contains("second") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `counts stash entries`() {
        val dir = Files.createTempDirectory("git-chan-stash").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("stash", "push", "-m", "snapshot")).ok)

            val repository = backend.open(dir.path)
            assertEquals(1, backend.stashCount(repository))

            assertTrue(backend.run(dir.path, listOf("stash", "pop")).ok)
            assertEquals(0, backend.stashCount(repository))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `filters log by branch author path date message and pickaxe`() {
        val dir = Files.createTempDirectory("git-chan-log-filters").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("alpha\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            commitAs(
                dir = dir,
                message = "initial alpha",
                author = "Me",
                email = "me@example.com",
                date = "2020-01-01T12:00:00+0000",
            )

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "c.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "c.txt")).ok)
            commitAs(
                dir = dir,
                message = "side work",
                author = "Other",
                email = "other@example.com",
                date = "2021-01-01T12:00:00+0000",
            )

            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "b.txt").writeText("needle\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            commitAs(
                dir = dir,
                message = "add beta",
                author = "Me",
                email = "me@example.com",
                date = "2020-06-01T12:00:00+0000",
            )

            val repository = backend.open(dir.path)
            assertEquals(
                listOf("add beta", "initial alpha"),
                backend.log(repository, maxCount = 10).map { it.subject },
            )
            assertEquals(
                listOf("initial alpha"),
                backend.log(repository, maxCount = 10, skip = 1).map { it.subject },
            )
            assertEquals(
                setOf("side work", "add beta", "initial alpha"),
                backend.log(repository, maxCount = 10, filter = LogFilter(onlyCurrent = false))
                    .map { it.subject }.toSet(),
            )
            assertEquals(
                setOf("side work", "initial alpha"),
                backend.log(repository, maxCount = 10, filter = LogFilter(branch = "side"))
                    .map { it.subject }.toSet(),
            )
            assertEquals(
                listOf("side work"),
                backend.log(
                    repository,
                    maxCount = 10,
                    filter = LogFilter(author = "other@example.com", onlyCurrent = false),
                )
                    .map { it.subject },
            )
            assertEquals(
                listOf("add beta"),
                backend.log(repository, maxCount = 10, filter = LogFilter(path = "b.txt"))
                    .map { it.subject },
            )
            assertEquals(
                listOf("side work"),
                backend.log(
                    repository,
                    maxCount = 10,
                    filter = LogFilter(since = "2021-01-01T00:00:00+0000", onlyCurrent = false),
                )
                    .map { it.subject },
            )
            assertEquals(
                listOf("add beta", "initial alpha"),
                backend.log(
                    repository,
                    maxCount = 10,
                    filter = LogFilter(until = "2020-12-31T23:59:59+0000"),
                )
                    .map { it.subject },
            )
            assertEquals(
                listOf("add beta"),
                backend.log(repository, maxCount = 10, filter = LogFilter(message = "beta"))
                    .map { it.subject },
            )
            assertEquals(
                listOf("add beta"),
                backend.log(repository, maxCount = 10, filter = LogFilter(pickaxe = "needle"))
                    .map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `bounds huge stdout and marks it truncated`() {
        val dir = Files.createTempDirectory("git-chan-output-cap").toFile()
        try {
            initRepository(dir)
            File(dir, "big.txt").writeText("x".repeat(9 * 1024 * 1024))
            assertTrue(backend.run(dir.path, listOf("add", "big.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "huge")).ok)

            val result = backend.run(
                dir.path,
                listOf("cat-file", "blob", "HEAD:big.txt"),
                timeoutSeconds = 60,
            )

            assertTrue(result.stdoutTruncated)
            assertTrue(result.stdout.isNotEmpty())
            assertTrue(result.stdout.length <= 8 * 1024 * 1024)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `blocks the ext transport even when the environment allows it`() {
        val dir = Files.createTempDirectory("git-chan-ext-transport").toFile()
        try {
            initRepository(dir)
            val sentinel = File(dir, "ext-pwned.txt")
            val sentinelPath = sentinel.absolutePath.replace('\\', '/')
            val command = "sh -c 'echo pwned > \"$sentinelPath\"'"
            val result = backend.run(
                dir.path,
                listOf("ls-remote", "ext::$command"),
                timeoutSeconds = 30,
                environment = mapOf(
                    "GIT_CONFIG_COUNT" to "1",
                    "GIT_CONFIG_KEY_0" to "protocol.ext.allow",
                    "GIT_CONFIG_VALUE_0" to "always",
                ),
            )

            assertFalse(result.ok)
            assertFalse(sentinel.exists())
            assertTrue(
                result.stderr.contains("ext", ignoreCase = true) ||
                    result.stderr.contains("transport", ignoreCase = true),
                result.stderr,
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `bare repository is detected and has no worktree`() {
        val dir = Files.createTempDirectory("git-chan-bare").toFile()
        try {
            val init = backend.run(null, listOf("init", "--bare", dir.path))
            assertTrue(init.ok, init.stderr)

            val repository = backend.open(dir.path)
            assertTrue(repository.isBare)
            assertNull(repository.workTree)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads all scopes and edits aliases unknown keys and worktree config`() {
        val dir = Files.createTempDirectory("git-chan-config").toFile()
        try {
            initRepository(dir)
            val repository = backend.open(dir.path)

            assertTrue(
                backend.setConfig(repository, GitConfigScope.Local, "alias.co", "checkout").ok,
            )
            assertTrue(
                backend.setConfig(repository, GitConfigScope.Local, "thirdparty.unknownKey", "kept").ok,
            )
            assertTrue(
                backend.setConfig(
                    repository,
                    GitConfigScope.Local,
                    "includeIf.gitdir:C:/work/.path",
                    "C:/work/.gitconfig",
                ).ok,
            )
            assertTrue(
                backend.setConfig(
                    repository,
                    GitConfigScope.Local,
                    "extensions.worktreeConfig",
                    "true",
                ).ok,
            )
            assertTrue(
                backend.setConfig(repository, GitConfigScope.Worktree, "worktree.test", "yes").ok,
            )

            val entries = backend.configEntries(repository)
            assertTrue(entries.any { it.key == "alias.co" && it.scope == GitConfigScope.Local })
            assertTrue(
                entries.any {
                    it.key.equals("thirdparty.unknownKey", ignoreCase = true) && it.value == "kept"
                },
            )
            assertTrue(entries.any { it.isIncludeIf && it.scope == GitConfigScope.Local })
            assertTrue(
                entries.any {
                    it.key.equals("worktree.test", ignoreCase = true) &&
                        it.scope == GitConfigScope.Worktree
                },
            )
            assertTrue(
                entries.none {
                    it.key.equals("color.ui", ignoreCase = true) && it.scope == GitConfigScope.Local
                },
            )

            assertTrue(backend.unsetConfig(repository, GitConfigScope.Local, "alias.co").ok)
            assertTrue(backend.configEntries(repository).none { it.key == "alias.co" })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `replaces and removes one multivalue config value without touching siblings`() {
        val dir = Files.createTempDirectory("git-chan-multivalue-config").toFile()
        try {
            initRepository(dir)
            val repository = backend.open(dir.path)
            val key = "thirdparty.multivalue"

            assertTrue(backend.addConfig(repository, GitConfigScope.Local, key, "alpha").ok)
            assertTrue(backend.addConfig(repository, GitConfigScope.Local, key, "beta").ok)
            assertTrue(backend.addConfig(repository, GitConfigScope.Local, key, "a.b").ok)
            assertTrue(backend.addConfig(repository, GitConfigScope.Local, key, "aXb").ok)

            fun values() = backend.configEntries(repository)
                .filter { it.scope == GitConfigScope.Local && it.key.equals(key, ignoreCase = true) }
                .map { it.value }

            assertTrue(
                backend.setConfig(
                    repository,
                    GitConfigScope.Local,
                    key,
                    "ALPHA",
                    oldValue = "alpha",
                ).ok,
            )
            assertEquals(setOf("ALPHA", "beta", "a.b", "aXb"), values().toSet())
            assertEquals(4, values().size)

            assertTrue(
                backend.setConfig(
                    repository,
                    GitConfigScope.Local,
                    key,
                    "DOT",
                    oldValue = "a.b",
                ).ok,
            )
            assertTrue(values().contains("aXb"))
            assertFalse(values().contains("a.b"))

            assertTrue(
                backend.unsetConfig(
                    repository,
                    GitConfigScope.Local,
                    key,
                    oldValue = "beta",
                ).ok,
            )
            assertEquals(setOf("ALPHA", "DOT", "aXb"), values().toSet())

            assertTrue(backend.unsetConfig(repository, GitConfigScope.Local, key).ok)
            assertTrue(
                backend.configEntries(repository).none {
                    it.scope == GitConfigScope.Local && it.key.equals(key, ignoreCase = true)
                },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun initRepository(dir: File) {
        val init = backend.run(dir.path, listOf("init", "-b", "main"))
        assertTrue(init.ok, init.stderr)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
    }

    @Test
    fun `cancels a git process tree and reports cancellation`() {
        val dir = Files.createTempDirectory("git-chan-controlled-cancel").toFile()
        try {
            initRepository(dir)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            val started = File(dir, "hook-started")
            val hook = File(dir, ".git/hooks/pre-commit")
            hook.writeText("#!/bin/sh\ntouch hook-started\nsleep 10\n")
            hook.setExecutable(true)

            val control = GitRunControl()
            var result: GitResult? = null
            val worker = thread(name = "controlled-git-test", isDaemon = true) {
                result = backend.runControlled(
                    dir.path,
                    listOf("commit", "-m", "slow hook"),
                    timeoutSeconds = 30,
                    control = control,
                )
            }
            val deadline = System.currentTimeMillis() + 5_000
            while (!started.isFile && System.currentTimeMillis() < deadline) Thread.sleep(20)
            assertTrue(started.isFile, "pre-commit hook did not start")

            control.cancel()
            worker.join(5_000)

            assertFalse(worker.isAlive, "git process was not stopped after cancellation")
            val cancelled = assertNotNull(result)
            assertTrue(cancelled.cancelled)
            assertFalse(cancelled.ok)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun commitAs(
        dir: File,
        message: String,
        author: String,
        email: String,
        date: String,
    ) {
        val result = backend.run(
            cwd = dir.path,
            args = listOf("commit", "-m", message),
            environment = mapOf(
                "GIT_AUTHOR_NAME" to author,
                "GIT_AUTHOR_EMAIL" to email,
                "GIT_AUTHOR_DATE" to date,
                "GIT_COMMITTER_NAME" to author,
                "GIT_COMMITTER_EMAIL" to email,
                "GIT_COMMITTER_DATE" to date,
            ),
        )
        assertTrue(result.ok, result.stderr)
    }
}
