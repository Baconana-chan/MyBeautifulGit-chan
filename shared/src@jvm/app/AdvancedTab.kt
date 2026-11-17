package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.ArchiveFormat
import org.baconana.mybeautifulgit_chan.domain.GitNoteEntry
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider

private enum class AdvancedSection(val label: String) {
    Storage("STORAGE"),
    Sparse("SPARSE"),
    Lfs("LFS"),
    Notes("NOTES"),
    Archive("ARCHIVE"),
    Bundle("BUNDLE"),
    Fsmonitor("FSMONITOR"),
    Partial("PARTIAL"),
}

@Composable
fun AdvancedTab(controller: RepoController, scope: CoroutineScope) {
    var section by remember(controller.repository.root) { mutableStateOf(AdvancedSection.Storage) }
    val strings = LocalStrings.current
    val busy = controller.busy || controller.advancedLoading || controller.notesLoading || controller.lfsLoading

    LaunchedEffect(controller.repository.root, section) {
        when (section) {
            AdvancedSection.Sparse -> controller.loadSparseState()
            AdvancedSection.Lfs -> controller.loadLfsState()
            AdvancedSection.Notes -> controller.loadNotes()
            AdvancedSection.Fsmonitor -> controller.loadFsmonitorConfig()
            AdvancedSection.Partial -> controller.loadPartialCloneState()
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize().background(Term.bg)) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).background(Term.panel)
                .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AdvancedSection.entries.forEach { item ->
                ActionButton(
                    key = null,
                    label = item.label,
                    primary = item == section,
                    enabled = !busy,
                    onClick = { section = item },
                )
            }
        }
        Divider()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (section) {
                    AdvancedSection.Storage -> StorageAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Sparse -> SparseAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Lfs -> LfsAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Notes -> NotesAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Archive -> ArchiveAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Bundle -> BundleAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Fsmonitor -> FsmonitorAdvancedSection(controller, scope, busy, strings)
                    AdvancedSection.Partial -> PartialAdvancedSection(controller, scope, busy, strings)
                }
            }
            VDivider()
            AdvancedResultPanel(controller, Modifier.width(380.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun StorageAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    AdvancedTitle("OBJECT STORAGE")
    AdvancedHint(strings.advancedStorageHint)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "COUNT OBJECTS", enabled = !busy) {
            scope.launch { controller.countObjects() }
        }
        ActionButton(null, "FSCK", enabled = !busy) {
            scope.launch { controller.fsckRepository() }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "GC", enabled = !busy) {
            scope.launch { controller.gcRepository() }
        }
        ActionButton(null, "GC AGGRESSIVE", enabled = !busy) {
            scope.launch { controller.gcRepository(aggressive = true) }
        }
        ActionButton(null, "REPACK -AD", enabled = !busy) {
            scope.launch { controller.repackRepository() }
        }
    }
    AdvancedDangerBox(strings.advancedDangerHint)
    AdvancedHint(strings.advancedPruneWarning, Term.amber)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "PRUNE DRY-RUN", enabled = !busy) {
            scope.launch { controller.previewPrune() }
        }
        ActionButton(
            key = null,
            label = "PRUNE",
            danger = true,
            enabled = !busy && controller.prunePreview != null,
        ) {
            scope.launch { controller.pruneRepository() }
        }
    }
    controller.prunePreview?.let { preview ->
        AdvancedLabel("PRUNE PREVIEW")
        Text(
            preview.ifBlank { "(no unreachable objects reported)" },
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            modifier = Modifier.fillMaxWidth().background(Term.black)
                .border(1.dp, Term.border).padding(9.dp),
        )
    }
}

