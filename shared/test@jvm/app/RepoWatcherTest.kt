package org.baconana.mybeautifulgit_chan.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.baconana.mybeautifulgit_chan.domain.Repository
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class RepoWatcherTest {

    @Test
    fun `worktree change triggers one debounced refresh`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-watch")
        val gitDir = Files.createDirectories(root.resolve(".git/refs/heads"))
        val file = root.resolve("notes.txt")
        val callbacks = AtomicInteger()
        val firstCallback = CompletableDeferred<Unit>()

        val watcher = RepoWatcher(
            repository = Repository(
                root = root.toString(),
                gitDir = gitDir.parent.parent.toString(),
                commonDir = gitDir.parent.parent.toString(),
                name = "watch-test",
                isBare = false,
                isWorktree = false,
                workTree = root.toString(),
            ),
            debounceMillis = 300L,
        ) {
            callbacks.incrementAndGet()
            firstCallback.complete(Unit)
        }

        try {
            watcher.start()

            repeat(5) { index ->
                Files.writeString(file, "change $index\n")
                delay(40)
            }

            withTimeout(10_000) { firstCallback.await() }
            delay(500)

            assertEquals(1, callbacks.get())
        } finally {
            watcher.close()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `symlink to an external directory is not watched`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-watch-symlink")
        val outside = Files.createTempDirectory("git-chan-watch-outside")
        val outsideFile = Files.writeString(outside.resolve("outside.txt"), "before\n")
        val link = root.resolve("outside-link")
        val gitDir = Files.createDirectories(root.resolve(".git/refs/heads"))
        if (runCatching { Files.createSymbolicLink(link, outside) }.isFailure) {
            root.toFile().deleteRecursively()
            outside.toFile().deleteRecursively()
            return@runBlocking
        }
        val callbacks = AtomicInteger()
        val watcher = RepoWatcher(repository(root, gitDir), debounceMillis = 100L) {
            callbacks.incrementAndGet()
        }

        try {
            watcher.start()
            // Let initial registration events (for example .git/refs) settle.
            delay(400)
            callbacks.set(0)
            Files.writeString(outsideFile, "after\n")
            delay(700)

            assertEquals(0, callbacks.get())
        } finally {
            watcher.close()
            Files.deleteIfExists(link)
            root.toFile().deleteRecursively()
            outside.toFile().deleteRecursively()
        }
    }

    @Test
    fun `new symlink is not registered as an external directory`() = runBlocking {
        val root = Files.createTempDirectory("git-chan-watch-new-symlink")
        val outside = Files.createTempDirectory("git-chan-watch-new-outside")
        val outsideFile = Files.writeString(outside.resolve("outside.txt"), "before\n")
        val link = root.resolve("late-link")
        val gitDir = Files.createDirectories(root.resolve(".git/refs/heads"))
        val callbacks = AtomicInteger()
        val watcher = RepoWatcher(repository(root, gitDir), debounceMillis = 100L) {
            callbacks.incrementAndGet()
        }

        try {
            watcher.start()
            delay(250)
            if (runCatching { Files.createSymbolicLink(link, outside) }.isFailure) return@runBlocking
            delay(400)
            callbacks.set(0)
            Files.writeString(outsideFile, "after\n")
            delay(700)

            assertEquals(0, callbacks.get())
        } finally {
            watcher.close()
            Files.deleteIfExists(link)
            root.toFile().deleteRecursively()
            outside.toFile().deleteRecursively()
        }
    }

    private fun repository(root: Path, gitDir: Path): Repository = Repository(
        root = root.toString(),
        gitDir = gitDir.parent.parent.toString(),
        commonDir = gitDir.parent.parent.toString(),
        name = "watch-test",
        isBare = false,
        isWorktree = false,
        workTree = root.toString(),
    )
}
