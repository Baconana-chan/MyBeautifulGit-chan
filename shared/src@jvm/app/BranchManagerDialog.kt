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
import org.baconana.mybeautifulgit_chan.domain.BranchCompareCommit
import org.baconana.mybeautifulgit_chan.domain.BranchCompareSide
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.branchDeleteArgs
import org.baconana.mybeautifulgit_chan.domain.branchRenameArgs
import org.baconana.mybeautifulgit_chan.domain.branchUpstreamArgs
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

private enum class BranchManagerMode {
    None,
    Create,
    RenameLocal,
    RenameRemote,
    DeleteLocal,
    DeleteRemote,
    Upstream,
    Push,
    Compare,
}

@Composable
fun BranchManagerDialog(
    controller: RepoController,
    initialRef: GitRef?,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var selectedName by remember(controller.repository.root) {
        mutableStateOf(initialRef?.name)
    }
    var mode by remember { mutableStateOf(BranchManagerMode.None) }
    var running by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var renameName by remember { mutableStateOf("") }
    var forceDelete by remember { mutableStateOf(false) }
    var upstreamName by remember { mutableStateOf("") }
    var pushRemote by remember { mutableStateOf("") }
    var pushRemoteBranch by remember { mutableStateOf("") }
    var compareFirst by remember { mutableStateOf("") }
    var compareSecond by remember { mutableStateOf("") }

    val locals = controller.refs.filter { it.kind == RefKind.LocalBranch }
    val remoteBranches = controller.refs.filter {
        it.kind == RefKind.RemoteBranch && !it.isSymbolic
    }
    val selected = controller.refs.firstOrNull { it.name == selectedName }
        ?: locals.firstOrNull { it.isHead }
        ?: locals.firstOrNull()
        ?: remoteBranches.firstOrNull()
    val selectedLocal = selected?.takeIf { it.kind == RefKind.LocalBranch }
    val selectedRemote = selected?.takeIf { it.kind == RefKind.RemoteBranch }
    val selectedRemoteName = selectedRemote?.let { remoteNameFor(it, controller.remotes) }
    val selectedRemoteBranch = if (selectedRemote != null && selectedRemoteName != null) {
        selectedRemote.name.removePrefix("refs/remotes/$selectedRemoteName/")
    } else {
        ""
    }
    val busy = running || controller.busy
    val currentBranch = locals.firstOrNull { it.isHead }?.shortName.orEmpty()

    fun resetFields() {
        createName = ""
        renameName = ""
        forceDelete = false
        upstreamName = ""
        pushRemote = controller.remotes.firstOrNull().orEmpty()
        pushRemoteBranch = ""
        compareFirst = ""
        compareSecond = ""
    }

    fun select(ref: GitRef) {
        selectedName = ref.name
        mode = BranchManagerMode.None
        resetFields()
        controller.clearBranchComparison()
    }

    fun openCreate() {
        mode = BranchManagerMode.Create
        createName = ""
        forceDelete = false
    }

    fun openRename() {
        if (selectedLocal == null) return
        mode = BranchManagerMode.RenameLocal
        renameName = selectedLocal.shortName
    }

    fun openRemoteRename() {
        if (selectedRemote == null || selectedRemoteName == null) return
        mode = BranchManagerMode.RenameRemote
        renameName = selectedRemoteBranch
    }

    fun openDelete() {
        if (selectedLocal == null) return
        mode = BranchManagerMode.DeleteLocal
        forceDelete = false
    }

    fun openRemoteDelete() {
        if (selectedRemote == null || selectedRemoteName == null) return
        mode = BranchManagerMode.DeleteRemote
    }

    fun openUpstream() {
        if (selectedLocal == null) return
        mode = BranchManagerMode.Upstream
        upstreamName = selectedLocal.upstream
            ?.removePrefix("refs/remotes/")
            ?: remoteBranches.firstOrNull {
                it.shortName.endsWith("/${selectedLocal.shortName}")
            }?.shortName
            ?: remoteBranches.firstOrNull()?.shortName.orEmpty()
    }

    fun openPush() {
        if (selectedLocal == null) return
        mode = BranchManagerMode.Push
        pushRemote = controller.remotes.firstOrNull().orEmpty()
        pushRemoteBranch = selectedLocal.shortName
    }

    fun openCompare() {
        mode = BranchManagerMode.Compare
        controller.clearBranchComparison()
        compareSecond = selected?.shortName.orEmpty()
        compareFirst = currentBranch.takeIf { it.isNotBlank() && it != compareSecond }
            ?: locals.firstOrNull { it.shortName != compareSecond }?.shortName
            ?: remoteBranches.firstOrNull { it.shortName != compareSecond }?.shortName
            ?: compareSecond
    }

    fun execute(block: suspend () -> Boolean) {
        if (busy) return
        running = true
        scope.launch {
            val ok = block()
            running = false
            if (ok) {
                selectedName = when (mode) {
                    BranchManagerMode.RenameLocal -> "refs/heads/${renameName.trim()}"
                    BranchManagerMode.RenameRemote -> if (selectedRemoteName != null) {
                        "refs/remotes/$selectedRemoteName/${renameName.trim()}"
                    } else {
                        selectedName
                    }
                    BranchManagerMode.Create -> "refs/heads/${createName.trim()}"
                    BranchManagerMode.DeleteLocal, BranchManagerMode.DeleteRemote -> null
                    else -> selectedName
                }
                mode = BranchManagerMode.None
                controller.clearBranchComparison()
                resetFields()
            }
        }
    }

    DialogShell(
        title = strings.branchManagerTitle,
        width = 900.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            Column(
                Modifier.width(300.dp).heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(strings.branchManagerLocal)
                    Spacer(Modifier.weight(1f))
                    ActionButton(null, strings.branchCreate, enabled = !busy) { openCreate() }
                }
                if (locals.isEmpty()) {
                    ManagerEmpty(strings.branchManagerNoBranches)
                } else {
                    locals.forEach { branch ->
                        ManagerBranchRow(
                            branch = branch,
                            selected = selected?.name == branch.name,
                            onClick = { select(branch) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Divider()
                Spacer(Modifier.height(6.dp))
                SectionLabel(strings.branchManagerRemote)
                if (remoteBranches.isEmpty()) {
                    ManagerEmpty(strings.branchManagerNoRemotes)
                } else {
                    remoteBranches.forEach { branch ->
                        ManagerBranchRow(
                            branch = branch,
                            selected = selected?.name == branch.name,
                            onClick = { select(branch) },
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (selected == null) {
                    Text(
                        strings.branchManagerSelect,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    Text(
                        selected.shortName,
                        fontSize = 11.sp,
                        fontFamily = Term.mono,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedLocal?.isHead == true) Term.green else Term.white,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedLocal?.upstream != null) {
                        Text(
                            "→ ${selectedLocal.upstream.removePrefix("refs/remotes/")}",
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.cyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedLocal != null) {
                            ActionButton(null, strings.branchRename, enabled = !busy) { openRename() }
                            ActionButton(null, strings.branchDelete, enabled = !busy) { openDelete() }
                            ActionButton(null, strings.branchSetUpstream, enabled = !busy) { openUpstream() }
                        }
                        if (selectedRemote != null && selectedRemoteName != null) {
                            ActionButton(null, strings.branchRename, enabled = !busy) { openRemoteRename() }
                            ActionButton(null, strings.branchDelete, enabled = !busy) { openRemoteDelete() }
                        }
                        ActionButton(null, strings.branchCompare, enabled = !busy) { openCompare() }
                    }
                    if (selectedLocal != null) {
                        Spacer(Modifier.height(6.dp))
                        ActionButton(null, strings.branchPush, enabled = !busy) { openPush() }
                    }
                }

                if (mode == BranchManagerMode.None && selected == null) {
                    Spacer(Modifier.height(8.dp))
                    ActionButton(null, strings.branchCreate, enabled = !busy) { openCreate() }
                }

                when (mode) {
                    BranchManagerMode.Create -> ManagerForm(
                        title = strings.branchCreate,
                        hint = strings.branchCreateHint,
                        commands = listOf("git branch ${quoteArg(createName.trim())}"),
                        confirm = "CREATE",
                        enabled = !busy && createName.isNotBlank(),
                        onCancel = { if (!busy) mode = BranchManagerMode.None },
                        onConfirm = { execute { controller.createBranchAtHead(createName) } },
                    ) {
                        TermTextField(
                            value = createName,
                            onValueChange = { createName = it },
                            placeholder = strings.branchNewName,
                        )
                    }

                    BranchManagerMode.RenameLocal -> if (selectedLocal != null) {
                        ManagerForm(
                            title = strings.branchRename,
                            hint = strings.branchRenameHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    branchRenameArgs(selectedLocal.shortName, renameName),
                                ),
                            ),
                            confirm = "RENAME",
                            enabled = !busy && renameName.isNotBlank(),
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = { execute { controller.renameBranch(selectedLocal.shortName, renameName) } },
                        ) {
                            TermTextField(
                                value = renameName,
                                onValueChange = { renameName = it },
                                placeholder = strings.branchNewName,
                            )
                        }
                    }

                    BranchManagerMode.RenameRemote -> if (selectedRemoteName != null) {
                        ManagerForm(
                            title = strings.branchRename,
                            hint = strings.branchRemoteRenameHint,
                            commands = listOf(
                                "git push ${quoteArg(selectedRemoteName)} " +
                                    "${quoteArg("${selectedRemote.name}:refs/heads/${renameName.trim()}")}",
                                "git push ${quoteArg(selectedRemoteName)} --delete ${quoteArg(selectedRemoteBranch)}",
                            ),
                            confirm = "RENAME",
                            enabled = !busy && renameName.isNotBlank(),
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = {
                                execute {
                                    controller.renameRemoteBranch(
                                        selectedRemoteName,
                                        selectedRemoteBranch,
                                        renameName,
                                    )
                                }
                            },
                        ) {
                            TermTextField(
                                value = renameName,
                                onValueChange = { renameName = it },
                                placeholder = strings.branchNewName,
                            )
                        }
                    }

                    BranchManagerMode.DeleteLocal -> if (selectedLocal != null) {
                        ManagerForm(
                            title = strings.branchDelete,
                            hint = strings.branchDeleteHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    branchDeleteArgs(selectedLocal.shortName, forceDelete),
                                ),
                            ),
                            confirm = "DELETE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = {
                                execute { controller.deleteLocalBranch(selectedLocal.shortName, forceDelete) }
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TermButton("s", strings.branchDelete, active = !forceDelete) {
                                    forceDelete = false
                                }
                                TermButton("f", strings.branchForceDelete, active = forceDelete) {
                                    forceDelete = true
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                if (forceDelete) strings.branchForceDeleteHint else strings.branchDeleteHint,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = if (forceDelete) Term.red else Term.amber,
                            )
                        }
                    }

                    BranchManagerMode.DeleteRemote -> if (selectedRemoteName != null) {
                        ManagerForm(
                            title = strings.branchDelete,
                            hint = strings.branchRemoteDeleteHint,
                            commands = listOf(
                                "git push ${quoteArg(selectedRemoteName)} --delete ${quoteArg(selectedRemoteBranch)}",
                            ),
                            confirm = "DELETE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = {
                                execute {
                                    controller.deleteRemoteBranch(selectedRemoteName, selectedRemoteBranch)
                                }
                            },
                        ) { }
                    }

                    BranchManagerMode.Upstream -> if (selectedLocal != null) {
                        ManagerForm(
                            title = strings.branchSetUpstream,
                            hint = strings.branchUpstreamHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    branchUpstreamArgs(selectedLocal.shortName, upstreamName),
                                ),
                            ),
                            confirm = "SET",
                            enabled = !busy && upstreamName.isNotBlank(),
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = {
                                execute { controller.setBranchUpstream(selectedLocal.shortName, upstreamName) }
                            },
                        ) {
                            TermTextField(
                                value = upstreamName,
                                onValueChange = { upstreamName = it },
                                placeholder = strings.branchUpstreamName,
                            )
                            RemoteChoices(remoteBranches) { upstreamName = it.shortName }
                        }
                    }

                    BranchManagerMode.Push -> if (selectedLocal != null) {
                        ManagerForm(
                            title = strings.branchPush,
                            hint = strings.branchPushHint,
                            commands = listOf(
                                "git push --set-upstream ${quoteArg(pushRemote.trim())} " +
                                    quoteArg(
                                        if (pushRemoteBranch.trim().ifBlank { selectedLocal.shortName } == selectedLocal.shortName) {
                                            selectedLocal.shortName
                                        } else {
                                            "${selectedLocal.shortName}:${pushRemoteBranch.trim()}"
                                        },
                                    ),
                            ),
                            confirm = "PUSH",
                            enabled = !busy && pushRemote.isNotBlank(),
                            onCancel = { if (!busy) mode = BranchManagerMode.None },
                            onConfirm = {
                                execute {
                                    controller.pushBranch(
                                        selectedLocal.shortName,
                                        pushRemote,
                                        pushRemoteBranch,
                                    )
                                }
                            },
                        ) {
                            TermTextField(
                                value = pushRemote,
                                onValueChange = { pushRemote = it },
                                placeholder = strings.branchPushRemote,
                            )
                            Spacer(Modifier.height(5.dp))
                            TermTextField(
                                value = pushRemoteBranch,
                                onValueChange = { pushRemoteBranch = it },
                                placeholder = strings.branchPushName,
                            )
                            if (controller.remotes.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    controller.remotes.take(4).forEach { remote ->
                                        Text(
                                            "[$remote]",
                                            fontSize = 8.sp,
                                            fontFamily = Term.mono,
                                            color = if (remote == pushRemote) Term.green else Term.faint,
                                            modifier = Modifier.clickable { pushRemote = remote },
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    strings.branchManagerNoRemotes,
                                    fontSize = 8.5.sp,
                                    fontFamily = Term.mono,
                                    color = Term.faint,
                                )
                            }
                        }
                    }

                    BranchManagerMode.Compare -> ManagerForm(
                        title = strings.branchCompare,
                        hint = strings.branchCompareHint,
                        commands = listOf(
                            "git rev-list --left-right --count " +
                                quoteArg("${compareFirst.trim()}...${compareSecond.trim()}"),
                            "git log --left-right --format=%H%x00%m%x00%s -n100 " +
                                quoteArg("${compareFirst.trim()}...${compareSecond.trim()}"),
                        ),
                        confirm = "COMPARE",
                        enabled = !busy && compareFirst.isNotBlank() && compareSecond.isNotBlank(),
                        onCancel = { if (!busy) mode = BranchManagerMode.None },
                        onConfirm = {
                            if (!busy) {
                                running = true
                                scope.launch {
                                    controller.compareBranches(compareFirst, compareSecond)
                                    running = false
                                }
                            }
                        },
                    ) {
                        TermTextField(
                            value = compareFirst,
                            onValueChange = { compareFirst = it },
                            placeholder = strings.branchCompareFirst,
                        )
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = compareSecond,
                            onValueChange = { compareSecond = it },
                            placeholder = strings.branchCompareSecond,
                        )
                        RemoteChoices(locals + remoteBranches) { ref -> compareFirst = ref.shortName }
                        RemoteChoices(locals + remoteBranches) { ref -> compareSecond = ref.shortName }
                        BranchCompareResult(controller)
                    }

                    BranchManagerMode.None -> Unit
                }

                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
                controller.branchCompareError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissBranchCompareError)
                }
            }
        }
    }
}