@Composable
private fun SparseAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    var paths by remember(controller.repository.root) { mutableStateOf("") }
    var cone by remember(controller.repository.root) { mutableStateOf(true) }
    var loadedPatterns by remember(controller.repository.root) { mutableStateOf("") }

    LaunchedEffect(controller.sparseState) {
        val current = controller.sparseState.patterns.joinToString("\n")
        if (paths.isBlank() || paths == loadedPatterns) {
            paths = current
            cone = controller.sparseState.cone
            loadedPatterns = current
        }
    }

    AdvancedTitle("SPARSE-CHECKOUT")
    AdvancedHint(strings.advancedSparseHint)
    Text(
        if (controller.sparseState.enabled) {
            "enabled · ${controller.sparseState.patterns.size} paths · cone=${controller.sparseState.cone}"
        } else {
            "disabled · full working tree"
        },
        fontSize = 9.5.sp,
        fontFamily = Term.mono,
        color = if (controller.sparseState.enabled) Term.green else Term.dim,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        ActionButton(
            key = null,
            label = if (cone) "CONE: ON" else "CONE: OFF",
            primary = cone,
            enabled = !busy,
            onClick = { cone = !cone },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "one path per line; cone mode accepts directories, not glob patterns",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
    AdvancedLabel("PATHS / PATTERNS")
    TermTextArea(
        value = paths,
        onValueChange = { paths = it },
        placeholder = "src\nshared/src@jvm",
        modifier = Modifier.fillMaxWidth().height(150.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "APPLY", primary = true, enabled = !busy) {
            scope.launch { controller.applySparseCheckout(paths, cone) }
        }
        ActionButton(null, "REAPPLY", enabled = !busy && controller.sparseState.enabled) {
            scope.launch { controller.reapplySparseCheckout() }
        }
        ActionButton(
            null,
            "DISABLE",
            danger = true,
            enabled = !busy && controller.sparseState.enabled,
        ) {
            scope.launch {
                if (controller.disableSparseCheckout()) {
                    paths = ""
                    cone = true
                }
            }
        }
    }
}

@Composable
private fun LfsAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    var pattern by remember(controller.repository.root) { mutableStateOf("*.psd") }
    val state = controller.lfsState

    AdvancedTitle("GIT LFS")
    AdvancedHint(strings.advancedLfsHint)
    if (controller.lfsLoading) {
        Text("checking git-lfs…", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
    } else if (state.available) {
        Text(
            state.version.ifBlank { "git-lfs available" },
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.green,
        )
    } else {
        Text(
            "${strings.advancedNoLfs}${state.version.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.red,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "INSTALL --LOCAL", enabled = !busy) {
            scope.launch { controller.installLfs() }
        }
        ActionButton(null, "RELOAD", enabled = !busy) {
            scope.launch { controller.loadLfsState() }
        }
        ActionButton(null, "PULL", enabled = !busy && state.available) {
            scope.launch { controller.lfsPull() }
        }
        ActionButton(null, "FETCH --ALL", enabled = !busy && state.available) {
            scope.launch { controller.lfsFetchAll() }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TermTextField(
            value = pattern,
            onValueChange = { pattern = it },
            placeholder = "*.psd",
            modifier = Modifier.weight(1f),
            onSubmit = {
                if (!busy && state.available) scope.launch { controller.trackLfs(pattern) }
            },
        )
        Spacer(Modifier.width(6.dp))
        ActionButton(null, "TRACK", enabled = !busy && state.available) {
            scope.launch { controller.trackLfs(pattern) }
        }
    }
    AdvancedLabel("TRACKED FILES (${state.files.size})")
    if (state.files.isEmpty()) {
        Text("no LFS files", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
    } else {
        state.files.take(100).forEach { file ->
            Text(file, fontSize = 9.sp, fontFamily = Term.mono, color = Term.text)
        }
        if (state.files.size > 100) {
            Text("… ${state.files.size - 100} more", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
    }
}

@Composable
private fun NotesAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    var target by remember(controller.repository.root) { mutableStateOf("HEAD") }
    var draft by remember(controller.repository.root) { mutableStateOf("") }
    var confirmDelete by remember(controller.repository.root) { mutableStateOf(false) }
    val notesBusy = busy || controller.notesLoading
    LaunchedEffect(target) { confirmDelete = false }

    AdvancedTitle("GIT NOTES")
    AdvancedHint(strings.advancedNotesHint)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TermTextField(
            value = target,
            onValueChange = { target = it },
            placeholder = "HEAD or a full/partial object id",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        ActionButton(null, "SHOW", enabled = !notesBusy) {
            scope.launch {
                if (controller.showNote(target)) draft = controller.gitNoteText
            }
        }
        Spacer(Modifier.width(6.dp))
        ActionButton(null, "LIST", enabled = !notesBusy) {
            scope.launch { controller.loadNotes() }
        }
    }
    AdvancedLabel("NOTES (${controller.gitNotes.size})")
    if (controller.notesLoading) {
        Text("loading notes…", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
    } else if (controller.gitNotes.isEmpty()) {
        Text(strings.advancedNotesEmpty, fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
    } else {
        Column(Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)) {
            controller.gitNotes.take(100).forEachIndexed { index, note ->
                NoteRow(note, index, busy) {
                    target = note.objectHash
                    scope.launch {
                        if (controller.showNote(note.objectHash)) draft = controller.gitNoteText
                    }
                }
            }
        }
        if (controller.gitNotes.size > 100) {
            Text("… ${controller.gitNotes.size - 100} more", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
    }
    AdvancedLabel("NOTE TEXT")
    TermTextArea(
        value = draft,
        onValueChange = { draft = it },
        placeholder = "write a note for the selected object",
        modifier = Modifier.fillMaxWidth().height(145.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "SAVE / REPLACE", primary = true, enabled = !notesBusy) {
            scope.launch {
                if (controller.saveNote(target, draft, append = false)) draft = controller.gitNoteText
            }
        }
        ActionButton(null, "APPEND", enabled = !notesBusy && draft.isNotBlank()) {
            scope.launch {
                if (controller.saveNote(target, draft, append = true)) draft = controller.gitNoteText
            }
        }
        ActionButton(
            null,
            if (confirmDelete) "CONFIRM DELETE" else "DELETE",
            danger = true,
            enabled = !notesBusy,
        ) {
            if (!confirmDelete) {
                confirmDelete = true
            } else {
                confirmDelete = false
                scope.launch {
                    if (controller.removeNote(target)) draft = ""
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: GitNoteEntry, index: Int, busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (index % 2 == 0) Term.panel else Term.panelAlt)
            .clickable(enabled = !busy) { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            note.shortObject,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.green,
            modifier = Modifier.width(105.dp),
        )
        Text(
            "note ${note.shortNote}",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun ArchiveAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    var ref by remember(controller.repository.root) { mutableStateOf("HEAD") }
    var format by remember(controller.repository.root) { mutableStateOf(ArchiveFormat.Zip) }
    var output by remember(controller.repository.root) {
        mutableStateOf("${controller.repository.name}.zip")
    }

    LaunchedEffect(format) {
        val old = if (format == ArchiveFormat.Zip) ".tar.gz" else ".zip"
        if (output.endsWith(old)) output = output.dropLast(old.length) + ".${format.extension}"
    }

    AdvancedTitle("ARCHIVE")
    AdvancedHint(strings.advancedArchiveHint)
    AdvancedLabel("REVISION")
    TermTextField(
        value = ref,
        onValueChange = { ref = it },
        placeholder = "HEAD, branch, tag or commit",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ArchiveFormat.entries.forEach { item ->
            ActionButton(
                key = null,
                label = item.label,
                primary = item == format,
                enabled = !busy,
                onClick = { format = item },
            )
        }
    }
    AdvancedLabel("OUTPUT FILE (relative to repository root or absolute)")
    TermTextField(
        value = output,
        onValueChange = { output = it },
        placeholder = "${controller.repository.name}.zip",
    )
    ActionButton(null, "CREATE ARCHIVE", primary = true, enabled = !busy) {
        scope.launch { controller.createArchive(ref, format, output) }
    }
}

@Composable
private fun BundleAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    var output by remember(controller.repository.root) {
        mutableStateOf("${controller.repository.name}.bundle")
    }

    AdvancedTitle("BUNDLE")
    AdvancedHint(strings.advancedBundleHint)
    AdvancedLabel("BUNDLE FILE (relative to repository root or absolute)")
    TermTextField(
        value = output,
        onValueChange = { output = it },
        placeholder = "${controller.repository.name}.bundle",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "CREATE ALL", primary = true, enabled = !busy) {
            scope.launch { controller.createBundle(output) }
        }
        ActionButton(null, "VERIFY", enabled = !busy) {
            scope.launch { controller.verifyBundle(output) }
        }
        ActionButton(null, "LIST HEADS", enabled = !busy) {
            scope.launch { controller.listBundleHeads(output) }
        }
    }
    Text(
        "CREATE refuses to overwrite an existing file; VERIFY/LIST require an existing bundle.",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
    )
}

@Composable
private fun FsmonitorAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    AdvancedTitle("FSMONITOR")
    AdvancedHint(strings.advancedFsmonitorHint)
    Text(
        "core.fsmonitor = ${controller.fsmonitorConfig.ifBlank { "unset" }}",
        fontSize = 10.sp,
        fontFamily = Term.mono,
        color = if (controller.fsmonitorConfig == "true") Term.green else Term.dim,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "CONFIG ON", enabled = !busy) {
            scope.launch { controller.setFsmonitor(true) }
        }
        ActionButton(null, "CONFIG OFF", enabled = !busy) {
            scope.launch { controller.setFsmonitor(false) }
        }
        ActionButton(null, "RELOAD", enabled = !busy) {
            scope.launch { controller.loadFsmonitorConfig() }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "STATUS", enabled = !busy) {
            scope.launch { controller.fsmonitor("status") }
        }
        ActionButton(null, "START", enabled = !busy) {
            scope.launch { controller.fsmonitor("start") }
        }
        ActionButton(null, "STOP", enabled = !busy) {
            scope.launch { controller.fsmonitor("stop") }
        }
    }
    Text(
        "The daemon is platform-dependent; unsupported platforms report the Git error below.",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
    )
}

@Composable
private fun PartialAdvancedSection(
    controller: RepoController,
    scope: CoroutineScope,
    busy: Boolean,
    strings: org.baconana.mybeautifulgit_chan.i18n.Strings,
) {
    val state = controller.partialCloneState
    AdvancedTitle("PARTIAL CLONE")
    AdvancedHint(strings.advancedPartialHint)
    Text(
        if (state.enabled) "partial clone configured" else "full clone / no promisor filter configured",
        fontSize = 10.sp,
        fontFamily = Term.mono,
        color = if (state.enabled) Term.green else Term.dim,
    )
    if (state.details.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border).padding(9.dp)) {
            state.details.forEach { line ->
                Text(line, fontSize = 9.sp, fontFamily = Term.mono, color = Term.text)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(null, "RELOAD", enabled = !busy) {
            scope.launch { controller.loadPartialCloneState() }
        }
    }
    AdvancedLabel("CLONE FILTERS")
    Text(
        "FULL · BLOB:NONE · BLOB:1M · TREE:0 — choose one in the clone dialog.",
        fontSize = 9.sp,
        fontFamily = Term.mono,
        color = Term.faint,
    )
}

@Composable
private fun AdvancedResultPanel(controller: RepoController, modifier: Modifier = Modifier) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(27.dp).background(Term.raised).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "RESULT",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            if (controller.advancedLoading) {
                Text("running…", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.amber)
            }
        }
        val result = controller.advancedResult
        if (result == null) {
            Text(
                "run a command; the exact git command and its output stay here.",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(10.dp),
            )
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (result.ok) "[ok]" else "[exit ${result.exitCode}]",
                        fontSize = 9.5.sp,
                        fontFamily = Term.mono,
                        color = if (result.ok) Term.green else Term.red,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${result.durationMs} ms",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Text(
                    "$ ${result.command}",
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.cyan,
                )
                result.note?.let {
                    Text(it, fontSize = 9.sp, fontFamily = Term.mono, color = Term.green)
                }
                Text(
                    result.text.ifBlank { "(no output)" },
                    fontSize = 8.8.sp,
                    fontFamily = Term.mono,
                    color = Term.text,
                )
            }
        }
    }
}

@Composable
private fun AdvancedTitle(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontFamily = Term.mono,
        color = Term.white,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun AdvancedLabel(text: String) {
    Text(
        text,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.dim,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun AdvancedHint(text: String, color: androidx.compose.ui.graphics.Color = Term.faint) {
    Text(text, fontSize = 9.sp, fontFamily = Term.mono, color = color)
}

@Composable
private fun AdvancedDangerBox(text: String) {
    Box(
        Modifier.fillMaxWidth().background(Term.red.copy(alpha = 0.08f))
            .border(1.dp, Term.red.copy(alpha = 0.55f)).padding(9.dp),
    ) {
        Text(text, fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
    }
}
