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
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

enum class DirectoryPickerMode(val label: String) {
    Custom("custom"),
    System("system");

    companion object {
        fun parse(value: String?): DirectoryPickerMode =
            if (value?.trim()?.equals(System.label, ignoreCase = true) == true) System else Custom
    }
}

/**
 * Пользовательские настройки в settings.properties. Каждый save перечитывает
 * файл и меняет только свой ключ, чтобы язык и режим пикера не затирали друг друга.
 */
class AppSettings(
    private val file: Path = AppPaths.settingsFile(),
) {
    fun loadPickerMode(): DirectoryPickerMode {
        val value = read().getProperty(PICKER_KEY)
        return DirectoryPickerMode.parse(value)
    }

    fun loadLanguage(default: Language = Language.EN): Language {
        val value = read().getProperty(LANGUAGE_KEY)?.trim().orEmpty()
        if (value.isEmpty()) return default
        return Language.entries.firstOrNull { it.code.equals(value, ignoreCase = true) } ?: default
    }

    fun savePickerMode(mode: DirectoryPickerMode) {
        update(PICKER_KEY, mode.label)
    }

    fun saveLanguage(language: Language) {
        update(LANGUAGE_KEY, language.code)
    }

    fun loadTheme(): ThemeMode =
        ThemeMode.parse(read().getProperty(THEME_KEY))

    fun saveTheme(theme: ThemeMode) {
        update(THEME_KEY, theme.label)
    }

    fun loadFont(): FontChoice =
        FontChoice.parse(read().getProperty(FONT_KEY))

    fun saveFont(font: FontChoice) {
        update(FONT_KEY, font.label)
    }

    fun loadHighContrast(): Boolean =
        read().getProperty(HIGH_CONTRAST_KEY)?.trim()?.equals("true", ignoreCase = true) == true

    fun saveHighContrast(enabled: Boolean) {
        update(HIGH_CONTRAST_KEY, enabled.toString())
    }

    fun loadZoom(): Float {
        val value = read().getProperty(ZOOM_KEY)?.trim()?.toFloatOrNull() ?: return 1f
        return value.takeIf { it.isFinite() && it in 0.5f..3f } ?: 1f
    }

    fun saveZoom(scale: Float) {
        update(ZOOM_KEY, scale.toString())
    }

    fun loadGitPath(): String =
        read().getProperty(GIT_PATH_KEY).orEmpty().trim()

    fun saveGitPath(path: String) {
        update(GIT_PATH_KEY, path.trim())
    }

    fun loadDefaultBranch(): String =
        read().getProperty(DEFAULT_BRANCH_KEY).orEmpty().trim().ifBlank { "main" }

    fun saveDefaultBranch(branch: String) {
        update(DEFAULT_BRANCH_KEY, branch.trim().ifBlank { "main" })
    }

    /** Null means: use the platform shell (PowerShell on Windows, POSIX elsewhere). */
    fun loadCopyShell(): ShellFlavor? {
        val value = read().getProperty(COPY_SHELL_KEY)?.trim().orEmpty()
        if (value.isEmpty()) return null
        return ShellFlavor.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }

    fun saveCopyShell(shell: ShellFlavor?) {
        update(COPY_SHELL_KEY, shell?.name.orEmpty())
    }

    fun loadCloseToTray(): Boolean = loadBoolean(CLOSE_TO_TRAY_KEY)

    fun saveCloseToTray(enabled: Boolean) {
        update(CLOSE_TO_TRAY_KEY, enabled.toString())
    }

    fun loadStartAtLogin(): Boolean = loadBoolean(START_AT_LOGIN_KEY)

    fun saveStartAtLogin(enabled: Boolean) {
        update(START_AT_LOGIN_KEY, enabled.toString())
    }

    fun loadUpdateChannel(): UpdateChannel =
        UpdateChannel.parse(read().getProperty(UPDATE_CHANNEL_KEY))

    fun saveUpdateChannel(channel: UpdateChannel) {
        update(UPDATE_CHANNEL_KEY, channel.wireName)
    }

    fun loadDismissedGitVersion(): String? =
        read().getProperty(DISMISSED_GIT_VERSION_KEY).orEmpty().trim().ifBlank { null }

    fun saveDismissedGitVersion(version: String?) {
        update(DISMISSED_GIT_VERSION_KEY, version.orEmpty().trim())
    }

    fun loadAuthUsername(): String =
        read().getProperty(AUTH_USERNAME_KEY).orEmpty().trim()

    fun saveAuthUsername(username: String) {
        update(AUTH_USERNAME_KEY, username.trim())
    }

    /** Empty means: keep using the user's git credential.helper configuration. */
    fun loadCredentialHelper(): String =
        read().getProperty(AUTH_HELPER_KEY).orEmpty().trim()

    fun saveCredentialHelper(helper: String) {
        update(AUTH_HELPER_KEY, helper.trim())
    }

    fun loadSshMode(): SshAuthMode =
        SshAuthMode.parse(read().getProperty(AUTH_SSH_MODE_KEY))

    fun saveSshMode(mode: SshAuthMode) {
        update(AUTH_SSH_MODE_KEY, mode.label)
    }

    fun loadSshKeyPath(): String =
        read().getProperty(AUTH_SSH_KEY_KEY).orEmpty().trim()

    fun saveSshKeyPath(path: String) {
        update(AUTH_SSH_KEY_KEY, path.trim())
    }

    fun loadAiSettings(): AiSettings {
        val properties = read()
        return AiSettings(
            enabled = properties.getProperty(AI_ENABLED_KEY)
                ?.trim()?.equals("true", ignoreCase = true) == true,
            endpoint = properties.getProperty(AI_ENDPOINT_KEY).orEmpty().trim(),
            model = properties.getProperty(AI_MODEL_KEY).orEmpty().trim()
                .ifBlank { "gpt-4o-mini" },
            messageLanguage = AiMessageLanguage.parse(properties.getProperty(AI_LANGUAGE_KEY)),
            style = AiCommitStyle.parse(properties.getProperty(AI_STYLE_KEY)),
            maxDiffChars = properties.getProperty(AI_MAX_DIFF_KEY)
                ?.trim()?.toIntOrNull()
                ?.coerceIn(AiSettings.MIN_DIFF_CHARS, AiSettings.MAX_DIFF_CHARS)
                ?: 12_000,
            temperature = properties.getProperty(AI_TEMPERATURE_KEY)
                ?.trim()?.toDoubleOrNull()
                ?.takeIf { it.isFinite() }
                ?.coerceIn(AiSettings.MIN_TEMPERATURE, AiSettings.MAX_TEMPERATURE)
                ?: 0.2,
            allowLocalHttp = properties.getProperty(AI_ALLOW_LOCAL_HTTP_KEY)
                ?.trim()?.equals("true", ignoreCase = true) == true,
        )
    }

    fun saveAiSettings(settings: AiSettings) {
        update(AI_ENABLED_KEY, settings.enabled.toString())
        update(AI_ENDPOINT_KEY, settings.endpoint.trim())
        update(AI_MODEL_KEY, settings.model.trim())
        update(AI_LANGUAGE_KEY, settings.messageLanguage.wireName)
        update(AI_STYLE_KEY, settings.style.wireName)
        update(
            AI_MAX_DIFF_KEY,
            settings.maxDiffChars.coerceIn(AiSettings.MIN_DIFF_CHARS, AiSettings.MAX_DIFF_CHARS).toString(),
        )
        update(
            AI_TEMPERATURE_KEY,
            settings.temperature
                .takeIf { it.isFinite() }
                ?.coerceIn(AiSettings.MIN_TEMPERATURE, AiSettings.MAX_TEMPERATURE)
                ?.toString()
                ?: "0.2",
        )
        update(AI_ALLOW_LOCAL_HTTP_KEY, settings.allowLocalHttp.toString())
    }

    private fun loadBoolean(key: String): Boolean =
        read().getProperty(key)?.trim()?.equals("true", ignoreCase = true) == true

    private fun read(): Properties {
        // Settings written by an older build may still have group/world access.
        // Harden the existing config directory and file before loading values.
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(file)) return Properties()
        OwnerOnlyPermissions.restrictFile(file)
        return runCatching {
            Properties().apply {
                Files.newInputStream(file).use(::load)
            }
        }.getOrDefault(Properties())
    }

    private fun update(key: String, value: String) {
        val parent = file.parent ?: return
        runCatching {
            Files.createDirectories(parent)
            OwnerOnlyPermissions.restrictDirectory(parent)
            val properties = read()
            properties.setProperty(key, value)
            val temp = Files.createTempFile(parent, "settings-", ".tmp")
            try {
                OwnerOnlyPermissions.restrictFile(temp)
                Files.newOutputStream(temp).use {
                    properties.store(it, "MyBeautifulGit-chan settings")
                }
                runCatching {
                    Files.move(
                        temp,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                }.getOrElse {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
                }
                OwnerOnlyPermissions.restrictFile(file)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
    }

    private companion object {
        const val PICKER_KEY = "picker.directory"
        const val LANGUAGE_KEY = "ui.language"
        const val THEME_KEY = "ui.theme"
        const val FONT_KEY = "ui.font"
        const val HIGH_CONTRAST_KEY = "ui.highContrast"
        const val ZOOM_KEY = "ui.zoom"
        const val GIT_PATH_KEY = "git.executable"
        const val DEFAULT_BRANCH_KEY = "git.defaultBranch"
        const val COPY_SHELL_KEY = "commands.copyShell"
        const val CLOSE_TO_TRAY_KEY = "desktop.closeToTray"
        const val START_AT_LOGIN_KEY = "desktop.startAtLogin"
        const val UPDATE_CHANNEL_KEY = "updates.channel"
        const val DISMISSED_GIT_VERSION_KEY = "git.dismissedVersion"
        const val AUTH_USERNAME_KEY = "auth.username"
        const val AUTH_HELPER_KEY = "auth.credentialHelper"
        const val AUTH_SSH_MODE_KEY = "auth.sshMode"
        const val AUTH_SSH_KEY_KEY = "auth.sshKeyPath"
        const val AI_ENABLED_KEY = "ai.commitMessage.enabled"
        const val AI_ENDPOINT_KEY = "ai.commitMessage.endpoint"
        const val AI_MODEL_KEY = "ai.commitMessage.model"
        const val AI_LANGUAGE_KEY = "ai.commitMessage.language"
        const val AI_STYLE_KEY = "ai.commitMessage.style"
        const val AI_MAX_DIFF_KEY = "ai.commitMessage.maxDiffChars"
        const val AI_TEMPERATURE_KEY = "ai.commitMessage.temperature"
        const val AI_ALLOW_LOCAL_HTTP_KEY = "ai.commitMessage.allowLocalHttp"
    }
}
