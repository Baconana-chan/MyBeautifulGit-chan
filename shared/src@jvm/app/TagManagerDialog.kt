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
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.tagDeleteArgs
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

private enum class TagManagerMode {
    None,
    Create,
    Push,
    PushAll,
    DeleteLocal,
    DeleteRemote,
}

@Composable
fun TagManagerDialog(
    controller: RepoController,
    initialTag: GitRef?,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val tags = controller.refs.filter { it.kind == RefKind.Tag }
    var selectedName by remember(controller.repository.root) {
        mutableStateOf(initialTag?.name)
    }
    var mode by remember { mutableStateOf(TagManagerMode.None) }
    var running by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createMessage by remember { mutableStateOf("") }
    var sign by remember { mutableStateOf(false) }
    var remote by remember { mutableStateOf("") }
    val selected = tags.firstOrNull { it.name == selectedName } ?: tags.firstOrNull()
    val busy = running || controller.busy || controller.signatureVerificationLoading

    fun resetFields() {
        createName = ""
        createMessage = ""
        sign = false
        remote = controller.remotes.firstOrNull().orEmpty()
    }

    fun select(tag: GitRef) {
        if (selectedName != tag.name) controller.clearSignatureVerification()
        selectedName = tag.name
        mode = TagManagerMode.None
        resetFields()
    }

    fun openCreate() {
        mode = TagManagerMode.Create
        resetFields()
    }

    fun openPush() {
        mode = TagManagerMode.Push
        remote = controller.remotes.firstOrNull().orEmpty()
    }

    fun openPushAll() {
        mode = TagManagerMode.PushAll
        remote = controller.remotes.firstOrNull().orEmpty()
    }

    fun openDeleteLocal() {
        mode = TagManagerMode.DeleteLocal
    }

    fun openDeleteRemote() {
        mode = TagManagerMode.DeleteRemote
        remote = controller.remotes.firstOrNull().orEmpty()
    }

    fun execute(block: suspend () -> Boolean) {
        if (busy) return
        running = true
        scope.launch {
            val ok = block()
            running = false
            if (ok) {
                selectedName = when (mode) {
                    TagManagerMode.Create -> "refs/tags/${createName.trim()}"
                    TagManagerMode.DeleteLocal -> null
                    else -> selectedName
                }
                mode = TagManagerMode.None
                resetFields()
            }
        }
    }

    DialogShell(
        title = strings.tagManagerTitle,
        width = 760.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            Column(
                Modifier.width(290.dp).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(strings.tagManagerTitle)
                    Spacer(Modifier.weight(1f))
                    ActionButton(null, strings.tagCreate, enabled = !busy) { openCreate() }
                }
                if (tags.isEmpty()) {
                    TagManagerEmpty(strings.tagManagerNoTags)
                } else {
                    tags.forEach { tag ->
                        TagManagerRow(
                            tag = tag,
                            selected = selected?.name == tag.name,
                            onClick = { select(tag) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(6.dp))
                ActionButton(null, strings.tagPushAll, enabled = !busy) { openPushAll() }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (selected == null) {
                    Text(
                        strings.tagManagerSelect,
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
                        color = Term.amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${tagType(selected, strings.tagAnnotated, strings.tagLightweight)} · ${selected.target.take(7)}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionButton(null, strings.tagPush, enabled = !busy) { openPush() }
                        ActionButton(null, strings.tagDeleteLocal, enabled = !busy) { openDeleteLocal() }
                        ActionButton(null, strings.tagDeleteRemote, enabled = !busy) { openDeleteRemote() }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActionButton(null, strings.signatureVerifyTag, enabled = !busy) {
                            controller.clearSignatureVerification()
                            scope.launch { controller.verifyTagSignature(selected.shortName) }
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "git verify-tag ${selected.name}",
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (controller.signatureVerificationLoading) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.signatureVerificationRunning,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.amber,
                        )
                    }
                    controller.signatureVerification
                        ?.takeIf { it.target == selected.name }
                        ?.let { report ->
                            Spacer(Modifier.height(6.dp))
                            SignatureVerificationView(report, maxOutputLines = 24)
                        }
                }

                if (mode == TagManagerMode.None && selected == null) {
                    Spacer(Modifier.height(8.dp))
                    ActionButton(null, strings.tagCreate, enabled = !busy) { openCreate() }
                }

                when (mode) {
                    TagManagerMode.Create -> ManagerForm(
                        title = strings.tagCreate,
                        hint = strings.tagCreateHint,
                        commands = listOf(tagCreateCommand(createName, createMessage, sign)),
                        confirm = "CREATE",
                        enabled = !busy && createName.isNotBlank() && (!sign || createMessage.isNotBlank()),
                        onCancel = { if (!busy) mode = TagManagerMode.None },
                        onConfirm = {
                            execute {
                                controller.createTagAtHead(
                                    createName,
                                    createMessage,
                                    sign,
                                )
                            }
                        },
                    ) {
                        TermTextField(
                            value = createName,
                            onValueChange = { createName = it },
                            placeholder = strings.commitActionTagName,
                        )
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = createMessage,
                            onValueChange = { createMessage = it },
                            placeholder = strings.commitActionTagMessage,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TermButton("s", strings.tagSign, active = sign) { sign = !sign }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                strings.tagSignHint,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = if (sign && createMessage.isBlank()) Term.red else Term.faint,
                            )
                        }
                        if (sign && createMessage.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                strings.tagSignNeedsMessage,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }

                    TagManagerMode.Push -> if (selected != null) {
                        ManagerForm(
                            title = strings.tagPush,
                            hint = strings.tagPushHint,
                            commands = listOf(
                                "git push ${quoteArg(remote.trim())} " +
                                    quoteArg("refs/tags/${selected.shortName}:refs/tags/${selected.shortName}"),
                            ),
                            confirm = "PUSH",
                            enabled = !busy && remote.isNotBlank(),
                            onCancel = { if (!busy) mode = TagManagerMode.None },
                            onConfirm = {
                                execute { controller.pushTag(remote, selected.shortName) }
                            },
                        ) {
                            TermTextField(
                                value = remote,
                                onValueChange = { remote = it },
                                placeholder = strings.tagPushRemote,
                            )
                            Spacer(Modifier.height(5.dp))
                            TagRemoteChoices(controller.remotes, remote) { remote = it }
                        }
                    }

                    TagManagerMode.PushAll -> ManagerForm(
                        title = strings.tagPushAll,
                        hint = strings.tagPushAllHint,
                        commands = listOf("git push ${quoteArg(remote.trim())} --tags"),
                        confirm = "PUSH ALL",
                        enabled = !busy && remote.isNotBlank(),
                        onCancel = { if (!busy) mode = TagManagerMode.None },
                        onConfirm = { execute { controller.pushAllTags(remote) } },
                    ) {
                        TermTextField(
                            value = remote,
                            onValueChange = { remote = it },
                            placeholder = strings.tagPushRemote,
                        )
                        Spacer(Modifier.height(5.dp))
                        TagRemoteChoices(controller.remotes, remote) { remote = it }
                    }

                    TagManagerMode.DeleteLocal -> if (selected != null) {
                        ManagerForm(
                            title = strings.tagDeleteLocal,
                            hint = strings.tagDeleteLocalHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    tagDeleteArgs(selected.shortName),
                                ),
                            ),
                            confirm = "DELETE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = TagManagerMode.None },
                            onConfirm = { execute { controller.deleteLocalTag(selected.shortName) } },
                        ) { }
                    }

                    TagManagerMode.DeleteRemote -> if (selected != null) {
                        ManagerForm(
                            title = strings.tagDeleteRemote,
                            hint = strings.tagDeleteRemoteHint,
                            commands = listOf(
                                "git push ${quoteArg(remote.trim())} --delete " +
                                    quoteArg("refs/tags/${selected.shortName}"),
                            ),
                            confirm = "DELETE",
                            enabled = !busy && remote.isNotBlank(),
                            onCancel = { if (!busy) mode = TagManagerMode.None },
                            onConfirm = {
                                execute { controller.deleteRemoteTag(remote, selected.shortName) }
                            },
                        ) {
                            TermTextField(
                                value = remote,
                                onValueChange = { remote = it },
                                placeholder = strings.tagPushRemote,
                            )
                            Spacer(Modifier.height(5.dp))
                            TagRemoteChoices(controller.remotes, remote) { remote = it }
                        }
                    }

                    TagManagerMode.None -> Unit
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
private fun TagManagerRow(tag: GitRef, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(25.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber, modifier = Modifier.width(12.dp))
        Text(
            tag.shortName,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.green else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (tag.objectType == "tag") "A" else "L",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun TagManagerEmpty(text: String) {
    Text(
        "  ($text)",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun TagRemoteChoices(remotes: List<String>, selected: String, onSelect: (String) -> Unit) {
    if (remotes.isEmpty()) {
        Text(
            LocalStrings.current.tagNoRemotes,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        remotes.take(5).forEach { remote ->
            Text(
                "[$remote]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (remote == selected) Term.green else Term.faint,
                modifier = Modifier.clickable { onSelect(remote) },
            )
        }
    }
}

private fun tagType(tag: GitRef, annotated: String, lightweight: String): String =
    if (tag.objectType == "tag") annotated else lightweight

private fun tagCreateCommand(name: String, message: String, signed: Boolean): String {
    val cleanName = quoteArg(name.trim())
    return when {
        signed && message.isNotBlank() ->
            "git tag -s -m ${quoteArg(message)} $cleanName"
        signed -> "git tag -s <message> $cleanName"
        message.isNotBlank() -> "git tag -a -m ${quoteArg(message)} $cleanName"
        else -> "git tag $cleanName"
    }
}