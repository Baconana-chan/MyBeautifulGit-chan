package org.baconana.mybeautifulgit_chan.platform

import org.baconana.mybeautifulgit_chan.domain.redactUrlCredentials
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties

/**
 * История и избранное raw-команд, отдельно для каждого репозитория.
 *
 * Файл намеренно один и в properties-формате: команды остаются читаемыми,
 * а репозитории разделяются стабильным хэшем канонического пути. URL userinfo
 * вырезается до записи, чтобы token не попал в историю.
 */
class CommandRunnerStore(
    private val file: Path = AppPaths.commandRunnerFile(),
    private val historyLimit: Int = 50,
    private val favoriteLimit: Int = 30,
) {
    init {
        require(historyLimit > 0) { "historyLimit must be positive" }
        require(favoriteLimit > 0) { "favoriteLimit must be positive" }
    }

    fun loadHistory(repositoryPath: String): List<String> =
        load(repositoryPath, HISTORY, historyLimit)

    fun addHistory(repositoryPath: String, command: String): List<String> {
        val clean = sanitize(command) ?: return loadHistory(repositoryPath)
        val updated = (listOf(clean) + loadHistory(repositoryPath))
            .distinct()
            .take(historyLimit)
        return runCatching { write(repositoryPath, HISTORY, updated) }
            .getOrElse { loadHistory(repositoryPath) }
    }

    fun clearHistory(repositoryPath: String): List<String> =
        runCatching { write(repositoryPath, HISTORY, emptyList()) }
            .getOrElse { loadHistory(repositoryPath) }

    fun loadFavorites(repositoryPath: String): List<String> =
        load(repositoryPath, FAVORITES, favoriteLimit)

    /** Новые избранные идут первыми; повторный вызов для той же команды удаляет её. */
    fun toggleFavorite(repositoryPath: String, command: String): List<String> {
        val clean = sanitize(command) ?: return loadFavorites(repositoryPath)
        val current = loadFavorites(repositoryPath)
        val updated = if (clean in current) {
            current - clean
        } else {
            (listOf(clean) + current).take(favoriteLimit)
        }
        return runCatching { write(repositoryPath, FAVORITES, updated) }
            .getOrElse { loadFavorites(repositoryPath) }
    }

    private fun load(repositoryPath: String, section: String, limit: Int): List<String> {
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(file)) return emptyList()
        OwnerOnlyPermissions.restrictFile(file)
        return runCatching {
            val properties = read()
            val prefix = prefix(repositoryPath, section)
            (0 until limit)
                .mapNotNull { index -> properties.getProperty("$prefix$index")?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }.getOrDefault(emptyList())
    }

    private fun write(repositoryPath: String, section: String, commands: List<String>): List<String> {
        val parent = file.parent ?: return commands
        Files.createDirectories(parent)
        OwnerOnlyPermissions.restrictDirectory(parent)

        val properties = read()
        val prefix = prefix(repositoryPath, section)
        properties.stringPropertyNames()
            .filter { it.startsWith(prefix) }
            .forEach(properties::remove)
        commands.forEachIndexed { index, command ->
            properties.setProperty("$prefix$index", command)
        }

        val temp = Files.createTempFile(parent, "commands-", ".tmp")
        try {
            OwnerOnlyPermissions.restrictFile(temp)
            Files.newOutputStream(temp).use {
                properties.store(it, "MyBeautifulGit-chan command runner")
            }
            runCatching {
                Files.move(
                    temp,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }.getOrElse {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
            }
            OwnerOnlyPermissions.restrictFile(file)
        } finally {
            Files.deleteIfExists(temp)
        }
        return commands
    }

    private fun read(): Properties {
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(file)) return Properties()
        OwnerOnlyPermissions.restrictFile(file)
        return runCatching {
            Properties().apply {
                Files.newInputStream(file).use(::load)
            }
        }.getOrDefault(Properties())
    }

    private fun prefix(repositoryPath: String, section: String): String =
        "repo.${repositoryKey(repositoryPath)}.$section."

    private fun repositoryKey(repositoryPath: String): String {
        val normalized = runCatching { File(repositoryPath).canonicalPath }
            .getOrDefault(File(repositoryPath).absolutePath)
        val stable = if (isWindows()) normalized.lowercase() else normalized
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun sanitize(command: String): String? {
        val clean = command.trim()
        if (clean.isEmpty()) return null
        if (clean.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return redactUrlCredentials(clean)
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    private companion object {
        const val HISTORY = "history"
        const val FAVORITES = "favorites"
    }
}
