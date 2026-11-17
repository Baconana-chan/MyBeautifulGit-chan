package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.git.cli.GitInstallKind
import org.baconana.mybeautifulgit_chan.git.cli.GitInstallOption
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitInstallerTest {

    @Test
    fun `option without a command is rejected without starting a process`() {
        val outcome = GitInstaller.install(
            GitInstallOption(
                kind = org.baconana.mybeautifulgit_chan.git.cli.GitInstallKind.DOWNLOAD_PAGE,
                title = "download",
                detail = "manual install",
            ),
        )

        assertFalse(outcome.ok)
        assertTrue(outcome.output.contains("no command", ignoreCase = true))
    }

    @Test
    fun `installer keeps huge output bounded and finishes`() {
        val dir = Files.createTempDirectory("git-chan-installer-huge")
        try {
            val fake = writeFakeProcessScript(dir, lines = 500)
            val started = System.nanoTime()
            val outcome = GitInstaller.install(
                GitInstallOption(
                    kind = GitInstallKind.PACKAGE_MANAGER,
                    title = "fake",
                    detail = "fake installer",
                    command = listOf(fake.toString()),
                    requiresElevation = false,
                ),
            )
            val elapsedMs = (System.nanoTime() - started) / 1_000_000

            assertTrue(outcome.ok, outcome.output)
            assertTrue(outcome.output.contains("git version 2.99.0"))
            assertTrue(outcome.output.length <= 8_000, "retained ${outcome.output.length} chars")
            assertTrue(elapsedMs < 8_000, "installer took $elapsedMs ms")
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `windows elevation passes the exact command through UAC`() {
        val command = GitInstaller.windowsElevationCommand(
            listOf(
                "winget",
                "install",
                "--id",
                "Git.Git",
                "-e",
                "--accept-package-agreements",
                "--accept-source-agreements",
            ),
        )

        assertTrue(command.contains("powershell"))
        assertTrue(command.contains("-Command"))
        val script = command.last()
        assertTrue(script.contains("-Verb RunAs"))
        assertTrue(script.contains("'winget'"))
        assertTrue(script.contains("'Git.Git'"))
        assertTrue(script.contains("--accept-package-agreements"))
    }

    @Test
    fun `powershell quoting does not let an argument escape its literal`() {
        val command = GitInstaller.windowsElevationCommand(
            listOf("git-installer.exe", "value'); Remove-Item -Recurse C:\\temp; '"),
        )

        val script = command.last()
        assertTrue(script.contains("''); Remove-Item"))
        assertFalse(script.contains("'; Remove-Item"))
    }
}
