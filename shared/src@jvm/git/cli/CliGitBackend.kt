package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.BlameResult
import org.baconana.mybeautifulgit_chan.domain.BranchComparison
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.CommitInspection
import org.baconana.mybeautifulgit_chan.domain.CommitSignature
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.CommitStats
import org.baconana.mybeautifulgit_chan.domain.GitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.validGitConfigKey
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.GitAuth
import org.baconana.mybeautifulgit_chan.domain.SshAuthMode
import org.baconana.mybeautifulgit_chan.domain.GrepResult
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.ReflogEntry
import org.baconana.mybeautifulgit_chan.domain.RepoStatus
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.RepositoryMetadataException
import org.baconana.mybeautifulgit_chan.domain.RepositoryRuntime
import org.baconana.mybeautifulgit_chan.domain.ResetPreview
import org.baconana.mybeautifulgit_chan.domain.StashDetail
import org.baconana.mybeautifulgit_chan.domain.StashEntry
import org.baconana.mybeautifulgit_chan.domain.StashFileChange
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.domain.UnreachableCommit
import org.baconana.mybeautifulgit_chan.domain.UnreachablePlan
import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.Worktree
import org.baconana.mybeautifulgit_chan.git.GitBackend
import org.baconana.mybeautifulgit_chan.git.GitBlob
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitRunControl
import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.git.porcelain.BlameParser
import org.baconana.mybeautifulgit_chan.git.porcelain.BranchCompareParser
import org.baconana.mybeautifulgit_chan.git.porcelain.CommitSignatureParser
import org.baconana.mybeautifulgit_chan.git.porcelain.CommitStatParser
import org.baconana.mybeautifulgit_chan.git.porcelain.ConfigParser
import org.baconana.mybeautifulgit_chan.git.porcelain.GrepParser
import org.baconana.mybeautifulgit_chan.git.porcelain.LogParser
import org.baconana.mybeautifulgit_chan.git.porcelain.RefParser
import org.baconana.mybeautifulgit_chan.git.porcelain.ReflogParser
import org.baconana.mybeautifulgit_chan.git.porcelain.StatusParser
import org.baconana.mybeautifulgit_chan.git.porcelain.SubmoduleParser
import org.baconana.mybeautifulgit_chan.git.porcelain.WorktreeParser
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class GitExecutableNotFoundException(executable: String, cause: Throwable) : RuntimeException(
    "Git executable not found: $executable",
    cause,
)

/** Keep a single Git invocation bounded even if a hook or repository prints forever. */
private const val MAX_STDOUT_BYTES = 8L * 1024 * 1024
private const val MAX_STDERR_BYTES = 2L * 1024 * 1024
private const val MAX_BINARY_STDERR_BYTES = 64L * 1024
private const val MAX_INSPECTION_FILES = 500
private const val MAX_INSPECTION_LINES = 20_000
private const val MAX_GIT_METADATA_POINTER_BYTES = 4 * 1024

/**
 * Git CLI как единственный источник правды.
 *
 * Каждый вызов получает чистый environment: без pager, без интерактивных промптов,
 * без унаследованных GIT_DIR/GIT_WORK_TREE. Вывод читается в UTF-8 и всегда отдельно
 * от stderr, чтобы ошибки и вывод хуков можно было показать как есть.
 */
