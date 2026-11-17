package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

@Composable
fun CommitOptionsDialog(
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    DialogShell(
        title = "COMMIT OPTIONS",
        width = 720.dp,
        onDismiss = { if (!controller.busy) onDismiss() },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel("IDENTITY")
            Spacer(Modifier.height(6.dp))
            CommitOptionField(
                label = strings.repoCommitAuthor,
                value = controller.commitAuthor,
                onValueChange = { controller.commitAuthor = it },
                placeholder = strings.repoCommitIdentityHint,
            )
            Spacer(Modifier.height(7.dp))
            CommitOptionField(
                label = strings.repoCommitCommitter,
                value = controller.commitCommitter,
                onValueChange = { controller.commitCommitter = it },
                placeholder = strings.repoCommitIdentityHint,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                strings.repoCommitIdentityHint,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )

            Spacer(Modifier.height(14.dp))
            SectionLabel("OPTIONS")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.repoCommitMode,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                    modifier = Modifier.width(78.dp),
                )
                TermButton(
                    hotkey = "n",
                    label = strings.repoCommitModeNormal,
                    active = controller.commitMode == CommitMode.Normal,
                    onClick = { controller.commitMode = CommitMode.Normal },
                )
                Spacer(Modifier.width(5.dp))
                TermButton(
                    hotkey = "f",
                    label = strings.repoCommitModeFixup,
                    active = controller.commitMode == CommitMode.Fixup,
                    onClick = {
                        controller.amend = false
                        controller.commitMode = CommitMode.Fixup
                    },
                )
                Spacer(Modifier.width(5.dp))
                TermButton(
                    hotkey = "s",
                    label = strings.repoCommitModeSquash,
                    active = controller.commitMode == CommitMode.Squash,
                    onClick = {
                        controller.amend = false
                        controller.commitMode = CommitMode.Squash
                    },
                )
                Spacer(Modifier.width(12.dp))
                TermButton(
                    hotkey = "v",
                    label = strings.repoCommitNoVerify,
                    active = controller.noVerify,
                    onClick = { controller.noVerify = !controller.noVerify },
                )
            }

            if (controller.commitMode != CommitMode.Normal) {
                Spacer(Modifier.height(9.dp))
                CommitOptionField(
                    label = strings.repoCommitTarget,
                    value = controller.fixupTarget,
                    onValueChange = { controller.fixupTarget = it },
                    placeholder = strings.repoCommitTargetHint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.repoCommitTargetHint,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
                if (controller.commits.isNotEmpty()) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        strings.repoCommitRecent,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(3.dp))
                    controller.commits.take(6).forEach { commit ->
                        CommitTargetRow(commit, controller.fixupTarget) {
                            controller.fixupTarget = commit.hash
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("TEMPLATE")
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TermTextField(
                    value = controller.commitTemplatePath,
                    onValueChange = { controller.commitTemplatePath = it },
                    placeholder = ".gitmessage",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(7.dp))
                ActionButton(
                    key = null,
                    label = strings.repoCommitTemplateLoad,
                    enabled = !controller.busy && controller.commitTemplatePath.isNotBlank(),
                    onClick = { scope.launch { controller.loadCommitTemplate() } },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                strings.repoCommitTemplateHint,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )

            controller.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                ErrorBanner(message, controller::dismissError)
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ActionButton(
                    key = null,
                    label = "CLOSE",
                    enabled = !controller.busy,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun CommitOptionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Text(
        label,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.dim,
    )
    Spacer(Modifier.height(4.dp))
    TermTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
    )
}

@Composable
private fun CommitTargetRow(commit: Commit, selectedTarget: String, onClick: () -> Unit) {
    val selected = commit.hash == selectedTarget
    Row(
        Modifier.fillMaxWidth().height(21.dp)
            .padding(horizontal = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            commit.shortHash,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Term.green else Term.cyan,
            modifier = Modifier.width(62.dp),
        )
        Text(
            commit.subject,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.white else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}