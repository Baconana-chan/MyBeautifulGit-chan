package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.git.ShellFlavor
import org.baconana.mybeautifulgit_chan.git.quoteArg
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowsShellQuotingTest {

    @Test
    fun `cmd quoting keeps metacharacters inert`() {
        if (!isWindows()) return
        val marker = tempMarker("mbg-cmd")
        try {
            val sentinel = "mbg-expanded-" + System.nanoTime()
            val payload = "safe %MBG_QUOTE_SENTINEL% & echo pwned > \"$marker\""
            val command = "echo " + quoteArg(payload, ShellFlavor.CMD)
            val output = runWithEnv(sentinel, "cmd.exe", "/d", "/s", "/c", command)

            assertFalse(Files.exists(marker), "cmd executed an injected redirection:\n$output")
            assertFalse(output.contains(sentinel), "cmd expanded an environment variable:\n$output")
        } finally {
            Files.deleteIfExists(marker)
        }
    }

    @Test
    fun `cmd round-trips percent bang and embedded quotes`() {
        if (!isWindows()) return
        val java = Paths.get(System.getProperty("java.home"), "bin", "java.exe")
        val testClasses = Paths.get(
            CmdArgumentProbe::class.java.protectionDomain.codeSource.location.toURI(),
        )
        val kotlinStdlib = Paths.get(
            Unit::class.java.protectionDomain.codeSource.location.toURI(),
        )
        val classpath = listOf(testClasses, kotlinStdlib).joinToString(File.pathSeparator)
        val payloads = listOf(
            "percent %MBG_QUOTE_SENTINEL% tail",
            "bang !MBG_QUOTE_SENTINEL! tail",
            "quotes \"one\" and \"two\"",
            "mixed %PATH% ! ! \"quoted\" `tick`",
        )

        payloads.forEachIndexed { index, payload ->
            val output = tempMarker("mbg-round-$index")
            try {
                val command = listOf(
                    quoteArg(java.toString(), ShellFlavor.CMD),
                    "-cp",
                    quoteArg(classpath, ShellFlavor.CMD),
                    CmdArgumentProbe::class.java.name,
                    quoteArg(output.toString(), ShellFlavor.CMD),
                    quoteArg(payload, ShellFlavor.CMD),
                ).joinToString(" ")
                val log = runWithEnv(
                    "mbg-expanded-" + System.nanoTime(),
                    "cmd.exe",
                    "/d",
                    "/v:on",
                    "/s",
                    "/c",
                    "\"$command\"",
                )

                assertTrue(
                    Files.exists(output),
                    "cmd round-trip did not create $output:\n$command\n$log",
                )
                assertEquals(payload, Files.readString(output), "cmd round-trip failed:\n$log")
            } finally {
                Files.deleteIfExists(output)
            }
        }
    }

    @Test
    fun `PowerShell quoting keeps metacharacters inert`() {
        if (!isWindows()) return
        val marker = tempMarker("mbg-pwsh")
        try {
            val payload = "safe; New-Item -ItemType File -Path '$marker' -Force | Out-Null"
            val command = "Write-Output " + quoteArg(payload, ShellFlavor.POWERSHELL)
            val output = run("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command)

            assertFalse(Files.exists(marker), "PowerShell executed an injected command:\n$output")
            assertTrue(output.contains(payload), "PowerShell did not preserve the quoted argument:\n$output")
        } finally {
            Files.deleteIfExists(marker)
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)

    private fun tempMarker(prefix: String): Path {
        val path = Files.createTempFile(prefix, ".marker")
        Files.deleteIfExists(path)
        return path
    }

    private fun run(vararg command: String): String = runWithEnv(null, *command)

    private fun runWithEnv(sentinel: String?, vararg command: String): String {
        val builder = ProcessBuilder(*command).redirectErrorStream(true)
        sentinel?.let { builder.environment()["MBG_QUOTE_SENTINEL"] = it }
        val process = builder.start()
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("shell quoting probe timed out: ${command.joinToString(" ")}")
            }
            return process.inputStream.bufferedReader().readText()
        } finally {
            process.destroyForcibly()
        }
    }
}

/** Child process used to observe exactly what cmd.exe delivers to an argv-based program. */
object CmdArgumentProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        check(args.size == 2) { "usage: CmdArgumentProbe <output> <value>" }
        Files.writeString(Path.of(args[0]), args[1])
    }
}
