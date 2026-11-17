package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.git.cli.GitDiscovery
import org.baconana.mybeautifulgit_chan.git.cli.GitInstallOption
import org.baconana.mybeautifulgit_chan.git.cli.GitStatus
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Panel
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun GitSetupScreen(
    discovery: GitDiscovery,
    installOptions: List<GitInstallOption>,
    manualPath: String,
    onManualPathChange: (String) -> Unit,
    onCheck: () -> Unit,
    checking: Boolean,
    installing: Boolean,
    installError: String?,
    onInstall: (GitInstallOption) -> Unit,
    onDismissInstallError: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopy: (String) -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalStrings.current
    val tooOld = discovery.status == GitStatus.UNSUPPORTED_VERSION
    val minimum = GitVersion.MINIMUM_SUPPORTED

    Column(
        Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ENVIRONMENT",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "git ${minimum.major}.${minimum.minor}+ required",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.width(12.dp))
            ActionButton(key = "S", label = "settings", onClick = onOpenSettings)
            Spacer(Modifier.width(12.dp))
            LanguageSwitch(language, onLanguageChange)
        }
        Spacer(Modifier.height(24.dp))

        Text(
            if (tooOld) "GIT IS TOO OLD" else "GIT NOT FOUND",
            fontSize = 27.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.amber,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            discovery.problem ?: strings.setupGitNotFound,
            fontSize = 10.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
        )

        discovery.installation?.let { installation ->
            Spacer(Modifier.height(10.dp))
            Text(
                "${installation.executable}  ·  ${installation.version.raw}",
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        installError?.let { message ->
            Spacer(Modifier.height(12.dp))
            ErrorBanner(message, onDismiss = onDismissInstallError)
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SectionLabel(strings.setupInstall)
                installOptions.forEach { option ->
                    InstallOptionRow(
                        option = option,
                        installing = installing,
                        onInstall = onInstall,
                        onOpenUrl = onOpenUrl,
                        onCopy = onCopy,
                    )
                }
                if (installOptions.isEmpty()) {
                    Text(
                        strings.setupNoInstallOptions,
                        fontSize = 9.5.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                }

                if (tooOld && discovery.unavailableFeatures.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))
                    SectionLabel(strings.setupMissingFeatures)
                    discovery.unavailableFeatures.forEach { feature ->
                        Text(
                            strings.setupFeatureSince(
                                feature.title(strings.language),
                                feature.since.major,
                                feature.since.minor,
                            ),
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(Modifier.width(360.dp)) {
                Panel(title = strings.setupManualPathTitle) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            strings.setupManualPathHint,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                        Spacer(Modifier.height(7.dp))
                        TermTextField(
                            value = manualPath,
                            onValueChange = onManualPathChange,
                            placeholder = "C:\\Program Files\\Git\\cmd\\git.exe",
                        )
                        Spacer(Modifier.height(8.dp))
                        ActionButton(
                            key = null,
                            label = if (checking) "checking…" else "CHECK PATH",
                            primary = true,
                            enabled = !checking,
                            onClick = onCheck,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Panel(title = strings.setupSearchTitle) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            strings.setupSearchHint,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                        Spacer(Modifier.height(7.dp))
                        discovery.searched.take(6).forEach { path ->
                            Text(
                                path,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.faint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    strings.setupAfterInstall,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }
    }
}

@Composable
internal fun InstallOptionRow(
    option: GitInstallOption,
    installing: Boolean,
    onInstall: (GitInstallOption) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopy: (String) -> Unit,
    actionLabel: String? = null,
) {
    val strings = LocalStrings.current
    Column(
        Modifier.fillMaxWidth()
            .background(Term.panel)
            .border(1.dp, Term.border)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                option.title,
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
            )
            Spacer(Modifier.width(8.dp))
            if (option.requiresElevation) {
                Text("[admin]", fontSize = 8.sp, fontFamily = Term.mono, color = Term.amber)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(option.detail, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.dim)
        option.commandLine?.let { command ->
            Spacer(Modifier.height(7.dp))
            Text(
                "$ $command",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    key = null,
                    label = if (installing) {
                        strings.setupInstalling
                    } else {
                        actionLabel ?: strings.setupInstall
                    },
                    primary = true,
                    enabled = !installing,
                    onClick = { onInstall(option) },
                )
                Spacer(Modifier.width(7.dp))
                ActionButton(key = null, label = "copy", onClick = { onCopy(command) })
            }
        }
        option.url?.let { url ->
            Spacer(Modifier.height(7.dp))
            ActionButton(key = null, label = "OPEN PAGE", onClick = { onOpenUrl(url) })
        }
    }
}
