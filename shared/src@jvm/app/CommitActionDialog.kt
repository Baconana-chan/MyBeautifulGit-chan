package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.ResetPreview
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

enum class CommitAction {
    Checkout,
    Reset,
    InteractiveRebase,
    Delete,
    HistoryRewrite,
    FilterRepo,
    CherryPick,
    Revert,
    Branch,
    Tag,
    CopyHash,
    Reflog,
    BisectGood,
    BisectBad,
}

data class CommitActionRequest(
    val action: CommitAction,
    val commit: Commit,
    val initialResetMode: ResetMode = ResetMode.Mixed,
    val initialRefName: String = "",
)

@Composable
fun CommitActionDialog(
    request: CommitActionRequest,
    controller: RepoController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var resetMode by remember(request) { mutableStateOf(request.initialResetMode) }
    var refName by remember(request) { mutableStateOf(request.initialRefName) }
    var tagMessage by remember { mutableStateOf("") }
    var tagSigned by remember { mutableStateOf(false) }
    var rangeMode by remember(request) { mutableStateOf(false) }
    var range by remember(request) { mutableStateOf("") }
    var noCommit by remember(request) { mutableStateOf(false) }
    val busy = running || controller.busy
    val action = request.action
    val commit = request.commit
    val supportsRange = action == CommitAction.CherryPick || action == CommitAction.Revert
    val rangeDisplay = quoteArg(range.trim().ifBlank { "<range>" })
    var preview by remember(request) { mutableStateOf<ResetPreview?>(null) }
    var previewLoading by remember(request) { mutableStateOf(false) }

    LaunchedEffect(action, commit.hash) {
        if (action != CommitAction.Reset) return@LaunchedEffect
        previewLoading = true
        try {
            preview = controller.resetPreview(commit.hash)
        } finally {
            previewLoading = false
        }
    }

    val commands = buildList {
        if ((action == CommitAction.BisectGood || action == CommitAction.BisectBad) &&
            controller.operation?.kind != GitOperationKind.Bisect
        ) {
            add("git bisect start")
        }
        when (action) {
            CommitAction.Checkout -> add("git checkout ${commit.hash}")
            CommitAction.Reset -> add("git reset ${resetMode.flag} ${commit.hash}")
            CommitAction.InteractiveRebase, CommitAction.Delete, CommitAction.HistoryRewrite,
            CommitAction.FilterRepo -> Unit
            CommitAction.CherryPick -> when {
                rangeMode && noCommit -> {
                    add("git rev-list --reverse $rangeDisplay")
                    add("git cherry-pick --no-commit <commit>  # each commit")
                }
                rangeMode -> add("git cherry-pick $rangeDisplay")
                noCommit -> add("git cherry-pick --no-commit ${commit.hash}")
                else -> add("git cherry-pick ${commit.hash}")
            }
            CommitAction.Revert -> when {
                rangeMode && noCommit -> {
                    add("git rev-list --reverse $rangeDisplay")
                    add("git revert --no-commit <commit>  # each commit")
                }
                rangeMode -> add("git revert --no-edit $rangeDisplay")
                noCommit -> add("git revert --no-commit ${commit.hash}")
                else -> add("git revert --no-edit ${commit.hash}")
            }
            CommitAction.Branch -> add("git branch ${displayRefName(refName)} ${commit.hash}")
            CommitAction.Tag -> when {
                tagSigned && tagMessage.isNotBlank() -> add(
                    "git tag -s -m ${quoteArg(tagMessage)} ${displayRefName(refName)} ${commit.hash}",
                )
                tagSigned -> add("git tag -s <message> ${displayRefName(refName)} ${commit.hash}")
                tagMessage.isBlank() -> add("git tag ${displayRefName(refName)} ${commit.hash}")
                else -> add(
                    "git tag -a -m ${quoteArg(tagMessage)} ${displayRefName(refName)} ${commit.hash}",
                )
            }
            CommitAction.BisectGood -> add("git bisect good ${commit.hash}")
            CommitAction.BisectBad -> add("git bisect bad ${commit.hash}")
            CommitAction.CopyHash, CommitAction.Reflog -> Unit
        }
    }

    fun run() {
        if (busy) return
        scope.launch {
            running = true
            val activeBefore = controller.operation != null || controller.pendingNoCommit != null
            val success = when (action) {
                CommitAction.Checkout -> controller.checkoutCommit(commit.hash)
                CommitAction.Reset -> controller.resetCommit(commit.hash, resetMode)
                CommitAction.InteractiveRebase, CommitAction.Delete, CommitAction.HistoryRewrite,
                CommitAction.FilterRepo -> false
                CommitAction.CherryPick -> if (rangeMode) {
                    controller.cherryPickRange(range, noCommit)
                } else {
                    controller.cherryPickCommit(commit.hash, noCommit)
                }
                CommitAction.Revert -> if (rangeMode) {
                    controller.revertRange(range, noCommit)
                } else {
                    controller.revertCommit(commit.hash, noCommit)
                }
                CommitAction.Branch -> controller.createBranch(refName, commit.hash)
                CommitAction.Tag -> controller.createTag(refName, tagMessage, commit.hash, tagSigned)
                CommitAction.BisectGood -> controller.markBisect(commit.hash, good = true)
                CommitAction.BisectBad -> controller.markBisect(commit.hash, good = false)
                CommitAction.CopyHash, CommitAction.Reflog -> false
            }
            running = false
            // Конфликт — это не ошибка ввода: операция уже началась, поэтому
            // закрываем диалог и показываем operation banner + merge editor.
            val startedNow = !activeBefore && (
                controller.operation != null || controller.pendingNoCommit != null
                )
            if (success || startedNow) {
                onDismiss()
                onSuccess()
            }
        }
    }

    DialogShell(
        title = "${actionTitle(action)} ${commit.shortHash}",
        width = 660.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Text(
            commit.subject,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.white,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (action) {
                CommitAction.Checkout -> strings.commitActionCheckoutHint
                CommitAction.Reset -> strings.commitActionResetHint
                CommitAction.InteractiveRebase -> ""
                CommitAction.Delete -> strings.commitActionDeleteHint
                CommitAction.HistoryRewrite -> strings.commitActionHistoryRewriteHint
                CommitAction.FilterRepo -> strings.commitActionFilterRepoHint
                CommitAction.CherryPick -> strings.commitActionCherryPickHint
                CommitAction.Revert -> strings.commitActionRevertHint
                CommitAction.Branch -> strings.commitActionBranchHint
                CommitAction.Tag -> strings.commitActionTagHint
                CommitAction.BisectGood, CommitAction.BisectBad -> strings.commitActionBisectHint
                CommitAction.CopyHash, CommitAction.Reflog -> ""
            },
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )

        if (action == CommitAction.Reset) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "mode",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                    modifier = Modifier.width(56.dp),
                )
                TermButton("s", "--soft", active = resetMode == ResetMode.Soft) {
                    resetMode = ResetMode.Soft
                }
                Spacer(Modifier.width(5.dp))
                TermButton("m", "--mixed", active = resetMode == ResetMode.Mixed) {
                    resetMode = ResetMode.Mixed
                }
                Spacer(Modifier.width(5.dp))
                TermButton("h", "--hard", active = resetMode == ResetMode.Hard) {
                    resetMode = ResetMode.Hard
                }
                Spacer(Modifier.width(5.dp))
                TermButton("k", "--keep", active = resetMode == ResetMode.Keep) {
                    resetMode = ResetMode.Keep
                }
            }
            if (resetMode == ResetMode.Hard) {
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.commitActionResetHardHint,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.red,
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel(strings.commitActionResetPreview)
            Spacer(Modifier.height(4.dp))
            when {
                previewLoading -> Text(
                    strings.commitActionResetPreviewLoading,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                preview != null -> ResetPreviewView(
                    preview = preview!!,
                    mode = resetMode,
                    strings = strings,
                )
            }
        }

        if (supportsRange) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "mode",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                    modifier = Modifier.width(56.dp),
                )
                TermButton("r", strings.commitActionRange, active = rangeMode) {
                    rangeMode = !rangeMode
                }
                Spacer(Modifier.width(5.dp))
                TermButton("n", strings.commitActionNoCommit, active = noCommit) {
                    noCommit = !noCommit
                }
            }
            if (rangeMode) {
                Spacer(Modifier.height(6.dp))
                TermTextField(
                    value = range,
                    onValueChange = { range = it },
                    placeholder = strings.commitActionRangePlaceholder,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.commitActionRangeHint,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
            if (noCommit) {
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.commitActionNoCommitHint,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.amber,
                )
            }
        }

        if (action == CommitAction.Branch || action == CommitAction.Tag) {
            Spacer(Modifier.height(10.dp))
            SectionLabel(if (action == CommitAction.Branch) "BRANCH" else "TAG")
            Spacer(Modifier.height(4.dp))
            TermTextField(
                value = refName,
                onValueChange = { refName = it },
                placeholder = if (action == CommitAction.Branch) {
                    strings.commitActionBranchName
                } else {
                    strings.commitActionTagName
                },
            )
            if (action == CommitAction.Tag) {
                Spacer(Modifier.height(6.dp))
                TermTextField(
                    value = tagMessage,
                    onValueChange = { tagMessage = it },
                    placeholder = strings.commitActionTagMessage,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TermButton("s", strings.tagSign, active = tagSigned) {
                        tagSigned = !tagSigned
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        strings.tagSignHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = if (tagSigned && tagMessage.isBlank()) Term.red else Term.faint,
                    )
                }
                if (tagSigned && tagMessage.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.tagSignNeedsMessage,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.red,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(8.dp))
        SectionLabel("COMMAND")
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            commands.forEach { command ->
                Text(
                    "$ $command",
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.greenDim,
                )
            }
        }

        controller.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            ErrorBanner(message, controller::dismissError)
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
            ActionButton(
                key = null,
                label = when {
                    running -> "WAIT…"
                    else -> confirmLabel(action)
                },
                danger = action == CommitAction.Reset && resetMode == ResetMode.Hard,
                enabled = !busy &&
                    (!rangeMode || range.isNotBlank()) && (
                        (action != CommitAction.Branch && action != CommitAction.Tag) ||
                            (refName.isNotBlank() && !(action == CommitAction.Tag && tagSigned && tagMessage.isBlank()))
                        ),
                onClick = { run() },
            )
        }
    }
}

