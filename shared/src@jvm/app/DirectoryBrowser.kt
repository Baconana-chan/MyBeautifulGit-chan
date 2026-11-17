package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import org.baconana.mybeautifulgit_chan.i18n.Strings
import java.io.File

data class DirectoryEntry(
    val name: String,
    val path: String,
    val isRepository: Boolean,
    val isBareRepository: Boolean,
)

data class DirectoryListing(
    val directory: String,
    val parent: String?,
    val entries: List<DirectoryEntry>,
    val repositoryRoot: String?,
    val isBareRepository: Boolean,
    val error: String? = null,
)

/**
 * Небольшой файловый браузер только для выбора папки репозитория.
 * Никаких рекурсивных обходов: один уровень, чтобы сетевые шары и огромные
 * каталоги не превращали открытие в зависание.
 */
object DirectoryBrowser {

    fun normalize(path: String): String =
        runCatching { File(path).canonicalPath }.getOrElse { File(path).absolutePath }

    fun homeDirectory(): String = normalize(System.getProperty("user.home"))

    fun roots(): List<String> =
        runCatching { File.listRoots().map { normalize(it.path) } }
            .getOrDefault(emptyList())
            .distinct()

    fun list(
        path: String,
        showHidden: Boolean = false,
        strings: Strings = EnglishStrings,
    ): DirectoryListing {
        val requested = File(path)
        val directory = when {
            requested.isDirectory -> requested
            requested.isFile -> requested.parentFile ?: requested
            else -> requested
        }
        val normalized = normalize(directory.path)
        val target = File(normalized)

        if (!target.isDirectory) {
            return DirectoryListing(
                directory = normalized,
                parent = target.parentFile?.let { normalize(it.path) },
                entries = emptyList(),
                repositoryRoot = null,
                isBareRepository = false,
                error = strings.directoryNotFound,
            )
        }

        val children = runCatching { target.listFiles() }.getOrNull()
        if (children == null) {
            return DirectoryListing(
                directory = normalized,
                parent = target.parentFile?.let { normalize(it.path) },
                entries = emptyList(),
                repositoryRoot = null,
                isBareRepository = false,
                error = strings.directoryNoAccess,
            )
        }

        val entries = children.asSequence()
            .filter { runCatching { it.isDirectory }.getOrDefault(false) }
            .filter { showHidden || !isHidden(it) }
            .map { child ->
                val bare = isBareRepository(child)
                DirectoryEntry(
                    name = child.name,
                    path = normalize(child.path),
                    isRepository = isGitRepository(child) || bare,
                    isBareRepository = bare,
                )
            }
            .sortedWith(
                compareByDescending<DirectoryEntry> { it.isRepository }
                    .thenBy { it.name.lowercase() },
            )
            .toList()

        val root = findRepositoryRoot(target)
        return DirectoryListing(
            directory = normalized,
            parent = target.parentFile?.let { normalize(it.path) },
            entries = entries,
            repositoryRoot = root,
            isBareRepository = root?.let { isBareRepository(File(it)) } == true,
        )
    }

    fun isGitRepository(path: File): Boolean =
        runCatching { File(path, ".git").exists() }.getOrDefault(false)

    private fun isHidden(file: File): Boolean =
        file.name.startsWith(".") || runCatching { file.isHidden }.getOrDefault(false)

    fun isBareRepository(path: File): Boolean =
        runCatching {
            File(path, "HEAD").isFile &&
                File(path, "objects").isDirectory &&
                File(path, "refs").isDirectory
        }.getOrDefault(false)

    private fun findRepositoryRoot(start: File): String? {
        var current: File? = start
        repeat(64) {
            val directory = current ?: return null
            if (isGitRepository(directory) || isBareRepository(directory)) {
                return normalize(directory.path)
            }
            current = directory.parentFile
        }
        return null
    }
}
