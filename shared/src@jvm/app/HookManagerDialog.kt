package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.HookEntry
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun HookManagerDialog(controller: RepoController, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val hooks = controller.hooks
    var filter by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var pendingSelection by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var loadedContent by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var toggling by remember { mutableStateOf(false) }
    var pathBusy by remember { mutableStateOf(false) }
    var pathDraft by remember { mutableStateOf(hooks.configuredPath) }
    var notice by remember { mutableStateOf<String?>(null) }
    var externalConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var closeConfirm by remember { mutableStateOf(false) }

    val filtered = remember(hooks.entries, filter) {
        val query = filter.trim().lowercase()
        if (query.isEmpty()) hooks.entries else hooks.entries.filter {
            it.name.lowercase().contains(query)
        }
    }
    val selectedEntry = hooks.entries.firstOrNull { it.name == selectedName }
    val dirty = loadedContent != null && draft != loadedContent

    LaunchedEffect(hooks.entries, selectedName) {
        if (selectedName == null || hooks.entries.none { it.name == selectedName }) {
            selectedName = hooks.entries.firstOrNull { it.enabled }?.name
                ?: hooks.entries.firstOrNull()?.name
        }
    }
    LaunchedEffect(selectedName) {
        loadedContent = null
        draft = ""
        pendingSelection = null
        val name = selectedName
        if (name != null) controller.loadHookDetail(name)
    }
    LaunchedEffect(controller.hookDetail, selectedName) {
        val detail = controller.hookDetail
        if (detail != null && detail.name == selectedName) {
            draft = detail.content
            loadedContent = detail.content
        }
    }
    LaunchedEffect(hooks.configuredPath) {
        pathDraft = hooks.configuredPath
    }

    fun select(name: String) {
        if (name == selectedName) return
        if (dirty) {
            pendingSelection = name
        } else {
            selectedName = name
            notice = null
        }
    }

    fun save(allowExternal: Boolean = false) {
        val name = selectedName ?: return
        scope.launch {
            saving = true
            try {
                if (controller.saveHook(name, draft, allowExternal)) {
                    loadedContent = draft
                    notice = strings.hookSaved
                    closeConfirm = false
                }
            } finally {
                saving = false
            }
        }
    }

    fun requestDismiss() {
        if (dirty) {
            closeConfirm = true
        } else {
            onDismiss()
        }
    }

    fun requestSave() {
        if (hooks.outsideHooksDir) {
            externalConfirm = { save(allowExternal = true) }
        } else {
            save()
        }
    }

    fun toggle(allowExternal: Boolean = false) {
        val entry = selectedEntry ?: return
        scope.launch {
            toggling = true
            try {
                if (controller.setHookEnabled(entry.name, !entry.enabled, allowExternal)) {
                    notice = if (entry.enabled) strings.hookDisabled else strings.hookEnabled
                }
            } finally {
                toggling = false
            }
        }
    }

    fun requestToggle() {
        if (hooks.outsideHooksDir) {
            externalConfirm = { toggle(allowExternal = true) }
        } else {
            toggle()
        }
    }

    fun applyPath(clear: Boolean, allowExternal: Boolean = false) {
        scope.launch {
            pathBusy = true
            try {
                val target = if (clear) "" else pathDraft
                if (controller.setHooksPath(target, allowExternal)) {
                    if (clear) pathDraft = ""
                    notice = if (target.isBlank()) {
                        strings.toastHooksPathCleared
                    } else {
                        strings.toastHooksPathSet
                    }
                }
            } finally {
                pathBusy = false
            }
        }
    }

    fun requestApplyPath(clear: Boolean) {
        val target = if (clear) "" else pathDraft
        if (target.isNotBlank() && controller.hooksPathRequiresConfirmation(target)) {
            externalConfirm = { applyPath(clear, allowExternal = true) }
        } else {
            applyPath(clear)
        }
    }

    LaunchedEffect(hooks.directory) {
        externalConfirm = null
    }

    DialogShell(title = strings.hookManagerTitle, onDismiss = ::requestDismiss, width = 980.dp) {
        Column(Modifier.fillMaxWidth().height(640.dp)) {
            if (closeConfirm) {
                Row(
                    Modifier.fillMaxWidth().background(Term.amber.copy(alpha = 0.12f))
                        .border(1.dp, Term.amber.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.hookUnsaved,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        key = null,
                        label = strings.hookDiscard,
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(5.dp))
                    ActionButton(
                        key = null,
                        label = strings.hookCancel,
                        onClick = { closeConfirm = false },
                    )
                }
                Spacer(Modifier.height(7.dp))
            }
            externalConfirm?.let { action ->
                Row(
                    Modifier.fillMaxWidth().background(Term.amber.copy(alpha = 0.12f))
                        .border(1.dp, Term.amber.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.hookPathConfirmationRequired,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        key = null,
                        label = strings.hookExternalConfirm,
                        onClick = {
                            externalConfirm = null
                            action()
                        },
                    )
                    Spacer(Modifier.width(5.dp))
                    ActionButton(
                        key = null,
                        label = strings.hookCancel,
                        onClick = { externalConfirm = null },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            controller.error?.let { message ->
                ErrorBanner(message, onDismiss = { controller.dismissError() })
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(290.dp).fillMaxHeight()) {
                    Row(
                        Modifier.fillMaxWidth().height(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(strings.hookManagerTitle)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${hooks.entries.count { it.enabled }}/${hooks.entries.size}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                    TermTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = strings.hookFilterPlaceholder,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (filtered.isEmpty()) {
                            Text(
                                if (hooks.entries.isEmpty()) strings.hookNoHooks else strings.hookSelect,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.faint,
                                modifier = Modifier.padding(8.dp),
                            )
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(filtered, key = { it.name }) { entry ->
                                    HookRow(
                                        entry = entry,
                                        selected = entry.name == selectedName,
                                        onClick = { select(entry.name) },
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        SectionLabel(strings.hookPath)
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = pathDraft,
                            onValueChange = {
                                pathDraft = it
                                externalConfirm = null
                            },
                            placeholder = strings.hookPathPlaceholder,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (hooks.outsideCommonDir ||
                            (pathDraft.isNotBlank() && controller.isExternalHooksPath(pathDraft))
                        ) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                strings.hookPathExternalWarning,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ActionButton(
                                key = null,
                                label = strings.hookPathSet,
                                enabled = !pathBusy && !controller.busy,
                                onClick = { requestApplyPath(clear = false) },
                            )
                            ActionButton(
                                key = null,
                                label = strings.hookPathClear,
                                enabled = !pathBusy && !controller.busy,
                                onClick = { requestApplyPath(clear = true) },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.hookPathEffective,
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                        Text(
                            hooks.directory.ifBlank { "—" },
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Term.border))
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp)) {
                    if (pendingSelection != null) {
                        Row(
                            Modifier.fillMaxWidth().background(Term.amber.copy(alpha = 0.12f))
                                .border(1.dp, Term.amber.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                strings.hookUnsaved,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                                modifier = Modifier.weight(1f),
                            )
                            ActionButton(
                                key = null,
                                label = strings.hookDiscard,
                                onClick = {
                                    selectedName = pendingSelection
                                    pendingSelection = null
                                    notice = null
                                },
                            )
                            Spacer(Modifier.width(5.dp))
                            ActionButton(
                                key = null,
                                label = strings.hookCancel,
                                onClick = { pendingSelection = null },
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                    if (selectedEntry == null) {
                        Text(
                            strings.hookSelect,
                            fontSize = 9.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                selectedEntry.name,
                                fontSize = 12.sp,
                                fontFamily = Term.mono,
                                fontWeight = FontWeight.Bold,
                                color = Term.white,
                            )
                            Spacer(Modifier.width(8.dp))
                            HookStatus(selectedEntry)
                            Spacer(Modifier.weight(1f))
                            ActionButton(
                                key = null,
                                label = if (selectedEntry.enabled) {
                                    strings.hookDisable
                                } else {
                                    strings.hookEnable
                                },
                                enabled = !toggling && !saving && !controller.busy,
                                onClick = { requestToggle() },
                            )
                            Spacer(Modifier.width(5.dp))
                            ActionButton(
                                key = null,
                                label = strings.hookReload,
                                enabled = !toggling && !saving && !dirty,
                                onClick = {
                                    scope.launch { controller.loadHookDetail(selectedEntry.name) }
                                },
                            )
                            Spacer(Modifier.width(5.dp))
                            ActionButton(
                                key = null,
                                label = strings.hookSave,
                                primary = true,
                                enabled = dirty && !toggling && !saving && !controller.busy,
                                onClick = { requestSave() },
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            selectedEntry.path,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (dirty) {
                            Text(
                                strings.hookUnsaved,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                            )
                        }
                        if (controller.hookDetail?.truncated == true) {
                            Text(
                                strings.hookTruncated,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                            )
                        }
                        controller.hookDetailError?.let { message ->
                            Spacer(Modifier.height(5.dp))
                            ErrorBanner(
                                message,
                                onDismiss = { controller.dismissHookDetailError() },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        TermTextArea(
                            value = draft,
                            onValueChange = {
                                draft = it
                                notice = null
                            },
                            placeholder = strings.hookEditorPlaceholder,
                            modifier = Modifier.weight(0.58f).fillMaxWidth(),
                        )
                    }
                    HookOutputPanel(
                        controller = controller,
                        modifier = Modifier.weight(0.35f).fillMaxWidth(),
                    )
                    notice?.let { message ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            message,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.green,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HookRow(entry: HookEntry, selected: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) Term.raised else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when {
                entry.enabled -> "[*]"
                entry.disabled -> "[ ]"
                else -> "[.]"
            },
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = when {
                entry.enabled -> Term.green
                entry.disabled -> Term.amber
                else -> Term.faint
            },
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = if (entry.enabled) Term.text else Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    entry.enabled -> strings.hookEnabled
                    entry.disabled -> strings.hookDisabled
                    else -> strings.hookSample
                },
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = when {
                    entry.enabled -> Term.greenDim
                    entry.disabled -> Term.amber
                    else -> Term.faint
                },
            )
        }
        Text(
            "${entry.size}b",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun HookStatus(entry: HookEntry) {
    val strings = LocalStrings.current
    val (label, color) = when {
        entry.enabled -> strings.hookEnabled to Term.green
        entry.disabled -> strings.hookDisabled to Term.amber
        else -> strings.hookSample to Term.faint
    }
    Text(
        "[$label]",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = color,
    )
}

@Composable
private fun HookOutputPanel(controller: RepoController, modifier: Modifier) {
    val strings = LocalStrings.current
    val output = controller.hookOutput
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(strings.hookOutput)
            Spacer(Modifier.weight(1f))
            if (!output?.text.isNullOrBlank()) {
                ActionButton(
                    key = null,
                    label = strings.hookCopy,
                    onClick = { DesktopActions.copy(output.text) },
                )
                Spacer(Modifier.width(5.dp))
            }
            ActionButton(
                key = null,
                label = strings.hookOutputClear,
                enabled = output != null,
                onClick = { controller.clearHookOutput() },
            )
        }
        Text(
            strings.hookOutputHint,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        if (output == null) {
            Text(
                strings.hookOutputEmpty,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Spacer(Modifier.height(3.dp))
            Text(
                "${output.command}  [${strings.hookExit} ${output.exitCode ?: "?"}]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (output.success) Term.greenDim else Term.red,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Term.black)
                    .border(1.dp, Term.border).padding(7.dp),
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        output.text.ifBlank { strings.hookOutputEmpty },
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (output.text.isBlank()) Term.faint else Term.text,
                    )
                }
            }
        }
    }
}
