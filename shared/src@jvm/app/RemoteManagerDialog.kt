package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.FetchOptions
import org.baconana.mybeautifulgit_chan.domain.FetchTags
import org.baconana.mybeautifulgit_chan.domain.RemoteHostKind
import org.baconana.mybeautifulgit_chan.domain.RemoteIdentityIssue
import org.baconana.mybeautifulgit_chan.domain.RemoteProfile
import org.baconana.mybeautifulgit_chan.domain.RemoteTransport
import org.baconana.mybeautifulgit_chan.domain.fetchArgs
import org.baconana.mybeautifulgit_chan.domain.redactRemoteUrl
import org.baconana.mybeautifulgit_chan.domain.urlContainsCredentials
import org.baconana.mybeautifulgit_chan.domain.validFetchDepth
import org.baconana.mybeautifulgit_chan.domain.validFetchFilter
import org.baconana.mybeautifulgit_chan.domain.validRemoteName
import org.baconana.mybeautifulgit_chan.domain.validRemoteUrl
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton

private enum class RemoteManagerMode {
    None,
    Add,
    Remove,
    EditUrl,
    Fetch,
    HeadAuto,
    HeadSet,
    HeadDelete,
}

@Composable
fun RemoteManagerDialog(
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val remotes = controller.remotes
    var selectedName by remember(controller.repository.root) {
        mutableStateOf(remotes.firstOrNull())
    }
    var mode by remember { mutableStateOf(RemoteManagerMode.None) }
    var running by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addUrl by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var fetchPrune by remember { mutableStateOf(false) }
    var fetchTags by remember { mutableStateOf(FetchTags.Default) }
    var fetchDepth by remember { mutableStateOf("") }
    var fetchFilter by remember { mutableStateOf("") }
    var headBranch by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<RemoteProfile?>(null) }
    var profileLoading by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val selected = remotes.firstOrNull { it == selectedName } ?: remotes.firstOrNull()
    val selectedHead = selected?.let { controller.remoteHead(it) }
    val busy = running || controller.busy

    LaunchedEffect(remotes) {
        if (selectedName == null) selectedName = remotes.firstOrNull()
    }

    LaunchedEffect(selected, controller.remoteUrls) {
        val name = selected
        profile = null
        profileError = null
        if (name == null) {
            profileLoading = false
            return@LaunchedEffect
        }
        profileLoading = true
        try {
            val loaded = try {
                controller.loadRemoteProfile(name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (selected == name) {
                profile = loaded
                if (loaded == null) profileError = strings.remoteProfileFailed
            }
        } finally {
            if (selected == name) profileLoading = false
        }
    }

    fun editableUrl(remote: String): String {
        val url = controller.remoteUrl(remote)
        return if (urlContainsCredentials(url)) "" else url
    }

    fun resetFields() {
        addName = ""
        addUrl = ""
        editUrl = selected?.let(::editableUrl).orEmpty()
        fetchPrune = false
        fetchTags = FetchTags.Default
        fetchDepth = ""
        fetchFilter = ""
        headBranch = selectedHead
            ?: selected?.let { remote ->
                controller.remoteBranches(remote)
                    .firstOrNull()
                    ?.shortName
                    ?.removePrefix("$remote/")
            }.orEmpty()
    }

    fun select(remote: String) {
        selectedName = remote
        mode = RemoteManagerMode.None
        resetFields()
    }

    fun openAdd() {
        mode = RemoteManagerMode.Add
        addName = ""
        addUrl = ""
    }

    fun openEditUrl() {
        if (selected == null) return
        mode = RemoteManagerMode.EditUrl
        editUrl = editableUrl(selected)
    }

    fun openFetch() {
        if (selected == null) return
        mode = RemoteManagerMode.Fetch
        fetchPrune = false
        fetchTags = FetchTags.Default
        fetchDepth = ""
        fetchFilter = ""
    }

    fun openHeadAuto() {
        if (selected == null) return
        mode = RemoteManagerMode.HeadAuto
    }

    fun openHeadSet() {
        if (selected == null) return
        mode = RemoteManagerMode.HeadSet
        headBranch = selectedHead
            ?: controller.remoteBranches(selected).firstOrNull()?.shortName
                ?.removePrefix("$selected/")
                .orEmpty()
    }

    fun openHeadDelete() {
        if (selected == null) return
        mode = RemoteManagerMode.HeadDelete
    }

    fun openRemove() {
        if (selected == null) return
        mode = RemoteManagerMode.Remove
    }

    fun execute(block: suspend () -> Boolean) {
        if (busy) return
        running = true
        scope.launch {
            val ok = block()
            running = false
            if (ok) {
                selectedName = when (mode) {
                    RemoteManagerMode.Add -> addName.trim()
                    RemoteManagerMode.Remove -> null
                    else -> selectedName
                }
                mode = RemoteManagerMode.None
                resetFields()
            }
        }
    }

    val fetchOptions = FetchOptions(
        prune = fetchPrune,
        tags = fetchTags,
        depth = fetchDepth,
        filter = fetchFilter,
    )
    val fetchCommand = displayCommandLine("git", fetchArgs(selected.orEmpty(), fetchOptions))

    DialogShell(
        title = strings.remoteManagerTitle,
        width = 780.dp,
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(max = 540.dp)) {
            Column(
                Modifier.width(300.dp).heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(strings.remoteManagerTitle)
                    Spacer(Modifier.weight(1f))
                    ActionButton(null, strings.remoteAdd, enabled = !busy) { openAdd() }
                }
                if (remotes.isEmpty()) {
                    RemoteManagerEmpty(strings.remoteManagerNoRemotes)
                } else {
                    remotes.forEach { remote ->
                        RemoteManagerRow(
                            remote = remote,
                            url = redactRemoteUrl(controller.remoteUrl(remote)),
                            head = controller.remoteHead(remote),
                            selected = selected == remote,
                            onClick = { select(remote) },
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f).heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (selected == null) {
                    Text(
                        strings.remoteManagerSelect,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                } else {
                    Text(
                        selected,
                        fontSize = 11.sp,
                        fontFamily = Term.mono,
                        fontWeight = FontWeight.Bold,
                        color = Term.amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        redactRemoteUrl(controller.remoteUrl(selected)).ifBlank { "—" },
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.cyan,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${strings.remoteHead}: ${selectedHead ?: strings.remoteHeadUnset}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = if (selectedHead == null) Term.faint else Term.green,
                    )
                    Spacer(Modifier.height(8.dp))
                    RemoteIdentityPanel(profile, profileLoading, profileError)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionButton(null, strings.remoteFetch, enabled = !busy) { openFetch() }
                        ActionButton(null, strings.remoteEditUrl, enabled = !busy) { openEditUrl() }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionButton(null, strings.remoteHeadAuto, enabled = !busy) { openHeadAuto() }
                        ActionButton(null, strings.remoteHeadSet, enabled = !busy) { openHeadSet() }
                        ActionButton(null, strings.remoteHeadDelete, enabled = !busy) { openHeadDelete() }
                    }
                    Spacer(Modifier.height(5.dp))
                    ActionButton(null, strings.remoteRemove, enabled = !busy) { openRemove() }
                    Spacer(Modifier.height(8.dp))
                    RemotePreflightPanel(controller)
                }

                when (mode) {
                    RemoteManagerMode.Add -> ManagerForm(
                        title = strings.remoteAdd,
                        hint = strings.remoteAddHint,
                        commands = listOf(
                            displayCommandLine(
                                "git",
                                listOf("remote", "add", addName.trim(), addUrl.trim()),
                            ),
                        ),
                        confirm = "ADD",
                        enabled = !busy &&
                            validRemoteName(addName.trim()) &&
                            validRemoteUrl(addUrl),
                        onCancel = { if (!busy) mode = RemoteManagerMode.None },
                        onConfirm = { execute { controller.addRemote(addName, addUrl) } },
                    ) {
                        TermTextField(
                            value = addName,
                            onValueChange = { addName = it },
                            placeholder = strings.remoteName,
                        )
                        Spacer(Modifier.height(5.dp))
                        TermTextField(
                            value = addUrl,
                            onValueChange = { addUrl = it },
                            placeholder = strings.remoteUrl,
                        )
                        if (addUrl.isNotBlank() && !validRemoteUrl(addUrl)) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                strings.remoteInvalidUrl,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }

                    RemoteManagerMode.EditUrl -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteEditUrl,
                            hint = strings.remoteEditUrlHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    listOf("remote", "set-url", selected, editUrl.trim()),
                                ),
                            ),
                            confirm = "SET URL",
                            enabled = !busy && validRemoteUrl(editUrl),
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = { execute { controller.editRemoteUrl(selected, editUrl) } },
                        ) {
                            TermTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                placeholder = strings.remoteUrl,
                            )
                            if (urlContainsCredentials(controller.remoteUrl(selected))) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    strings.remoteUrlCredentialsHidden,
                                    fontSize = 8.sp,
                                    fontFamily = Term.mono,
                                    color = Term.amber,
                                )
                            }
                            if (editUrl.isNotBlank() && !validRemoteUrl(editUrl)) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    strings.remoteInvalidUrl,
                                    fontSize = 8.5.sp,
                                    fontFamily = Term.mono,
                                    color = Term.red,
                                )
                            }
                        }
                    }

                    RemoteManagerMode.Fetch -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteFetch,
                            hint = strings.remoteFetchHint,
                            commands = listOf(fetchCommand),
                            confirm = "FETCH",
                            enabled = !busy &&
                                validFetchDepth(fetchDepth) &&
                                validFetchFilter(fetchFilter),
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = {
                                execute { controller.fetchRemote(selected, fetchOptions) }
                            },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TermButton("p", strings.remoteFetchPrune, active = fetchPrune) {
                                    fetchPrune = !fetchPrune
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "git fetch --prune",
                                    fontSize = 8.sp,
                                    fontFamily = Term.mono,
                                    color = Term.faint,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                TermButton(
                                    "t",
                                    strings.remoteFetchTagsDefault,
                                    active = fetchTags == FetchTags.Default,
                                ) { fetchTags = FetchTags.Default }
                                TermButton(
                                    "t",
                                    strings.remoteFetchTags,
                                    active = fetchTags == FetchTags.Tags,
                                ) { fetchTags = FetchTags.Tags }
                                TermButton(
                                    "t",
                                    strings.remoteFetchNoTags,
                                    active = fetchTags == FetchTags.NoTags,
                                ) { fetchTags = FetchTags.NoTags }
                            }
                            Spacer(Modifier.height(6.dp))
                            TermTextField(
                                value = fetchDepth,
                                onValueChange = { fetchDepth = it },
                                placeholder = strings.remoteFetchDepth,
                            )
                            if (!validFetchDepth(fetchDepth)) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    strings.remoteInvalidDepth,
                                    fontSize = 8.sp,
                                    fontFamily = Term.mono,
                                    color = Term.red,
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            TermTextField(
                                value = fetchFilter,
                                onValueChange = { fetchFilter = it },
                                placeholder = strings.remoteFetchFilter,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                strings.remoteFetchFilterHint,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = if (validFetchFilter(fetchFilter)) Term.faint else Term.red,
                            )
                        }
                    }

                    RemoteManagerMode.HeadAuto -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteHeadAuto,
                            hint = strings.remoteHeadAutoHint,
                            commands = listOf(
                                displayCommandLine("git", listOf("remote", "set-head", selected, "--auto")),
                            ),
                            confirm = "AUTO",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = { execute { controller.setRemoteHeadAuto(selected) } },
                        ) { }
                    }

                    RemoteManagerMode.HeadSet -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteHeadSet,
                            hint = strings.remoteHeadSetHint,
                            commands = listOf(
                                displayCommandLine(
                                    "git",
                                    listOf("remote", "set-head", selected, headBranch.trim()),
                                ),
                            ),
                            confirm = "SET",
                            enabled = !busy && headBranch.isNotBlank(),
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = {
                                execute { controller.setRemoteHead(selected, headBranch) }
                            },
                        ) {
                            TermTextField(
                                value = headBranch,
                                onValueChange = { headBranch = it },
                                placeholder = strings.remoteHeadBranch,
                            )
                        }
                    }

                    RemoteManagerMode.HeadDelete -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteHeadDelete,
                            hint = strings.remoteHeadDeleteHint,
                            commands = listOf(
                                displayCommandLine("git", listOf("remote", "set-head", selected, "--delete")),
                            ),
                            confirm = "DELETE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = { execute { controller.deleteRemoteHead(selected) } },
                        ) { }
                    }

                    RemoteManagerMode.Remove -> if (selected != null) {
                        ManagerForm(
                            title = strings.remoteRemove,
                            hint = strings.remoteRemoveHint,
                            commands = listOf(
                                displayCommandLine("git", listOf("remote", "remove", selected)),
                            ),
                            confirm = "REMOVE",
                            enabled = !busy,
                            onCancel = { if (!busy) mode = RemoteManagerMode.None },
                            onConfirm = { execute { controller.removeRemote(selected) } },
                        ) { }
                    }

                    RemoteManagerMode.None -> Unit
                }

                controller.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(message, controller::dismissError)
                }
            }
        }
    }
}

