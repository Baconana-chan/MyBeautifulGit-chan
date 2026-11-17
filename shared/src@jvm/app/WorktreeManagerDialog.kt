package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Worktree
import org.baconana.mybeautifulgit_chan.domain.WorktreeAddOptions
import org.baconana.mybeautifulgit_chan.domain.validWorktreePath
import org.baconana.mybeautifulgit_chan.domain.worktreeAddArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeLockArgs
import org.baconana.mybeautifulgit_chan.domain.worktreePruneArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeRemoveArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeSwitchArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeUnlockArgs
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import java.io.File

private enum class WorktreeManagerMode {
    None,
    Add,
    Branch,
    Lock,
    Remove,
    Prune,
}

@Composable
fun WorktreeManagerDialog(
    controller: RepoController,
    initialPath: String?,
    onOpen: (String) -> Unit,
    onOpenWindow: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val worktrees = controller.worktrees
    var selectedPath by remember(controller.repository.root) {
        mutableStateOf(
            initialPath?.takeIf { path -> worktrees.any { samePath(it.path, path) } },
        )
    }
    var mode by remember { mutableStateOf(WorktreeManagerMode.None) }
    var running by remember { mutableStateOf(false) }
    var path by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var base by remember { mutableStateOf("") }
    var createBranch by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }
    var force by remember { mutableStateOf(false) }
    var prunePreview by remember { mutableStateOf<String?>(null) }
    var pruneLoading by remember { mutableStateOf(false) }
    val busy = running || controller.busy || pruneLoading
    val selected = worktrees.firstOrNull { samePath(it.path, selectedPath.orEmpty()) }
    val current = selected != null && samePath(selected.path, controller.repository.root)

    LaunchedEffect(worktrees, selectedPath) {
        val currentPath = selectedPath
        if (currentPath != null && worktrees.none { samePath(it.path, currentPath) }) {
            selectedPath = null
        }
    }

    fun resetFields() {
        path = ""
        branch = ""
        base = ""
        createBranch = true
        reason = ""
        force = false
    }

    fun select(next: String?) {
        if (busy) return
        selectedPath = next
        mode = WorktreeManagerMode.None
        resetFields()
        prunePreview = null
    }

    fun openMode(next: WorktreeManagerMode) {
        if (busy) return
        mode = next
        resetFields()
        if (next == WorktreeManagerMode.Prune) {
            prunePreview = null
            pruneLoading = true
            scope.launch {
                prunePreview = controller.previewWorktreePrune()
                pruneLoading = false
            }
        }
    }

    fun execute(
        block: suspend () -> Boolean,
        after: () -> Unit = {},
    ) {
        if (busy) return
        running = true
        scope.launch {
            val ok = block()
            running = false
            if (ok) {
                after()
                mode = WorktreeManagerMode.None
                resetFields()
            }
        }
    }

    fun command(args: List<String>): String =
        "git " + args.joinToString(" ") { quoteArg(it) }

    val targetPath = selected?.path
    val targetLabel = targetPath ?: strings.worktreeSelect
    val lockArgs = if (targetPath == null) {
        emptyList()
    } else if (selected.isLocked) {
        worktreeUnlockArgs(targetPath)
    } else {
        worktreeLockArgs(targetPath, reason)
    }
    val branchArgs = targetPath?.let {
        worktreeSwitchArgs(it, branch, createBranch, base)
    }.orEmpty()
    val removeArgs = targetPath?.let { worktreeRemoveArgs(it, force) }.orEmpty()
    val addArgs = worktreeAddArgs(
        WorktreeAddOptions(
            path = path,
            branch = branch,
            base = base,
            createBranch = createBranch,
        ),
    )

    DialogShell(
        title = strings.worktreeManagerTitle,
        width = 920.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 570.dp)) {
            Column(
                Modifier.width(320.dp).heightIn(max = 570.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(strings.worktreeManagerTitle)
                    Spacer(Modifier.weight(1f))
                    ActionButton(null, strings.worktreeAdd, enabled = !busy) {
                        openMode(WorktreeManagerMode.Add)
                    }
                    Spacer(Modifier.width(5.dp))
                    ActionButton(null, strings.worktreePrune, enabled = !busy) {
                        openMode(WorktreeManagerMode.Prune)
                    }
                }

                if (worktrees.isEmpty()) {
                    Text(
                        "  ${strings.worktreeNoWorktrees}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    worktrees.forEach { worktree ->
                        WorktreeManagerRow(
                            worktree = worktree,
                            strings = strings,
                            selected = selected != null && samePath(selected.path, worktree.path),
                            current = samePath(worktree.path, controller.repository.root),
                            enabled = !busy,
                            onClick = { select(worktree.path) },
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 570.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    targetLabel,
                    fontSize = 11.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == null) Term.faint else Term.amber,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                selected?.let { worktree ->
                    val state = buildList {
                        if (current) add(strings.worktreeCurrent)
                        if (worktree.isLocked) add(strings.worktreeLocked)
                        if (worktree.isPrunable) add(strings.worktreePrunable)
                        if (worktree.isBare) add(strings.worktreeBare)
                        if (worktree.isDetached) add(strings.worktreeDetached)
                    }.joinToString(" · ")
                    Text(
                        "${worktree.branchName ?: "—"} · ${worktree.head?.take(7) ?: "—"}"
                            + if (state.isNotEmpty()) " · $state" else "",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (worktree.isPrunable) Term.red else Term.dim,
                    )
                    worktree.lockReason?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(
                        null,
                        strings.worktreeOpen,
                        enabled = !busy && selected != null,
                    ) {
                        selected?.let { onOpen(it.path) }
                    }
                    ActionButton(
                        null,
                        strings.worktreeOpenWindow,
                        enabled = !busy && selected != null,
                    ) {
                        selected?.let { onOpenWindow(it.path) }
                    }
                    ActionButton(
                        null,
                        strings.worktreeBranchAction,
                        enabled = !busy && selected != null && !selected.isBare,
                    ) {
                        openMode(WorktreeManagerMode.Branch)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(
                        null,
                        if (selected?.isLocked == true) strings.worktreeUnlock else strings.worktreeLock,
                        enabled = !busy && selected != null,
                    ) {
                        openMode(WorktreeManagerMode.Lock)
                    }
                    ActionButton(
                        null,
                        strings.worktreeRemove,
                        enabled = !busy && selected != null && !current && !selected.isBare,
                    ) {
                        openMode(WorktreeManagerMode.Remove)
                    }
                }

                if (mode == WorktreeManagerMode.None) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selected == null) strings.worktreeSelect else strings.worktreeBranchHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }

                when (mode) {
                    WorktreeManagerMode.Add -> WorktreeForm(
                        strings = strings,
                        title = strings.worktreeAdd,
                        hint = strings.worktreeAddHint,
                        commands = listOf(command(addArgs)),
                        confirm = strings.worktreeAdd,
                        enabled = !busy &&
                            validWorktreePath(path) &&
                            (!createBranch || branch.isNotBlank()),
                        onCancel = { if (!busy) mode = WorktreeManagerMode.None },
                        onConfirm = {
                            execute(
                                block = {
                                    controller.addWorktree(
                                        WorktreeAddOptions(
                                            path = path,
                                            branch = branch,
                                            base = base,
                                            createBranch = createBranch,
                                        ),
                                    )
                                },
                                after = { selectedPath = path.trim() },
                            )
                        },
                    ) {
                        WorktreeField(strings.worktreePath, path, { path = it }, strings.worktreePathPlaceholder)
                        Spacer(Modifier.height(6.dp))
                        WorktreeField(strings.worktreeBranch, branch, { branch = it }, strings.worktreeBranchPlaceholder)
                        if (createBranch || branch.isBlank()) {
                            Spacer(Modifier.height(6.dp))
                            WorktreeField(strings.worktreeBase, base, { base = it }, strings.worktreeBasePlaceholder)
                        }
                        Spacer(Modifier.height(6.dp))
                        TermButton(
                            hotkey = "b",
                            label = strings.worktreeCreateBranch,
                            active = createBranch,
                        ) { createBranch = !createBranch }
                    }

                    WorktreeManagerMode.Branch -> if (selected != null) {
                        WorktreeForm(
                            strings = strings,
                            title = strings.worktreeBranchAction,
                            hint = strings.worktreeBranchHint,
                            commands = listOf(command(branchArgs)),
                            confirm = strings.worktreeBranchAction,
                            enabled = !busy && branch.isNotBlank(),
                            onCancel = { if (!busy) mode = WorktreeManagerMode.None },
                            onConfirm = {
                                execute(
                                    block = {
                                        controller.branchInWorktree(
                                            path = selected.path,
                                            branch = branch,
                                            create = createBranch,
                                            base = base,
                                        )
                                    },
                                )
                            },
                        ) {
                            Text(
                                selected.path,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            WorktreeField(strings.worktreeBranch, branch, { branch = it }, strings.worktreeBranchPlaceholder)
                            if (createBranch) {
                                Spacer(Modifier.height(6.dp))
                                WorktreeField(strings.worktreeBase, base, { base = it }, strings.worktreeBasePlaceholder)
                            }
                            Spacer(Modifier.height(6.dp))
                            TermButton(
                                hotkey = "b",
                                label = strings.worktreeCreateBranch,
                                active = createBranch,
                            ) { createBranch = !createBranch }
                        }
                    }

                    WorktreeManagerMode.Lock -> if (selected != null) {
                        WorktreeForm(
                            strings = strings,
                            title = if (selected.isLocked) strings.worktreeUnlock else strings.worktreeLock,
                            hint = strings.worktreeLockHint,
                            commands = listOf(command(lockArgs)),
                            confirm = if (selected.isLocked) strings.worktreeUnlock else strings.worktreeLock,
                            enabled = !busy,
                            onCancel = { if (!busy) mode = WorktreeManagerMode.None },
                            onConfirm = {
                                execute(
                                    block = {
                                        if (selected.isLocked) {
                                            controller.unlockWorktree(selected.path)
                                        } else {
                                            controller.lockWorktree(selected.path, reason)
                                        }
                                    },
                                )
                            },
                        ) {
                            Text(
                                selected.path,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!selected.isLocked) {
                                Spacer(Modifier.height(6.dp))
                                WorktreeField(
                                    strings.worktreeLockReason,
                                    reason,
                                    { reason = it },
                                    strings.worktreeReasonPlaceholder,
                                )
                            }
                        }
                    }

                    WorktreeManagerMode.Remove -> if (selected != null) {
                        WorktreeForm(
                            strings = strings,
                            title = strings.worktreeRemove,
                            hint = strings.worktreeRemoveHint,
                            commands = listOf(command(removeArgs)),
                            confirm = strings.worktreeRemove,
                            enabled = !busy && !current && !selected.isBare,
                            onCancel = { if (!busy) mode = WorktreeManagerMode.None },
                            onConfirm = {
                                execute(
                                    block = { controller.removeWorktree(selected.path, force) },
                                    after = { selectedPath = null },
                                )
                            },
                        ) {
                            Text(
                                selected.path,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            TermButton(
                                hotkey = "f",
                                label = strings.worktreeForce,
                                active = force,
                            ) { force = !force }
                        }
                    }

                    WorktreeManagerMode.Prune -> WorktreeForm(
                        strings = strings,
                        title = strings.worktreePrune,
                        hint = strings.worktreePruneHint,
                        commands = listOf(command(worktreePruneArgs(dryRun = false))),
                        confirm = strings.worktreePruneConfirm,
                        enabled = !busy,
                        onCancel = { if (!busy) mode = WorktreeManagerMode.None },
                        onConfirm = {
                            execute(
                                block = {
                                    controller.pruneWorktrees()
                                },
                            )
                        },
                    ) {
                        SectionLabel(strings.worktreePrunePreview)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (pruneLoading) "…" else (prunePreview ?: strings.worktreePruneNothing),
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = if (pruneLoading) Term.faint else Term.text,
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    WorktreeManagerMode.None -> Unit
                }

                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
            }
        }
    }
}

@Composable
private fun WorktreeManagerRow(
    worktree: Worktree,
    strings: Strings,
    selected: Boolean,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = when {
        worktree.isBare -> strings.worktreeBare
        worktree.branchName != null -> worktree.branchName.orEmpty()
        worktree.isDetached -> strings.worktreeDetached
        else -> "—"
    }
    Row(
        Modifier.fillMaxWidth().height(33.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (current) "*" else "↳",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (current) Term.green else Term.dim,
            modifier = Modifier.width(12.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = if (selected) Term.green else Term.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (worktree.isLocked) {
                    Spacer(Modifier.width(4.dp))
                    Text("[locked]", fontSize = 7.sp, fontFamily = Term.mono, color = Term.amber)
                }
                if (worktree.isPrunable) {
                    Spacer(Modifier.width(4.dp))
                    Text("[prunable]", fontSize = 7.sp, fontFamily = Term.mono, color = Term.red)
                }
            }
            Text(
                worktree.path,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WorktreeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Text(
        label,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = Term.dim,
    )
    Spacer(Modifier.height(3.dp))
    TermTextField(value = value, onValueChange = onValueChange, placeholder = placeholder)
}

@Composable
private fun WorktreeForm(
    strings: Strings,
    title: String,
    hint: String,
    commands: List<String>,
    confirm: String,
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Divider()
        Spacer(Modifier.height(7.dp))
        Text(
            title,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.amber,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            hint,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.height(7.dp))
        content()
        Spacer(Modifier.height(7.dp))
        commands.forEach { command ->
            Text(
                command,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(null, confirm, enabled = enabled, primary = true, onClick = onConfirm)
            ActionButton(null, strings.worktreeCancel, onClick = onCancel)
        }
    }
}

private fun samePath(first: String, second: String): Boolean {
    if (second.isBlank()) return false
    val a = runCatching { File(first).canonicalFile }.getOrElse { File(first).absoluteFile.normalize() }
    val b = runCatching { File(second).canonicalFile }.getOrElse { File(second).absoluteFile.normalize() }
    return a.path.equals(
        b.path,
        ignoreCase = System.getProperty("os.name").orEmpty().lowercase().contains("win"),
    )
}
