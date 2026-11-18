package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MockupApp() {
    MockupHost { zoomLabel, onZoom -> GitChanTitleBar(zoomLabel, onZoom) }
}

@Composable
fun MockupHost(titleBar: @Composable (String, (Int) -> Unit) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var amend by remember { mutableStateOf(false) }
    var sign by remember { mutableStateOf(true) }
    var repoIndex by remember { mutableStateOf(0) }
    var commandLine by remember { mutableStateOf<String?>(null) }
    var undoVisible by remember { mutableStateOf(false) }
    val zoomSteps = listOf(0.9f, 1f, 1.1f, 1.25f, 1.5f)
    var zoomIndex by remember { mutableStateOf(1) }
    val baseDensity = LocalDensity.current

    LaunchedEffect(commandLine) {
        if (commandLine != null) {
            delay(3400)
            commandLine = null
        }
    }
    LaunchedEffect(undoVisible) {
        if (undoVisible) {
            delay(14000)
            undoVisible = false
        }
    }

    Box(Modifier.fillMaxSize().background(Term.bg)) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * zoomSteps[zoomIndex],
                fontScale = baseDensity.fontScale,
            ),
        ) {
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            titleBar("${(zoomSteps[zoomIndex] * 100).roundToInt()}%") { delta ->
                zoomIndex = (zoomIndex + delta).coerceIn(0, zoomSteps.lastIndex)
            }
            Toolbar { commandLine = it }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Sidebar(
                    modifier = Modifier.width(198.dp).fillMaxHeight(),
                    selectedRepo = repoIndex,
                    onSelectRepo = { repoIndex = it },
                )
                VDivider()
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    RepoLine()
                    TabStrip(tab) { tab = it }
                    Divider()
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            0 -> Row(Modifier.fillMaxSize()) {
                                CommitTable(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    selectedIndex = selected,
                                    onSelect = { selected = it },
                                )
                                VDivider()
                                InspectPanel(
                                    modifier = Modifier.width(352.dp).fillMaxHeight(),
                                    commit = mockCommits[selected],
                                    onDrop = { undoVisible = true },
                                    onCommand = { commandLine = it },
                                )
                            }
                            1 -> ChangesPanel(Modifier.fillMaxSize())
                            2 -> ReflogPanel(Modifier.fillMaxSize())
                            else -> ConsolePanel(Modifier.fillMaxSize())
                        }
                        if (undoVisible) {
                            UndoBar(
                                Modifier.align(Alignment.BottomStart).padding(10.dp),
                                onUndo = {
                                    undoVisible = false
                                    commandLine = "git reset --hard HEAD@{1}"
                                },
                                onDismiss = { undoVisible = false },
                            )
                        }
                    }
                    CommandBar(
                        message = message,
                        onMessageChange = { message = it },
                        amend = amend,
                        onAmend = { amend = !amend },
                        sign = sign,
                        onSign = { sign = !sign },
                        onCommit = { commandLine = "git commit -m \"$message\"" },
                    )
                }
            }
            StatusBar()
        }
        commandLine?.let {
            Toast(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
        }
        }
        }
    }
}

@Composable
fun GitChanTitleBar(
    zoomLabel: String,
    onZoom: (Int) -> Unit,
    onHome: () -> Unit = {},
    context: String = "root@localhost:~/dev/mybeautifulgit-chan",
    version: String = "v0.4.0",
    onZoomReset: () -> Unit = {},
    controls: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(26.dp).background(Term.panel)
            .a11yFocusGroup()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clickable { onHome() }
                .a11yButton("Open start screen")
                .a11yFocusRing(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("█", fontSize = 11.sp, fontFamily = Term.mono, color = Term.green)
            Text(
                " GIT-CHAN ",
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.black,
                modifier = Modifier.background(Term.green),
            )
            Text(" $version", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.dim)
        }
        Text("  │  ", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.border)
        Text(
            context,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(24.dp)
                .clickable { onZoom(-1) }
                .a11yButton("Zoom out")
                .a11yFocusRing(),
            contentAlignment = Alignment.Center,
        ) {
            Text("[-]", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.dim)
        }
        Text(
            " $zoomLabel ",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.green,
            modifier = Modifier
                .clickable { onZoomReset() }
                .a11yButton("Reset interface scale")
                .a11yState("Current scale $zoomLabel")
                .a11yFocusRing()
                .padding(horizontal = 3.dp, vertical = 4.dp),
        )
        Box(
            Modifier.size(24.dp)
                .clickable { onZoom(1) }
                .a11yButton("Zoom in")
                .a11yFocusRing(),
            contentAlignment = Alignment.Center,
        ) {
            Text("[+]", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.dim)
        }
        Spacer(Modifier.width(12.dp))
        Text("xterm-256color", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
        Spacer(Modifier.width(8.dp))
        controls()
    }
    Divider()
}

