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
import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.SubmoduleState
import org.baconana.mybeautifulgit_chan.domain.SubmoduleUpdateOptions
import org.baconana.mybeautifulgit_chan.domain.submoduleAddArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleDeinitArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleInitArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleRemoveArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleSyncArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleUpdateArgs
import org.baconana.mybeautifulgit_chan.domain.validSubmodulePath
import org.baconana.mybeautifulgit_chan.domain.validSubmoduleUrl
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

private enum class SubmoduleManagerMode {
    None,
    Init,
    Update,
    Sync,
    Add,
    Remove,
}

@Composable
fun SubmoduleManagerDialog(
    controller: RepoController,
    initialPath: String?,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val submodules = controller.submodules
    var selectedPath by remember(controller.repository.root) {
        mutableStateOf(initialPath?.takeIf { value -> submodules.any { it.path == value } })
    }
    var mode by remember { mutableStateOf(SubmoduleManagerMode.None) }
    var running by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var recursive by remember { mutableStateOf(true) }
    var initialize by remember { mutableStateOf(true) }
    var checkout by remember { mutableStateOf(true) }
    var customUpdateConfirmed by remember { mutableStateOf(false) }
    val busy = running || controller.busy

    LaunchedEffect(submodules, selectedPath) {
        if (selectedPath != null && submodules.none { it.path == selectedPath }) {
            selectedPath = null
        }
    }

    val selected = submodules.firstOrNull { it.path == selectedPath }
    val all = selected == null
    val targetPath = selected?.path

    LaunchedEffect(targetPath) {
        customUpdateConfirmed = false
    }
    val targetLabel = targetPath ?: strings.submoduleAll
    val removeAvailable = selected != null && !busy

    fun resetFields() {
        url = ""
        path = ""
        recursive = true
        initialize = true
        checkout = true
        customUpdateConfirmed = false
    }

    fun open(next: SubmoduleManagerMode) {
        if (busy) return
        mode = next
        resetFields()
        if (next == SubmoduleManagerMode.Remove) {
            path = selected?.path.orEmpty()
        }
    }

    fun select(next: String?) {
        if (busy) return
        selectedPath = next
        mode = SubmoduleManagerMode.None
        resetFields()
    }

    fun execute(block: suspend () -> Boolean) {
        if (busy) return
        running = true
        scope.launch {
            val ok = block()
            running = false
            if (ok) {
                when (mode) {
                    SubmoduleManagerMode.Add -> selectedPath = path.trim()
                    SubmoduleManagerMode.Remove -> selectedPath = null
                    else -> Unit
                }
                mode = SubmoduleManagerMode.None
                resetFields()
            }
        }
    }

    fun command(args: List<String>): String = displayCommandLine("git", args)

    val addCommand = command(submoduleAddArgs(url, path))
    val initCommand = command(submoduleInitArgs(targetPath))
    val updateOptions = SubmoduleUpdateOptions(
        init = initialize,
        recursive = recursive,
        checkout = checkout,
    )
    val updateCommand = command(submoduleUpdateArgs(targetPath, updateOptions))
    val syncCommand = command(submoduleSyncArgs(targetPath, recursive))
    val removeCommands = targetPath?.let {
        listOf(
            command(submoduleDeinitArgs(it)),
            command(submoduleRemoveArgs(it)),
        )
    }.orEmpty()

    DialogShell(
        title = strings.submoduleManagerTitle,
        width = 820.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            Column(
                Modifier.width(300.dp).heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(strings.submoduleManagerTitle)
                    Spacer(Modifier.weight(1f))
                    ActionButton(null, strings.submoduleAdd, enabled = !busy) {
                        open(SubmoduleManagerMode.Add)
                    }
                }

                SubmoduleAllRow(
                    strings = strings,
                    selected = all,
                    count = submodules.size,
                    enabled = !busy,
                    onClick = { select(null) },
                )
                if (submodules.isEmpty()) {
                    Text(
                        "  (${strings.submoduleManagerNoSubmodules})",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    submodules.forEach { submodule ->
                        SubmoduleManagerRow(
                            submodule = submodule,
                            strings = strings,
                            selected = selected?.path == submodule.path,
                            enabled = !busy,
                            onClick = { select(submodule.path) },
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    targetLabel,
                    fontSize = 11.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = if (all) Term.faint else Term.amber,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                selected?.let { submodule ->
                    Text(
                        "${stateLabel(submodule.state, strings)} · ${submodule.head?.take(7) ?: "—"}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = stateColor(submodule.state),
                    )
                    submodule.describe?.let { describe ->
                        Text(
                            describe,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(null, strings.submoduleInit, enabled = !busy) {
                        open(SubmoduleManagerMode.Init)
                    }
                    ActionButton(null, strings.submoduleUpdate, enabled = !busy) {
                        open(SubmoduleManagerMode.Update)
                    }
                    ActionButton(null, strings.submoduleSync, enabled = !busy) {
                        open(SubmoduleManagerMode.Sync)
                    }
                }
                Spacer(Modifier.height(5.dp))
                ActionButton(null, strings.submoduleRemove, enabled = removeAvailable) {
                    open(SubmoduleManagerMode.Remove)
                }

                if (mode == SubmoduleManagerMode.None) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (all) strings.submoduleManagerSelect else strings.submoduleUpdateHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }

                when (mode) {
                    SubmoduleManagerMode.Init -> ManagerForm(
                        title = strings.submoduleInit,
                        hint = strings.submoduleInitHint,
                        commands = listOf(initCommand),
                        confirm = "INIT",
                        enabled = !busy && submodules.isNotEmpty(),
                        onCancel = { if (!busy) mode = SubmoduleManagerMode.None },
                        onConfirm = { execute { controller.initSubmodules(targetPath) } },
                    ) {
                        Text(
                            targetLabel,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                    }

                    SubmoduleManagerMode.Update -> ManagerForm(
                        title = strings.submoduleUpdate,
                        hint = strings.submoduleUpdateHint,
                        commands = listOf(updateCommand),
                        confirm = "UPDATE",
                        enabled = !busy && submodules.isNotEmpty() &&
                            (checkout || customUpdateConfirmed),
                        onCancel = { if (!busy) mode = SubmoduleManagerMode.None },
                        onConfirm = { execute { controller.updateSubmodules(targetPath, updateOptions) } },
                    ) {
                        Text(
                            targetLabel,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TermButton(
                                hotkey = "i",
                                label = strings.submoduleInitOnUpdate,
                                active = initialize,
                            ) { initialize = !initialize }
                            Spacer(Modifier.width(8.dp))
                            TermButton(
                                hotkey = "r",
                                label = strings.submoduleRecursive,
                                active = recursive,
                            ) { recursive = !recursive }
                            Spacer(Modifier.width(8.dp))
                            TermButton(
                                hotkey = "c",
                                label = strings.submoduleCheckout,
                                active = checkout,
                            ) {
                                checkout = !checkout
                                customUpdateConfirmed = false
                            }
                        }
                        if (!checkout) {
                            Spacer(Modifier.height(7.dp))
                            Text(
                                strings.submoduleCustomUpdateWarning,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                            )
                            Spacer(Modifier.height(6.dp))
                            TermButton(
                                hotkey = "!",
                                label = strings.submoduleCustomUpdateConfirm,
                                active = customUpdateConfirmed,
                            ) { customUpdateConfirmed = !customUpdateConfirmed }
                        }
                    }

                    SubmoduleManagerMode.Sync -> ManagerForm(
                        title = strings.submoduleSync,
                        hint = strings.submoduleSyncHint,
                        commands = listOf(syncCommand),
                        confirm = "SYNC",
                        enabled = !busy && submodules.isNotEmpty(),
                        onCancel = { if (!busy) mode = SubmoduleManagerMode.None },
                        onConfirm = { execute { controller.syncSubmodules(targetPath, recursive) } },
                    ) {
                        Text(
                            targetLabel,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                        Spacer(Modifier.height(6.dp))
                        TermButton(
                            hotkey = "r",
                            label = strings.submoduleRecursive,
                            active = recursive,
                        ) { recursive = !recursive }
                    }

                    SubmoduleManagerMode.Add -> ManagerForm(
                        title = strings.submoduleAdd,
                        hint = strings.submoduleAddHint,
                        commands = listOf(addCommand),
                        confirm = "ADD",
                        enabled = !busy &&
                            validSubmoduleUrl(url) &&
                            validSubmodulePath(path),
                        onCancel = {
                            if (!busy) {
                                mode = SubmoduleManagerMode.None
                                resetFields()
                            }
                        },
                        onConfirm = { execute { controller.addSubmodule(url, path) } },
                    ) {
                        Text(
                            strings.submoduleUrl,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                        Spacer(Modifier.height(4.dp))
                        TermTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = "https://example.com/libs/foo.git",
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            strings.submodulePath,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                        Spacer(Modifier.height(4.dp))
                        TermTextField(
                            value = path,
                            onValueChange = { path = it },
                            placeholder = "libs/foo",
                        )
                        if (url.isNotBlank() && !validSubmoduleUrl(url)) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                strings.submoduleInvalidUrl,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                        if (path.isNotBlank() && !validSubmodulePath(path)) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                strings.submoduleInvalidPath,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }

                    SubmoduleManagerMode.Remove -> if (selected != null) {
                        ManagerForm(
                            title = strings.submoduleRemove,
                            hint = strings.submoduleRemoveHint,
                            commands = removeCommands,
                            confirm = "REMOVE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = SubmoduleManagerMode.None },
                            onConfirm = { execute { controller.removeSubmodule(selected.path) } },
                        ) {
                            Text(
                                selected.path,
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }

                    SubmoduleManagerMode.None -> Unit
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
private fun SubmoduleAllRow(
    strings: Strings,
    selected: Boolean,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(25.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("*", fontSize = 9.sp, fontFamily = Term.mono, color = Term.green, modifier = Modifier.width(12.dp))
        Text(
            "${strings.submoduleAll} ($count)",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.green else Term.text,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SubmoduleManagerRow(
    submodule: Submodule,
    strings: Strings,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(27.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            marker(submodule.state),
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = stateColor(submodule.state),
            modifier = Modifier.width(12.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                submodule.path,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (selected) Term.green else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stateLabel(submodule.state, strings),
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
            )
        }
    }
}

private fun marker(state: SubmoduleState): String = when (state) {
    SubmoduleState.Current -> "="
    SubmoduleState.Modified -> "+"
    SubmoduleState.Uninitialized -> "-"
    SubmoduleState.Conflict -> "!"
}

private fun stateColor(state: SubmoduleState) = when (state) {
    SubmoduleState.Current -> Term.greenDim
    SubmoduleState.Modified -> Term.amber
    SubmoduleState.Uninitialized -> Term.faint
    SubmoduleState.Conflict -> Term.red
}

private fun stateLabel(state: SubmoduleState, strings: Strings): String = when (state) {
    SubmoduleState.Current -> strings.submoduleStateCurrent
    SubmoduleState.Modified -> strings.submoduleStateModified
    SubmoduleState.Uninitialized -> strings.submoduleStateUninitialized
    SubmoduleState.Conflict -> strings.submoduleStateConflict
}
