package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun TermTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null,
    secret: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.background(Term.black).border(1.dp, Term.border)
            .border(1.dp, if (focused) Term.focus else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 10.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 10.sp, fontFamily = Term.mono, color = Term.white),
            cursorBrush = SolidColor(Term.green),
            visualTransformation = if (secret) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = placeholder },
        )
    }
}

/**
 * Multiline input that grows with its content and scrolls after [maxLines].
 * Enter inserts a newline; Ctrl/Cmd+Enter calls [onSubmit].
 */
@Composable
fun TermGrowingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 5,
    onSubmit: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val lineHeight = 14.dp
    val verticalPadding = 5.dp
    val firstLine = minLines.coerceAtLeast(1)
    val lastLine = maxLines.coerceAtLeast(firstLine)
    Box(
        modifier.heightIn(
            min = lineHeight * firstLine + verticalPadding * 2,
            max = lineHeight * lastLine + verticalPadding * 2,
        )
            .background(Term.black)
            .border(1.dp, Term.border)
            .border(1.dp, if (focused) Term.focus else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 8.dp, vertical = verticalPadding),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 10.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            textStyle = TextStyle(
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontFamily = Term.mono,
                color = Term.white,
            ),
            cursorBrush = SolidColor(Term.green),
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (event.type == KeyEventType.KeyDown &&
                        enter &&
                        (event.isCtrlPressed || event.isMetaPressed) &&
                        onSubmit != null
                    ) {
                        onSubmit()
                        true
                    } else {
                        false
                    }
                }
                .semantics { contentDescription = placeholder },
        )
    }
}

@Composable
fun TermTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.background(Term.black).border(1.dp, Term.border)
            .border(1.dp, if (focused) Term.focus else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            textStyle = TextStyle(fontSize = 9.5.sp, fontFamily = Term.mono, color = Term.white),
            cursorBrush = SolidColor(Term.green),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxSize()
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = placeholder },
        )
    }
}

@Composable
fun ActionButton(
    key: String?,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> Term.panelAlt
        primary -> Term.green
        else -> Term.panelAlt
    }
    val border = when {
        !enabled -> Term.border
        primary -> Term.green
        danger -> Term.red
        else -> Term.borderHi
    }
    val textColor = when {
        !enabled -> Term.faint
        primary -> Term.black
        danger -> Term.red
        else -> Term.text
    }
    val keyColor = when {
        !enabled -> Term.faint
        primary -> Term.black
        danger -> Term.red
        else -> Term.green
    }

    Row(
        modifier.height(24.dp)
            .background(background)
            .border(1.dp, border)
            .clickable(enabled = enabled) { onClick() }
            .a11yButton(label = label, enabled = enabled)
            .a11yFocusRing(enabled = enabled)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (key != null) {
            Text(
                "[$key]",
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = keyColor,
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            label,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = textColor,
        )
    }
}

@Composable
fun ErrorBanner(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val presented = remember(text, strings) { presentGitError(text, strings) }
    var stderrOpen by remember(text) { mutableStateOf(false) }
    var copied by remember(text) { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth()
            .background(Term.red.copy(alpha = 0.12f))
            .border(1.dp, Term.red.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .a11yLiveRegion(assertive = true)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                "[!]",
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.red,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    presented?.message ?: text,
                    fontSize = 9.5.sp,
                    fontFamily = Term.mono,
                    color = Term.text,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                val preview = presented?.stderr
                    ?.lineSequence()
                    ?.filter { it.isNotBlank() }
                    ?.take(3)
                    ?.joinToString("\n")
                if (!preview.isNullOrBlank()) {
                    Text(
                        preview,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "[x]",
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier
                    .sizeIn(minWidth = 24.dp, minHeight = 20.dp)
                    .clickable { onDismiss() }
                    .a11yButton(strings.a11yDismiss)
                    .a11yFocusRing()
                    .padding(horizontal = 3.dp),
            )
        }

        if (presented != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$ ${presented.command} · exit ${presented.exitCode}",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                ErrorBannerAction(
                    label = if (copied) "[copied]" else "[${strings.toastCopy}]",
                    onClick = {
                        DesktopActions.copy(presented.command)
                        copied = true
                    },
                )
                if (presented.stderr != null) {
                    Spacer(Modifier.width(8.dp))
                    ErrorBannerAction(
                        label = if (stderrOpen) "[hide]" else "[stderr]",
                        onClick = { stderrOpen = !stderrOpen },
                    )
                }
            }
        }

        val stderr = presented?.stderr
        if (stderrOpen && stderr != null) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(start = 28.dp, end = 10.dp, bottom = 9.dp)
                    .background(Term.panelAlt)
                    .border(1.dp, Term.border),
            ) {
                Text(
                    "stderr",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.SemiBold,
                    color = Term.red,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
                Box(
                    Modifier.fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    SelectionContainer {
                        Text(
                            stderr,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            lineHeight = 12.sp,
                            color = Term.dim,
                        )
                    }
                }
                if (presented.truncated) {
                    Text(
                        strings.gitOutputTruncated,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBannerAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.greenDim,
        modifier = Modifier
            .sizeIn(minWidth = 24.dp, minHeight = 20.dp)
            .clickable(onClick = onClick)
            .a11yButton(label)
            .a11yFocusRing()
            .padding(horizontal = 3.dp),
    )
}

@Composable
fun DialogShell(
    title: String,
    onDismiss: () -> Unit,
    width: Dp = 580.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val strings = LocalStrings.current
    val dialogFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { dialogFocus.requestFocus() } }
    Column(
        Modifier.width(width)
            .background(Term.panel)
            .border(1.dp, Term.greenDim)
            .semantics {
                dialog()
                paneTitle = title
            }
            .a11yFocusGroup(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(27.dp).background(Term.raised)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
                modifier = Modifier.a11yHeading(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "[esc]",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier
                    .focusRequester(dialogFocus)
                    .clickable { onDismiss() }
                    .a11yButton(strings.a11yClose)
                    .a11yFocusRing()
                    .padding(2.dp),
            )
        }
        Divider()
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

/** Компактный переключатель языка для шапок и футеров. */
@Composable
fun LanguageSwitch(language: Language, onSelect: (Language) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.a11yFocusGroup()) {
        Text("lang", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        Spacer(Modifier.width(5.dp))
        Language.entries.forEach { candidate ->
            Text(
                "[${candidate.label}]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (candidate == language) Term.greenDim else Term.faint,
                modifier = Modifier
                    .clickable { onSelect(candidate) }
                    .a11yButton(
                        label = candidate.label,
                        selected = candidate == language,
                        role = Role.RadioButton,
                    )
                    .a11yFocusRing()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
    }
}
