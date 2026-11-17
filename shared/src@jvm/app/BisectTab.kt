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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.BisectMark
import org.baconana.mybeautifulgit_chan.domain.BisectMarkKind
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider

private enum class BisectLogMode { Automation, Git }

@Composable
fun BisectTab(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    var badRef by remember(controller.repository.root) { mutableStateOf("HEAD") }
    var goodRef by remember(controller.repository.root) { mutableStateOf("") }
    var command by remember(controller.repository.root) { mutableStateOf("") }
    var logMode by remember { mutableStateOf(BisectLogMode.Automation) }
    val state = controller.bisect
    val complete = state.complete
    val active = state.active && !complete
    val running = controller.bisectAutoRunning
    val busy = controller.busy || running
    val commits = controller.commits
    val marksByHash = remember(state.good, state.bad, state.skipped) {
        state.allMarks.associateBy { it.hash }
    }
    val logScroll = rememberScrollState()
    val rawScroll = rememberScrollState()
    val autoScroll = rememberScrollState()

    LaunchedEffect(controller.bisectAutoLog) {
        if (logMode == BisectLogMode.Automation && controller.bisectAutoLog.isNotBlank()) {
            autoScroll.animateScrollTo(autoScroll.maxValue)
        }
    }

    fun markCurrent(good: Boolean) {
        if (busy) return
        scope.launch { controller.markBisectCurrent(good) }
    }

    fun reset() {
        if (busy) return
        scope.launch { controller.resetBisect() }
    }

    Column(Modifier.fillMaxSize().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.bisectTitle,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    complete -> "[${strings.bisectStatusComplete}]"
                    active -> "[${strings.bisectStatusActive}]"
                    else -> "[${strings.bisectStatusIdle}]"
                },
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = when {
                    complete -> Term.red
                    active -> Term.amber
                    else -> Term.faint
                },
            )
            Spacer(Modifier.weight(1f))
            state.currentHash.takeIf { it.isNotBlank() }?.let { hash ->
                Text(
                    hash.take(7),
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.cyan,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                "[${strings.bisectRefresh}]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (busy) Term.faint else Term.greenDim,
                modifier = Modifier.clickable(enabled = !busy) {
                    scope.launch { controller.refresh() }
                },
            )
        }
        Divider()

        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(425.dp).fillMaxHeight()
                    .verticalScroll(logScroll)
                    .padding(horizontal = 9.dp, vertical = 8.dp),
            ) {
                if (!active && !complete) {
                    SectionLabel(strings.bisectStartTitle)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.bisectBadRef,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(3.dp))
                    TermTextField(
                        value = badRef,
                        onValueChange = { badRef = it },
                        placeholder = strings.bisectBadPlaceholder,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.bisectGoodRef,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(3.dp))
                    TermTextField(
                        value = goodRef,
                        onValueChange = { goodRef = it },
                        placeholder = strings.bisectGoodPlaceholder,
                    )
                    Spacer(Modifier.height(7.dp))
                    ActionButton(
                        null,
                        strings.bisectStart,
                        primary = true,
                        enabled = !busy && badRef.isNotBlank() && goodRef.isNotBlank(),
                    ) {
                        if (!busy) {
                            scope.launch { controller.startBisect(badRef, goodRef) }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.bisectStartHint,
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    SectionLabel(strings.bisectCurrent)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.currentHash.ifBlank { "—" },
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = if (complete) Term.red else Term.cyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.currentSubject.isNotBlank()) {
                        Text(
                            state.currentSubject,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.originalRef.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${strings.bisectOriginal}: ${state.originalRef}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.dim,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionButton(
                            null,
                            strings.bisectMarkGood,
                            enabled = active && !busy,
                        ) { markCurrent(true) }
                        ActionButton(
                            null,
                            strings.bisectMarkBad,
                            enabled = active && !busy,
                        ) { markCurrent(false) }
                        ActionButton(
                            null,
                            strings.bisectSkip,
                            enabled = active && !busy,
                        ) {
                            scope.launch { controller.skipBisect() }
                        }
                        ActionButton(
                            null,
                            strings.bisectReset,
                            enabled = !busy && (active || complete || state.log.isNotBlank()),
                        ) { reset() }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.bisectResetHint,
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    complete.let { found ->
                        if (found) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(Term.red.copy(alpha = 0.12f))
                                    .border(1.dp, Term.red.copy(alpha = 0.55f))
                                    .padding(7.dp),
                            ) {
                                Column {
                                    Text(
                                        "FIRST BAD",
                                        fontSize = 8.sp,
                                        fontFamily = Term.mono,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Term.red,
                                    )
                                    state.firstBad?.let { mark ->
                                        Text(
                                            "${mark.shortHash} ${mark.subject}",
                                            fontSize = 8.5.sp,
                                            fontFamily = Term.mono,
                                            color = Term.text,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                SectionLabel(strings.bisectCommand)
                Spacer(Modifier.height(4.dp))
                TermTextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = strings.bisectCommandPlaceholder,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (running) {
                        ActionButton(null, strings.bisectStop, primary = true) {
                            controller.stopBisectAutomation()
                        }
                    } else {
                        ActionButton(
                            null,
                            strings.bisectRun,
                            primary = true,
                            enabled = active && !busy && command.isNotBlank(),
                        ) {
                            scope.launch { controller.runBisectAutomation(command) }
                        }
                    }
                    if (controller.bisectAutoLog.isNotBlank()) {
                        Text(
                            "[clear]",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.clickable {
                                controller.clearBisectAutomationLog()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (running) strings.bisectRunning else strings.bisectCommandHint,
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = if (running) Term.amber else Term.faint,
                )

                Spacer(Modifier.height(10.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                SectionLabel("${strings.bisectMarksTitle} (${state.allMarks.size})")
                Spacer(Modifier.height(4.dp))
                if (state.allMarks.isEmpty()) {
                    Text(
                        strings.bisectNoMarks,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 260.dp)
                            .border(1.dp, Term.border)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                    ) {
                        state.allMarks.forEach { mark ->
                            MarkRow(mark)
                        }
                    }
                }
            }

            VDivider()

            Column(Modifier.weight(1f).fillMaxHeight().padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(strings.bisectVisualization)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${commits.size} commits",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (commits.isEmpty()) {
                    Text(
                        strings.bisectNoCommits,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(0.58f)
                            .border(1.dp, Term.border),
                    ) {
                        items(commits, key = { it.hash }) { commit ->
                            BisectCommitRow(
                                hash = commit.hash,
                                shortHash = commit.shortHash,
                                subject = commit.subject,
                                mark = marksByHash[commit.hash],
                                current = commit.hash.equals(state.currentHash, ignoreCase = true),
                                firstBad = state.firstBad?.hash.equals(commit.hash, ignoreCase = true),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        if (logMode == BisectLogMode.Automation) {
                            strings.bisectAutoLog
                        } else {
                            strings.bisectLogTitle
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    TermButton(
                        hotkey = "a",
                        label = "AUTO",
                        active = logMode == BisectLogMode.Automation,
                    ) { logMode = BisectLogMode.Automation }
                    Spacer(Modifier.width(4.dp))
                    TermButton(
                        hotkey = "g",
                        label = "GIT LOG",
                        active = logMode == BisectLogMode.Git,
                    ) { logMode = BisectLogMode.Git }
                }
                controller.bisectAutoResult?.let { result ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        result,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = if (result.contains("bad", ignoreCase = true)) Term.red else Term.greenDim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                val text = if (logMode == BisectLogMode.Automation) {
                    controller.bisectAutoLog
                } else {
                    state.log
                }
                if (text.isBlank()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (logMode == BisectLogMode.Automation) {
                                strings.bisectAutoEmpty
                            } else {
                                strings.bisectNoState
                            },
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).background(Term.black)
                            .border(1.dp, Term.border)
                            .verticalScroll(
                                if (logMode == BisectLogMode.Automation) autoScroll else rawScroll,
                            )
                            .padding(6.dp),
                    ) {
                        Text(
                            text,
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

@Composable
private fun MarkRow(mark: BisectMark) {
    val color = when (mark.kind) {
        BisectMarkKind.Good -> Term.green
        BisectMarkKind.Bad -> Term.red
        BisectMarkKind.Skip -> Term.amber
    }
    val label = when (mark.kind) {
        BisectMarkKind.Good -> "GOOD"
        BisectMarkKind.Bad -> "BAD"
        BisectMarkKind.Skip -> "SKIP"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "[$label]",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = color,
            modifier = Modifier.width(52.dp),
        )
        Text(
            mark.shortHash,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            mark.subject,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BisectCommitRow(
    hash: String,
    shortHash: String,
    subject: String,
    mark: BisectMark?,
    current: Boolean,
    firstBad: Boolean,
) {
    val label = buildString {
        if (firstBad) append("[FIRST]")
        if (current) append("[CUR]")
        when (mark?.kind) {
            BisectMarkKind.Good -> append("[GOOD]")
            BisectMarkKind.Bad -> append("[BAD]")
            BisectMarkKind.Skip -> append("[SKIP]")
            null -> Unit
        }
    }.ifBlank { "[ ]" }
    val color = when {
        firstBad -> Term.red
        current -> Term.cyan
        mark?.kind == BisectMarkKind.Good -> Term.green
        mark?.kind == BisectMarkKind.Bad -> Term.red
        mark?.kind == BisectMarkKind.Skip -> Term.amber
        else -> Term.faint
    }
    Row(
        Modifier.fillMaxWidth()
            .background(if (current) Term.cyan.copy(alpha = 0.08f) else Term.panel)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = color,
            modifier = Modifier.width(112.dp),
        )
        Text(
            shortHash,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            subject,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (current) Term.text else Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (current) {
            Spacer(Modifier.width(6.dp))
            Text(
                "HEAD",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
            )
        }
    }
}
