package org.baconana.mybeautifulgit_chan.app

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal data class BisectCommandResult(
    val exitCode: Int? = null,
    val output: String = "",
    val timedOut: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null,
)

/**
 * Запускает проверочную команду bisect через системный shell. Команда
 * пользовательская, поэтому shell нужен для пайпов и условных операторов;
 * cwd — рабочий каталог репозитория.
 */
internal class BisectCommandRunner(
    private val workingDirectory: File,
    private val timeoutSeconds: Long = 600L,
    private val outputLimit: Int = 256 * 1024,
) {
    @Volatile
    private var process: Process? = null

    @Volatile
    private var cancelRequested = false

    fun cancel() {
        cancelRequested = true
        process?.let(::destroyTree)
    }

    fun run(command: String): BisectCommandResult {
        val builder = ProcessBuilder(shellCommand(command))
        builder.directory(workingDirectory)
        builder.redirectErrorStream(true)
        builder.environment().apply {
            put("GIT_TERMINAL_PROMPT", "0")
            put("GIT_PAGER", "cat")
            // Не позволяем чужому git-окружению увести команду в другой репозиторий.
            listOf(
                "GIT_DIR",
                "GIT_WORK_TREE",
                "GIT_INDEX_FILE",
                "GIT_OBJECT_DIRECTORY",
                "GIT_ALTERNATE_OBJECT_DIRECTORIES",
                "GIT_COMMON_DIR",
                "GIT_NAMESPACE",
            ).forEach { remove(it) }
        }

        val started = try {
            builder.start()
        } catch (error: Exception) {
            return BisectCommandResult(error = error.message ?: error.toString())
        }
        process = started
        // Verification commands are non-interactive; EOF prevents tools that
        // unexpectedly read stdin from hanging until the timeout.
        runCatching { started.outputStream.close() }

        val output = ByteArrayOutputStream()
        val reader = thread(name = "bisect-command-output", isDaemon = true) {
            try {
                started.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        synchronized(output) {
                            val remaining = outputLimit - output.size()
                            if (remaining > 0) {
                                output.write(buffer, 0, minOf(read, remaining))
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Процесс мог быть убит по timeout/cancel.
            }
        }

        var timedOut = false
        var cancelled = false
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        try {
            while (true) {
                if (cancelRequested) {
                    cancelled = true
                    destroyTree(started)
                    break
                }
                val remainingNanos = deadline - System.nanoTime()
                if (remainingNanos <= 0) {
                    timedOut = true
                    destroyTree(started)
                    break
                }
                val waitMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceIn(25L, 100L)
                try {
                    if (started.waitFor(waitMillis, TimeUnit.MILLISECONDS)) break
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    cancelled = true
                    destroyTree(started)
                    break
                }
            }
        } finally {
            process = null
        }
        reader.join(2_000)
        val text = synchronized(output) {
            output.toByteArray().toString(Charsets.UTF_8)
        }
        val exitCode = runCatching { started.exitValue() }.getOrNull()
        return BisectCommandResult(
            exitCode = exitCode,
            output = text,
            timedOut = timedOut,
            cancelled = cancelled,
        )
    }

    private fun shellCommand(command: String): List<String> {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return if (os.contains("win")) {
            val shell = System.getenv("ComSpec")
                ?.takeIf { it.isNotBlank() }
                ?: "cmd.exe"
            listOf(shell, "/d", "/s", "/c", command)
        } else {
            listOf("/bin/sh", "-c", command)
        }
    }

    private fun destroyTree(target: Process) {
        runCatching {
            target.toHandle().descendants().forEach { it.destroy() }
        }
        target.destroy()
        val stopped = runCatching { target.waitFor(1, TimeUnit.SECONDS) }.getOrDefault(false)
        if (!stopped) {
            runCatching {
                target.toHandle().descendants().forEach { it.destroyForcibly() }
            }
            target.destroyForcibly()
        }
    }
}
