package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

/** One runnable command exposed by the palette. */
data class PaletteCommand(
    val id: String,
    val title: String,
    val category: String,
    val description: String = "",
    val command: String = "",
    val shortcut: String = "",
    val keywords: String = "",
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val run: () -> Unit,
)

/**
 * Per-window palette state. A repository screen publishes its commands with
 * [setScreenCommands]; the DesktopApp keeps a few application-wide commands.
 */
class CommandPaletteController {
    var isOpen by mutableStateOf(false)
        private set

    private var baseItems by mutableStateOf<List<PaletteCommand>>(emptyList())
    private var screenItems by mutableStateOf<List<PaletteCommand>>(emptyList())
    private var screenOwner: Any? = null

    val commands: List<PaletteCommand>
        get() = baseItems + screenItems

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun toggle() {
        isOpen = !isOpen
    }

    fun setBaseCommands(commands: List<PaletteCommand>) {
        baseItems = commands
    }

    fun setScreenCommands(commands: List<PaletteCommand>) {
        screenOwner = null
        screenItems = commands
    }

    /** Registers commands for one screen; another screen can replace them safely. */
    fun setScreenCommands(owner: Any, commands: List<PaletteCommand>) {
        screenOwner = owner
        screenItems = commands
    }

    fun clearScreenCommands() {
        screenOwner = null
        screenItems = emptyList()
    }

    /** Clears only this owner's commands, so closing an inactive tab is harmless. */
    fun clearScreenCommands(owner: Any) {
        if (screenOwner !== owner) return
        screenOwner = null
        screenItems = emptyList()
    }
}

