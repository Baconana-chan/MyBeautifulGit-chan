package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommitTable(modifier: Modifier, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("GRAPH", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(58.dp))
            Text("COMMIT", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(60.dp))
            Text("MESSAGE", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.weight(1f))
            Text("AUTHOR", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(58.dp))
            Text("AGE", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(34.dp))
            Text("±", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(74.dp))
        }
        Divider()
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(mockCommits) { index, commit ->
                CommitRow(commit, index == selectedIndex) { onSelect(index) }
                if (index != mockCommits.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().height(18.dp).background(Term.raised)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("[j/k] move", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(10.dp))
            Text("[enter] inspect", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(10.dp))
            Text("[space] stage", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(10.dp))
            Text("[d] drop", fontSize = 8.sp, fontFamily = Term.mono, color = Term.red.copy(alpha = 0.75f))
            Spacer(Modifier.weight(1f))
            Text("12/128", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(8.dp))
            Text("[/] filter", fontSize = 8.sp, fontFamily = Term.mono, color = Term.greenDim)
        }
    }
}

@Composable
private fun CommitRow(commit: MockCommit, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .background(if (selected) Term.green.copy(alpha = 0.12f) else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(2.dp).fillMaxHeight()
                .background(if (selected) Term.green else androidx.compose.ui.graphics.Color.Transparent),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            graphText(commit),
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            maxLines = 1,
            modifier = Modifier.width(58.dp),
        )
        Text(
            commit.shortHash,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.green else Term.faint,
            modifier = Modifier.width(60.dp),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            commit.refs.take(2).forEach { ref ->
                RefTag(ref)
                Spacer(Modifier.width(3.dp))
            }
            Text(
                commit.message,
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = if (selected) Term.white else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            commit.author,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            modifier = Modifier.width(58.dp),
            maxLines = 1,
        )
        Text(
            commit.age,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.width(34.dp),
        )
        Row(Modifier.width(74.dp), horizontalArrangement = Arrangement.End) {
            Text("+${commit.additions}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.greenDim)
            Spacer(Modifier.width(4.dp))
            Text("−${commit.deletions}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.red.copy(alpha = 0.8f))
        }
    }
}

private fun graphText(commit: MockCommit): AnnotatedString {
    val maxLane = (commit.lanes + commit.mergeLanes + commit.lane).maxOrNull() ?: 0
    val chars = CharArray(maxLane + 1) { ' ' }
    commit.lanes.forEach { if (it <= maxLane) chars[it] = '|' }
    commit.mergeLanes.forEach { if (it <= maxLane) chars[it] = '\\' }
    chars[commit.lane] = '*'
    val rendered = " " + chars.joinToString(" ")
    return buildAnnotatedString {
        rendered.forEach { ch ->
            withStyle(
                SpanStyle(
                    color = when (ch) {
                        '*' -> Term.green
                        '|' -> Term.faint
                        '\\' -> Term.faint
                        else -> Term.faint
                    },
                ),
            ) { append(ch) }
        }
    }
}

@Composable
fun InspectPanel(
    modifier: Modifier,
    commit: MockCommit,
    onDrop: () -> Unit,
    onCommand: (String) -> Unit,
) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("INSPECT", fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text(commit.shortHash, fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(8.dp))
            Text("[q]", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).padding(8.dp),
        ) {
            Text(
                commit.message,
                fontSize = 11.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.white,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(7.dp))
            LabeledValue("COMMIT", commit.shortHash)
            LabeledValue("AUTHOR", "${commit.author} <${commit.email}>")
            LabeledValue("DATE", commit.date)
            LabeledValue("PARENTS", commit.parents)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("REFS", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint, letterSpacing = 0.8.sp, modifier = Modifier.width(58.dp))
                commit.refs.forEach { ref ->
                    RefTag(ref)
                    Spacer(Modifier.width(4.dp))
                }
            }
            LabeledValue("STAT", "+${commit.additions} −${commit.deletions} · ${commit.files} files", Term.greenDim)
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(6.dp))
            SectionLabel("FILES (${commit.files})")
            Spacer(Modifier.height(4.dp))
            mockDetailFiles.forEach { file ->
                Row(
                    Modifier.fillMaxWidth().height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FileCodeTag(file.code)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        file.path,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("+${file.added}", fontSize = 8.sp, fontFamily = Term.mono, color = Term.greenDim)
                    Spacer(Modifier.width(3.dp))
                    Text("−${file.removed}", fontSize = 8.sp, fontFamily = Term.mono, color = Term.red.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(6.dp))
            SectionLabel("ACTIONS")
            Spacer(Modifier.height(3.dp))
            InspectAction("c", "cherry-pick → main") {
                onCommand("git cherry-pick ${commit.shortHash}")
            }
            InspectAction("v", "revert") {
                onCommand("git revert --no-edit ${commit.shortHash}")
            }
            InspectAction("R", "reset --hard") {
                onCommand("git reset --hard ${commit.shortHash}")
            }
            InspectAction("d", "drop from history", danger = true, onClick = onDrop)
            Text(
                "  rebase -i ${commit.shortHash}~1 · backup ref auto",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                    .padding(horizontal = 7.dp, vertical = 6.dp),
            ) {
                Text(
                    "git rebase -i ${commit.shortHash}~1",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.greenDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "[+] refs/git-chan/backups/2026-11-12_main",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
