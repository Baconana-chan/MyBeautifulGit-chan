package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.ui.mockup.A11yFocusIndication
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

/** Description of a worktree opened in a separate desktop window. */
data class WorktreeWindowSpec(
    val id: Long,
    val runtime: GitRuntime,
    val path: String,
    val language: Language,
)

/**
 * Minimal repository host for a secondary window. The main window owns the
 * list of worktree windows; this composable only opens and displays one path.
 */
@Composable
fun WorktreeWindow(
    spec: WorktreeWindowSpec,
    onClose: () -> Unit,
    zoom: ZoomController,
    commandPalette: CommandPaletteController,
) {
    var language by remember(spec.id) { mutableStateOf(spec.language) }
    var currentPath by remember(spec.id) { mutableStateOf(spec.path) }
    var repository by remember(spec.id) { mutableStateOf<Repository?>(null) }
    var openError by remember(spec.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath, spec.runtime) {
        repository = null
        openError = null
        val opened = withContext(Dispatchers.IO) {
            runCatching { spec.runtime.backend.open(currentPath) }
        }
        opened.fold(
            onSuccess = { repository = it },
            onFailure = { openError = it.message ?: it.toString() },
        )
    }

    SideEffect {
        commandPalette.setBaseCommands(
            listOf(
                PaletteCommand(
                    id = "worktree.zoom-in",
                    title = "Zoom in",
                    category = "VIEW",
                    description = "Increase the interface scale",
                    shortcut = "Ctrl+=",
                    keywords = "scale larger ui",
                ) { zoom.step(1) },
                PaletteCommand(
                    id = "worktree.zoom-out",
                    title = "Zoom out",
                    category = "VIEW",
                    description = "Decrease the interface scale",
                    shortcut = "Ctrl+-",
                    keywords = "scale smaller ui",
                ) { zoom.step(-1) },
                PaletteCommand(
                    id = "worktree.zoom-reset",
                    title = "Reset zoom",
                    category = "VIEW",
                    description = "Return the interface scale to 100%",
                    shortcut = "Ctrl+0",
                    keywords = "scale default ui",
                ) { zoom.reset() },
                PaletteCommand(
                    id = "worktree.close",
                    title = "Close worktree window",
                    category = "WINDOW",
                    description = "Close this window; the worktree and its files are kept",
                    keywords = "exit window",
                ) { onClose() },
            ),
        )
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalStrings provides language.strings,
        LocalIndication provides A11yFocusIndication,
        LocalDensity provides Density(
            density = baseDensity.density * zoom.scale,
            fontScale = baseDensity.fontScale,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Term.bg)) {
            val current = repository
            when {
                current != null -> RepoHost(
                    runtime = spec.runtime,
                    repository = current,
                    commandPalette = commandPalette,
                    onClose = onClose,
                    language = language,
                    onLanguageChange = { language = it },
                    onOpenWorktree = { currentPath = it },
                    onOpenWorktreeWindow = { currentPath = it },
                )

                openError != null -> Text(
                    "worktree: ${openError}",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Term.red,
                )

                else -> Text(
                    "opening worktree…",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Term.faint,
                )
            }
            CommandPaletteHost(commandPalette)
        }
    }
}
