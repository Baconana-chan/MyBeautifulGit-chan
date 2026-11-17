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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.ReflogEntry
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Полный список переходов HEAD и веток. Восстановление не выполняется
 * по клику на строку: выбранная запись открывает подтверждение с точной
 * командой, чтобы recovery не был необратимым сюрпризом.
 */
@Composable
fun ReflogTab(
    controller: RepoController,
    scope: CoroutineScope,
    focusHash: String?,
    focusId: Long,
    onFocusConsumed: () -> Unit,
    onOpenCommit: (String) -> Unit,
    onRecover: (ReflogEntry, CommitAction) -> Unit,
) {
    val strings = LocalStrings.current
    var filter by remember(controller.repository.root) { mutableStateOf(focusHash.orEmpty()) }
    var selectedSelector by remember(controller.repository.root) { mutableStateOf<String?>(null) }

    LaunchedEffect(controller.repository.root, focusId) {
        if (!focusHash.isNullOrBlank()) {
            filter = focusHash
            onFocusConsumed()
        }
        controller.loadReflog()
    }

    val entries = remember(controller.reflogEntries, filter) {
        val needle = filter.trim()
        if (needle.isEmpty()) {
            controller.reflogEntries
        } else {
            controller.reflogEntries.filter { entry ->
                entry.selector.contains(needle, ignoreCase = true) ||
                    entry.hash.contains(needle, ignoreCase = true) ||
                    entry.subject.contains(needle, ignoreCase = true)
            }
        }
    }
    val selected = entries.firstOrNull { it.selector == selectedSelector }
        ?: entries.firstOrNull()

    LaunchedEffect(entries) {
        if (selectedSelector == null || entries.none { it.selector == selectedSelector }) {
            selectedSelector = entries.firstOrNull()?.selector
        }
    }

    Column(Modifier.fillMaxSize().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "REFLOG",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Text(
                " (${entries.size}${if (entries.size != controller.reflogEntries.size) "/${controller.reflogEntries.size}" else ""})",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                controller.status?.branch?.head ?: "(detached)",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "[refresh]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (controller.reflogLoading) Term.faint else Term.greenDim,
                modifier = Modifier.clickable(enabled = !controller.reflogLoading) {
                    scope.launch { controller.loadReflog() }
                },
            )
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().background(Term.panel).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("FILTER", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.dim)
            Spacer(Modifier.width(6.dp))
            TermTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = strings.reflogFilterPlaceholder,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "git reflog --all --format=%H%x00%gD%x00%gs%x00%ct -n1000",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            strings.reflogPanelHint,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Divider()

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                controller.reflogLoading -> ReflogPlaceholder(strings.commitReflogLoading, Term.faint)
                controller.reflogError != null -> ErrorBanner(
                    "${strings.commitReflogFailed}: ${controller.reflogError}",
                    controller::dismissReflogError,
                    Modifier.padding(8.dp),
                )
                controller.reflogEntries.isEmpty() -> ReflogPlaceholder(
                    strings.commitReflogEmpty,
                    Term.faint,
                )
                entries.isEmpty() -> ReflogPlaceholder(strings.reflogNoMatches, Term.faint)
                else -> ReflogList(
                    entries = entries,
                    selectedSelector = selected?.selector,
                    onSelect = { selectedSelector = it.selector },
                )
            }
        }

        if (selected != null && !controller.reflogLoading && controller.reflogError == null) {
            Divider()
            ReflogRecovery(
                entry = selected,
                controller = controller,
                onOpenCommit = onOpenCommit,
                onRecover = onRecover,
            )
        }
    }
}

@Composable
private fun ReflogList(
    entries: List<ReflogEntry>,
    selectedSelector: String?,
    onSelect: (ReflogEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(19.dp).background(Term.panelAlt)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("REF", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(160.dp))
            Text("COMMIT", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(64.dp))
            Text("MESSAGE", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.weight(1f))
            Text("TIME", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(92.dp))
        }
        Divider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { "${it.selector}|${it.hash}|${it.time}|${it.subject}" }) { entry ->
                val selected = entry.selector == selectedSelector
                Row(
                    Modifier.fillMaxWidth().height(23.dp)
                        .background(if (selected) Term.green.copy(alpha = 0.12f) else Term.panel)
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entry.selector,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Term.green else Term.cyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(160.dp),
                    )
                    Text(
                        entry.hash.take(7),
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.greenDim,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(
                        entry.subject.ifBlank { "—" },
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = if (selected) Term.white else Term.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatReflogTime(entry.time),
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.width(92.dp),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
            }
        }
    }
}

@Composable
private fun ReflogRecovery(
    entry: ReflogEntry,
    controller: RepoController,
    onOpenCommit: (String) -> Unit,
    onRecover: (ReflogEntry, CommitAction) -> Unit,
) {
    val strings = LocalStrings.current
    val enabled = !controller.busy
    Column(Modifier.fillMaxWidth().background(Term.panelAlt).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SELECTED",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "${entry.selector}  ${entry.hash}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatReflogTime(entry.time),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            entry.subject.ifBlank { "—" },
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                key = null,
                label = strings.reflogOpenCommit,
                enabled = enabled,
                onClick = { onOpenCommit(entry.hash) },
            )
            ActionButton(
                key = null,
                label = strings.commitActionCopyHash,
                enabled = true,
                onClick = { DesktopActions.copy(entry.hash) },
            )
            ActionButton(
                key = null,
                label = strings.reflogRestore,
                danger = true,
                enabled = enabled,
                onClick = { onRecover(entry, CommitAction.Reset) },
            )
            ActionButton(
                key = null,
                label = strings.reflogCreateBranch,
                enabled = enabled,
                onClick = { onRecover(entry, CommitAction.Branch) },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${strings.reflogRestoreHint} ${strings.reflogCreateBranchHint}",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            strings.reflogActionHint,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReflogPlaceholder(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = color,
        )
    }
}

/** Имя удалённой локальной ветки, если запись позволяет его вывести. */
fun reflogSuggestedBranch(entry: ReflogEntry): String {
    val ref = entry.selector.substringBefore("@{")
    return if (ref.startsWith("refs/heads/")) ref.removePrefix("refs/heads/") else ""
}

private val REFLOG_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatReflogTime(seconds: Long): String =
    Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).format(REFLOG_TIME)
