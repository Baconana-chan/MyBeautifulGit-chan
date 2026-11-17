package org.baconana.mybeautifulgit_chan.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.PartialCloneFilter
import org.baconana.mybeautifulgit_chan.domain.repositoryNameFromUrl
import org.baconana.mybeautifulgit_chan.domain.validCloneTarget
import org.baconana.mybeautifulgit_chan.domain.validCloneUrl
import org.baconana.mybeautifulgit_chan.domain.validInitBranch
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitRunControl
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun CloneDialog(
    target: String,
    onTargetChange: (String) -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit,
    onClone: suspend (
        url: String,
        target: String,
        filter: PartialCloneFilter,
        control: GitRunControl,
    ) -> GitResult,
    onOpened: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var partialClone by remember { mutableStateOf(PartialCloneFilter.Full) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cancelled by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<OperationProgress?>(null) }
    var cloneStartedAt by remember { mutableStateOf(0L) }
    var cloneNow by remember { mutableStateOf(System.currentTimeMillis()) }
    var control by remember { mutableStateOf<GitRunControl?>(null) }
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val repositoryName = remember(url) { repositoryNameFromUrl(url) }
    val resolvedTarget = remember(target, repositoryName) {
        resolveCloneTarget(target, repositoryName).orEmpty()
    }

    LaunchedEffect(busy) {
        while (busy) {
            cloneNow = System.currentTimeMillis()
            delay(200)
        }
        cloneNow = System.currentTimeMillis()
    }

    DialogShell(
        title = "CLONE REPOSITORY",
        onDismiss = { if (!busy) onDismiss() },
    ) {
        FieldLabel("URL")
        TermTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = "https://github.com/user/repo.git",
        )
        Spacer(Modifier.height(11.dp))

        FieldLabel(strings.whereLabel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermTextField(
                value = target,
                onValueChange = onTargetChange,
                placeholder = strings.clonePlaceholder,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(7.dp))
            ActionButton(
                key = null,
                label = "CHOOSE",
                enabled = !busy,
                onClick = onBrowse,
            )
        }
        if (target.isNotBlank() && resolvedTarget.isBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                strings.cloneInvalidTarget,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.red,
            )
        } else if (resolvedTarget.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                strings.cloneWillCreate(resolvedTarget),
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }

        Spacer(Modifier.height(13.dp))
        FieldLabel("PARTIAL CLONE")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PartialCloneFilter.entries.forEach { filter ->
                ActionButton(
                    key = null,
                    label = filter.label,
                    primary = filter == partialClone,
                    enabled = !busy,
                    onClick = { partialClone = filter },
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            strings.clonePartialHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )

        if (busy) {
            Spacer(Modifier.height(11.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (stopping) "STOPPING…" else "CLONING…",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.amber,
                )
                Text(
                    buildString {
                        progress?.stage?.let { append(it).append(' ') }
                        progress?.percent?.let { append(it).append('%') }
                    }.ifBlank { "…" },
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(150.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatOperationDuration(cloneNow - cloneStartedAt),
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
            }
            Spacer(Modifier.height(5.dp))
            CloneProgressBar(progress)
        }
        if (cancelled && !busy) {
            Spacer(Modifier.height(10.dp))
            Text(
                strings.cloneCancelled,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            ErrorBanner(it, onDismiss = { error = null })
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End) {
            Spacer(Modifier.weight(1f))
            ActionButton(
                key = null,
                label = if (busy) "STOP" else "CANCEL",
                enabled = !stopping,
                onClick = {
                    if (busy) {
                        stopping = true
                        control?.cancel()
                    } else {
                        onDismiss()
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = when {
                    stopping -> "STOPPING…"
                    busy -> "CLONING…"
                    else -> "CLONE"
                },
                primary = true,
                enabled = !busy && url.isNotBlank() && target.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (!validCloneUrl(url)) {
                            error = strings.cloneInvalidUrl
                            return@launch
                        }
                        val cleanUrl = url.trim()
                        val destination = resolveCloneTarget(target, repositoryName)
                        if (destination == null) {
                            error = strings.cloneInvalidTarget
                            return@launch
                        }
                        cancelled = false
                        stopping = false
                        error = null
                        progress = null
                        val runBuffer = StringBuilder()
                        val runControl = GitRunControl { chunk ->
                            runBuffer.append(chunk)
                            if (runBuffer.length > MAX_CLONE_PROGRESS_CHARS) {
                                runBuffer.delete(
                                    0,
                                    runBuffer.length - MAX_CLONE_PROGRESS_CHARS,
                                )
                            }
                            val parsed = parseOperationProgress(runBuffer.toString())
                            if (parsed != null) {
                                Snapshot.withMutableSnapshot {
                                    if (progress != parsed) progress = parsed
                                }
                            }
                        }
                        control = runControl
                        cloneStartedAt = System.currentTimeMillis()
                        cloneNow = cloneStartedAt
                        busy = true
                        val result: Result<GitResult> = try {
                            Result.success(
                                onClone(cleanUrl, destination, partialClone, runControl),
                            )
                        } catch (failure: Exception) {
                            Result.failure(failure)
                        }
                        busy = false
                        stopping = false
                        control = null
                        result.fold(
                            onSuccess = { completed ->
                                when {
                                    completed.cancelled -> {
                                        cancelled = true
                                        progress = null
                                    }
                                    completed.ok -> {
                                        progress = null
                                        onOpened(destination)
                                    }
                                    else -> {
                                        progress = null
                                        error = listOfNotNull(
                                            completed.authFailureText(strings),
                                            GitCommandException(completed).message
                                                ?: completed.stderr.trim().ifBlank { null },
                                        ).joinToString("\n\n")
                                    }
                                }
                            },
                            onFailure = {
                                progress = null
                                error = listOfNotNull(
                                    it.authFailureText(strings),
                                    it.message ?: it.toString(),
                                ).joinToString("\n\n")
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CloneProgressBar(progress: OperationProgress?) {
    val percent = progress?.percent
    Box(
        Modifier.fillMaxWidth().height(4.dp).background(Term.border).clipToBounds(),
    ) {
        if (percent != null) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0.01f, 1f))
                    .background(Term.green),
            )
        } else {
            val transition = rememberInfiniteTransition(label = "clone-progress")
            val shift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "clone-progress-shift",
            )
            Box(
                Modifier.offset(x = (shift * 320f).dp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(Term.amber),
            )
        }
    }
}

private const val MAX_CLONE_PROGRESS_CHARS = 8 * 1024

@Composable
fun InitDialog(
    path: String,
    defaultBranch: String = "main",
    onPathChange: (String) -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit,
    onInit: suspend (path: String) -> Result<String>,
    onOpened: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    DialogShell(
        title = "INIT REPOSITORY",
        onDismiss = { if (!busy) onDismiss() },
    ) {
        FieldLabel(strings.folderLabel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermTextField(
                value = path,
                onValueChange = onPathChange,
                placeholder = strings.initPlaceholder,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(7.dp))
            ActionButton(
                key = null,
                label = "CHOOSE",
                enabled = !busy,
                onClick = onBrowse,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            strings.initHint(defaultBranch),
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )

        error?.let {
            Spacer(Modifier.height(10.dp))
            ErrorBanner(it, onDismiss = { error = null })
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End) {
            Spacer(Modifier.weight(1f))
            ActionButton(key = null, label = "CANCEL", enabled = !busy, onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = if (busy) "INITIALIZING…" else "INIT",
                primary = true,
                enabled = !busy && path.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (!validCloneTarget(path)) {
                            error = strings.cloneInvalidTarget
                            return@launch
                        }
                        val cleanPath = path.trim()
                        val branch = defaultBranch.trim().ifBlank { "main" }
                        if (!validInitBranch(branch)) {
                            error = strings.settingsDefaultBranchInvalid
                            return@launch
                        }
                        busy = true
                        error = null
                        val result = runCatching { onInit(cleanPath) }
                            .getOrElse { Result.failure(it) }
                        result.fold(
                            onSuccess = { created ->
                                busy = false
                                onOpened(created)
                            },
                            onFailure = {
                                busy = false
                                error = it.message ?: it.toString()
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.dim,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(5.dp))
}

