package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

enum class FilterRepoMode { RemovePath, ReplaceText, RenamePath }

data class FilterRepoRequest(
    val mode: FilterRepoMode,
    val path: String = "",
    val find: String = "",
    val replacement: String = "***REMOVED***",
    val regex: Boolean = false,
    val oldPath: String = "",
    val newPath: String = "",
)

data class FilterRepoPreview(
    val command: String,
    val summary: String,
    val commitsBefore: Int,
    val commitsAfter: Int,
    val changedRefs: Int,
    val changedPaths: Int,
)

@Composable
fun FilterRepoDialog(
    commit: Commit,
    controller: RepoController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var mode by remember(commit.hash) { mutableStateOf(FilterRepoMode.RemovePath) }
    var path by remember(commit.hash) { mutableStateOf("") }
    var find by remember(commit.hash) { mutableStateOf("") }
    var replacement by remember(commit.hash) { mutableStateOf("***REMOVED***") }
    var regex by remember(commit.hash) { mutableStateOf(false) }
    var oldPath by remember(commit.hash) { mutableStateOf("") }
    var newPath by remember(commit.hash) { mutableStateOf("") }
    var preview by remember(commit.hash) { mutableStateOf<FilterRepoPreview?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    var filterAvailable by remember(commit.hash) { mutableStateOf<Boolean?>(null) }
    val busy = running || previewing || controller.busy
    val canPreview = !busy && filterAvailable == true && when (mode) {
        FilterRepoMode.RemovePath -> path.isNotBlank()
        FilterRepoMode.ReplaceText -> find.isNotBlank()
        FilterRepoMode.RenamePath -> oldPath.isNotBlank() && newPath.isNotBlank()
    }

    LaunchedEffect(commit.hash) {
        filterAvailable = controller.filterRepoAvailable()
    }

    fun invalidatePreview() {
        preview = null
        confirmed = false
        controller.dismissError()
    }

    fun currentRequest() = FilterRepoRequest(
        mode = mode,
        path = path,
        find = find,
        replacement = replacement,
        regex = regex,
        oldPath = oldPath,
        newPath = newPath,
    )

    fun selectMode(next: FilterRepoMode) {
        if (busy) return
        mode = next
        invalidatePreview()
    }

    fun startPreview() {
        if (!canPreview) return
        scope.launch {
            previewing = true
            confirmed = false
            try {
                preview = controller.filterRepoPreview(currentRequest())
            } finally {
                previewing = false
            }
        }
    }

    fun startRun() {
        if (busy || preview == null || !confirmed || filterAvailable != true) return
        scope.launch {
            running = true
            try {
                if (controller.filterRepoRewrite(currentRequest())) {
                    onDismiss()
                    onSuccess()
                }
            } finally {
                running = false
            }
        }
    }

    val displayCommand = when (mode) {
        FilterRepoMode.RemovePath ->
            "git filter-repo --force --refs <all refs except backup> --invert-paths --path " +
                quoteArg(path.trim().ifBlank { "<path>" })
        FilterRepoMode.ReplaceText ->
            "git filter-repo --force --refs <all refs except backup> " +
                "--replace-text <temporary expressions file>"
        FilterRepoMode.RenamePath ->
            "git filter-repo --force --refs <all refs except backup> --path-rename " +
                quoteArg("${oldPath.trim().ifBlank { "<old>" }}:${newPath.trim().ifBlank { "<new>" }}")
    }

    DialogShell(
        title = "${strings.filterRepoTitle} ${commit.shortHash}",
        width = 860.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Text(
            commit.subject,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.white,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            strings.filterRepoHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermButton(
                "f",
                strings.filterRepoModeRemove,
                active = mode == FilterRepoMode.RemovePath,
            ) { selectMode(FilterRepoMode.RemovePath) }
            Spacer(Modifier.width(5.dp))
            TermButton(
                "s",
                strings.filterRepoModeReplace,
                active = mode == FilterRepoMode.ReplaceText,
            ) { selectMode(FilterRepoMode.ReplaceText) }
            Spacer(Modifier.width(5.dp))
            TermButton(
                "r",
                strings.filterRepoModeRename,
                active = mode == FilterRepoMode.RenamePath,
            ) { selectMode(FilterRepoMode.RenamePath) }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
        ) {
            when (mode) {
                FilterRepoMode.RemovePath -> {
                    FilterRepoField(strings.filterRepoRemovePath)
                    TermTextField(
                        value = path,
                        onValueChange = {
                            path = it
                            invalidatePreview()
                        },
                        placeholder = "path/to/file-or-folder",
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.filterRepoRemovePathHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }

                FilterRepoMode.ReplaceText -> {
                    FilterRepoField(strings.filterRepoFind)
                    TermTextField(
                        value = find,
                        onValueChange = {
                            find = it
                            invalidatePreview()
                        },
                        placeholder = "secret or text",
                    )
                    Spacer(Modifier.height(7.dp))
                    FilterRepoField(strings.filterRepoReplacement)
                    TermTextField(
                        value = replacement,
                        onValueChange = {
                            replacement = it
                            invalidatePreview()
                        },
                        placeholder = "***REMOVED***",
                    )
                    Spacer(Modifier.height(6.dp))
                    TermButton("x", strings.filterRepoRegex, active = regex) {
                        regex = !regex
                        invalidatePreview()
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.filterRepoReplaceHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }

                FilterRepoMode.RenamePath -> {
                    FilterRepoField(strings.filterRepoOldPath)
                    TermTextField(
                        value = oldPath,
                        onValueChange = {
                            oldPath = it
                            invalidatePreview()
                        },
                        placeholder = "old/path",
                    )
                    Spacer(Modifier.height(7.dp))
                    FilterRepoField(strings.filterRepoNewPath)
                    TermTextField(
                        value = newPath,
                        onValueChange = {
                            newPath = it
                            invalidatePreview()
                        },
                        placeholder = "new/path",
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.filterRepoRenameHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    key = null,
                    label = if (previewing) {
                        strings.filterRepoPreviewRunning
                    } else {
                        strings.filterRepoPreview
                    },
                    enabled = canPreview,
                    onClick = { startPreview() },
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    strings.filterRepoPreviewHint,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }

            if (filterAvailable == null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    strings.filterRepoChecking,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            } else if (filterAvailable == false) {
                Spacer(Modifier.height(7.dp))
                Text(
                    strings.deleteCommitFilterMissing,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.red,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    strings.deleteCommitFilterInstall,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.amber,
                )
            }

            preview?.let { result ->
                Spacer(Modifier.height(9.dp))
                SectionLabel(strings.filterRepoPreviewTitle)
                Spacer(Modifier.height(3.dp))
                Column(
                    Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        result.summary,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                    if (mode == FilterRepoMode.ReplaceText) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            strings.filterRepoContentPreviewHint,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$ ${result.command}",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.greenDim,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                strings.filterRepoBackupNote,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
            Spacer(Modifier.height(6.dp))
            TermButton(
                "!",
                strings.filterRepoConfirm,
                active = confirmed,
            ) {
                if (preview != null) confirmed = !confirmed
            }
        }

        Spacer(Modifier.height(9.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel("COMMAND")
        Spacer(Modifier.height(3.dp))
        Column(
            Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                "$ $displayCommand",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
            )
            Text(
                "# ${strings.filterRepoAllRefs}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }

        controller.error?.let { message ->
            Spacer(Modifier.height(9.dp))
            ErrorBanner(message, controller::dismissError)
        }

        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(
                key = null,
                label = strings.commitRewriteCancel,
                enabled = !busy,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = if (running) "WAIT…" else strings.filterRepoRun,
                danger = true,
                enabled = !busy && preview != null && confirmed && filterAvailable == true,
                onClick = { startRun() },
            )
        }
    }
}

@Composable
private fun FilterRepoField(text: String) {
    Text(
        text,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.dim,
    )
}
