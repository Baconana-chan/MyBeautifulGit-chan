package org.baconana.mybeautifulgit_chan.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Repository paths and the foreground tab from the previous application run. */
data class RepositorySession(
    val repositories: List<String> = emptyList(),
    val active: String? = null,
)

/**
 * Persists open repository tabs in a small properties file. Like the recent
 * list, the format stays intentionally boring and readable. Missing folders
 * are skipped by the caller when the session is restored.
 */
class RepositorySessionStore(
    private val file: Path = AppPaths.sessionFile(),
    private val limit: Int = 16,
) {
    init {
        require(limit > 0) { "limit must be positive" }
    }

    fun load(): RepositorySession {
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(file)) return RepositorySession()
        OwnerOnlyPermissions.restrictFile(file)
        return runCatching {
            val properties = Properties()
            Files.newInputStream(file).use(properties::load)
            val repositories = (0 until limit)
                .mapNotNull { index ->
                    properties.getProperty("repo.$index")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                }
                .distinctBy(::key)
            val active = properties.getProperty(ACTIVE_KEY)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { path -> repositories.firstOrNull { key(it) == key(path) } }
                ?: repositories.firstOrNull()
            RepositorySession(repositories, active)
        }.getOrElse { RepositorySession() }
    }

    fun save(repositories: List<String>, active: String?): RepositorySession {
        val normalized = repositories
            .filter { it.isNotBlank() }
            .map(::normalize)
            .distinctBy(::key)
            .take(limit)
        val normalizedActive = active
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalize)
            ?.let { path -> normalized.firstOrNull { key(it) == key(path) } }
            ?: normalized.firstOrNull()
        val session = RepositorySession(normalized, normalizedActive)
        runCatching { write(session) }
        return session
    }

    private fun write(session: RepositorySession) {
        val parent = file.parent ?: return
        Files.createDirectories(parent)
        OwnerOnlyPermissions.restrictDirectory(parent)
        val properties = Properties()
        session.repositories.forEachIndexed { index, path ->
            properties.setProperty("repo.$index", path)
        }
        session.active?.let { properties.setProperty(ACTIVE_KEY, it) }
        val temp = Files.createTempFile(parent, "session-", ".tmp")
        try {
            OwnerOnlyPermissions.restrictFile(temp)
            Files.newOutputStream(temp).use {
                properties.store(it, "MyBeautifulGit-chan open repository tabs")
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
    }

    private fun normalize(path: String): String =
        runCatching { File(path).canonicalPath }.getOrDefault(File(path).absolutePath)

    private fun key(path: String): String =
        if (isWindows()) path.lowercase() else path

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    private companion object {
        const val ACTIVE_KEY = "active"
    }
}