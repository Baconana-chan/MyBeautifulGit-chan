package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.git.cli.GitDiscovery
import org.baconana.mybeautifulgit_chan.git.cli.GitFeature
import org.baconana.mybeautifulgit_chan.git.cli.GitSupportPolicy
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.platform.DirectoryPickerMode
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Panel
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun WelcomeScreen(
    runtime: GitRuntime,
    discovery: GitDiscovery,
    recent: List<String>,
    errorText: String?,
    onDismissError: () -> Unit,
    onOpen: () -> Unit,
    onOpenRecent: (String) -> Unit,
    onForgetRecent: (String) -> Unit,
    onClone: () -> Unit,
    onInit: () -> Unit,
    pickerMode: DirectoryPickerMode,
    onTogglePicker: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onOpenSettings: () -> Unit,
    updateReminderDismissed: Boolean,
    onUpdateGit: () -> Unit,
    onDismissUpdateReminder: () -> Unit,
    onQuit: () -> Unit,
) {
    val strings = LocalStrings.current
    val policy = remember { GitSupportPolicy() }
    val securitySupported = policy.hasSecuritySupport(runtime.version)
    val fullToolset = policy.hasFullToolset(runtime.version)
    val missing = discovery.unavailableFeatures
    val showUpdateReminder = (!securitySupported || !fullToolset) && !updateReminderDismissed

    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("█", fontSize = 11.sp, fontFamily = Term.mono, color = Term.green)
            Text(
                " START ",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.black,
                modifier = Modifier.background(Term.green),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                runtime.version.raw.trim(),
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            ActionButton(key = "S", label = "settings", onClick = onOpenSettings)
            Spacer(Modifier.width(12.dp))
            LanguageSwitch(language, onLanguageChange)
            Spacer(Modifier.width(14.dp))
            ActionButton(key = "Q", label = "quit", onClick = onQuit)
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.width(880.dp)) {
                Text(
                    "MYBEAUTIFULGIT-CHAN",
                    fontSize = 30.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.green,
                    letterSpacing = 2.sp,
                    modifier = Modifier.a11yHeading(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.welcomeTagline,
                    fontSize = 10.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(22.dp))

                errorText?.let {
                    ErrorBanner(it, onDismissError)
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.a11yFocusGroup(),
                ) {
                    WelcomeAction("O", "OPEN REPOSITORY", primary = true, onClick = onOpen)
                    WelcomeAction("C", "CLONE", onClick = onClone)
                    WelcomeAction("I", "INIT", onClick = onInit)
                }

                Spacer(Modifier.height(18.dp))
                Panel(title = "RECENT", trailing = "${recent.size}") {
                    if (recent.isEmpty()) {
                        Text(
                            strings.welcomeNoRecent,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        )
                    } else {
                        recent.take(6).forEachIndexed { index, path ->
                            RecentRow(
                                path = path,
                                onOpen = { onOpenRecent(path) },
                                onForget = { onForgetRecent(path) },
                            )
                            if (index != recent.take(6).lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (showUpdateReminder) {
                    if (!securitySupported) {
                        WarningLine(
                            tag = "EOL",
                            color = Term.amber,
                            text = strings.welcomeEolWarning(
                                runtime.version.major,
                                runtime.version.minor,
                            ),
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                    if (!fullToolset) {
                        WarningLine(
                            tag = "PARTIAL",
                            color = Term.dim,
                            text = strings.welcomePartialWarning(
                                missing.size,
                                missing.take(3).joinToString(" · ") { it.title(strings.language) },
                            ),
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        ActionButton(
                            key = null,
                            label = strings.welcomeUpdateGit,
                            primary = true,
                            onClick = onUpdateGit,
                        )
                        Text(
                            strings.welcomeDontRemind(runtime.version.key),
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier
                                .clickable { onDismissUpdateReminder() }
                                .a11yButton(strings.welcomeDontRemind(runtime.version.key))
                                .a11yFocusRing()
                                .padding(horizontal = 3.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("DROP A REPOSITORY FOLDER ANYWHERE")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${missing.size} unavailable",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (missing.isEmpty()) Term.greenDim else Term.faint,
                    )
                }
            }
        }

        Divider()
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                runtime.executable,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text("Ctrl+= / Ctrl+- / Ctrl+0 zoom", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(12.dp))
            Text("picker ", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Text(
                "[${pickerMode.label}]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                modifier = Modifier
                    .clickable { onTogglePicker() }
                    .a11yButton(label = "Toggle folder picker")
                    .a11yFocusRing()
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun WelcomeAction(
    key: String,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(196.dp).height(52.dp)
            .background(if (primary) Term.green else Term.panelAlt)
            .border(1.dp, if (primary) Term.green else Term.borderHi)
            .clickable { onClick() }
            .a11yButton(label = label)
            .a11yFocusRing()
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Text(
            "[$key]",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = if (primary) Term.black else Term.green,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Term.black else Term.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecentRow(path: String, onOpen: () -> Unit, onForget: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .clickable { onOpen() }
            .a11yButton(strings.a11yOpenRecent(path))
            .a11yFocusRing()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("›", fontSize = 11.sp, fontFamily = Term.mono, color = Term.greenDim)
        Spacer(Modifier.width(7.dp))
        Text(
            path,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "[x]",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier
                .sizeIn(minWidth = 24.dp, minHeight = 22.dp)
                .clickable { onForget() }
                .a11yButton(strings.a11yForgetRecent(path))
                .a11yFocusRing()
                .padding(horizontal = 3.dp),
        )
    }
}

@Composable
private fun WarningLine(tag: String, color: androidx.compose.ui.graphics.Color, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("[$tag]", fontSize = 8.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(7.dp))
        Text(text, fontSize = 8.5.sp, fontFamily = Term.mono, color = color)
    }
}
