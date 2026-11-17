package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val historyTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/** Единый экран журнала: что сделали, когда, с каким результатом и как отменить. */
@Composable
fun HistoryTab(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    val entries = controller.journal.asReversed()

    Column(Modifier.fillMaxSize().background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(34.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "HISTORY",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "APPLICATION JOURNAL",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${controller.journal.size} ${strings.journalOperations} · " +
                    "${controller.journalUndoDepth} ${strings.journalUndo} / " +
                    "${controller.journalRedoDepth} ${strings.journalRedo}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            ActionButton(
                key = null,
                label = strings.journalUndo.uppercase(),
                enabled = controller.canUndoJournal,
            ) { scope.launch { controller.undoJournal() } }
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = strings.journalRedo.uppercase(),
                enabled = controller.canRedoJournal,
            ) { scope.launch { controller.redoJournal() } }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().height(23.dp).background(Term.black)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.journalOnlyLatest,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Divider()

        if (entries.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        strings.journalEmpty,
                        fontSize = 9.5.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.journalEmptyHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    JournalEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry) {
    val strings = LocalStrings.current
    val stateColor = when {
        entry.state == JournalEntryState.Undone -> Term.amber
        entry.action != null -> Term.greenDim
        else -> Term.faint
    }
    val stateLabel = when {
        entry.state == JournalEntryState.Undone -> strings.journalUndone
        entry.action != null -> strings.journalUndoable
        else -> strings.journalNoUndo
    }

    Column(Modifier.fillMaxWidth().background(Term.panel).padding(horizontal = 9.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${entry.id}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                formatHistoryTime(entry.timestampMs),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (entry.success) "[ok]" else "[failed]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (entry.success) Term.green else Term.red,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                entry.message,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stateLabel,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = stateColor,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "$ ${entry.command}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "[copy]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clickable { DesktopActions.copy(entry.command) },
            )
        }
        journalActionDetail(entry.action)?.let { detail ->
            Spacer(Modifier.height(4.dp))
            Text(
                detail,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun journalActionDetail(action: JournalAction?): String? = when (action) {
    null -> null
    is JournalAction.Rewrite -> "rewrite · ${shortTip(action.undo.backup.previousTip)} → " +
        "${shortTip(action.undo.postTip)} · ${action.undo.backup.operation}"
    is JournalAction.CommitReset -> "commit · ${shortTip(action.beforeTip)} → " +
        "${shortTip(action.afterTip)}" + (action.branch?.let { " · $it" } ?: "")
    is JournalAction.Checkout -> "checkout · ${action.beforeRef} → " +
        shortTip(action.afterRef)
}

private fun shortTip(value: String): String =
    value.takeIf { it.isNotBlank() }?.take(7) ?: "?"

private fun formatHistoryTime(epochMs: Long): String = runCatching {
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(historyTimeFormatter)
}.getOrDefault("--:--:--")