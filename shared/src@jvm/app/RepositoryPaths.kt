package org.baconana.mybeautifulgit_chan.app

import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A repository-controlled path tried to escape its base directory or passed
 * through a symbolic link. Callers turn this into a localized UI error.
 */
internal class RepositoryPathException(message: String) : IllegalArgumentException(message)

/**
 * Path checks for files that are written or deleted on behalf of a repository.
 *
 * All checks use canonical paths and reject symlinks in every existing path
 * segment. Writes and deletes additionally use [LinkOption.NOFOLLOW_LINKS]
 * where the platform supports it. These checks are best-effort: a hostile
 * process running as the same OS user can still win a TOCTOU race between the
 * check and the operation. They prevent a tracked symlink from redirecting a
 * normal repository action, not a local attacker who can race the app.
 *
 * Windows note: symlink creation usually needs Developer Mode or elevated
 * privileges. Junctions and other reparse points are not fully distinguishable
 * from directories by Java's portable API; canonical containment and NOFOLLOW
 * checks are the practical limit there. This limitation is documented in
 * SECURITY_TODO.md.
 */
internal data class LimitedRepositoryText(
    val text: String,
    val truncated: Boolean,
)

private const val MAX_REPOSITORY_READ_BYTES = 16L * 1024 * 1024

internal object RepositoryPaths {

    /** Resolves [relative] against [base] and rejects traversal and symlinks. */
    fun resolve(
        base: File,
        relative: String,
        allowFinalSymlink: Boolean = false,
    ): Path {
        if (!base.isDirectory) {
            throw RepositoryPathException("base is not a directory: ${base.path}")
        }
        val root = try {
            base.toPath().toRealPath()
        } catch (error: IOException) {
            throw RepositoryPathException("cannot resolve base directory: ${error.message}")
        }
        val cleaned = validateRelative(relative)
        val resolved = try {
            root.resolve(cleaned).normalize()
        } catch (error: Exception) {
            throw RepositoryPathException("invalid repository path: ${error.message}")
        }
        if (!resolved.startsWith(root)) {
            throw RepositoryPathException("path leaves its base directory")
        }
        checkSegments(root, resolved, allowFinalSymlink)
        return resolved
    }

    fun exists(base: File, relative: String): Boolean =
        Files.exists(resolve(base, relative), LinkOption.NOFOLLOW_LINKS)

    fun isRegularFile(base: File, relative: String): Boolean =
        Files.isRegularFile(resolve(base, relative), LinkOption.NOFOLLOW_LINKS)

    fun readText(base: File, relative: String): String =
        readTextLimited(base, relative, Long.MAX_VALUE).text

    /** Reads at most [maxBytes] and leaves the rest of the file untouched. */
    fun readTextLimited(base: File, relative: String, maxBytes: Long): LimitedRepositoryText {
        val path = resolve(base, relative)
        val limit = maxBytes.coerceIn(0L, MAX_REPOSITORY_READ_BYTES).toInt()
        return try {
            Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { input ->
                val bytes = input.readNBytes(limit + 1)
                LimitedRepositoryText(
                    text = bytes.copyOf(minOf(bytes.size, limit)).toString(Charsets.UTF_8),
                    truncated = bytes.size > limit,
                )
            }
        } catch (error: IOException) {
            throw RepositoryPathException("cannot read ${base.path}/$relative: ${error.message}")
        }
    }

