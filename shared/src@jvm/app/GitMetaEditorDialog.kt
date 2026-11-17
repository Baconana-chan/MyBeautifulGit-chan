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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.GitMetaFile
import org.baconana.mybeautifulgit_chan.domain.GitMetaIssue
import org.baconana.mybeautifulgit_chan.domain.GitMetaIssueCode
import org.baconana.mybeautifulgit_chan.domain.GitMetaIssueSeverity
import org.baconana.mybeautifulgit_chan.domain.GitMetaSpan
import org.baconana.mybeautifulgit_chan.domain.GitMetaTokenKind
import org.baconana.mybeautifulgit_chan.domain.highlightGitMeta
import org.baconana.mybeautifulgit_chan.domain.validateGitMeta
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun GitMetaEditorDialog(controller: RepoController, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(GitMetaFile.GitIgnore) }
    var content by remember { mutableStateOf("") }
    var loadedContent by remember { mutableStateOf("") }
    var exists by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loadVersion by remember { mutableStateOf(0) }

    val validation = remember(selected, content) { validateGitMeta(selected, content) }
    val dirty = content != loadedContent

    fun selectFile(file: GitMetaFile) {
        pendingAction = null
        selected = file
        content = ""
        loadedContent = ""
        exists = false
        error = null
        notice = null
        loading = true
    }

    fun requestSelect(file: GitMetaFile) {
        if (saving || loading || file == selected) return
        if (dirty) {
            pendingAction = { selectFile(file) }
        } else {
            selectFile(file)
        }
    }

    fun requestReload() {
        if (saving) return
        if (dirty) {
            pendingAction = { loadVersion += 1 }
        } else {
            pendingAction = null
            loadVersion += 1
        }
    }

    fun save() {
        if (saving || !validation.ok) {
            if (!validation.ok) error = strings.gitMetaValidationBlocked
            return
        }
        val targetFile = selected
        val targetContent = content
        scope.launch {
            saving = true
            error = null
            try {
                if (controller.saveGitMetaFile(targetFile, targetContent)) {
                    loadedContent = targetContent
                    exists = true
                    notice = strings.gitMetaSaved
                    pendingAction = null
                } else {
                    error = controller.error
                }
            } finally {
                saving = false
            }
        }
    }

    LaunchedEffect(selected, loadVersion) {
        loading = true
        error = null
        val loaded = controller.loadGitMetaFile(selected)
        if (loaded != null) {
            content = loaded.text
            loadedContent = loaded.text
            exists = loaded.exists
            notice = null
        } else {
            error = controller.error
        }
        loading = false
    }

    DialogShell(
        title = strings.gitMetaTitle,
        onDismiss = {
            if (!saving) {
                if (dirty) pendingAction = onDismiss else onDismiss()
            }
        },
        width = 1_080.dp,
    ) {
        Column(Modifier.fillMaxWidth().height(660.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GitMetaTab(
                    file = GitMetaFile.GitIgnore,
                    selected = selected == GitMetaFile.GitIgnore,
                    onClick = { requestSelect(GitMetaFile.GitIgnore) },
                )
                GitMetaTab(
                    file = GitMetaFile.GitAttributes,
                    selected = selected == GitMetaFile.GitAttributes,
                    onClick = { requestSelect(GitMetaFile.GitAttributes) },
                )
                GitMetaTab(
                    file = GitMetaFile.Mailmap,
                    selected = selected == GitMetaFile.Mailmap,
                    onClick = { requestSelect(GitMetaFile.Mailmap) },
                )
                Spacer(Modifier.weight(1f))
                ActionButton(
                    key = null,
                    label = strings.gitMetaReload,
                    enabled = !loading && !saving && !controller.busy,
                    onClick = ::requestReload,
                )
                Spacer(Modifier.width(6.dp))
                ActionButton(
                    key = null,
                    label = strings.gitMetaSave,
                    primary = true,
                    enabled = dirty && validation.ok && !loading && !saving && !controller.busy,
                    onClick = ::save,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selected.path,
                    fontSize = 10.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.green,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (exists) strings.gitMetaWorktree else strings.gitMetaMissing,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = if (exists) Term.faint else Term.amber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (loading) {
                        strings.gitMetaLoading
                    } else {
                        "${validation.errors.size} ${strings.gitMetaError} · " +
                            "${validation.warnings.size} ${strings.gitMetaWarning}"
                    },
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = when {
                        validation.errors.isNotEmpty() -> Term.red
                        validation.warnings.isNotEmpty() -> Term.amber
                        else -> Term.faint
                    },
                )
            }
            pendingAction?.let { action ->
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Term.amber.copy(alpha = 0.12f))
                        .border(1.dp, Term.amber.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.gitMetaUnsaved,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        key = null,
                        label = strings.gitMetaDiscard,
                        onClick = {
                            val discard = pendingAction
                            pendingAction = null
                            discard?.invoke()
                        },
                    )
                    Spacer(Modifier.width(5.dp))
                    ActionButton(
                        key = null,
                        label = strings.gitMetaCancel,
                        onClick = { pendingAction = null },
                    )
                }
            }
            error?.let {
                Spacer(Modifier.height(7.dp))
                ErrorBanner(it, onDismiss = { error = null })
            }
            notice?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.green,
                )
            }
            Spacer(Modifier.height(7.dp))
            Divider()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp)) {
                    TermTextArea(
                        value = content,
                        onValueChange = {
                            content = it
                            error = null
                            notice = null
                        },
                        placeholder = strings.gitMetaPlaceholder,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        visualTransformation = remember(selected) {
                            GitMetaVisualTransformation(selected)
                        },
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.gitMetaPlaceholder,
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Term.border))
                Column(
                    Modifier.width(315.dp).fillMaxHeight().padding(start = 12.dp, top = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(strings.gitMetaCheck)
                        Spacer(Modifier.weight(1f))
                        Text(
                            when {
                                validation.errors.isNotEmpty() ->
                                    "${validation.errors.size} ${strings.gitMetaError}"
                                validation.warnings.isNotEmpty() ->
                                    "${validation.warnings.size} ${strings.gitMetaWarning}"
                                else -> "0"
                            },
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = when {
                                validation.errors.isNotEmpty() -> Term.red
                                validation.warnings.isNotEmpty() -> Term.amber
                                else -> Term.green
                            },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    if (loading) {
                        Text(
                            strings.gitMetaChecking,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    } else if (validation.issues.isEmpty()) {
                        Text(
                            strings.gitMetaNoIssues,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.green,
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(validation.issues) { issue ->
                                GitMetaIssueRow(issue, strings)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitMetaTab(
    file: GitMetaFile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        file.path,
        fontSize = 9.sp,
        fontFamily = Term.mono,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Term.green else Term.faint,
        modifier = Modifier
            .background(if (selected) Term.raised else Term.panelAlt)
            .border(1.dp, if (selected) Term.greenDim else Term.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
    Spacer(Modifier.width(5.dp))
}

@Composable
private fun GitMetaIssueRow(issue: GitMetaIssue, strings: Strings) {
    val isError = issue.severity == GitMetaIssueSeverity.Error
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "${issue.line}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = if (isError) Term.red else Term.amber,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                if (isError) strings.gitMetaError else strings.gitMetaWarning,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (isError) Term.red else Term.amber,
            )
            Text(
                gitMetaIssueText(issue.code, strings),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.text,
            )
        }
    }
}

private fun gitMetaIssueText(code: GitMetaIssueCode, strings: Strings): String = when (code) {
    GitMetaIssueCode.IgnoreEmptyNegation -> strings.gitMetaIssueIgnoreEmptyNegation
    GitMetaIssueCode.IgnoreTrailingSpace -> strings.gitMetaIssueIgnoreTrailingSpace
    GitMetaIssueCode.TrailingEscape -> strings.gitMetaIssueTrailingEscape
    GitMetaIssueCode.UnclosedCharacterClass -> strings.gitMetaIssueUnclosedClass
    GitMetaIssueCode.AttributesMissing -> strings.gitMetaIssueAttributesMissing
    GitMetaIssueCode.AttributesNameInvalid -> strings.gitMetaIssueAttributesNameInvalid
    GitMetaIssueCode.AttributesDuplicate -> strings.gitMetaIssueAttributesDuplicate
    GitMetaIssueCode.AttributesConflict -> strings.gitMetaIssueAttributesConflict
    GitMetaIssueCode.MailmapEmailRequired -> strings.gitMetaIssueMailmapEmailRequired
    GitMetaIssueCode.MailmapEmailInvalid -> strings.gitMetaIssueMailmapEmailInvalid
    GitMetaIssueCode.MailmapTooManyEmails -> strings.gitMetaIssueMailmapTooManyEmails
    GitMetaIssueCode.MailmapUnexpectedText -> strings.gitMetaIssueMailmapUnexpectedText
    GitMetaIssueCode.MailmapDuplicateMapping -> strings.gitMetaIssueMailmapDuplicate
    GitMetaIssueCode.MailmapUnclosedEmail -> strings.gitMetaIssueMailmapUnclosed
}

private class GitMetaVisualTransformation(
    private val file: GitMetaFile,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotated = buildAnnotatedString {
            append(text.text)
            highlightGitMeta(file, text.text).forEach { span ->
                addGitMetaSpan(span, text.length)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun androidx.compose.ui.text.AnnotatedString.Builder.addGitMetaSpan(
        span: GitMetaSpan,
        textLength: Int,
    ) {
        val start = span.start.coerceIn(0, textLength)
        val end = span.end.coerceIn(start, textLength)
        if (start < end) addStyle(SpanStyle(color = gitMetaColor(span.kind)), start, end)
    }
}

private fun gitMetaColor(kind: GitMetaTokenKind): Color = when (kind) {
    GitMetaTokenKind.Comment -> Term.faint
    GitMetaTokenKind.Pattern -> Term.green
    GitMetaTokenKind.Negation -> Term.magenta
    GitMetaTokenKind.Attribute -> Term.amber
    GitMetaTokenKind.Value -> Term.cyan
    GitMetaTokenKind.Email -> Term.cyan
    GitMetaTokenKind.Name -> Term.text
    GitMetaTokenKind.Invalid -> Term.red
}
