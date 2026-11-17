package org.baconana.mybeautifulgit_chan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.baconana.mybeautifulgit_chan.app.CommandPaletteController
import org.baconana.mybeautifulgit_chan.app.DesktopApp
import org.baconana.mybeautifulgit_chan.app.GitRuntime
import org.baconana.mybeautifulgit_chan.app.a11yButton
import org.baconana.mybeautifulgit_chan.app.a11yFocusRing
import org.baconana.mybeautifulgit_chan.app.isCommandPaletteShortcut
import org.baconana.mybeautifulgit_chan.app.WorktreeWindow
import org.baconana.mybeautifulgit_chan.app.WorktreeWindowSpec
import org.baconana.mybeautifulgit_chan.app.ZoomController
import org.baconana.mybeautifulgit_chan.domain.AppVersion
import org.baconana.mybeautifulgit_chan.domain.WindowHitRect
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.platform.AppSettings
import org.baconana.mybeautifulgit_chan.ui.mockup.GitChanTitleBar
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.awt.Component
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point
import java.awt.SystemTray
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val settings = remember { AppSettings() }
    var appVisible by remember { mutableStateOf(true) }
    val onQuit: () -> Unit = { exitApplication() }
    val onCloseWindow: () -> Unit = {
        if (settings.loadCloseToTray() && trayAvailable()) {
            appVisible = false
        } else {
            onQuit()
        }
    }
    val zoom = remember { ZoomController() }
    val commandPalette = remember { CommandPaletteController() }
    val nativeTitleBar = remember { NativeWindowChrome.useNativeTitleBar() }
    val state = rememberWindowState(
        size = DpSize(1360.dp, 860.dp),
        position = WindowPosition(Alignment.Center),
    )
    var worktreeWindows by remember { mutableStateOf<List<WorktreeWindowSpec>>(emptyList()) }
    var nextWorktreeWindowId by remember { mutableStateOf(1L) }
    var fatalError by remember { mutableStateOf<Throwable?>(null) }
    val errorHandlerFactory = remember {
        WindowExceptionHandlerFactory { _ ->
            WindowExceptionHandler { error ->
                System.err.println("Unhandled UI exception: ${error.message}")
                error.printStackTrace()
                fatalError = error
            }
        }
    }

    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides errorHandlerFactory) {
        Window(
            onCloseRequest = onCloseWindow,
            state = state,
            visible = fatalError == null && appVisible,
            title = "MyBeautifulGit-chan",
            decoration = if (nativeTitleBar) {
                WindowDecoration.SystemDefault
            } else {
                WindowDecoration.Undecorated(6.dp)
            },
            onPreviewKeyEvent = { event ->
                handleCommandPaletteShortcut(event, commandPalette) ||
                    handleZoomShortcut(event, zoom)
            },
        ) {
            var droppedPath by remember { mutableStateOf<String?>(null) }
            var maximizeBounds by remember { mutableStateOf<WindowHitRect?>(null) }
            var chromeHandle by remember { mutableStateOf<WindowChromeHandle?>(null) }

            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(1024, 640)
                installFolderDropTarget(window) { path -> droppedPath = path }
                window.contentPane?.let { content ->
                    installFolderDropTarget(content) { path -> droppedPath = path }
                }
                if (!nativeTitleBar) {
                    repeat(30) {
                        if (chromeHandle == null) {
                            chromeHandle = NativeWindowChrome.installNow(window) { maximizeBounds }
                        }
                        if (chromeHandle != null) return@LaunchedEffect
                        delay(50)
                    }
                }
            }

            LaunchedEffect(chromeHandle) {
                val handle = chromeHandle ?: return@LaunchedEffect
                while (true) {
                    delay(500)
                    handle.refresh()
                }
            }

            LaunchedEffect(appVisible) {
                if (appVisible) window.toFront()
            }

            DisposableEffect(Unit) {
                onDispose {
                    chromeHandle?.dispose()
                    chromeHandle = null
                }
            }

            DesktopApp(
                zoom = zoom,
                commandPalette = commandPalette,
                showCustomTitleBar = !nativeTitleBar,
                titleBar = { zoomLabel, onZoom ->
                    Box(Modifier.fillMaxWidth().windowDrag(window)) {
                        GitChanTitleBar(
                            zoomLabel = zoomLabel,
                            onZoom = onZoom,
                            context = "root@localhost:~",
                            version = AppVersion.CURRENT_TAG,
                            onZoomReset = { zoom.reset() },
                        ) {
                            WindowCaptionButtons(
                                onClose = onCloseWindow,
                                onMaximizeBounds = { maximizeBounds = it },
                            )
                        }
                    }
                },
                onQuit = onQuit,
                droppedPath = droppedPath,
                onDropConsumed = { droppedPath = null },
                onOpenWorktreeWindow = { runtime, path, language ->
                    if (worktreeWindows.none { sameWorktreePath(it.path, path) }) {
                        worktreeWindows = worktreeWindows + WorktreeWindowSpec(
                            id = nextWorktreeWindowId++,
                            runtime = runtime,
                            path = path,
                            language = language,
                        )
                    }
                },
            )
        }

        if (!appVisible && fatalError == null && trayAvailable()) {
            val trayStrings = settings.loadLanguage(
                Language.fromCode(System.getProperty("user.language")),
            ).strings
            Tray(
                icon = GitChanTrayIcon,
                tooltip = "MyBeautifulGit-chan",
                onAction = { appVisible = true },
                menu = {
                    Item(trayStrings.open, onClick = { appVisible = true })
                    Separator()
                    Item(trayStrings.trayQuit, onClick = onQuit)
                },
            )
        }

        worktreeWindows.forEach { spec ->
            key(spec.id) {
                val worktreePalette = remember(spec.id) { CommandPaletteController() }
                Window(
                    onCloseRequest = {
                        worktreeWindows = worktreeWindows.filterNot { it.id == spec.id }
                    },
                    state = rememberWindowState(
                        size = DpSize(1360.dp, 860.dp),
                        position = WindowPosition(Alignment.Center),
                    ),
                    visible = fatalError == null && appVisible,
                    title = "MyBeautifulGit-chan — ${File(spec.path).name}",
                    onPreviewKeyEvent = { event ->
                        handleCommandPaletteShortcut(event, worktreePalette) ||
                            handleZoomShortcut(event, zoom)
                    },
                ) {
                    LaunchedEffect(Unit) {
                        window.minimumSize = Dimension(1024, 640)
                    }
                    WorktreeWindow(
                        spec = spec,
                        onClose = {
                            worktreeWindows = worktreeWindows.filterNot { it.id == spec.id }
                        },
                        zoom = zoom,
                        commandPalette = worktreePalette,
                    )
                }
            }
        }

        fatalError?.let { error ->
            AppErrorWindow(error = error, onClose = onQuit)
        }
    }
}

