package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.domain.Repository
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes

/**
 * Следит за `.git` и рабочим деревом и сообщает об изменениях после паузы.
 *
 * WatchService не умеет рекурсивно следить за каталогами, поэтому дерево
 * регистрируется вручную. Рабочее дерево ограничено по числу каталогов и
 * пропускает заведомо сгенерированные папки; для остального остаётся ручной
 * refresh. Сам watcher не делает git-вызовов и не блокирует UI: регистрация
 * идёт на Dispatchers.IO, callback вызывается после debounce.
 */
class RepoWatcher(
    private val repository: Repository,
    private val debounceMillis: Long = 300L,
    private val onChanged: () -> Unit,
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registered = mutableSetOf<Path>()
    private val worktreeRoot: Path? = repository.workTree
        ?.let { runCatching { Path.of(it).toRealPath() }.getOrNull() }
    private var watchService: WatchService? = null
    private var watchJob: Job? = null
    private var debounceJob: Job? = null

    @Volatile
    private var closed = false

    /**
     * Регистрирует каталоги и запускает цикл событий. Регистрация может быть
     * долгой на большом дереве, поэтому вызывать из корутины, а не из композиции.
     */
    suspend fun start() {
        if (watchJob != null || closed) return

        val service = withContext(Dispatchers.IO) {
            if (closed) return@withContext null
            val created = runCatching { FileSystems.getDefault().newWatchService() }.getOrNull()
                ?: return@withContext null
            if (closed) {
                runCatching { created.close() }
                return@withContext null
            }

            registerGitDirectories(created)
            registerWorktree(created)
            created
        } ?: return

        synchronized(this) {
            if (closed) {
                runCatching { service.close() }
                return
            }
            watchService = service
            watchJob = scope.launch { watchLoop(service) }
        }
    }

    override fun close() {
        val service = synchronized(this) {
            if (closed) return
            closed = true
            watchService.also { watchService = null }
        }
        debounceJob?.cancel()
        watchJob?.cancel()
        runCatching { service?.close() }
        scope.cancel()
    }

    private suspend fun watchLoop(service: WatchService) {
        while (true) {
            val key = try {
                service.take()
            } catch (_: ClosedWatchServiceException) {
                return
            } catch (_: InterruptedException) {
                return
            }

            val events = key.pollEvents()
            val directory = key.watchable() as? Path
            if (directory != null) {
                events.forEach { event ->
                    if (event.kind() == ENTRY_CREATE) {
                        val child = event.context() as? Path ?: return@forEach
                        registerNewDirectory(service, directory.resolve(child))
                    }
                }
            }
            key.reset()
            if (events.isNotEmpty()) scheduleRefresh()
        }
    }

    private fun scheduleRefresh() {
        if (closed) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMillis)
            if (!closed) runCatching { onChanged() }
        }
    }

    private fun registerGitDirectories(service: WatchService) {
        val gitDir = Path.of(repository.gitDir)
        val commonDir = Path.of(repository.commonDir)

        registerDirectory(service, gitDir)
        registerRecursive(service, gitDir.resolve("refs"), MAX_GIT_DIRECTORIES, emptySet())
        registerRecursive(service, gitDir.resolve("worktrees"), MAX_GIT_DIRECTORIES, emptySet())

        if (commonDir != gitDir) {
            registerDirectory(service, commonDir)
            registerRecursive(service, commonDir.resolve("refs"), MAX_GIT_DIRECTORIES, emptySet())
        }
    }

    private fun registerWorktree(service: WatchService) {
        val root = worktreeRoot ?: return
        registerRecursive(
            service = service,
            root = root,
            maxDirectories = MAX_WORKTREE_DIRECTORIES,
            skipNames = WORKTREE_SKIP,
            boundary = root,
        )
    }

    private fun registerNewDirectory(service: WatchService, path: Path) {
        val boundary = worktreeRoot ?: return
        // A WatchService event for a symlink must not turn its target into a watched directory.
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) return
        val directory = runCatching { path.toRealPath() }.getOrNull() ?: return
        if (!directory.startsWith(boundary)) return
        val name = directory.fileName?.toString()?.lowercase()
        if (name in WORKTREE_SKIP) return
        registerRecursive(
            service = service,
            root = directory,
            maxDirectories = MAX_WORKTREE_DIRECTORIES,
            skipNames = WORKTREE_SKIP,
            boundary = boundary,
        )
    }

    private fun registerRecursive(
        service: WatchService,
        root: Path,
        maxDirectories: Int,
        skipNames: Set<String>,
        boundary: Path? = null,
    ) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) return
        if (registered.size >= maxDirectories) return
        if (boundary != null) {
            val realRoot = runCatching { root.toRealPath() }.getOrNull() ?: return
            if (!realRoot.startsWith(boundary)) return
        }
        runCatching {
            Files.walkFileTree(
                root,
                object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(
                        directory: Path,
                        attrs: BasicFileAttributes,
                    ): FileVisitResult {
                        if (directory != root) {
                            val name = directory.fileName?.toString()?.lowercase()
                            if (name in skipNames) return FileVisitResult.SKIP_SUBTREE
                        }
                        if (boundary != null) {
                            if (Files.isSymbolicLink(directory)) return FileVisitResult.SKIP_SUBTREE
                            val realDirectory = runCatching { directory.toRealPath() }.getOrNull()
                            if (realDirectory == null || !realDirectory.startsWith(boundary)) {
                                return FileVisitResult.SKIP_SUBTREE
                            }
                        }
                        if (registered.size >= maxDirectories) return FileVisitResult.TERMINATE
                        registerDirectory(service, directory, boundary)
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                },
            )
        }
    }

    private fun registerDirectory(service: WatchService, path: Path, boundary: Path? = null) {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) return
        val directory = runCatching { path.toRealPath() }.getOrNull() ?: return
        if (boundary != null && !directory.startsWith(boundary)) return
        if (!registered.add(directory)) return
        runCatching {
            directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        }
    }

    private companion object {
        const val MAX_GIT_DIRECTORIES = 512
        const val MAX_WORKTREE_DIRECTORIES = 2_000

        val WORKTREE_SKIP = setOf(
            ".git",
            ".gradle",
            ".idea",
            ".venv",
            "venv",
            "__pycache__",
            "build",
            "dist",
            "node_modules",
            "out",
            "target",
        )
    }
}