class CliGitBackend(
    private val executable: String = "git",
    initialAuth: GitAuth = GitAuth(),
) : GitBackend {

    /** Updated from Settings; network operations pick it up on the next run. */
    @Volatile
    var auth: GitAuth = initialAuth

    override fun version(): GitVersion {
        val result = run(cwd = null, args = listOf("--version"), timeoutSeconds = 10)
        if (!result.ok) throw GitCommandException(result)
        return GitVersion.parse(result.stdout)
            ?: throw IllegalStateException("Cannot parse git version: ${result.stdout.trim()}")
    }

    override fun open(path: String): Repository {
        val requested = File(path).canonicalFile
        val directory = when {
            requested.isDirectory -> requested
            requested.isFile -> requested.parentFile
                ?: throw IllegalArgumentException("Path has no parent directory: $path")

            else -> throw IllegalArgumentException("Path does not exist: $path")
        }

        val gitDirResult = run(directory.path, listOf("rev-parse", "--absolute-git-dir"))
        if (!gitDirResult.ok) {
            findBrokenRepositoryMetadata(directory)?.let { problem ->
                throw RepositoryMetadataException(
                    repositoryPath = problem.root.canonicalPath,
                    missingMetadata = problem.missing,
                    cause = GitCommandException(gitDirResult),
                )
            }
            throw GitCommandException(gitDirResult)
        }

        val bareResult = run(directory.path, listOf("rev-parse", "--is-bare-repository"))
        if (!bareResult.ok) throw GitCommandException(bareResult)
        val isBare = bareResult.stdout.trim() == "true"

        val root = if (isBare) {
            directory.canonicalPath
        } else {
            val topLevel = run(directory.path, listOf("rev-parse", "--show-toplevel"))
            if (!topLevel.ok) throw GitCommandException(topLevel)
            File(topLevel.stdout.trim()).canonicalPath
        }

        val gitDir = File(gitDirResult.stdout.trim()).canonicalPath
        val commonDir = resolveCommonDir(directory.path, root)

        return Repository(
            root = root,
            gitDir = gitDir,
            commonDir = commonDir,
            name = File(root).name.ifBlank { root },
            isBare = isBare,
            isWorktree = !isBare && gitDir != commonDir,
            workTree = if (isBare) null else root,
        )
    }

    override fun status(repository: Repository): RepoStatus {
        val workTree = repository.workTree
            ?: throw IllegalStateException("Bare repository has no working tree: ${repository.root}")

        val result = run(
            cwd = workTree,
            args = listOf(
                "status",
                "--porcelain=v2",
                "-z",
                "--branch",
                "--untracked-files=all",
                "--ignored=matching",
                "--ignore-submodules=none",
            ),
        )
        if (!result.ok) throw GitCommandException(result)
        return StatusParser.parse(result.stdout)
    }

    override fun commitMessage(repository: Repository, hash: String): String {
        require(hash.matches(Regex("^[0-9a-fA-F]{4,64}$"))) { "Invalid commit hash: $hash" }
        val result = run(
            cwd = repository.root,
            args = listOf("show", "-s", "--format=%B", hash),
            timeoutSeconds = 60,
        )
        if (!result.ok) throw GitCommandException(result)
        return result.stdout.trimEnd()
    }

    override fun resetPreview(
        repository: Repository,
        hash: String,
        maxCommitSubjects: Int,
    ): ResetPreview {
        require(hash.matches(Regex("^[0-9a-fA-F]{4,64}$"))) { "Invalid commit hash: $hash" }
        require(maxCommitSubjects > 0) { "maxCommitSubjects must be positive" }

        val plan = unreachablePlan(repository, listOf(hash), maxCommitSubjects)

        // Переименования раскладываем на старый и новый путь: локальная правка
        // любого из них должна участвовать в проверке --keep.
        val changed = run(
            cwd = repository.root,
            args = listOf("diff", "--name-only", "-z", "--no-renames", hash, "HEAD"),
            timeoutSeconds = 60,
        )
        if (!changed.ok) throw GitCommandException(changed)
        val targetPaths = changed.stdout.split('\u0000')
            .filter { it.isNotEmpty() }
            .toSet()

        val entries = if (repository.isBare) emptyList() else status(repository).entries
        val staged = entries.filter { it.isStaged }.map { it.path }
        val unstaged = entries
            .filter { it.isUnstaged && it.kind != StatusKind.Untracked }
            .map { it.path }
        val conflicted = entries.filter { it.isConflicted }.map { it.path }
        val untracked = entries
            .filter { it.kind == StatusKind.Untracked }
            .map { it.path }
        val localPaths = entries
            .filter { it.isStaged || it.isUnstaged || it.isConflicted }
            .flatMap { entry -> listOfNotNull(entry.path, entry.originalPath) }
            .toSet()
        val keepConflicts = targetPaths.filter { it in localPaths }.sorted()

        return ResetPreview(
            lostCommits = plan.commitCount,
            lostCommitSubjects = plan.commits.map { it.subject },
            stagedPaths = staged,
            unstagedPaths = unstaged,
            conflictedPaths = conflicted,
            untrackedPaths = untracked,
            keepConflicts = keepConflicts,
            unreachable = plan,
        )
    }

    override fun unreachablePlan(
        repository: Repository,
        targets: List<String>,
        maxCommits: Int,
    ): UnreachablePlan {
        targets.forEach { target ->
            require(target.matches(Regex("^[0-9a-fA-F]{4,64}$"))) {
                "Invalid commit hash: $target"
            }
        }
        require(maxCommits > 0) { "maxCommits must be positive" }

        val currentBranch = refs(repository).firstOrNull { it.isHead }?.name
        val allRefs = run(
            cwd = repository.root,
            args = listOf("for-each-ref", "--format=%(refname)"),
            timeoutSeconds = 30,
        )
        if (!allRefs.ok) throw GitCommandException(allRefs)
        val exclusions = buildList {
            targets.forEach(::add)
            allRefs.stdout.lineSequence()
                .map { it.trim() }
                .filter { ref ->
                    ref.isNotEmpty() &&
                        ref != currentBranch &&
                        // Backup-refs — страховка, а не обычная история; их tip не
                        // должен скрывать потери, которые показываем пользователю.
                        !ref.startsWith("refs/mybeautifulgit/backups/")
                }
                .forEach(::add)
        }.distinct()
        val notArgs = if (exclusions.isEmpty()) emptyList() else listOf("--not") + exclusions

        val count = run(
            cwd = repository.root,
            args = listOf("rev-list", "--count", "HEAD") + notArgs,
            timeoutSeconds = 60,
        )
        if (!count.ok) throw GitCommandException(count)
        val commitCount = count.stdout.trim().toIntOrNull()
            ?: throw IllegalStateException("Cannot parse unreachable commit count: ${count.stdout.trim()}")

        val objectCount = if (commitCount == 0) {
            0
        } else {
            val objects = run(
                cwd = repository.root,
                args = listOf("rev-list", "--objects", "--count", "HEAD") + notArgs,
                timeoutSeconds = 60,
            )
            if (!objects.ok) throw GitCommandException(objects)
            objects.stdout.trim().toIntOrNull()
                ?: throw IllegalStateException("Cannot parse unreachable object count: ${objects.stdout.trim()}")
        }

        val commits = if (commitCount == 0) {
            emptyList()
        } else {
            val result = run(
                cwd = repository.root,
                args = listOf(
                    "log",
                    "--format=%H%x00%s%x00%an%x00%at",
                    "--max-count=$maxCommits",
                    "HEAD",
                ) + notArgs,
                timeoutSeconds = 60,
            )
            if (!result.ok) throw GitCommandException(result)
            result.stdout.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val fields = line.split('\u0000')
                    if (fields.size < 4 || !fields[0].matches(Regex("^[0-9a-fA-F]{40,64}$"))) {
                        return@mapNotNull null
                    }
                    UnreachableCommit(
                        hash = fields[0],
                        subject = fields[1],
                        author = fields[2],
                        time = fields[3].toLongOrNull() ?: 0L,
                    )
                }
                .toList()
        }

        val fsck = run(
            cwd = repository.root,
            args = listOf("fsck", "--unreachable", "--no-reflogs", "--no-progress"),
            timeoutSeconds = 120,
        )
        val fsckObjects = mutableSetOf<String>()
        val fsckCommitIds = mutableSetOf<String>()
        fsck.stdout.lineSequence().forEach { raw ->
            val parts = raw.trim().split(' ', limit = 3)
            if (parts.size != 3 || (parts[0] != "unreachable" && parts[0] != "dangling")) {
                return@forEach
            }
            val oid = parts[2]
            if (!oid.matches(Regex("^[0-9a-fA-F]{40,64}$"))) return@forEach
            fsckObjects += oid
            if (parts[1] == "commit") fsckCommitIds += oid
        }
        val fsckError = when {
            fsck.ok -> null
            fsck.timedOut -> "git fsck timed out"
            else -> fsck.stderr.trim().lineSequence().firstOrNull { it.isNotBlank() }
                ?: "git fsck exited with ${fsck.exitCode}"
        }

        return UnreachablePlan(
            commitCount = commitCount,
            commits = commits,
            objectCount = objectCount,
            fsckUnreachableObjects = fsckObjects.size,
            fsckUnreachableCommits = fsckCommitIds.size,
            fsckError = fsckError,
        )
    }

    override fun log(
        repository: Repository,
        maxCount: Int,
        rev: String,
        filter: LogFilter,
        skip: Int,
    ): List<Commit> {
        require(maxCount > 0) { "maxCount must be positive" }
        require(skip >= 0) { "skip must not be negative" }
        require(rev.isNotBlank()) { "rev must not be blank" }

        val target = when {
            filter.branch.isNotBlank() -> filter.branch.trim()
            rev != "HEAD" -> rev
            filter.onlyCurrent -> "HEAD"
            else -> "--all"
        }
        require(!target.startsWith("-") || target == "--all") {
            "Revision must not start with '-': $target"
        }

        if (target == "HEAD") {
            val head = run(repository.root, listOf("rev-parse", "--verify", "--quiet", "HEAD"))
            if (!head.ok) return emptyList()
        }

        val normalized = filter.normalized()
        val format = "%H%x00%P%x00%an%x00%ae%x00%at%x00%cn%x00%ce%x00%ct%x00%s%x00%D"
        val args = buildList {
            add("log")
            add("--topo-order")
            add("--max-count=$maxCount")
            if (skip > 0) add("--skip=$skip")
            add("--pretty=format:$format")
            normalized.author.takeIf { it.isNotEmpty() }?.let { add("--author=$it") }
            normalized.since.takeIf { it.isNotEmpty() }?.let { add("--since=$it") }
            normalized.until.takeIf { it.isNotEmpty() }?.let { add("--until=$it") }
            normalized.message.takeIf { it.isNotEmpty() }?.let { add("--grep=$it") }
            normalized.pickaxe.takeIf { it.isNotEmpty() }?.let { add("-S$it") }
            if (target == "--all") add("--exclude=refs/mybeautifulgit/backups/*")
            add(target)
            normalized.path.takeIf { it.isNotEmpty() }?.let {
                add("--")
                add(it)
            }
        }
        val result = run(cwd = repository.root, args = args)
        if (!result.ok) throw GitCommandException(result)
        return LogParser.parse(result.stdout)
    }

    override fun commitInspection(
        repository: Repository,
        hash: String,
        maxDiffBytes: Long,
    ): CommitInspection {
        require(hash.matches(Regex("^[0-9a-fA-F]{4,64}$"))) { "Invalid commit hash: $hash" }
        require(maxDiffBytes > 0) { "maxDiffBytes must be positive" }

        // Сначала получаем дешёвую статистику. Полный patch не запускаем для
        // заведомо огромного коммита: bounded reader всё равно бы его оборвал,
        // но незачем тратить время и память на первые 8 МиБ.
        val statResult = run(
            cwd = repository.root,
            args = listOf("show", "--format=", "--numstat", "--first-parent", hash),
        )
        if (!statResult.ok && !statResult.stdoutTruncated) throw GitCommandException(statResult)
        val stats = CommitStats(CommitStatParser.parseNumstat(statResult.stdout))
        val changedLines = stats.additions.toLong() + stats.deletions.toLong()
        val tooLargeForPatch = statResult.stdoutTruncated ||
            stats.files.size > MAX_INSPECTION_FILES ||
            changedLines > MAX_INSPECTION_LINES

        val signatureResult = run(
            cwd = repository.root,
            args = listOf("show", "-s", "--format=%G?%x00%GS%x00%GK", hash),
            timeoutSeconds = 60,
        )
        val signature = if (signatureResult.ok) {
            CommitSignatureParser.parse(signatureResult.stdout)
        } else {
            CommitSignature(status = CommitSignatureStatus.CannotCheck)
        }

        if (tooLargeForPatch) {
            return CommitInspection(
                hash = hash,
                diff = "",
                stats = stats,
                signature = signature,
                diffTruncated = true,
            )
        }

        // `git show` без --first-parent для merge печатает combined diff или вообще пусто.
        // Mainline diff предсказуемее и совпадает с обычным diff родителя.
        val diffResult = run(
            cwd = repository.root,
            args = listOf(
                "show",
                "--format=",
                "--no-ext-diff",
                "--no-color",
                "--patch",
                "--first-parent",
                hash,
            ),
        )
        if (!diffResult.ok && !diffResult.stdoutTruncated) throw GitCommandException(diffResult)

        val diffBytes = diffResult.stdout.toByteArray(Charsets.UTF_8)
        val truncated = diffResult.stdoutTruncated || diffBytes.size > maxDiffBytes
        val visibleDiff = if (truncated) {
            val visibleBytes = maxDiffBytes
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .coerceAtMost(diffBytes.size.toLong())
                .toInt()
            diffBytes.copyOf(visibleBytes).toString(Charsets.UTF_8)
        } else {
            diffResult.stdout
        }

        return CommitInspection(
            hash = hash,
            diff = visibleDiff,
            stats = stats,
            signature = signature,
            diffTruncated = truncated,
        )
    }

    override fun fileHistory(
        repository: Repository,
        path: String,
        maxCount: Int,
    ): List<Commit> {
        require(path.isNotBlank()) { "path must not be blank" }
        require(maxCount > 0) { "maxCount must be positive" }
        if (!run(repository.root, listOf("rev-parse", "--verify", "--quiet", "HEAD")).ok) {
            return emptyList()
        }
        val format = "%H%x00%P%x00%an%x00%ae%x00%at%x00%cn%x00%ce%x00%ct%x00%s%x00%D"
        val result = run(
            cwd = repository.root,
            args = listOf(
                "log",
                "--follow",
                "--max-count=$maxCount",
                "--pretty=format:$format",
                "--",
                path,
            ),
        )
        if (!result.ok) throw GitCommandException(result)
        return LogParser.parse(result.stdout)
    }

    override fun blame(repository: Repository, path: String, maxLines: Int): BlameResult {
        require(path.isNotBlank()) { "path must not be blank" }
        require(maxLines > 0) { "maxLines must be positive" }
        // Просим на одну строку больше, чтобы отличить ровно N строк от обрезанного вывода.
        val requested = maxLines.coerceAtMost(Int.MAX_VALUE - 1) + 1
        val result = run(
            cwd = repository.root,
            args = listOf(
                "blame",
                "--line-porcelain",
                "-L",
                "1,$requested",
                "HEAD",
                "--",
                path,
            ),
            timeoutSeconds = 120,
        )
        if (!result.ok) throw GitCommandException(result)
        val parsed = BlameParser.parse(result.stdout)
        return BlameResult(
            path = path,
            lines = parsed.take(maxLines),
            truncated = parsed.size > maxLines,
        )
    }

    override fun grep(
        repository: Repository,
        query: String,
        path: String,
        maxMatches: Int,
    ): GrepResult {
        require(query.isNotEmpty()) { "query must not be empty" }
        require(maxMatches > 0) { "maxMatches must be positive" }
        val args = buildList {
            add("grep")
            add("-n")
            add("-I")
            add("-F")
            add("--no-color")
            add("--null")
            add("-m")
            add("20")
            add("-e")
            add(query)
            if (path.isNotBlank()) {
                add("--")
                add(path)
            }
        }
        val result = run(cwd = repository.root, args = args, timeoutSeconds = 120)
        // git grep returns 1 for “no matches”; it is not a failure for the UI.
        if (result.exitCode == 1) {
            return GrepResult(emptyList())
        }
        if (!result.ok) throw GitCommandException(result)
        val parsed = GrepParser.parse(result.stdout)
        return GrepResult(
            matches = parsed.take(maxMatches),
            truncated = parsed.size > maxMatches,
        )
    }

    override fun reflog(repository: Repository, maxCount: Int): List<ReflogEntry> {
        require(maxCount > 0) { "maxCount must be positive" }
        val format = "%H%x00%gD%x00%gs%x00%ct"
        val result = run(
            cwd = repository.root,
            args = listOf("reflog", "--all", "--format=$format", "-n$maxCount"),
        )
        if (!result.ok) throw GitCommandException(result)
        return ReflogParser.parse(result.stdout)
    }

    override fun refs(repository: Repository): List<GitRef> {
        val format = "%(refname)%00%(objectname)%00%(objecttype)%00%(symref)%00%(upstream)%00%(upstream:track)%00%(HEAD)"
        val result = run(
            cwd = repository.root,
            args = listOf(
                "for-each-ref",
                "--format=$format",
                "refs/heads",
                "refs/remotes",
                "refs/tags",
            ),
        )
        if (!result.ok) throw GitCommandException(result)
        return RefParser.parse(result.stdout)
    }

    override fun remotes(repository: Repository): List<String> {
        val result = run(cwd = repository.root, args = listOf("remote"))
        if (!result.ok) throw GitCommandException(result)
        return result.stdout.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    override fun remoteUrls(repository: Repository): Map<String, String> {
        val result = run(cwd = repository.root, args = listOf("remote", "-v"))
        if (!result.ok) throw GitCommandException(result)
        val urls = linkedMapOf<String, String>()
        result.stdout.lineSequence().forEach { line ->
            val record = line.trimEnd('\r')
            val tab = record.indexOf('\t')
            if (tab <= 0 || !record.endsWith(" (fetch)")) return@forEach
            val name = record.substring(0, tab).trim()
            val url = record.substring(tab + 1).removeSuffix(" (fetch)").trim()
            if (name.isNotEmpty() && url.isNotEmpty()) urls.putIfAbsent(name, url)
        }
        return urls
    }

    override fun compareBranches(
        repository: Repository,
        first: String,
        second: String,
        maxCommits: Int,
    ): BranchComparison {
        require(maxCommits > 0) { "maxCommits must be positive" }
        val range = "$first...$second"
        val counts = run(
            cwd = repository.root,
            args = listOf("rev-list", "--left-right", "--count", range),
        )
        if (!counts.ok) throw GitCommandException(counts)
        val (ahead, behind) = BranchCompareParser.parseCounts(counts.stdout)

        val log = run(
            cwd = repository.root,
            args = listOf(
                "log",
                "--left-right",
                "--format=%H%x00%m%x00%s",
                "-n$maxCommits",
                range,
            ),
        )
        if (!log.ok) throw GitCommandException(log)
        return BranchComparison(
            first = first,
            second = second,
            ahead = ahead,
            behind = behind,
            commits = BranchCompareParser.parseCommits(log.stdout),
        )
    }

    override fun worktrees(repository: Repository): List<Worktree> {
        val result = run(
            cwd = repository.root,
            args = listOf("worktree", "list", "--porcelain"),
        )
        if (!result.ok) throw GitCommandException(result)
        return WorktreeParser.parse(result.stdout)
    }

    override fun submodules(repository: Repository): List<Submodule> {
        val workTree = repository.workTree ?: return emptyList()
        val result = run(
            cwd = workTree,
            args = listOf("submodule", "status", "--recursive"),
        )
        if (!result.ok) throw GitCommandException(result)
        return SubmoduleParser.parse(result.stdout)
    }

    override fun stashCount(repository: Repository): Int {
        val workTree = repository.workTree ?: return 0
        val result = run(
            cwd = workTree,
            args = listOf("stash", "list", "--format=%gd"),
        )
        if (!result.ok) throw GitCommandException(result)
        return result.stdout.lineSequence().count { it.isNotBlank() }
    }

    override fun stashList(repository: Repository): List<StashEntry> {
        val workTree = repository.workTree ?: return emptyList()
        val result = run(
            cwd = workTree,
            args = listOf(
                "stash",
                "list",
                "--format=%gd%x00%H%x00%gs%x00%ct%x00%an%x00%ae",
            ),
        )
        if (!result.ok) throw GitCommandException(result)
        return result.stdout.lineSequence().mapNotNull { line ->
            val fields = line.split('\u0000')
            if (fields.size < 4 || fields[0].isBlank()) return@mapNotNull null
            StashEntry(
                selector = fields[0],
                hash = fields[1],
                message = fields[2],
                time = fields[3].toLongOrNull() ?: 0L,
                authorName = fields.getOrNull(4).orEmpty(),
                authorEmail = fields.getOrNull(5).orEmpty(),
            )
        }.toList()
    }

    override fun stashShow(
        repository: Repository,
        selector: String,
        maxBytes: Long,
    ): StashDetail {
        require(maxBytes > 0) { "maxBytes must be positive" }
        val workTree = repository.workTree
            ?: throw IllegalStateException("Stash is unavailable for a bare repository")
        // --include-untracked показывает второй родительский коммит stash,
        // созданный через -u/-a. На старом git опция может отсутствовать.
        fun show(vararg args: String): GitResult {
            val withUntracked = run(
                cwd = workTree,
                args = listOf("stash", "show", "--include-untracked") + args + selector,
            )
            if (withUntracked.ok || withUntracked.stdoutTruncated) return withUntracked
            return run(
                cwd = workTree,
                args = listOf("stash", "show") + args + selector,
            )
        }

        val filesResult = show("--name-status", "--no-color")
        if (!filesResult.ok && !filesResult.stdoutTruncated) throw GitCommandException(filesResult)
        val files = filesResult.stdout.lineSequence().mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size < 2) return@mapNotNull null
            StashFileChange(
                status = fields[0],
                path = fields.last(),
                oldPath = fields.getOrNull(1)?.takeIf { fields.size > 2 },
            )
        }.toList()

        // Как и для commitInspection, сначала оцениваем объём. Иначе огромный
        // stash patch всё равно заполнил бы 8 МиБ до срабатывания общего лимита.
        val statResult = show("--numstat", "--no-color")
        if (!statResult.ok && !statResult.stdoutTruncated) throw GitCommandException(statResult)
        val stats = CommitStats(CommitStatParser.parseNumstat(statResult.stdout))
        val changedLines = stats.additions.toLong() + stats.deletions.toLong()
        if (statResult.stdoutTruncated ||
            stats.files.size > MAX_INSPECTION_FILES ||
            changedLines > MAX_INSPECTION_LINES
        ) {
            return StashDetail(
                selector = selector,
                files = files,
                patch = "",
                truncated = true,
            )
        }

        val patchResult = show("--patch", "--no-color", "--no-ext-diff")
        if (!patchResult.ok && !patchResult.stdoutTruncated) throw GitCommandException(patchResult)
        val limit = maxBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val rawPatch = patchResult.stdout
        val truncated = filesResult.stdoutTruncated ||
            patchResult.stdoutTruncated ||
            rawPatch.length > limit
        return StashDetail(
            selector = selector,
            files = files,
            patch = if (truncated) rawPatch.take(limit) else rawPatch,
            truncated = truncated,
        )
    }

    override fun configEntries(repository: Repository): List<GitConfigEntry> {
        val result = run(
            cwd = repository.root,
            args = listOf(
                "config",
                "--list",
                "--show-origin",
                "--show-scope",
                "--null",
                "--includes",
            ),
            timeoutSeconds = 60,
        )
        if (!result.ok) throw GitCommandException(result)
        return ConfigParser.parse(result.stdout)
    }

    override fun setConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        value: String,
        oldValue: String?,
    ): GitResult {
        require(validGitConfigKey(key)) { "Invalid git config key" }
        val args = mutableListOf("config", scope.flag, "--replace-all")
        if (oldValue != null) args += "--fixed-value"
        args += listOf("--", key, value)
        if (oldValue != null) args += oldValue
        return run(
            cwd = repository.root,
            args = args,
            timeoutSeconds = 60,
        )
    }

    override fun addConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        value: String,
    ): GitResult {
        require(validGitConfigKey(key)) { "Invalid git config key" }
        return run(
            cwd = repository.root,
            args = listOf("config", scope.flag, "--add", "--", key, value),
            timeoutSeconds = 60,
        )
    }

    override fun unsetConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        oldValue: String?,
    ): GitResult {
        require(validGitConfigKey(key)) { "Invalid git config key" }
        val args = mutableListOf("config", scope.flag)
        if (oldValue == null) {
            args += listOf("--unset-all", "--", key)
        } else {
            args += listOf("--unset", "--fixed-value", "--", key, oldValue)
        }
        return run(
            cwd = repository.root,
            args = args,
            timeoutSeconds = 60,
        )
    }

    override fun runtimeState(repository: Repository): RepositoryRuntime =
        RepositoryRuntimeProbe.read(repository)

    override fun removeIndexLock(repository: Repository): Boolean =
        RepositoryRuntimeProbe.deleteIndexLock(repository)

    override fun readBlob(repository: Repository, spec: String, maxBytes: Long): GitBlob {
        require(maxBytes > 0) { "maxBytes must be positive" }
        val sizeResult = run(
            cwd = repository.root,
            args = listOf("cat-file", "-s", spec),
            timeoutSeconds = 30,
        )
        if (!sizeResult.ok) {
            return GitBlob(
                size = null,
                missing = true,
                error = sizeResult.stderr.trim().ifEmpty { sizeResult.stdout.trim() },
            )
        }
        val size = sizeResult.stdout.trim().toLongOrNull()
            ?: return GitBlob(size = null, error = "Cannot parse blob size for $spec")
        if (size > maxBytes) return GitBlob(size = size, tooLarge = true)

        return try {
            val bytes = readBinary(
                cwd = repository.root,
                args = listOf("cat-file", "blob", spec),
                maxBytes = maxBytes,
                timeoutSeconds = 60,
            )
            if (bytes == null) {
                GitBlob(size = size, error = "Cannot read blob $spec")
            } else {
                GitBlob(size = size, bytes = bytes)
            }
        } catch (error: Exception) {
            GitBlob(size = size, error = error.message ?: error.toString())
        }
    }

    override fun run(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
    ): GitResult = runInternal(cwd, args, stdin, timeoutSeconds, environment, control = null)

    override fun runControlled(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
        control: GitRunControl,
    ): GitResult = runInternal(cwd, args, stdin, timeoutSeconds, environment, control)

    private fun runInternal(
        cwd: String?,
        args: List<String>,
        stdin: String?,
        timeoutSeconds: Long,
        environment: Map<String, String>,
        control: GitRunControl?,
    ): GitResult {
        require(timeoutSeconds > 0) { "timeoutSeconds must be positive" }
        val directory = cwd?.let {
            val file = File(it)
            require(file.isDirectory) { "Not a directory: $it" }
            file
        }

        val authEnabled = authRequired(args)
        val preparedAuth = if (authEnabled) auth.prepare() else null
        val effectiveArgs = if (control != null) args.withProgressFlag() else args
        try {
            val command = buildList {
                add(executable)
                if (cwd != null) {
                    add("-C")
                    add(cwd)
                }
                add("--no-pager")
                add("-c")
                add("color.ui=false")
                add("-c")
                add("core.quotepath=false")
                // SEC-05: never let a global `protocol.ext.allow=always`
                // re-enable the shell-backed ext transport for this process.
                add("-c")
                add("protocol.ext.allow=never")
                if (preparedAuth != null) addAll(preparedAuth.configArgs)
                addAll(effectiveArgs)
            }

            val builder = ProcessBuilder(command)
            if (directory != null) builder.directory(directory)
            builder.redirectErrorStream(false)
            builder.environment().apply {
                put("GIT_TERMINAL_PROMPT", "0")
                put("GIT_PAGER", "cat")
                put("LC_ALL", "C")
                put("LANG", "C")
                // Не наследуем окружение чужого git-процесса: иначе все команды уедут не в тот репозиторий.
                listOf(
                    "GIT_DIR",
                    "GIT_WORK_TREE",
                    "GIT_INDEX_FILE",
                    "GIT_OBJECT_DIRECTORY",
                    "GIT_ALTERNATE_OBJECT_DIRECTORIES",
                    "GIT_COMMON_DIR",
                    "GIT_NAMESPACE",
                    "GIT_ASKPASS",
                    "SSH_ASKPASS",
                ).forEach { remove(it) }
                // Do not let an inherited Git config inject a credential helper or
                // any other auth setting into this invocation.
                keys.filter { it.startsWith("GIT_CONFIG_") }.forEach { remove(it) }
                if (authEnabled && auth.sshMode == SshAuthMode.Key) {
                    remove("GIT_SSH_COMMAND")
                    remove("GIT_SSH")
                }
                putAll(environment)
                preparedAuth?.environment?.let { putAll(it) }
            }

            val startedAt = System.nanoTime()
            val process = try {
                builder.start()
            } catch (error: IOException) {
                throw GitExecutableNotFoundException(executable, error)
            }

            val stdoutCapture = BoundedOutputReader(
                input = process.inputStream,
                limit = MAX_STDOUT_BYTES,
                onLimit = { destroyTree(process) },
            )
            val stderrCapture = BoundedOutputReader(
                input = process.errorStream,
                limit = MAX_STDERR_BYTES,
                onLimit = { destroyTree(process) },
                onChunk = control?.let { runControl ->
                    { bytes, length ->
                        runControl.reportProgress(
                            String(bytes, 0, length, Charsets.UTF_8),
                        )
                    }
                },
            )
            val stdoutReader = thread(name = "git-stdout", isDaemon = true) {
                stdoutCapture.run()
            }
            val stderrReader = thread(name = "git-stderr", isDaemon = true) {
                stderrCapture.run()
            }

            try {
                process.outputStream.use { stream ->
                    if (stdin != null) stream.write(stdin.toByteArray(Charsets.UTF_8))
                }
            } catch (_: IOException) {
                // Команда могла завершиться, не дочитав stdin.
            }

            var finished = false
            var timedOut = false
            var cancelled = false
            try {
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
                while (true) {
                    if (control?.isCancelled() == true) {
                        cancelled = true
                        destroyTree(process)
                        break
                    }
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) {
                        timedOut = true
                        destroyTree(process)
                        break
                    }
                    val waitMillis = TimeUnit.NANOSECONDS.toMillis(remaining).coerceIn(25L, 100L)
                    if (process.waitFor(waitMillis, TimeUnit.MILLISECONDS)) {
                        finished = true
                        break
                    }
                }
            } catch (error: InterruptedException) {
                destroyTree(process)
                Thread.currentThread().interrupt()
                throw error
            }

            stdoutReader.join(2_000)
            stderrReader.join(2_000)

            val stdout = stdoutCapture.bytes().toString(Charsets.UTF_8)
            val stderr = stderrCapture.bytes().toString(Charsets.UTF_8)
            // A configured token is redacted from every command's output, not
            // only network commands: local commands and hooks can echo it too.
            val secret = auth.token.takeIf { it.isNotBlank() }
            return GitResult(
                executable = executable,
                args = effectiveArgs,
                cwd = cwd,
                exitCode = if (finished) process.exitValue() else -1,
                stdout = redactSecret(stdout, secret),
                stderr = redactSecret(stderr, secret),
                durationMs = (System.nanoTime() - startedAt) / 1_000_000,
                timedOut = timedOut,
                cancelled = cancelled,
                stdoutTruncated = stdoutCapture.truncated,
                stderrTruncated = stderrCapture.truncated,
            )
        } finally {
            preparedAuth?.cleanup()
        }
    }

    /**
     * Git печатает progress только в терминал, если явно не передан --progress.
     * Добавляем флаг только для команд, которые его понимают, и не спорим с -q.
     */
    private fun List<String>.withProgressFlag(): List<String> {
        val command = firstOrNull()?.lowercase() ?: return this
        if (command !in setOf("clone", "fetch", "pull", "push")) return this
        if (any { it == "--progress" || it == "--no-progress" || it == "-q" || it == "--quiet" }) {
            return this
        }
        return buildList {
            add(first())
            add("--progress")
            addAll(drop(1))
        }
    }

    private fun redactSecret(text: String, secret: String?): String =
        if (secret.isNullOrBlank()) text else text.replace(secret, "***")

    private fun resolveCommonDir(cwd: String, root: String): String {
        val absolute = run(cwd, listOf("rev-parse", "--path-format=absolute", "--git-common-dir"))
        if (absolute.ok) {
            return File(absolute.stdout.trim()).canonicalPath
        }

        val relative = run(cwd, listOf("rev-parse", "--git-common-dir"))
        if (!relative.ok) throw GitCommandException(relative)
        val value = relative.stdout.trim()
        val file = File(value)
        return (if (file.isAbsolute) file else File(root, value)).canonicalPath
    }

    private data class MissingRepositoryMetadata(
        val root: File,
        val missing: List<String>,
    )

    /**
     * Когда Git отвечает «not a git repository», проверяем, не является ли
     * причина локальной потерей служебных файлов. Ищем и в родительских
     * каталогах: пользователь мог открыть подпапку репозитория.
     */
    private fun findBrokenRepositoryMetadata(start: File): MissingRepositoryMetadata? {
        var current: File? = start
        repeat(64) {
            val directory = current ?: return null
            val dotGit = File(directory, ".git")
            when {
                dotGit.isDirectory -> return missingInGitDirectory(directory, dotGit)
                dotGit.isFile -> return missingInLinkedWorktree(directory, dotGit)
                looksLikeBareRepository(directory) -> return missingInBareRepository(directory)
            }
            current = directory.parentFile
        }
        return null
    }

    private fun missingInGitDirectory(
        root: File,
        gitDir: File,
    ): MissingRepositoryMetadata? {
        val missing = buildList {
            if (!File(gitDir, "objects").isDirectory) add(".git/objects")
            if (!File(gitDir, "HEAD").isFile) add(".git/HEAD")
        }
        return if (missing.isEmpty()) null else MissingRepositoryMetadata(root, missing)
    }

    private fun missingInLinkedWorktree(
        root: File,
        dotGit: File,
    ): MissingRepositoryMetadata? {
        val gitDir = readGitDirPointer(dotGit)
            ?: return MissingRepositoryMetadata(root, listOf(".git"))
        if (!gitDir.isDirectory) return MissingRepositoryMetadata(root, listOf(".git"))

        val commonDir = readCommonDir(gitDir) ?: gitDir
        val missing = buildList {
            if (!File(commonDir, "objects").isDirectory) add(".git/objects")
            if (!File(gitDir, "HEAD").isFile) add(".git/HEAD")
        }
        return if (missing.isEmpty()) null else MissingRepositoryMetadata(root, missing)
    }

    private fun missingInBareRepository(root: File): MissingRepositoryMetadata? {
        val missing = buildList {
            if (!File(root, "objects").isDirectory) add("objects")
            if (!File(root, "HEAD").isFile) add("HEAD")
        }
        return if (missing.isEmpty()) null else MissingRepositoryMetadata(root, missing)
    }

    private fun looksLikeBareRepository(directory: File): Boolean =
        File(directory, "HEAD").isFile &&
            (File(directory, "objects").isDirectory || File(directory, "refs").isDirectory)

    private fun readGitDirPointer(file: File): File? {
        val text = readSmallMetadataFile(file) ?: return null
        val line = text.lineSequence().firstOrNull()?.trim() ?: return null
        if (!line.startsWith("gitdir:")) return null
        val raw = line.substringAfter(':').trim()
        if (raw.isEmpty() || raw.any { it == '\u0000' }) return null
        val target = File(raw)
        return if (target.isAbsolute) target else File(file.parentFile, raw)
    }

    private fun readCommonDir(gitDir: File): File? {
        val file = File(gitDir, "commondir")
        if (!file.isFile) return null
        val raw = readSmallMetadataFile(file)?.trim().orEmpty()
        if (raw.isEmpty() || raw.any { it == '\u0000' }) return null
        val target = File(raw)
        return if (target.isAbsolute) target else File(gitDir, raw)
    }

    private fun readSmallMetadataFile(file: File): String? {
        val bytes = runCatching {
            file.inputStream().use { it.readNBytes(MAX_GIT_METADATA_POINTER_BYTES + 1) }
        }.getOrNull() ?: return null
        if (bytes.size > MAX_GIT_METADATA_POINTER_BYTES) return null
        return bytes.toString(Charsets.UTF_8)
    }


    /** Отдельный запуск для бинарного stdout: обычный run декодирует его как UTF-8. */
    private fun readBinary(
        cwd: String,
        args: List<String>,
        maxBytes: Long,
        timeoutSeconds: Long,
    ): ByteArray? {
        val command = buildList {
            add(executable)
            add("-C")
            add(cwd)
            add("--no-pager")
            add("-c")
            add("color.ui=false")
            add("-c")
            add("core.quotepath=false")
            add("-c")
            add("protocol.ext.allow=never")
            addAll(args)
        }
            val builder = ProcessBuilder(command).directory(File(cwd))
            builder.redirectErrorStream(false)
            builder.environment().apply {
                put("GIT_TERMINAL_PROMPT", "0")
                put("GIT_PAGER", "cat")
                put("LC_ALL", "C")
                put("LANG", "C")
                listOf(
                    "GIT_DIR",
                    "GIT_WORK_TREE",
                    "GIT_INDEX_FILE",
                    "GIT_OBJECT_DIRECTORY",
                    "GIT_ALTERNATE_OBJECT_DIRECTORIES",
                    "GIT_COMMON_DIR",
                    "GIT_NAMESPACE",
                ).forEach { remove(it) }
            }

            val process = builder.start()
            val stderrCapture = BoundedOutputReader(
                input = process.errorStream,
                limit = MAX_BINARY_STDERR_BYTES,
                onLimit = { destroyTree(process) },
            )
            val stdoutCapture = BoundedOutputReader(
                input = process.inputStream,
                limit = maxBytes,
                onLimit = { destroyTree(process) },
            )
            val stderrReader = thread(name = "git-binary-stderr", isDaemon = true) {
                stderrCapture.run()
            }
            val stdoutReader = thread(name = "git-binary-stdout", isDaemon = true) {
                stdoutCapture.run()
            }

            val finished = try {
                process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            } catch (error: InterruptedException) {
                destroyTree(process)
                Thread.currentThread().interrupt()
                return null
            }
            if (!finished) destroyTree(process)
            stdoutReader.join(2_000)
            stderrReader.join(2_000)
            if (!finished || process.exitValue() != 0 || stdoutCapture.truncated || stderrCapture.truncated) return null
            if (stdoutCapture.bytes().size.toLong() > maxBytes) return null
            return stdoutCapture.bytes()
        }

        private fun destroyTree(process: Process) {
            runCatching {
                process.toHandle().descendants().forEach { it.destroy() }
            }
            process.destroy()
            val stopped = runCatching { process.waitFor(1, TimeUnit.SECONDS) }.getOrDefault(false)
            if (!stopped) {
                runCatching {
                    process.toHandle().descendants().forEach { it.destroyForcibly() }
                }
                process.destroyForcibly()
            }
        }

        fun run(
            cwd: String?,
            args: List<String>,
            stdin: String? = null,
            timeoutSeconds: Long = 120,
        ): GitResult = run(cwd, args, stdin, timeoutSeconds, emptyMap())
    }

    /**
     * Only commands that can contact a remote get auth material. Local forms such
     * as `remote -v`, `submodule status` and `submodule init` stay token-free.
     */
    internal fun authRequired(args: List<String>): Boolean {
        val command = args.firstOrNull()?.lowercase() ?: return false
        val subcommand = args.getOrNull(1)?.lowercase()
        return when (command) {
            "clone", "credential", "fetch", "ls-remote", "pull", "push" -> true
            "archive" -> args.any { it == "--remote" || it.startsWith("--remote=") }
            "remote" -> subcommand in setOf("update", "prune", "show")
            "submodule" -> subcommand in setOf("add", "update")
            else -> false
        }
    }