private fun handleCommandPaletteShortcut(
    event: KeyEvent,
    palette: CommandPaletteController,
): Boolean {
    if (!isCommandPaletteShortcut(event)) return false
    palette.toggle()
    return true
}

private fun handleZoomShortcut(event: KeyEvent, zoom: ZoomController): Boolean {
    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return false
    return when (event.key) {
        Key.Equals, Key.Plus -> {
            zoom.step(1)
            true
        }

        Key.Minus -> {
            zoom.step(-1)
            true
        }

        Key.Zero -> {
            zoom.reset()
            true
        }

        else -> false
    }
}

private fun trayAvailable(): Boolean =
    runCatching { SystemTray.isSupported() }.getOrDefault(false)

private fun sameWorktreePath(first: String, second: String): Boolean {
    val a = runCatching { File(first).canonicalFile }.getOrElse { File(first).absoluteFile.normalize() }
    val b = runCatching { File(second).canonicalFile }.getOrElse { File(second).absoluteFile.normalize() }
    return a.path.equals(
        b.path,
        ignoreCase = System.getProperty("os.name").orEmpty().lowercase().contains("win"),
    )
}

private fun installFolderDropTarget(component: Component, onPath: (String) -> Unit) {
    component.dropTarget = object : DropTarget() {
        override fun dragEnter(dtde: DropTargetDragEvent) = acceptOrReject(dtde)

        override fun dragOver(dtde: DropTargetDragEvent) = acceptOrReject(dtde)

        override fun dropActionChanged(dtde: DropTargetDragEvent) = acceptOrReject(dtde)

        override fun drop(dtde: DropTargetDropEvent) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY)
            val path = runCatching {
                @Suppress("UNCHECKED_CAST")
                val files = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                files.firstOrNull()?.path
            }.getOrNull()
            if (path != null) onPath(path)
            dtde.dropComplete(path != null)
        }

        private fun acceptOrReject(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                dtde.rejectDrag()
            }
        }
    }
}

