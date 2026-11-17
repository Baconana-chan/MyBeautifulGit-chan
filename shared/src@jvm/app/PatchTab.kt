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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.PatchExportOptions
import org.baconana.mybeautifulgit_chan.domain.PatchMailbox
import org.baconana.mybeautifulgit_chan.domain.PatchMailboxParser
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider

private enum class PatchOutputMode { Files, Mailbox }

@Composable
fun PatchTab(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    var rangeMode by remember(controller.repository.root) { mutableStateOf(false) }
    var selection by remember(controller.repository.root) { mutableStateOf(emptySet<String>()) }
    var range by remember(controller.repository.root) { mutableStateOf("HEAD~3..HEAD") }
    var outputMode by remember(controller.repository.root) { mutableStateOf(PatchOutputMode.Files) }
    var outputDir by remember(controller.repository.root) { mutableStateOf("") }
    var outputFile by remember(controller.repository.root) { mutableStateOf("") }
    var coverLetter by remember { mutableStateOf(false) }
    var numbered by remember { mutableStateOf(false) }
    var signoff by remember { mutableStateOf(false) }
    var binary by remember { mutableStateOf(false) }
    var subjectPrefix by remember { mutableStateOf("") }
    var previewText by remember { mutableStateOf("") }
    var exportedFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var mailboxPath by remember(controller.repository.root) { mutableStateOf("") }
    var threeWay by remember { mutableStateOf(true) }
    var applySignoff by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val busy = running || controller.busy
    val commits = controller.commits

    LaunchedEffect(commits) {
        if (selection.any { hash -> commits.none { it.hash == hash } }) {
            selection = selection.filterTo(HashSet()) { hash -> commits.any { it.hash == hash } }
        }
    }

    fun exportOptions(preview: Boolean): PatchExportOptions = PatchExportOptions(
        hashes = if (rangeMode) emptyList() else selection.toList(),
        range = if (rangeMode) range else "",
        outputDir = if (!preview && outputMode == PatchOutputMode.Files) outputDir else "",
        outputFile = if (!preview && outputMode == PatchOutputMode.Mailbox) outputFile else "",
        previewOnly = preview,
        coverLetter = coverLetter && rangeMode,
        numbered = numbered,
        signoff = signoff,
        binary = binary,
        subjectPrefix = subjectPrefix,
    )

    fun canExport(preview: Boolean): Boolean {
        if (busy) return false
        if (rangeMode) {
            if (range.isBlank()) return false
        } else if (selection.isEmpty()) {
            return false
        }
        return preview || outputMode == PatchOutputMode.Files || outputFile.isNotBlank()
    }

    fun runExport(preview: Boolean) {
        if (!canExport(preview)) return
        running = true
        scope.launch {
            try {
                val result = controller.exportPatches(exportOptions(preview))
                if (result != null) {
                    if (result.mailbox.isNotBlank()) previewText = result.mailbox
                    if (!preview && result.outputFile.isBlank()) previewText = ""
                    exportedFiles = result.files
                    if (result.outputFile.isNotBlank()) {
                        mailboxPath = result.outputFile
                    } else {
                        // A preview or a directory export does not produce the
                        // loaded mailbox; do not let Apply target a stale file.
                        mailboxPath = ""
                    }
                }
            } finally {
                running = false
            }
        }
    }

    fun loadMailbox() {
        if (busy || mailboxPath.isBlank()) return
        previewText = ""
        scope.launch { controller.loadPatchMailbox(mailboxPath) }
    }

    val generatedMailbox: PatchMailbox? = remember(previewText) {
        previewText.takeIf { it.isNotBlank() }
            ?.let { PatchMailboxParser.parse(it, "(preview)") }
    }
    val shownMailbox = generatedMailbox ?: controller.patchMailbox
    val canUsePatch = mailboxPath.isNotBlank() && !busy

    Column(Modifier.fillMaxSize().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.patchesTitle,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Text(
                "  ${commits.size} commits · ${controller.repository.name}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            if (controller.busy) {
                Text("[working…]", fontSize = 8.sp, fontFamily = Term.mono, color = Term.amber)
            }
        }
        Divider()

        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(480.dp).fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 9.dp, vertical = 8.dp),
            ) {
                SectionLabel(strings.patchExportTitle)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TermButton(
                        hotkey = "c",
                        label = strings.patchSelectMode,
                        active = !rangeMode,
                    ) { rangeMode = false }
                    TermButton(
                        hotkey = "r",
                        label = strings.patchRangeMode,
                        active = rangeMode,
                    ) { rangeMode = true }
                }
                Spacer(Modifier.height(7.dp))

                if (rangeMode) {
                    Text(
                        strings.patchRange,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(3.dp))
                    TermTextField(
                        value = range,
                        onValueChange = { range = it },
                        placeholder = strings.patchRangePlaceholder,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${strings.patchSelectMode} ${strings.patchSelectedCount(selection.size)}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "[${strings.patchSelectAll}]",
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.greenDim,
                            modifier = Modifier.clickable {
                                selection = commits.mapTo(HashSet()) { it.hash }
                            },
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "[${strings.patchClearSelection}]",
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.clickable { selection = emptySet() },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (commits.isEmpty()) {
                        Text(
                            strings.patchNoCommits,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    } else {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 245.dp)
                                .border(1.dp, Term.border)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 2.dp),
                        ) {
                            commits.forEach { commit ->
                                val selected = commit.hash in selection
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(
                                            if (selected) Term.green.copy(alpha = 0.10f)
                                            else Term.panel,
                                        )
                                        .clickable {
                                            selection = if (selected) {
                                                selection - commit.hash
                                            } else {
                                                selection + commit.hash
                                            }
                                        }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        if (selected) "[x]" else "[ ]",
                                        fontSize = 8.sp,
                                        fontFamily = Term.mono,
                                        color = if (selected) Term.green else Term.faint,
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        commit.shortHash,
                                        fontSize = 8.sp,
                                        fontFamily = Term.mono,
                                        color = Term.faint,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        commit.subject,
                                        fontSize = 8.5.sp,
                                        fontFamily = Term.mono,
                                        color = if (selected) Term.text else Term.dim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                Divider()
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TermButton(
                        hotkey = "f",
                        label = strings.patchModeFiles,
                        active = outputMode == PatchOutputMode.Files,
                    ) { outputMode = PatchOutputMode.Files }
                    TermButton(
                        hotkey = "m",
                        label = strings.patchModeMailbox,
                        active = outputMode == PatchOutputMode.Mailbox,
                    ) { outputMode = PatchOutputMode.Mailbox }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (outputMode == PatchOutputMode.Files) strings.patchOutputDir else strings.patchOutputFile,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(3.dp))
                if (outputMode == PatchOutputMode.Files) {
                    TermTextField(
                        value = outputDir,
                        onValueChange = { outputDir = it },
                        placeholder = strings.patchOutputDirPlaceholder,
                    )
                } else {
                    TermTextField(
                        value = outputFile,
                        onValueChange = { outputFile = it },
                        placeholder = strings.patchOutputFilePlaceholder,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row {
                    TermButton(
                        hotkey = "l",
                        label = strings.patchCoverLetter,
                        active = coverLetter,
                    ) { if (rangeMode) coverLetter = !coverLetter }
                    Spacer(Modifier.width(4.dp))
                    TermButton(
                        hotkey = "n",
                        label = strings.patchNumbered,
                        active = numbered,
                    ) { numbered = !numbered }
                    Spacer(Modifier.width(4.dp))
                    TermButton(
                        hotkey = "s",
                        label = strings.patchSignoff,
                        active = signoff,
                    ) { signoff = !signoff }
                    Spacer(Modifier.width(4.dp))
                    TermButton(
                        hotkey = "b",
                        label = strings.patchBinary,
                        active = binary,
                    ) { binary = !binary }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.patchSubjectPrefix,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(3.dp))
                TermTextField(
                    value = subjectPrefix,
                    onValueChange = { subjectPrefix = it },
                    placeholder = strings.patchSubjectPrefixPlaceholder,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(
                        null,
                        strings.patchPreview,
                        enabled = canExport(preview = true),
                    ) { runExport(preview = true) }
                    ActionButton(
                        null,
                        strings.patchExport,
                        primary = true,
                        enabled = canExport(preview = false),
                    ) { runExport(preview = false) }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (previewText.isBlank()) strings.patchPreviewHint else strings.patchExportHint,
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                if (exportedFiles.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    SectionLabel("FILES")
                    exportedFiles.take(12).forEach { file ->
                        Text(
                            file,
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                mailboxPath = file
                                previewText = ""
                                scope.launch { controller.loadPatchMailbox(file) }
                            },
                        )
                    }
                }
                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
            }

            VDivider()

            Column(Modifier.weight(1f).fillMaxHeight().padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(strings.patchMailboxTitle)
                    Spacer(Modifier.weight(1f))
                    Text(
                        strings.patchMailboxMessages + " ${shownMailbox?.messages?.size ?: 0}",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.patchMailboxPath,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TermTextField(
                        value = mailboxPath,
                        onValueChange = { mailboxPath = it },
                        placeholder = strings.patchMailboxPathPlaceholder,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    ActionButton(
                        null,
                        strings.patchLoad,
                        enabled = !busy && mailboxPath.isNotBlank(),
                    ) { loadMailbox() }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TermButton(
                        hotkey = "3",
                        label = strings.patchApply3way,
                        active = threeWay,
                    ) { threeWay = !threeWay }
                    TermButton(
                        hotkey = "s",
                        label = strings.patchAmSignoff,
                        active = applySignoff,
                    ) { applySignoff = !applySignoff }
                    Spacer(Modifier.weight(1f))
                    ActionButton(
                        null,
                        strings.patchApplyCheck,
                        enabled = canUsePatch,
                    ) {
                        scope.launch {
                            running = true
                            try {
                                controller.applyPatchFile(mailboxPath, check = true, threeWay = threeWay)
                            } finally {
                                running = false
                            }
                        }
                    }
                    ActionButton(
                        null,
                        strings.patchApply,
                        enabled = canUsePatch,
                    ) {
                        scope.launch {
                            running = true
                            try {
                                controller.applyPatchFile(mailboxPath, check = false, threeWay = threeWay)
                            } finally {
                                running = false
                            }
                        }
                    }
                    ActionButton(
                        null,
                        strings.patchAm,
                        enabled = canUsePatch,
                    ) {
                        scope.launch {
                            running = true
                            try {
                                controller.applyMailbox(
                                    mailboxPath,
                                    threeWay = threeWay,
                                    signoff = applySignoff,
                                )
                            } finally {
                                running = false
                            }
                        }
                    }
                }
                Text(
                    strings.patchApplyHint,
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                controller.patchMailboxError?.let { message ->
                    Spacer(Modifier.height(6.dp))
                    ErrorBanner(message, controller::dismissPatchMailboxError)
                }
                if (controller.patchMailboxLoading) {
                    Text(
                        "loading…",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Spacer(Modifier.height(7.dp))

                if (shownMailbox == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            strings.patchMailboxEmpty,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                } else {
                    if (shownMailbox.truncated) {
                        Text(
                            strings.patchMailboxTruncated,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.amber,
                        )
                    }
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 125.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        shownMailbox.messages.forEach { message ->
                            Text(
                                "${message.index}. ${message.subject}",
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (message.from.isNotBlank()) {
                                Text(
                                    message.from,
                                    fontSize = 7.5.sp,
                                    fontFamily = Term.mono,
                                    color = Term.faint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SectionLabel(strings.patchRaw)
                    Spacer(Modifier.height(3.dp))
                    Column(
                        Modifier.fillMaxWidth().weight(1f).background(Term.black)
                            .border(1.dp, Term.border)
                            .verticalScroll(rememberScrollState())
                            .padding(6.dp),
                    ) {
                        Text(
                            shownMailbox.raw,
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                    }
                }
            }
        }
    }
}