@Composable
private fun RemoteIdentityPanel(
    profile: RemoteProfile?,
    loading: Boolean,
    error: String?,
) {
    val strings = LocalStrings.current
    if (loading) {
        Text(
            strings.remoteProfileLoading,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        return
    }
    if (error != null) {
        Text(
            error,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.red,
        )
        return
    }
    val remote = profile ?: return
    val primary = remote.primary ?: return

    SectionLabel("CUSTOM GIT HOST")
    if (primary.isLocal) {
        RemoteField("URL", primary.resolvedUrl)
        Text(
            strings.remoteIdentityLocal,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    } else {
        val host = primary.host ?: "—"
        RemoteField("HOST", if (primary.port == null) host else "$host:${primary.port}")
        RemoteField("OWNER", primary.owner ?: "—")
        RemoteField("REPO", primary.repository ?: "—")
        RemoteField("TRANSPORT", primary.transport.label())
        RemoteField("PROVIDER", primary.hostKind.label())
        if (primary.originalUrl != primary.resolvedUrl) {
            RemoteField("RESOLVED", primary.resolvedUrl)
        }
        if (primary.issue != null) {
            RemoteField(
                "STATUS",
                "${strings.remoteIdentityUnknown}: ${primary.issue.message()}",
                color = Term.amber,
            )
        } else {
            RemoteField("STATUS", "host/owner/repo parsed", color = Term.green)
        }
    }

    if (remote.fetchUrls.size > 1) {
        RemoteField("FETCH URLS", remote.fetchUrls.size.toString())
        remote.fetchUrls.drop(1).forEach { identity ->
            Text(
                "  ${identity.resolvedUrl}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val fetchValues = remote.fetchUrls.map { it.resolvedUrl }.toSet()
    val distinctPush = remote.pushUrls.filter { it.resolvedUrl !in fetchValues }
    if (distinctPush.isNotEmpty()) {
        RemoteField("PUSH URLS", distinctPush.size.toString())
        distinctPush.forEach { push ->
            Text(
                "  ${push.resolvedUrl}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RemoteField(label: String, value: String, color: androidx.compose.ui.graphics.Color = Term.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            modifier = Modifier.width(82.dp),
        )
        Text(
            value,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RemotePreflightPanel(controller: RepoController) {
    val strings = LocalStrings.current
    val branch = controller.status?.branch
    val branchLabel = when {
        branch == null -> "—"
        branch.detached -> strings.remotePreflightDetached
        branch.head.isNullOrBlank() -> strings.remotePreflightUnborn
        else -> branch.head.orEmpty()
    }
    val upstream = branch?.upstream ?: strings.remotePreflightUnset
    val aheadBehind = if (branch?.upstream == null) {
        strings.remotePreflightUnset
    } else {
        "+${branch.ahead} / -${branch.behind}"
    }
    val status = controller.status

    SectionLabel("PREFLIGHT")
    RemoteField("BRANCH", branchLabel)
    RemoteField("UPSTREAM", upstream)
    RemoteField("AHEAD/BEHIND", aheadBehind)
    RemoteField(
        "WORKTREE",
        "${status?.stagedCount ?: 0} staged · ${status?.unstagedCount ?: 0} unstaged · " +
            "${status?.untrackedCount ?: 0} untracked · ${status?.conflictedCount ?: 0} conflicts",
    )
    Spacer(Modifier.height(4.dp))
    Text(
        strings.remotePreflightPermissions,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = Term.amber,
    )
}

private fun RemoteTransport.label(): String = when (this) {
    RemoteTransport.Https -> "HTTPS"
    RemoteTransport.Ssh -> "SSH"
    RemoteTransport.Git -> "GIT"
    RemoteTransport.Http -> "HTTP (unsupported)"
    RemoteTransport.Local -> "LOCAL"
    RemoteTransport.Unknown -> "UNKNOWN"
}

private fun RemoteHostKind.label(): String = when (this) {
    RemoteHostKind.GitHub -> "GITHUB"
    RemoteHostKind.GitLab -> "GITLAB"
    RemoteHostKind.Custom -> "CUSTOM"
    RemoteHostKind.Local -> "LOCAL"
    RemoteHostKind.Unknown -> "UNKNOWN"
}

private fun RemoteIdentityIssue.message(): String = when (this) {
    RemoteIdentityIssue.InvalidUrl -> "invalid URL"
    RemoteIdentityIssue.UnsupportedTransport -> "unsupported transport"
    RemoteIdentityIssue.MissingHost -> "missing host"
    RemoteIdentityIssue.MissingRepository -> "missing repository"
    RemoteIdentityIssue.MissingOwner -> "path has no owner/repository"
}

@Composable
private fun RemoteManagerRow(
    remote: String,
    url: String,
    head: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(38.dp)
            .background(if (selected) Term.green.copy(alpha = 0.10f) else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                remote,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (selected) Term.green else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                url.ifBlank { "—" },
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            head ?: "—",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = if (head == null) Term.faint else Term.cyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RemoteManagerEmpty(text: String) {
    Text(
        "  ($text)",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}