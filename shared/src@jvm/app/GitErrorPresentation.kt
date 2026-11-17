package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.domain.detectAuthFailure
import org.baconana.mybeautifulgit_chan.i18n.Strings

/**
 * A Git failure as it should be shown to a human. [stderr] is the original
 * command output (already redacted by the controller), not a reconstruction.
 */
data class PresentedGitError(
    val message: String,
    val command: String,
    val exitCode: Int,
    val stderr: String?,
    val truncated: Boolean = false,
)

private val FAILURE_HEADER =
    Regex("""^(.+?)\s*(?:→|->)\s*exit\s+(-?\d+)(.*)$""")

/**
 * Recognizes the two failure formats used by the controller:
 *
 *   git push ... → exit 1
 *   git push ... -> exit 1 (timed out)
 *
 * Anything else (validation errors, installer output, etc.) is left alone.
 */
fun presentGitError(text: String, strings: Strings): PresentedGitError? {
    val trimmed = text.trimEnd()
    if (trimmed.isEmpty()) return null
    val newline = trimmed.indexOf('\n')
    val header = (if (newline >= 0) trimmed.substring(0, newline) else trimmed).trimEnd('\r')
    val match = FAILURE_HEADER.matchEntire(header) ?: return null
    val command = match.groupValues[1].trim()
    if (command.isEmpty()) return null
    val exitCode = match.groupValues[2].toIntOrNull() ?: return null
    val flags = match.groupValues[3].lowercase()
    val stderr = if (newline >= 0) {
        trimmed.substring(newline + 1).trimStart('\n', '\r').ifBlank { null }
    } else {
        null
    }
    val timedOut = "timed out" in flags
    val truncated = "output truncated" in flags ||
        stderr?.contains("output truncated", ignoreCase = true) == true
    return PresentedGitError(
        message = if (timedOut) {
            strings.gitErrorTimedOut
        } else {
            humanGitErrorMessage(stderr.orEmpty(), strings)
        },
        command = command,
        exitCode = exitCode,
        stderr = stderr,
        truncated = truncated,
    )
}

internal fun humanGitErrorMessage(output: String, strings: Strings): String {
    if (output.isBlank()) return strings.gitErrorFailed
    detectAuthFailure(output)?.let { return strings.authFailureText(it) }

    val text = output.lowercase()
    fun has(vararg needles: String): Boolean = needles.any { it in text }
    return when {
        has("index.lock") && has("file exists", "another git process", "unable to create") ->
            strings.gitErrorIndexLock

        has(
            "automatic merge failed",
            "fix conflicts",
            "unmerged paths",
            "needs merge",
            "conflict (content)",
            "you have not concluded your merge",
        ) -> strings.gitErrorConflicts

        has(
            "would be overwritten by",
            "please commit your changes or stash",
            "your local changes",
            "local changes would be lost",
            "cannot cherry-pick with uncommitted changes",
            "cannot rebase with uncommitted changes",
        ) -> strings.gitErrorDirtyWorktree

        has(
            "failed to push",
            "updates were rejected",
            "fetch first",
            "remote contains work",
            "remote rejected",
            "hook declined",
            "protected branch",
        ) -> strings.gitErrorPushRejected

        has("has no upstream branch", "no upstream configured", "no tracking information") ->
            strings.gitErrorNoUpstream

        has("refusing to merge unrelated histories") -> strings.gitErrorUnrelatedHistories

        has("permission denied") -> strings.gitErrorPermission

        has("not a git repository") -> strings.gitErrorNotRepository

        has(
            "repository not found",
            "could not read from remote repository",
            "does not appear to be a git repository",
            "couldn't find remote ref",
        ) || (has("repository") && has("not found")) -> strings.gitErrorRemoteNotFound

        has(
            "unknown revision",
            "bad revision",
            "not a valid object name",
            "invalid reference",
        ) -> strings.gitErrorRefNotFound

        has("pathspec", "did not match any file") -> strings.gitErrorPathNotFound

        has("already exists") -> strings.gitErrorAlreadyExists

        has(
            "not possible to fast-forward",
            "not a fast-forward",
            "non-fast-forward",
            "diverging branches",
        ) -> strings.gitErrorNotFastForward

        else -> strings.gitErrorFailed
    }
}
