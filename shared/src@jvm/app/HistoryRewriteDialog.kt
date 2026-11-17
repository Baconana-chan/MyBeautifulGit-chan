package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

enum class HistoryRewriteMode { Reorder, Onto, MoveRange, Squash }

@Composable
fun HistoryRewriteDialog(
    commit: Commit,
    controller: RepoController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var mode by remember(commit.hash) { mutableStateOf(HistoryRewriteMode.Reorder) }
    var upstream by remember(commit.hash) { mutableStateOf("") }
    var onto by remember(commit.hash) { mutableStateOf("") }
    var range by remember(commit.hash) { mutableStateOf("") }
    var message by remember(commit.hash) { mutableStateOf("") }
    var preserveMerges by remember(commit.hash) { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val busy = running || controller.busy
    val parent = commit.parents.firstOrNull()
    val parentCommit = controller.commits.firstOrNull { it.hash == parent }
    val children = controller.commits.filter { it.parents.contains(commit.hash) }
    val child = children.singleOrNull()
    val canEarlier = !busy && !commit.isMerge && parentCommit != null &&
        !parentCommit.isMerge
    val canLater = !busy && !commit.isMerge && child != null && !child.isMerge &&
        children.size == 1
    val rangeStart = range.trim().split("..", limit = 2)
        .getOrNull(0)
        ?.trim()
        .orEmpty()
    val mergeFlag = if (preserveMerges) "--rebase-merges " else ""

    fun finish(success: Boolean, activeBefore: Boolean) {
        running = false
        val startedNow = !activeBefore && (
            controller.operation != null || controller.pendingNoCommit != null
            )
        if (success || startedNow) {
            onDismiss()
            onSuccess()
        }
    }

    fun runReorder(earlier: Boolean) {
        if (busy) return
        scope.launch {
            running = true
            val activeBefore = controller.operation != null || controller.pendingNoCommit != null
            val success = controller.moveCommit(commit, earlier)
            finish(success, activeBefore)
        }
    }

    fun runForm() {
        if (busy) return
        val successAllowed = when (mode) {
            HistoryRewriteMode.Onto -> upstream.isNotBlank() && onto.isNotBlank()
            HistoryRewriteMode.MoveRange -> range.isNotBlank() && onto.isNotBlank()
            HistoryRewriteMode.Squash -> range.isNotBlank()
            HistoryRewriteMode.Reorder -> false
        }
        if (!successAllowed) return
        scope.launch {
            running = true
            val activeBefore = controller.operation != null || controller.pendingNoCommit != null
            val success = when (mode) {
                HistoryRewriteMode.Onto ->
                    controller.rebaseOnto(upstream, onto, preserveMerges)
                HistoryRewriteMode.MoveRange ->
                    controller.moveRange(range, onto, preserveMerges)
                HistoryRewriteMode.Squash ->
                    controller.squashRange(range, message)
                HistoryRewriteMode.Reorder -> false
            }
            finish(success, activeBefore)
        }
    }

    fun selectMode(next: HistoryRewriteMode) {
        if (busy) return
        mode = next
        controller.dismissError()
    }

    DialogShell(
        title = "${strings.historyRewriteTitle} ${commit.shortHash}",
        width = 820.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Text(
            commit.subject,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.white,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            strings.historyRewriteHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermButton(
                "r",
                strings.historyRewriteModeReorder,
                active = mode == HistoryRewriteMode.Reorder,
            ) { selectMode(HistoryRewriteMode.Reorder) }
            Spacer(Modifier.width(5.dp))
            TermButton(
                "o",
                strings.historyRewriteModeOnto,
                active = mode == HistoryRewriteMode.Onto,
            ) { selectMode(HistoryRewriteMode.Onto) }
            Spacer(Modifier.width(5.dp))
            TermButton(
                "m",
                strings.historyRewriteModeMoveRange,
                active = mode == HistoryRewriteMode.MoveRange,
            ) { selectMode(HistoryRewriteMode.MoveRange) }
            Spacer(Modifier.width(5.dp))
            TermButton(
                "s",
                strings.historyRewriteModeSquash,
                active = mode == HistoryRewriteMode.Squash,
            ) { selectMode(HistoryRewriteMode.Squash) }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
        ) {
            when (mode) {
                HistoryRewriteMode.Reorder -> {
                    Text(
                        strings.historyRewriteReorderHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                    Spacer(Modifier.height(8.dp))
                    ReorderTarget(
                        title = strings.historyRewriteEarlier,
                        hint = strings.historyRewriteEarlierHint,
                        target = parentCommit?.subject ?: parent?.take(7) ?: strings.historyRewriteNoEarlier,
                        enabled = canEarlier,
                    )
                    Spacer(Modifier.height(6.dp))
                    ReorderTarget(
                        title = strings.historyRewriteLater,
                        hint = strings.historyRewriteLaterHint,
                        target = child?.subject ?: strings.historyRewriteNoLater,
                        enabled = canLater,
                    )
                    if (commit.isMerge || parentCommit?.isMerge == true || child?.isMerge == true) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            strings.reorderMergeUnsupported,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.red,
                        )
                    }
                    if (children.size > 1) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            strings.reorderAmbiguousChild,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.red,
                        )
                    }
                }

                HistoryRewriteMode.Onto -> {
                    FieldLabel(strings.historyRewriteUpstream)
                    TermTextField(
                        value = upstream,
                        onValueChange = { upstream = it },
                        placeholder = "A (exclusive)",
                    )
                    Spacer(Modifier.height(7.dp))
                    FieldLabel(strings.historyRewriteOnto)
                    TermTextField(
                        value = onto,
                        onValueChange = { onto = it },
                        placeholder = "new base",
                    )
                    Spacer(Modifier.height(7.dp))
                    PreserveMergesRow(preserveMerges, strings.historyRewritePreserveMerges) {
                        preserveMerges = !preserveMerges
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        strings.historyRewriteOntoHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }

                HistoryRewriteMode.MoveRange -> {
                    FieldLabel(strings.historyRewriteRange)
                    TermTextField(
                        value = range,
                        onValueChange = { range = it },
                        placeholder = strings.historyRewriteRangePlaceholder,
                    )
                    Text(
                        strings.historyRewriteRangeExcludes,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(7.dp))
                    FieldLabel(strings.historyRewriteOnto)
                    TermTextField(
                        value = onto,
                        onValueChange = { onto = it },
                        placeholder = "new base",
                    )
                    Spacer(Modifier.height(7.dp))
                    PreserveMergesRow(preserveMerges, strings.historyRewritePreserveMerges) {
                        preserveMerges = !preserveMerges
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        strings.historyRewriteMoveRangeHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }

                HistoryRewriteMode.Squash -> {
                    FieldLabel(strings.historyRewriteRange)
                    TermTextField(
                        value = range,
                        onValueChange = { range = it },
                        placeholder = strings.historyRewriteRangePlaceholder,
                    )
                    Text(
                        strings.historyRewriteRangeExcludes,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(7.dp))
                    FieldLabel(strings.historyRewriteMessage)
                    TermTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = strings.historyRewriteMessageHint,
                        singleLine = false,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        strings.historyRewriteSquashHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }
            }
        }

        Spacer(Modifier.height(9.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel("COMMAND")
        Spacer(Modifier.height(3.dp))
        Column(
            Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            val command = when (mode) {
                HistoryRewriteMode.Reorder ->
                    "git rebase -i --rebase-merges <base>  # swap adjacent commits"
                HistoryRewriteMode.Onto ->
                    "git rebase ${mergeFlag}--onto ${onto.ifBlank { "<onto>" }} " +
                        upstream.ifBlank { "<upstream>" }
                HistoryRewriteMode.MoveRange ->
                    "git rebase ${mergeFlag}--onto ${onto.ifBlank { "<onto>" }} " +
                        rangeStart.ifBlank { "<range-start>" }
                HistoryRewriteMode.Squash ->
                    "git rebase -i --rebase-merges ${rangeStart.ifBlank { "<range-start>" }}  # fixup range"
            }
            Text(
                "$ $command",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
            )
            if (mode != HistoryRewriteMode.Reorder) {
                Text(
                    "# ${strings.historyRewriteCurrentBranch}",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }

        controller.error?.let { messageText ->
            Spacer(Modifier.height(9.dp))
            ErrorBanner(messageText, controller::dismissError)
        }

        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(
                key = null,
                label = "CANCEL",
                enabled = !busy,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(8.dp))
            if (mode == HistoryRewriteMode.Reorder) {
                ActionButton(
                    key = null,
                    label = strings.historyRewriteEarlier,
                    enabled = canEarlier,
                    onClick = { runReorder(true) },
                )
                Spacer(Modifier.width(8.dp))
                ActionButton(
                    key = null,
                    label = strings.historyRewriteLater,
                    enabled = canLater,
                    onClick = { runReorder(false) },
                )
            } else {
                ActionButton(
                    key = null,
                    label = if (running) "WAIT…" else strings.historyRewriteRun,
                    primary = true,
                    enabled = !busy && when (mode) {
                        HistoryRewriteMode.Onto -> upstream.isNotBlank() && onto.isNotBlank()
                        HistoryRewriteMode.MoveRange -> range.isNotBlank() && onto.isNotBlank()
                        HistoryRewriteMode.Squash -> range.isNotBlank()
                        HistoryRewriteMode.Reorder -> false
                    },
                    onClick = { runForm() },
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.dim,
    )
}

@Composable
private fun PreserveMergesRow(active: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TermButton("p", label, active = active, onClick = onClick)
    }
}

@Composable
private fun ReorderTarget(
    title: String,
    hint: String,
    target: String,
    enabled: Boolean,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Term.panelAlt)
            .border(1.dp, if (enabled) Term.border else Term.borderHi)
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Term.green else Term.faint,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                target,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (enabled) Term.text else Term.faint,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            hint,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}
