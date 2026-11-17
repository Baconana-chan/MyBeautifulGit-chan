package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun DiscardDialog(
    target: DiscardTarget,
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    val busy = running || controller.busy
    val untracked = target.entry.kind == StatusKind.Untracked
    val targetLabel = when (target.scope) {
        DiscardScope.File -> strings.repoDiscardFile
        DiscardScope.Hunk -> strings.repoDiscardHunk
        DiscardScope.Lines -> strings.repoDiscardLine
    }

    fun run(backup: DiscardBackup) {
        if (busy) return
        scope.launch {
            running = true
            val success = controller.discard(target, backup)
            running = false
            if (success) onDismiss()
        }
    }

    DialogShell(
        title = if (untracked) "DELETE UNTRACKED FILE" else "DISCARD CHANGES",
        width = 620.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Text(
            "$targetLabel · ${target.entry.path}",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.white,
            maxLines = 2,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            strings.repoDiscardWarning,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            when {
                target.scope != DiscardScope.File -> strings.repoDiscardPartialNote
                untracked ->
                    strings.repoDiscardUntrackedNote
                else -> strings.repoDiscardFileNote
            },
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
        )
        if (target.scope == DiscardScope.File) {
            Spacer(Modifier.height(5.dp))
            Text(
                strings.repoDiscardStashHint,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            strings.repoDiscardPatchHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )

        controller.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            ErrorBanner(message, controller::dismissError)
        }

        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            ActionButton(
                key = null,
                label = "CANCEL",
                enabled = !busy,
                onClick = onDismiss,
            )
            if (target.scope == DiscardScope.File) {
                Spacer(Modifier.width(8.dp))
                ActionButton(
                    key = null,
                    label = strings.repoDiscardStash,
                    enabled = !busy,
                    onClick = { run(DiscardBackup.Stash) },
                )
            }
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = strings.repoDiscardPatch,
                enabled = !busy,
                onClick = { run(DiscardBackup.Patch) },
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = when {
                    running -> "WAIT…"
                    untracked -> strings.repoDeleteConfirm
                    else -> strings.repoDiscardConfirm
                },
                danger = true,
                enabled = !busy,
                onClick = { run(DiscardBackup.None) },
            )
        }
    }
}