fun isCommandPaletteShortcut(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown &&
        event.key == Key.K &&
        (event.isCtrlPressed || event.isMetaPressed)

internal fun filterPaletteCommands(
    commands: List<PaletteCommand>,
    rawQuery: String,
): List<PaletteCommand> {
    val query = rawQuery.trim().lowercase()
    if (query.isEmpty()) return commands
    val tokens = query.split(Regex("\\s+"))
    return commands.mapIndexedNotNull { index, command ->
        val title = command.title.lowercase()
        val commandText = command.command.lowercase()
        val description = command.description.lowercase()
        val category = command.category.lowercase()
        val keywords = command.keywords.lowercase()
        val haystack = "$title\n$commandText\n$description\n$category\n$keywords"
        if (!tokens.all { haystack.contains(it) }) return@mapIndexedNotNull null
        val score = when {
            title == query -> 0
            title.startsWith(query) -> 1
            title.contains(query) -> 2
            commandText.contains(query) -> 3
            description.contains(query) -> 4
            category.contains(query) -> 5
            else -> 6
        }
        PaletteRanked(score, index, command)
    }.sortedWith(compareBy<PaletteRanked> { it.rank }.thenBy { it.index })
        .map { it.command }
}

private data class PaletteRanked(
    val rank: Int,
    val index: Int,
    val command: PaletteCommand,
)

@Composable
fun CommandPaletteHost(controller: CommandPaletteController) {
    if (!controller.isOpen) return
    val strings = LocalStrings.current
    val commands = controller.commands
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val filtered = remember(commands, query) { filterPaletteCommands(commands, query) }
    val activeIndex = selected.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val shortcut = remember { commandPaletteShortcutLabel() }
    var inputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(query) {
        selected = 0
    }
    LaunchedEffect(filtered.size) {
        selected = selected.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(activeIndex, filtered.size) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(activeIndex)
    }

    fun run(command: PaletteCommand) {
        if (!command.enabled) return
        controller.close()
        command.run()
    }

    fun move(delta: Int) {
        if (filtered.isEmpty()) return
        selected = (activeIndex + delta).coerceIn(0, filtered.lastIndex)
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = controller::close,
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.padding(top = 74.dp)
                .fillMaxWidth(0.72f)
                .widthIn(min = 560.dp, max = 820.dp)
                .heightIn(max = 540.dp)
                .background(Term.panel)
                .border(1.dp, Term.borderHi)
                .semantics {
                    dialog()
                    paneTitle = "Command palette"
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Escape -> {
                            controller.close()
                            true
                        }

                        Key.DirectionDown -> {
                            move(1)
                            true
                        }

                        Key.DirectionUp -> {
                            move(-1)
                            true
                        }

                        Key.Enter, Key.NumPadEnter -> {
                            filtered.getOrNull(activeIndex)?.let(::run)
                            true
                        }

                        Key.Tab -> {
                            if (event.isShiftPressed) move(-1) else move(1)
                            true
                        }

                        else -> false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Row(
                Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "COMMAND PALETTE",
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.green,
                    letterSpacing = 1.sp,
                    modifier = Modifier.a11yHeading(),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${filtered.size}/${commands.size}",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.weight(1f))
                Text("[ESC] close", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
                Spacer(Modifier.width(10.dp))
                Text(shortcut, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.dim)
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().height(42.dp).background(Term.black)
                    .border(1.dp, if (inputFocused) Term.focus else Color.Transparent)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(">", fontSize = 12.sp, fontFamily = Term.mono, color = Term.green)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Type a command, git command, or description…",
                            fontSize = 11.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = Term.mono,
                            color = Term.white,
                        ),
                        cursorBrush = SolidColor(Term.green),
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { inputFocused = it.isFocused }
                            .semantics {
                                contentDescription = strings.a11ySearchCommands
                                stateDescription = filtered.getOrNull(activeIndex)?.title.orEmpty()
                            },
                    )
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "clear",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.clickable { query = "" },
                    )
                }
            }
            Divider()
            if (filtered.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().height(120.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "NO MATCHING COMMANDS",
                        fontSize = 10.sp,
                        fontFamily = Term.mono,
                        fontWeight = FontWeight.Bold,
                        color = Term.dim,
                    )
                    Text(
                        "Try a different name or git command.",
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f), state = listState) {
                    itemsIndexed(filtered, key = { _, command -> command.id }) { index, command ->
                        PaletteRow(
                            command = command,
                            selected = index == activeIndex,
                            onClick = { run(command) },
                        )
                    }
                }
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().height(25.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "↑↓ navigate   ↵ run   esc close",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "dangerous actions stay marked in red",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    command: PaletteCommand,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val described = buildString {
        append(command.title)
        if (command.description.isNotBlank()) append(". ").append(command.description)
        if (command.command.isNotBlank()) append(". ").append(command.command)
        if (!command.enabled) {
            append(". ")
            append(command.disabledReason ?: "unavailable")
        }
    }
    val titleColor = when {
        !command.enabled -> Term.faint
        command.danger -> Term.red
        selected -> Term.white
        else -> Term.text
    }
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) Term.raised else Color.Transparent)
            .alpha(if (command.enabled) 1f else 0.65f)
            .clickable(enabled = command.enabled, onClick = onClick)
            .a11yFocusRing(enabled = command.enabled)
            .clearAndSetSemantics {
                contentDescription = described
                role = Role.Button
                this.selected = selected
                if (!command.enabled) disabled()
                if (command.enabled) {
                    onClick(label = command.title) {
                        onClick()
                        true
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (command.danger) "!" else if (selected) ">" else " ",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = if (command.danger) Term.red else Term.green,
            modifier = Modifier.width(13.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    command.title,
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    command.category.uppercase(),
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    maxLines = 1,
                )
                if (!command.enabled && command.disabledReason != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        command.disabledReason,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (command.description.isNotBlank()) {
                Text(
                    command.description,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (command.command.isNotBlank()) {
                Text(
                    command.command,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = if (command.danger) Term.red.copy(alpha = 0.85f) else Term.greenDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (command.shortcut.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(
                command.shortcut,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }
}

fun commandPaletteShortcutLabel(): String =
    if (System.getProperty("os.name").orEmpty().lowercase().contains("mac")) "⌘K" else "Ctrl+K"
