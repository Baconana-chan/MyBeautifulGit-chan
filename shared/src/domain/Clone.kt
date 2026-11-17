package org.baconana.mybeautifulgit_chan.domain

private val cloneUrlSchemes = setOf("http", "https", "ssh", "git")
private val repositoryNameForbidden = "\\/:*?\"<>|"

/**
 * Git options and the repository URL are separated by `--`: a URL that starts
 * with `-` must never be interpreted as an option (for example --upload-pack).
 */
fun cloneArgs(url: String, target: String): List<String> = listOf(
    "clone",
    "--progress",
    "--",
    url.trim(),
    target.trim(),
)

/** The path is separated from `git init` options for the same reason as in [cloneArgs]. */
fun initArgs(branch: String, path: String): List<String> = listOf(
    "init",
    "-b",
    branch.trim(),
    "--",
    path.trim(),
)

/**
 * Clone accepts HTTP(S), SSH and git transports as well as local paths
 * (including scp-like git@host:repo). Unknown URI schemes and Git's ext
 * transport are rejected before the process is started.
 */
fun validCloneUrl(value: String): Boolean {
    if (value.any { it.isISOControl() }) return false
    val clean = value.trim()
    if (clean.isEmpty() || clean.startsWith("-")) return false
    if (clean.startsWith("ext:", ignoreCase = true)) return false

    val schemeEnd = clean.indexOf("://")
    if (schemeEnd < 0) return true

    val scheme = clean.substring(0, schemeEnd).lowercase()
    if (scheme !in cloneUrlSchemes) return false
    val authority = clean.substring(schemeEnd + 3)
    return authority.isNotEmpty() && !authority.startsWith("-")
}

/** A future path or an existing parent directory; `.`/`..` segments are not allowed. */
fun validCloneTarget(value: String): Boolean {
    if (value.any { it.isISOControl() }) return false
    val clean = value.trim()
    if (clean.isEmpty() || clean.startsWith("-")) return false
    val normalized = clean.replace('\\', '/')
    return normalized.split('/').none { it == "." || it == ".." }
}

/** Same restrictions as the settings screen plus the ref-name rules Git enforces for -b. */
fun validInitBranch(value: String): Boolean {
    if (value.any { it.isISOControl() }) return false
    val clean = value.trim()
    if (clean.isEmpty() || clean.startsWith("-")) return false
    if (clean.any { it.isWhitespace() || it.isISOControl() }) return false
    if (clean == "." || clean == ".." || clean.contains("..")) return false
    if (clean.contains("@{") || clean.contains("//")) return false
    if (clean.endsWith("/") || clean.endsWith(".") || clean.endsWith(".lock")) return false
    return clean.none { it in "~^:?*[\\" }
}

/**
 * Uses only the last URL component (or the scp-like repository name) as a
 * directory name. This prevents a URL such as `.../../../outside` from
 * becoming part of the clone target.
 */
fun repositoryNameFromUrl(url: String): String {
    val clean = url.trim().substringBefore('#').substringBefore('?').trimEnd('/', '\\')
    val separator = maxOf(clean.lastIndexOf('/'), clean.lastIndexOf('\\'))
    val tail = when {
        separator >= 0 -> clean.substring(separator + 1)
        ':' in clean -> clean.substringAfterLast(':')
        else -> clean
    }
    val name = tail.removeSuffix(".git").trim()
    return if (validRepositoryName(name)) name else "repository"
}

internal fun validRepositoryName(value: String): Boolean {
    if (value.any { it.isISOControl() }) return false
    val clean = value.trim()
    if (clean.isEmpty() || clean.length > 120) return false
    if (clean == "." || clean == ".." || clean.startsWith("-")) return false
    if (clean.any { it.isISOControl() || it in repositoryNameForbidden }) return false
    return true
}
