package org.baconana.mybeautifulgit_chan.domain

/** One formatted patch from a mailbox or `git format-patch --stdout`. */
data class PatchMessage(
    val index: Int,
    val from: String,
    val subject: String,
    val date: String,
)

data class PatchMailbox(
    val path: String,
    val raw: String,
    val messages: List<PatchMessage>,
    val truncated: Boolean = false,
)

data class PatchExportOptions(
    val hashes: List<String> = emptyList(),
    val range: String = "",
    val outputDir: String = "",
    val outputFile: String = "",
    val previewOnly: Boolean = false,
    val coverLetter: Boolean = false,
    val numbered: Boolean = false,
    val signoff: Boolean = false,
    val binary: Boolean = false,
    val subjectPrefix: String = "",
)

data class PatchExportResult(
    val mailbox: String = "",
    val outputFile: String = "",
    val outputDir: String = "",
    val files: List<String> = emptyList(),
)

fun formatPatchArgs(
    revisions: List<String>,
    outputDir: String? = null,
    stdout: Boolean = false,
    single: Boolean = false,
    coverLetter: Boolean = false,
    numbered: Boolean = false,
    signoff: Boolean = false,
    binary: Boolean = false,
    subjectPrefix: String = "",
    startNumber: Int? = null,
): List<String> = buildList {
    add("format-patch")
    if (single) add("-1")
    if (stdout) {
        add("--stdout")
    } else if (outputDir != null) {
        add("-o")
        add(outputDir)
    }
    if (coverLetter) add("--cover-letter")
    if (numbered) add("--numbered")
    if (signoff) add("--signoff")
    if (binary) add("--binary")
    if (startNumber != null) {
        add("--start-number")
        add(startNumber.toString())
    }
    if (subjectPrefix.isNotBlank()) {
        add("--subject-prefix")
        add(subjectPrefix.trim())
    }
    addAll(revisions)
}

fun applyPatchArgs(path: String, check: Boolean, threeWay: Boolean): List<String> = buildList {
    add("apply")
    if (check) add("--check")
    if (threeWay) add("--3way")
    add(path)
}

fun amArgs(path: String, threeWay: Boolean, signoff: Boolean): List<String> = buildList {
    add("am")
    if (threeWay) add("--3way")
    if (signoff) add("--signoff")
    add(path)
}

fun validPatchPath(path: String): Boolean {
    val clean = path.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        clean.none { it == '\n' || it == '\r' || it == '\u0000' }
}

/**
 * Lightweight mbox reader for the mailbox pane. It does not validate patch
 * contents; it only extracts the headers needed by the UI.
 */
object PatchMailboxParser {

    fun parse(raw: String, path: String, truncated: Boolean = false): PatchMailbox {
        val lines = raw.lineSequence().toList()
        val blocks = ArrayList<List<String>>()
        var current = ArrayList<String>()

        fun flush() {
            if (current.any { it.isNotBlank() }) blocks += current
            current = ArrayList()
        }

        lines.forEach { line ->
            if (line.startsWith("From ") && !line.startsWith("From: ")) {
                if (current.isNotEmpty()) flush()
            } else {
                current += line
            }
        }
        if (current.isNotEmpty()) flush()
        if (blocks.isEmpty() && raw.isNotBlank()) blocks += lines

        val messages = blocks.mapIndexedNotNull { index, block ->
            val from = header(block, "From")
            val subject = header(block, "Subject")
            val date = header(block, "Date")
            if (from.isBlank() && subject.isBlank()) {
                null
            } else {
                PatchMessage(
                    index = index + 1,
                    from = from,
                    subject = subject.ifBlank { "(no subject)" },
                    date = date,
                )
            }
        }
        return PatchMailbox(
            path = path,
            raw = raw,
            messages = messages,
            truncated = truncated,
        )
    }

    private fun header(lines: List<String>, name: String): String {
        val prefix = "$name:"
        val start = lines.indexOfFirst { it.startsWith(prefix) }
        if (start < 0) return ""
        val value = StringBuilder(lines[start].removePrefix(prefix).trim())
        var index = start + 1
        while (index < lines.size) {
            val line = lines[index]
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (value.isNotEmpty()) value.append(' ')
                value.append(line.trim())
                index++
            } else {
                break
            }
        }
        return value.toString()
    }
}
