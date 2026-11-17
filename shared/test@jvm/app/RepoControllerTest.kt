package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.DiffLineKind
import org.baconana.mybeautifulgit_chan.domain.FetchOptions
import org.baconana.mybeautifulgit_chan.domain.FetchTags
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.GitMetaFile
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.OperationAction
import org.baconana.mybeautifulgit_chan.domain.PatchExportOptions
import org.baconana.mybeautifulgit_chan.domain.PushForce
import org.baconana.mybeautifulgit_chan.domain.PushMode
import org.baconana.mybeautifulgit_chan.domain.PushOptions
import org.baconana.mybeautifulgit_chan.domain.PullMode
import org.baconana.mybeautifulgit_chan.domain.RebaseCommand
import org.baconana.mybeautifulgit_chan.domain.RebaseTodoEntry
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.RepoStatus
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.StashCreateOptions
import org.baconana.mybeautifulgit_chan.domain.SubmoduleState
import org.baconana.mybeautifulgit_chan.domain.SubmoduleUpdateOptions
import org.baconana.mybeautifulgit_chan.domain.WorktreeAddOptions
import org.baconana.mybeautifulgit_chan.domain.parsePatch
import org.baconana.mybeautifulgit_chan.git.GitBackend
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepoControllerTest {

    @Test
    fun `stash partial create, show, apply, drop and branch from stash`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-stash").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("one\n")
            File(dir, "b.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt", "b.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)

            File(dir, "a.txt").writeText("two\n")
            File(dir, "b.txt").writeText("two\n")
            File(dir, "untracked.txt").writeText("new\n")

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()

            assertTrue(
                controller.createStash(
                    StashCreateOptions(
                        message = "partial a",
                        includeUntracked = true,
                        paths = listOf("a.txt"),
                    ),
                ),
                controller.error ?: "partial stash failed",
            )
            assertEquals(1, controller.stashes.size)
            assertTrue(controller.stashes.single().message.contains("partial a"))
            assertEquals("one", File(dir, "a.txt").readText().trim())
            assertEquals("two", File(dir, "b.txt").readText().trim())
            assertEquals("new", File(dir, "untracked.txt").readText().trim())

            assertTrue(
                controller.createStash(
                    StashCreateOptions("remaining", includeUntracked = true),
                ),
                controller.error ?: "full stash failed",
            )
            assertEquals(2, controller.stashes.size)
            assertTrue(controller.status?.isClean == true)

            val remaining = controller.stashes.first()
            controller.loadStashDetail(remaining.selector)
            val detail = assertNotNull(controller.stashDetail)
            assertTrue(detail.files.any { it.path == "b.txt" })
            assertTrue(detail.files.any { it.path == "untracked.txt" })
            assertTrue(detail.patch.contains("b.txt"))
            assertTrue(detail.patch.contains("untracked.txt"))

            assertTrue(
                controller.applyStash(remaining.selector, pop = false),
                controller.error ?: "stash apply failed",
            )
            assertEquals(2, controller.stashes.size)
            assertTrue(controller.dropStash(remaining.selector), controller.error ?: "stash drop failed")
            assertEquals(1, controller.stashes.size)

            assertTrue(git.run(dir.path, listOf("reset", "--hard", "HEAD")).ok)
            assertTrue(git.run(dir.path, listOf("clean", "-fd")).ok)
            controller.refresh()
            val partial = controller.stashes.single()
            assertTrue(
                controller.branchFromStash(partial.selector, "from-stash"),
                controller.error ?: "stash branch failed",
            )
            controller.refresh()
            assertTrue(
                controller.refs.any {
                    it.kind == RefKind.LocalBranch && it.shortName == "from-stash"
                },
            )
            assertEquals(0, controller.stashes.size)
            assertEquals("two", File(dir, "a.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stash all includes ignored files and pop restores them`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-stash-all").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, ".gitignore").writeText("*.log\n")
            File(dir, "tracked.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", ".gitignore", "tracked.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            File(dir, "ignored.log").writeText("secret\n")

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()
            assertTrue(
                controller.createStash(StashCreateOptions("all", includeIgnored = true)),
                controller.error ?: "stash --all failed",
            )
            assertEquals(1, controller.stashes.size)
            assertFalse(File(dir, "ignored.log").exists())
            val selector = controller.stashes.single().selector
            assertTrue(
                controller.applyStash(selector, pop = true),
                controller.error ?: "stash pop failed",
            )
            assertEquals(0, controller.stashes.size)
            assertEquals("one", File(dir, "tracked.txt").readText().trim())
            assertEquals("secret", File(dir, "ignored.log").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stash staged only keeps unstaged changes`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-stash-staged").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("one\n")
            File(dir, "b.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt", "b.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            File(dir, "a.txt").writeText("two\n")
            File(dir, "b.txt").writeText("two\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()
            assertTrue(
                controller.createStash(StashCreateOptions("staged", stagedOnly = true)),
                controller.error ?: "staged-only stash failed",
            )
            assertEquals(1, controller.stashes.size)
            assertEquals("one", File(dir, "a.txt").readText().trim())
            assertEquals("two", File(dir, "b.txt").readText().trim())
            assertTrue(controller.status?.entries.orEmpty().any { it.path == "b.txt" })
            val selector = controller.stashes.single().selector
            assertTrue(
                controller.applyStash(selector, pop = true),
                controller.error ?: "staged stash pop failed",
            )
            assertEquals(0, controller.stashes.size)
            assertEquals("two", File(dir, "a.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `worktree add branch lock unlock prune and remove`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-worktree-manager").toFile()
        val linked = File(dir.parentFile, "${dir.name}-linked")
        val stale = File(dir.parentFile, "${dir.name}-stale")
        linked.deleteRecursively()
        stale.deleteRecursively()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(git.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()

            assertTrue(
                controller.addWorktree(
                    WorktreeAddOptions(
                        path = linked.path,
                        branch = "side",
                        createBranch = true,
                    ),
                ),
                controller.error ?: "worktree add failed",
            )
            controller.refresh()
            assertEquals(2, controller.worktrees.size)
            val side = controller.worktrees.single { it.branchName == "side" }
            assertTrue(File(linked, "base.txt").isFile)

            assertTrue(
                controller.branchInWorktree(side.path, "feature", create = true),
                controller.error ?: "worktree branch failed",
            )
            controller.refresh()
            val feature = controller.worktrees.single { it.branchName == "feature" }
            assertTrue(controller.lockWorktree(feature.path, "test lock"), controller.error ?: "worktree lock failed")
            controller.refresh()
            assertTrue(controller.worktrees.single { it.branchName == "feature" }.isLocked)
            assertTrue(controller.unlockWorktree(feature.path), controller.error ?: "worktree unlock failed")
            controller.refresh()
            assertFalse(controller.worktrees.single { it.branchName == "feature" }.isLocked)

            assertNotNull(controller.previewWorktreePrune())
            assertTrue(
                controller.addWorktree(
                    WorktreeAddOptions(
                        path = stale.path,
                        branch = "stale",
                        createBranch = true,
                    ),
                ),
                controller.error ?: "stale worktree add failed",
            )
            controller.refresh()
            stale.deleteRecursively()
            controller.refresh()
            assertTrue(controller.worktrees.any { samePath(it.path, stale.path) })
            assertTrue(controller.worktrees.single { samePath(it.path, stale.path) }.isPrunable)
            val prunePreview = assertNotNull(controller.previewWorktreePrune())
            assertTrue(prunePreview.contains(stale.name))
            assertTrue(controller.pruneWorktrees(), controller.error ?: "worktree prune failed")
            controller.refresh()
            assertEquals(2, controller.worktrees.size)

            assertFalse(
                controller.removeWorktree("-not-a-worktree"),
                "a path outside the worktree list must be rejected",
            )
            assertEquals(EnglishStrings.worktreeNotFound, controller.error)
            assertFalse(
                controller.removeWorktree(controller.repository.root),
                "the current worktree must not be removable",
            )
            assertTrue(
                controller.removeWorktree(feature.path, force = false),
                controller.error ?: "worktree remove failed",
            )
            controller.refresh()
            assertEquals(1, controller.worktrees.size)
            assertFalse(linked.exists())
        } finally {
            linked.deleteRecursively()
            stale.deleteRecursively()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `patches format selected export apply check and am`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-patches").toFile()
        val patchesDir = Files.createTempDirectory("git-chan-patches-out").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(git.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = git.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "first.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", "first.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "first patch")).ok)
            val first = git.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "second.txt").writeText("two\n")
            assertTrue(git.run(dir.path, listOf("add", "second.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "second patch")).ok)
            val second = git.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val repository = git.open(dir.path)
            val controller = RepoController(repository, git, git.version())
            controller.refresh()

            val preview = assertNotNull(
                controller.exportPatches(
                    PatchExportOptions(
                        hashes = listOf(second, first),
                        previewOnly = true,
                    ),
                ),
                controller.error ?: "selected format-patch preview failed",
            )
            assertTrue(preview.mailbox.contains("Subject: [PATCH] first patch"))
            assertTrue(preview.mailbox.contains("Subject: [PATCH] second patch"))

            val rangePreview = assertNotNull(
                controller.exportPatches(
                    PatchExportOptions(range = "$base..HEAD", previewOnly = true),
                ),
                controller.error ?: "range format-patch preview failed",
            )
            assertTrue(rangePreview.mailbox.contains("Subject: [PATCH 1/2] first patch"))
            assertTrue(rangePreview.mailbox.contains("Subject: [PATCH 2/2] second patch"))

            val exported = assertNotNull(
                controller.exportPatches(
                    PatchExportOptions(
                        hashes = listOf(second, first),
                        outputDir = patchesDir.path,
                    ),
                ),
                controller.error ?: "selected format-patch export failed",
            )
            assertEquals(2, exported.files.size)
            assertTrue(exported.files.all { File(it).isFile })

            val exportedMbox = File(patchesDir, "selected-export.mbox")
            val mailboxResult = assertNotNull(
                controller.exportPatches(
                    PatchExportOptions(
                        hashes = listOf(second, first),
                        outputFile = exportedMbox.path,
                    ),
                ),
                controller.error ?: "mailbox export failed",
            )
            assertEquals(exportedMbox.path, mailboxResult.outputFile)
            assertTrue(exportedMbox.isFile)
            assertEquals(2, controller.patchMailbox?.messages?.size)

            val plainPatch = File(patchesDir, "change.diff")
            val diff = git.run(dir.path, listOf("diff", base, first))
            assertTrue(diff.ok, diff.stderr)
            plainPatch.writeText(diff.stdout)

            assertTrue(git.run(dir.path, listOf("reset", "--hard", base)).ok)
            assertTrue(git.run(dir.path, listOf("clean", "-fd")).ok)
            controller.refresh()

            assertTrue(
                controller.applyPatchFile(plainPatch.path, check = true, threeWay = false),
                controller.error ?: "git apply --check failed",
            )
            assertTrue(
                controller.applyPatchFile(plainPatch.path, check = true, threeWay = true),
                controller.error ?: "git apply --check --3way failed",
            )
            assertFalse(File(dir, "first.txt").exists())
            assertTrue(
                controller.applyPatchFile(plainPatch.path, check = false, threeWay = true),
                controller.error ?: "git apply --3way failed",
            )
            assertEquals("one", File(dir, "first.txt").readText().trim())

            val mailboxFile = File(patchesDir, "selected.mbox")
            mailboxFile.writeText(preview.mailbox)
            assertTrue(controller.loadPatchMailbox(mailboxFile.path), controller.patchMailboxError ?: "mailbox load failed")
            assertEquals(2, controller.patchMailbox?.messages?.size)

            assertTrue(git.run(dir.path, listOf("reset", "--hard", base)).ok)
            assertTrue(git.run(dir.path, listOf("clean", "-fd")).ok)
            controller.refresh()
            assertTrue(
                controller.applyMailbox(mailboxFile.path, threeWay = false),
                controller.error ?: "git am failed",
            )
            assertEquals("second patch", controller.commits.first().subject)
            assertEquals("one", File(dir, "first.txt").readText().trim())
            assertEquals("two", File(dir, "second.txt").readText().trim())
        } finally {
            patchesDir.deleteRecursively()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `filter-repo preview and rewrite use a dry-run, protect the secret and create backups`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-filter-repo").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "secret.txt").writeText("token=s3cr3t\n")
            File(dir, "keep.txt").writeText("keep\n")
            assertTrue(git.run(dir.path, listOf("add", "secret.txt", "keep.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val repository = git.open(dir.path)
            val backend = FakeFilterRepoBackend(git, repository.gitDir)
            val controller = RepoController(repository, backend, git.version())
            controller.refresh()

            val preview = controller.filterRepoPreview(
                FilterRepoRequest(FilterRepoMode.RemovePath, path = "secret.txt"),
            )
            assertNotNull(preview)
            assertEquals(2, preview.commitsBefore)
            assertEquals(1, preview.commitsAfter)
            assertEquals(1, preview.changedPaths)
            assertTrue(backend.lastArgs.contains("--dry-run"))
            assertTrue(backend.lastArgs.contains("secret.txt"))
            assertTrue(backend.lastArgs.contains("refs/heads/main"))
            assertFalse(backend.lastArgs.any { it.startsWith("refs/mybeautifulgit/") })

            val renamePreview = controller.filterRepoPreview(
                FilterRepoRequest(
                    FilterRepoMode.RenamePath,
                    oldPath = "old/path",
                    newPath = "new/path",
                ),
            )
            assertNotNull(renamePreview)
            assertTrue(backend.lastArgs.contains("--path-rename"))
            assertTrue(backend.lastArgs.contains("old/path:new/path"))

            assertTrue(
                controller.filterRepoRewrite(
                    FilterRepoRequest(
                        mode = FilterRepoMode.ReplaceText,
                        find = "s3cr3t",
                        replacement = "redacted",
                    ),
                ),
                controller.error ?: "filter-repo rewrite failed",
            )
            assertFalse(backend.lastArgs.any { "s3cr3t" in it })
            assertTrue(backend.lastArgs.contains("--refs"))
            assertFalse(backend.lastArgs.contains("--dry-run"))
            assertTrue(backend.expressionsText?.contains("literal:s3cr3t==>redacted") == true)
            assertNotNull(backend.expressionsPath)
            assertFalse(backend.expressionsPath!!.exists())
            assertNull(controller.lastRewriteUndo)
            val backups = git.run(
                dir.path,
                listOf("for-each-ref", "--format=%(refname)", "refs/mybeautifulgit/backups"),
            ).stdout
            assertTrue(backups.isNotBlank())
            assertTrue(
                File(repository.gitDir, "mybeautifulgit/backups")
                    .listFiles()
                    ?.any { it.extension == "bundle" } == true,
            )
            assertNotNull(controller.toast)
            Unit
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `opens status stages shows diff and commits`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-controller").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())

            controller.refresh()
            assertTrue(controller.status?.isClean ?: false)
            assertTrue(controller.commits.isEmpty())
            assertEquals(1, controller.worktrees.size)
            assertEquals("main", controller.worktrees.single().branchName)
            assertTrue(controller.submodules.isEmpty())
            assertEquals(0, controller.stashCount)

            File(dir, "hello.txt").writeText("hello\n")
            controller.refresh()

            val entry = controller.status!!.entries.single { it.path == "hello.txt" }
            assertTrue(entry.isUnstaged)

            controller.select(entry, staged = false)
            assertTrue(controller.diff.contains("+hello"))

            controller.stage(entry)
            assertEquals(1, controller.status?.stagedCount)
            assertEquals(0, controller.status?.unstagedCount)
            val stageToast = assertNotNull(controller.toast)
            assertTrue(stageToast.success)
            assertTrue(stageToast.command.contains("add"))
            assertEquals("staged", stageToast.message)

            controller.commitMessage = "initial commit"
            controller.commit()

            val commit = controller.commits.single()
            assertEquals("initial commit", commit.subject)
            assertTrue(controller.status?.isClean ?: false)
            assertEquals("", controller.commitMessage)
            val commitToast = assertNotNull(controller.toast)
            assertTrue(commitToast.success)
            assertTrue(commitToast.command.contains("commit"))
            assertEquals("commit created", commitToast.message)
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stages and unstages one line with git apply`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-line-stage").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\ntwo\nthree\nfour\nfive\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("one\nTWO\nthree\nfour\nFIVE\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)

            val hunk = parsePatch(controller.diff).hunks.single()
            val added = hunk.lines.single {
                it.kind == DiffLineKind.Added && it.text == "+TWO"
            }
            controller.applyLine(hunk.index, added.lineIndex)

            assertEquals("one\ntwo\nTWO\nthree\nfour\nfive\n", backend.run(dir.path, listOf("show", ":a.txt")).stdout)
            val afterStage = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(afterStage.isStaged)
            assertTrue(afterStage.isUnstaged)

            controller.select(entry, staged = true)
            assertTrue(controller.diff.contains("+TWO"))
            assertFalse(controller.diff.contains("+FIVE"))
            val stagedHunk = parsePatch(controller.diff).hunks.single()
            val stagedAdded = stagedHunk.lines.single {
                it.kind == DiffLineKind.Added && it.text == "+TWO"
            }
            controller.applyLine(stagedHunk.index, stagedAdded.lineIndex)

            assertEquals("one\ntwo\nthree\nfour\nfive\n", backend.run(dir.path, listOf("show", ":a.txt")).stdout)
            val afterUnstage = controller.status!!.entries.single { it.path == "a.txt" }
            assertFalse(afterUnstage.isStaged)
            assertTrue(afterUnstage.isUnstaged)
            assertTrue(controller.diff.contains("+TWO"))
            assertTrue(controller.diff.contains("+FIVE"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stages one line of an untracked file with git apply`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-untracked-line").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "new.txt").writeText("one\ntwo\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "new.txt" }
            controller.select(entry, staged = false)
            val hunk = parsePatch(controller.diff).hunks.single()
            val first = hunk.lines.single {
                it.kind == DiffLineKind.Added && it.text == "+one"
            }
            controller.applyLine(hunk.index, first.lineIndex)

            assertEquals("one\n", backend.run(dir.path, listOf("show", ":new.txt")).stdout)
            val after = controller.status!!.entries.single { it.path == "new.txt" }
            assertTrue(after.isStaged)
            assertTrue(after.isUnstaged)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stages only the selected hunk when a file has two hunks`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-two-hunks").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val original = (1..30).joinToString("\n") { "line$it" } + "\n"
            File(dir, "a.txt").writeText(original)
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText(
                original.replace("line2\n", "TWO\n").replace("line28\n", "TWENTY-EIGHT\n"),
            )

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val hunks = parsePatch(controller.diff).hunks
            assertEquals(2, hunks.size)
            controller.applyHunk(hunks[1].index)

            val staged = backend.run(dir.path, listOf("show", ":a.txt")).stdout
            assertTrue(staged.contains("line2\n"))
            assertTrue(staged.contains("TWENTY-EIGHT\n"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `commits selected hunks and leaves the rest unstaged`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-partial-commit").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val original = (1..30).joinToString("\n") { "line$it" } + "\n"
            File(dir, "a.txt").writeText(original)
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText(
                original.replace("line2\n", "TWO\n").replace("line28\n", "TWENTY-EIGHT\n"),
            )

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val hunks = parsePatch(controller.diff).hunks
            assertEquals(2, hunks.size)

            controller.commitMessage = "partial"
            controller.toggleHunkSelection(hunks[1].index)
            assertTrue(controller.commitSelectedHunks())

            assertEquals("partial", controller.commits.first().subject)
            assertTrue(controller.selectedHunks.isEmpty())
            val after = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(after.isUnstaged)
            assertFalse(after.isStaged)
            val index = backend.run(dir.path, listOf("show", ":a.txt")).stdout
            assertTrue(index.contains("line2\n"))
            assertFalse(index.contains("TWO\n"))
            assertTrue(index.contains("TWENTY-EIGHT\n"))
            assertFalse(index.contains("line28\n"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `commits multiple selected hunks in one patch`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-multi-hunk-commit").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val original = (1..30).joinToString("\n") { "line$it" } + "\n"
            File(dir, "a.txt").writeText(original)
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText(
                original.replace("line2\n", "TWO\nTWO-EXTRA\n").replace("line28\n", "TWENTY-EIGHT\n"),
            )

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val hunks = parsePatch(controller.diff).hunks
            assertEquals(2, hunks.size)

            controller.commitMessage = "both hunks"
            hunks.forEach { controller.toggleHunkSelection(it.index) }
            assertTrue(controller.commitSelectedHunks())

            assertEquals("both hunks", controller.commits.first().subject)
            assertTrue(controller.status?.isClean ?: false)
            val index = backend.run(dir.path, listOf("show", ":a.txt")).stdout
            assertTrue(index.contains("TWO\n"))
            assertTrue(index.contains("TWO-EXTRA\n"))
            assertTrue(index.contains("TWENTY-EIGHT\n"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stages and unstages a whole hunk with git apply`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-hunk-stage").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\ntwo\nthree\nfour\nfive\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("one\nTWO\nthree\nfour\nFIVE\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val hunk = parsePatch(controller.diff).hunks.single()
            controller.applyHunk(hunk.index)

            val stagedEntry = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(stagedEntry.isStaged)
            assertFalse(stagedEntry.isUnstaged)
            assertEquals("one\nTWO\nthree\nfour\nFIVE\n", backend.run(dir.path, listOf("show", ":a.txt")).stdout)

            controller.select(entry, staged = true)
            val stagedHunk = parsePatch(controller.diff).hunks.single()
            controller.applyHunk(stagedHunk.index)

            val after = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(after.isUnstaged)
            assertFalse(after.isStaged)
            assertEquals("one\ntwo\nthree\nfour\nfive\n", backend.run(dir.path, listOf("show", ":a.txt")).stdout)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `discards file worktree changes and keeps staged changes`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-discard-file").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\ntwo\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            File(dir, "a.txt").writeText("ONE\nTWO\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            File(dir, "a.txt").writeText("ONE\nTWO\nTHREE\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(entry.isStaged)
            assertTrue(entry.isUnstaged)
            controller.select(entry, staged = false)

            assertTrue(controller.discard(DiscardTarget(entry, DiscardScope.File)))
            assertEquals("ONE\nTWO\n", backend.run(dir.path, listOf("show", ":a.txt")).stdout)
            assertEquals("ONE\nTWO\n", File(dir, "a.txt").readText().replace("\r\n", "\n"))
            val after = controller.status!!.entries.single { it.path == "a.txt" }
            assertTrue(after.isStaged)
            assertFalse(after.isUnstaged)
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertTrue(toast.command.contains("restore"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `discards an untracked file`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-discard-untracked").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "new.txt").writeText("new\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "new.txt" }
            controller.select(entry, staged = false)

            assertTrue(controller.discard(DiscardTarget(entry, DiscardScope.File)))
            assertFalse(File(dir, "new.txt").exists())
            assertTrue(controller.status?.isClean ?: false)
            assertTrue(assertNotNull(controller.toast).command.contains("clean"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `discards one line and preserves another change in the same hunk`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-discard-line").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\ntwo\nthree\nfour\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("one\nTWO\nTHREE\nfour\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val hunk = parsePatch(controller.diff).hunks.single()
            val line = hunk.lines.single { it.text == "+TWO" }

            assertTrue(
                controller.discard(
                    DiscardTarget(entry, DiscardScope.Lines, hunk.index, setOf(line.lineIndex)),
                ),
            )
            assertEquals("one\ntwo\nTHREE\nfour\n", File(dir, "a.txt").readText().replace("\r\n", "\n"))
            assertTrue(controller.diff.contains("+THREE"))
            assertFalse(controller.diff.contains("+TWO"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `discards a hunk and saves its patch`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-discard-hunk").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val original = (1..30).joinToString("\n") { "line$it" } + "\n"
            File(dir, "a.txt").writeText(original)
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText(
                original.replace("line2\n", "TWO\n").replace("line28\n", "TWENTY-EIGHT\n"),
            )

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(entry, staged = false)
            val first = parsePatch(controller.diff).hunks.first()

            assertTrue(
                controller.discard(
                    DiscardTarget(entry, DiscardScope.Hunk, hunkIndex = first.index),
                    DiscardBackup.Patch,
                ),
            )
            val worktree = File(dir, "a.txt").readText().replace("\r\n", "\n")
            assertTrue(worktree.contains("line2\n"))
            assertFalse(worktree.contains("TWO\n"))
            assertTrue(worktree.contains("TWENTY-EIGHT\n"))

            val patches = File(File(dir, ".git"), "chan-backups").listFiles().orEmpty()
            assertEquals(1, patches.size)
            val saved = patches.single().readText()
            assertTrue(saved.contains("+TWO"))
            assertFalse(saved.contains("+TWENTY-EIGHT"))
            assertTrue(controller.toast?.message?.contains("patch saved") == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `stashes tracked and untracked files before discarding`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-discard-stash").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("one\nchanged\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val tracked = controller.status!!.entries.single { it.path == "a.txt" }
            controller.select(tracked, staged = false)
            assertTrue(controller.discard(DiscardTarget(tracked, DiscardScope.File), DiscardBackup.Stash))
            assertTrue(controller.status?.isClean ?: false)
            assertEquals("one\n", File(dir, "a.txt").readText().replace("\r\n", "\n"))
            assertEquals(1, controller.stashCount)

            File(dir, "new.txt").writeText("new\n")
            controller.refresh()
            val untracked = controller.status!!.entries.single { it.path == "new.txt" }
            controller.select(untracked, staged = false)
            assertTrue(controller.discard(DiscardTarget(untracked, DiscardScope.File), DiscardBackup.Stash))
            assertFalse(File(dir, "new.txt").exists())
            assertEquals(2, controller.stashCount)
            assertTrue(assertNotNull(controller.toast).message.contains("stash"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `adds an untracked file explicitly`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-untracked-add").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "new.txt").writeText("new\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "new.txt" }
            assertTrue(entry.isUnstaged)

            controller.addUntracked(entry)
            val after = controller.status!!.entries.single { it.path == "new.txt" }
            assertTrue(after.isStaged)
            assertFalse(after.isUnstaged)
            assertEquals("new\n", backend.run(dir.path, listOf("show", ":new.txt")).stdout)
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertTrue(toast.command.contains("add"))
            assertEquals("added to index", toast.message)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `adds an untracked path to gitignore and verifies it with check-ignore`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-untracked-ignore").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "tracked.txt").writeText("tracked\n")
            assertTrue(backend.run(dir.path, listOf("add", "tracked.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "dir").mkdirs()
            File(dir, "dir/new#1.txt").writeText("new\n")
            File(dir, ".gitignore").writeText("existing")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val entry = controller.status!!.entries.single { it.path == "dir/new#1.txt" }

            assertTrue(controller.ignore(entry))
            assertEquals(
                "existing\n/dir/new\\#1.txt\n",
                File(dir, ".gitignore").readText().replace("\r\n", "\n"),
            )
            assertTrue(
                backend.run(dir.path, listOf("check-ignore", "-v", "--", "dir/new#1.txt")).ok,
            )
            assertTrue(controller.ignoredEntries.any { it.path == "dir/new#1.txt" })
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertTrue(toast.command.contains("check-ignore"))
            assertEquals("added to .gitignore", toast.message)

            assertTrue(controller.ignore(entry))
            assertEquals(
                "existing\n/dir/new\\#1.txt\n",
                File(dir, ".gitignore").readText().replace("\r\n", "\n"),
            )
            assertEquals("already ignored by git", controller.toast?.message)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `commits with explicit author and committer identities`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-identities").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitMessage = "identity"
            controller.commitAuthor = "Ada Lovelace <ada@example.com>"
            controller.commitCommitter = "Grace Hopper <grace@example.com>"

            assertTrue(controller.commit())
            val commit = controller.commits.first()
            assertEquals("Ada Lovelace", commit.authorName)
            assertEquals("ada@example.com", commit.authorEmail)
            assertEquals("Grace Hopper", commit.committerName)
            assertEquals("grace@example.com", commit.committerEmail)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `keeps configured author when only committer is overridden`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-committer").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitMessage = "committer only"
            controller.commitCommitter = "Grace Hopper <grace@example.com>"

            assertTrue(controller.commit())
            val commit = controller.commits.first()
            assertEquals("Git Chan Test", commit.authorName)
            assertEquals("test@example.com", commit.authorEmail)
            assertEquals("Grace Hopper", commit.committerName)
            assertEquals("grace@example.com", commit.committerEmail)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `retries the exact failed commit after the hook is fixed`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-retry").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            val hook = File(dir, ".git/hooks/pre-commit")
            hook.parentFile.mkdirs()
            hook.writeText("#!/bin/sh\nexit 1\n")
            hook.setExecutable(true)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitMessage = "after hook"
            assertFalse(controller.commit())
            assertEquals(1, controller.commits.size)
            assertNotNull(controller.error)
            assertTrue(controller.retryCommitAvailable)
            assertTrue(controller.retryCommitCommand!!.contains("after hook"))

            controller.commitMessage = "changed after failure"
            hook.writeText("#!/bin/sh\nexit 0\n")
            assertTrue(controller.retryCommit())
            assertEquals(2, controller.commits.size)
            assertEquals("after hook", controller.commits.first().subject)
            assertFalse(controller.retryCommitAvailable)
            assertNull(controller.retryCommitCommand)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `skips a failing pre-commit hook with no-verify`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-no-verify").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            val hook = File(dir, ".git/hooks/pre-commit")
            hook.parentFile.mkdirs()
            hook.writeText("#!/bin/sh\nexit 1\n")
            hook.setExecutable(true)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitMessage = "hooked"
            assertFalse(controller.commit())
            assertNotNull(controller.error)
            assertEquals(1, controller.commits.size)

            controller.noVerify = true
            assertTrue(controller.commit())
            assertEquals(2, controller.commits.size)
            assertTrue(controller.toast!!.command.contains("--no-verify"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `missing commit template reports a localized error`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-template-missing").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.commitTemplatePath = "missing-template.txt"

            assertFalse(controller.loadCommitTemplate())
            assertEquals(EnglishStrings.repoCommitTemplateMissing, controller.error)
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `filter-repo preview rejects oversized fast-export output`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-filter-preview-large").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "secret.txt").writeText("secret\n")
            assertTrue(git.run(dir.path, listOf("add", "secret.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val repository = git.open(dir.path)
            val backend = FakeFilterRepoBackend(git, repository.gitDir).apply {
                oversizedPreview = true
            }
            val controller = RepoController(repository, backend, git.version())
            controller.refresh()

            val preview = controller.filterRepoPreview(
                FilterRepoRequest(FilterRepoMode.RemovePath, path = "secret.txt"),
            )

            assertNull(preview)
            assertEquals(EnglishStrings.repoFileTooLarge, controller.error)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `newer selection wins when an older diff finishes last`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-stale-diff").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("base-a\n")
            File(dir, "b.txt").writeText("base-b\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt", "b.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            File(dir, "a.txt").writeText("changed-a\n")
            File(dir, "b.txt").writeText("changed-b\n")

            val repository = git.open(dir.path)
            val slowBackend = SlowDiffBackend(git, slowPath = "a.txt")
            val controller = RepoController(repository, slowBackend, git.version())
            controller.refresh()
            val a = controller.status!!.entries.single { it.path == "a.txt" }
            val b = controller.status!!.entries.single { it.path == "b.txt" }

            val first = async { controller.select(a, staged = false) }
            delay(100)
            val second = async { controller.select(b, staged = false) }
            first.await()
            second.await()

            assertEquals("b.txt", controller.selectedPath)
            assertTrue(controller.diff.contains("changed-b"), controller.diff)
            assertFalse(controller.diff.contains("changed-a"), controller.diff)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads a commit template into the message`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-template").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)
            File(dir, "template.txt").writeText("Template subject\n\nBody\n# remove me\n")
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitTemplatePath = "template.txt"
            assertTrue(controller.loadCommitTemplate())
            assertEquals("Template subject\n\nBody", controller.commitMessage)

            assertTrue(controller.commit())
            assertEquals("Template subject", controller.commits.first().subject)
            assertTrue(controller.toast!!.command.contains("--template=template.txt"))
            assertEquals("", controller.commitTemplatePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `creates fixup and squash commits from the target`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-fixup").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val base = controller.commits.first().hash
            File(dir, "a.txt").writeText("fixup\n")
            controller.refresh()
            val fixupEntry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.stage(fixupEntry)
            controller.commitMode = CommitMode.Fixup
            controller.fixupTarget = base

            assertTrue(controller.commit())
            assertTrue(controller.commits.first().subject.startsWith("fixup! initial"))
            assertEquals(CommitMode.Normal, controller.commitMode)
            assertEquals("", controller.fixupTarget)

            File(dir, "a.txt").writeText("squash\n")
            controller.refresh()
            val squashEntry = controller.status!!.entries.single { it.path == "a.txt" }
            controller.stage(squashEntry)
            controller.commitMode = CommitMode.Squash
            controller.fixupTarget = base

            assertTrue(controller.commit())
            assertTrue(controller.commits.first().subject.startsWith("squash! initial"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads and edits config entries with scope and type validation`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-config-editor").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val controller = RepoController(backend.open(dir.path), backend, backend.version())

            val initial = controller.loadConfigEntries()
            assertTrue(initial.any { it.key.equals("user.name", ignoreCase = true) })

            assertTrue(controller.saveConfigEntry(GitConfigScope.Local, "alias.co", "checkout"))
            assertTrue(
                controller.saveConfigEntry(
                    GitConfigScope.Local,
                    "thirdparty.unknownKey",
                    "kept",
                ),
            )
            assertTrue(
                controller.saveConfigEntry(
                    GitConfigScope.Local,
                    "includeIf.gitdir:C:/work/.path",
                    "C:/work/.gitconfig",
                ),
            )
            assertTrue(controller.saveConfigEntry(GitConfigScope.Local, "commit.gpgsign", "true"))

            assertFalse(
                controller.saveConfigEntry(GitConfigScope.Local, "commit.gpgsign", "maybe"),
            )
            assertEquals(EnglishStrings.configBooleanInvalid, controller.error)

            val loaded = controller.loadConfigEntries()
            assertTrue(loaded.any { it.key == "alias.co" && it.scope == GitConfigScope.Local })
            assertTrue(
                loaded.any {
                    it.key.equals("thirdparty.unknownKey", ignoreCase = true) &&
                        it.value == "kept"
                },
            )
            assertTrue(loaded.any { it.isIncludeIf && it.scope == GitConfigScope.Local })

            assertTrue(controller.deleteConfigEntry(GitConfigScope.Local, "alias.co"))
            assertTrue(
                controller.loadConfigEntries().none {
                    it.key.equals("alias.co", ignoreCase = true) && it.scope == GitConfigScope.Local
                },
            )
            assertTrue(controller.saveConfigEntry(GitConfigScope.Local, "commit.gpgsign", "false"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `edits one multivalue config value and keeps the rest`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-multivalue-editor").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            val key = "thirdparty.multivalue"

            assertTrue(controller.addConfigEntry(GitConfigScope.Local, key, "one"))
            assertTrue(controller.addConfigEntry(GitConfigScope.Local, key, "two"))
            assertTrue(
                controller.saveConfigEntry(
                    GitConfigScope.Local,
                    key,
                    "ONE",
                    oldValue = "one",
                ),
            )
            suspend fun values() = controller.loadConfigEntries()
                .filter {
                    it.scope == GitConfigScope.Local && it.key.equals(key, ignoreCase = true)
                }
                .map { it.value }

            assertEquals(setOf("ONE", "two"), values().toSet())
            assertTrue(controller.deleteConfigEntry(GitConfigScope.Local, key, oldValue = "two"))
            assertEquals(listOf("ONE"), values())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `requires confirmation for system and global config writes`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-config-confirm").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            val key = "alias.confirm"

            assertFalse(controller.saveConfigEntry(GitConfigScope.System, key, "status"))
            assertEquals(EnglishStrings.configScopeConfirmationRequired, controller.error)

            assertFalse(controller.addConfigEntry(GitConfigScope.Global, key, "status"))
            assertEquals(EnglishStrings.configScopeConfirmationRequired, controller.error)

            assertFalse(
                controller.deleteConfigEntry(
                    GitConfigScope.Global,
                    key,
                    oldValue = "status",
                ),
            )
            assertEquals(EnglishStrings.configScopeConfirmationRequired, controller.error)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads and saves git meta files with validation`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-meta-files").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val controller = RepoController(backend.open(dir.path), backend, backend.version())

            val missing = assertNotNull(controller.loadGitMetaFile(GitMetaFile.GitIgnore))
            assertFalse(missing.exists)
            assertEquals("", missing.text)

            assertFalse(controller.saveGitMetaFile(GitMetaFile.GitIgnore, "!\n"))
            assertEquals(EnglishStrings.gitMetaValidationBlocked, controller.error)
            assertFalse(File(dir, ".gitignore").exists())

            assertTrue(controller.saveGitMetaFile(GitMetaFile.GitIgnore, "*.log\n!important.log\n"))
            assertEquals("*.log\n!important.log\n", File(dir, ".gitignore").readText())
            val ignore = assertNotNull(controller.loadGitMetaFile(GitMetaFile.GitIgnore))
            assertTrue(ignore.exists)
            assertEquals("*.log\n!important.log\n", ignore.text)

            assertTrue(controller.saveGitMetaFile(GitMetaFile.GitAttributes, "*.txt text\n"))
            assertTrue(
                controller.saveGitMetaFile(
                    GitMetaFile.Mailmap,
                    "Proper Name <commit@example.com>\n",
                ),
            )
            assertEquals("*.txt text\n", File(dir, ".gitattributes").readText())
            assertEquals(
                "Proper Name <commit@example.com>\n",
                File(dir, ".mailmap").readText(),
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `runs an arbitrary git command in the repository`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-command").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())

            assertTrue(controller.runCommand("git status --short"))
            assertTrue(controller.log.any { it.kind == LogKind.Command && it.text.contains("status --short") })
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertTrue(toast.command.contains("status --short"))
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `runs a configured git alias as a command`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-alias").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val repository = backend.open(dir.path)
            assertTrue(
                backend.setConfig(
                    repository,
                    GitConfigScope.Local,
                    "alias.hello",
                    "version",
                ).ok,
            )

            val controller = RepoController(repository, backend, backend.version())
            val alias = assertNotNull(controller.loadAliases().singleOrNull { it.name == "hello" })
            assertTrue(controller.runAlias(alias))
            assertTrue(
                controller.log.any { it.kind == LogKind.Output && it.text.contains("git version") },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `detects and removes index lock`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-lock").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            assertNull(controller.indexLock)

            File(File(dir, ".git"), "index.lock").writeText("")
            controller.refresh()
            assertNotNull(controller.indexLock)

            controller.removeIndexLock()
            assertNull(controller.indexLock)
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `filters history by mine and current branch`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-log-filter-controller").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "side.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "side.txt")).ok)
            val other = backend.run(
                cwd = dir.path,
                args = listOf("commit", "-m", "side"),
                environment = mapOf(
                    "GIT_AUTHOR_NAME" to "Other",
                    "GIT_AUTHOR_EMAIL" to "other@example.com",
                    "GIT_COMMITTER_NAME" to "Other",
                    "GIT_COMMITTER_EMAIL" to "other@example.com",
                ),
            )
            assertTrue(other.ok, other.stderr)

            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "main.txt").writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("add", "main.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertEquals(setOf("main", "base"), controller.commits.map { it.subject }.toSet())

            assertTrue(controller.applyLogFilter(LogFilter(onlyCurrent = false)))
            assertFalse(controller.logFilter.onlyCurrent)
            assertEquals(
                setOf("main", "base", "side"),
                controller.commits.map { it.subject }.toSet(),
            )

            assertTrue(controller.applyLogFilter(LogFilter(onlyMine = true)))
            assertEquals(setOf("main", "base"), controller.commits.map { it.subject }.toSet())

            assertTrue(controller.applyLogFilter(LogFilter()))
            assertEquals(setOf("main", "base"), controller.commits.map { it.subject }.toSet())
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads selected commit inspection with diff stats and signature`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-inspection").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "hello.txt").writeText("hello\n")
            assertTrue(backend.run(dir.path, listOf("add", "hello.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val commit = controller.commits.single()
            assertNull(controller.commitInspection)

            controller.loadCommitInspection(commit.hash)

            val inspection = assertNotNull(controller.commitInspection)
            assertEquals(commit.hash, inspection.hash)
            assertTrue(inspection.diff.contains("+hello"))
            assertEquals("hello.txt", inspection.stats.files.single().path)
            assertEquals(1, inspection.stats.additions)
            assertEquals(CommitSignatureStatus.NoSignature, inspection.signature.status)
            assertFalse(controller.commitInspectionLoading)
            assertNull(controller.commitInspectionError)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `loads file history blame and grep for navigation`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-file-search-controller").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "first")).ok)
            assertTrue(backend.run(dir.path, listOf("mv", "a.txt", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "rename")).ok)
            File(dir, "b.txt").writeText("one\nTWO\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "change")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()

            controller.loadFileHistory("b.txt")
            assertEquals("b.txt", controller.fileHistoryPath)
            assertFalse(controller.fileHistoryLoading)
            assertNull(controller.fileHistoryError)
            assertEquals(
                listOf("change", "rename", "first"),
                controller.fileHistoryCommits.map { it.subject },
            )

            controller.loadBlame("b.txt")
            assertFalse(controller.blameLoading)
            assertNull(controller.blameError)
            assertEquals(2, controller.blameResult?.lines?.size)
            assertEquals("TWO", controller.blameResult?.lines?.get(1)?.text)

            controller.runGrep("TWO", "b.txt")
            assertFalse(controller.grepLoading)
            assertNull(controller.grepError)
            assertEquals(1, controller.grepResult?.matches?.size)
            assertEquals("b.txt", controller.grepResult?.matches?.single()?.path)

            val commit = assertNotNull(controller.commitForNavigation(controller.fileHistoryCommits.first().hash))
            assertEquals("change", commit.subject)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `creates branch and tag at commit and loads reflog`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-refs").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "next.txt").writeText("next\n")
            assertTrue(backend.run(dir.path, listOf("add", "next.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "next")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.createBranch("feature/actions", base))
            assertTrue(controller.createTag("v1-actions", "release", base))

            assertTrue(controller.refs.any { it.kind == RefKind.LocalBranch && it.shortName == "feature/actions" })
            assertTrue(controller.refs.any { it.kind == RefKind.Tag && it.shortName == "v1-actions" })
            assertEquals(base, backend.run(dir.path, listOf("rev-parse", "feature/actions")).stdout.trim())

            controller.loadCommitReflog(base)
            assertFalse(controller.reflogLoading)
            assertNull(controller.reflogError)
            assertTrue(controller.reflogEntries.isNotEmpty())
            assertTrue(controller.reflogEntries.all { it.hash == base })

            controller.loadReflog()
            assertFalse(controller.reflogLoading)
            assertNull(controller.reflogError)
            assertNull(controller.reflogHash)
            assertTrue(controller.reflogEntries.any { it.hash == base })
            assertTrue(controller.reflogEntries.any { it.hash != base })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `checks out a commit and resets a branch`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-reset").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "one")).ok)
            val first = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "two")).ok)
            val second = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.checkoutCommit(first))
            assertTrue(controller.status?.branch?.detached == true)

            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            controller.refresh()
            assertTrue(controller.resetCommit(first, ResetMode.Soft))
            assertEquals(first, controller.status?.branch?.oid)
            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(second, undo.backup.previousTip)
            assertTrue(backend.run(dir.path, listOf("show-ref", "--verify", undo.backup.ref)).ok)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(second, controller.status?.branch?.oid)
            assertEquals(1, controller.stashCount)
            assertNull(controller.toast?.undo)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `deletes a commit with reset hard and undo restores the tail`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-delete-reset").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            val base = commitFile("base.txt")
            commitFile("one.txt")
            val two = commitFile("two.txt")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val one = controller.commits.first { it.subject == "one" }
            assertTrue(controller.deleteCommitByReset(one), controller.error ?: "delete failed")
            assertEquals(base, controller.status?.branch?.oid)
            assertEquals(listOf("base"), controller.commits.map { it.subject })
            assertFalse(File(dir, "one.txt").exists())
            assertFalse(File(dir, "two.txt").exists())

            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(two, undo.backup.previousTip)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(listOf("two", "one", "base"), controller.commits.map { it.subject })

            val root = controller.commits.last()
            assertFalse(controller.deleteCommitByReset(root))
            assertEquals("A root commit has no parent; reset --hard cannot delete it. Use drop, revert or filter-repo.", controller.error)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `amends the current commit and undoes from the backup ref`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-amend-undo").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "one")).ok)
            val originalTip = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            controller.amend = true
            controller.commitMessage = "one amended"
            assertTrue(controller.commit(), controller.error ?: "amend failed")
            assertEquals("one amended", controller.commits.first().subject)

            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(originalTip, undo.backup.previousTip)
            assertTrue(backend.run(dir.path, listOf("show-ref", "--verify", undo.backup.ref)).ok)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(originalTip, controller.status?.branch?.oid)
            assertEquals("one", controller.commits.first().subject)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `resets with keep and preserves unrelated local changes`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-keep").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            File(dir, "b.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val first = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "b.txt").writeText("second\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            File(dir, "a.txt").writeText("local\n")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val preview = assertNotNull(controller.resetPreview(first))
            assertEquals(1, preview.lostCommits)
            assertEquals(listOf("a.txt"), preview.unstagedPaths)
            assertTrue(preview.keepConflicts.isEmpty())
            assertTrue(preview.unreachable.objectCount > 0)
            assertNull(preview.unreachable.fsckError)
            val plan = assertNotNull(controller.unreachablePlan(listOf(first)))
            assertEquals(1, plan.commitCount)
            assertEquals(listOf("second"), plan.commits.map { it.subject })

            assertTrue(controller.resetCommit(first, ResetMode.Keep))
            assertEquals(first, controller.status?.branch?.oid)
            assertEquals("local", File(dir, "a.txt").readText().trimEnd())
            assertEquals("base", File(dir, "b.txt").readText().trimEnd())
            assertTrue(controller.status?.entries?.single { it.path == "a.txt" }?.isUnstaged == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `keep reset aborts when local changes overlap the target`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-keep-abort").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val first = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            File(dir, "a.txt").writeText("second\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "second")).ok)
            val second = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "a.txt").writeText("local\n")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val preview = assertNotNull(controller.resetPreview(first))
            assertEquals(listOf("a.txt"), preview.keepConflicts)

            assertFalse(controller.resetCommit(first, ResetMode.Keep))
            assertNotNull(controller.error)
            assertEquals(second, controller.status?.branch?.oid)
            assertEquals("local", File(dir, "a.txt").readText().trimEnd())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rewords a middle commit changes author and replays descendants`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-reword").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            File(dir, "a.txt").writeText("target\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "target")).ok)
            val target = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "b.txt").writeText("tip\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "tip")).ok)
            val originalTip = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val original = controller.commits.first { it.hash == target }
            assertTrue(
                controller.rewordCommit(
                    original,
                    "target reworded\n\nwith body",
                    "New Author <new@example.com>",
                ),
            )

            val reworded = controller.commits.first { it.subject == "target reworded" }
            assertTrue(reworded.hash != target)
            assertEquals("New Author", reworded.authorName)
            assertEquals("new@example.com", reworded.authorEmail)
            assertEquals(original.authorTime, reworded.authorTime)
            assertTrue(controller.commits.any { it.subject == "tip" })
            assertEquals("target", File(dir, "a.txt").readText().trimEnd())
            assertEquals("tip", File(dir, "b.txt").readText().trimEnd())
            assertTrue(controller.status?.isClean == true)
            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(originalTip, undo.backup.previousTip)
            assertTrue(backend.run(dir.path, listOf("show-ref", "--verify", undo.backup.ref)).ok)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(originalTip, controller.status?.branch?.oid)
            assertTrue(controller.commits.any { it.hash == target })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rewords the root commit`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-root-reword").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("root\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "root")).ok)
            val root = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val original = controller.commits.single()
            assertTrue(controller.rewordCommit(original, "root reworded", "Root Author <root@example.com>"))

            val reworded = controller.commits.single()
            assertTrue(reworded.hash != root)
            assertEquals("root reworded", reworded.subject)
            assertEquals("Root Author", reworded.authorName)
            assertTrue(reworded.parents.isEmpty())
            assertEquals("root", File(dir, "a.txt").readText().trimEnd())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rewords a merge commit without replaying descendants`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-merge-reword").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
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
            assertTrue(
                backend.run(
                    dir.path,
                    listOf("merge", "--no-ff", "side", "-m", "merge side"),
                ).ok,
            )

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val merge = controller.commits.first { it.parents.size == 2 }
            assertTrue(controller.rewordCommit(merge, "merge reworded", merge.authorName + " <" + merge.authorEmail + ">"))

            val reworded = controller.commits.first { it.subject == "merge reworded" }
            assertEquals(2, reworded.parents.size)
            assertTrue(reworded.hash != merge.hash)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `splits a middle commit and replays descendants`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-split").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            File(dir, "a.txt").writeText("target\n")
            File(dir, "b.txt").writeText("target\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "target")).ok)
            val target = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "c.txt").writeText("tip\n")
            assertTrue(backend.run(dir.path, listOf("add", "c.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "tip")).ok)
            val originalTip = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val selected = controller.commits.first { it.hash == target }
            assertTrue(
                controller.startCommitRewrite(
                    selected,
                    RewriteMode.Split,
                    "",
                    "Git Chan Test <test@example.com>",
                ),
            )
            assertNotNull(controller.pendingRewrite)
            assertTrue(controller.status?.entries?.any { it.path == "a.txt" && it.isUnstaged } == true)
            assertTrue(controller.status?.entries?.any { it.path == "b.txt" && it.isUnstaged } == true)

            controller.stage(controller.status!!.entries.first { it.path == "a.txt" })
            controller.commitMessage = "split one"
            assertTrue(controller.commit())
            controller.stage(controller.status!!.entries.first { it.path == "b.txt" })
            controller.commitMessage = "split two"
            assertTrue(controller.commit())

            assertTrue(controller.finishCommitRewrite())
            assertNull(controller.pendingRewrite)
            assertEquals(
                listOf("tip", "split two", "split one", "base"),
                controller.commits.map { it.subject },
            )
            assertTrue(controller.commits.none { it.hash == target })
            assertEquals("target", File(dir, "a.txt").readText().trimEnd())
            assertEquals("tip", File(dir, "c.txt").readText().trimEnd())
            assertTrue(controller.status?.isClean == true)
            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(originalTip, undo.backup.previousTip)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(originalTip, controller.status?.branch?.oid)
            assertTrue(controller.commits.any { it.hash == target })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `concurrent refresh calls complete without operation deadlock`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-concurrent-refresh").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val backend = SlowStatusBackend(git)
            val controller = RepoController(git.open(dir.path), backend, git.version())
            controller.refresh()
            val first = async { controller.refresh() }
            val second = async { controller.refresh() }
            withTimeout(10_000) {
                first.await()
                second.await()
            }
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `busy stays set while a nested refresh runs`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-nested-busy").toFile()
        try {
            val git = CliGitBackend()
            initRepository(dir, git)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(git.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "base")).ok)
            File(dir, "a.txt").writeText("target\n")
            assertTrue(git.run(dir.path, listOf("commit", "-am", "target")).ok)
            val target = git.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "c.txt").writeText("tip\n")
            assertTrue(git.run(dir.path, listOf("add", "c.txt")).ok)
            assertTrue(git.run(dir.path, listOf("commit", "-m", "tip")).ok)

            val probe = BusyProbeBackend(git)
            val controller = RepoController(git.open(dir.path), probe, git.version())
            probe.controller = controller
            controller.refresh()
            val selected = controller.commits.first { it.hash == target }
            assertTrue(
                controller.startCommitRewrite(
                    selected,
                    RewriteMode.Split,
                    "",
                    "Git Chan Test <test@example.com>",
                ),
            )
            controller.stage(controller.status!!.entries.first { it.path == "a.txt" })
            controller.commitMessage = "split one"
            assertTrue(controller.commit(), controller.error ?: "commit failed")

            probe.reset()
            assertTrue(controller.finishCommitRewrite(), controller.error ?: "finish failed")
            assertTrue(
                probe.observedBusyAfterRefresh,
                "nested refresh cleared busy before the outer rewrite finished",
            )
            assertFalse(controller.busy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `starts amend with staged target changes and replays descendants`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-amend").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            File(dir, "a.txt").writeText("target\n")
            File(dir, "b.txt").writeText("target\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "target")).ok)
            val target = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "c.txt").writeText("tip\n")
            assertTrue(backend.run(dir.path, listOf("add", "c.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "tip")).ok)
            val originalTip = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val selected = controller.commits.first { it.hash == target }
            assertTrue(
                controller.startCommitRewrite(
                    selected,
                    RewriteMode.Amend,
                    "amended target",
                    "Git Chan Test <test@example.com>",
                ),
            )
            assertTrue((controller.status?.stagedCount ?: 0) > 0)
            assertEquals("amended target", controller.commitMessage)

            File(dir, "a.txt").writeText("amended\n")
            controller.stageAll()
            controller.commitMessage = "amended target"
            assertTrue(controller.commit())
            assertTrue(controller.finishCommitRewrite())

            assertNull(controller.pendingRewrite)
            assertEquals(
                listOf("tip", "amended target", "base"),
                controller.commits.map { it.subject },
            )
            assertTrue(controller.commits.none { it.hash == target })
            assertEquals("amended", File(dir, "a.txt").readText().trimEnd())
            val undo = assertNotNull(controller.toast?.undo)
            assertEquals(originalTip, undo.backup.previousTip)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(originalTip, controller.status?.branch?.oid)
            assertTrue(controller.commits.any { it.hash == target })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `cherry-picks and reverts a commit`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-pick").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "side.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("add", "side.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "side")).ok)
            val side = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.cherryPickCommit(side))
            assertTrue(controller.commits.any { it.subject == "side" })

            val picked = controller.commits.first().hash
            assertTrue(controller.revertCommit(picked))
            assertTrue(controller.commits.first().subject.contains("Revert"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `cherry-picks a range without commits`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-range-no-commit").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "one.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "one.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "one")).ok)
            File(dir, "two.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "two.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "two")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val tip = controller.status!!.branch!!.oid
            val commitCount = controller.commits.size

            assertTrue(
                controller.cherryPickRange("side~2..side", noCommit = true),
                controller.error ?: "range failed",
            )

            assertNull(controller.pendingNoCommit)
            assertEquals(tip, controller.status!!.branch!!.oid)
            assertEquals(commitCount, controller.commits.size)
            assertEquals(2, controller.status!!.stagedCount)
            assertEquals("one", File(dir, "one.txt").readText().trim())
            assertEquals("two", File(dir, "two.txt").readText().trim())
            assertTrue(controller.toast?.success == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reverts a range without commits`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-revert-range-no-commit").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "one")).ok)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "two")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val tip = controller.status!!.branch!!.oid
            val commitCount = controller.commits.size

            assertTrue(
                controller.revertRange("HEAD~2..HEAD", noCommit = true),
                controller.error ?: "revert range failed",
            )

            assertNull(controller.pendingNoCommit)
            assertEquals(tip, controller.status!!.branch!!.oid)
            assertEquals(commitCount, controller.commits.size)
            assertEquals("base", File(dir, "a.txt").readText().trim())
            assertEquals(1, controller.status!!.stagedCount)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `resolves a cherry-pick range conflict in progress`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-range-conflict").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "f.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "f.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "f.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "side")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "f.txt").writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertFalse(controller.cherryPickRange("main..side"))
            assertEquals(GitOperationKind.CherryPick, controller.operation?.kind)
            val conflict = controller.status!!.entries.single { it.isConflicted }
            assertNotNull(controller.mergeEditor)

            File(dir, conflict.path).writeText("resolved\n")
            controller.stage(conflict)
            assertTrue(
                controller.operationAction(OperationAction.Continue),
                controller.error ?: "continue failed",
            )

            assertNull(controller.operation)
            assertEquals("side", controller.commits.first().subject)
            assertEquals("resolved", File(dir, "f.txt").readText().trim())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `continues a no-commit range after a conflict`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-range-no-commit-conflict").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "f.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "f.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            File(dir, "f.txt").writeText("side\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "side one")).ok)
            File(dir, "two.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "two.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "side two")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            File(dir, "f.txt").writeText("main\n")
            assertTrue(backend.run(dir.path, listOf("commit", "-am", "main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val tip = controller.status!!.branch!!.oid
            val commitCount = controller.commits.size

            assertFalse(controller.cherryPickRange("side~2..side", noCommit = true))
            val pending = assertNotNull(controller.pendingNoCommit)
            assertEquals(1, pending.remaining.size)
            assertNull(controller.operation)
            val conflict = controller.status!!.entries.single { it.isConflicted }
            assertNotNull(controller.mergeEditor)

            File(dir, conflict.path).writeText("resolved\n")
            controller.stage(conflict)
            assertTrue(
                controller.continueNoCommit(),
                controller.error ?: "continue failed",
            )

            assertNull(controller.pendingNoCommit)
            assertEquals(tip, controller.status!!.branch!!.oid)
            assertEquals(commitCount, controller.commits.size)
            assertEquals("resolved", File(dir, "f.txt").readText().trim())
            assertEquals("two", File(dir, "two.txt").readText().trim())
            assertEquals(2, controller.status!!.stagedCount)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `marks good and bad commits during bisect`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-commit-actions-bisect").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            repeat(3) { index ->
                File(dir, "c$index.txt").writeText("$index\n")
                assertTrue(backend.run(dir.path, listOf("add", "c$index.txt")).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", "c$index")).ok)
            }

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val oldest = controller.commits.last().hash
            val newest = controller.commits.first().hash
            assertTrue(controller.markBisect(oldest, good = true))
            assertEquals(GitOperationKind.Bisect, controller.operation?.kind)
            assertTrue(controller.markBisect(newest, good = false))
            assertEquals(GitOperationKind.Bisect, controller.operation?.kind)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `bisect start skip reset and automatic command`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-bisect-tab").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "broken.txt").writeText("broken\n")
            assertTrue(backend.run(dir.path, listOf("add", "broken.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "good base")).ok)
            val good = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            repeat(4) { index ->
                File(dir, "step$index.txt").writeText("$index\n")
                assertTrue(backend.run(dir.path, listOf("add", "step$index.txt")).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", "step $index")).ok)
            }

            File(dir, "broken.txt").delete()
            assertTrue(backend.run(dir.path, listOf("add", "-A")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "remove marker")).ok)
            val firstBad = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            File(dir, "later.txt").writeText("later\n")
            assertTrue(backend.run(dir.path, listOf("add", "later.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "later bad")).ok)
            val bad = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertTrue(controller.startBisect(bad, good), controller.error ?: "bisect start failed")
            assertEquals(GitOperationKind.Bisect, controller.operation?.kind)
            assertTrue(controller.bisect.active)
            assertTrue(controller.bisect.good.any { it.hash == good })
            assertTrue(controller.bisect.bad.any { it.hash == bad })
            val candidate = assertNotNull(controller.bisect.currentHash.takeIf { it.isNotBlank() })
            assertTrue(controller.skipBisect(), controller.error ?: "bisect skip failed")
            assertTrue(controller.bisect.skipped.any { it.hash == candidate })
            assertTrue(controller.resetBisect(), controller.error ?: "bisect reset failed")
            assertNull(controller.operation)
            assertFalse(controller.bisect.active)

            assertTrue(controller.startBisect(bad, good), controller.error ?: "second bisect start failed")
            assertTrue(
                controller.runBisectAutomation("git grep -q broken"),
                controller.error ?: "bisect automation failed",
            )
            val found = assertNotNull(controller.bisect.firstBad)
            assertEquals(firstBad, found.hash)
            assertFalse(controller.bisectAutoRunning)
            assertTrue(controller.bisectAutoLog.contains("[exit 0]"))
            assertTrue(controller.bisectAutoLog.contains("[exit 1]"))
            assertTrue(controller.bisectAutoResult?.contains(firstBad.take(7)) == true)
            assertTrue(controller.resetBisect(), controller.error ?: "bisect reset after automation failed")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `manages local and remote branches and compares diverged history`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-branch-manager").toFile()
        val remote = File(dir.parentFile, "${dir.name}-remote")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertTrue(controller.createBranchAtHead("feature/local"))
            assertTrue(controller.refs.any {
                it.kind == RefKind.LocalBranch && it.shortName == "feature/local"
            })
            assertTrue(controller.renameBranch("feature/local", "feature/renamed"))
            assertTrue(controller.refs.any {
                it.kind == RefKind.LocalBranch && it.shortName == "feature/renamed"
            })
            assertTrue(controller.deleteLocalBranch("feature/renamed", force = true))
            assertFalse(controller.refs.any {
                it.kind == RefKind.LocalBranch && it.shortName == "feature/renamed"
            })

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "feature/a")).ok)
            File(dir, "a.txt").writeText("a\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "a")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "feature/b")).ok)
            File(dir, "b.txt").writeText("b\n")
            assertTrue(backend.run(dir.path, listOf("add", "b.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "b")).ok)
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            controller.refresh()

            assertTrue(controller.compareBranches("feature/a", "feature/b"))
            assertFalse(controller.branchCompareLoading)
            assertNull(controller.branchCompareError)
            assertEquals(1, controller.branchComparison?.ahead)
            assertEquals(1, controller.branchComparison?.behind)
            assertEquals(2, controller.branchComparison?.commits?.size)

            assertTrue(remote.mkdirs())
            assertTrue(backend.run(remote.path, listOf("init", "--bare")).ok)
            assertTrue(backend.run(dir.path, listOf("remote", "add", "origin", remote.path)).ok)
            controller.refresh()
            assertTrue(controller.remotes.contains("origin"))

            assertTrue(controller.pushBranch("main", "origin", "main"))
            controller.refresh()
            assertTrue(controller.refs.any {
                it.kind == RefKind.RemoteBranch && it.shortName == "origin/main"
            })
            assertTrue(controller.setBranchUpstream("main", "origin/main"))
            controller.refresh()
            assertEquals(
                "refs/remotes/origin/main",
                controller.refs.single { it.kind == RefKind.LocalBranch && it.isHead }.upstream,
            )

            assertTrue(controller.pushBranch("feature/a", "origin", "feature/a"))
            controller.refresh()
            assertTrue(controller.refs.any {
                it.kind == RefKind.RemoteBranch && it.shortName == "origin/feature/a"
            })
            assertTrue(controller.renameRemoteBranch("origin", "feature/a", "feature/renamed-remote"))
            controller.refresh()
            assertTrue(controller.refs.any {
                it.kind == RefKind.RemoteBranch && it.shortName == "origin/feature/renamed-remote"
            })
            assertFalse(controller.refs.any {
                it.kind == RefKind.RemoteBranch && it.shortName == "origin/feature/a"
            })
            assertTrue(controller.deleteRemoteBranch("origin", "feature/renamed-remote"))
            controller.refresh()
            assertFalse(controller.refs.any {
                it.kind == RefKind.RemoteBranch && it.shortName == "origin/feature/renamed-remote"
            })
        } finally {
            dir.deleteRecursively()
            remote.deleteRecursively()
        }
    }

    @Test
    fun `deletes tag and branch names starting with dash`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-dash-refs").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(backend.run(dir.path, listOf("update-ref", "refs/tags/-foo", "HEAD")).ok)
            assertTrue(backend.run(dir.path, listOf("update-ref", "refs/heads/-branch", "HEAD")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.refs.any { it.kind == RefKind.Tag && it.shortName == "-foo" })
            assertTrue(controller.refs.any { it.kind == RefKind.LocalBranch && it.shortName == "-branch" })

            assertTrue(controller.deleteLocalTag("-foo"), controller.error ?: "dash tag delete failed")
            assertFalse(controller.refs.any { it.kind == RefKind.Tag && it.shortName == "-foo" })
            assertTrue(
                controller.deleteLocalBranch("-branch", force = false),
                controller.error ?: "dash branch delete failed",
            )
            assertFalse(controller.refs.any { it.kind == RefKind.LocalBranch && it.shortName == "-branch" })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `manages lightweight annotated and remote tags`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-tag-manager").toFile()
        val remote = File(dir.parentFile, "${dir.name}-remote")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            val base = backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()

            assertTrue(controller.createTagAtHead("v-light", "", signed = false))
            assertTrue(controller.createTagAtHead("v-annotated", "release notes", signed = false))
            assertTrue(controller.createTag("v-at-commit", "", base, signed = false))
            val light = controller.refs.single {
                it.kind == RefKind.Tag && it.shortName == "v-light"
            }
            val annotated = controller.refs.single {
                it.kind == RefKind.Tag && it.shortName == "v-annotated"
            }
            assertEquals("commit", light.objectType)
            assertEquals("tag", annotated.objectType)
            assertTrue(controller.refs.any {
                it.kind == RefKind.Tag && it.shortName == "v-at-commit"
            })

            assertFalse(controller.createTagAtHead("v-signed", "", signed = true))
            assertEquals("Signed tags require a message.", controller.error)

            assertTrue(remote.mkdirs())
            assertTrue(backend.run(remote.path, listOf("init", "--bare")).ok)
            assertTrue(backend.run(dir.path, listOf("remote", "add", "origin", remote.path)).ok)
            controller.refresh()
            assertTrue(controller.remotes.contains("origin"))

            assertTrue(controller.pushTag("origin", "v-light"))
            assertTrue(controller.pushAllTags("origin"))
            val remoteTags = backend.run(remote.path, listOf("tag", "-l")).stdout
            assertTrue(remoteTags.lineSequence().any { it == "v-light" })
            assertTrue(remoteTags.lineSequence().any { it == "v-annotated" })
            assertTrue(remoteTags.lineSequence().any { it == "v-at-commit" })

            assertTrue(controller.deleteRemoteTag("origin", "v-light"))
            val afterRemoteDelete = backend.run(remote.path, listOf("tag", "-l")).stdout
            assertFalse(afterRemoteDelete.lineSequence().any { it == "v-light" })

            assertTrue(controller.deleteLocalTag("v-annotated"))
            assertFalse(controller.refs.any {
                it.kind == RefKind.Tag && it.shortName == "v-annotated"
            })
        } finally {
            dir.deleteRecursively()
            remote.deleteRecursively()
        }
    }

    @Test
    fun `manages remotes fetches and remote HEAD`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-remote-manager").toFile()
        val remote = File(dir.parentFile, "${dir.name}-remote")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(remote.mkdirs())
            assertTrue(backend.run(remote.path, listOf("init", "--bare")).ok)
            assertTrue(backend.run(remote.path, listOf("symbolic-ref", "HEAD", "refs/heads/main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.addRemote("origin", remote.path))
            controller.refresh()
            assertEquals(remote.path, controller.remoteUrl("origin"))

            assertTrue(controller.pushBranch("main", "origin", "main"))
            controller.refresh()
            assertTrue(controller.fetchRemote(
                "origin",
                FetchOptions(prune = true, tags = FetchTags.Tags, depth = "1"),
            ))
            val fetchToast = assertNotNull(controller.toast)
            assertTrue(fetchToast.success)
            assertTrue(fetchToast.command.contains("--prune"))
            assertTrue(fetchToast.command.contains("--tags"))
            assertTrue(fetchToast.command.contains("--depth=1"))

            assertTrue(controller.setRemoteHeadAuto("origin"))
            assertEquals("main", controller.remoteHead("origin"))
            assertTrue(controller.setRemoteHead("origin", "main"))
            assertTrue(controller.deleteRemoteHead("origin"))
            assertNull(controller.remoteHead("origin"))

            assertTrue(controller.editRemoteUrl("origin", remote.path))
            assertEquals(remote.path, controller.remoteUrl("origin"))
            assertTrue(controller.addRemote("backup", remote.path))
            controller.refresh()
            assertTrue(controller.remotes.contains("backup"))
            assertEquals(remote.path, controller.remoteUrl("backup"))
            assertTrue(controller.removeRemote("backup"))
            assertFalse(controller.remotes.contains("backup"))

            assertFalse(controller.fetchRemote("origin", FetchOptions(depth = "0")))
            assertEquals("Fetch depth must be a positive integer.", controller.error)
            assertFalse(controller.addRemote("origin", remote.path))
            assertEquals("A remote with this name already exists.", controller.error)

            assertTrue(
                backend.run(
                    dir.path,
                    listOf("config", "remote.-evil.url", "https://example.com/evil.git"),
                ).ok,
            )
            controller.refresh()
            assertFalse(controller.remotes.contains("-evil"))
            assertTrue(controller.remoteUrl("-evil").isEmpty())
            assertFalse(controller.fetchRemote("-evil", FetchOptions()))
            assertEquals(EnglishStrings.remoteNotFound, controller.error)
        } finally {
            dir.deleteRecursively()
            remote.deleteRecursively()
        }
    }

    @Test
    fun `pushes current another refspec bulk modes and deletions`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-push-manager").toFile()
        val remote = File(dir.parentFile, "${dir.name}-remote")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(remote.mkdirs())
            assertTrue(backend.run(remote.path, listOf("init", "--bare")).ok)
            assertTrue(backend.run(remote.path, listOf("symbolic-ref", "HEAD", "refs/heads/main")).ok)

            fun remoteHeads(): Set<String> = backend.run(
                remote.path,
                listOf("for-each-ref", "--format=%(refname:short)", "refs/heads"),
            ).stdout.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.addRemote("origin", remote.path))
            controller.refresh()

            assertTrue(controller.push("origin", PushOptions(mode = PushMode.CurrentBranch)))
            assertTrue(remoteHeads().contains("main"))
            assertTrue(assertNotNull(controller.toast).command.contains("--set-upstream"))

            assertTrue(controller.push(
                "origin",
                PushOptions(mode = PushMode.AnotherBranch, destination = "release"),
            ))
            assertTrue(remoteHeads().contains("release"))
            assertTrue(controller.push(
                "origin",
                PushOptions(mode = PushMode.Refspec, refspec = "main:refs/heads/custom"),
            ))
            assertTrue(remoteHeads().contains("custom"))

            assertTrue(controller.push(
                "origin",
                PushOptions(
                    mode = PushMode.AnotherBranch,
                    destination = "lease",
                    force = PushForce.Lease,
                ),
            ))
            assertTrue(assertNotNull(controller.toast).command.contains("--force-with-lease"))
            assertTrue(controller.push(
                "origin",
                PushOptions(
                    mode = PushMode.AnotherBranch,
                    destination = "forced",
                    force = PushForce.Force,
                ),
            ))
            assertTrue(assertNotNull(controller.toast).command.contains("--force"))

            assertTrue(controller.createTagAtHead("v1", "", signed = false))
            assertTrue(controller.push("origin", PushOptions(mode = PushMode.AllTags)))
            assertTrue(assertNotNull(controller.toast).command.contains("--tags"))
            assertTrue(backend.run(remote.path, listOf("tag", "-l")).stdout.contains("v1"))

            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "feature")).ok)
            controller.refresh()
            assertTrue(controller.push("origin", PushOptions(mode = PushMode.AllBranches)))
            assertTrue(assertNotNull(controller.toast).command.contains("--all"))
            assertTrue(remoteHeads().contains("feature"))

            assertTrue(controller.push(
                "origin",
                PushOptions(mode = PushMode.DeleteBranch, branch = "release"),
            ))
            assertFalse(remoteHeads().contains("release"))

            assertTrue(controller.push("origin", PushOptions(mode = PushMode.Mirror)))
            assertTrue(assertNotNull(controller.toast).command.contains("--mirror"))
        } finally {
            dir.deleteRecursively()
            remote.deleteRecursively()
        }
    }

    @Test
    fun `pulls explicitly with ff-only rebase and merge`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-pull-manager").toFile()
        val remote = File(dir.parentFile, "${dir.name}-remote")
        val peer = File(dir.parentFile, "${dir.name}-peer")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(remote.mkdirs())
            assertTrue(backend.run(remote.path, listOf("init", "--bare")).ok)
            assertTrue(backend.run(remote.path, listOf("symbolic-ref", "HEAD", "refs/heads/main")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.addRemote("origin", remote.path))
            controller.refresh()
            assertTrue(controller.push("origin", PushOptions(mode = PushMode.CurrentBranch)))

            assertTrue(backend.run(dir.path, listOf("clone", remote.path, peer.path)).ok)
            assertTrue(backend.run(peer.path, listOf("config", "user.name", "Peer")).ok)
            assertTrue(backend.run(peer.path, listOf("config", "user.email", "peer@example.com")).ok)
            File(peer, "peer.txt").writeText("peer\n")
            assertTrue(backend.run(peer.path, listOf("add", "peer.txt")).ok)
            assertTrue(backend.run(peer.path, listOf("commit", "-m", "peer")).ok)
            assertTrue(backend.run(peer.path, listOf("push", "origin", "main")).ok)

            controller.refresh()
            assertTrue(controller.pull("origin", "main", PullMode.FfOnly))
            assertTrue(assertNotNull(controller.toast).command.contains("--ff-only"))
            assertTrue(File(dir, "peer.txt").isFile)

            File(dir, "local.txt").writeText("local\n")
            assertTrue(backend.run(dir.path, listOf("add", "local.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "local")).ok)
            File(peer, "peer2.txt").writeText("peer2\n")
            assertTrue(backend.run(peer.path, listOf("add", "peer2.txt")).ok)
            assertTrue(backend.run(peer.path, listOf("commit", "-m", "peer2")).ok)
            assertTrue(backend.run(peer.path, listOf("push", "origin", "main")).ok)

            controller.refresh()
            assertFalse(controller.pull("origin", "main", PullMode.FfOnly))
            assertNotNull(controller.error)
            assertTrue(controller.pull("origin", "main", PullMode.Rebase))
            assertTrue(assertNotNull(controller.toast).command.contains("--rebase"))

            File(peer, "peer3.txt").writeText("peer3\n")
            assertTrue(backend.run(peer.path, listOf("add", "peer3.txt")).ok)
            assertTrue(backend.run(peer.path, listOf("commit", "-m", "peer3")).ok)
            assertTrue(backend.run(peer.path, listOf("push", "origin", "main")).ok)
            controller.refresh()
            assertTrue(controller.pull("origin", "main", PullMode.Merge))
            val pullCommand = assertNotNull(controller.toast).command
            assertTrue(pullCommand.contains("--no-rebase"))
            assertTrue(pullCommand.contains("--no-edit"))
        } finally {
            dir.deleteRecursively()
            remote.deleteRecursively()
            peer.deleteRecursively()
        }
    }

    @Test
    fun `adds updates syncs and removes a submodule`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-submodule").toFile()
        val source = File(dir.parentFile, "${dir.name}-source")
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            assertTrue(source.mkdirs())
            initRepository(source, backend)
            File(source, "lib.txt").writeText("library\n")
            assertTrue(backend.run(source.path, listOf("add", "lib.txt")).ok)
            assertTrue(backend.run(source.path, listOf("commit", "-m", "library")).ok)
            File(dir, "base.txt").writeText("base\n")
            assertTrue(backend.run(dir.path, listOf("add", "base.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "base")).ok)
            assertTrue(backend.run(dir.path, listOf("config", "protocol.file.allow", "always")).ok)
            assertTrue(backend.run(source.path, listOf("config", "protocol.file.allow", "always")).ok)

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            // Recent git blocks file transport for submodules by default. The app's
            // command is covered by SubmodulesTest; this test only needs a fixture.
            assertTrue(
                backend.run(
                    dir.path,
                    listOf(
                        "-c", "protocol.file.allow=always",
                        "submodule", "add", "--", source.path, "libs/foo",
                    ),
                ).ok,
                "submodule fixture failed",
            )
            controller.refresh()
            assertTrue(backend.run(dir.path, listOf("add", ".gitmodules", "libs/foo")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "add submodule")).ok)
            controller.refresh()

            val submodule = controller.submodules.single { it.path == "libs/foo" }
            assertEquals(SubmoduleState.Current, submodule.state)
            assertTrue(controller.initSubmodules("libs/foo"), controller.error ?: "init failed")
            val marker = File(dir, "submodule-update-ran.txt")
            assertTrue(
                backend.run(
                    dir.path,
                    listOf(
                        "config",
                        "submodule.libs/foo.update",
                        "!touch ${marker.path.replace('\\', '/')}",
                    ),
                ).ok,
                "could not configure custom submodule update",
            )
            assertTrue(
                controller.updateSubmodules(
                    "libs/foo",
                    SubmoduleUpdateOptions(init = true, recursive = true),
                ),
                controller.error ?: "update failed",
            )
            assertTrue(assertNotNull(controller.toast).command.contains("--checkout"))
            assertFalse(marker.exists(), "custom submodule update command must not run")
            assertTrue(controller.syncSubmodules("libs/foo", recursive = true), controller.error ?: "sync failed")
            assertTrue(assertNotNull(controller.toast).command.contains("--recursive"))

            assertTrue(controller.removeSubmodule("libs/foo"), controller.error ?: "remove failed")
            controller.refresh()
            assertTrue(controller.submodules.isEmpty())
            // git rm removes the entry; git may leave an empty .gitmodules file
            // for the user to commit.
            assertFalse(
                backend.run(
                    dir.path,
                    listOf("config", "-f", ".gitmodules", "--get-regexp", "^submodule\\."),
                ).ok,
            )
        } finally {
            dir.deleteRecursively()
            source.deleteRecursively()
        }
    }

    @Test
    fun `interactive rebase reorders drops and undo restores the original history`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rebase-plan").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            commitFile("base.txt")
            val one = commitFile("one.txt")
            val two = commitFile("two.txt")
            val three = commitFile("three.txt")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val first = controller.commits.first { it.hash == one }
            val plan = assertNotNull(
                controller.loadInteractiveRebasePlan(first),
                controller.error ?: "loadInteractiveRebasePlan returned null",
            )
            assertEquals(3, plan.entries.count { it.command.takesCommit })

            fun entryFor(hash: String): RebaseTodoEntry =
                plan.entries.first { it.argument == hash || hash.startsWith(it.argument) }

            val prefix = plan.entries.takeWhile { !it.command.takesCommit }
            val entryOne = entryFor(one)
            val entryTwo = entryFor(two)
            val entryThree = entryFor(three)
            val edited = plan.copy(
                entries = prefix + listOf(
                    entryThree,
                    entryOne,
                    entryTwo.copy(command = RebaseCommand.Drop),
                ),
            )

            assertTrue(controller.startInteractiveRebase(edited))
            assertNull(controller.operation)
            assertEquals(
                listOf("one", "three", "base"),
                controller.commits.map { it.subject },
            )
            assertFalse(File(dir, "two.txt").exists())
            val toast = assertNotNull(controller.toast)
            assertTrue(toast.success)
            assertEquals("interactive rebase complete", toast.message)
            assertNotNull(toast.undo)

            assertTrue(controller.undoRewrite())
            assertEquals(
                listOf("three", "two", "one", "base"),
                controller.commits.map { it.subject },
            )
            assertTrue(File(dir, "two.txt").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `interactive reword uses the prepared message editor`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rebase-reword").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            commitFile("base.txt")
            val one = commitFile("one.txt")
            commitFile("two.txt")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val plan = assertNotNull(
                controller.loadInteractiveRebasePlan(controller.commits.first { it.hash == one }),
                controller.error ?: "loadInteractiveRebasePlan returned null",
            )
            val edited = plan.copy(
                entries = plan.entries.map { entry ->
                    if (entry.argument == one || one.startsWith(entry.argument)) {
                        entry.copy(command = RebaseCommand.Reword, message = "one reworded")
                    } else {
                        entry
                    }
                },
            )

            assertTrue(controller.startInteractiveRebase(edited))
            assertEquals(
                listOf("two", "one reworded", "base"),
                controller.commits.map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `edit todo while a rebase is stopped changes the remaining commits`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rebase-edit-todo").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            commitFile("base.txt")
            val one = commitFile("one.txt")
            val two = commitFile("two.txt")
            commitFile("three.txt")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val plan = assertNotNull(
                controller.loadInteractiveRebasePlan(controller.commits.first { it.hash == one }),
            )
            val withEdit = plan.copy(
                entries = plan.entries.map { entry ->
                    if (entry.argument == one || one.startsWith(entry.argument)) {
                        entry.copy(command = RebaseCommand.Edit)
                    } else {
                        entry
                    }
                },
            )

            assertFalse(controller.startInteractiveRebase(withEdit))
            assertEquals(GitOperationKind.Rebase, controller.operation?.kind)
            val remaining = assertNotNull(controller.loadRebaseTodo())
            val dropTwo = remaining.entries.map { entry ->
                if (entry.argument == two || two.startsWith(entry.argument)) {
                    entry.copy(command = RebaseCommand.Drop)
                } else {
                    entry
                }
            }
            assertTrue(controller.saveRebaseTodo(dropTwo))
            assertTrue(controller.operationAction(OperationAction.Continue))
            assertNull(controller.operation)
            assertEquals(
                listOf("three", "one", "base"),
                controller.commits.map { it.subject },
            )
            assertNotNull(controller.toast?.undo)
            Unit
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `moves adjacent commits earlier and later through the rebase plan`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-reorder").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            commitFile("base.txt")
            commitFile("one.txt")
            commitFile("two.txt")
            commitFile("three.txt")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            val two = controller.commits.first { it.subject == "two" }
            assertTrue(controller.moveCommit(two, earlier = true), controller.error ?: "move failed")
            assertEquals(
                listOf("three", "one", "two", "base"),
                controller.commits.map { it.subject },
            )
            assertNotNull(controller.toast?.undo)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(
                listOf("three", "two", "one", "base"),
                controller.commits.map { it.subject },
            )

            val one = controller.commits.first { it.subject == "one" }
            assertTrue(controller.moveCommit(one, earlier = false), controller.error ?: "move failed")
            assertEquals(
                listOf("three", "one", "two", "base"),
                controller.commits.map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rebases the current branch onto another base and undoes it`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rebase-onto").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            val base = commitFile("base.txt")
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            commitFile("side.txt")
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            commitFile("one.txt")
            commitFile("two.txt")
            val side = backend.run(dir.path, listOf("rev-parse", "side")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.rebaseOnto(base, side), controller.error ?: "rebase --onto failed")
            assertEquals(
                listOf("two", "one", "side", "base"),
                controller.commits.map { it.subject },
            )
            assertNotNull(controller.toast?.undo)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(
                listOf("two", "one", "base"),
                controller.commits.map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `moves a range that ends at head onto another base`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-move-range").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            val base = commitFile("base.txt")
            assertTrue(backend.run(dir.path, listOf("checkout", "-b", "side")).ok)
            commitFile("side.txt")
            assertTrue(backend.run(dir.path, listOf("checkout", "main")).ok)
            commitFile("one.txt")
            commitFile("two.txt")
            val side = backend.run(dir.path, listOf("rev-parse", "side")).stdout.trim()

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(controller.moveRange("$base..HEAD", side), controller.error ?: "move range failed")
            assertEquals(
                listOf("two", "one", "side", "base"),
                controller.commits.map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `squashes a range and keeps its descendants`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-squash-range").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", name.removeSuffix(".txt"))).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            val base = commitFile("base.txt")
            commitFile("one.txt")
            val two = commitFile("two.txt")
            commitFile("three.txt")

            val controller = RepoController(backend.open(dir.path), backend, backend.version())
            controller.refresh()
            assertTrue(
                controller.squashRange("$base..$two", "combined"),
                controller.error ?: "squash failed",
            )
            assertEquals(
                listOf("three", "combined", "base"),
                controller.commits.map { it.subject },
            )
            assertNotNull(controller.toast?.undo)
            assertTrue(controller.undoRewrite(), controller.error ?: "undo failed")
            assertEquals(
                listOf("three", "two", "one", "base"),
                controller.commits.map { it.subject },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `autosquash plan converts fixup commits before editing`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-rebase-autosquash").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)

            fun commitFile(name: String, message: String): String {
                File(dir, name).writeText("$name\n")
                assertTrue(backend.run(dir.path, listOf("add", name)).ok)
                assertTrue(backend.run(dir.path, listOf("commit", "-m", message)).ok)
                return backend.run(dir.path, listOf("rev-parse", "HEAD")).stdout.trim()
            }

            commitFile("base.txt", "base")
            val target = commitFile("target.txt", "target")
            commitFile("fixup.txt", "fixup! target")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            val targetCommit = controller.commits.first { it.hash == target }
            val base = assertNotNull(targetCommit.parents.firstOrNull())
            val plan = assertNotNull(
                controller.loadInteractiveRebasePlan(base = base, root = false, autosquash = true),
                controller.error ?: "loadInteractiveRebasePlan returned null",
            )

            assertEquals(
                RebaseCommand.Pick,
                plan.entries.first { it.command.takesCommit }.command,
            )
            assertTrue(plan.entries.any { it.command == RebaseCommand.Fixup })
            assertTrue(plan.autosquash)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `manages hooks list edit enable disable and hooks path`() = runBlocking {
        val dir = Files.createTempDirectory("git-chan-hooks").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            val hooksDir = File(dir, ".git/hooks")
            val sample = File(hooksDir, "pre-commit.sample")
            sample.writeText("#!/bin/sh\necho original\n")

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()

            val sampleEntry = assertNotNull(
                controller.hooks.entries.firstOrNull { it.name == "pre-commit" },
            )
            assertTrue(sampleEntry.sample)
            assertFalse(sampleEntry.enabled)
            controller.loadHookDetail("pre-commit")
            assertTrue(controller.hookDetail?.content?.contains("original") == true)

            assertTrue(
                controller.saveHook("pre-commit", "#!/bin/sh\necho edited\n"),
                controller.error ?: "saving a sample hook failed",
            )
            assertTrue(sample.readText().contains("edited"))

            assertTrue(
                controller.setHookEnabled("pre-commit", true),
                controller.error ?: "enabling a sample hook failed",
            )
            val active = File(hooksDir, "pre-commit")
            assertTrue(active.isFile)
            assertTrue(
                controller.hooks.entries.first { it.name == "pre-commit" }.enabled,
            )

            assertTrue(
                controller.setHookEnabled("pre-commit", false),
                controller.error ?: "disabling a hook failed",
            )
            assertFalse(active.exists())
            val disabled = File(hooksDir, "pre-commit.disabled")
            assertTrue(disabled.isFile)
            assertTrue(
                controller.hooks.entries.first { it.name == "pre-commit" }.disabled,
            )

            assertTrue(
                controller.setHookEnabled("pre-commit", true),
                controller.error ?: "re-enabling a hook failed",
            )
            assertTrue(active.isFile)
            assertFalse(disabled.exists())

            assertFalse(
                controller.setHooksPath(".git/custom-hooks"),
                "a custom hooks directory inside .git but outside .git/hooks must require confirmation",
            )
            assertEquals(EnglishStrings.hookPathConfirmationRequired, controller.error)

            val customDir = File(dir, "custom-hooks")
            customDir.mkdirs()
            File(customDir, "commit-msg").writeText("#!/bin/sh\nexit 0\n")
            assertFalse(
                controller.setHooksPath("custom-hooks"),
                "external core.hooksPath must require confirmation",
            )
            assertEquals(EnglishStrings.hookPathConfirmationRequired, controller.error)
            assertTrue(
                controller.setHooksPath("custom-hooks", allowExternal = true),
                controller.error ?: "setting core.hooksPath failed",
            )
            assertEquals("custom-hooks", controller.hooks.configuredPath)
            assertTrue(controller.hooks.directory.endsWith("custom-hooks"))
            assertTrue(controller.hooks.outsideCommonDir)
            assertTrue(controller.hooks.entries.any { it.name == "commit-msg" && it.enabled })

            assertFalse(
                controller.saveHook("commit-msg", "#!/bin/sh\necho changed\n"),
                "external hook files must require confirmation",
            )
            assertEquals(EnglishStrings.hookPathConfirmationRequired, controller.error)
            assertTrue(
                controller.saveHook(
                    "commit-msg",
                    "#!/bin/sh\necho changed\n",
                    allowExternal = true,
                ),
                controller.error ?: "saving an external hook failed",
            )
            assertTrue(File(customDir, "commit-msg").readText().contains("changed"))

            assertTrue(
                controller.resetHooksPath(),
                controller.error ?: "resetting core.hooksPath failed",
            )
            assertEquals("", controller.hooks.configuredPath)
            assertFalse(controller.hooks.entries.any { it.name == "commit-msg" && it.enabled })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `captures hook output from a failed commit`() = runBlocking<Unit> {
        val dir = Files.createTempDirectory("git-chan-hook-output").toFile()
        try {
            val backend = CliGitBackend()
            initRepository(dir, backend)
            File(dir, "a.txt").writeText("one\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)
            assertTrue(backend.run(dir.path, listOf("commit", "-m", "initial")).ok)

            val hook = File(dir, ".git/hooks/pre-commit")
            hook.writeText("#!/bin/sh\necho 'blocked by test hook' >&2\nexit 1\n")
            hook.setExecutable(true)
            File(dir, "a.txt").writeText("two\n")
            assertTrue(backend.run(dir.path, listOf("add", "a.txt")).ok)

            val repository = backend.open(dir.path)
            val controller = RepoController(repository, backend, backend.version())
            controller.refresh()
            controller.commitMessage = "blocked"
            assertFalse(controller.commit())
            assertEquals(1, controller.commits.size)

            val output = assertNotNull(controller.hookOutput)
            assertTrue(output.text.contains("blocked by test hook"), output.text)
            assertFalse(output.success)
            assertNotNull(output.exitCode)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun initRepository(dir: File, backend: CliGitBackend) {
        val init = backend.run(dir.path, listOf("init", "-b", "main"))
        assertTrue(init.ok, init.stderr)
        assertTrue(backend.run(dir.path, listOf("config", "user.name", "Git Chan Test")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "user.email", "test@example.com")).ok)
        assertTrue(backend.run(dir.path, listOf("config", "commit.gpgsign", "false")).ok)
    }
}

private fun samePath(first: String, second: String): Boolean {
    val a = runCatching { File(first).canonicalFile }.getOrElse { File(first).absoluteFile.normalize() }
    val b = runCatching { File(second).canonicalFile }.getOrElse { File(second).absoluteFile.normalize() }
    return a.path.equals(
        b.path,
        ignoreCase = System.getProperty("os.name").orEmpty().lowercase().contains("win"),
    )
}

private class SlowStatusBackend(
    private val delegate: GitBackend,
) : GitBackend by delegate {
    override fun status(repository: Repository): RepoStatus {
        Thread.sleep(300)
        return delegate.status(repository)
    }
}

private class BusyProbeBackend(
    private val delegate: GitBackend,
) : GitBackend by delegate {
    var controller: RepoController? = null
    var observedBusyAfterRefresh = false

    fun reset() {
        observedBusyAfterRefresh = false
    }

    override fun run(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
    ): GitResult {
        if (!observedBusyAfterRefresh &&
            controller?.busy == true &&
            args.firstOrNull() == "rev-parse" &&
            args.contains("HEAD")
        ) {
            observedBusyAfterRefresh = true
        }
        return delegate.run(cwd, args, stdin, timeoutSeconds, environment)
    }
}

private class SlowDiffBackend(
    private val delegate: GitBackend,
    private val slowPath: String,
) : GitBackend by delegate {
    override fun run(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
    ): GitResult {
        if (args.firstOrNull() == "diff" && args.lastOrNull() == slowPath) {
            Thread.sleep(400)
        }
        return delegate.run(cwd, args, stdin, timeoutSeconds, environment)
    }
}

private class FakeFilterRepoBackend(
    private val delegate: GitBackend,
    private val gitDir: String,
) : GitBackend by delegate {
    var lastArgs: List<String> = emptyList()
    var expressionsPath: File? = null
    var expressionsText: String? = null
    var oversizedPreview: Boolean = false

    override fun run(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
    ): GitResult {
        if (args.firstOrNull() != "filter-repo") {
            return delegate.run(cwd, args, stdin, timeoutSeconds, environment)
        }
        if (args.contains("--version")) {
            return GitResult(
                args = args,
                cwd = cwd,
                exitCode = 0,
                stdout = "2.47.0\n",
                stderr = "",
                durationMs = 1,
            )
        }
        lastArgs = args
        if (args.contains("--replace-text")) {
            val path = args.getOrNull(args.indexOf("--replace-text") + 1)
            expressionsPath = path?.let(::File)
            expressionsText = expressionsPath?.takeIf { it.isFile }?.readText()
        }
        if (args.contains("--dry-run")) {
            val dir = File(gitDir, "filter-repo")
            dir.mkdirs()
            val original = File(dir, "fast-export.original")
            if (oversizedPreview) {
                RandomAccessFile(original, "rw").use { it.setLength(16L * 1024 * 1024 + 1) }
            } else {
                original.writeText(
                    "commit refs/heads/main\n" +
                        "M 100644 :1 secret.txt\n" +
                        "commit refs/heads/main\n" +
                        "M 100644 :2 keep.txt\n",
                )
            }
            File(dir, "fast-export.filtered").writeText(
                "commit refs/heads/main\n" +
                    "M 100644 :2 keep.txt\n",
            )
            File(dir, "changed-refs").writeText("aaaa bbbb refs/heads/main\n")
        }
        return GitResult(
            args = args,
            cwd = cwd,
            exitCode = 0,
            stdout = "",
            stderr = "",
            durationMs = 1,
        )
    }
}
