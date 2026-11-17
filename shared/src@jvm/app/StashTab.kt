package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import org.baconana.mybeautifulgit_chan.domain.StashCreateOptions
import org.baconana.mybeautifulgit_chan.domain.StashEntry
import org.baconana.mybeautifulgit_chan.domain.StatusEntry
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StashTab(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    var message by remember(controller.repository.root) { mutableStateOf("") }
    var includeUntracked by remember(controller.repository.root) { mutableStateOf(false) }
    var includeIgnored by remember(controller.repository.root) { mutableStateOf(false) }
    var stagedOnly by remember(controller.repository.root) { mutableStateOf(false) }
    var selectedPaths by remember(controller.repository.root) { mutableStateOf(emptySet<String>()) }
    var selectedHash by remember(controller.repository.root) { mutableStateOf<String?>(null) }
    var branchName by remember(controller.repository.root) { mutableStateOf("") }
    var confirmDrop by remember { mutableStateOf(false) }
    var confirmPop by remember { mutableStateOf(false) }
    val stashes = controller.stashes
    val selected = stashes.firstOrNull { it.hash == selectedHash }
        ?: stashes.firstOrNull()
    val stagedSupported = controller.gitVersion >= GitVersion(2, 35, 0, "2.35.0")
    val statusEntries = controller.status?.entries.orEmpty()
        .filter { it.kind != StatusKind.Ignored || includeIgnored }

    LaunchedEffect(stashes) {
        if (stashes.none { it.hash == selectedHash }) {
            selectedHash = stashes.firstOrNull()?.hash
            branchName = ""
            confirmDrop = false
            confirmPop = false
        }
    }

    LaunchedEffect(selected?.hash, selected?.selector) {
        val selector = selected?.selector
        if (selector == null) {
            return@LaunchedEffect
        }
        branchName = ""
        confirmDrop = false
        confirmPop = false
        controller.loadStashDetail(selector)
    }

    Column(Modifier.fillMaxSize().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.stashTitle,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Text(
                " (${stashes.size})",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                controller.status?.branch?.head ?: "(detached)",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "[refresh]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (controller.busy) Term.faint else Term.greenDim,
                modifier = Modifier.clickable(enabled = !controller.busy) {
                    scope.launch { controller.refresh() }
                },
            )
        }
        Divider()

        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(360.dp).fillMaxHeight().background(Term.panel),
            ) {
                StashCreatePanel(
                    controller = controller,
                    scope = scope,
                    message = message,
                    onMessageChange = { message = it },
                    includeUntracked = includeUntracked,
                    onIncludeUntrackedChange = { includeUntracked = it },
                    includeIgnored = includeIgnored,
                    onIncludeIgnoredChange = { includeIgnored = it },
                    stagedOnly = stagedOnly,
                    onStagedOnlyChange = { stagedOnly = it },
                    stagedSupported = stagedSupported,
                    entries = statusEntries,
                    selectedPaths = selectedPaths,
                    onTogglePath = { entry ->
                        val next = if (entry.path in selectedPaths) {
                            selectedPaths - entry.path
                        } else {
                            selectedPaths + entry.path
                        }
                        selectedPaths = next
                        if (entry.kind == StatusKind.Untracked) includeUntracked = true
                        if (entry.kind == StatusKind.Ignored) includeIgnored = true
                    },
                    onClearPaths = { selectedPaths = emptySet() },
                    onCreated = {
                        message = ""
                        selectedPaths = emptySet()
                    },
                )
                Divider()
                StashList(
                    stashes = stashes,
                    selectedHash = selected?.hash,
                    onSelect = { selectedHash = it },
                    modifier = Modifier.weight(1f),
                )
            }
            VDivider()
            StashDetailPane(
                controller = controller,
                scope = scope,
                selected = selected,
                branchName = branchName,
                onBranchNameChange = { branchName = it },
                confirmDrop = confirmDrop,
                onConfirmDropChange = { confirmDrop = it },
                confirmPop = confirmPop,
                onConfirmPopChange = { confirmPop = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StashCreatePanel(
    controller: RepoController,
    scope: CoroutineScope,
    message: String,
    onMessageChange: (String) -> Unit,
    includeUntracked: Boolean,
    onIncludeUntrackedChange: (Boolean) -> Unit,
    includeIgnored: Boolean,
    onIncludeIgnoredChange: (Boolean) -> Unit,
    stagedOnly: Boolean,
    onStagedOnlyChange: (Boolean) -> Unit,
    stagedSupported: Boolean,
    entries: List<StatusEntry>,
    selectedPaths: Set<String>,
    onTogglePath: (StatusEntry) -> Unit,
    onClearPaths: () -> Unit,
    onCreated: () -> Unit,
) {
    val strings = LocalStrings.current
    val canCreate = !controller.busy &&
        (controller.status?.isClean == false || includeIgnored)
    var running by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.stashCreateTitle,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (selectedPaths.isEmpty()) strings.stashAllChanges else strings.stashPartialCount(selectedPaths.size),
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = if (selectedPaths.isEmpty()) Term.faint else Term.amber,
            )
        }
        Spacer(Modifier.height(5.dp))
        TermTextField(
            value = message,
            onValueChange = onMessageChange,
            placeholder = strings.stashMessagePlaceholder,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermButton(
                "u",
                strings.stashIncludeUntracked,
                active = includeUntracked,
            ) { onIncludeUntrackedChange(!includeUntracked) }
            Spacer(Modifier.width(4.dp))
            TermButton(
                "a",
                strings.stashIncludeIgnored,
                active = includeIgnored,
            ) { onIncludeIgnoredChange(!includeIgnored) }
            Spacer(Modifier.width(4.dp))
            TermButton(
                "s",
                strings.stashStaged,
                active = stagedOnly,
            ) {
                if (stagedSupported) onStagedOnlyChange(!stagedOnly)
            }
        }
        if (!stagedSupported) {
            Text(
                strings.stashStagedVersion,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.stashPartialFiles,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
            Spacer(Modifier.weight(1f))
            if (selectedPaths.isNotEmpty()) {
                Text(
                    "[clear]",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.greenDim,
                    modifier = Modifier.clickable { onClearPaths() },
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                strings.stashNoChanges,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            Column(Modifier.heightIn(max = 145.dp).verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    StashPathRow(
                        entry = entry,
                        selected = entry.path in selectedPaths,
                        onClick = { onTogglePath(entry) },
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            strings.stashPartialHint,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.height(6.dp))
        ActionButton(
            key = null,
            label = if (running) "WAIT…" else strings.stashCreate,
            enabled = canCreate && !running,
            onClick = {
                if (!canCreate || running) return@ActionButton
                running = true
                scope.launch {
                    try {
                        val created = controller.createStash(
                            StashCreateOptions(
                                message = message,
                                includeUntracked = includeUntracked,
                                includeIgnored = includeIgnored,
                                stagedOnly = stagedOnly,
                                paths = selectedPaths.toList(),
                            ),
                        )
                        if (created) {
                            onCreated()
                            controller.dismissError()
                        }
                    } finally {
                        running = false
                    }
                }
            },
        )
        controller.error?.let { text ->
            Spacer(Modifier.height(7.dp))
            ErrorBanner(text, controller::dismissError)
        }
    }
}

@Composable
private fun StashPathRow(
    entry: StatusEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (selected) "[x]" else "[ ]",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.green else Term.faint,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            entry.path,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.text else Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${entry.indexStatus}${entry.workTreeStatus}",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = when (entry.kind) {
                StatusKind.Untracked -> Term.amber
                StatusKind.Ignored -> Term.faint
                else -> Term.cyan
            },
        )
    }
}

@Composable
private fun StashList(
    stashes: List<StashEntry>,
    selectedHash: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("STASHES", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text("${stashes.size}", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        if (stashes.isEmpty()) {
            Text(
                strings.stashEmpty,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(10.dp),
            )
            return
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            stashes.forEach { entry ->
                val selected = entry.hash == selectedHash
                Column(
                    Modifier.fillMaxWidth()
                        .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
                        .clickable { onSelect(entry.hash) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.selector,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Term.green else Term.cyan,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            formatStashTime(entry.time),
                            fontSize = 7.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                    Text(
                        entry.message.ifBlank { strings.stashUntitled },
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (selected) Term.white else Term.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
            }
        }
    }
}

@Composable
private fun StashDetailPane(
    controller: RepoController,
    scope: CoroutineScope,
    selected: StashEntry?,
    branchName: String,
    onBranchNameChange: (String) -> Unit,
    confirmDrop: Boolean,
    onConfirmDropChange: (Boolean) -> Unit,
    confirmPop: Boolean,
    onConfirmPopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var runningAction by remember(selected?.selector) { mutableStateOf<String?>(null) }
    val busy = controller.busy || runningAction != null
    val detail = controller.stashDetail?.takeIf { it.selector == selected?.selector }
    val detailError = controller.stashDetailError

    Column(modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        if (selected == null) {
            Text(
                strings.stashSelectHint,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            return
        }

        Text(
            "${selected.selector}  ${selected.hash.take(7)}",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.green,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            selected.message.ifBlank { strings.stashUntitled },
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                key = null,
                label = strings.stashApply,
                enabled = !busy,
                onClick = {
                    onConfirmPopChange(false)
                    onConfirmDropChange(false)
                    runningAction = "apply"
                    scope.launch {
                        try {
                            controller.applyStash(selected.selector, pop = false)
                        } finally {
                            runningAction = null
                        }
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = if (confirmPop) strings.stashPopConfirm else strings.stashPop,
                danger = true,
                enabled = !busy,
                onClick = {
                    if (!confirmPop) {
                        onConfirmDropChange(false)
                        onConfirmPopChange(true)
                        return@ActionButton
                    }
                    onConfirmPopChange(false)
                    runningAction = "pop"
                    scope.launch {
                        try {
                            controller.applyStash(selected.selector, pop = true)
                        } finally {
                            runningAction = null
                        }
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = if (confirmDrop) strings.stashDropConfirm else strings.stashDrop,
                danger = true,
                enabled = !busy,
                onClick = {
                    if (!confirmDrop) {
                        onConfirmPopChange(false)
                        onConfirmDropChange(true)
                        return@ActionButton
                    }
                    onConfirmDropChange(false)
                    runningAction = "drop"
                    scope.launch {
                        try {
                            controller.dropStash(selected.selector)
                        } finally {
                            runningAction = null
                        }
                    }
                },
            )
        }
        if (confirmPop || confirmDrop) {
            Text(
                if (confirmPop) strings.stashPopWarning else strings.stashDropWarning,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.stashBranchName,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
            Spacer(Modifier.width(6.dp))
            TermTextField(
                value = branchName,
                onValueChange = onBranchNameChange,
                placeholder = strings.stashBranchPlaceholder,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = strings.stashBranchAction,
                enabled = !busy && branchName.isNotBlank() &&
                    controller.status?.isClean == true,
                onClick = {
                    onConfirmDropChange(false)
                    onConfirmPopChange(false)
                    runningAction = "branch"
                    scope.launch {
                        try {
                            controller.branchFromStash(selected.selector, branchName)
                        } finally {
                            runningAction = null
                        }
                    }
                },
            )
        }
        Text(
            strings.stashBranchHint,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.padding(top = 3.dp),
        )

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel(strings.stashFiles)
        if (controller.stashDetailLoading) {
            Text(
                strings.stashShowLoading,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        } else if (detailError != null) {
            Spacer(Modifier.height(4.dp))
            ErrorBanner(detailError, controller::dismissStashDetailError)
        } else if (detail != null) {
            if (detail.files.isEmpty()) {
                Text(
                    strings.stashShowEmpty,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            } else {
                Column(Modifier.heightIn(max = 110.dp).verticalScroll(rememberScrollState())) {
                    detail.files.forEach { change ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                change.status,
                                fontSize = 7.5.sp,
                                fontFamily = Term.mono,
                                color = Term.cyan,
                                modifier = Modifier.width(42.dp),
                            )
                            Text(
                                change.path,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (detail.truncated) {
                Text(
                    strings.stashShowTruncated,
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.amber,
                )
            }
            Spacer(Modifier.height(7.dp))
            SectionLabel(strings.stashPatch)
            Spacer(Modifier.height(3.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).background(Term.black)
                    .border(1.dp, Term.border)
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp),
            ) {
                Text(
                    detail.patch.ifBlank { strings.stashShowEmpty },
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.text,
                )
            }
        }
    }
}

private fun formatStashTime(seconds: Long): String {
    if (seconds <= 0L) return "—"
    return STASH_TIME.format(Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()))
}

private val STASH_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
