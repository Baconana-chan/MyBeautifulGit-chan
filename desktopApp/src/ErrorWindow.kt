package org.baconana.mybeautifulgit_chan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.baconana.mybeautifulgit_chan.app.ActionButton
import org.baconana.mybeautifulgit_chan.domain.AppVersion
import org.baconana.mybeautifulgit_chan.app.a11yButton
import org.baconana.mybeautifulgit_chan.app.a11yFocusRing
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


/**
 * Last-resort UI shown when a composable throws. It intentionally does not use
 * the application's CompositionLocals (indication, density, strings) so that a
 * broken app-level provider cannot break the crash report itself.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppErrorWindow(error: Throwable, onClose: () -> Unit) {
    val report = remember(error) { buildErrorReport(error) }
    Window(
        onCloseRequest = onClose,
        state = rememberWindowState(
            size = DpSize(980.dp, 700.dp),
            position = WindowPosition(Alignment.Center),
        ),
        title = "MyBeautifulGit-chan — UI crash",
        decoration = WindowDecoration.Undecorated(6.dp),
        resizable = false,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(Unit) {
            window.isAlwaysOnTop = true
        }
        CrashReportWindow(window, error, report, onClose)
    }
}

@Composable
private fun CrashReportWindow(
    window: java.awt.Window,
    error: Throwable,
    report: String,
    onClose: () -> Unit,
) {
    var copied by remember(error) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Term.bg)
            .border(1.dp, Term.borderHi),
    ) {
        Box(Modifier.fillMaxWidth().windowDrag(window)) {
            Row(
                Modifier.fillMaxWidth().height(28.dp).background(Term.panel)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("█", fontSize = 11.sp, fontFamily = Term.mono, color = Term.red)
                Text(
                    " GIT-CHAN ",
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.black,
                    modifier = Modifier.background(Term.red),
                )
                Text(
                    " CRASH REPORT",
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.red,
                )
                Spacer(Modifier.weight(1f))
                Text("ESC close", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.width(30.dp).fillMaxHeight()
                        .clickable(onClick = onClose)
                        .a11yButton("Close crash report")
                        .a11yFocusRing(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", fontSize = 12.sp, fontFamily = Term.mono, color = Term.red)
                }
            }
        }
        Divider()

        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text(
                "UNHANDLED UI EXCEPTION",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.red,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                error.message?.ifBlank { error::class.java.simpleName }
                    ?: error::class.java.simpleName,
                fontSize = 13.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.white,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "The window was hidden to protect the app. The full report is below and can be copied.",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.height(13.dp))

            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(Term.panel)
                    .border(1.dp, Term.border),
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    SelectionContainer(Modifier.fillMaxWidth()) {
                        Text(
                            report,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            lineHeight = 13.sp,
                            color = Term.text,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "terminal crash boundary",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                Spacer(Modifier.width(12.dp))
                if (copied) {
                    Text(
                        "copied to clipboard",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.green,
                    )
                }
                Spacer(Modifier.weight(1f))
                ActionButton(
                    key = null,
                    label = if (copied) "COPIED" else "COPY REPORT",
                    primary = true,
                    onClick = {
                        DesktopActions.copy(report)
                        copied = true
                    },
                )
                Spacer(Modifier.width(8.dp))
                ActionButton(
                    key = null,
                    label = "CLOSE",
                    danger = true,
                    onClick = onClose,
                )
            }
        }
    }
}

private fun buildErrorReport(error: Throwable): String = buildString {
    appendLine("Git-chan crash report")
    appendLine("version: ${AppVersion.CURRENT_TAG}")
    appendLine("time: ${OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
    appendLine(
        "os: ${System.getProperty("os.name")} ${System.getProperty("os.version")} " +
            "(${System.getProperty("os.arch")})",
    )
    appendLine(
        "java: ${System.getProperty("java.version")} / " +
            "${System.getProperty("java.vendor")}",
    )
    appendLine("error: ${error::class.qualifiedName ?: error::class.java.name}")
    appendLine("message: ${error.message ?: "(no message)"}")
    appendLine()
    append(error.stackTraceToString())
}
