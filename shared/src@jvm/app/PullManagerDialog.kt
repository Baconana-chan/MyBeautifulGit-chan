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
import org.baconana.mybeautifulgit_chan.domain.PullMode
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.pullArgs
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

@Composable
fun PullManagerDialog(
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val currentBranch = controller.refs.firstOrNull {
        it.kind == RefKind.LocalBranch && it.isHead
    }?.shortName.orEmpty()
    val upstreamPath = controller.status?.branch?.upstream?.removePrefix("refs/remotes/")
    val inferredRemote = controller.remotes
        .filter { upstreamPath?.startsWith("$it/") == true }
        .maxByOrNull { it.length }
    val inferredBranch = if (upstreamPath != null && inferredRemote != null) {
        upstreamPath.removePrefix("$inferredRemote/")
    } else {
        currentBranch
    }
    var remote by remember(controller.repository.root) {
        mutableStateOf(inferredRemote ?: controller.remotes.firstOrNull().orEmpty())
    }
    var branch by remember(controller.repository.root) { mutableStateOf(inferredBranch) }
    var mode by remember { mutableStateOf(PullMode.Merge) }
    var verifySignatures by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val remoteBranches = controller.remoteBranches(remote)
    val busy = running || controller.busy
    val command = "git " + pullArgs(remote, branch, mode, verifySignatures)
        .joinToString(" ") { quoteArg(it) }
    val enabled = !busy &&
        remote.isNotBlank() &&
        branch.isNotBlank() &&
        currentBranch.isNotBlank()

    fun selectRemote(value: String) {
        remote = value
        if (branch.isBlank()) branch = currentBranch
    }

    fun execute() {
        if (busy) return
        running = true
        scope.launch {
            val ok = controller.pull(remote, branch, mode, verifySignatures)
            running = false
            if (ok) onDismiss()
        }
    }

    DialogShell(
        title = strings.pullManagerTitle,
        width = 720.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            Column(
                Modifier.width(250.dp).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionLabel(strings.pullRemote)
                Spacer(Modifier.height(4.dp))
                TermTextField(
                    value = remote,
                    onValueChange = { selectRemote(it) },
                    placeholder = strings.pullRemote,
                )
                Spacer(Modifier.height(4.dp))
                if (controller.remotes.isEmpty()) {
                    Text(
                        strings.pullNoRemotes,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        controller.remotes.take(4).forEach { name ->
                            Text(
                                "[$name]",
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = if (name == remote) Term.green else Term.faint,
                                modifier = Modifier.clickable { selectRemote(name) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                SectionLabel(strings.pullBranch)
                Spacer(Modifier.height(4.dp))
                TermTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    placeholder = strings.pullBranch,
                )
                if (remoteBranches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        remoteBranches.take(6).forEach { remoteBranch ->
                            val name = remoteBranch.shortName.removePrefix("$remote/")
                            Text(
                                "[$name]",
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = if (name == branch) Term.green else Term.faint,
                                modifier = Modifier.clickable { branch = name },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                SectionLabel("MODE")
                Spacer(Modifier.height(3.dp))
                PullMode.entries.forEach { entry ->
                    PullModeRow(
                        label = pullModeLabel(strings, entry),
                        active = mode == entry,
                        enabled = !busy,
                    ) { mode = entry }
                }
                if (mode == PullMode.Merge) {
                    Spacer(Modifier.height(8.dp))
                    TermButton(
                        hotkey = "v",
                        label = strings.pullVerifySignatures,
                        active = verifySignatures,
                    ) {
                        if (!busy) verifySignatures = !verifySignatures
                    }
                    Text(
                        strings.pullVerifySignaturesHint,
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ManagerForm(
                    title = strings.pullManagerTitle,
                    hint = pullHint(strings, mode),
                    commands = listOf(command),
                    confirm = strings.pullConfirm,
                    enabled = enabled,
                    onCancel = { if (!busy) onDismiss() },
                    onConfirm = { execute() },
                ) {
                    Text(
                        "${strings.pullCurrentBranch}: ${currentBranch.ifBlank { "<detached HEAD>" }}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (currentBranch.isBlank()) Term.red else Term.cyan,
                    )
                    if (currentBranch.isBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            strings.pullNoCurrentBranch,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.red,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.pullLocalChangesHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }

                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
            }
        }
    }
}

private fun pullModeLabel(
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
    mode: PullMode,
): String = when (mode) {
    PullMode.Merge -> strings.pullModeMerge
    PullMode.Rebase -> strings.pullModeRebase
    PullMode.FfOnly -> strings.pullModeFfOnly
}

private fun pullHint(
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
    mode: PullMode,
): String = when (mode) {
    PullMode.Merge -> strings.pullHintMerge
    PullMode.Rebase -> strings.pullHintRebase
    PullMode.FfOnly -> strings.pullHintFfOnly
}

@Composable
private fun PullModeRow(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        "${if (active) "›" else " "} $label",
        fontSize = 9.sp,
        fontFamily = Term.mono,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        color = if (active) Term.green else Term.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().height(23.dp)
            .background(if (active) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 5.dp, vertical = 5.dp),
    )
}