    /** Writes a file whose parent already exists. */
    fun writeText(base: File, relative: String, text: String): Path {
        val path = resolve(base, relative)
        val parent = path.parent ?: throw RepositoryPathException("path has no parent")
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw RepositoryPathException("parent is not a directory: $parent")
        }
        writeExistingPath(path, text)
        return path
    }

    fun deleteFile(base: File, relative: String): Boolean {
        val path = resolve(base, relative)
        return try {
            Files.deleteIfExists(path)
        } catch (error: IOException) {
            throw RepositoryPathException("cannot delete ${base.path}/$relative: ${error.message}")
        }
    }

    /** Creates missing directory segments without following symlinks. */
    fun createDirectories(base: File, relative: String): Path {
        val target = resolve(base, relative)
        val root = base.toPath().toRealPath()
        var current = root
        for (segment in root.relativize(target)) {
            current = current.resolve(segment)
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current) ||
                    !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                ) {
                    throw RepositoryPathException("path segment is not a real directory: $current")
                }
            } else {
                try {
                    Files.createDirectory(current)
                } catch (_: FileAlreadyExistsException) {
                    if (Files.isSymbolicLink(current) ||
                        !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    ) {
                        throw RepositoryPathException("path segment became unsafe: $current")
                    }
                } catch (error: IOException) {
                    throw RepositoryPathException("cannot create directory $current: ${error.message}")
                }
            }
        }
        return resolve(base, relative)
    }

    /**
     * Deletes a file or tree without following symlinks. When
     * [allowFinalSymlink] is true, a symlink at the target is unlinked itself;
     * its target is never touched.
     */
    fun deleteTree(
        base: File,
        relative: String,
        allowFinalSymlink: Boolean = false,
    ): Boolean {
        val path = resolve(base, relative, allowFinalSymlink)
        return deleteNoFollow(path)
    }

    /**
     * Writes a file in an externally configured directory (for example a
     * custom core.hooksPath). The final file and the directory itself must not
     * be symlinks; missing directories are created.
     */
    fun writeExternalFile(file: File, text: String): Path {
        val target = externalTarget(file)
        writeExistingPath(target, text)
        return target
    }

    /**
     * Checks every existing segment of an absolute external path, including
     * the final segment. Missing trailing segments are allowed so a path can
     * be validated before it is created.
     */
    fun checkExternalPath(file: File): Path {
        val path = file.toPath().toAbsolutePath().normalize()
        var current = path.root ?: return path
        for (segment in path) {
            current = current.resolve(segment)
            if (Files.isSymbolicLink(current)) {
                throw RepositoryPathException("path segment is a symlink: $current")
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) break
        }
        return path
    }

    /** Opens an existing external file for reading without following any symlink segment. */
    fun existingExternalFile(file: File): Path {
        val path = checkExternalPath(file)
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw RepositoryPathException("not a regular file: $path")
        }
        return path
    }

    /** Copies or moves a hook file after validating both ends. */
    fun copyExternalFile(source: File, target: File, move: Boolean = false): Path {
        val from = existingExternalFile(source)
        val to = externalTarget(target)
        if (Files.exists(to, LinkOption.NOFOLLOW_LINKS)) {
            throw RepositoryPathException("target already exists: $to")
        }
        try {
            if (move) {
                Files.move(from, to)
            } else {
                Files.copy(from, to, LinkOption.NOFOLLOW_LINKS)
            }
        } catch (error: IOException) {
            throw RepositoryPathException("cannot ${if (move) "move" else "copy"} $from: ${error.message}")
        }
        return to
    }

    private fun externalTarget(file: File): Path {
        val absolute = checkExternalPath(file)
        if (Files.isSymbolicLink(absolute)) {
            throw RepositoryPathException("refusing to write through a symlink: $absolute")
        }
        val parent = absolute.parent ?: throw RepositoryPathException("path has no parent")
        ensureExternalDirectory(parent)
        val target = parent.resolve(absolute.fileName)
        if (Files.isSymbolicLink(target)) {
            throw RepositoryPathException("refusing to write through a symlink: $target")
        }
        return target
    }

    private fun ensureExternalDirectory(path: Path): Path {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                throw RepositoryPathException("path is not a real directory: $path")
            }
            return path
        }
        val parent = path.parent ?: throw RepositoryPathException("directory has no parent: $path")
        ensureExternalDirectory(parent)
        try {
            Files.createDirectory(path)
        } catch (_: FileAlreadyExistsException) {
            if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                throw RepositoryPathException("path became unsafe: $path")
            }
        } catch (error: IOException) {
            throw RepositoryPathException("cannot create directory $path: ${error.message}")
        }
        return path
    }

    private fun writeExistingPath(path: Path, text: String) {
        try {
            Files.newOutputStream(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS,
            ).use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
        } catch (error: IOException) {
            throw RepositoryPathException("cannot write $path: ${error.message}")
        }
    }

    private fun deleteNoFollow(path: Path): Boolean {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return true
        if (Files.isSymbolicLink(path)) return Files.deleteIfExists(path)
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.newDirectoryStream(path).use { entries ->
                entries.forEach { child -> deleteNoFollow(child) }
            }
        }
        return Files.deleteIfExists(path)
    }

    private fun checkSegments(root: Path, path: Path, allowFinalSymlink: Boolean) {
        val segments = root.relativize(path).toList()
        var current = root
        for ((index, segment) in segments.withIndex()) {
            current = current.resolve(segment)
            if (index == segments.lastIndex && allowFinalSymlink) return
            if (Files.isSymbolicLink(current)) {
                throw RepositoryPathException("path segment is a symlink: $current")
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) return
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            val real = try {
                path.toRealPath()
            } catch (error: IOException) {
                throw RepositoryPathException("cannot resolve path: ${error.message}")
            }
            if (!real.startsWith(root)) {
                throw RepositoryPathException("path leaves its base directory")
            }
        }
    }

    private fun validateRelative(relative: String): Path {
        if (relative.isBlank() || relative.any { it == '\u0000' }) {
            throw RepositoryPathException("empty or invalid repository path")
        }
        val portable = relative.replace('\\', '/')
        if (portable.startsWith("/") || portable.matches(Regex("^[A-Za-z]:.*"))) {
            throw RepositoryPathException("absolute paths are not allowed")
        }
        val segments = portable.split('/')
        if (segments.any { it.isEmpty() || it == ".." }) {
            throw RepositoryPathException("path traversal is not allowed")
        }
        return try {
            Path.of(relative)
        } catch (error: Exception) {
            throw RepositoryPathException("invalid repository path: ${error.message}")
        }
    }
}
