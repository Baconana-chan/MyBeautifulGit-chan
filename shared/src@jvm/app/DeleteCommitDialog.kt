package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
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
import org.baconana.mybeautifulgit_chan.domain.ResetPreview
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

private enum class DeleteCommitMethod { Drop, Reset, Revert, FilterRepo }

@Composable
fun DeleteCommitDialog(
    commit: Commit,
    controller: RepoController,
    onDismiss: () -> Unit,
    onDropInRebase: (Commit) -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var method by remember(commit.hash) {
        mutableStateOf(
            if (commit.isMerge) DeleteCommitMethod.Reset else DeleteCommitMethod.Drop,
        )
    }
    var running by remember { mutableStateOf(false) }
    var filterAvailable by remember(commit.hash) { mutableStateOf<Boolean?>(null) }
    var filterConfirmed by remember(commit.hash) { mutableStateOf(false) }
    var preview by remember(commit.hash) { mutableStateOf<ResetPreview?>(null) }
    var previewLoading by remember(commit.hash) { mutableStateOf(false) }
    val parent = commit.parents.firstOrNull()
    val busy = running || controller.busy
    val dirty = controller.status?.isClean == false
    val filterReady = filterAvailable == true && !dirty && controller.stashCount == 0

    LaunchedEffect(commit.hash) {
        filterAvailable = controller.filterRepoAvailable()
    }

    LaunchedEffect(commit.hash, method) {
        if (method != DeleteCommitMethod.Reset || parent == null) {
            preview = null
            previewLoading = false
            return@LaunchedEffect
        }
        previewLoading = true
        try {
            preview = controller.resetPreview(parent)
        } finally {
            previewLoading = false
        }
    }

    fun run() {
        if (busy) return
        if (method == DeleteCommitMethod.Drop) {
            if (commit.isMerge) return
            onDropInRebase(commit)
            onDismiss()
            return
        }
        if (method == DeleteCommitMethod.Reset && parent == null) return
        if (method == DeleteCommitMethod.Revert && commit.isMerge) return
        if (method == DeleteCommitMethod.FilterRepo && (!filterReady || !filterConfirmed)) return
        scope.launch {
            running = true
            val activeBefore = controller.operation != null || controller.pendingNoCommit != null
            val success = when (method) {
                DeleteCommitMethod.Reset -> controller.deleteCommitByReset(commit)
                DeleteCommitMethod.Revert -> controller.revertCommit(commit.hash)
                DeleteCommitMethod.FilterRepo -> if (filterConfirmed) {
                    controller.deleteCommitWithFilterRepo(commit)
                } else {
                    false
                }
                DeleteCommitMethod.Drop -> false
            }
            running = false
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
        title = "${strings.deleteCommitTitle} ${commit.shortHash}",
        width = 860.dp,
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
            strings.deleteCommitHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(10.dp))

        Column(
            Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
        ) {
            DeleteMethodCard(
                marker = "1",
                title = strings.deleteCommitDropTitle,
                hint = strings.deleteCommitDropHint,
                command = "git rebase -i --rebase-merges " +
                    (parent?.take(7) ?: "--root"),
                selected = method == DeleteCommitMethod.Drop,
                enabled = !busy && !commit.isMerge,
                warning = if (commit.isMerge) strings.deleteCommitDropMergeWarning else null,
            ) { method = DeleteCommitMethod.Drop }

            Spacer(Modifier.height(7.dp))
            DeleteMethodCard(
                marker = "2",
                title = strings.deleteCommitResetTitle,
                hint = strings.deleteCommitResetHint,
                command = "git reset --hard ${parent?.take(7) ?: "<root has no parent>"}",
                selected = method == DeleteCommitMethod.Reset,
                enabled = !busy && parent != null,
                warning = when {
                    parent == null -> strings.deleteCommitRootNoParent
                    commit.isMerge -> strings.deleteCommitResetMergeWarning
                    else -> null
                },
            ) { method = DeleteCommitMethod.Reset }

            Spacer(Modifier.height(7.dp))
            DeleteMethodCard(
                marker = "3",
                title = strings.deleteCommitRevertTitle,
                hint = strings.deleteCommitRevertHint,
                command = "git revert --no-edit ${commit.hash}",
                selected = method == DeleteCommitMethod.Revert,
                enabled = !busy && !commit.isMerge,
                warning = if (commit.isMerge) strings.deleteCommitMergeRevert else null,
            ) { method = DeleteCommitMethod.Revert }

            Spacer(Modifier.height(7.dp))
            DeleteMethodCard(
                marker = "4",
                title = strings.deleteCommitFilterTitle,
                hint = strings.deleteCommitFilterHint,
                command = "git filter-repo --force --commit-callback " +
                    "'if commit.original_id == b\"${commit.hash}\": commit.skip()'",
                selected = method == DeleteCommitMethod.FilterRepo,
                // Карточку можно выбрать и без установленного filter-repo:
                // иначе пользователь не увидит инструкцию по установке.
                enabled = !busy,
                warning = when {
                    filterAvailable == null -> null
                    filterAvailable == false -> strings.deleteCommitFilterMissing
                    dirty -> strings.deleteCommitDirty
                    controller.stashCount > 0 -> strings.deleteCommitStashes
                    else -> null
                },
            ) { method = DeleteCommitMethod.FilterRepo }

            if (method == DeleteCommitMethod.Reset && parent != null) {
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
                    preview != null -> UnreachablePlanDetails(preview!!.unreachable, strings)
                }
            }

            if (method == DeleteCommitMethod.FilterRepo) {
                Spacer(Modifier.height(8.dp))
                if (filterAvailable == false) {
                    Text(
                        strings.deleteCommitFilterMissing,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.red,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        strings.deleteCommitFilterInstall,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                } else {
                    Text(
                        strings.deleteCommitBackupNote,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        strings.deleteCommitFilterBackup,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(6.dp))
                    TermButton(
                        "!",
                        strings.deleteCommitFilterConfirm,
                        active = filterConfirmed,
                    ) {
                        filterConfirmed = !filterConfirmed
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Divider()
        controller.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message, controller::dismissError)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(
                key = null,
                label = strings.commitRewriteCancel,
                enabled = !busy,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = when {
                    running -> "WAIT…"
                    method == DeleteCommitMethod.Drop -> strings.deleteCommitDropAction
                    method == DeleteCommitMethod.Reset -> strings.deleteCommitResetAction
                    method == DeleteCommitMethod.Revert -> strings.deleteCommitRevertAction
                    else -> strings.deleteCommitFilterAction
                },
                danger = method != DeleteCommitMethod.Drop,
                enabled = !busy && when (method) {
                    DeleteCommitMethod.Drop -> !commit.isMerge
                    DeleteCommitMethod.Reset -> parent != null
                    DeleteCommitMethod.Revert -> !commit.isMerge
                    DeleteCommitMethod.FilterRepo -> filterReady && filterConfirmed
                },
                onClick = { run() },
            )
        }
    }
}

@Composable
private fun DeleteMethodCard(
    marker: String,
    title: String,
    hint: String,
    command: String,
    selected: Boolean,
    enabled: Boolean,
    warning: String? = null,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) Term.panelAlt else Term.panel)
            .border(1.dp, if (selected) Term.green else Term.border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selected) "[$marker]" else " $marker ",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (selected) Term.green else Term.faint,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                title,
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Term.white else Term.faint,
            )
            if (!enabled) {
                Spacer(Modifier.width(7.dp))
                Text(
                    "N/A",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.red,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            hint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (enabled) Term.text else Term.faint,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "$ $command",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = if (enabled) Term.greenDim else Term.faint,
        )
        warning?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.red,
            )
        }
    }
}