@Composable
private fun FrameWindowScope.WindowCaptionButtons(
    onClose: () -> Unit,
    onMaximizeBounds: (WindowHitRect?) -> Unit,
) {
    val frame = window
    val windowInfo = LocalWindowInfo.current
    var restoreBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    Row(Modifier.fillMaxHeight()) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(Term.border))
        CaptionButton("–", description = "Minimize window") { frame.extendedState = Frame.ICONIFIED }
        CaptionButton(
            label = "□",
            description = if (restoreBounds == null) "Maximize window" else "Restore window",
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val size = windowInfo.containerSize
                if (size.width <= 0 || size.height <= 0) return@onGloballyPositioned
                val rect = coordinates.boundsInWindow()
                onMaximizeBounds(
                    WindowHitRect(
                        left = rect.left / size.width,
                        top = rect.top / size.height,
                        right = rect.right / size.width,
                        bottom = rect.bottom / size.height,
                    ),
                )
            },
        ) {
            val saved = restoreBounds
            if (saved == null) {
                // Оконная максимизация по рабочей области: таскбар и панель задач остаются.
                restoreBounds = frame.bounds
                frame.bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
            } else {
                frame.bounds = saved
                restoreBounds = null
            }
        }
        CaptionButton("×", description = "Close window", danger = true, onClick = onClose)
    }
}

@Composable
private fun CaptionButton(
    label: String,
    description: String = label,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.fillMaxHeight().width(30.dp)
            .clickable { onClick() }
            .a11yButton(description)
            .a11yFocusRing(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = if (danger) Term.red else Term.dim,
        )
    }
}

/** Terminal prompt icon used by the system tray. */
private object GitChanTrayIcon : Painter() {
    override val intrinsicSize = Size(32f, 32f)

    override fun DrawScope.onDraw() {
        val inset = size.minDimension * 0.05f
        val box = Size(size.width - inset * 2, size.height - inset * 2)
        val radius = CornerRadius(size.minDimension * 0.22f)
        drawRoundRect(
            color = Color(0xFF0E1410),
            topLeft = Offset(inset, inset),
            size = box,
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFF59E08F),
            topLeft = Offset(inset, inset),
            size = box,
            cornerRadius = radius,
            style = Stroke(width = size.minDimension * 0.09f),
        )
        val green = Color(0xFF59E08F)
        val stroke = size.minDimension * 0.11f
        drawLine(
            color = green,
            start = Offset(size.width * 0.24f, size.height * 0.28f),
            end = Offset(size.width * 0.46f, size.height * 0.50f),
            strokeWidth = stroke,
        )
        drawLine(
            color = green,
            start = Offset(size.width * 0.46f, size.height * 0.50f),
            end = Offset(size.width * 0.24f, size.height * 0.72f),
            strokeWidth = stroke,
        )
        drawLine(
            color = green,
            start = Offset(size.width * 0.55f, size.height * 0.72f),
            end = Offset(size.width * 0.76f, size.height * 0.72f),
            strokeWidth = stroke,
        )
    }
}

/**
 * Перетаскивание окна за кастомный заголовок. Считаем в экранных координатах,
 * чтобы не зависеть от DPI/масштаба дисплея.
 */
internal fun Modifier.windowDrag(window: java.awt.Window): Modifier = pointerInput(window) {
    var pointerStart = Point(0, 0)
    var windowStart = Point(0, 0)

    detectDragGestures(
        onDragStart = {
            val pointer = MouseInfo.getPointerInfo()?.location ?: Point(window.x, window.y)
            pointerStart = pointer
            windowStart = window.location
        },
        onDrag = { change, _ ->
            change.consume()
            val pointer = MouseInfo.getPointerInfo()?.location ?: pointerStart
            window.setLocation(
                windowStart.x + pointer.x - pointerStart.x,
                windowStart.y + pointer.y - pointerStart.y,
            )
        },
    )
}
