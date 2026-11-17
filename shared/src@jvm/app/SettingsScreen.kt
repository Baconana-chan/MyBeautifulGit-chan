package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.ai.AiCommitStyle
import org.baconana.mybeautifulgit_chan.ai.AiMessageLanguage
import org.baconana.mybeautifulgit_chan.ai.AiSettings
import org.baconana.mybeautifulgit_chan.ai.CommitMessageGenerator
import org.baconana.mybeautifulgit_chan.git.ShellFlavor
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.domain.SshAuthMode
import org.baconana.mybeautifulgit_chan.domain.UpdateChannel
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.platform.DirectoryPickerMode
import org.baconana.mybeautifulgit_chan.platform.StartAtLogin
import org.baconana.mybeautifulgit_chan.update.GitHubReleases
import org.baconana.mybeautifulgit_chan.update.ReleaseCheckResult
import org.baconana.mybeautifulgit_chan.update.ReleaseCheckStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.FontChoice
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.ThemeMode

data class SettingsValues(
    val theme: ThemeMode,
    val font: FontChoice,
    val highContrast: Boolean = false,
    val gitPath: String,
    val defaultBranch: String,
    val pickerMode: DirectoryPickerMode,
    val language: Language,
    val authUsername: String = "",
    val authToken: String = "",
    val credentialHelper: String = "",
    val sshMode: SshAuthMode = SshAuthMode.Agent,
    val sshKeyPath: String = "",
    val copyShell: ShellFlavor? = null,
    val closeToTray: Boolean = false,
    val startAtLogin: Boolean = false,
    val updateChannel: UpdateChannel = UpdateChannel.Chan,
    val aiEnabled: Boolean = false,
    val aiEndpoint: String = "",
    val aiModel: String = "gpt-4o-mini",
    val aiApiKey: String = "",
    val aiMessageLanguage: AiMessageLanguage = AiMessageLanguage.Auto,
    val aiStyle: AiCommitStyle = AiCommitStyle.Conventional,
    val aiMaxDiffChars: Int = 12_000,
    val aiTemperature: Double = 0.2,
    val aiAllowLocalHttp: Boolean = false,
)

