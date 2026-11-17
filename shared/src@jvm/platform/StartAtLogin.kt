package org.baconana.mybeautifulgit_chan.platform

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Registers the application for the current user's login startup. No elevation
 * is required: Windows uses HKCU, macOS writes a LaunchAgent, Linux writes an
 * XDG autostart entry.
 */
object StartAtLogin {
    private const val LABEL = "org.baconana.mybeautifulgit-chan"
    private const val WINDOWS_VALUE = "MyBeautifulGit-chan"
    private const val WINDOWS_RUN_KEY =
        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val MAIN_CLASS = "org.baconana.mybeautifulgit_chan.MainKt"

    private val JAVA_COMMANDS = setOf("java", "java.exe", "javaw", "javaw.exe")

    fun isSupported(): Boolean = isWindows() || isMac() || isLinux()

    fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
        when {
            isWindows() -> setWindows(enabled)
            isMac() -> setMac(enabled)
            isLinux() -> setLinux(enabled)
            else -> error("Autostart is not supported on ${osName()}.")
        }
    }

    private fun setWindows(enabled: Boolean) {
        if (enabled) {
            val command = launchCommand().joinToString(" ") { quoteWindowsArgument(it) }
            val result = runProcess(
                listOf(
                    "reg.exe",
                    "add",
                    WINDOWS_RUN_KEY,
                    "/v",
                    WINDOWS_VALUE,
                    "/t",
                    "REG_SZ",
                    "/d",
                    command,
                    "/f",
                ),
            )
            check(result.exitCode == 0) {
                "reg add failed (${result.exitCode})${result.detailSuffix()}"
            }
            return
        }

        // The value may not exist; reg delete exits with 1 in that case.
        val query = runProcess(listOf("reg.exe", "query", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE))
        if (query.exitCode != 0) return
        val delete = runProcess(
            listOf("reg.exe", "delete", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE, "/f"),
        )
        check(delete.exitCode == 0) {
            "reg delete failed (${delete.exitCode})${delete.detailSuffix()}"
        }
    }

    private fun setMac(enabled: Boolean) {
        val file = Paths.get(
            System.getProperty("user.home").orEmpty(),
            "Library",
            "LaunchAgents",
            "$LABEL.plist",
        )
        if (!enabled) {
            Files.deleteIfExists(file)
            return
        }
        writeFile(file, macPlist(launchCommand()))
    }

    private fun setLinux(enabled: Boolean) {
        val configHome = System.getenv("XDG_CONFIG_HOME")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: Paths.get(System.getProperty("user.home").orEmpty(), ".config").toString()
        val file = Paths.get(configHome, "autostart", "$LABEL.desktop")
        if (!enabled) {
            Files.deleteIfExists(file)
            return
        }
        writeFile(file, linuxDesktopEntry(launchCommand()))
    }

    private fun writeFile(file: Path, content: String) {
        Files.createDirectories(file.parent)
        Files.writeString(file, content, StandardCharsets.UTF_8)
    }

    private fun macPlist(command: List<String>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine(
            """<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" """ +
                """"http://www.apple.com/DTDs/PropertyList-1.0.dtd">""",
        )
        appendLine("""<plist version="1.0">""")
        appendLine("<dict>")
        appendLine("  <key>Label</key>")
        appendLine("  <string>${xmlEscape(LABEL)}</string>")
        appendLine("  <key>ProgramArguments</key>")
        appendLine("  <array>")
        command.forEach { appendLine("    <string>${xmlEscape(it)}</string>") }
        appendLine("  </array>")
        appendLine("  <key>RunAtLoad</key>")
        appendLine("  <true/>")
        appendLine("</dict>")
        appendLine("</plist>")
    }

    private fun linuxDesktopEntry(command: List<String>): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=MyBeautifulGit-chan")
        appendLine("Comment=Git client")
        appendLine("Exec=${command.joinToString(" ") { quoteDesktopArgument(it) }}")
        appendLine("Terminal=false")
        appendLine("X-GNOME-Autostart-enabled=true")
    }

    /**
     * Prefer the packaged launcher. When developing, the current JVM command
     * line (or a plain classpath launch) is used instead.
     */
    private fun launchCommand(): List<String> {
        val appPath = System.getProperty("jpackage.app-path")
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotEmpty() }
        if (appPath != null && File(appPath).isFile) return listOf(File(appPath).absolutePath)

        val info = runCatching { ProcessHandle.current().info() }.getOrNull()
        val command = info?.command()?.orElse(null)?.trim().orEmpty()
        val arguments = info?.arguments()?.orElse(null)?.toList().orEmpty()
        if (command.isNotEmpty() && File(command).name.lowercase() !in JAVA_COMMANDS) {
            // The jpackage launcher is a native executable; keep its arguments.
            return listOf(File(command).absolutePath) + arguments
        }

        // Development runs are started by a JVM. Absolute classpath entries keep
        // the registration valid when the login shell has another working directory.
        val classPath = System.getProperty("java.class.path").orEmpty()
        check(classPath.isNotBlank()) { "Cannot determine the application launch command." }
        val absoluteClassPath = classPath
            .split(File.pathSeparator)
            .joinToString(File.pathSeparator) { File(it).absolutePath }
        return listOf(javaExecutable(), "-cp", absoluteClassPath, MAIN_CLASS)
    }

    private fun javaExecutable(): String {
        val bin = Paths.get(System.getProperty("java.home").orEmpty(), "bin")
        if (isWindows()) {
            val javaw = bin.resolve("javaw.exe")
            if (Files.isRegularFile(javaw)) return javaw.toString()
            val java = bin.resolve("java.exe")
            if (Files.isRegularFile(java)) return java.toString()
        }
        return bin.resolve("java").toString()
    }

    private fun runProcess(command: List<String>): ProcessResult {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return ProcessResult(process.waitFor(), output)
    }

    private data class ProcessResult(val exitCode: Int, val output: String) {
        fun detailSuffix(): String = if (output.isBlank()) "" else ": $output"
    }

    internal fun quoteWindowsArgument(argument: String): String {
        if (argument.isNotEmpty() && argument.none { it.isWhitespace() || it == '"' }) {
            return argument
        }
        val result = StringBuilder("\"")
        var backslashes = 0
        for (character in argument) {
            when (character) {
                '\\' -> {
                    backslashes++
                    result.append(character)
                }

                '"' -> {
                    repeat(backslashes + 1) { result.append('\\') }
                    result.append('"')
                    backslashes = 0
                }

                else -> {
                    backslashes = 0
                    result.append(character)
                }
            }
        }
        repeat(backslashes) { result.append('\\') }
        result.append('"')
        return result.toString()
    }

    internal fun quoteDesktopArgument(argument: String): String {
        val escaped = buildString {
            for (character in argument) {
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '`' -> append("\\`")
                    '$' -> append("\\$")
                    '%' -> append("%%")
                    else -> append(character)
                }
            }
        }
        val needsQuotes = escaped.any { it.isWhitespace() || it in "\"'\\><~|&;*?#()" }
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

    private fun isMac(): Boolean =
        System.getProperty("os.name").orEmpty().let {
            it.contains("Mac", ignoreCase = true) || it.contains("Darwin", ignoreCase = true)
        }

    private fun isLinux(): Boolean =
        System.getProperty("os.name").orEmpty().contains("Linux", ignoreCase = true)

    private fun osName(): String = System.getProperty("os.name").orEmpty().ifBlank { "this OS" }
}