private fun actionTitle(action: CommitAction): String = when (action) {
    CommitAction.Checkout -> "CHECKOUT"
    CommitAction.Reset -> "RESET"
    CommitAction.InteractiveRebase -> "REBASE"
    CommitAction.Delete -> "DELETE"
    CommitAction.HistoryRewrite -> "HISTORY REWRITE"
    CommitAction.FilterRepo -> "FILTER-REPO"
    CommitAction.CherryPick -> "CHERRY-PICK"
    CommitAction.Revert -> "REVERT"
    CommitAction.Branch -> "BRANCH"
    CommitAction.Tag -> "TAG"
    CommitAction.BisectGood -> "BISECT GOOD"
    CommitAction.BisectBad -> "BISECT BAD"
    CommitAction.CopyHash -> "COPY HASH"
    CommitAction.Reflog -> "REFLOG"
}

private fun confirmLabel(action: CommitAction): String = when (action) {
    CommitAction.Checkout -> "CHECKOUT"
    CommitAction.Reset -> "RESET"
    CommitAction.InteractiveRebase -> "REBASE"
    CommitAction.Delete -> "DELETE"
    CommitAction.HistoryRewrite -> "HISTORY REWRITE"
    CommitAction.FilterRepo -> "FILTER-REPO"
    CommitAction.CherryPick -> "CHERRY-PICK"
    CommitAction.Revert -> "REVERT"
    CommitAction.Branch -> "CREATE BRANCH"
    CommitAction.Tag -> "CREATE TAG"
    CommitAction.BisectGood -> "MARK GOOD"
    CommitAction.BisectBad -> "MARK BAD"
    CommitAction.CopyHash, CommitAction.Reflog -> "OK"
}