@Composable
private fun ManagerBranchRow(
    branch: GitRef,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (branch.isHead) "*" else if (branch.kind == RefKind.RemoteBranch) "↳" else " ",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (branch.isHead) Term.green else Term.dim,
            modifier = Modifier.width(12.dp),
        )
        Text(
            branch.shortName,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (selected || branch.isHead) Term.green else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (branch.ahead > 0) {
            Text("↑${branch.ahead}", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.amber)
        }
        if (branch.behind > 0) {
            Spacer(Modifier.width(3.dp))
            Text("↓${branch.behind}", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.cyan)
        }
    }
}

@Composable
private fun ManagerEmpty(text: String) {
    Text(
        "  ($text)",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun RemoteChoices(refs: List<GitRef>, onSelect: (GitRef) -> Unit) {
    if (refs.isEmpty()) return
    Spacer(Modifier.height(5.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        refs.take(5).forEach { ref ->
            Text(
                "[${ref.shortName}]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clickable { onSelect(ref) },
            )
        }
    }
}

@Composable
private fun BranchCompareResult(controller: RepoController) {
    val strings = LocalStrings.current
    if (controller.branchCompareLoading) {
        Spacer(Modifier.height(8.dp))
        Text(strings.branchCompareLoading, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        return
    }
    val result = controller.branchComparison ?: return
    Spacer(Modifier.height(8.dp))
    SectionLabel("COMPARE")
    Spacer(Modifier.height(4.dp))
    Text(
        "${result.first}  ↑${result.ahead}  ↓${result.behind}  ${result.second}",
        fontSize = 9.sp,
        fontFamily = Term.mono,
        color = Term.green,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        "${strings.branchCompareFirstOnly}: ${result.ahead} · " +
            "${strings.branchCompareSecondOnly}: ${result.behind}",
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = Term.faint,
    )
    if (result.commits.isEmpty()) {
        Text(strings.branchCompareEmpty, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        return
    }
    Spacer(Modifier.height(4.dp))
    result.commits.take(12).forEach { commit ->
        BranchCompareCommitRow(commit)
    }
    if (result.commits.size > 12) {
        Text(
            "+ ${result.commits.size - 12} more",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun BranchCompareCommitRow(commit: BranchCompareCommit) {
    val mark = if (commit.side == BranchCompareSide.First) "<" else ">"
    Text(
        "$mark ${commit.shortHash} ${commit.subject}",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = if (commit.side == BranchCompareSide.First) Term.amber else Term.cyan,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ManagerForm(
    title: String,
    hint: String,
    commands: List<String>,
    confirm: String,
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(12.dp))
    Divider()
    Spacer(Modifier.height(8.dp))
    Text(title, fontSize = 10.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.green)
    Spacer(Modifier.height(3.dp))
    Text(hint, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.amber)
    Spacer(Modifier.height(7.dp))
    content()
    Spacer(Modifier.height(8.dp))
    SectionLabel("COMMAND")
    Spacer(Modifier.height(3.dp))
    Column(
        Modifier.fillMaxWidth().background(Term.black).padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        commands.forEach { command ->
            Text("$ $command", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.greenDim)
        }
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton(null, "CANCEL", onClick = onCancel)
        Spacer(Modifier.width(6.dp))
        ActionButton(null, confirm, enabled = enabled, onClick = onConfirm)
    }
}

private fun remoteNameFor(ref: GitRef, remotes: List<String>): String? {
    return remotes
        .filter { ref.name.startsWith("refs/remotes/$it/") }
        .maxByOrNull { it.length }
}