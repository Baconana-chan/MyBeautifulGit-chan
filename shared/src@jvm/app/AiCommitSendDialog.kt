package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.baconana.mybeautifulgit_chan.ai.AiDiffSource
import org.baconana.mybeautifulgit_chan.ai.PreparedCommitMessage
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun AiCommitSendDialog(
    prepared: PreparedCommitMessage,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DialogShell(
            title = strings.aiSendTitle,
            width = 680.dp,
            onDismiss = onDismiss,
        ) {
            Text(
                when {
                    prepared.source == AiDiffSource.Worktree -> strings.aiSendWorktreeWarning
                    prepared.endpointIsLocal -> strings.aiSendLocalWarning
                    else -> strings.aiSendWarning
                },
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
            Spacer(Modifier.height(12.dp))
            PreviewRow(strings.aiDialogEndpoint, prepared.endpoint.toString())
            PreviewRow(strings.aiDialogBranch, prepared.branch)
            PreviewRow(
                strings.aiDialogPayload,
                strings.aiPreviewSummary(prepared.fileCount, prepared.diffChars),
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (prepared.omittedSections > 0) {
                    Text(
                        "${strings.aiDialogOmitted}: ${prepared.omittedSections}",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                if (prepared.truncated) {
                    Text(
                        strings.aiDialogTruncated,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                }
                if (prepared.redacted) {
                    Text(
                        strings.aiDialogRedacted,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButton(key = null, label = strings.aiCancel, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                ActionButton(key = null, label = strings.aiSendConfirm, primary = true, onClick = onSend)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = Term.dim,
            modifier = Modifier.width(74.dp),
        )
        Text(
            value,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
        )
    }
}
