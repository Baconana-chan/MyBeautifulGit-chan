package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.GitOperation
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.IndexLock
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.RepositoryRuntime
import java.nio.file.Files
import java.nio.file.Path

/**
 * Читает state-файлы git напрямую, без запуска git.
 *
 * Это дешёвая проверка, которую можно делать на каждом refresh. Важен порядок:
 * во время rebase git тоже может оставить CHERRY_PICK_HEAD, поэтому каталоги
 * rebase проверяются раньше одиночных маркеров.
 */
object RepositoryRuntimeProbe {

    fun read(repository: Repository): RepositoryRuntime {
        val gitDir = Path.of(repository.gitDir)
        if (!Files.isDirectory(gitDir)) return RepositoryRuntime()
        return RepositoryRuntime(
            operation = detectOperation(gitDir),
            indexLock = detectIndexLock(repository, gitDir),
        )
    }

    fun deleteIndexLock(repository: Repository): Boolean {
        if (repository.workTree == null) return false
        return runCatching {
            Files.deleteIfExists(Path.of(repository.gitDir).resolve("index.lock"))
            true
        }.getOrDefault(false)
    }

    private fun detectOperation(gitDir: Path): GitOperation? {
        val rebaseMerge = gitDir.resolve("rebase-merge")
        if (Files.isDirectory(rebaseMerge)) {
            val progress = progress(rebaseMerge, "msgnum", "end")
            return GitOperation(
                kind = GitOperationKind.Rebase,
                step = progress.first,
                total = progress.second,
            )
        }

        val rebaseApply = gitDir.resolve("rebase-apply")
        if (Files.isDirectory(rebaseApply)) {
            val kind = when {
                Files.exists(rebaseApply.resolve("rebasing")) -> GitOperationKind.Rebase
                Files.exists(rebaseApply.resolve("applying")) -> GitOperationKind.Am
                else -> GitOperationKind.Rebase
            }
            val progress = progress(rebaseApply, "next", "last")
            return GitOperation(
                kind = kind,
                step = progress.first,
                total = progress.second,
            )
        }

        if (Files.isRegularFile(gitDir.resolve("MERGE_HEAD"))) {
            return GitOperation(GitOperationKind.Merge)
        }
        if (Files.isRegularFile(gitDir.resolve("CHERRY_PICK_HEAD"))) {
            return GitOperation(GitOperationKind.CherryPick)
        }
        if (Files.isRegularFile(gitDir.resolve("REVERT_HEAD"))) {
            return GitOperation(GitOperationKind.Revert)
        }
        if (
            Files.isRegularFile(gitDir.resolve("BISECT_LOG")) ||
            Files.isRegularFile(gitDir.resolve("BISECT_START"))
        ) {
            return GitOperation(GitOperationKind.Bisect)
        }
        return null
    }

    private fun detectIndexLock(repository: Repository, gitDir: Path): IndexLock? {
        if (repository.workTree == null) return null
        val lock = gitDir.resolve("index.lock")
        return if (Files.isRegularFile(lock)) IndexLock(lock.toString()) else null
    }

    private fun progress(directory: Path, currentName: String, totalName: String): Pair<Int?, Int?> =
        readPositiveInt(directory.resolve(currentName)) to
            readPositiveInt(directory.resolve(totalName))

    private fun readPositiveInt(path: Path): Int? =
        runCatching { Files.readString(path).trim().toIntOrNull()?.takeIf { it > 0 } }
            .getOrNull()
}