@Composable
fun SettingsScreen(
    initial: SettingsValues,
    onCancel: () -> Unit,
    onSave: (SettingsValues) -> Unit,
) {
    val strings = LocalStrings.current
    var theme by remember { mutableStateOf(initial.theme) }
    var font by remember { mutableStateOf(initial.font) }
    var highContrast by remember { mutableStateOf(initial.highContrast) }
    var gitPath by remember { mutableStateOf(initial.gitPath) }
    var defaultBranch by remember { mutableStateOf(initial.defaultBranch) }
    var copyShell by remember { mutableStateOf(initial.copyShell) }
    var closeToTray by remember { mutableStateOf(initial.closeToTray) }
    var startAtLogin by remember { mutableStateOf(initial.startAtLogin) }
    var updateChannel by remember { mutableStateOf(initial.updateChannel) }
    var aiEnabled by remember { mutableStateOf(initial.aiEnabled) }
    var aiEndpoint by remember { mutableStateOf(initial.aiEndpoint) }
    var aiModel by remember { mutableStateOf(initial.aiModel) }
    var aiApiKey by remember { mutableStateOf(initial.aiApiKey) }
    var aiMessageLanguage by remember { mutableStateOf(initial.aiMessageLanguage) }
    var aiStyle by remember { mutableStateOf(initial.aiStyle) }
    var aiMaxDiffText by remember { mutableStateOf(initial.aiMaxDiffChars.toString()) }
    var aiTemperatureText by remember { mutableStateOf(initial.aiTemperature.toString()) }
    var aiAllowLocalHttp by remember { mutableStateOf(initial.aiAllowLocalHttp) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var releaseCheck by remember { mutableStateOf<ReleaseCheckResult?>(null) }
    val updateScope = rememberCoroutineScope()
    var pickerMode by remember { mutableStateOf(initial.pickerMode) }
    var language by remember { mutableStateOf(initial.language) }
    var authUsername by remember { mutableStateOf(initial.authUsername) }
    var authToken by remember { mutableStateOf(initial.authToken) }
    var credentialHelper by remember { mutableStateOf(initial.credentialHelper) }
    var sshMode by remember { mutableStateOf(initial.sshMode) }
    var sshKeyPath by remember { mutableStateOf(initial.sshKeyPath) }
    var error by remember { mutableStateOf<String?>(null) }

    fun checkForUpdates() {
        if (checkingUpdates) return
        checkingUpdates = true
        releaseCheck = null
        val channel = updateChannel
        updateScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    GitHubReleases.check(channel)
                }
                if (updateChannel == channel) releaseCheck = result
            } finally {
                checkingUpdates = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Term.bg)) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).background(Term.raised)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "█",
                fontSize = 11.sp,
                fontFamily = Term.mono,
                color = Term.green,
            )
            Text(
                " SETTINGS ",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.black,
                modifier = Modifier.background(Term.green).a11yHeading(),
            )
            Spacer(Modifier.weight(1f))
            ActionButton(key = null, label = "CANCEL", onClick = onCancel)
        }
        Divider()

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.width(220.dp).fillMaxHeight().background(Term.panel)
                    .padding(18.dp),
            ) {
                Text(
                    "PREFERENCES",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.SemiBold,
                    color = Term.green,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "applied on save",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    "git config --global",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }

            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                    .padding(horizontal = 34.dp, vertical = 20.dp),
            ) {
                SectionLabel("APPEARANCE")
                SettingRow("theme") {
                    ThemeMode.entries.forEach { mode ->
                        Choice(
                            label = mode.label,
                            selected = theme == mode,
                            onClick = { theme = mode },
                        )
                    }
                }
                SettingRow("font") {
                    FontChoice.entries.forEach { choice ->
                        Choice(
                            label = choice.label,
                            selected = font == choice,
                            onClick = { font = choice },
                        )
                    }
                }
                SettingRow(strings.settingsContrast) {
                    Choice(
                        label = strings.contrastNormal,
                        selected = !highContrast,
                        onClick = { highContrast = false },
                    )
                    Choice(
                        label = strings.contrastHigh,
                        selected = highContrast,
                        onClick = { highContrast = true },
                    )
                }
                Text(
                    "Aa  git status  +  diff  —  commit",
                    fontSize = 11.sp,
                    fontFamily = font.family,
                    color = Term.dim,
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                )

                Spacer(Modifier.height(14.dp))
                SectionLabel("GIT")
                Text(
                    "executable",
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(5.dp))
                TermTextField(
                    value = gitPath,
                    onValueChange = { gitPath = it },
                    placeholder = "auto-detect",
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.settingsGitPathHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )

                Spacer(Modifier.height(18.dp))
                SectionLabel("BEHAVIOR")
                Text(
                    "default branch",
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(5.dp))
                TermTextField(
                    value = defaultBranch,
                    onValueChange = { defaultBranch = it },
                    placeholder = "main",
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.settingsDefaultBranchHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(14.dp))
                SettingRow(strings.settingsCopyShell) {
                    Choice(
                        label = "auto",
                        selected = copyShell == null,
                        onClick = { copyShell = null },
                    )
                    listOf(
                        ShellFlavor.CMD,
                        ShellFlavor.POWERSHELL,
                        ShellFlavor.POSIX,
                    ).forEach { shell ->
                        Choice(
                            label = shell.name.lowercase(),
                            selected = copyShell == shell,
                            onClick = { copyShell = shell },
                        )
                    }
                }
                Text(
                    strings.settingsCopyShellHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(14.dp))
                SettingRow("folder picker") {
                    Choice(
                        label = "custom",
                        selected = pickerMode == DirectoryPickerMode.Custom,
                        onClick = { pickerMode = DirectoryPickerMode.Custom },
                    )
                    Choice(
                        label = "system",
                        selected = pickerMode == DirectoryPickerMode.System,
                        onClick = { pickerMode = DirectoryPickerMode.System },
                    )
                }
                SettingRow("language") {
                    LanguageSwitch(language) { language = it }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("DESKTOP")
                SettingRow(strings.settingsCloseToTray) {
                    Choice(
                        label = strings.settingsOff,
                        selected = !closeToTray,
                        onClick = { closeToTray = false },
                    )
                    Choice(
                        label = strings.settingsOn,
                        selected = closeToTray,
                        onClick = { closeToTray = true },
                    )
                }
                Text(
                    strings.settingsCloseToTrayHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(10.dp))
                SettingRow(strings.settingsStartAtLogin) {
                    Choice(
                        label = strings.settingsOff,
                        selected = !startAtLogin,
                        onClick = { startAtLogin = false },
                    )
                    Choice(
                        label = strings.settingsOn,
                        selected = startAtLogin,
                        onClick = { startAtLogin = true },
                    )
                }
                Text(
                    strings.settingsStartAtLoginHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )

                Spacer(Modifier.height(18.dp))
                SectionLabel("UPDATES")
                SettingRow(strings.settingsUpdateChannel) {
                    Choice(
                        label = strings.settingsUpdateChannelChan,
                        selected = updateChannel == UpdateChannel.Chan,
                        onClick = {
                            updateChannel = UpdateChannel.Chan
                            releaseCheck = null
                        },
                    )
                    Choice(
                        label = strings.settingsUpdateChannelKun,
                        selected = updateChannel == UpdateChannel.Kun,
                        onClick = {
                            updateChannel = UpdateChannel.Kun
                            releaseCheck = null
                        },
                    )
                }
                Text(
                    strings.settingsUpdateChannelHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionButton(
                        key = null,
                        label = if (checkingUpdates) {
                            strings.settingsUpdateChecking
                        } else {
                            strings.settingsUpdateCheck
                        },
                        primary = true,
                        enabled = !checkingUpdates,
                        onClick = { checkForUpdates() },
                    )
                    releaseCheck
                        ?.takeIf { it.status == ReleaseCheckStatus.UpdateAvailable }
                        ?.release
                        ?.let { release ->
                            Spacer(Modifier.width(8.dp))
                            ActionButton(
                                key = null,
                                label = strings.settingsUpdateOpen,
                                onClick = { DesktopActions.openUrl(release.pageUrl) },
                            )
                        }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsUpdateNoInstall,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                releaseCheck?.let { result ->
                    val message = when (result.status) {
                        ReleaseCheckStatus.UpdateAvailable -> strings.settingsUpdateAvailable(
                            current = result.current.display,
                            latest = result.release?.tag.orEmpty(),
                        )
                        ReleaseCheckStatus.UpToDate ->
                            strings.settingsUpdateUpToDate(result.current.display)
                        ReleaseCheckStatus.NoReleases -> strings.settingsUpdateNoReleases
                        ReleaseCheckStatus.RepositoryUnavailable ->
                            strings.settingsUpdateRepositoryUnavailable
                        ReleaseCheckStatus.Failed ->
                            strings.settingsUpdateFailed(result.failure.orEmpty())
                    }
                    val color = when (result.status) {
                        ReleaseCheckStatus.UpdateAvailable -> Term.green
                        ReleaseCheckStatus.UpToDate -> Term.dim
                        else -> Term.amber
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        message,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = color,
                    )
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel(strings.aiSettingsSection)
                SettingRow(strings.aiEnabled) {
                    Choice(
                        label = strings.settingsOff,
                        selected = !aiEnabled,
                        onClick = { aiEnabled = false },
                    )
                    Choice(
                        label = strings.settingsOn,
                        selected = aiEnabled,
                        onClick = { aiEnabled = true },
                    )
                }
                Text(
                    strings.aiEnabledHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                if (aiEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        strings.aiEndpoint,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(5.dp))
                    TermTextField(
                        value = aiEndpoint,
                        onValueChange = { aiEndpoint = it },
                        placeholder = "https://api.openai.com/v1/chat/completions",
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.aiEndpointHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        strings.aiModel,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(5.dp))
                    TermTextField(
                        value = aiModel,
                        onValueChange = { aiModel = it },
                        placeholder = "gpt-4o-mini",
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        strings.aiApiKey,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(5.dp))
                    TermTextField(
                        value = aiApiKey,
                        onValueChange = { aiApiKey = it },
                        placeholder = "sk-…",
                        secret = true,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.aiApiKeyHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(9.dp))
                    SettingRow(strings.aiMessageLanguage) {
                        AiMessageLanguage.entries.forEach { language ->
                            Choice(
                                label = when (language) {
                                    AiMessageLanguage.Auto -> strings.aiLanguageAuto
                                    AiMessageLanguage.English -> strings.aiLanguageEnglish
                                    AiMessageLanguage.Russian -> strings.aiLanguageRussian
                                },
                                selected = aiMessageLanguage == language,
                                onClick = { aiMessageLanguage = language },
                            )
                        }
                    }
                    SettingRow(strings.aiStyle) {
                        AiCommitStyle.entries.forEach { style ->
                            Choice(
                                label = when (style) {
                                    AiCommitStyle.Conventional -> strings.aiStyleConventional
                                    AiCommitStyle.Plain -> strings.aiStylePlain
                                },
                                selected = aiStyle == style,
                                onClick = { aiStyle = style },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.aiMaxDiff,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                            modifier = Modifier.width(140.dp),
                        )
                        TermTextField(
                            value = aiMaxDiffText,
                            onValueChange = { aiMaxDiffText = it },
                            placeholder = "12000",
                            modifier = Modifier.width(110.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            strings.aiTemperature,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                        Spacer(Modifier.width(5.dp))
                        TermTextField(
                            value = aiTemperatureText,
                            onValueChange = { aiTemperatureText = it },
                            placeholder = "0.2",
                            modifier = Modifier.width(70.dp),
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    SettingRow(strings.aiAllowLocalHttp) {
                        Choice(
                            label = strings.settingsOff,
                            selected = !aiAllowLocalHttp,
                            onClick = { aiAllowLocalHttp = false },
                        )
                        Choice(
                            label = strings.settingsOn,
                            selected = aiAllowLocalHttp,
                            onClick = { aiAllowLocalHttp = true },
                        )
                    }
                    Text(
                        strings.aiAllowLocalHttpHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        strings.aiSecurityHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("AUTHENTICATION")
                Text(
                    strings.settingsAuthUsername,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(5.dp))
                TermTextField(
                    value = authUsername,
                    onValueChange = { authUsername = it },
                    placeholder = "git",
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsAuthUsernameHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    strings.settingsAuthToken,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(5.dp))
                TermTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    placeholder = "personal access token",
                    secret = true,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsAuthTokenHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    strings.settingsAuthHelper,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(5.dp))
                TermTextField(
                    value = credentialHelper,
                    onValueChange = { credentialHelper = it },
                    placeholder = "system default",
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsAuthHelperHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsAuthHelperWarning,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.amber,
                )

                Spacer(Modifier.height(14.dp))
                SettingRow(strings.settingsAuthSsh) {
                    Choice(
                        label = strings.settingsAuthSshAgent,
                        selected = sshMode == SshAuthMode.Agent,
                        onClick = { sshMode = SshAuthMode.Agent },
                    )
                    Choice(
                        label = strings.settingsAuthSshKey,
                        selected = sshMode == SshAuthMode.Key,
                        onClick = { sshMode = SshAuthMode.Key },
                    )
                }
                if (sshMode == SshAuthMode.Key) {
                    Spacer(Modifier.height(5.dp))
                    TermTextField(
                        value = sshKeyPath,
                        onValueChange = { sshKeyPath = it },
                        placeholder = "C:\\Users\\you\\.ssh\\id_ed25519",
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.settingsAuthSshKeyHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )

                error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorBanner(it, onDismiss = { error = null })
                }

                Spacer(Modifier.height(26.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionButton(key = null, label = "CANCEL", onClick = onCancel)
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        key = null,
                        label = "SAVE",
                        primary = true,
                        onClick = {
                            val branch = defaultBranch.trim().ifBlank { "main" }
                            val token = authToken.trim()
                            val helper = credentialHelper.trim()
                            val keyPath = sshKeyPath.trim()
                            val aiMaxDiff = aiMaxDiffText.trim().toIntOrNull()
                            val aiTemperature = aiTemperatureText.trim().toDoubleOrNull()
                            val aiEndpointValid = !aiEnabled || runCatching {
                                CommitMessageGenerator.validateEndpoint(aiEndpoint, aiAllowLocalHttp)
                            }.isSuccess
                            error = when {
                                branch.any { it.isWhitespace() } || branch.startsWith("-") ->
                                    strings.settingsDefaultBranchInvalid
                                token.any { it == '\n' || it == '\r' || it == '\u0000' } ->
                                    strings.settingsAuthTokenInvalid
                                helper.any { it == '\n' || it == '\r' || it == '\u0000' } ->
                                    strings.settingsAuthHelperInvalid
                                sshMode == SshAuthMode.Key && keyPath.isBlank() ->
                                    strings.settingsAuthSshKeyRequired
                                aiEnabled && !aiEndpointValid -> strings.aiEndpointInvalid
                                aiEnabled && aiModel.trim().isEmpty() -> strings.aiModelRequired
                                aiEnabled && (aiMaxDiff == null ||
                                    aiMaxDiff !in AiSettings.MIN_DIFF_CHARS..AiSettings.MAX_DIFF_CHARS) ->
                                    strings.aiMaxDiffInvalid
                                aiEnabled && (aiTemperature == null ||
                                    !aiTemperature.isFinite() ||
                                    aiTemperature !in AiSettings.MIN_TEMPERATURE..AiSettings.MAX_TEMPERATURE) ->
                                    strings.aiTemperatureInvalid
                                else -> null
                            }
                            if (error == null && startAtLogin != initial.startAtLogin) {
                                val result = StartAtLogin.setEnabled(startAtLogin)
                                result.exceptionOrNull()?.let {
                                    error = strings.settingsStartAtLoginFailed(it.message.orEmpty())
                                }
                            }
                            if (error == null) {
                                onSave(
                                    SettingsValues(
                                        theme = theme,
                                        font = font,
                                        highContrast = highContrast,
                                        gitPath = gitPath.trim(),
                                        defaultBranch = branch,
                                        copyShell = copyShell,
                                        pickerMode = pickerMode,
                                        language = language,
                                        authUsername = authUsername.trim(),
                                        authToken = token,
                                        credentialHelper = helper,
                                        sshMode = sshMode,
                                        sshKeyPath = keyPath,
                                        closeToTray = closeToTray,
                                        startAtLogin = startAtLogin,
                                        updateChannel = updateChannel,
                                        aiEnabled = aiEnabled,
                                        aiEndpoint = aiEndpoint.trim(),
                                        aiModel = aiModel.trim(),
                                        aiApiKey = aiApiKey.trim(),
                                        aiMessageLanguage = aiMessageLanguage,
                                        aiStyle = aiStyle,
                                        aiMaxDiffChars = aiMaxDiff ?: 12_000,
                                        aiTemperature = aiTemperature ?: 0.2,
                                        aiAllowLocalHttp = aiAllowLocalHttp,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).a11yFocusGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            modifier = Modifier.width(140.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        label,
        fontSize = 9.sp,
        fontFamily = Term.mono,
        color = if (selected) Term.black else Term.dim,
        modifier = Modifier
            .background(if (selected) Term.green else Term.panelAlt)
            .border(1.dp, if (selected) Term.green else Term.border)
            .clickable(onClick = onClick)
            .a11yButton(label = label, selected = selected, role = Role.RadioButton)
            .a11yFocusRing()
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}
