package org.baconana.mybeautifulgit_chan.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

/**
 * Недавние репозитории в простом properties-файле. Формат намеренно скучный:
 * его легко прочитать, перенести и отладить, и он не тянет зависимостей.
 */
class RecentRepos(
    private val file: Path = AppPaths.recentFile(),
    private val limit: Int = 8,
) {
    init {
        require(limit > 0) { "limit must be positive" }
    }

    fun load(): List<String> {
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(file)) return emptyList()
        OwnerOnlyPermissions.restrictFile(file)
        return runCatching {
            val properties = Properties()
            Files.newInputStream(file).use(properties::load)
            (0 until limit)
                .mapNotNull { index -> properties.getProperty("repo.$index")?.trim()?.takeIf { it.isNotEmpty() } }
                .distinctBy(::key)
        }.getOrDefault(emptyList())
    }

    fun add(path: String): List<String> {
        val normalized = normalize(path)
        val updated = (listOf(normalized) + load())
            .distinctBy(::key)
            .take(limit)
        return runCatching { write(updated) }.getOrElse { load() }
    }

    fun remove(path: String): List<String> {
        val updated = load().filterNot { key(it) == key(normalize(path)) }
        return runCatching { write(updated) }.getOrElse { load() }
    }

    private fun write(paths: List<String>): List<String> {
        val parent = file.parent ?: return paths
        Files.createDirectories(parent)
        OwnerOnlyPermissions.restrictDirectory(parent)
        val properties = Properties()
        paths.forEachIndexed { index, path -> properties.setProperty("repo.$index", path) }
        val temp = Files.createTempFile(parent, "recent-", ".tmp")
        try {
            OwnerOnlyPermissions.restrictFile(temp)
            Files.newOutputStream(temp).use { properties.store(it, "MyBeautifulGit-chan recent repositories") }
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
        return paths
    }

    private fun normalize(path: String): String =
        runCatching { File(path).canonicalPath }.getOrDefault(File(path).absolutePath)

    private fun key(path: String): String =
        if (isWindows()) path.lowercase() else path

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")
}
