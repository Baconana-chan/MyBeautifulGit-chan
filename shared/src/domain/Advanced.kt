package org.baconana.mybeautifulgit_chan.domain

/** Частичный клон выбирается при clone: существующий полный клон так не превратить. */
enum class PartialCloneFilter(val value: String, val label: String) {
    Full("", "FULL"),
    BlobNone("blob:none", "BLOB:NONE"),
    BlobLimit1M("blob:limit=1m", "BLOB:1M"),
    TreeZero("tree:0", "TREE:0"),
}

enum class ArchiveFormat(val gitFormat: String, val label: String, val extension: String) {
    Zip("zip", "ZIP", "zip"),
    TarGz("tar.gz", "TAR.GZ", "tar.gz"),
}

/** Результат одной advanced-команды; полный output показывается рядом с командой. */
data class AdvancedCommandResult(
    val command: String,
    val ok: Boolean,
    val exitCode: Int,
    val durationMs: Long,
    val text: String,
    val note: String? = null,
)

data class GitNoteEntry(
    val noteHash: String,
    val objectHash: String,
) {
    val shortObject: String get() = objectHash.take(10)
    val shortNote: String get() = noteHash.take(10)
}

data class SparseCheckoutState(
    val enabled: Boolean = false,
    val cone: Boolean = true,
    val patterns: List<String> = emptyList(),
)

data class LfsState(
    val available: Boolean = false,
    val version: String = "",
    val files: List<String> = emptyList(),
)

data class PartialCloneState(
    val enabled: Boolean = false,
    val details: List<String> = emptyList(),
)

/**
 * URL и target по-прежнему отделены `--`, поэтому даже враждебный URL не
 * становится опцией clone. Фильтр выбирается только из [PartialCloneFilter].
 */
fun cloneArgs(
    url: String,
    target: String,
    filter: PartialCloneFilter = PartialCloneFilter.Full,
): List<String> = buildList {
    add("clone")
    add("--progress")
    if (filter.value.isNotEmpty()) add("--filter=${filter.value}")
    add("--")
    add(url.trim())
    add(target.trim())
}

fun archiveArgs(ref: String, format: ArchiveFormat, outputPath: String): List<String> =
    listOf(
        "archive",
        "--format=${format.gitFormat}",
        "--output=$outputPath",
        ref.trim(),
    )

fun bundleCreateArgs(outputPath: String): List<String> =
    listOf("bundle", "create", outputPath, "--all")

fun bundleVerifyArgs(path: String): List<String> = listOf("bundle", "verify", path)

fun bundleListHeadsArgs(path: String): List<String> = listOf("bundle", "list-heads", path)

fun notesAddArgs(objectHash: String, message: String, append: Boolean): List<String> =
    if (append) {
        listOf("notes", "append", "-m", message, objectHash)
    } else {
        listOf("notes", "add", "-f", "-m", message, objectHash)
    }

fun notesRemoveArgs(objectHash: String): List<String> = listOf("notes", "remove", objectHash)

/** `git notes list` печатает пары `<note-oid> <annotated-oid>`. */
fun parseNotesList(raw: String): List<GitNoteEntry> = raw.lineSequence()
    .mapNotNull { line ->
        val parts = line.trim().split(Regex("\\s+"), limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val note = parts[0]
        val objectHash = parts[1]
        if (!isObjectId(note) || !isObjectId(objectHash)) return@mapNotNull null
        GitNoteEntry(note, objectHash)
    }
    .distinctBy { it.objectHash }
    .toList()

fun parseSparsePatterns(raw: String): List<String> = raw.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toList()

/**
 * Sparse paths are relative, may not escape the worktree and (in cone mode)
 * may not contain patterns. Otherwise a path is passed as one Git argument.
 */
fun validSparsePath(path: String, cone: Boolean): Boolean {
    val clean = path.trim().replace('\\', '/')
    if (clean.isEmpty() || clean.startsWith("/")) return false
    if (clean.startsWith("-") || clean.any { it.isISOControl() }) return false
    if (Regex("""^[A-Za-z]:""").containsMatchIn(clean)) return false
    if (clean.split('/').any { it == ".." }) return false
    if (cone && clean.any { it in "*?[" || it == '!' }) return false
    return true
}

fun validAdvancedRef(value: String): Boolean {
    if (value.any { it == '\n' || it == '\r' || it == '\u0000' }) return false
    val clean = value.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        clean.none { it.isWhitespace() || it.isISOControl() }
}

fun validLfsPattern(value: String): Boolean {
    val clean = value.trim()
    return clean.isNotEmpty() &&
        clean.length <= 500 &&
        !clean.startsWith("-") &&
        clean.none { it == '\n' || it == '\r' || it == '\u0000' }
}

fun isObjectId(value: String): Boolean =
    value.length in setOf(40, 64) && value.all { it in "0123456789abcdefABCDEF" }
