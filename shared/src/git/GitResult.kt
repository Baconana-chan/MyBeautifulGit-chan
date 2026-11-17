package org.baconana.mybeautifulgit_chan.git

import org.baconana.mybeautifulgit_chan.domain.redactUrlCredentials

data class GitVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val raw: String,
) : Comparable<GitVersion> {
    val isSupported: Boolean get() = this >= MINIMUM_SUPPORTED

    /** Числовая версия без vendor-суффикса: ключ для «не напоминать для этой версии». */
    val key: String get() = "$major.$minor.$patch"

    override fun compareTo(other: GitVersion): Int {
        major.compareTo(other.major).takeIf { it != 0 }?.let { return it }
        minor.compareTo(other.minor).takeIf { it != 0 }?.let { return it }
        return patch.compareTo(other.patch)
    }

    companion object {
        val MINIMUM_SUPPORTED = GitVersion(2, 30, 0, "2.30.0")

        private val VERSION = Regex("""\bversion\s+(\d+)\.(\d+)(?:\.(\d+))?""")

        fun parse(output: String): GitVersion? {
            val match = VERSION.find(output) ?: return null
            val (major, minor, patch) = match.destructured
            return GitVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: 0,
                raw = output.trim(),
            )
        }
    }
}

data class GitResult(
    val executable: String = "git",
    val args: List<String>,
    val cwd: String?,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val timedOut: Boolean = false,
    val cancelled: Boolean = false,
    val stdoutTruncated: Boolean = false,
    val stderrTruncated: Boolean = false,
) {
    val ok: Boolean get() = !timedOut && exitCode == 0

    val outputTruncated: Boolean get() = stdoutTruncated || stderrTruncated

    /** Короткий alias для UI-кода, которому не важен конкретный поток. */
    val truncated: Boolean get() = outputTruncated

    /** Команда для показа и копирования в терминал. */
    val commandLine: String
        get() = displayCommandLine(executable, args)
}

/** Содержимое blob'а с ограничением; байты не читаются, если размер больше лимита. */
data class GitBlob(
    val size: Long?,
    val bytes: ByteArray? = null,
    val tooLarge: Boolean = false,
    val missing: Boolean = false,
    val error: String? = null,
)

class GitCommandException(val result: GitResult) : RuntimeException(
    buildString {
        append(result.commandLine)
        append(" -> exit ")
        append(result.exitCode)
        if (result.timedOut) append(" (timed out)")
        if (result.outputTruncated) append(" (output truncated)")
        val details = redactSecrets(result.stderr.trim().ifEmpty { result.stdout.trim() }).take(4000)
        if (details.isNotEmpty()) {
            append('\n')
            append(details)
        }
    },
)

/** Shell syntax used when showing a command that can be copied. */
enum class ShellFlavor {
    POSIX,
    CMD,
    POWERSHELL,
}

expect fun currentShellFlavor(): ShellFlavor

/**
 * Process-wide shell used by the UI when it builds copyable commands. The
 * settings screen updates this preference; explicit [ShellFlavor] arguments
 * remain available for tests and one-off renderers.
 */
object CommandShellPreference {
    var flavor: ShellFlavor = currentShellFlavor()
}

/**
 * Builds a command line for display and copying, not for ProcessBuilder.
 *
 * URL userinfo is replaced before quoting so a remote token cannot be copied
 * from a toast, log line, error banner or command preview. The default shell
 * flavor follows the current platform; callers can select CMD or POWERSHELL
 * explicitly when the copy target is known.
 */
fun displayCommandLine(
    executable: String,
    args: List<String>,
    shell: ShellFlavor = CommandShellPreference.flavor,
): String =
    (listOf(executable) + args.map(::redactUrlCredentials))
        .joinToString(" ") { quoteArg(it, shell) }

/** Replaces URL userinfo and explicit secret values in display-only text. */
fun redactSecrets(value: String, secrets: Iterable<String> = emptyList()): String {
    var redacted = redactUrlCredentials(value)
    secrets.asSequence()
        .filter { it.isNotBlank() }
        .distinct()
        .forEach { secret -> redacted = redacted.replace(secret, "***") }
    return redacted
}

private val SAFE_SHELL_ARG = Regex("""[A-Za-z0-9_@%+=:,./-]+""")
private val SAFE_POWERSHELL_ARG = Regex("""[A-Za-z0-9_+=./-]+""")
private val SAFE_CMD_ARG = Regex("""[A-Za-z0-9_@+=:,./-]+""")

/** Quotes one argument for the selected shell. */
fun quoteArg(arg: String, shell: ShellFlavor = CommandShellPreference.flavor): String =
    when (shell) {
        ShellFlavor.POSIX -> quotePosixArg(arg)
        ShellFlavor.CMD -> quoteCmdArg(arg)
        ShellFlavor.POWERSHELL -> quotePowerShellArg(arg)
    }

/**
 * A POSIX single-quoted string keeps `;`, `&`, `|`, backticks, `$()` and
 * redirections literal. An embedded single quote is closed, quoted literally
 * and reopened.
 */
private fun quotePosixArg(arg: String): String {
    if (arg.isEmpty()) return "''"
    if (arg.matches(SAFE_SHELL_ARG)) return arg
    return "'" + arg.replace("'", "'\"'\"'") + "'"
}

/**
 * cmd.exe groups unsafe arguments in double quotes. Metacharacters such as
 * `;`, `&`, `|`, `<`, `>` and backticks are literal inside that group.
 * Quotes, percent signs and exclamation marks are close-quoted and caret-
 * escaped, then the group is reopened. This keeps `%NAME%` from expanding and
 * avoids an embedded quote ending the argument. Copying into PowerShell should
 * use [ShellFlavor.POWERSHELL] instead.
 */
private fun quoteCmdArg(arg: String): String {
    if (arg.isEmpty()) return "\"\""
    if (arg.matches(SAFE_CMD_ARG)) return arg
    val quoted = StringBuilder("\"")
    for (char in arg) {
        when (char) {
            '"', '%', '!' -> {
                quoted.append('"')
                quoted.append('^').append(char)
                quoted.append('"')
            }
            else -> quoted.append(char)
        }
    }
    quoted.append('"')
    return quoted.toString()
}

/**
 * PowerShell single-quoted strings are literal; an embedded quote is doubled.
 * This prevents `;`, `&`, `|`, backticks and `$()` from being interpreted when
 * the command is pasted into PowerShell.
 */
private fun quotePowerShellArg(arg: String): String {
    if (arg.isEmpty()) return "''"
    if (arg.matches(SAFE_POWERSHELL_ARG)) return arg
    return "'" + arg.replace("'", "''") + "'"
}
