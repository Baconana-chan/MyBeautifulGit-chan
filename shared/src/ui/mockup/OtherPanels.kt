package org.baconana.mybeautifulgit_chan.ui.mockup

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChangesPanel(modifier: Modifier = Modifier) {
    val files = remember { mutableStateListOf<MockFile>().also { it.addAll(mockFiles) } }
    val unstaged = files.filter { !it.staged }
    val staged = files.filter { it.staged }

    fun toggle(file: MockFile) {
        val index = files.indexOfFirst { it.path == file.path }
        if (index >= 0) files[index] = files[index].copy(staged = !files[index].staged)
    }

    Row(modifier) {
        StagingPanel(
            title = "UNSTAGED",
            hint = "[space] stage",
            files = unstaged,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onToggle = ::toggle,
        )
        VDivider()
        StagingPanel(
            title = "STAGED",
            hint = "[space] unstage",
            files = staged,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onToggle = ::toggle,
        )
        VDivider()
        DiffPanel(Modifier.weight(1.15f).fillMaxHeight())
    }
}

@Composable
private fun StagingPanel(
    title: String,
    hint: String,
    files: List<MockFile>,
    modifier: Modifier,
    onToggle: (MockFile) -> Unit,
) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Text(" (${files.size})", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text(hint, fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(files) { file ->
                FileRow(file) { onToggle(file) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
            }
        }
    }
}

@Composable
private fun FileRow(file: MockFile, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(22.dp).clickable { onClick() }
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileCodeTag(file.code)
        Spacer(Modifier.width(6.dp))
        Text(
            file.path,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        Text("+${file.added}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.greenDim)
        Spacer(Modifier.width(4.dp))
        Text("−${file.removed}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.red.copy(alpha = 0.8f))
    }
}

@Composable
private fun DiffPanel(modifier: Modifier) {
    Column(modifier.background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DIFF", fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Spacer(Modifier.width(7.dp))
            Text("src/git/CliGitBackend.kt", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.dim, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("+42 −8", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 5.dp)) {
            items(mockDiff) { line -> DiffLineRow(line) }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("git diff --cached", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text("[C] copy", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val color = when (line.kind) {
        DiffKind.Added -> Term.green
        DiffKind.Removed -> Term.red
        DiffKind.Hunk -> Term.cyan
        DiffKind.Context -> Term.dim
    }
    val bg = when (line.kind) {
        DiffKind.Added -> Term.green.copy(alpha = 0.06f)
        DiffKind.Removed -> Term.red.copy(alpha = 0.06f)
        else -> Term.black
    }
    Row(
        Modifier.fillMaxWidth().background(bg).padding(horizontal = 8.dp, vertical = 1.5.dp),
    ) {
        Text(
            line.text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
fun ReflogPanel(modifier: Modifier = Modifier) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("REFLOG", fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Text(" (27)", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text("every operation is undoable", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(mockReflog) { index, entry ->
                Row(
                    Modifier.fillMaxWidth().height(26.dp)
                        .background(if (entry.danger) Term.red.copy(alpha = 0.06f) else Term.panel)
                        .padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "HEAD@{${entry.index}}",
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = if (entry.danger) Term.red else Term.cyan,
                        modifier = Modifier.width(76.dp),
                    )
                    Text(
                        entry.action,
                        fontSize = 9.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(entry.hash, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
                    Spacer(Modifier.width(10.dp))
                    Text(entry.time, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(34.dp))
                    Text("[enter] restore", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.green)
                }
                if (index != mockReflog.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                "[+] 3 backup refs · gc.reflogExpire=90 days",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }
}

@Composable
fun ConsolePanel(modifier: Modifier = Modifier) {
    Column(modifier.background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TERMINAL", fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Text("zsh — /bin/zsh", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text("[^C] cancel", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 7.dp)) {
            items(mockConsole) { line -> ConsoleLineRow(line) }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("➜", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
            Text(" mybeautifulgit-chan ", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.cyan)
            Text("git:(main)", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.magenta)
            Text(" $ ", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
            Text("git status --porcelain=v2 -z", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.white)
            Box(Modifier.width(6.dp).height(12.dp).background(Term.green))
        }
    }
}

@Composable
private fun ConsoleLineRow(line: ConsoleLine) {
    val color = when (line.kind) {
        ConsoleKind.Prompt -> Term.green
        ConsoleKind.Comment -> Term.faint
        ConsoleKind.Output -> Term.dim
        ConsoleKind.Success -> Term.greenDim
        ConsoleKind.Hint -> Term.amber
        ConsoleKind.Error -> Term.red
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 1.dp)) {
        Text(
            line.text,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
