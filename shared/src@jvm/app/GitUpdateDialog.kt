package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.git.cli.GitInstallOption
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun GitUpdateDialog(
    currentVersion: String,
    options: List<GitInstallOption>,
    installing: Boolean,
    installError: String?,
    onInstall: (GitInstallOption) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDismissInstallError: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    DialogShell(
        title = "UPDATE GIT",
        width = 620.dp,
        onDismiss = { if (!installing) onDismiss() },
    ) {
        Text(
            strings.updateCurrent(currentVersion),
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            strings.updateHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )

        installError?.let { message ->
            Spacer(Modifier.height(10.dp))
            ErrorBanner(message, onDismiss = onDismissInstallError)
        }

        Spacer(Modifier.height(12.dp))
        if (options.isEmpty()) {
            Text(
                strings.setupNoInstallOptions,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
        } else {
            Column(
                Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    InstallOptionRow(
                        option = option,
                        installing = installing,
                        onInstall = onInstall,
                        onOpenUrl = onOpenUrl,
                        onCopy = onCopy,
                        actionLabel = strings.updateRun,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(
                key = null,
                label = "CLOSE",
                enabled = !installing,
                onClick = onDismiss,
            )
        }
    }
}