@Composable
private fun Toolbar(onCommand: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(30.dp).background(Term.bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("git", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.faint)
        Text(">", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.green)
        Spacer(Modifier.width(2.dp))
        mockActions.forEach { action ->
            TermButton(
                hotkey = action.key,
                label = action.label,
                badge = action.badge,
                onClick = { onCommand(action.command) },
            )
        }
        Spacer(Modifier.weight(1f))
        Text("↑2", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.amber)
        Spacer(Modifier.width(8.dp))
        Text("↓3", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.cyan)
        Spacer(Modifier.width(10.dp))
        Text("✓ clean", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.green)
    }
    Divider()
}

@Composable
private fun RepoLine() {
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(Term.panelAlt)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("*", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
        Text(" main", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
        Spacer(Modifier.width(10.dp))
        Text("✓ clean", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.dim)
        Spacer(Modifier.width(10.dp))
        Text("↑2 ↓3", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.amber)
        Spacer(Modifier.width(10.dp))
        Text("HEAD → 9c2f1ab", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.faint)
        Spacer(Modifier.weight(1f))
        Text("128 commits", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
    }
}

@Composable
private fun TabStrip(current: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(26.dp).background(Term.panel)) {
        TabItem(1, "LOG", active = current == 0) { onSelect(0) }
        TabItem(2, "CHANGES", badge = "4", active = current == 1) { onSelect(1) }
        TabItem(3, "REFLOG", active = current == 2) { onSelect(2) }
        TabItem(4, "TERMINAL", active = current == 3) { onSelect(3) }
        Spacer(Modifier.weight(1f))
        Text(
            "^K palette",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
        )
    }
}

@Composable
private fun Sidebar(modifier: Modifier, selectedRepo: Int, onSelectRepo: (Int) -> Unit) {
    Column(modifier.background(Term.panel)) {
        SidebarHeader("TREE", "~/dev")
        Row(Modifier.height(20.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("~/dev", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.dim)
        }
        mockRepos.forEachIndexed { index, repo ->
            val selected = index == selectedRepo
            Row(
                Modifier.fillMaxWidth().height(20.dp)
                    .background(if (selected) Term.green else Term.panel)
                    .clickable { onSelectRepo(index) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (index == mockRepos.lastIndex) "└ " else "├ ",
                    fontSize = 9.5.sp,
                    fontFamily = Term.mono,
                    color = if (selected) Term.black else Term.faint,
                )
                Text(
                    repo.name,
                    fontSize = 9.5.sp,
                    fontFamily = Term.mono,
                    color = if (selected) Term.black else Term.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Divider()
        SidebarHeader("BRANCHES", "4")
        mockBranches.forEach { branch ->
            Row(
                Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (branch.current) "*" else " ",
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    color = Term.green,
                )
                Text(
                    " ${branch.name}",
                    fontSize = 9.5.sp,
                    fontFamily = Term.mono,
                    color = if (branch.current) Term.green else Term.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (branch.ahead > 0) {
                    Text("↑${branch.ahead}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.amber)
                }
                if (branch.behind > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text("↓${branch.behind}", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.cyan)
                }
            }
        }
        Divider()
        SidebarHeader("REMOTES", "1")
        Row(Modifier.height(20.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("origin", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.cyan)
            Spacer(Modifier.width(6.dp))
            Text(
                "github.com/baconana/git-chan",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Divider()
        SidebarHeader("STASH", "2")
        listOf("wip: ui mockup", "before rebase").forEach { entry ->
            Row(Modifier.height(20.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("~ $entry", fontSize = 9.sp, fontFamily = Term.mono, color = Term.dim, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        Divider()
        Column(Modifier.padding(8.dp)) {
            Text("3 backup refs", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.greenDim)
            Spacer(Modifier.height(3.dp))
            Text("reflog: 27 entries", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.height(3.dp))
            Text("git 2.47.0 · jvm 25", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
    }
}

@Composable
private fun SidebarHeader(title: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth().height(18.dp).background(Term.raised).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 8.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text(trailing, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
    }
}

@Composable
private fun CommandBar(
    message: String,
    onMessageChange: (String) -> Unit,
    amend: Boolean,
    onAmend: () -> Unit,
    sign: Boolean,
    onSign: () -> Unit,
    onCommit: () -> Unit,
) {
    Divider()
    Row(
        Modifier.fillMaxWidth().height(38.dp).background(Term.panel)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("3 staged", fontSize = 9.sp, fontFamily = Term.mono, color = Term.green)
        Spacer(Modifier.width(10.dp))
        Text("$", fontSize = 11.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.green)
        Spacer(Modifier.width(6.dp))
        Text("git commit -m \"", fontSize = 10.sp, fontFamily = Term.mono, color = Term.dim)
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f)) {
            if (message.isEmpty()) {
                Text("commit message…", fontSize = 10.5.sp, fontFamily = Term.mono, color = Term.faint)
            }
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 10.5.sp, fontFamily = Term.mono, color = Term.white),
                cursorBrush = SolidColor(Term.green),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text("\"", fontSize = 10.sp, fontFamily = Term.mono, color = Term.dim)
        Spacer(Modifier.width(12.dp))
        TermButton("a", "amend", active = amend, onClick = onAmend)
        Spacer(Modifier.width(4.dp))
        TermButton("g", "gpg", active = sign, onClick = onSign)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.height(21.dp).background(Term.green).clickable { onCommit() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("[^↵] commit", fontSize = 9.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.black)
        }
    }
}

@Composable
private fun StatusBar() {
    Row(
        Modifier.fillMaxWidth().height(21.dp).background(Term.green)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(" main ", fontSize = 9.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.black)
        Text(" clean  ", fontSize = 9.sp, fontFamily = Term.mono, color = Term.black.copy(alpha = 0.8f))
        Text(" 3 staged  4 unstaged  128 commits ", fontSize = 9.sp, fontFamily = Term.mono, color = Term.black.copy(alpha = 0.8f))
        Spacer(Modifier.weight(1f))
        Text("git-chan 0.4.0   ^K palette   ? help ", fontSize = 9.sp, fontFamily = Term.mono, color = Term.black.copy(alpha = 0.8f))
    }
}

@Composable
private fun UndoBar(modifier: Modifier, onUndo: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier.background(Term.panelAlt).border(1.dp, Term.amber)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("[!]", fontSize = 10.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.amber)
        Spacer(Modifier.width(8.dp))
        Text("commit 4f2a1c9 dropped", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.text)
        Spacer(Modifier.width(8.dp))
        Text("backup ref saved", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
        Spacer(Modifier.width(14.dp))
        Text("[u] undo", fontSize = 9.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.green, modifier = Modifier.clickable { onUndo() })
        Spacer(Modifier.width(10.dp))
        Text("[x]", fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.clickable { onDismiss() })
    }
}

@Composable
private fun Toast(command: String, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Term.panelAlt).border(1.dp, Term.green)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$ ", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
        Text(command, fontSize = 10.sp, fontFamily = Term.mono, color = Term.white, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text("↩", fontSize = 10.sp, fontFamily = Term.mono, color = Term.faint)
    }
}
