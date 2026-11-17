package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.RebaseCommand
import org.baconana.mybeautifulgit_chan.domain.RebasePlan
import org.baconana.mybeautifulgit_chan.domain.RebaseTodoEntry
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

enum class RebaseTodoMode { Prepare, Edit }

data class RebaseTodoRequest(
    val mode: RebaseTodoMode,
    val commit: Commit? = null,
    /** Коммиты, которые нужно сразу пометить drop (удаление через rebase). */
    val presetDropHashes: Set<String> = emptySet(),
)

@Composable
fun RebaseTodoDialog(
    request: RebaseTodoRequest,
    controller: RepoController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var plan by remember(request) { mutableStateOf<RebasePlan?>(null) }
    var entries by remember(request) { mutableStateOf<List<RebaseTodoEntry>>(emptyList()) }
    var autosquash by remember(request) { mutableStateOf(false) }
    var loading by remember(request) { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var paletteFor by remember { mutableStateOf<Long?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val busy = loading || running || controller.busy
    val edit = request.mode == RebaseTodoMode.Edit

    LaunchedEffect(request, autosquash) {
        loading = true
        controller.dismissError()
        val loaded = if (edit) {
            controller.loadRebaseTodo()
        } else {
            request.commit?.let { controller.loadInteractiveRebasePlan(it, autosquash) }
        }
        plan = loaded
        entries = loaded?.entries.orEmpty().map { entry ->
            val selected = entry.command.takesCommit && request.presetDropHashes.any { hash ->
                hash == entry.argument ||
                    (entry.hash.isNotBlank() && hash.startsWith(entry.hash))
            }
            if (selected) {
                entry.copy(command = RebaseCommand.Drop, argument = entry.hash)
            } else {
                entry
            }
        }
        paletteFor = null
        loading = false
    }

    fun updateEntry(id: Long, transform: (RebaseTodoEntry) -> RebaseTodoEntry) {
        entries = entries.map { if (it.id == id) transform(it) else it }
    }

    fun selectCommand(entry: RebaseTodoEntry, command: RebaseCommand) {
        updateEntry(entry.id) { old ->
            var next = old.copy(command = command)
            if (!command.takesCommit) {
                next = next.copy(argument = "", comment = "", message = "")
            } else if (!old.command.takesCommit) {
                next = next.copy(argument = "", comment = "", message = "")
            } else if (old.argument.trim().startsWith("-") &&
                command != RebaseCommand.Fixup
            ) {
                next = next.copy(argument = old.hash)
            }
            if (command == RebaseCommand.Reword && next.message.isBlank()) {
                next = next.copy(message = next.subject)
            }
            next
        }
        paletteFor = null
    }

    fun moveEntry(id: Long, delta: Int) {
        val from = entries.indexOfFirst { it.id == id }
        if (from < 0) return
        val target = (from + delta).coerceIn(0, entries.lastIndex)
        if (target == from) return
        val updated = entries.toMutableList()
        val item = updated.removeAt(from)
        updated.add(target, item)
        entries = updated
    }

    fun addEntry(command: RebaseCommand) {
        val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries = entries + RebaseTodoEntry(id = id, command = command)
        paletteFor = id
    }

    fun removeEntry(entry: RebaseTodoEntry) {
        if (entry.command.takesCommit && entry.command != RebaseCommand.Drop) {
            updateEntry(entry.id) {
                it.copy(
                    command = RebaseCommand.Drop,
                    argument = if (it.argument.trim().startsWith("-")) it.hash else it.argument,
                )
            }
        } else {
            entries = entries.filterNot { it.id == entry.id }
        }
    }

    fun run() {
        if (busy) return
        val currentPlan = plan ?: return
        scope.launch {
            running = true
            val success = if (edit) {
                controller.saveRebaseTodo(entries)
            } else {
                controller.startInteractiveRebase(
                    currentPlan.copy(entries = entries, autosquash = autosquash),
                )
            }
            running = false
            if (success || controller.operation != null) {
                onDismiss()
                onSuccess()
            }
        }
    }

    DialogShell(
        title = if (edit) strings.rebaseEditorEditTitle else strings.rebaseEditorPrepareTitle,
        width = 900.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        val base = when {
            edit -> "HEAD"
            plan?.root == true -> "--root"
            plan?.base != null -> plan!!.base!!
            request.commit?.parents?.firstOrNull() != null -> request.commit.parents.first()
            else -> "--root"
        }
        Text(
            if (edit) strings.rebaseEditorEditHint else strings.rebaseEditorBackupHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.rebaseEditorBase,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                modifier = Modifier.width(56.dp),
            )
            Text(
                base,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!edit) {
                TermButton("a", strings.rebaseEditorAutosquash, active = autosquash) {
                    autosquash = !autosquash
                }
            }
        }
        if (!edit) {
            Text(
                strings.rebaseEditorAutosquashHint,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            strings.rebaseEditorCommandHint,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Text(
                strings.rebaseEditorLoading,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )

            plan == null -> Text(
                strings.rebaseEditorEmpty,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.red,
            )

            else -> {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 390.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    entries.forEach { entry ->
                        RebaseTodoRow(
                            entry = entry,
                            dragging = draggingId == entry.id,
                            paletteOpen = paletteFor == entry.id,
                            strings = strings,
                            onDragStart = { draggingId = entry.id },
                            onDragEnd = { draggingId = null },
                            onMove = { delta -> moveEntry(entry.id, delta) },
                            onTogglePalette = {
                                paletteFor = if (paletteFor == entry.id) null else entry.id
                            },
                            onSelectCommand = { selectCommand(entry, it) },
                            onUpdate = { transform -> updateEntry(entry.id, transform) },
                            onRemove = { removeEntry(entry) },
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.rebaseEditorAdd,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
            Spacer(Modifier.width(6.dp))
            listOf(
                RebaseCommand.Exec,
                RebaseCommand.Break,
                RebaseCommand.Label,
                RebaseCommand.Reset,
                RebaseCommand.Merge,
            ).forEach { command ->
                TermButton(command.token.take(1), command.token) { addEntry(command) }
                Spacer(Modifier.width(4.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel("COMMAND")
        Spacer(Modifier.height(3.dp))
        Column(
            Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            val root = plan?.root == true
            val prefix = if (edit) {
                "git rebase --edit-todo"
            } else {
                "git rebase -i --rebase-merges ${if (root) "--root" else base}"
            }
            Text(
                "$ $prefix",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
            )
            Text(
                "# ${entries.size} ${strings.rebaseEditorTodo}" +
                    if (!edit && autosquash) " · ${strings.rebaseEditorAutosquash}" else "",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }

        controller.error?.let { message ->
            Spacer(Modifier.height(9.dp))
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
                    edit -> strings.rebaseEditorSave
                    else -> strings.rebaseEditorStart
                },
                primary = true,
                enabled = !busy && plan != null && entries.isNotEmpty(),
                onClick = { run() },
            )
        }
    }
}

@Composable
private fun RebaseTodoRow(
    entry: RebaseTodoEntry,
    dragging: Boolean,
    paletteOpen: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int) -> Unit,
    onTogglePalette: () -> Unit,
    onSelectCommand: (RebaseCommand) -> Unit,
    onUpdate: ((RebaseTodoEntry) -> RebaseTodoEntry) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(if (dragging) Term.green.copy(alpha = 0.12f) else Term.panelAlt)
            .border(1.dp, Term.border)
            .padding(horizontal = 5.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var accumulated by remember(entry.id) { mutableStateOf(0f) }
            Text(
                "≡",
                fontSize = 12.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.width(18.dp)
                    .pointerInput(entry.id) {
                        detectDragGestures(
                            onDragStart = {
                                accumulated = 0f
                                onDragStart()
                            },
                            onDragEnd = {
                                accumulated = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                accumulated = 0f
                                onDragEnd()
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                accumulated += amount.y
                                val steps = (accumulated / 18f).toInt()
                                if (steps != 0) {
                                    onMove(steps)
                                    accumulated -= steps * 18f
                                }
                            },
                        )
                    },
            )
            Text(
                "[${entry.command.token}]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (entry.command == RebaseCommand.Drop) Term.red else Term.green,
                modifier = Modifier.width(82.dp).clickable { onTogglePalette() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.hash.take(10).ifBlank { "—" },
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                modifier = Modifier.width(88.dp),
            )
            Text(
                when {
                    entry.subject.isNotBlank() -> entry.subject
                    entry.argument.isNotBlank() -> entry.argument
                    else -> strings.rebaseEditorArgument
                },
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (entry.command == RebaseCommand.Drop) Term.faint else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "[x]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clickable { onRemove() },
            )
        }

        if (entry.command == RebaseCommand.Reword) {
            Spacer(Modifier.height(3.dp))
            TermTextField(
                value = entry.message,
                onValueChange = { value -> onUpdate { it.copy(message = value) } },
                placeholder = strings.rebaseEditorRewordMessage,
                singleLine = false,
            )
        }
        if (entry.command in setOf(
                RebaseCommand.Exec,
                RebaseCommand.Label,
                RebaseCommand.Reset,
                RebaseCommand.Merge,
            )
        ) {
            Spacer(Modifier.height(3.dp))
            TermTextField(
                value = entry.argument,
                onValueChange = { value -> onUpdate { it.copy(argument = value) } },
                placeholder = strings.rebaseEditorArgument,
            )
        }

        if (paletteOpen) {
            Spacer(Modifier.height(4.dp))
            CommandPicker(entry.command, onSelectCommand)
        }
    }
}

@Composable
private fun CommandPicker(
    selected: RebaseCommand,
    onSelect: (RebaseCommand) -> Unit,
) {
    Column {
        RebaseCommand.entries.chunked(6).forEach { chunk ->
            Row {
                chunk.forEach { command ->
                    TermButton(
                        hotkey = command.token.take(1),
                        label = command.token,
                        active = command == selected,
                    ) { onSelect(command) }
                    Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
