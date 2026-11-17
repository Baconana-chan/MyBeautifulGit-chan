package org.baconana.mybeautifulgit_chan.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandRunnerStoreTest {

    @Test
    fun `history is per repository newest first and unique`() {
        val dir = Files.createTempDirectory("git-chan-command-store")
        val file = dir.resolve("command-runner.properties")
        val first = Files.createDirectory(dir.resolve("first")).toString()
        val second = Files.createDirectory(dir.resolve("second")).toString()
        val store = CommandRunnerStore(file, historyLimit = 2)

        store.addHistory(first, "status --short")
        store.addHistory(first, "log --oneline")
        store.addHistory(first, "status --short")
        store.addHistory(second, "branch --all")

        assertEquals(
            listOf("status --short", "log --oneline"),
            CommandRunnerStore(file, historyLimit = 2).loadHistory(first),
        )
        assertEquals(listOf("branch --all"), store.loadHistory(second))

        assertEquals(emptyList(), store.clearHistory(first))
        assertTrue(store.loadHistory(first).isEmpty())
        assertEquals(listOf("branch --all"), store.loadHistory(second))
    }

    @Test
    fun `favorites toggle and survive a reload`() {
        val dir = Files.createTempDirectory("git-chan-command-favorites")
        val file = dir.resolve("command-runner.properties")
        val repo = Files.createDirectory(dir.resolve("repo")).toString()
        val store = CommandRunnerStore(file)

        assertEquals(listOf("fetch --prune"), store.toggleFavorite(repo, "fetch --prune"))
        assertEquals(
            listOf("status --short", "fetch --prune"),
            store.toggleFavorite(repo, "status --short"),
        )
        assertEquals(
            listOf("status --short", "fetch --prune"),
            CommandRunnerStore(file).loadFavorites(repo),
        )

        assertEquals(listOf("status --short"), store.toggleFavorite(repo, "fetch --prune"))
    }

    @Test
    fun `url credentials are redacted before persistence`() {
        val dir = Files.createTempDirectory("git-chan-command-redaction")
        val repo = Files.createDirectory(dir.resolve("repo")).toString()
        val store = CommandRunnerStore(dir.resolve("command-runner.properties"))

        store.addHistory(repo, "remote add origin https://user:secret@example.com/repo.git")

        assertEquals(
            listOf("remote add origin https://***@example.com/repo.git"),
            store.loadHistory(repo),
        )
    }

    @Test
    fun `blank and multiline commands are ignored`() {
        val dir = Files.createTempDirectory("git-chan-command-invalid")
        val repo = Files.createDirectory(dir.resolve("repo")).toString()
        val store = CommandRunnerStore(dir.resolve("command-runner.properties"))

        store.addHistory(repo, "   ")
        store.addHistory(repo, "status\n--short")

        assertTrue(store.loadHistory(repo).isEmpty())
    }
}