private fun displayRefName(name: String): String = quoteArg(name.trim().ifBlank { "<name>" })

@Composable
private fun ResetPreviewView(
    preview: ResetPreview,
    mode: ResetMode,
    strings: Strings,
) {
    val modeText = when (mode) {
        ResetMode.Soft -> strings.commitActionResetSoftPreview
        ResetMode.Mixed -> strings.commitActionResetMixedPreview
        ResetMode.Hard -> strings.commitActionResetHardPreview
        ResetMode.Keep -> if (preview.keepConflicts.isEmpty()) {
            strings.commitActionResetKeepPreview
        } else {
            strings.commitActionResetKeepBlocked
        }
    }
    Text(
        modeText,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = when {
            mode == ResetMode.Keep && preview.keepConflicts.isNotEmpty() -> Term.red
            mode == ResetMode.Hard && preview.hasLocalChanges -> Term.red
            else -> Term.amber
        },
    )

    Spacer(Modifier.height(4.dp))
    UnreachablePlanDetails(preview.unreachable, strings)

    val paths = preview.pathsFor(mode)
    val counts = preview.countsFor(mode, strings)
    if (counts.isNotEmpty()) {
        Spacer(Modifier.height(3.dp))
        Text(
            counts.joinToString(" · "),
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
        )
    }
    paths.take(6).forEach { path ->
        Text(
            "  · $path",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (paths.size > 6) {
        Text(
            "  +${paths.size - 6} ${strings.commitActionResetMore}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }

    if (mode == ResetMode.Hard && preview.untrackedPaths.isNotEmpty()) {
        Spacer(Modifier.height(3.dp))
        Text(
            "${preview.untrackedPaths.size} ${strings.commitActionResetUntracked} · " +
                strings.commitActionResetUntrackedPreview,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

private val ResetPreview.hasLocalChanges: Boolean
    get() = stagedPaths.isNotEmpty() ||
        unstagedPaths.isNotEmpty() ||
        conflictedPaths.isNotEmpty()

private fun ResetPreview.pathsFor(mode: ResetMode): List<String> = when (mode) {
    ResetMode.Soft -> emptyList()
    ResetMode.Mixed -> stagedPaths.distinct()
    ResetMode.Hard -> (stagedPaths + unstagedPaths + conflictedPaths).distinct()
    ResetMode.Keep -> keepConflicts
}

private fun ResetPreview.countsFor(mode: ResetMode, strings: Strings): List<String> = buildList {
    when (mode) {
        ResetMode.Soft -> Unit
        ResetMode.Mixed -> if (stagedPaths.isNotEmpty()) {
            add("${stagedPaths.size} ${strings.commitActionResetStaged}")
        }
        ResetMode.Hard -> {
            if (stagedPaths.isNotEmpty()) add("${stagedPaths.size} ${strings.commitActionResetStaged}")
            if (unstagedPaths.isNotEmpty()) add("${unstagedPaths.size} ${strings.commitActionResetUnstaged}")
            if (conflictedPaths.isNotEmpty()) add("${conflictedPaths.size} ${strings.commitActionResetConflicted}")
        }
        ResetMode.Keep -> if (keepConflicts.isNotEmpty()) {
            add("${keepConflicts.size} ${strings.commitActionResetOverlapping}")
        }
    }
}
