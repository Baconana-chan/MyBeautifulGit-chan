package org.baconana.mybeautifulgit_chan.platform

import org.baconana.mybeautifulgit_chan.ai.AiCommitStyle
import org.baconana.mybeautifulgit_chan.ai.AiMessageLanguage
import org.baconana.mybeautifulgit_chan.ai.AiSettings
import org.baconana.mybeautifulgit_chan.git.ShellFlavor
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.domain.SshAuthMode
import org.baconana.mybeautifulgit_chan.domain.UpdateChannel
import org.baconana.mybeautifulgit_chan.ui.mockup.FontChoice
import org.baconana.mybeautifulgit_chan.ui.mockup.ThemeMode
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AppSettingsTest {

    @Test
    fun `missing file defaults to custom picker and requested language`() {
        val dir = Files.createTempDirectory("git-chan-settings")

        assertEquals(
            DirectoryPickerMode.Custom,
            AppSettings(dir.resolve("settings.properties")).loadPickerMode(),
        )
        assertEquals(
            Language.RU,
            AppSettings(dir.resolve("settings.properties")).loadLanguage(Language.RU),
        )
        assertEquals(
            ThemeMode.Dark,
            AppSettings(dir.resolve("settings.properties")).loadTheme(),
        )
        assertEquals(
            FontChoice.Mono,
            AppSettings(dir.resolve("settings.properties")).loadFont(),
        )
        assertEquals(
            false,
            AppSettings(dir.resolve("settings.properties")).loadHighContrast(),
        )
        assertEquals(
            "",
            AppSettings(dir.resolve("settings.properties")).loadGitPath(),
        )
        assertEquals(
            "main",
            AppSettings(dir.resolve("settings.properties")).loadDefaultBranch(),
        )
        assertNull(AppSettings(dir.resolve("settings.properties")).loadCopyShell())
        assertEquals(
            false,
            AppSettings(dir.resolve("settings.properties")).loadCloseToTray(),
        )
        assertEquals(
            false,
            AppSettings(dir.resolve("settings.properties")).loadStartAtLogin(),
        )
        assertEquals(
            UpdateChannel.Chan,
            AppSettings(dir.resolve("settings.properties")).loadUpdateChannel(),
        )
    }

    @Test
    fun `appearance, git path and default branch survive a reload`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveTheme(ThemeMode.Light)
        settings.saveFont(FontChoice.Sans)
        settings.saveHighContrast(true)
        settings.saveGitPath("  C:\\Program Files\\Git\\cmd\\git.exe  ")
        settings.saveDefaultBranch("  trunk  ")

        val reloaded = AppSettings(file)
        assertEquals(ThemeMode.Light, reloaded.loadTheme())
        assertEquals(FontChoice.Sans, reloaded.loadFont())
        assertEquals(true, reloaded.loadHighContrast())
        assertEquals("C:\\Program Files\\Git\\cmd\\git.exe", reloaded.loadGitPath())
        assertEquals("trunk", reloaded.loadDefaultBranch())

        settings.saveTheme(ThemeMode.System)
        settings.saveFont(FontChoice.Serif)
        settings.saveHighContrast(false)
        settings.saveDefaultBranch("  ")

        assertEquals(ThemeMode.System, reloaded.loadTheme())
        assertEquals(FontChoice.Serif, reloaded.loadFont())
        assertEquals(false, reloaded.loadHighContrast())
        assertEquals("main", reloaded.loadDefaultBranch())
    }

    @Test
    fun `system picker and language survive a reload without clobbering each other`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.savePickerMode(DirectoryPickerMode.System)
        settings.saveLanguage(Language.RU)

        val reloaded = AppSettings(file)
        assertEquals(DirectoryPickerMode.System, reloaded.loadPickerMode())
        assertEquals(Language.RU, reloaded.loadLanguage(Language.EN))

        settings.savePickerMode(DirectoryPickerMode.Custom)
        settings.saveLanguage(Language.EN)

        assertEquals(DirectoryPickerMode.Custom, reloaded.loadPickerMode())
        assertEquals(Language.EN, reloaded.loadLanguage(Language.RU))
    }

    @Test
    fun `desktop start preferences survive a reload`() {
        val dir = Files.createTempDirectory("git-chan-settings-desktop")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveCloseToTray(true)
        settings.saveStartAtLogin(true)

        val reloaded = AppSettings(file)
        assertEquals(true, reloaded.loadCloseToTray())
        assertEquals(true, reloaded.loadStartAtLogin())

        settings.saveCloseToTray(false)
        settings.saveStartAtLogin(false)
        assertEquals(false, reloaded.loadCloseToTray())
        assertEquals(false, reloaded.loadStartAtLogin())
    }

    @Test
    fun `update channel survives a reload and accepts legacy aliases`() {
        val dir = Files.createTempDirectory("git-chan-settings-updates")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveUpdateChannel(UpdateChannel.Kun)
        assertEquals(UpdateChannel.Kun, AppSettings(file).loadUpdateChannel())

        settings.saveUpdateChannel(UpdateChannel.Chan)
        assertEquals(UpdateChannel.Chan, AppSettings(file).loadUpdateChannel())

        Files.writeString(file, "updates.channel=beta\n")
        assertEquals(UpdateChannel.Kun, AppSettings(file).loadUpdateChannel())
    }

    @Test
    fun `dismissed git version survives a reload and can be cleared`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveDismissedGitVersion("2.43.0")
        assertEquals("2.43.0", AppSettings(file).loadDismissedGitVersion())

        settings.saveDismissedGitVersion(null)
        assertNull(AppSettings(file).loadDismissedGitVersion())
    }

    @Test
    fun `authentication preferences survive a reload`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveAuthUsername("  octocat  ")
        settings.saveCredentialHelper("  manager-core  ")
        settings.saveSshMode(SshAuthMode.Key)
        settings.saveSshKeyPath("  C:\\Users\\me\\.ssh\\id_ed25519  ")

        val reloaded = AppSettings(file)
        assertEquals("octocat", reloaded.loadAuthUsername())
        assertEquals("manager-core", reloaded.loadCredentialHelper())
        assertEquals(SshAuthMode.Key, reloaded.loadSshMode())
        assertEquals("C:\\Users\\me\\.ssh\\id_ed25519", reloaded.loadSshKeyPath())

        settings.saveAuthUsername("")
        settings.saveCredentialHelper("")
        settings.saveSshMode(SshAuthMode.Agent)
        assertEquals("", reloaded.loadAuthUsername())
        assertEquals("", reloaded.loadCredentialHelper())
        assertEquals(SshAuthMode.Agent, reloaded.loadSshMode())
    }

    @Test
    fun `AI commit settings survive a reload without storing an API key`() {
        val dir = Files.createTempDirectory("git-chan-settings-ai")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveAiSettings(
            AiSettings(
                enabled = true,
                endpoint = "https://example.test/v1/chat/completions",
                model = "test-model",
                messageLanguage = AiMessageLanguage.Russian,
                style = AiCommitStyle.Plain,
                maxDiffChars = 24_000,
                temperature = 0.7,
                allowLocalHttp = true,
            ),
        )

        val reloaded = AppSettings(file).loadAiSettings()
        assertEquals(true, reloaded.enabled)
        assertEquals("https://example.test/v1/chat/completions", reloaded.endpoint)
        assertEquals("test-model", reloaded.model)
        assertEquals(AiMessageLanguage.Russian, reloaded.messageLanguage)
        assertEquals(AiCommitStyle.Plain, reloaded.style)
        assertEquals(24_000, reloaded.maxDiffChars)
        assertEquals(0.7, reloaded.temperature)
        assertEquals(true, reloaded.allowLocalHttp)
        assertFalse(Files.readString(file).contains("apiKey", ignoreCase = true))
    }

    @Test
    fun `copy shell survives a reload and can return to platform default`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        val settings = AppSettings(file)

        settings.saveCopyShell(ShellFlavor.CMD)
        assertEquals(ShellFlavor.CMD, AppSettings(file).loadCopyShell())

        settings.saveCopyShell(ShellFlavor.POWERSHELL)
        assertEquals(ShellFlavor.POWERSHELL, AppSettings(file).loadCopyShell())

        settings.saveCopyShell(null)
        assertNull(AppSettings(file).loadCopyShell())
    }

    @Test
    fun `existing settings and config directory are hardened when loaded on POSIX`() {
        val dir = Files.createTempDirectory("git-chan-settings-existing-permissions")
        val file = dir.resolve("settings.properties")
        Files.writeString(file, "ui.theme=light\n")

        // Windows does not expose POSIX permissions; the reader is best-effort there.
        if (Files.getFileAttributeView(file, PosixFileAttributeView::class.java) == null) return
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw-rw-"))
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"))

        assertEquals(ThemeMode.Light, AppSettings(file).loadTheme())
        assertEquals(
            "rw-------",
            PosixFilePermissions.toString(Files.getPosixFilePermissions(file)),
        )
        assertEquals(
            "rwx------",
            PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)),
        )
    }

    @Test
    fun `fresh settings and config directory are owner-only on POSIX`() {
        val dir = Files.createTempDirectory("git-chan-settings-permissions")
        val file = dir.resolve("settings.properties")

        AppSettings(file).saveTheme(ThemeMode.Light)

        // Windows does not expose POSIX permissions; the writer is best-effort there.
        if (Files.getFileAttributeView(file, PosixFileAttributeView::class.java) == null) return
        assertEquals(
            "rw-------",
            PosixFilePermissions.toString(Files.getPosixFilePermissions(file)),
        )
        assertEquals(
            "rwx------",
            PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)),
        )
    }

    @Test
    fun `unknown values fall back to defaults`() {
        val dir = Files.createTempDirectory("git-chan-settings")
        val file = dir.resolve("settings.properties")
        Files.writeString(
            file,
            "picker.directory=whatever\nui.language=de\n" +
                "ui.theme=neon\nui.font=comic\ngit.defaultBranch=\ngit.dismissedVersion=\n" +
                "commands.copyShell=not-a-shell\n",
        )

        val settings = AppSettings(file)
        assertEquals(DirectoryPickerMode.Custom, settings.loadPickerMode())
        assertEquals(Language.EN, settings.loadLanguage(Language.EN))
        assertEquals(ThemeMode.Dark, settings.loadTheme())
        assertEquals(FontChoice.Mono, settings.loadFont())
        assertEquals("main", settings.loadDefaultBranch())
        assertNull(settings.loadCopyShell())
    }
}
