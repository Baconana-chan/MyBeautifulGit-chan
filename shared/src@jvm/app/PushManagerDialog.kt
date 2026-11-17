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
import org.baconana.mybeautifulgit_chan.domain.PushForce
import org.baconana.mybeautifulgit_chan.domain.PushMode
import org.baconana.mybeautifulgit_chan.domain.PushOptions
import org.baconana.mybeautifulgit_chan.domain.pushArgs
import org.baconana.mybeautifulgit_chan.domain.validPushRefspec
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

@Composable
fun PushManagerDialog(
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val currentBranch = controller.refs.firstOrNull {
        it.kind == org.baconana.mybeautifulgit_chan.domain.RefKind.LocalBranch && it.isHead
    }?.shortName.orEmpty()
    var remote by remember(controller.repository.root) {
        mutableStateOf(controller.remotes.firstOrNull().orEmpty())
    }
    var mode by remember { mutableStateOf(PushMode.CurrentBranch) }
    var destination by remember(controller.repository.root) { mutableStateOf(currentBranch) }
    var deleteBranch by remember { mutableStateOf("") }
    var refspec by remember { mutableStateOf("") }
    var force by remember { mutableStateOf(PushForce.None) }
    var running by remember { mutableStateOf(false) }
    val remoteBranches = controller.remoteBranches(remote)
    val busy = running || controller.busy
    val options = PushOptions(
        mode = mode,
        source = currentBranch,
        destination = destination,
        branch = deleteBranch,
        refspec = refspec,
        force = if (mode == PushMode.DeleteBranch) PushForce.None else force,
    )
    val command = "git " + pushArgs(remote, options).joinToString(" ") { quoteArg(it) }
    val enabled = !busy && remote.isNotBlank() && when (mode) {
        PushMode.CurrentBranch -> currentBranch.isNotBlank()
        PushMode.AnotherBranch -> currentBranch.isNotBlank() && destination.isNotBlank()
        PushMode.DeleteBranch -> deleteBranch.isNotBlank()
        PushMode.Refspec -> validPushRefspec(refspec)
        PushMode.AllTags, PushMode.AllBranches, PushMode.Mirror -> true
    }

    fun selectRemote(value: String) {
        remote = value
        if (deleteBranch.isNotBlank() &&
            controller.remoteBranches(value).none {
                it.shortName.removePrefix("$value/") == deleteBranch
            }
        ) {
            deleteBranch = ""
        }
    }

    fun selectMode(value: PushMode) {
        mode = value
        if (value == PushMode.DeleteBranch && deleteBranch.isBlank()) {
            deleteBranch = controller.remoteBranches(remote)
                .firstOrNull()
                ?.shortName
                ?.removePrefix("$remote/")
                .orEmpty()
        }
    }

    fun execute() {
        if (busy) return
        running = true
        scope.launch {
            val ok = controller.push(remote, options)
            running = false
            if (ok) onDismiss()
        }
    }

    DialogShell(
        title = strings.pushManagerTitle,
        width = 760.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            Column(
                Modifier.width(240.dp).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionLabel(strings.pushRemote)
                Spacer(Modifier.height(4.dp))
                TermTextField(
                    value = remote,
                    onValueChange = { selectRemote(it) },
                    placeholder = strings.pushRemote,
                )
                Spacer(Modifier.height(4.dp))
                if (controller.remotes.isEmpty()) {
                    Text(
                        strings.pushNoRemotes,
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
                Spacer(Modifier.height(12.dp))
                SectionLabel("MODE")
                Spacer(Modifier.height(3.dp))
                pushModeEntries().forEach { entryMode ->
                    PushModeRow(
                        label = pushModeLabel(strings, entryMode),
                        active = mode == entryMode,
                        enabled = !busy,
                    ) { selectMode(entryMode) }
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ManagerForm(
                    title = strings.pushManagerTitle,
                    hint = pushHint(strings, mode),
                    commands = listOf(command),
                    confirm = strings.pushConfirm,
                    enabled = enabled,
                    onCancel = { if (!busy) onDismiss() },
                    onConfirm = { execute() },
                ) {
                    if (mode == PushMode.CurrentBranch || mode == PushMode.AnotherBranch) {
                        Text(
                            "source: ${currentBranch.ifBlank { "<detached HEAD>" }}",
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = if (currentBranch.isBlank()) Term.red else Term.cyan,
                        )
                        if (currentBranch.isBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                strings.pushNoCurrentBranch,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }
                    if (mode == PushMode.AnotherBranch) {
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            placeholder = strings.pushDestination,
                        )
                    }
                    if (mode == PushMode.DeleteBranch) {
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = deleteBranch,
                            onValueChange = { deleteBranch = it },
                            placeholder = strings.pushBranch,
                        )
                        if (remoteBranches.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                remoteBranches.take(6).forEach { branch ->
                                    Text(
                                        "[${branch.shortName.removePrefix("$remote/")}]",
                                        fontSize = 8.sp,
                                        fontFamily = Term.mono,
                                        color = if (branch.shortName.removePrefix("$remote/") == deleteBranch) {
                                            Term.green
                                        } else {
                                            Term.faint
                                        },
                                        modifier = Modifier.clickable {
                                            deleteBranch = branch.shortName.removePrefix("$remote/")
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (mode == PushMode.Refspec) {
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = refspec,
                            onValueChange = { refspec = it },
                            placeholder = strings.pushRefspecPlaceholder,
                        )
                        Text(
                            strings.pushHintRefspec,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = if (validPushRefspec(refspec)) Term.faint else Term.red,
                        )
                    }
                    if (mode != PushMode.DeleteBranch) {
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(strings.pushForce)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TermButton(
                                "f",
                                strings.pushForceNone,
                                active = force == PushForce.None,
                            ) { force = PushForce.None }
                            TermButton(
                                "f",
                                strings.pushForceLease,
                                active = force == PushForce.Lease,
                            ) { force = PushForce.Lease }
                            TermButton(
                                "f",
                                strings.pushForceFlag,
                                active = force == PushForce.Force,
                            ) { force = PushForce.Force }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (force) {
                                PushForce.None -> strings.pushForceNoneHint
                                PushForce.Lease -> strings.pushForceLeaseHint
                                PushForce.Force -> strings.pushForceHint
                            },
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = when (force) {
                                PushForce.None -> Term.faint
                                PushForce.Lease -> Term.amber
                                PushForce.Force -> Term.red
                            },
                        )
                    }
                    if (mode == PushMode.Mirror) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            strings.pushHintMirror,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.red,
                        )
                    }
                }

                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
            }
        }
    }
}

private fun pushModeEntries(): List<PushMode> = listOf(
    PushMode.CurrentBranch,
    PushMode.AnotherBranch,
    PushMode.AllTags,
    PushMode.AllBranches,
    PushMode.Mirror,
    PushMode.DeleteBranch,
    PushMode.Refspec,
)

private fun pushModeLabel(strings: org.baconana.mybeautifulgit_chan.i18n.Strings, mode: PushMode): String =
    when (mode) {
        PushMode.CurrentBranch -> strings.pushModeCurrent
        PushMode.AnotherBranch -> strings.pushModeAnother
        PushMode.AllTags -> strings.pushModeTags
        PushMode.AllBranches -> strings.pushModeAllBranches
        PushMode.Mirror -> strings.pushModeMirror
        PushMode.DeleteBranch -> strings.pushModeDelete
        PushMode.Refspec -> strings.pushModeRefspec
    }

private fun pushHint(strings: org.baconana.mybeautifulgit_chan.i18n.Strings, mode: PushMode): String =
    when (mode) {
        PushMode.CurrentBranch -> strings.pushHintCurrent
        PushMode.AnotherBranch -> strings.pushHintAnother
        PushMode.AllTags -> strings.pushHintTags
        PushMode.AllBranches -> strings.pushHintAllBranches
        PushMode.Mirror -> strings.pushHintMirror
        PushMode.DeleteBranch -> strings.pushHintDelete
        PushMode.Refspec -> strings.pushHintRefspec
    }

@Composable
private fun PushModeRow(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        "${if (active) "›" else " "} $label",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = if (active) Term.green else Term.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().height(22.dp)
            .background(if (active) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 5.dp, vertical = 5.dp),
    )
}