package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.git.cli.BoundedOutputReader
import org.baconana.mybeautifulgit_chan.git.cli.GitInstallOption
import org.baconana.mybeautifulgit_chan.git.cli.GitOs
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class InstallOutcome(
    val ok: Boolean,
    val output: String,
)

/**
 * Запускает установщик, который уже выбран пользователем. Команды не собираются
 * из пользовательского ввода: список аргументов приходит из GitLocator.
 *
 * Повышение прав выполняется системным механизмом ОС, а не через строку `sudo`,
 * чтобы не показывать и не выполнять пароль внутри приложения.
 */
object GitInstaller {

    private const val TIMEOUT_SECONDS = 900L
    private const val OUTPUT_LIMIT = 8_000

    fun install(
        option: GitInstallOption,
        os: GitOs = GitOs.current(),
    ): InstallOutcome {
        val command = option.command
            ?: return InstallOutcome(false, "This install option has no command.")
        if (command.isEmpty()) {
            return InstallOutcome(false, "The install command is empty.")
        }

        return if (option.requiresElevation) {
            runElevated(command, os)
        } else {
            run(command)
        }
    }

    private fun runElevated(command: List<String>, os: GitOs): InstallOutcome =
        when (os) {
            GitOs.WINDOWS -> run(windowsElevationCommand(command))
            GitOs.MACOS -> run(macosElevationCommand(command))
            GitOs.LINUX, GitOs.OTHER -> {
                val pkexec = PKEXEC_PATHS.firstOrNull { File(it).isFile }
                if (pkexec == null) {
                    InstallOutcome(
                        false,
                        "pkexec was not found. Run the command manually in a terminal " +
                            "with administrator rights.",
                    )
                } else {
                    run(listOf(pkexec) + command)
                }
            }
        }

    internal fun windowsElevationCommand(command: List<String>): List<String> {
        val executable = powerShellQuote(command.first())
        val arguments = command.drop(1).joinToString(", ") { powerShellQuote(it) }
        val script = "\$process = Start-Process -FilePath $executable " +
            "-ArgumentList @($arguments) -Verb RunAs -Wait -PassThru; " +
            "if (\$null -eq \$process) { exit 1 }; exit \$process.ExitCode"
        return listOf(
            "powershell",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            script,
        )
    }

    private fun macosElevationCommand(command: List<String>): List<String> {
        val shell = command.joinToString(" ") { shellQuote(it) }
        val script = "do shell script ${appleScriptQuote(shell)} with administrator privileges"
        return listOf("osascript", "-e", script)
    }

    private fun run(command: List<String>): InstallOutcome {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (error: IOException) {
            return InstallOutcome(false, error.message ?: error.toString())
        }

        // Keep the retained output bounded while still draining the pipe: an
        // installer that prints a lot must not block or grow the heap.
        val output = BoundedOutputReader(
            input = process.inputStream,
            limit = OUTPUT_LIMIT.toLong(),
        )
        val reader = thread(name = "git-installer", isDaemon = true) {
            output.run()
        }

        val finished = runCatching {
            process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.getOrDefault(false)

        if (!finished) {
            destroyTree(process)
            reader.join(1_000)
            val partial = output.bytes().toString(Charsets.UTF_8).trim()
            return InstallOutcome(
                false,
                buildString {
                    append("Installation timed out after $TIMEOUT_SECONDS seconds.")
                    if (partial.isNotEmpty()) append("\n").append(partial)
                },
            )
        }

        reader.join(2_000)
        val text = output.bytes().toString(Charsets.UTF_8).trim()
            .ifBlank { "exit code ${process.exitValue()}" }
        return InstallOutcome(process.exitValue() == 0, text.take(OUTPUT_LIMIT))
    }

    private fun destroyTree(process: Process) {
        runCatching {
            process.toHandle().descendants().forEach { it.destroy() }
        }
        process.destroy()
        val stopped = runCatching { process.waitFor(1, TimeUnit.SECONDS) }.getOrDefault(false)
        if (!stopped) {
            runCatching {
                process.toHandle().descendants().forEach { it.destroyForcibly() }
            }
            process.destroyForcibly()
        }
    }

    private fun powerShellQuote(value: String): String =
        "'" + value.replace("'", "''") + "'"

    private fun appleScriptQuote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private val PKEXEC_PATHS = listOf(
        "/usr/bin/pkexec",
        "/bin/pkexec",
        "/usr/local/bin/pkexec",
    )
}
