package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.UnreachablePlan
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

data class CommitRewriteRequest(
    val commit: Commit,
    val initialMode: RewriteMode = RewriteMode.Reword,
)

@Composable
fun CommitRewriteDialog(
    request: CommitRewriteRequest,
    controller: RepoController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val commit = request.commit
    var mode by remember(request) { mutableStateOf(request.initialMode) }
    var message by remember(request) { mutableStateOf(commit.subject) }
    var author by remember(request) {
        mutableStateOf("${commit.authorName} <${commit.authorEmail}>")
    }
    var loading by remember(request) { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var plan by remember(request) { mutableStateOf<UnreachablePlan?>(null) }
    var planLoading by remember(request) { mutableStateOf(true) }
    val busy = loading || planLoading || running || controller.busy
    val contentRewrite = commit.parents.size == 1

    LaunchedEffect(commit.hash) {
        loading = true
        try {
            controller.loadCommitMessage(commit.hash)?.let { loaded ->
                message = loaded
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(commit.hash) {
        planLoading = true
        try {
            plan = controller.unreachablePlan(commit.parents)
        } finally {
            planLoading = false
        }
    }

    fun apply() {
        if (busy) return
        scope.launch {
            running = true
            val success = when (mode) {
                RewriteMode.Reword -> controller.rewordCommit(commit, message, author)
                RewriteMode.Amend, RewriteMode.Split ->
                    controller.startCommitRewrite(commit, mode, message, author)
            }
            running = false
            if (success) {
                onDismiss()
                onSuccess()
            }
        }
    }

    DialogShell(
        title = "REWRITE ${commit.shortHash}",
        width = 680.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Text(
            commit.subject,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = Term.white,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            strings.commitRewriteHint,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.commitRewriteMode,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                modifier = Modifier.width(56.dp),
            )
            TermButton("r", strings.commitRewriteModeReword, active = mode == RewriteMode.Reword) {
                mode = RewriteMode.Reword
            }
            if (contentRewrite) {
                Spacer(Modifier.width(5.dp))
                TermButton("a", strings.commitRewriteModeAmend, active = mode == RewriteMode.Amend) {
                    mode = RewriteMode.Amend
                }
                Spacer(Modifier.width(5.dp))
                TermButton("s", strings.commitRewriteModeSplit, active = mode == RewriteMode.Split) {
                    mode = RewriteMode.Split
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            when (mode) {
                RewriteMode.Reword -> strings.commitRewriteRewordHint
                RewriteMode.Amend -> strings.commitRewriteAmendHint
                RewriteMode.Split -> strings.commitRewriteSplitHint
            },
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (mode == RewriteMode.Split) Term.dim else Term.text,
        )

        Spacer(Modifier.height(8.dp))
        SectionLabel(strings.commitRewriteLossPreview)
        Spacer(Modifier.height(4.dp))
        when {
            planLoading -> Text(
                strings.commitActionResetPreviewLoading,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            plan != null -> UnreachablePlanDetails(plan!!, strings)
        }

        if (mode != RewriteMode.Split) {
            Spacer(Modifier.height(9.dp))
            SectionLabel(strings.commitRewriteMessage)
            Spacer(Modifier.height(4.dp))
            TermTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = strings.commitRewriteMessage,
                singleLine = false,
            )
            if (loading) {
                Spacer(Modifier.height(3.dp))
                Text(
                    strings.commitRewriteLoading,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }

        Spacer(Modifier.height(9.dp))
        SectionLabel(strings.commitRewriteAuthor)
        Spacer(Modifier.height(4.dp))
        TermTextField(
            value = author,
            onValueChange = { author = it },
            placeholder = strings.commitRewriteAuthor,
        )

        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel("COMMAND")
        Spacer(Modifier.height(3.dp))
        Column(
            Modifier.fillMaxWidth().background(Term.black).border(1.dp, Term.border)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            commandPreview(mode, commit).forEach { command ->
                Text(
                    "$ $command",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.greenDim,
                )
            }
        }

        controller.error?.let { messageText ->
            Spacer(Modifier.height(9.dp))
            ErrorBanner(messageText, controller::dismissError)
        }

        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(
                key = null,
                label = "CANCEL",
                enabled = !busy,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = if (running) {
                    "WAIT…"
                } else {
                    when (mode) {
                        RewriteMode.Reword -> strings.commitRewriteStart + " REWORD"
                        RewriteMode.Amend -> strings.commitRewriteStart + " AMEND"
                        RewriteMode.Split -> strings.commitRewriteStart + " SPLIT"
                    }
                },
                primary = true,
                enabled = !busy && author.isNotBlank() &&
                    (mode == RewriteMode.Split || message.isNotBlank()),
                onClick = { apply() },
            )
        }
    }
}

private fun commandPreview(mode: RewriteMode, commit: Commit): List<String> = when (mode) {
    RewriteMode.Reword -> listOf(
        "git commit-tree ${commit.shortHash}^{tree} -p … -m …",
        "git reset --hard <new-commit>",
        "git cherry-pick ${commit.shortHash}..<old-tip>",
    )
    RewriteMode.Amend -> listOf(
        "git reset --hard ${commit.shortHash}",
        "git reset --soft ${commit.shortHash}^1",
        "git commit …  # replacement commit",
        "git cherry-pick ${commit.shortHash}..<old-tip>",
    )
    RewriteMode.Split -> listOf(
        "git reset --hard ${commit.shortHash}",
        "git reset --mixed ${commit.shortHash}^1",
        "# stage and commit the replacement pieces",
        "git cherry-pick ${commit.shortHash}..<old-tip>",
    )
}