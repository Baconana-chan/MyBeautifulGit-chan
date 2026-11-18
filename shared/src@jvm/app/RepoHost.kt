package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.baconana.mybeautifulgit_chan.ai.AiDiffSource
import org.baconana.mybeautifulgit_chan.ai.CommitMessageGenerator
import org.baconana.mybeautifulgit_chan.ai.PreparedCommitMessage
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.CommitGraphRow
import org.baconana.mybeautifulgit_chan.domain.CommitInspection
import org.baconana.mybeautifulgit_chan.domain.CommitSignature
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.DiffHunk
import org.baconana.mybeautifulgit_chan.domain.DiffLineKind
import org.baconana.mybeautifulgit_chan.domain.DiffLineView
import org.baconana.mybeautifulgit_chan.domain.DiffLayout
import org.baconana.mybeautifulgit_chan.domain.DiffViewOptions
import org.baconana.mybeautifulgit_chan.domain.DiffViewRow
import org.baconana.mybeautifulgit_chan.domain.ParsedDiff
import org.baconana.mybeautifulgit_chan.domain.SyntaxKind
import org.baconana.mybeautifulgit_chan.domain.buildDiffView
import org.baconana.mybeautifulgit_chan.domain.GitOperation
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.GitAlias
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.IndexLock
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.MergeConflict
import org.baconana.mybeautifulgit_chan.domain.MergeLineChoice
import org.baconana.mybeautifulgit_chan.domain.MergeSide
import org.baconana.mybeautifulgit_chan.domain.OperationAction
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.ReflogEntry
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.StatusEntry
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.SubmoduleState
import org.baconana.mybeautifulgit_chan.domain.Worktree
import org.baconana.mybeautifulgit_chan.domain.operationArgs
import org.baconana.mybeautifulgit_chan.domain.parsePatch
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.platform.CommandRunnerStore
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.LabeledValue
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.TabItem
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class RepoTab { Changes, Log, Reflog, History, Search, Console, Rerere, Stash, Patches, Bisect, Advanced }

private enum class RunnerSection { History, Favorites, Aliases, Jobs }

private enum class CommitDetailTab { Info, Diff, Stat }

@Composable
fun RepoHost(
    runtime: GitRuntime,
    repository: Repository,
    commandPalette: CommandPaletteController,
    active: Boolean = true,
    onClose: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onOpenWorktree: (String) -> Unit,
    onOpenWorktreeWindow: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val controller = remember(repository.root) {
        RepoController(repository, runtime.backend, runtime.version)
    }
    SideEffect { controller.strings = strings }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(RepoTab.Changes) }
    var selectedCommit by remember { mutableStateOf<String?>(null) }
    var showIgnored by remember(repository.root) { mutableStateOf(false) }
    var diffOptions by remember(repository.root) { mutableStateOf(DiffViewOptions()) }
    var discard by remember(repository.root) { mutableStateOf<DiscardTarget?>(null) }
    var commitOptionsOpen by remember(repository.root) { mutableStateOf(false) }
    var commitAction by remember(repository.root) { mutableStateOf<CommitActionRequest?>(null) }
    var deleteCommit by remember(repository.root) { mutableStateOf<Commit?>(null) }
    var historyRewriteCommit by remember(repository.root) { mutableStateOf<Commit?>(null) }
    var filterRepoCommit by remember(repository.root) { mutableStateOf<Commit?>(null) }
    var commitRewrite by remember(repository.root) { mutableStateOf<CommitRewriteRequest?>(null) }
    var rebaseTodo by remember(repository.root) { mutableStateOf<RebaseTodoRequest?>(null) }
    var branchManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var branchManagerInitial by remember(repository.root) { mutableStateOf<GitRef?>(null) }
    var tagManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var tagManagerInitial by remember(repository.root) { mutableStateOf<GitRef?>(null) }
    var remoteManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var submoduleManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var submoduleManagerInitial by remember(repository.root) { mutableStateOf<String?>(null) }
    var worktreeManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var worktreeManagerInitial by remember(repository.root) { mutableStateOf<String?>(null) }
    var hookManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var configEditorOpen by remember(repository.root) { mutableStateOf(false) }
    var gitMetaEditorOpen by remember(repository.root) { mutableStateOf(false) }
    var pushManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var pullManagerOpen by remember(repository.root) { mutableStateOf(false) }
    var performanceOpen by remember(repository.root) { mutableStateOf(false) }
    var focusedCommit by remember(repository.root) { mutableStateOf<Commit?>(null) }
    var reflogFocusHash by remember(repository.root) { mutableStateOf<String?>(null) }
    var reflogFocusId by remember(repository.root) { mutableStateOf(0L) }
    var searchRequest by remember(repository.root) { mutableStateOf<SearchOpenRequest?>(null) }
    var searchRequestId by remember(repository.root) { mutableStateOf(0L) }
    var searchMode by remember(repository.root) { mutableStateOf(SearchMode.History) }
    var searchPath by remember(repository.root) { mutableStateOf("") }
    var searchQuery by remember(repository.root) { mutableStateOf("") }

    val watcher = remember(repository.root, active) {
        if (active) {
            RepoWatcher(repository) {
                scope.launch {
                    if (!controller.busy) controller.refresh()
                }
            }
        } else {
            null
        }
    }
    LaunchedEffect(watcher) {
        watcher?.start()
    }
    DisposableEffect(watcher) {
        onDispose { watcher?.close() }
    }

    LaunchedEffect(repository.root, active) {
        if (active && !controller.busy) controller.refresh()
    }

    LaunchedEffect(controller.toast?.id) {
        val current = controller.toast ?: return@LaunchedEffect
        delay(if (current.undo != null) 20_000 else 5_000)
        if (controller.toast?.id == current.id) controller.dismissToast()
    }

    fun openSearch(mode: SearchMode, path: String) {
        searchMode = mode
        searchPath = path
        searchRequestId += 1
        searchRequest = SearchOpenRequest(mode, path, searchRequestId)
        tab = RepoTab.Search
    }

    fun openCommit(hash: String) {
        scope.launch {
            val commit = controller.commitForNavigation(hash)
            if (commit != null) {
                selectedCommit = commit.hash
                focusedCommit = commit
                tab = RepoTab.Log
            }
        }
    }

    val paletteCommit = focusedCommit ?: selectedCommit?.let { hash ->
        controller.commits.firstOrNull { it.hash == hash }
    }
    val paletteCommands = mutableListOf<PaletteCommand>()

    fun paletteCommand(
        id: String,
        title: String,
        category: String,
        description: String = "",
        command: String = "",
        shortcut: String = "",
        keywords: String = "",
        danger: Boolean = false,
        enabled: Boolean = true,
        disabledReason: String? = null,
        run: () -> Unit,
    ) {
        paletteCommands += PaletteCommand(
            id = id,
            title = title,
            category = category,
            description = description,
            command = command,
            shortcut = shortcut,
            keywords = keywords,
            danger = danger,
            enabled = enabled,
            disabledReason = disabledReason,
            run = run,
        )
    }

    fun commitPaletteCommand(
        id: String,
        title: String,
        category: String,
        description: String,
        command: String,
        danger: Boolean = false,
        run: (Commit) -> Unit,
    ) {
        val commit = paletteCommit
        paletteCommand(
            id = id,
            title = title,
            category = category,
            description = description,
            command = commit?.let { command.replace("<commit>", it.shortHash) } ?: command,
            danger = danger,
            enabled = commit != null && !controller.busy,
            disabledReason = when {
                commit == null -> "Select a commit in LOG or REFLOG"
                controller.busy -> "Repository is busy"
                else -> null
            },
        ) {
            commit?.let { selected ->
                controller.dismissError()
                run(selected)
            }
        }
    }

    fun openTab(next: RepoTab) {
        tab = next
    }

    paletteCommand(
        "tab.changes", "Changes", "NAVIGATION",
        "Working tree, staging and commit", "git status", keywords = "stage diff working tree",
    ) { openTab(RepoTab.Changes) }
    paletteCommand(
        "tab.log", "Commit log", "NAVIGATION",
        "History graph, commit details and actions", "git log --graph", keywords = "history graph commits",
    ) { openTab(RepoTab.Log) }
    paletteCommand(
        "tab.reflog", "Reflog", "NAVIGATION",
        "Browse and recover previous HEAD positions", "git reflog", keywords = "recover undo head",
    ) { openTab(RepoTab.Reflog) }
    paletteCommand(
        "tab.history", "Operation history", "NAVIGATION",
        "Review application operations, their results and undo/redo path", "git reflog",
        keywords = "journal undo redo operations rollback",
    ) { openTab(RepoTab.History) }
    paletteCommand(
        "tab.search", "Search", "NAVIGATION",
        "File history, blame and grep", "git grep", keywords = "find blame history grep",
    ) { openTab(RepoTab.Search) }
    paletteCommand(
        "tab.console", "Command console", "NAVIGATION",
        "Run arbitrary Git commands and inspect output", "git <command>", keywords = "terminal runner raw",
    ) { openTab(RepoTab.Console) }
    paletteCommand(
        "tab.rerere", "Rerere cache", "NAVIGATION",
        "Inspect, enable and clear recorded conflict resolutions", "git rerere",
        keywords = "conflict resolution reuse clear",
    ) { openTab(RepoTab.Rerere) }
    paletteCommand(
        "tab.stash", "Stashes", "NAVIGATION",
        "Create, apply, pop, drop and inspect stashes", "git stash",
        keywords = "work in progress wip apply drop branch",
    ) { openTab(RepoTab.Stash) }
    paletteCommand(
        "tab.patches", "Patches", "NAVIGATION",
        "Export, inspect and apply patch series and mailboxes", "git format-patch", keywords = "mailbox am apply",
    ) { openTab(RepoTab.Patches) }
    paletteCommand(
        "tab.bisect", "Bisect", "NAVIGATION",
        "Find the first bad commit", "git bisect", keywords = "regression good bad",
    ) { openTab(RepoTab.Bisect) }
    paletteCommand(
        "tab.advanced", "Advanced repository tools", "NAVIGATION",
        "LFS, sparse-checkout, notes, bundle, gc, prune and maintenance", "git gc",
        keywords = "lfs sparse notes archive bundle fsck prune repack maintenance",
    ) { openTab(RepoTab.Advanced) }

    paletteCommand(
        "repo.refresh", "Refresh repository", "REPOSITORY",
        "Reload status, refs and history", "git status", keywords = "reload update",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.refresh() } }
    paletteCommand(
        "repo.performance", "Show performance profile", "DIAGNOSTICS",
        "Inspect refresh timings, graph/cache activity and loaded history",
        keywords = "profile benchmark slow timing cache latency performance",
    ) { performanceOpen = true }
    paletteCommand(
        "history.undo", "Undo latest reversible operation", "HISTORY",
        "Revert the newest reversible journal operation", "journal · undo",
        keywords = "undo journal rollback commit checkout rewrite", danger = true,
        enabled = controller.canUndoJournal,
        disabledReason = if (controller.busy) "Repository is busy" else "No reversible operation to undo",
    ) { scope.launch { controller.undoJournal() } }
    paletteCommand(
        "history.redo", "Redo newest undone operation", "HISTORY",
        "Reapply the newest undone journal operation", "journal · redo",
        keywords = "redo journal repeat restore commit checkout rewrite", danger = true,
        enabled = controller.canRedoJournal,
        disabledReason = if (controller.busy) "Repository is busy" else "Nothing to redo",
    ) { scope.launch { controller.redoJournal() } }
    paletteCommand(
        "operation.cancel", "Cancel active operation", "OPERATIONS",
        "Stop the running Git process and keep its partial state visible",
        "terminate git process", keywords = "stop abort kill cancel progress long",
        danger = true,
        enabled = controller.operationActivity?.running == true &&
            controller.operationActivity?.cancelling == false,
        disabledReason = "No cancellable operation is running",
    ) { controller.cancelOperationActivity() }
    paletteCommand(
        "repo.push", "Push…", "REMOTE",
        "Choose branch, remote and force policy", "git push", keywords = "publish upload force lease",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        pushManagerOpen = true
    }
    paletteCommand(
        "repo.pull", "Pull…", "REMOTE",
        "Choose merge, rebase or fast-forward-only", "git pull", keywords = "fetch merge rebase",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        pullManagerOpen = true
    }
    paletteCommand(
        "repo.fetch", "Fetch or manage remotes…", "REMOTE",
        "Fetch with prune, tags, depth and partial-clone filters", "git fetch",
        keywords = "remote prune tags",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        remoteManagerOpen = true
    }
    paletteCommand(
        "repo.close", "Close repository tab", "REPOSITORY",
        "Close the current tab; other repository tabs stay open and nothing is changed",
        keywords = "quit workspace tab close",
    ) { onClose() }

    paletteCommand(
        "changes.stage-all", "Stage all changes", "CHANGES",
        "Add every working-tree change to the index", "git add -A",
        keywords = "index add all",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.stageAll() } }
    paletteCommand(
        "changes.unstage-all", "Unstage all changes", "CHANGES",
        "Remove every staged change from the index", "git reset",
        keywords = "index reset",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.unstageAll() } }
    paletteCommand(
        "changes.commit-options", "Commit…", "CHANGES",
        "Open the commit dialog with amend, sign-off and author options", "git commit",
        keywords = "message amend signoff author",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        commitOptionsOpen = true
    }
    paletteCommand(
        "changes.toggle-ignored", "Toggle ignored files", "CHANGES",
        if (showIgnored) "Hide ignored files" else "Show ignored files",
        "git status --ignored", keywords = "excluded gitignore",
    ) { showIgnored = !showIgnored }

    commitPaletteCommand(
        "commit.checkout", "Checkout selected commit", "COMMIT",
        "Detach HEAD at the selected commit", "git checkout <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.Checkout, selected)
    }
    commitPaletteCommand(
        "commit.reset", "Reset to selected commit…", "COMMIT",
        "Choose soft, mixed or hard reset with a loss preview", "git reset --mixed <commit>",
        danger = true,
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.Reset, selected)
    }
    commitPaletteCommand(
        "commit.cherry-pick", "Cherry-pick selected commit", "COMMIT",
        "Apply the commit to the current branch", "git cherry-pick <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.CherryPick, selected)
    }
    commitPaletteCommand(
        "commit.revert", "Revert selected commit", "COMMIT",
        "Create a new commit that reverses the selected change", "git revert --no-edit <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.Revert, selected)
    }
    commitPaletteCommand(
        "commit.branch", "Create branch at commit", "BRANCH",
        "Create a local branch pointing at the selected commit", "git branch <name> <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.Branch, selected)
    }
    commitPaletteCommand(
        "commit.tag", "Create tag at commit", "TAG",
        "Create a lightweight, annotated or signed tag", "git tag <name> <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.Tag, selected)
    }
    commitPaletteCommand(
        "commit.copy-hash", "Copy commit hash", "COMMIT",
        "Copy the full hash of the selected commit", "<commit>",
        ) { selected -> DesktopActions.copy(selected.hash) }
    commitPaletteCommand(
        "commit.reflog", "Show commit in reflog", "HISTORY",
        "Jump to reflog entries for the selected commit", "git reflog",
    ) { selected ->
        reflogFocusHash = selected.hash
        reflogFocusId += 1
        tab = RepoTab.Reflog
    }
    commitPaletteCommand(
        "commit.rebase", "Interactive rebase from commit…", "HISTORY",
        "Reorder, reword, squash or drop commits from the selected base",
        "git rebase -i <commit>", danger = true,
    ) { selected ->
        rebaseTodo = RebaseTodoRequest(mode = RebaseTodoMode.Prepare, commit = selected)
    }
    commitPaletteCommand(
        "commit.delete", "Delete commit…", "HISTORY",
        "Choose a safe or destructive way to remove the selected change",
        "git rebase -i <commit>^", danger = true,
    ) { selected -> deleteCommit = selected }
    commitPaletteCommand(
        "commit.history-rewrite", "Rewrite history from commit…", "HISTORY",
        "Reorder, move, squash or rebase commits onto another base",
        "git rebase --onto <new-base> <old-base> <branch>", danger = true,
    ) { selected -> historyRewriteCommit = selected }
    commitPaletteCommand(
        "commit.filter-repo", "Remove commit content with filter-repo…", "HISTORY",
        "Remove paths or replace strings across all refs; requires dry-run",
        "git filter-repo --path <path> --invert-paths", danger = true,
    ) { selected -> filterRepoCommit = selected }
    commitPaletteCommand(
        "commit.reword", "Reword commit…", "HISTORY",
        "Change the selected commit message and author", "git rebase -i <commit>^",
        danger = true,
    ) { selected ->
        commitRewrite = CommitRewriteRequest(selected, RewriteMode.Reword)
    }
    commitPaletteCommand(
        "commit.amend", "Amend commit…", "HISTORY",
        "Fold staged changes into the selected commit", "git commit --amend",
        danger = true,
    ) { selected ->
        commitRewrite = CommitRewriteRequest(selected, RewriteMode.Amend)
    }
    commitPaletteCommand(
        "commit.split", "Split commit…", "HISTORY",
        "Split the selected commit into smaller commits", "git reset --mixed <commit>^",
        danger = true,
    ) { selected ->
        commitRewrite = CommitRewriteRequest(selected, RewriteMode.Split)
    }
    commitPaletteCommand(
        "commit.bisect-good", "Mark commit as good", "BISECT",
        "Mark the selected commit as good for bisect", "git bisect good <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.BisectGood, selected)
    }
    commitPaletteCommand(
        "commit.bisect-bad", "Mark commit as bad", "BISECT",
        "Mark the selected commit as bad for bisect", "git bisect bad <commit>",
    ) { selected ->
        commitAction = CommitActionRequest(CommitAction.BisectBad, selected)
    }

    paletteCommand(
        "branches.manage", "Manage branches…", "BRANCH",
        "Create, rename, delete, compare and set upstream branches", "git branch",
        keywords = "checkout switch upstream rename delete compare",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        branchManagerInitial = null
        branchManagerOpen = true
    }
    paletteCommand(
        "tags.manage", "Manage tags…", "TAG",
        "Create, push, verify and delete local or remote tags", "git tag",
        keywords = "annotated signed push delete",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        tagManagerInitial = null
        tagManagerOpen = true
    }
    paletteCommand(
        "remotes.manage", "Manage remotes…", "REMOTE",
        "Add, edit, remove remotes and configure fetch behavior", "git remote",
        keywords = "origin url fetch head delete remove",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        remoteManagerOpen = true
    }
    paletteCommand(
        "submodules.manage", "Manage submodules…", "SUBMODULE",
        "Init, update, sync, add and remove submodules", "git submodule",
        keywords = "recursive init update sync",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        submoduleManagerInitial = null
        submoduleManagerOpen = true
    }
    paletteCommand(
        "worktrees.manage", "Manage worktrees…", "WORKTREE",
        "Add, remove, lock, unlock, prune and open linked worktrees", "git worktree",
        keywords = "linked checkout lock prune remove delete",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        worktreeManagerInitial = null
        worktreeManagerOpen = true
    }
    paletteCommand(
        "hooks.manage", "Manage hooks…", "REPOSITORY",
        "Inspect, edit, enable, disable and configure Git hooks", "git config core.hooksPath",
        keywords = "pre-commit pre-push executable",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        hookManagerOpen = true
    }
    paletteCommand(
        "config.manage", "Edit Git config…", "REPOSITORY",
        "Read and change repository configuration entries", "git config --list",
        keywords = "settings key value local global",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        configEditorOpen = true
    }
    paletteCommand(
        "git-meta.manage", "Edit Git metadata files…", "REPOSITORY",
        "Inspect and edit HEAD, config, packed-refs and similar files", "git rev-parse --git-dir",
        keywords = "head config packed refs shallow",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        controller.dismissError()
        gitMetaEditorOpen = true
    }

    paletteCommand(
        "operation.continue", "Continue current operation", "OPERATION",
        "Continue the pending merge, rebase, cherry-pick, revert or am",
        controller.operation?.kind?.operationArgs(OperationAction.Continue)
            ?.joinToString(" ", prefix = "git ").orEmpty(),
        keywords = "merge rebase conflict",
        enabled = controller.operation != null && !controller.busy,
        disabledReason = if (controller.operation == null) "No operation in progress" else "Repository is busy",
    ) { scope.launch { controller.operationAction(OperationAction.Continue) } }
    paletteCommand(
        "operation.undo-rewrite", "Undo last history rewrite", "RECOVERY",
        "Restore the tip saved by the last rewrite operation", "git reset --hard <backup>",
        keywords = "backup restore reflog",
        danger = controller.toast?.undo != null,
        enabled = controller.toast?.undo != null && !controller.busy,
        disabledReason = "No rewrite backup to restore",
    ) { scope.launch { controller.undoRewrite() } }
    paletteCommand(
        "console.clear", "Clear command console output", "CONSOLE",
        "Clear the visible command log; history and favorites are kept",
        keywords = "terminal clear",
    ) { controller.clearLog() }
    paletteCommand(
        "console.clear-finished", "Clear finished background jobs", "CONSOLE",
        "Remove completed background command entries", keywords = "runner jobs",
    ) { controller.clearFinishedBackgroundCommands() }

    val bisectActive = controller.bisect.active && !controller.bisect.complete
    paletteCommand(
        "bisect.current-good", "Mark current commit good", "BISECT",
        "Mark the checked-out bisect commit as good", "git bisect good",
        enabled = bisectActive && !controller.busy,
        disabledReason = "Bisect is not active",
    ) { scope.launch { controller.markBisectCurrent(true) } }
    paletteCommand(
        "bisect.current-bad", "Mark current commit bad", "BISECT",
        "Mark the checked-out bisect commit as bad", "git bisect bad",
        enabled = bisectActive && !controller.busy,
        disabledReason = "Bisect is not active",
    ) { scope.launch { controller.markBisectCurrent(false) } }
    paletteCommand(
        "bisect.skip", "Skip current bisect commit", "BISECT",
        "Skip a commit that cannot be tested", "git bisect skip",
        enabled = bisectActive && !controller.busy,
        disabledReason = "Bisect is not active",
    ) { scope.launch { controller.skipBisect() } }
    paletteCommand(
        "bisect.reset", "Reset bisect", "BISECT",
        "End bisect and return to the original branch", "git bisect reset",
        danger = true,
        enabled = (controller.bisect.active || controller.bisect.complete) && !controller.busy,
        disabledReason = "Bisect is not active",
    ) { scope.launch { controller.resetBisect() } }
    paletteCommand(
        "rerere.toggle", "Toggle rerere", "RERERE",
        if (controller.rerere?.enabled == true) "Disable automatic conflict reuse" else "Enable automatic conflict reuse",
        "git config rerere.enabled",
        keywords = "conflict reuse resolution",
        enabled = controller.rerere != null && !controller.busy,
        disabledReason = "Rerere state is not loaded",
    ) {
        val enabled = controller.rerere?.enabled == true
        scope.launch { controller.setRerereEnabled(!enabled) }
    }

    paletteCommand(
        "advanced.count-objects", "Count Git objects", "ADVANCED",
        "Show loose and packed object counts", "git count-objects -v",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.countObjects() } }
    paletteCommand(
        "advanced.fsck", "Check repository integrity", "ADVANCED",
        "Run git fsck and show dangling or broken objects", "git fsck --full",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.fsckRepository() } }
    paletteCommand(
        "advanced.gc", "Run garbage collection", "ADVANCED",
        "Pack reachable objects and clean unreachable data", "git gc",
        danger = true,
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.gcRepository() } }
    paletteCommand(
        "advanced.gc-aggressive", "Run aggressive garbage collection", "ADVANCED",
        "Repack aggressively and prune unreachable objects", "git gc --aggressive --prune=now",
        danger = true,
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.gcRepository(aggressive = true) } }
    paletteCommand(
        "advanced.repack", "Repack objects", "ADVANCED",
        "Repack all objects and remove redundant packs", "git repack -ad",
        danger = true,
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.repackRepository() } }
    paletteCommand(
        "advanced.prune-preview", "Preview unreachable objects", "ADVANCED",
        "Run prune as a dry-run; nothing is deleted", "git prune --dry-run",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) {
        tab = RepoTab.Advanced
        scope.launch { controller.previewPrune() }
    }
    paletteCommand(
        "advanced.lfs-pull", "LFS pull", "ADVANCED",
        "Download LFS objects for the current checkout", "git lfs pull",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.lfsPull() } }
    paletteCommand(
        "advanced.lfs-fetch", "LFS fetch all", "ADVANCED",
        "Fetch all LFS objects for known refs", "git lfs fetch --all",
        enabled = !controller.busy, disabledReason = "Repository is busy",
    ) { scope.launch { controller.lfsFetchAll() } }
    paletteCommand(
        "search.grep", "Search tracked content", "SEARCH",
        "Open grep mode in the search workspace", "git grep -n <query>",
        keywords = "find text code",
    ) {
        searchMode = SearchMode.Grep
        searchPath = ""
        tab = RepoTab.Search
    }
    paletteCommand(
        "search.file-history", "Search file history", "SEARCH",
        "Open the file-history search workspace", "git log --follow -- <path>",
        keywords = "file path follow",
    ) {
        searchMode = SearchMode.History
        searchPath = ""
        searchRequest = null
        tab = RepoTab.Search
    }
    paletteCommand(
        "search.blame", "Blame lines", "SEARCH",
        "Open blame mode in the search workspace", "git blame -- <path>",
        keywords = "annotate author line",
    ) {
        searchMode = SearchMode.Blame
        searchPath = ""
        searchRequest = null
        tab = RepoTab.Search
    }

    SideEffect {
        if (active) commandPalette.setScreenCommands(controller, paletteCommands)
    }
    val activeNow by rememberUpdatedState(active)
    DisposableEffect(commandPalette) {
        onDispose {
            if (controller.operationActivity?.running == true) {
                controller.cancelOperationActivity()
            }
            controller.backgroundCommands
                .filter { it.state == BackgroundCommandState.Running }
                .forEach { controller.cancelBackgroundCommand(it.id) }
            commandPalette.clearScreenCommands(controller)
            if (activeNow) commandPalette.close()
        }
    }

    Box(Modifier.fillMaxSize().background(Term.bg)) {
        Column(Modifier.fillMaxSize()) {
        RepoHeader(
            controller,
            onClose = onClose,
            onRefresh = { scope.launch { controller.refresh() } },
            onPush = {
                controller.dismissError()
                pushManagerOpen = true
            },
            onPull = {
                controller.dismissError()
                pullManagerOpen = true
            },
            language = language,
            onLanguageChange = onLanguageChange,
        )
        controller.pendingNoCommit?.let { PendingNoCommitBanner(it, controller, scope) }
        if (controller.pendingNoCommit == null) {
            controller.operation?.let {
                OperationBanner(
                    operation = it,
                    controller = controller,
                    scope = scope,
                    onEditTodo = {
                        controller.dismissError()
                        rebaseTodo = RebaseTodoRequest(RebaseTodoMode.Edit)
                    },
                )
            }
        }
        controller.pendingRewrite?.let { PendingRewriteBanner(it, controller, scope) }
        controller.indexLock?.let { lock ->
            IndexLockBanner(
                lock = lock,
                deleting = controller.busy,
                onDelete = { scope.launch { controller.removeIndexLock() } },
            )
        }
        TabBar(controller, tab) { tab = it }
        Divider()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            RepoSidebar(
                controller = controller,
                modifier = Modifier.width(210.dp).fillMaxHeight(),
                onOpenWorktree = onOpenWorktree,
                onManageBranches = { ref ->
                    controller.dismissError()
                    branchManagerInitial = ref
                    branchManagerOpen = true
                },
                onManageTags = { tag ->
                    controller.dismissError()
                    tagManagerInitial = tag
                    tagManagerOpen = true
                },
                onManageRemotes = {
                    controller.dismissError()
                    remoteManagerOpen = true
                },
                onManageSubmodules = { path ->
                    controller.dismissError()
                    submoduleManagerInitial = path
                    submoduleManagerOpen = true
                },
                onManageWorktrees = {
                    controller.dismissError()
                    worktreeManagerInitial = null
                    worktreeManagerOpen = true
                },
                onManageHooks = {
                    controller.dismissError()
                    hookManagerOpen = true
                },
                onManageConfig = {
                    controller.dismissError()
                    configEditorOpen = true
                },
                onManageGitMeta = {
                    controller.dismissError()
                    gitMetaEditorOpen = true
                },
            )
            VDivider()
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (tab) {
                    RepoTab.Changes -> ChangesTab(
                        controller = controller,
                        scope = scope,
                        showIgnored = showIgnored,
                        onShowIgnoredChange = { showIgnored = it },
                        diffOptions = diffOptions,
                        onDiffOptionsChange = { diffOptions = it },
                        onDiscard = {
                            controller.dismissError()
                            discard = it
                        },
                        onCommitOptions = {
                            controller.dismissError()
                            commitOptionsOpen = true
                        },
                        onOpenFileHistory = { openSearch(SearchMode.History, it) },
                        onOpenFileBlame = { openSearch(SearchMode.Blame, it) },
                    )
                    RepoTab.Log -> LogTab(
                        controller = controller,
                        selectedHash = selectedCommit,
                        focusedCommit = focusedCommit,
                        onSelect = {
                            focusedCommit = null
                            selectedCommit = it
                        },
                        onCommitAction = { request ->
                            when (request.action) {
                                CommitAction.CopyHash -> DesktopActions.copy(request.commit.hash)
                                CommitAction.Reflog -> {
                                    controller.dismissError()
                                    reflogFocusHash = request.commit.hash
                                    reflogFocusId += 1
                                    tab = RepoTab.Reflog
                                }
                                CommitAction.InteractiveRebase -> {
                                    controller.dismissError()
                                    rebaseTodo = RebaseTodoRequest(
                                        mode = RebaseTodoMode.Prepare,
                                        commit = request.commit,
                                    )
                                }
                                CommitAction.Delete -> {
                                    controller.dismissError()
                                    deleteCommit = request.commit
                                }
                                CommitAction.HistoryRewrite -> {
                                    controller.dismissError()
                                    historyRewriteCommit = request.commit
                                }
                                CommitAction.FilterRepo -> {
                                    controller.dismissError()
                                    filterRepoCommit = request.commit
                                }
                                else -> {
                                    controller.dismissError()
                                    commitAction = request
                                }
                            }
                        },
                        onCommitRewrite = { commit, mode ->
                            controller.dismissError()
                            commitRewrite = CommitRewriteRequest(commit, mode)
                        },
                    )
                    RepoTab.Reflog -> ReflogTab(
                        controller = controller,
                        scope = scope,
                        focusHash = reflogFocusHash,
                        focusId = reflogFocusId,
                        onFocusConsumed = { reflogFocusHash = null },
                        onOpenCommit = { openCommit(it) },
                        onRecover = { entry, action ->
                            controller.dismissError()
                            commitAction = recoveryRequest(entry, action)
                        },
                    )
                    RepoTab.History -> HistoryTab(controller, scope)
                    RepoTab.Search -> SearchTab(
                        controller = controller,
                        scope = scope,
                        mode = searchMode,
                        path = searchPath,
                        query = searchQuery,
                        onModeChange = { searchMode = it },
                        onPathChange = { searchPath = it },
                        onQueryChange = { searchQuery = it },
                        openRequest = searchRequest,
                        onOpenRequestHandled = { searchRequest = null },
                        onOpenCommit = { openCommit(it) },
                    )
                    RepoTab.Console -> ConsoleTab(controller, scope)
                    RepoTab.Rerere -> RerereTab(controller, scope)
                    RepoTab.Stash -> StashTab(controller, scope)
                    RepoTab.Patches -> PatchTab(controller, scope)
                    RepoTab.Bisect -> BisectTab(controller, scope)
                    RepoTab.Advanced -> AdvancedTab(controller, scope)
                }
            }
        }
            OperationActivityBar(controller)
            controller.error?.let { ErrorBanner(it, controller::dismissError) }
            RepoStatusBar(controller, onRefresh = { scope.launch { controller.refresh() } })
        }

        controller.toast?.let { toast ->
            OperationToastBar(
                toast = toast,
                onCopy = { DesktopActions.copy(toast.command) },
                onUndo = { scope.launch { controller.undoRewrite() } },
                onDismiss = controller::dismissToast,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            )
        }

        discard?.let { target ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                DiscardDialog(
                    target = target,
                    controller = controller,
                    onDismiss = { if (!controller.busy) discard = null },
                )
            }
        }

        if (commitOptionsOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CommitOptionsDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) commitOptionsOpen = false },
                )
            }
        }

        commitAction?.let { request ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CommitActionDialog(
                    request = request,
                    controller = controller,
                    onDismiss = { if (!controller.busy) commitAction = null },
                    onSuccess = {
                        if (tab == RepoTab.Reflog) scope.launch { controller.loadReflog() }
                    },
                )
            }
        }

        deleteCommit?.let { commit ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                DeleteCommitDialog(
                    commit = commit,
                    controller = controller,
                    onDismiss = { if (!controller.busy) deleteCommit = null },
                    onDropInRebase = { target ->
                        deleteCommit = null
                        rebaseTodo = RebaseTodoRequest(
                            mode = RebaseTodoMode.Prepare,
                            commit = target,
                            presetDropHashes = setOf(target.hash),
                        )
                    },
                )
            }
        }

        historyRewriteCommit?.let { commit ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                HistoryRewriteDialog(
                    commit = commit,
                    controller = controller,
                    onDismiss = { if (!controller.busy) historyRewriteCommit = null },
                    onSuccess = {
                        if (controller.operation != null) tab = RepoTab.Changes
                    },
                )
            }
        }

        filterRepoCommit?.let { commit ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                FilterRepoDialog(
                    commit = commit,
                    controller = controller,
                    onDismiss = { if (!controller.busy) filterRepoCommit = null },
                    onSuccess = { tab = RepoTab.Changes },
                )
            }
        }

        commitRewrite?.let { request ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CommitRewriteDialog(
                    request = request,
                    controller = controller,
                    onDismiss = { if (!controller.busy) commitRewrite = null },
                    onSuccess = {
                        if (controller.pendingRewrite != null) tab = RepoTab.Changes
                    },
                )
            }
        }

        rebaseTodo?.let { request ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                RebaseTodoDialog(
                    request = request,
                    controller = controller,
                    onDismiss = { if (!controller.busy) rebaseTodo = null },
                    onSuccess = {
                        if (controller.operation != null) tab = RepoTab.Changes
                    },
                )
            }
        }

        if (branchManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                BranchManagerDialog(
                    controller = controller,
                    initialRef = branchManagerInitial,
                    onDismiss = { if (!controller.busy) branchManagerOpen = false },
                )
            }
        }

        if (tagManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                TagManagerDialog(
                    controller = controller,
                    initialTag = tagManagerInitial,
                    onDismiss = { if (!controller.busy) tagManagerOpen = false },
                )
            }
        }

        if (remoteManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                RemoteManagerDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) remoteManagerOpen = false },
                )
            }
        }

        if (submoduleManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                SubmoduleManagerDialog(
                    controller = controller,
                    initialPath = submoduleManagerInitial,
                    onDismiss = { if (!controller.busy) submoduleManagerOpen = false },
                )
            }
        }

        if (worktreeManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                WorktreeManagerDialog(
                    controller = controller,
                    initialPath = worktreeManagerInitial,
                    onOpen = onOpenWorktree,
                    onOpenWindow = onOpenWorktreeWindow,
                    onDismiss = { if (!controller.busy) worktreeManagerOpen = false },
                )
            }
        }

        if (hookManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                HookManagerDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) hookManagerOpen = false },
                )
            }
        }

        if (configEditorOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                ConfigEditorDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) configEditorOpen = false },
                )
            }
        }

        if (gitMetaEditorOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                GitMetaEditorDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) gitMetaEditorOpen = false },
                )
            }
        }

        if (pushManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                PushManagerDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) pushManagerOpen = false },
                )
            }
        }

        if (pullManagerOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                PullManagerDialog(
                    controller = controller,
                    onDismiss = { if (!controller.busy) pullManagerOpen = false },
                )
            }
        }

        if (performanceOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                PerformanceDialog(
                    controller = controller,
                    onDismiss = { performanceOpen = false },
                )
            }
        }

    }
}

@Composable
private fun OperationBanner(
    operation: GitOperation,
    controller: RepoController,
    scope: CoroutineScope,
    onEditTodo: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    var pending by remember(operation.kind, operation.step, operation.total) {
        mutableStateOf<OperationAction?>(null)
    }
    val hasActions = operation.kind != GitOperationKind.Bisect
    Column(
        Modifier.fillMaxWidth()
            .background(Term.amber.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "[!]",
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.amber,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                operationLabel(operation),
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.amber,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                strings.repoOperationInProgress,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasActions) {
                Spacer(Modifier.width(8.dp))
                if (operation.kind == GitOperationKind.Rebase && onEditTodo != null) {
                    OperationActionButton(
                        label = strings.repoOperationEditTodo,
                        enabled = !controller.busy,
                        color = Term.amber,
                    ) { onEditTodo() }
                    Spacer(Modifier.width(8.dp))
                }
                OperationActionButton(
                    label = strings.repoOperationContinue,
                    enabled = !controller.busy,
                ) {
                    scope.launch { controller.operationAction(OperationAction.Continue) }
                }
                Spacer(Modifier.width(8.dp))
                OperationActionButton(
                    label = strings.repoOperationAbort,
                    enabled = !controller.busy,
                    color = Term.red,
                ) { pending = OperationAction.Abort }
                Spacer(Modifier.width(8.dp))
                OperationActionButton(
                    label = strings.repoOperationSkip,
                    enabled = !controller.busy && operation.kind.operationArgs(OperationAction.Skip) != null,
                ) { pending = OperationAction.Skip }
            }
        }

        if (operation.kind == GitOperationKind.Merge) {
            Text(
                "[${strings.repoOperationSkip}: ${strings.repoOperationSkipUnsupported}]",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(top = 3.dp, start = 22.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        pending?.let { action ->
            val args = operation.kind.operationArgs(action)
            if (args != null) {
                Spacer(Modifier.height(3.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Term.red.copy(alpha = 0.10f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${operationActionLabel(action)} · ${operationLabel(operation)}",
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            fontWeight = FontWeight.Bold,
                            color = Term.red,
                        )
                        Text(
                            if (action == OperationAction.Abort) {
                                strings.repoOperationAbortWarning
                            } else {
                                strings.repoOperationSkipWarning
                            },
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                        Text(
                            "git ${args.joinToString(" ")}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OperationActionButton(
                        label = strings.repoOperationCancel,
                        enabled = !controller.busy,
                        color = Term.dim,
                    ) { pending = null }
                    Spacer(Modifier.width(8.dp))
                    OperationActionButton(
                        label = strings.repoOperationConfirm,
                        enabled = !controller.busy,
                        color = Term.red,
                    ) {
                        pending = null
                        scope.launch { controller.operationAction(action) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingNoCommitBanner(
    pending: PendingNoCommit,
    controller: RepoController,
    scope: CoroutineScope,
) {
    val strings = LocalStrings.current
    var confirmAbort by remember(pending.range) { mutableStateOf(false) }
    val command = when (pending.kind) {
        GitOperationKind.CherryPick -> "cherry-pick"
        GitOperationKind.Revert -> "revert"
        else -> ""
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Term.amber.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "[!] ${strings.repoNoCommitInProgress}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.amber,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "${operationKindLabel(pending.kind)} · ${pending.range} · " +
                    "${pending.remaining.size} ${strings.repoNoCommitRemaining}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            OperationActionButton(
                label = strings.repoOperationContinue,
                enabled = !controller.busy,
                color = Term.green,
            ) {
                scope.launch { controller.continueNoCommit() }
            }
            Spacer(Modifier.width(8.dp))
            OperationActionButton(
                label = strings.repoOperationAbort,
                enabled = !controller.busy,
                color = Term.red,
            ) { confirmAbort = true }
        }
        Text(
            strings.repoNoCommitHint,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.text,
            modifier = Modifier.padding(start = 22.dp, top = 2.dp),
        )
        Text(
            "git $command --quit" + if (pending.remaining.isEmpty()) {
                ""
            } else {
                " · git $command --no-commit ${pending.remaining.first()}"
            },
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.padding(start = 22.dp, top = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (confirmAbort) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.repoNoCommitAbortWarning,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.red,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OperationActionButton(
                    label = strings.repoOperationCancel,
                    enabled = !controller.busy,
                    color = Term.dim,
                ) { confirmAbort = false }
                Spacer(Modifier.width(8.dp))
                OperationActionButton(
                    label = strings.repoOperationConfirm,
                    enabled = !controller.busy,
                    color = Term.red,
                ) {
                    confirmAbort = false
                    scope.launch { controller.abortNoCommit() }
                }
            }
        }
    }
}

@Composable
private fun PendingRewriteBanner(
    pending: PendingRewrite,
    controller: RepoController,
    scope: CoroutineScope,
) {
    val strings = LocalStrings.current
    var confirmCancel by remember(pending.targetHash) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(Term.amber.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.commitRewriteInProgress,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.amber,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "${pending.targetHash.take(7)} · ${pending.mode.label(strings)}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            OperationActionButton(
                label = strings.commitRewriteFinish,
                enabled = !controller.busy,
                color = Term.green,
            ) {
                scope.launch { controller.finishCommitRewrite() }
            }
            Spacer(Modifier.width(8.dp))
            OperationActionButton(
                label = strings.commitRewriteCancel,
                enabled = !controller.busy,
                color = Term.red,
            ) { confirmCancel = true }
        }
        Text(
            if (pending.mode == RewriteMode.Amend) {
                strings.commitRewriteAmendInstruction
            } else {
                strings.commitRewriteSplitInstruction
            },
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.text,
            modifier = Modifier.padding(start = 22.dp, top = 2.dp),
        )
        if (confirmCancel) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.commitRewriteCancelWarning,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.red,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OperationActionButton(
                    label = strings.commitRewriteConfirmCancel,
                    enabled = !controller.busy,
                    color = Term.red,
                ) {
                    confirmCancel = false
                    scope.launch { controller.cancelCommitRewrite() }
                }
            }
        }
    }
}

private fun RewriteMode.label(strings: org.baconana.mybeautifulgit_chan.i18n.Strings): String = when (this) {
    RewriteMode.Reword -> strings.commitRewriteModeReword
    RewriteMode.Amend -> strings.commitRewriteModeAmend
    RewriteMode.Split -> strings.commitRewriteModeSplit
}

@Composable
private fun OperationActionButton(
    label: String,
    enabled: Boolean,
    color: Color = Term.green,
    onClick: () -> Unit,
) {
    Text(
        "[$label]",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = if (enabled) color else Term.faint,
        maxLines = 1,
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
    )
}

private fun operationActionLabel(action: OperationAction): String = when (action) {
    OperationAction.Continue -> "CONTINUE"
    OperationAction.Abort -> "ABORT"
    OperationAction.Skip -> "SKIP"
}

@Composable
private fun IndexLockBanner(lock: IndexLock, deleting: Boolean, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        Modifier.fillMaxWidth()
            .background(Term.red.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "[!] INDEX.LOCK",
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.red,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                lock.path,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (deleting) "[deleting…]" else "[${strings.repoIndexLockDelete}]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (deleting) Term.faint else Term.red,
                modifier = Modifier.clickable(enabled = !deleting) { onDelete() },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            strings.repoIndexLockWarning,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun operationKindLabel(kind: GitOperationKind): String = when (kind) {
    GitOperationKind.Merge -> "MERGE"
    GitOperationKind.Rebase -> "REBASE"
    GitOperationKind.CherryPick -> "CHERRY-PICK"
    GitOperationKind.Revert -> "REVERT"
    GitOperationKind.Am -> "AM"
    GitOperationKind.Bisect -> "BISECT"
}

private fun operationLabel(operation: GitOperation): String {
    val label = operationKindLabel(operation.kind)
    return operation.progress?.let { "$label $it" } ?: label
}

@Composable
private fun RepoHeader(
    controller: RepoController,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
) {
    val branch = controller.status?.branch
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(Term.panelAlt)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("*", fontSize = 10.sp, fontFamily = Term.mono, color = Term.green)
        Text(
            " ${branchLabel(controller)}",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = Term.green,
        )
        val upstream = branch?.upstream
        if (!upstream.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                "→ $upstream",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (branch != null && !branch.detached && branch.head != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                "(no upstream)",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            controller.repository.name,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = Term.white,
            maxLines = 1,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            controller.repository.root,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if ((branch?.ahead ?: 0) > 0) {
            Text("↑${branch?.ahead}", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
            Spacer(Modifier.width(7.dp))
        }
        if ((branch?.behind ?: 0) > 0) {
            Text("↓${branch?.behind}", fontSize = 9.sp, fontFamily = Term.mono, color = Term.cyan)
            Spacer(Modifier.width(7.dp))
        }
        if (controller.busy) {
            Text("[working…]", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
            Spacer(Modifier.width(7.dp))
        }
        LanguageSwitch(language, onLanguageChange)
        Spacer(Modifier.width(12.dp))
        ActionButton(key = "l", label = "pull", enabled = !controller.busy, onClick = onPull)
        Spacer(Modifier.width(6.dp))
        ActionButton(key = "p", label = "push", enabled = !controller.busy, onClick = onPush)
        Spacer(Modifier.width(6.dp))
        ActionButton(key = "r", label = "refresh", enabled = !controller.busy, onClick = onRefresh)
        Spacer(Modifier.width(6.dp))
        ActionButton(key = "q", label = "close", onClick = onClose)
    }
}

@Composable
private fun TabBar(controller: RepoController, current: RepoTab, onSelect: (RepoTab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(26.dp).background(Term.panel).a11yFocusGroup()) {
        TabItem(
            index = 1,
            label = "CHANGES",
            badge = controller.visibleEntries.size.toString(),
            active = current == RepoTab.Changes,
        ) { onSelect(RepoTab.Changes) }
        TabItem(
            index = 2,
            label = "LOG",
            active = current == RepoTab.Log,
        ) { onSelect(RepoTab.Log) }
        TabItem(
            index = 3,
            label = "REFLOG",
            active = current == RepoTab.Reflog,
        ) { onSelect(RepoTab.Reflog) }
        TabItem(
            index = 4,
            label = "HISTORY",
            badge = controller.journal.size.takeIf { it > 0 }?.let { if (it > 99) "99+" else it.toString() },
            active = current == RepoTab.History,
        ) { onSelect(RepoTab.History) }
        TabItem(
            index = 5,
            label = "SEARCH",
            active = current == RepoTab.Search,
        ) { onSelect(RepoTab.Search) }
        TabItem(
            index = 6,
            label = "CONSOLE",
            active = current == RepoTab.Console,
        ) { onSelect(RepoTab.Console) }
        TabItem(
            index = 7,
            label = "RERERE",
            badge = controller.rerere?.entries?.size?.takeIf { it > 0 }?.toString(),
            active = current == RepoTab.Rerere,
        ) { onSelect(RepoTab.Rerere) }
        TabItem(
            index = 8,
            label = "STASH",
            badge = controller.stashCount.takeIf { it > 0 }?.toString(),
            active = current == RepoTab.Stash,
        ) { onSelect(RepoTab.Stash) }
        TabItem(
            index = 9,
            label = "PATCHES",
            active = current == RepoTab.Patches,
        ) { onSelect(RepoTab.Patches) }
        TabItem(
            index = 10,
            label = "BISECT",
            badge = controller.bisect.currentHash.takeIf { it.isNotBlank() }?.let { "•" },
            active = current == RepoTab.Bisect,
        ) { onSelect(RepoTab.Bisect) }
        TabItem(
            index = 11,
            label = "ADVANCED",
            active = current == RepoTab.Advanced,
        ) { onSelect(RepoTab.Advanced) }
        Spacer(Modifier.weight(1f))
        Text(
            "${controller.commits.size}${if (controller.historyHasMore) "+" else ""} commits",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${commandPaletteShortcutLabel()} palette",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
        )
    }
}

@Composable
private fun RepoSidebar(
    controller: RepoController,
    modifier: Modifier,
    onOpenWorktree: (String) -> Unit,
    onManageBranches: (GitRef?) -> Unit,
    onManageTags: (GitRef?) -> Unit,
    onManageRemotes: () -> Unit,
    onManageSubmodules: (String?) -> Unit,
    onManageWorktrees: () -> Unit,
    onManageHooks: () -> Unit,
    onManageConfig: () -> Unit,
    onManageGitMeta: () -> Unit,
) {
    val branches = controller.refs.filter { it.kind == RefKind.LocalBranch }
    val remoteBranches = controller.refs.filter { it.kind == RefKind.RemoteBranch }
    val tags = controller.refs.filter { it.kind == RefKind.Tag }

    Column(modifier.background(Term.panel)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                controller.repository.name,
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                controller.repository.root,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (controller.repository.isBare) {
                Spacer(Modifier.height(4.dp))
                Text("[bare]", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.amber)
            }
        }
        Divider()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            SidebarHeader(
                title = "BRANCHES",
                trailing = branches.size.toString(),
                action = "+",
                onAction = { onManageBranches(null) },
            )
            if (branches.isEmpty()) {
                SidebarEmpty("no commits yet")
            } else {
                branches.take(14).forEach { branch ->
                    BranchRow(branch) { onManageBranches(branch) }
                }
                if (branches.size > 14) SidebarMore(branches.size - 14)
            }

            Divider()
            SidebarHeader(
                title = "WORKTREES",
                trailing = controller.worktrees.size.toString(),
                action = "+",
                onAction = onManageWorktrees,
            )
            if (controller.worktrees.isEmpty()) {
                SidebarEmpty("none")
            } else {
                controller.worktrees.take(5).forEach { worktree ->
                    WorktreeRow(
                        worktree = worktree,
                        current = samePath(worktree.path, controller.repository.root),
                        onOpen = onOpenWorktree,
                    )
                }
                if (controller.worktrees.size > 5) {
                    SidebarMore(controller.worktrees.size - 5)
                }
            }

            Divider()
            SidebarHeader(
                title = "HOOKS",
                trailing = controller.hooks.entries.count { it.enabled }.toString(),
                action = "+",
                onAction = onManageHooks,
            )
            if (controller.hooks.entries.isEmpty()) {
                SidebarEmpty("none")
            } else {
                controller.hooks.entries.take(5).forEach { hook ->
                    Row(
                        Modifier.fillMaxWidth().height(19.dp)
                            .clickable { onManageHooks() }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            when {
                                hook.enabled -> "*"
                                hook.disabled -> "-"
                                else -> "·"
                            },
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = when {
                                hook.enabled -> Term.green
                                hook.disabled -> Term.amber
                                else -> Term.faint
                            },
                        )
                        Text(
                            " ${hook.name}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = if (hook.enabled) Term.text else Term.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (controller.hooks.entries.size > 5) {
                    SidebarMore(controller.hooks.entries.size - 5)
                }
            }

            Divider()
            SidebarHeader(
                title = "CONFIG",
                trailing = "git",
                action = "e",
                onAction = onManageConfig,
            )

            Divider()
            SidebarHeader(
                title = "GIT FILES",
                trailing = "3",
                action = "e",
                onAction = onManageGitMeta,
            )

            Divider()
            SidebarHeader(
                title = "SUBMODULES",
                trailing = controller.submodules.size.toString(),
                action = "+",
                onAction = { onManageSubmodules(null) },
            )
            if (controller.submodules.isEmpty()) {
                SidebarEmpty("none")
            } else {
                controller.submodules.take(6).forEach { submodule ->
                    SubmoduleRow(submodule) { onManageSubmodules(submodule.path) }
                }
                if (controller.submodules.size > 6) {
                    SidebarMore(controller.submodules.size - 6)
                }
            }

            Divider()
            SidebarHeader(
                title = "TAGS",
                trailing = tags.size.toString(),
                action = "+",
                onAction = { onManageTags(null) },
            )
            if (tags.isEmpty()) {
                SidebarEmpty("none")
            } else {
                tags.take(6).forEach { tag ->
                    TagSidebarRow(tag) { onManageTags(tag) }
                }
                if (tags.size > 6) SidebarMore(tags.size - 6)
            }

            Divider()
            SidebarHeader(
                title = "REMOTES",
                trailing = remoteBranches.size.toString(),
                action = "+",
                onAction = onManageRemotes,
            )
            if (remoteBranches.isEmpty()) {
                SidebarEmpty("none")
            } else {
                remoteBranches.take(6).forEach { remote ->
                    Row(
                        Modifier.fillMaxWidth().height(19.dp)
                            .clickable { onManageBranches(remote) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            remote.shortName,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.cyan,
                            maxLines = 1,
                        )
                    }
                }
                if (remoteBranches.size > 6) SidebarMore(remoteBranches.size - 6)
            }
        }
        Divider()
        Column(Modifier.padding(8.dp)) {
            Text(
                "git ${controller.gitVersion.major}.${controller.gitVersion.minor}.${controller.gitVersion.patch}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                when {
                    controller.repository.isBare -> "bare repository"
                    controller.repository.isWorktree -> "worktree"
                    else -> "main worktree"
                },
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }
}

@Composable
private fun SidebarEmpty(text: String) {
    Text(
        "  ($text)",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SidebarMore(count: Int) {
    Text(
        "  +$count more",
        fontSize = 7.5.sp,
        fontFamily = Term.mono,
        color = Term.faint,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun WorktreeRow(worktree: Worktree, current: Boolean, onOpen: (String) -> Unit) {
    val label = when {
        worktree.isBare -> "(bare)"
        worktree.branchName != null -> worktree.branchName!!
        worktree.isDetached -> "(detached)"
        else -> "(unknown)"
    }
    Row(
        Modifier.fillMaxWidth().height(30.dp)
            .background(if (current) Term.green.copy(alpha = 0.08f) else Term.panel)
            .clickable(enabled = !current && !worktree.isPrunable) { onOpen(worktree.path) }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (current) "*" else "↳",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (current) Term.green else Term.dim,
            modifier = Modifier.width(12.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (current) Term.green else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                worktree.path,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (worktree.isLocked) {
            Text("[locked]", fontSize = 7.sp, fontFamily = Term.mono, color = Term.amber)
        }
        if (worktree.isPrunable) {
            Text("[prunable]", fontSize = 7.sp, fontFamily = Term.mono, color = Term.red)
        }
    }
}

@Composable
private fun SubmoduleRow(submodule: Submodule, onClick: () -> Unit) {
    val marker = when (submodule.state) {
        SubmoduleState.Current -> "="
        SubmoduleState.Modified -> "+"
        SubmoduleState.Uninitialized -> "-"
        SubmoduleState.Conflict -> "!"
    }
    val color = when (submodule.state) {
        SubmoduleState.Current -> Term.greenDim
        SubmoduleState.Modified -> Term.amber
        SubmoduleState.Uninitialized -> Term.faint
        SubmoduleState.Conflict -> Term.red
    }
    Row(
        Modifier.fillMaxWidth().height(21.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            marker,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(12.dp),
        )
        Text(
            submodule.path,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        submodule.describe?.let { describe ->
            Spacer(Modifier.width(4.dp))
            Text(
                describe.take(18),
                fontSize = 7.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun samePath(first: String, second: String): Boolean {
    val a = File(first).normalize().path
    val b = File(second).normalize().path
    return a.equals(b, ignoreCase = System.getProperty("os.name").orEmpty()
        .lowercase().contains("win"))
}

@Composable
private fun SidebarHeader(
    title: String,
    trailing: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(18.dp).background(Term.raised).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = Term.green,
            letterSpacing = 1.sp,
            modifier = Modifier.a11yHeading(),
        )
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                "[$action]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                modifier = Modifier
                    .clickable { onAction() }
                    .a11yButton("Manage $title")
                    .a11yFocusRing()
                    .padding(2.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(trailing, fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
    }
}

@Composable
private fun TagSidebarRow(tag: GitRef, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(20.dp)
            .clickable { onClick() }
            .a11yButton("Tag ${tag.shortName}")
            .a11yFocusRing()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
        Text(
            " ${tag.shortName}",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (tag.objectType == "tag") "A" else "L",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun BranchRow(branch: GitRef, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(20.dp)
            .clickable { onClick() }
            .a11yButton("Branch ${branch.shortName}")
            .a11yFocusRing()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (branch.isHead) "*" else " ",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = Term.green,
        )
        Text(
            " ${branch.shortName}",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (branch.isHead) Term.green else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (branch.ahead > 0) {
            Text("↑${branch.ahead}", fontSize = 8.sp, fontFamily = Term.mono, color = Term.amber)
        }
        if (branch.behind > 0) {
            Spacer(Modifier.width(4.dp))
            Text("↓${branch.behind}", fontSize = 8.sp, fontFamily = Term.mono, color = Term.cyan)
        }
    }
}

@Composable
private fun ChangesTab(
    controller: RepoController,
    scope: CoroutineScope,
    showIgnored: Boolean,
    onShowIgnoredChange: (Boolean) -> Unit,
    diffOptions: DiffViewOptions,
    onDiffOptionsChange: (DiffViewOptions) -> Unit,
    onDiscard: (DiscardTarget) -> Unit,
    onCommitOptions: () -> Unit,
    onOpenFileHistory: (String) -> Unit,
    onOpenFileBlame: (String) -> Unit,
) {
    if (controller.repository.workTree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BARE REPOSITORY", fontSize = 14.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.amber)
                Spacer(Modifier.height(6.dp))
                Text(
                    LocalStrings.current.repoBareNotice,
                    fontSize = 9.5.sp,
                    fontFamily = Term.mono,
                    color = Term.dim,
                )
            }
        }
        return
    }

    val unstaged = controller.visibleEntries.filter { it.isUnstaged || it.isConflicted }
    val staged = controller.visibleEntries.filter { it.isStaged }
    val ignored = controller.ignoredEntries

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            FilePanel(
                title = "UNSTAGED",
                entries = unstaged,
                staged = false,
                controller = controller,
                scope = scope,
                onDiscard = onDiscard,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            VDivider()
            FilePanel(
                title = "STAGED",
                entries = staged,
                staged = true,
                controller = controller,
                scope = scope,
                onDiscard = onDiscard,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            VDivider()
            DiffPanel(
                controller,
                scope,
                Modifier.weight(1.25f).fillMaxHeight(),
                onDiscard = onDiscard,
                options = diffOptions,
                onOptionsChange = onDiffOptionsChange,
                onOpenFileHistory = onOpenFileHistory,
                onOpenFileBlame = onOpenFileBlame,
            )
        }
        if (ignored.isNotEmpty()) {
            IgnoredSection(
                entries = ignored,
                expanded = showIgnored,
                onToggle = { onShowIgnoredChange(!showIgnored) },
            )
        }
        if (controller.retryCommitCommand != null) {
            RetryCommitBar(controller, scope)
        }
        CommitBar(controller, scope, onCommitOptions)
    }
}

@Composable
private fun RetryCommitBar(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    Divider()
    Row(
        Modifier.fillMaxWidth().background(Term.amber.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("[!]", fontSize = 10.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.amber)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                strings.repoRetryCommitHint,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                controller.retryCommitCommand.orEmpty(),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "[x]",
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier
                .clickable(enabled = !controller.busy) {
                    controller.dismissRetryCommit()
                }
                .a11yButton(strings.a11yDismiss, enabled = !controller.busy)
                .a11yFocusRing(enabled = !controller.busy),
        )
        Spacer(Modifier.width(7.dp))
        ActionButton(
            key = null,
            label = if (controller.busy) "WAIT…" else strings.repoRetryCommit,
            enabled = !controller.busy,
            onClick = { scope.launch { controller.retryCommit() } },
        )
    }
}

@Composable
private fun IgnoredSection(
    entries: List<StatusEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth()) {
        Divider()
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "IGNORED",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Text(
                " (${entries.size})",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (expanded) "[${strings.repoHideIgnored}]" else "[${strings.repoShowIgnored}]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                modifier = Modifier.clickable { onToggle() },
            )
        }
        if (expanded) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 110.dp)) {
                items(entries, key = { it.path }) { entry ->
                    IgnoredRow(entry)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
    }
}

@Composable
private fun IgnoredRow(entry: StatusEntry) {
    Row(
        Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "!",
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.faint,
            modifier = Modifier.width(13.dp),
        )
        Text(
            entry.path,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilePanel(
    title: String,
    entries: List<StatusEntry>,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    onDiscard: (DiscardTarget) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Text(" (${entries.size})", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                Text(
                    if (staged) "[unstage all]" else "[stage all]",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.greenDim,
                    modifier = Modifier.clickable {
                        scope.launch { if (staged) controller.unstageAll() else controller.stageAll() }
                    },
                )
            }
        }
        Divider()
        if (entries.isEmpty()) {
            Text(
                if (staged) LocalStrings.current.repoNothingStaged else LocalStrings.current.repoWorktreeClean,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(entries, key = { it.path + "|" + it.originalPath }) { entry ->
                    FileRow(entry, staged, controller, scope, onDiscard)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: StatusEntry,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    onDiscard: (DiscardTarget) -> Unit,
) {
    val strings = LocalStrings.current
    val untracked = entry.kind == StatusKind.Untracked
    val selected = controller.selectedPath == entry.path && controller.selectedStaged == staged
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .background(if (selected) Term.green.copy(alpha = 0.12f) else Term.panel)
            .clickable { scope.launch { controller.select(entry, staged) } }
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            statusCode(entry, staged),
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = statusColor(entry, staged),
            modifier = Modifier.width(13.dp),
        )
        Text(
            entry.path,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.white else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        entry.submodule?.let { code ->
            Spacer(Modifier.width(4.dp))
            Text(
                "[$code]",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = when {
                    entry.isConflicted -> Term.red
                    entry.submoduleDirty -> Term.amber
                    else -> Term.cyan
                },
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            when {
                staged -> "[-]"
                untracked -> "[${strings.repoAdd}]"
                else -> "[+]"
            },
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (staged) Term.red.copy(alpha = 0.8f) else Term.green,
            modifier = Modifier.clickable {
                scope.launch {
                    when {
                        staged -> controller.unstage(entry)
                        untracked -> controller.addUntracked(entry)
                        else -> controller.stage(entry)
                    }
                }
            },
        )
        if (!staged && !entry.isConflicted) {
            if (untracked) {
                Spacer(Modifier.width(5.dp))
                Text(
                    "[${strings.repoIgnore}]",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.cyan,
                    maxLines = 1,
                    modifier = Modifier.clickable(enabled = !controller.busy) {
                        scope.launch { controller.ignore(entry) }
                    },
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                "[${if (untracked) strings.repoDelete else strings.repoDiscard}]",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.red.copy(alpha = 0.85f),
                maxLines = 1,
                modifier = Modifier.clickable(enabled = !controller.busy) {
                    onDiscard(DiscardTarget(entry, DiscardScope.File))
                },
            )
        }
    }
}

@Composable
private fun DiffPanel(
    controller: RepoController,
    scope: CoroutineScope,
    modifier: Modifier,
    onDiscard: (DiscardTarget) -> Unit,
    options: DiffViewOptions,
    onOptionsChange: (DiffViewOptions) -> Unit,
    onOpenFileHistory: (String) -> Unit,
    onOpenFileBlame: (String) -> Unit,
) {
    if (controller.mergeEditor != null) {
        MergeEditorPanel(controller, scope, modifier)
        return
    }
    Column(modifier.background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "DIFF",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(7.dp))
            val selectedPath = controller.selectedPath
            Text(
                selectedPath ?: "—",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selectedPath != null) {
                Text(
                    "[${LocalStrings.current.repoHistory}]",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.cyan,
                    modifier = Modifier.clickable { onOpenFileHistory(selectedPath) },
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "[${LocalStrings.current.repoBlame}]",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.cyan,
                    modifier = Modifier.clickable { onOpenFileBlame(selectedPath) },
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (controller.selectedStaged) "staged" else "worktree",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }
        if (controller.diffFileInfo == null) {
            DiffToolbar(options, onOptionsChange)
        }
        Divider()

        val raw = controller.diff
        val fileInfo = controller.diffFileInfo
        if (fileInfo != null) {
            DiffFileInfoPanel(fileInfo, Modifier.weight(1f).fillMaxWidth())
        } else if (raw.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    LocalStrings.current.repoSelectFile,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        } else if (raw.startsWith("git diff failed") || raw.startsWith("Binary file") || raw.startsWith("File is too large")) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
                SelectionContainer {
                    Text(raw, fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
                }
            }
        } else {
            val parsed = remember(raw) { parsePatch(raw) }
            val rows = remember(parsed, controller.selectedPath, options) {
                buildDiffView(parsed, controller.selectedPath, options)
            }
            val discardEntry = controller.visibleEntries
                .firstOrNull { it.path == controller.selectedPath }
            SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                    itemsIndexed(rows) { _, row ->
                        DiffViewContentRow(
                            row = row,
                            parsed = parsed,
                            staged = controller.selectedStaged,
                            controller = controller,
                            scope = scope,
                            discardEntry = discardEntry,
                            onDiscard = onDiscard,
                        )
                    }
                }
            }
        }

        Divider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (controller.selectedStaged) "git diff --cached" else "git diff",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                LocalStrings.current.repoDiffHint,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MergeEditorPanel(
    controller: RepoController,
    scope: CoroutineScope,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val state = controller.mergeEditor ?: return
    var tab by remember(state.path) { mutableStateOf(MergeTab.Conflicts) }
    val unavailable = state.binary || state.tooLarge || state.error != null

    Column(modifier.background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.repoMergeTitle,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                state.path,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${controller.mergeConflicts.size}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(end = 8.dp),
            )
            MergeAction(
                label = strings.repoMergeSave,
                enabled = !controller.busy && !unavailable,
            ) {
                scope.launch { controller.saveMergeMarkers() }
            }
            Spacer(Modifier.width(8.dp))
            MergeAction(strings.repoMergeReload, enabled = !controller.busy) {
                scope.launch { controller.reloadMergeEditor() }
            }
        }
        Divider()

        if (unavailable) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error ?: strings.repoMergeUnavailable,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.repoMergeStageHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(23.dp).background(Term.panel)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MergeTabButton(
                    "${strings.repoMergeConflicts} (${controller.mergeConflicts.size})",
                    active = tab == MergeTab.Conflicts,
                ) { tab = MergeTab.Conflicts }
                MergeTabButton(strings.repoMergeBase, tab == MergeTab.Base) { tab = MergeTab.Base }
                MergeTabButton(strings.repoMergeOurs, tab == MergeTab.Ours) { tab = MergeTab.Ours }
                MergeTabButton(strings.repoMergeTheirs, tab == MergeTab.Theirs) { tab = MergeTab.Theirs }
                MergeTabButton(strings.repoMergeManual, tab == MergeTab.Manual) { tab = MergeTab.Manual }
            }
            Divider()

            when (tab) {
                MergeTab.Conflicts -> MergeConflictsView(controller, Modifier.weight(1f))
                MergeTab.Manual -> MergeManualView(controller, Modifier.weight(1f))
                MergeTab.Base -> MergeVersionView(
                    label = "${strings.repoMergeBase} :1:${state.path}",
                    text = state.base,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                MergeTab.Ours -> MergeVersionView(
                    label = "${strings.repoMergeOurs} :2:${state.path}",
                    text = state.ours,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                MergeTab.Theirs -> MergeVersionView(
                    label = "${strings.repoMergeTheirs} :3:${state.path}",
                    text = state.theirs,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        Divider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${strings.repoMergeConflicts}: ${controller.mergeConflicts.size}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (controller.mergeDirty) strings.repoMergeUnsaved else strings.repoMergeSaved,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (controller.mergeDirty) Term.amber else Term.greenDim,
            )
            Spacer(Modifier.weight(1f))
            Text(
                strings.repoMergeStageHint,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private enum class MergeTab { Conflicts, Base, Ours, Theirs, Manual }

@Composable
private fun MergeConflictsView(
    controller: RepoController,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(23.dp).background(Term.black)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "markers: ${controller.mergeConflicts.size}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            MergeAction(
                label = strings.repoMergeTakeAllOurs,
                enabled = !controller.busy && controller.mergeConflicts.isNotEmpty(),
            ) { controller.takeAllMergeSide(MergeSide.Ours) }
            Spacer(Modifier.width(9.dp))
            MergeAction(
                label = strings.repoMergeTakeAllTheirs,
                enabled = !controller.busy && controller.mergeConflicts.isNotEmpty(),
            ) { controller.takeAllMergeSide(MergeSide.Theirs) }
        }
        Divider()
        if (controller.mergeParse.malformed) {
            Text(
                strings.repoMergeMalformed,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.amber,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            )
        }
        if (controller.mergeConflicts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        strings.repoMergeNoConflicts,
                        fontSize = 9.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.repoMergeManualHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(controller.mergeConflicts, key = { it.id }) { conflict ->
                    MergeConflictCard(
                        conflict = conflict,
                        choice = controller.mergeSelections[conflict.id] ?: MergeLineChoice(),
                        controller = controller,
                    )
                }
            }
        }
    }
}

@Composable
private fun MergeConflictCard(
    conflict: MergeConflict,
    choice: MergeLineChoice,
    controller: RepoController,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().background(Term.panel).padding(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${strings.repoMergeConflict.uppercase()} ${conflict.id + 1}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.red,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "${conflict.startLine + 1}–${conflict.endLine + 1}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            conflict.base?.let {
                Spacer(Modifier.width(7.dp))
                Text(
                    "${strings.repoMergeBase}: ${it.size}",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
            Spacer(Modifier.weight(1f))
            MergeAction(strings.repoMergeUseOurs, enabled = !controller.busy) {
                controller.applyMergeConflictSide(conflict.id, MergeSide.Ours)
            }
            Spacer(Modifier.width(6.dp))
            MergeAction(strings.repoMergeUseTheirs, enabled = !controller.busy) {
                controller.applyMergeConflictSide(conflict.id, MergeSide.Theirs)
            }
            Spacer(Modifier.width(6.dp))
            MergeAction(
                label = strings.repoMergeClear,
                enabled = !controller.busy && !choice.isEmpty,
                color = Term.faint,
            ) { controller.clearMergeSelection(conflict.id) }
            Spacer(Modifier.width(6.dp))
            MergeAction(
                label = strings.repoMergeApply,
                enabled = !controller.busy && !choice.isEmpty,
            ) { controller.applyMergeConflict(conflict.id) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            MergeLineColumn(
                lines = conflict.ours,
                side = MergeSide.Ours,
                conflictId = conflict.id,
                choice = choice,
                controller = controller,
                modifier = Modifier.weight(1f),
            )
            VDivider()
            MergeLineColumn(
                lines = conflict.theirs,
                side = MergeSide.Theirs,
                conflictId = conflict.id,
                choice = choice,
                controller = controller,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MergeLineColumn(
    lines: List<String>,
    side: MergeSide,
    conflictId: Int,
    choice: MergeLineChoice,
    controller: RepoController,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier.padding(4.dp)) {
        Text(
            when (side) {
                MergeSide.Ours -> strings.repoMergeOurs
                MergeSide.Theirs -> strings.repoMergeTheirs
            },
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = if (side == MergeSide.Ours) Term.cyan else Term.amber,
        )
        if (lines.isEmpty()) {
            Text("…", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
        }
        lines.forEachIndexed { index, line ->
            val selected = choice.isSelected(side, index)
            Row(
                Modifier.fillMaxWidth()
                    .background(if (selected) Term.green.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable(enabled = !controller.busy) {
                        controller.toggleMergeLine(conflictId, side, index)
                    }
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected) "[x]" else "[ ]",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = if (selected) Term.green else Term.faint,
                    modifier = Modifier.width(18.dp),
                )
                Text(
                    line.ifEmpty { " " },
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = if (selected) Term.text else Term.dim,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MergeManualView(controller: RepoController, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxWidth()) {
        Text(
            strings.repoMergeManualHint,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        )
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Term.panelAlt).padding(6.dp),
        ) {
            BasicTextField(
                value = controller.mergeDraft,
                onValueChange = { controller.editMergeDraft(it) },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    color = Term.text,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                ),
                cursorBrush = SolidColor(Term.green),
            )
        }
    }
}

@Composable
private fun MergeVersionView(label: String, text: String?, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier.background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label.uppercase(),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (text == null) {
                    strings.repoMergeVersionMissing
                } else {
                    "${text.split('\n').size} lines"
                },
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Divider()
        if (text == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    strings.repoMergeVersionMissing,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp)) {
                itemsIndexed(text.split('\n')) { index, raw ->
                    val line = raw.removeSuffix("\r")
                    Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 0.5.dp)) {
                        Text(
                            "${index + 1}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.width(34.dp),
                        )
                        Text(
                            line.ifEmpty { " " },
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = if (active) Term.green else Term.dim,
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (active) Term.green.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun MergeAction(
    label: String,
    enabled: Boolean = true,
    color: Color = Term.green,
    onClick: () -> Unit,
) {
    Text(
        label,
        fontSize = 7.5.sp,
        fontFamily = Term.mono,
        color = if (enabled) color else Term.faint,
        maxLines = 1,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 1.dp),
    )
}

@Composable
private fun DiffFileInfoPanel(info: DiffFileInfo, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    info.image -> strings.repoBinaryImageTitle
                    info.binary -> strings.repoBinaryTitle
                    else -> strings.repoDiffLargeTitle
                },
                fontSize = 9.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.cyan,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                info.path,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            DiffVersionCard(info.old, strings.repoBinaryOld, Modifier.weight(1f))
            Spacer(Modifier.width(7.dp))
            DiffVersionCard(info.new, strings.repoBinaryNew, Modifier.weight(1f))
        }
        if (info.tooLarge) {
            Spacer(Modifier.height(8.dp))
            Text(
                strings.repoDiffTooLarge(
                    formatSize(info.new.size ?: info.old.size),
                    formatSize(info.limitBytes),
                ),
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (info.image && !info.tooLarge) {
            ImageDiffPreview(info, Modifier.weight(1f).fillMaxWidth())
        } else {
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Term.panelAlt),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (info.image) strings.repoBinaryImageUnavailable else strings.repoBinaryNoText,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        }
    }
}

@Composable
private fun DiffVersionCard(version: DiffVersion, label: String, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(
        modifier.background(Term.panelAlt).padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text(
                if (version.missing) strings.repoBinaryMissing else formatSize(version.size),
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (version.missing) Term.amber else Term.text,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            version.label,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ImageDiffPreview(info: DiffFileInfo, modifier: Modifier) {
    val strings = LocalStrings.current
    val oldBitmap = remember(info.old.bytes) { decodeImage(info.old.bytes) }
    val newBitmap = remember(info.new.bytes) { decodeImage(info.new.bytes) }
    var onion by remember(info.path) { mutableStateOf(false) }
    var opacity by remember(info.path) { mutableStateOf(0.5f) }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiffToggle(strings.repoBinarySideBySide, !onion) { onion = false }
            DiffToggle(strings.repoBinaryOnion, onion) { onion = true }
            if (onion) {
                Spacer(Modifier.width(8.dp))
                Text(strings.repoBinaryOpacity, fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                )
                Text("${(opacity * 100).toInt()}%", fontSize = 8.sp, fontFamily = Term.mono, color = Term.dim)
            }
        }
        if (onion) {
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Term.panelAlt),
                contentAlignment = Alignment.Center,
            ) {
                oldBitmap?.let {
                    Image(it, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                }
                newBitmap?.let {
                    Image(
                        it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp).alpha(opacity),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (oldBitmap == null && newBitmap == null) {
                    Text(strings.repoBinaryImageUnavailable, fontSize = 9.sp, fontFamily = Term.mono, color = Term.amber)
                }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ImageDiffPane(oldBitmap, info.old, strings.repoBinaryOld, Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(Term.border))
                ImageDiffPane(newBitmap, info.new, strings.repoBinaryNew, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ImageDiffPane(
    bitmap: ImageBitmap?,
    version: DiffVersion,
    label: String,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    Box(modifier.fillMaxHeight().background(Term.black), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                if (version.missing) strings.repoBinaryMissing else strings.repoBinaryImageUnavailable,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
        }
        Text(
            label.uppercase(),
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.align(Alignment.TopStart).padding(5.dp),
        )
        if (bitmap != null) {
            Text(
                "${bitmap.width}×${bitmap.height}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
            )
        }
    }
}

private fun decodeImage(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null) return null
    return runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

private fun formatSize(bytes: Long?): String {
    if (bytes == null) return "—"
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return "${(kib * 10).toLong() / 10.0} KiB"
    val mib = kib / 1024.0
    return "${(mib * 10).toLong() / 10.0} MiB"
}

@Composable
private fun DiffToolbar(
    options: DiffViewOptions,
    onOptionsChange: (DiffViewOptions) -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().height(22.dp).background(Term.panelAlt)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("VIEW", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint)
        DiffToggle(strings.repoDiffUnified, options.layout == DiffLayout.Unified) {
            onOptionsChange(options.copy(layout = DiffLayout.Unified))
        }
        DiffToggle(strings.repoDiffSplit, options.layout == DiffLayout.SideBySide) {
            onOptionsChange(options.copy(layout = DiffLayout.SideBySide))
        }
        Spacer(Modifier.width(8.dp))
        Text("COMPARE", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint)
        DiffToggle(strings.repoDiffIgnoreWhitespace, options.ignoreWhitespace) {
            onOptionsChange(options.copy(ignoreWhitespace = !options.ignoreWhitespace))
        }
        DiffToggle(strings.repoDiffIgnoreCase, options.ignoreCase) {
            onOptionsChange(options.copy(ignoreCase = !options.ignoreCase))
        }
        Spacer(Modifier.weight(1f))
        Text(
            strings.repoDiffWordSyntax,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DiffToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        if (active) "[$label]" else " $label ",
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = if (active) Term.green else Term.faint,
        maxLines = 1,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 2.dp),
    )
}

@Composable
private fun DiffViewContentRow(
    row: DiffViewRow,
    parsed: ParsedDiff,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
) {
    when (row) {
        is DiffViewRow.Full -> when (row.view.line.kind) {
            DiffLineKind.Hunk -> HunkHeaderRow(
                view = row.view,
                parsed = parsed,
                staged = staged,
                controller = controller,
                scope = scope,
                discardEntry = discardEntry,
                onDiscard = onDiscard,
            )
            DiffLineKind.Header, DiffLineKind.Meta -> FullTextDiffRow(row.view)
            else -> UnifiedDiffRow(
                view = row.view,
                parsed = parsed,
                staged = staged,
                controller = controller,
                scope = scope,
                discardEntry = discardEntry,
                onDiscard = onDiscard,
            )
        }
        is DiffViewRow.Split -> SplitDiffRow(
            row = row,
            parsed = parsed,
            staged = staged,
            controller = controller,
            scope = scope,
            discardEntry = discardEntry,
            onDiscard = onDiscard,
        )
        is DiffViewRow.Omitted -> OmittedDiffRow(row.count)
    }
}

@Composable
private fun FullTextDiffRow(view: DiffLineView) {
    Row(
        Modifier.fillMaxWidth().background(Term.black).padding(horizontal = 8.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            view.text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HunkHeaderRow(
    view: DiffLineView,
    parsed: ParsedDiff,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
) {
    val strings = LocalStrings.current
    val line = view.line
    val hunk = parsed.hunks.getOrNull(line.hunkIndex)
    val canApply = hunk != null && !hunk.isDeletion
    val selectedForCommit = hunk != null && line.hunkIndex in controller.selectedHunks
    val discardTargetEntry = discardEntry?.takeIf {
        canApply && !staged && controller.canDiscardSelectedPartially
    }
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (selectedForCommit) Term.amber.copy(alpha = 0.08f) else Term.black,
            )
            .padding(horizontal = 8.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canApply && !staged) {
            Text(
                if (selectedForCommit) "[x]" else "[ ]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (selectedForCommit) Term.amber else Term.faint,
                modifier = Modifier.clickable(enabled = !controller.busy) {
                    controller.toggleHunkSelection(hunk.index)
                },
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            view.text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.cyan,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
        if (hunk != null && canApply) {
            Spacer(Modifier.width(6.dp))
            Text(
                if (staged) "[${strings.repoUnstageHunk}]" else "[${strings.repoStageHunk}]",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.cyan,
                maxLines = 1,
                modifier = Modifier.clickable(enabled = !controller.busy) {
                    scope.launch { controller.applyHunk(hunk.index) }
                },
            )
            if (discardTargetEntry != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "[${strings.repoDiscard}]",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.red.copy(alpha = 0.85f),
                    maxLines = 1,
                    modifier = Modifier.clickable(enabled = !controller.busy) {
                        onDiscard(
                            DiscardTarget(
                                entry = discardTargetEntry,
                                scope = DiscardScope.Hunk,
                                hunkIndex = hunk.index,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun UnifiedDiffRow(
    view: DiffLineView,
    parsed: ParsedDiff,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
) {
    val line = view.line
    val hunk = parsed.hunks.getOrNull(line.hunkIndex)
    val color = diffLineColor(line.kind)
    Row(
        Modifier.fillMaxWidth().background(diffLineBackground(line.kind))
            .padding(horizontal = 8.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            diffPrefix(line.kind),
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = color,
            maxLines = 1,
            modifier = Modifier.width(8.dp),
        )
        Text(
            diffAnnotatedText(view),
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
        LineActions(
            view = view,
            hunk = hunk,
            staged = staged,
            controller = controller,
            scope = scope,
            discardEntry = discardEntry,
            onDiscard = onDiscard,
        )
    }
}

@Composable
private fun SplitDiffRow(
    row: DiffViewRow.Split,
    parsed: ParsedDiff,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 18.dp)) {
        SplitDiffCell(
            view = row.left,
            modifier = Modifier.weight(1f),
            parsed = parsed,
            staged = staged,
            controller = controller,
            scope = scope,
            discardEntry = discardEntry,
            onDiscard = onDiscard,
        )
        Box(Modifier.width(1.dp).height(18.dp).background(Term.border))
        SplitDiffCell(
            view = row.right,
            modifier = Modifier.weight(1f),
            parsed = parsed,
            staged = staged,
            controller = controller,
            scope = scope,
            discardEntry = discardEntry,
            onDiscard = onDiscard,
        )
    }
}

@Composable
private fun SplitDiffCell(
    view: DiffLineView?,
    modifier: Modifier,
    parsed: ParsedDiff,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
) {
    Row(
        modifier.background(
            if (view == null) Term.panelAlt else diffLineBackground(view.line.kind),
        ).padding(horizontal = 4.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (view == null) {
            Spacer(Modifier.weight(1f))
        } else {
            val line = view.line
            Text(
                diffAnnotatedText(view),
                fontSize = 8.2.sp,
                fontFamily = Term.mono,
                color = diffLineColor(line.kind),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
            )
            LineActions(
                view = view,
                hunk = parsed.hunks.getOrNull(line.hunkIndex),
                staged = staged,
                controller = controller,
                scope = scope,
                discardEntry = discardEntry,
                onDiscard = onDiscard,
                compact = true,
            )
        }
    }
}

@Composable
private fun LineActions(
    view: DiffLineView,
    hunk: DiffHunk?,
    staged: Boolean,
    controller: RepoController,
    scope: CoroutineScope,
    discardEntry: StatusEntry?,
    onDiscard: (DiscardTarget) -> Unit,
    compact: Boolean = false,
) {
    val strings = LocalStrings.current
    val line = view.line
    if (hunk == null || hunk.isDeletion) return
    if (line.kind != DiffLineKind.Added && line.kind != DiffLineKind.Removed) return

    val discardTargetEntry = discardEntry?.takeIf {
        !staged && controller.canDiscardSelectedPartially
    }
    Spacer(Modifier.width(if (compact) 3.dp else 6.dp))
    Text(
        if (staged) "[-]" else "[+]",
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        color = if (staged) Term.red.copy(alpha = 0.8f) else Term.green,
        maxLines = 1,
        modifier = Modifier.clickable(enabled = !controller.busy) {
            scope.launch {
                controller.applyLine(hunk.index, line.lineIndex)
            }
        },
    )
    if (discardTargetEntry != null) {
        Spacer(Modifier.width(if (compact) 3.dp else 5.dp))
        Text(
            "[${strings.repoDiscard}]",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.red.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.clickable(enabled = !controller.busy) {
                onDiscard(
                    DiscardTarget(
                        entry = discardTargetEntry,
                        scope = DiscardScope.Lines,
                        hunkIndex = hunk.index,
                        lineIndexes = setOf(line.lineIndex),
                    ),
                )
            },
        )
    }
}

@Composable
private fun OmittedDiffRow(count: Int) {
    Row(
        Modifier.fillMaxWidth().background(Term.panelAlt).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("≈ ", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.amber)
        Text(
            LocalStrings.current.repoDiffHiddenChanges(count),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun diffPrefix(kind: DiffLineKind): String = when (kind) {
    DiffLineKind.Added -> "+"
    DiffLineKind.Removed -> "-"
    else -> " "
}

private fun diffLineColor(kind: DiffLineKind): Color = when (kind) {
    DiffLineKind.Added -> Term.green
    DiffLineKind.Removed -> Term.red
    DiffLineKind.Hunk -> Term.cyan
    DiffLineKind.Header, DiffLineKind.Meta -> Term.faint
    DiffLineKind.Context -> Term.dim
}

private fun diffLineBackground(kind: DiffLineKind): Color = when (kind) {
    DiffLineKind.Added -> Term.green.copy(alpha = 0.06f)
    DiffLineKind.Removed -> Term.red.copy(alpha = 0.06f)
    else -> Term.black
}

private fun diffAnnotatedText(view: DiffLineView): AnnotatedString = buildAnnotatedString {
    append(view.text)
    view.syntaxSpans.forEach { span ->
        if (span.start in 0..view.text.length && span.end in span.start..view.text.length) {
            addStyle(SpanStyle(color = syntaxColor(span.kind)), span.start, span.end)
        }
    }
    view.wordSpans.forEach { span ->
        if (span.start in 0..view.text.length && span.end in span.start..view.text.length) {
            addStyle(
                SpanStyle(
                    background = Term.amber.copy(alpha = 0.28f),
                    color = Term.white,
                    fontWeight = FontWeight.Bold,
                ),
                span.start,
                span.end,
            )
        }
    }
}

private fun syntaxColor(kind: SyntaxKind): Color = when (kind) {
    SyntaxKind.Keyword -> Term.magenta
    SyntaxKind.String -> Term.green
    SyntaxKind.Comment -> Term.faint
    SyntaxKind.Number -> Term.cyan
    SyntaxKind.Type -> Term.amber
    SyntaxKind.Function -> Term.cyan
    SyntaxKind.Constant -> Term.amber
    SyntaxKind.Annotation -> Term.magenta
    SyntaxKind.Tag -> Term.cyan
    SyntaxKind.Attribute -> Term.amber
}


@Composable
private fun CommitBar(
    controller: RepoController,
    scope: CoroutineScope,
    onOptions: () -> Unit,
) {
    val strings = LocalStrings.current
    val generator = remember { CommitMessageGenerator() }
    val aiEnabled = remember { generator.loadSettings().enabled }
    var aiPreparing by remember { mutableStateOf(false) }
    var aiGenerating by remember { mutableStateOf(false) }
    var aiPrepared by remember { mutableStateOf<PreparedCommitMessage?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiGenerationJob by remember { mutableStateOf<Job?>(null) }
    var aiRequestId by remember { mutableStateOf(0L) }
    var aiPreviousMessage by remember { mutableStateOf<String?>(null) }
    var aiRestorable by remember { mutableStateOf(false) }
    val stagedCount = controller.status?.stagedCount ?: 0
    val worktreeCount = controller.status?.unstagedCount ?: 0

    fun cancelGeneration() {
        aiRequestId += 1
        aiGenerationJob?.cancel()
        aiGenerationJob = null
        aiGenerating = false
    }

    fun startGeneration(prepared: PreparedCommitMessage) {
        if (aiGenerating || controller.busy) return
        aiPrepared = null
        aiError = null
        aiGenerating = true
        val requestId = ++aiRequestId
        aiGenerationJob = scope.launch {
            try {
                val message = withTimeout(45_000) { generator.generate(prepared) }
                if (requestId != aiRequestId) return@launch
                aiPreviousMessage = controller.commitMessage
                aiRestorable = true
                controller.commitMessage = message
            } catch (timeout: TimeoutCancellationException) {
                if (requestId == aiRequestId) aiError = strings.aiError("timeout")
            } catch (cancelled: CancellationException) {
                // The user cancelled or left the repository; leave the message field untouched.
            } catch (failure: Exception) {
                if (requestId == aiRequestId) aiError = strings.aiError(failure.message.orEmpty())
            } finally {
                if (requestId == aiRequestId) {
                    aiGenerating = false
                    aiGenerationJob = null
                }
            }
        }
    }

    fun prepareGeneration(source: AiDiffSource) {
        if (aiPreparing || aiGenerating || controller.busy) return
        aiError = null
        aiPreparing = true
        val branch = controller.status?.branch?.let { branch ->
            when {
                branch.detached -> "detached HEAD"
                branch.head.isNullOrBlank() -> "unborn branch"
                else -> branch.head
            }
        } ?: "detached HEAD"
        val subjects = controller.commits.take(8).map { it.subject }
        val files = if (source == AiDiffSource.Staged) stagedCount else worktreeCount
        scope.launch {
            try {
                val diff = withContext(Dispatchers.IO) {
                    if (source == AiDiffSource.Staged) {
                        controller.stagedDiffForAi()
                    } else {
                        controller.worktreeDiffForAi()
                    }
                }
                if (diff == null) {
                    aiError = strings.aiError(controller.error.orEmpty())
                    controller.dismissError()
                    return@launch
                }
                val prepared = withContext(Dispatchers.IO) {
                    generator.prepare(diff, branch, subjects, files, source)
                }
                aiPrepared = prepared
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                aiError = strings.aiError(failure.message.orEmpty())
            } finally {
                aiPreparing = false
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Divider()
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(Term.panel)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$stagedCount staged",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                modifier = Modifier.width(62.dp),
            )
            Text("$", fontSize = 11.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.green)
            Spacer(Modifier.width(5.dp))
            TermTextField(
                value = controller.commitMessage,
                onValueChange = { controller.commitMessage = it },
                placeholder = "commit message…",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(5.dp))
            ActionButton(
                key = null,
                label = when {
                    aiPreparing || aiGenerating -> strings.aiGenerating
                    controller.commitMessage.isNotBlank() -> strings.aiRegenerate
                    else -> strings.aiGenerate
                },
                enabled = !controller.busy && aiEnabled && stagedCount > 0 &&
                    !aiPreparing && !aiGenerating,
                onClick = { prepareGeneration(AiDiffSource.Staged) },
            )
            if (aiEnabled && worktreeCount > 0) {
                Spacer(Modifier.width(4.dp))
                TermButton("w", strings.aiGenerateWorktree, active = false) {
                    prepareGeneration(AiDiffSource.Worktree)
                }
            }
            Spacer(Modifier.width(5.dp))
            TermButton("o", strings.repoCommitOptions, active = controller.commitOptionsActive, onClick = onOptions)
            Spacer(Modifier.width(4.dp))
            TermButton("a", "amend", active = controller.amend) {
                controller.amend = !controller.amend
                if (controller.amend) controller.commitMode = CommitMode.Normal
            }
            Spacer(Modifier.width(4.dp))
            TermButton("g", "gpg", active = controller.sign) { controller.sign = !controller.sign }
            Spacer(Modifier.width(7.dp))
            val selectedCount = controller.selectedHunks.size
            val action = when (controller.commitMode) {
                CommitMode.Normal -> "COMMIT"
                CommitMode.Fixup -> "FIXUP"
                CommitMode.Squash -> "SQUASH"
            }
            ActionButton(
                key = null,
                label = when {
                    controller.busy -> "WAIT…"
                    selectedCount > 0 -> "$action $selectedCount HUNK${if (selectedCount == 1) "" else "S"}"
                    else -> action
                },
                primary = true,
                enabled = !controller.busy && (selectedCount > 0 || controller.canCommit),
                onClick = {
                    scope.launch {
                        if (selectedCount > 0) {
                            controller.commitSelectedHunks()
                        } else {
                            controller.commit()
                        }
                    }
                },
            )
        }
        if (aiPreparing || aiGenerating || aiError != null || (aiRestorable && controller.commitMessage.isNotBlank())) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 21.dp).background(Term.panelAlt)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        aiPreparing -> strings.aiPreparing
                        aiGenerating -> strings.aiGenerating
                        aiError != null -> aiError.orEmpty()
                        else -> ""
                    },
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = if (aiError != null) Term.red else Term.dim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (aiGenerating) {
                    Text(
                        "[${strings.aiCancel}]",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                        modifier = Modifier.clickable { cancelGeneration() },
                    )
                }
                if (aiRestorable && controller.commitMessage.isNotBlank()) {
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "[${strings.aiRestore}]",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.greenDim,
                        modifier = Modifier.clickable {
                            controller.commitMessage = aiPreviousMessage.orEmpty()
                            aiRestorable = false
                        },
                    )
                }
            }
        }
    }

    aiPrepared?.let { prepared ->
        AiCommitSendDialog(
            prepared = prepared,
            onDismiss = { aiPrepared = null },
            onSend = {
                aiPrepared = null
                startGeneration(prepared)
            },
        )
    }
}

@Composable
private fun LogTab(
    controller: RepoController,
    selectedHash: String?,
    focusedCommit: Commit?,
    onSelect: (String) -> Unit,
    onCommitAction: (CommitActionRequest) -> Unit,
    onCommitRewrite: (Commit, RewriteMode) -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var filtersOpen by remember(controller.repository.root) { mutableStateOf(false) }
    var draft by remember(controller.logFilter) { mutableStateOf(controller.logFilter) }
    val graph = remember(controller.commits, controller.collapsedMerges) {
        controller.commitGraph()
    }
    val listState = rememberLazyListState()
    val graphWidth = graphColumnWidth(graph.laneCount)
    val selected = graph.rows.firstOrNull { it.commit.hash == selectedHash }?.commit
        ?: focusedCommit?.takeIf { it.hash == selectedHash }
        ?: graph.rows.firstOrNull()?.commit

    // Подгружаем следующую страницу заранее, но только когда пользователь уже
    // прокрутил близко к концу загруженного окна. Первая страница всегда
    // остаётся ограниченной 200 коммитами, независимо от размера репозитория.
    LaunchedEffect(listState, graph.rows.size) {
        if (!controller.historyHasMore) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= graph.rows.size - 25 &&
                    !controller.historyLoadingMore &&
                    !controller.busy
                ) {
                    scope.launch { controller.loadMoreHistory() }
                }
            }
    }

    Column(Modifier.fillMaxSize()) {
        if (filtersOpen) {
            LogFilterPanel(
                controller = controller,
                draft = draft,
                onDraftChange = { draft = it },
                scope = scope,
            )
            Divider()
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.weight(1f).fillMaxHeight().background(Term.panel)) {
                Row(
                    Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                        .padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("GRAPH", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(graphWidth))
                    Text("COMMIT", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(62.dp))
                    Text("MESSAGE", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.weight(1f))
                    Text("AUTHOR", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(70.dp))
                    Text("AGE", fontSize = 8.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(38.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "[${strings.logFilters}]${if (controller.logFilter.isActive) " (${controller.logFilter.activeCount})" else ""}",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = if (controller.logFilter.isActive) Term.amber else Term.greenDim,
                        modifier = Modifier.clickable { filtersOpen = !filtersOpen },
                    )
                }
                Divider()
                if (graph.rows.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (controller.logFilter.isActive) strings.logFilterNoMatch else "no commits yet",
                            fontSize = 9.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(graph.rows, key = { it.commit.hash }) { row ->
                            CommitRow(
                                row = row,
                                graphWidth = graphWidth,
                                selected = row.commit.hash == selected?.hash,
                                collapsed = row.commit.hash in controller.collapsedMerges,
                                collapsedCount = graph.collapsedCount(row.commit.hash),
                                onToggleCollapse = { controller.toggleMergeCollapsed(row.commit.hash) },
                                onClick = { onSelect(row.commit.hash) },
                            )
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                        }
                        if (controller.historyHasMore || controller.historyLoadingMore) {
                            item(key = "history-load-more") {
                                Row(
                                    Modifier.fillMaxWidth().height(30.dp)
                                        .background(Term.panelAlt)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    ActionButton(
                                        key = null,
                                        label = if (controller.historyLoadingMore) {
                                            "LOADING HISTORY…"
                                        } else {
                                            "LOAD MORE · ${controller.commits.size} LOADED"
                                        },
                                        enabled = !controller.busy && !controller.historyLoadingMore,
                                    ) {
                                        scope.launch { controller.loadMoreHistory() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            VDivider()
            CommitDetails(
                controller = controller,
                commit = selected,
                onCommitAction = onCommitAction,
                onCommitRewrite = onCommitRewrite,
                modifier = Modifier.width(460.dp).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun LogFilterPanel(
    controller: RepoController,
    draft: LogFilter,
    onDraftChange: (LogFilter) -> Unit,
    scope: CoroutineScope,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().background(Term.panel).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            LogFilterField(
                label = strings.logFilterBranch,
                value = draft.branch,
                placeholder = strings.logFilterBranchPlaceholder,
                onValueChange = { onDraftChange(draft.copy(branch = it)) },
                modifier = Modifier.weight(1.1f),
            )
            Spacer(Modifier.width(6.dp))
            LogFilterField(
                label = strings.logFilterAuthor,
                value = draft.author,
                placeholder = strings.logFilterAuthorPlaceholder,
                onValueChange = { onDraftChange(draft.copy(author = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            LogFilterField(
                label = strings.logFilterPath,
                value = draft.path,
                placeholder = strings.logFilterPathPlaceholder,
                onValueChange = { onDraftChange(draft.copy(path = it)) },
                modifier = Modifier.weight(1.1f),
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            LogFilterField(
                label = strings.logFilterSince,
                value = draft.since,
                placeholder = strings.logFilterDatePlaceholder,
                onValueChange = { onDraftChange(draft.copy(since = it)) },
                modifier = Modifier.weight(0.8f),
            )
            Spacer(Modifier.width(6.dp))
            LogFilterField(
                label = strings.logFilterUntil,
                value = draft.until,
                placeholder = strings.logFilterDatePlaceholder,
                onValueChange = { onDraftChange(draft.copy(until = it)) },
                modifier = Modifier.weight(0.8f),
            )
            Spacer(Modifier.width(6.dp))
            LogFilterField(
                label = strings.logFilterMessage,
                value = draft.message,
                placeholder = strings.logFilterMessagePlaceholder,
                onValueChange = { onDraftChange(draft.copy(message = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            LogFilterField(
                label = strings.logFilterPickaxe,
                value = draft.pickaxe,
                placeholder = strings.logFilterPickaxePlaceholder,
                onValueChange = { onDraftChange(draft.copy(pickaxe = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TermButton(
                hotkey = "m",
                label = strings.logFilterOnlyMine,
                active = draft.onlyMine,
                onClick = { onDraftChange(draft.copy(onlyMine = !draft.onlyMine)) },
            )
            Spacer(Modifier.width(6.dp))
            TermButton(
                hotkey = "c",
                label = strings.logFilterOnlyCurrent,
                active = draft.onlyCurrent,
                onClick = { onDraftChange(draft.copy(onlyCurrent = !draft.onlyCurrent)) },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                strings.logFilterHint,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(key = null, label = strings.logFilterClear, enabled = !controller.busy) {
                onDraftChange(LogFilter())
                scope.launch { controller.applyLogFilter(LogFilter()) }
            }
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = strings.logFilterApply,
                enabled = !controller.busy,
                primary = true,
            ) {
                scope.launch { controller.applyLogFilter(draft) }
            }
        }
    }
}

@Composable
private fun LogFilterField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            label,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
        )
        Spacer(Modifier.height(2.dp))
        TermTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
        )
    }
}

@Composable
private fun CommitRow(
    row: CommitGraphRow,
    graphWidth: Dp,
    selected: Boolean,
    collapsed: Boolean,
    collapsedCount: Int,
    onToggleCollapse: () -> Unit,
    onClick: () -> Unit,
) {
    val commit = row.commit
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .background(if (selected) Term.green.copy(alpha = 0.12f) else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphCell(row, Modifier.width(graphWidth))
        Text(
            commit.shortHash,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (selected) Term.green else Term.faint,
            modifier = Modifier.width(62.dp),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (commit.isMerge) {
                Text(
                    "M",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = Term.amber,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Text(
                commit.subject,
                fontSize = 9.5.sp,
                fontFamily = Term.mono,
                color = if (selected) Term.white else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (collapsedCount > 0) {
                Text(
                    if (collapsed) "[+$collapsedCount]" else "[-$collapsedCount]",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.amber,
                    modifier = Modifier.padding(start = 5.dp).clickable { onToggleCollapse() },
                )
            }
        }
        Text(
            commit.authorName,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            modifier = Modifier.width(70.dp),
        )
        Text(
            formatAge(commit.authorTime),
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.width(38.dp),
        )
    }
}

@Composable
private fun GraphCell(row: CommitGraphRow, modifier: Modifier) {
    Text(
        graphText(row),
        fontSize = 9.5.sp,
        fontFamily = Term.mono,
        maxLines = 1,
        modifier = modifier,
    )
}

private fun graphText(row: CommitGraphRow): AnnotatedString {
    val chars = arrayOfNulls<Char>(row.laneCount)
    row.activeBefore.forEach { lane -> chars[lane] = '|' }
    row.activeAfter.forEach { lane -> chars[lane] = '|' }
    row.parentLanes.forEach { parentLane ->
        chars[parentLane] = when {
            parentLane == row.lane -> '|'
            parentLane > row.lane -> '\\'
            else -> '/'
        }
    }
    chars[row.lane] = if (row.commit.isMerge) '◆' else '*'

    return buildAnnotatedString {
        for (lane in 0 until row.laneCount) {
            if (lane > 0) append(' ')
            val glyph = chars[lane]
            if (glyph == null) {
                append(' ')
            } else {
                withStyle(SpanStyle(color = laneColor(lane))) { append(glyph) }
            }
        }
    }
}

private fun laneColor(lane: Int): Color = when (lane % 5) {
    0 -> Term.green
    1 -> Term.cyan
    2 -> Term.magenta
    3 -> Term.amber
    else -> Term.greenDim
}

private fun graphColumnWidth(laneCount: Int) =
    ((laneCount.coerceAtLeast(1) * 12) + 8).coerceAtLeast(58).dp

@Composable
private fun CommitDetails(
    controller: RepoController,
    commit: Commit?,
    onCommitAction: (CommitActionRequest) -> Unit,
    onCommitRewrite: (Commit, RewriteMode) -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    var tab by remember(commit?.hash) { mutableStateOf(CommitDetailTab.Info) }
    LaunchedEffect(commit?.hash) {
        if (commit != null) controller.loadCommitInspection(commit.hash)
    }
    val inspection = controller.commitInspection?.takeIf { it.hash == commit?.hash }

    Column(modifier.background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("INSPECT", fontSize = 8.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            if (commit?.isMerge == true) {
                Spacer(Modifier.width(6.dp))
                Text("MERGE", fontSize = 8.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.amber)
            }
            Spacer(Modifier.weight(1f))
            Text(commit?.shortHash ?: "—", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
        }
        Divider()
        if (commit == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.repoCommitSelect, fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
            }
            return@Column
        }

        Row(Modifier.fillMaxWidth().height(22.dp).background(Term.panelAlt)) {
            CommitDetailTabButton("INFO", tab == CommitDetailTab.Info) { tab = CommitDetailTab.Info }
            CommitDetailTabButton("DIFF", tab == CommitDetailTab.Diff) { tab = CommitDetailTab.Diff }
            CommitDetailTabButton("STAT", tab == CommitDetailTab.Stat) { tab = CommitDetailTab.Stat }
            Spacer(Modifier.weight(1f))
            if (inspection != null) {
                Text(
                    "${strings.commitFiles} ${inspection.stats.files.size}",
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Divider()

        when (tab) {
            CommitDetailTab.Info -> CommitInfoTab(
                commit = commit,
                inspection = inspection,
                controller = controller,
                onAction = { action ->
                    onCommitAction(CommitActionRequest(action, commit))
                },
                onRewrite = onCommitRewrite,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            CommitDetailTab.Diff -> CommitDiffTab(
                inspection = inspection,
                error = controller.commitInspectionError,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            CommitDetailTab.Stat -> CommitStatTab(
                inspection = inspection,
                error = controller.commitInspectionError,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun CommitDetailTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().background(if (active) Term.panel else Term.panelAlt)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Term.green else Term.faint,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun CommitInfoTab(
    commit: Commit,
    inspection: CommitInspection?,
    controller: RepoController,
    onAction: (CommitAction) -> Unit,
    onRewrite: (Commit, RewriteMode) -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(9.dp),
    ) {
        Text(
            commit.subject,
            fontSize = 11.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.white,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        LabeledValue("COMMIT", commit.hash)
        LabeledValue("AUTHOR", "${commit.authorName} <${commit.authorEmail}>")
        LabeledValue("DATE", formatDate(commit.authorTime))
        LabeledValue("PARENTS", commit.parents.joinToString(" ").ifBlank { "—" })
        if (commit.refs.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SectionLabel("REFS")
            Spacer(Modifier.height(3.dp))
            commit.refs.forEach { ref ->
                Text(
                    "[$ref]",
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = when {
                        ref.contains("tag:") -> Term.amber
                        ref.contains("HEAD") -> Term.green
                        else -> Term.cyan
                    },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionLabel(strings.commitSignature)
        Text(
            "git show -s --format=%G?%x00%GS%x00%GK ${commit.shortHash}",
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.height(4.dp))
        when {
            inspection != null -> CommitSignatureView(inspection.signature)
            controller.commitInspectionLoading -> Text(
                strings.commitInspectLoading,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            controller.commitInspectionError != null -> Text(
                "${strings.commitInspectFailed}: ${controller.commitInspectionError}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.red,
            )
            else -> CommitSignatureView(null)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                key = null,
                label = if (commit.isMerge) {
                    strings.signatureVerifyMerge
                } else {
                    strings.signatureVerifyCommit
                },
                enabled = !controller.busy && !controller.signatureVerificationLoading,
                onClick = {
                    scope.launch {
                        controller.verifyCommitSignature(commit.hash, merge = commit.isMerge)
                    }
                },
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (commit.isMerge) {
                    "git verify-commit ${commit.shortHash} · " +
                        "git show --show-signature ${commit.shortHash}"
                } else {
                    "git verify-commit ${commit.shortHash}"
                },
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (controller.signatureVerificationLoading) {
            Spacer(Modifier.height(4.dp))
            Text(
                strings.signatureVerificationRunning,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.amber,
            )
        }
        controller.signatureVerification
            ?.takeIf { it.target == commit.hash }
            ?.let { report ->
                Spacer(Modifier.height(6.dp))
                SignatureVerificationView(report)
            }

        if (inspection != null) {
            Spacer(Modifier.height(10.dp))
            SectionLabel("STAT")
            Text(
                "+${inspection.stats.additions}  −${inspection.stats.deletions}  ·  ${strings.commitFiles} ${inspection.stats.files.size}",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
            )
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(7.dp))
        SectionLabel("ACTIONS")
        Spacer(Modifier.height(3.dp))
        val enabled = !controller.busy
        CommitActionRow(
            label = strings.commitActionCheckout,
            command = "git checkout ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.Checkout) }
        CommitActionRow(
            label = strings.commitActionReset,
            command = "git reset --mixed ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.Reset) }
        CommitActionRow(
            label = strings.commitActionInteractiveRebase,
            command = "git rebase -i --rebase-merges " +
                (commit.parents.firstOrNull()?.take(7) ?: "--root"),
            enabled = enabled,
        ) { onAction(CommitAction.InteractiveRebase) }
        CommitActionRow(
            label = strings.commitActionDelete,
            command = "git rebase -i --rebase-merges " +
                (commit.parents.firstOrNull()?.take(7) ?: "--root") +
                "  # drop / reset --hard / revert / filter-repo",
            enabled = enabled,
        ) { onAction(CommitAction.Delete) }
        CommitActionRow(
            label = strings.commitActionHistoryRewrite,
            command = "git rebase --onto <new-base> <upstream>  # reorder / move range / squash",
            enabled = enabled,
        ) { onAction(CommitAction.HistoryRewrite) }
        CommitActionRow(
            label = strings.commitActionFilterRepo,
            command = "git filter-repo (--invert-paths|--replace-text|--path-rename) …",
            enabled = enabled,
        ) { onAction(CommitAction.FilterRepo) }
        CommitActionRow(
            label = strings.commitActionRewrite,
            command = "git commit-tree ${commit.shortHash}^{tree} -p … -m …",
            enabled = enabled,
        ) { onRewrite(commit, RewriteMode.Reword) }
        if (commit.parents.size == 1) {
            CommitActionRow(
                label = strings.commitActionSplit,
                command = "git reset --hard ${commit.shortHash} && git reset --mixed ${commit.shortHash}^1",
                enabled = enabled,
            ) { onRewrite(commit, RewriteMode.Split) }
        }
        CommitActionRow(
            label = strings.commitActionCherryPick,
            command = "git cherry-pick ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.CherryPick) }
        CommitActionRow(
            label = strings.commitActionRevert,
            command = "git revert --no-edit ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.Revert) }
        CommitActionRow(
            label = strings.commitActionBranch,
            command = "git branch <name> ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.Branch) }
        CommitActionRow(
            label = strings.commitActionTag,
            command = "git tag <name> ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.Tag) }
        CommitActionRow(
            label = strings.commitActionCopyHash,
            command = commit.hash,
            enabled = true,
        ) { onAction(CommitAction.CopyHash) }
        CommitActionRow(
            label = strings.commitActionReflog,
            command = "git reflog --all",
            enabled = enabled,
        ) { onAction(CommitAction.Reflog) }
        CommitActionRow(
            label = strings.commitActionBisectGood,
            command = "git bisect good ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.BisectGood) }
        CommitActionRow(
            label = strings.commitActionBisectBad,
            command = "git bisect bad ${commit.shortHash}",
            enabled = enabled,
        ) { onAction(CommitAction.BisectBad) }
    }
}

@Composable
private fun CommitActionRow(
    label: String,
    command: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(20.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (enabled) Term.green else Term.faint,
            modifier = Modifier.width(126.dp),
        )
        Text(
            command,
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CommitSignatureView(signature: CommitSignature?) {
    val strings = LocalStrings.current
    val status = signature?.status ?: CommitSignatureStatus.Unknown
    val text = signatureStatusText(strings, status)
    val color = signatureStatusColor(status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("●", fontSize = 9.sp, fontFamily = Term.mono, color = color)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 9.sp, fontFamily = Term.mono, color = color)
    }
    signature?.signer?.let { LabeledValue(strings.commitSigner, it) }
    signature?.key?.let { LabeledValue(strings.commitKey, it) }
}

@Composable
private fun CommitDiffTab(
    inspection: CommitInspection?,
    error: String?,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    if (inspection == null) {
        CommitInspectionPlaceholder(error, modifier)
        return
    }
    val rows = remember(inspection.diff) {
        buildDiffView(parsePatch(inspection.diff), null, DiffViewOptions(layout = DiffLayout.Unified))
    }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().background(Term.panelAlt).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$ git show --format= --no-ext-diff --no-color --patch --first-parent ${inspection.hash}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        if (inspection.diffTruncated) {
            Text(
                strings.commitDiffTruncated,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.amber,
                modifier = Modifier.fillMaxWidth().background(Term.amber.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        if (inspection.diff.isBlank()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(strings.commitDiffEmpty, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            }
        } else {
            SelectionContainer(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows) { row ->
                        when (row) {
                            is DiffViewRow.Full -> CommitDiffLineRow(row.view)
                            is DiffViewRow.Split -> CommitDiffLineRow(row.left ?: row.right)
                            is DiffViewRow.Omitted -> FullTextDiffRow(
                                DiffLineView(
                                    line = org.baconana.mybeautifulgit_chan.domain.DiffLine(
                                        text = "… ${row.count} hidden",
                                        kind = DiffLineKind.Meta,
                                    ),
                                    text = "… ${row.count} hidden",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitDiffLineRow(view: org.baconana.mybeautifulgit_chan.domain.DiffLineView?) {
    if (view == null) return
    val kind = view.line.kind
    when (kind) {
        DiffLineKind.Header -> Text(
            view.text,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.greenDim,
            modifier = Modifier.fillMaxWidth().background(Term.black)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        DiffLineKind.Hunk -> Text(
            view.text,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.cyan,
            modifier = Modifier.fillMaxWidth().background(Term.cyan.copy(alpha = 0.06f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        DiffLineKind.Meta -> Text(
            view.text,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.5.dp),
        )
        else -> Row(
            Modifier.fillMaxWidth().background(diffLineBackground(kind))
                .padding(horizontal = 8.dp, vertical = 1.5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                diffPrefix(kind),
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = diffLineColor(kind),
                modifier = Modifier.width(8.dp),
            )
            Text(
                diffAnnotatedText(view),
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = diffLineColor(kind),
                modifier = Modifier.weight(1f),
                softWrap = true,
            )
        }
    }
}

@Composable
private fun CommitStatTab(
    inspection: CommitInspection?,
    error: String?,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    if (inspection == null) {
        CommitInspectionPlaceholder(error, modifier)
        return
    }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().background(Term.panelAlt).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$ git show --format= --numstat --first-parent ${inspection.hash}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "+${inspection.stats.additions}",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.green,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "−${inspection.stats.deletions}",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.red,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${strings.commitFiles} ${inspection.stats.files.size}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Divider()
        if (inspection.stats.files.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(strings.commitStatEmpty, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(inspection.stats.files) { file ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            file.path,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.white,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        if (file.binary) {
                            Text(
                                strings.commitStatBinary,
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.amber,
                            )
                        } else {
                            Text(
                                "+${file.additions ?: 0}",
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.green,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "−${file.deletions ?: 0}",
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
    }
}

@Composable
private fun CommitInspectionPlaceholder(error: String?, modifier: Modifier) {
    val strings = LocalStrings.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            if (error != null) "${strings.commitInspectFailed}: $error" else strings.commitInspectLoading,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = if (error != null) Term.red else Term.faint,
        )
    }
}

@Composable
private fun ConsoleTab(controller: RepoController, scope: CoroutineScope) {
    val strings = LocalStrings.current
    val store = remember(controller.repository.root) { CommandRunnerStore() }
    var command by remember { mutableStateOf("") }
    var history by remember(controller.repository.root) {
        mutableStateOf(store.loadHistory(controller.repository.root))
    }
    var favorites by remember(controller.repository.root) {
        mutableStateOf(store.loadFavorites(controller.repository.root))
    }
    var aliases by remember(controller.repository.root) { mutableStateOf<List<GitAlias>>(emptyList()) }
    var aliasesLoading by remember { mutableStateOf(true) }
    var aliasesReload by remember { mutableStateOf(0) }
    var section by remember { mutableStateOf(RunnerSection.History) }

    LaunchedEffect(controller.repository.root, aliasesReload) {
        aliasesLoading = true
        aliases = controller.loadAliases()
        aliasesLoading = false
    }

    fun submit(background: Boolean) {
        val raw = command.trim()
        if (raw.isEmpty() || controller.busy) return
        command = ""
        history = store.addHistory(controller.repository.root, raw)
        if (background) section = RunnerSection.Jobs
        scope.launch {
            if (background) controller.runCommandInBackground(raw) else controller.runCommand(raw)
        }
    }

    fun runAlias(alias: GitAlias) {
        if (controller.busy) return
        command = ""
        scope.launch { controller.runAlias(alias) }
    }

    fun toggleFavorite(raw: String) {
        val clean = raw.trim()
        if (clean.isEmpty()) return
        favorites = store.toggleFavorite(controller.repository.root, clean)
    }

    fun clearHistory() {
        history = store.clearHistory(controller.repository.root)
    }

    val favoriteCurrent = command.trim().let { it.isNotEmpty() && it in favorites }

    Column(Modifier.fillMaxSize().background(Term.black)) {
        Row(
            Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("OPERATIONS", fontSize = 8.5.sp, fontFamily = Term.mono, fontWeight = FontWeight.SemiBold, color = Term.green, letterSpacing = 1.sp)
            Text(" (${controller.log.size})", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.weight(1f))
            Text(
                "[clear]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clickable { controller.clearLog() },
            )
        }
        Divider()
        RunnerPanel(
            section = section,
            onSectionChange = { section = it },
            history = history,
            favorites = favorites,
            aliases = aliases,
            aliasesLoading = aliasesLoading,
            jobs = controller.backgroundCommands,
            canRun = !controller.busy,
            onUseCommand = { command = "$it " },
            onToggleFavorite = ::toggleFavorite,
            onClearHistory = ::clearHistory,
            onReloadAliases = { aliasesReload += 1 },
            onUseAlias = { alias -> command = alias.name + " " },
            onRunAlias = ::runAlias,
            onClearFinishedJobs = controller::clearFinishedBackgroundCommands,
            onDismissJob = controller::dismissBackgroundCommand,
            onCancelJob = controller::cancelBackgroundCommand,
        )
        Divider()
        if (controller.log.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    strings.repoConsoleEmpty,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 5.dp)) {
                items(controller.log) { line ->
                    val color = when (line.kind) {
                        LogKind.Command -> Term.green
                        LogKind.Output -> Term.dim
                        LogKind.Error -> Term.red
                        LogKind.Success -> Term.greenDim
                        LogKind.Info -> Term.amber
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            line.text,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = color,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (line.kind == LogKind.Command) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "[copy]",
                                fontSize = 8.sp,
                                fontFamily = Term.mono,
                                color = Term.faint,
                                modifier = Modifier.clickable {
                                    DesktopActions.copy(line.text.removePrefix("$ "))
                                },
                            )
                        }
                    }
                }
            }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(Term.panel)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$", fontSize = 11.sp, fontFamily = Term.mono, fontWeight = FontWeight.Bold, color = Term.green)
            Spacer(Modifier.width(6.dp))
            TermTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = strings.repoCommandPlaceholder,
                modifier = Modifier.weight(1f),
                onSubmit = { submit(background = false) },
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (favoriteCurrent) "★" else "☆",
                fontSize = 14.sp,
                fontFamily = Term.mono,
                color = if (favoriteCurrent) Term.amber else Term.faint,
                modifier = Modifier
                    .clickable(enabled = command.isNotBlank() && !controller.busy) {
                        toggleFavorite(command)
                    }
                    .padding(horizontal = 3.dp),
            )
            Spacer(Modifier.width(4.dp))
            ActionButton(
                key = null,
                label = "RUN",
                primary = true,
                enabled = !controller.busy && command.isNotBlank(),
                onClick = { submit(background = false) },
            )
            Spacer(Modifier.width(5.dp))
            ActionButton(
                key = null,
                label = strings.runnerRunBackground,
                enabled = !controller.busy && command.isNotBlank(),
                onClick = { submit(background = true) },
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(
                strings.repoCommandHint,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Text(
                strings.runnerBackgroundHint,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }
}

@Composable
private fun RunnerPanel(
    section: RunnerSection,
    onSectionChange: (RunnerSection) -> Unit,
    history: List<String>,
    favorites: List<String>,
    aliases: List<GitAlias>,
    aliasesLoading: Boolean,
    jobs: List<BackgroundCommand>,
    canRun: Boolean,
    onUseCommand: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearHistory: () -> Unit,
    onReloadAliases: () -> Unit,
    onUseAlias: (GitAlias) -> Unit,
    onRunAlias: (GitAlias) -> Unit,
    onClearFinishedJobs: () -> Unit,
    onDismissJob: (Long) -> Unit,
    onCancelJob: (Long) -> Unit,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.raised)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunnerTab(
                label = "${strings.runnerHistory} ${history.size}",
                active = section == RunnerSection.History,
                onClick = { onSectionChange(RunnerSection.History) },
            )
            Spacer(Modifier.width(6.dp))
            RunnerTab(
                label = "${strings.runnerFavorites} ${favorites.size}",
                active = section == RunnerSection.Favorites,
                onClick = { onSectionChange(RunnerSection.Favorites) },
            )
            Spacer(Modifier.width(6.dp))
            RunnerTab(
                label = "${strings.runnerAliases} ${aliases.size}",
                active = section == RunnerSection.Aliases,
                onClick = { onSectionChange(RunnerSection.Aliases) },
            )
            Spacer(Modifier.width(6.dp))
            RunnerTab(
                label = "${strings.runnerJobs} ${jobs.size}",
                active = section == RunnerSection.Jobs,
                onClick = { onSectionChange(RunnerSection.Jobs) },
            )
            Spacer(Modifier.width(10.dp))
            val hint = when (section) {
                RunnerSection.History -> strings.runnerHistoryHint
                RunnerSection.Favorites -> null
                RunnerSection.Aliases -> strings.repoAliasesHint
                RunnerSection.Jobs -> strings.runnerBackgroundHint
            }
            if (hint != null) {
                Text(
                    hint,
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            when (section) {
                RunnerSection.History -> if (history.isNotEmpty()) {
                    RunnerAction(strings.runnerClear, onClick = onClearHistory)
                }

                RunnerSection.Favorites -> Unit
                RunnerSection.Aliases -> RunnerAction(
                    strings.repoAliasesReload,
                    enabled = !aliasesLoading,
                    onClick = onReloadAliases,
                )

                RunnerSection.Jobs -> if (jobs.any { it.state != BackgroundCommandState.Running }) {
                    RunnerAction(strings.runnerClearFinished, onClick = onClearFinishedJobs)
                }
            }
        }
        when (section) {
            RunnerSection.History -> if (history.isEmpty()) {
                RunnerEmpty(strings.runnerNoHistory)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 110.dp)) {
                    items(history, key = { it }) { item ->
                        CommandHistoryRow(
                            command = item,
                            favorite = item in favorites,
                            onUse = { onUseCommand(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                        )
                    }
                }
            }

            RunnerSection.Favorites -> if (favorites.isEmpty()) {
                RunnerEmpty(strings.runnerNoFavorites)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 110.dp)) {
                    items(favorites, key = { it }) { item ->
                        CommandHistoryRow(
                            command = item,
                            favorite = true,
                            onUse = { onUseCommand(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                        )
                    }
                }
            }

            RunnerSection.Aliases -> when {
                aliasesLoading -> RunnerEmpty(strings.repoAliasesLoading)
                aliases.isEmpty() -> RunnerEmpty(strings.repoAliasesEmpty)
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 110.dp)) {
                    items(aliases, key = { it.name.lowercase() }) { alias ->
                        AliasRow(
                            alias = alias,
                            runLabel = strings.repoAliasesRun,
                            enabled = canRun,
                            onUse = { onUseAlias(alias) },
                            onRun = { onRunAlias(alias) },
                        )
                    }
                }
            }

            RunnerSection.Jobs -> if (jobs.isEmpty()) {
                RunnerEmpty(strings.runnerNoJobs)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 110.dp)) {
                    items(jobs, key = { it.id }) { job ->
                        BackgroundJobRow(
                            job = job,
                            onDismiss = { onDismissJob(job.id) },
                            onCancel = { onCancelJob(job.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunnerTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) Term.green else Term.faint,
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp),
    )
}

@Composable
private fun RunnerAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        "[$label]",
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = if (enabled) Term.greenDim else Term.faint,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun RunnerEmpty(text: String) {
    Box(
        Modifier.fillMaxWidth().height(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun CommandHistoryRow(
    command: String,
    favorite: Boolean,
    onUse: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(24.dp)
            .clickable(onClick = onUse)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (favorite) "★" else "☆",
            fontSize = 11.sp,
            fontFamily = Term.mono,
            color = if (favorite) Term.amber else Term.faint,
            modifier = Modifier
                .width(19.dp)
                .clickable(onClick = onToggleFavorite),
        )
        Text(
            command,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BackgroundJobRow(
    job: BackgroundCommand,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    val stateLabel = when (job.state) {
        BackgroundCommandState.Running -> if (job.cancelling) {
            strings.runnerCancelling
        } else {
            strings.runnerRunning
        }
        BackgroundCommandState.Succeeded -> strings.runnerSucceeded
        BackgroundCommandState.Failed -> strings.runnerFailed
        BackgroundCommandState.TimedOut -> strings.runnerTimedOut
        BackgroundCommandState.Cancelled -> strings.runnerCancelled
    }
    val color = when (job.state) {
        BackgroundCommandState.Running -> if (job.cancelling) Term.faint else Term.amber
        BackgroundCommandState.Succeeded -> Term.greenDim
        BackgroundCommandState.Failed, BackgroundCommandState.TimedOut -> Term.red
        BackgroundCommandState.Cancelled -> Term.amber
    }
    val details = buildList {
        add(stateLabel)
        job.exitCode?.let { add("exit $it") }
        job.durationMs?.let { add("$it ms") }
        job.progress?.let { progress ->
            val text = buildString {
                progress.stage?.let { append(it).append(' ') }
                progress.percent?.let { append(it).append('%') }
            }
            if (text.isNotBlank()) add(text)
        }
    }.joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                job.command,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                details,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = color,
                maxLines = 1,
            )
        }
        if (job.state == BackgroundCommandState.Running) {
            Text(
                if (job.cancelling) strings.runnerCancelling else "[${strings.runnerCancel}]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (job.cancelling) Term.faint else Term.amber,
                modifier = Modifier
                    .clickable(enabled = !job.cancelling, onClick = onCancel)
                    .padding(start = 6.dp),
            )
        } else {
            Text(
                "[×]",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.clickable(onClick = onDismiss).padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AliasRow(
    alias: GitAlias,
    runLabel: String,
    enabled: Boolean,
    onUse: () -> Unit,
    onRun: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .clickable(enabled = enabled, onClick = onUse)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("›", fontSize = 10.sp, fontFamily = Term.mono, color = Term.cyan)
        Spacer(Modifier.width(5.dp))
        Text(
            alias.name,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Term.text else Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            alias.command.replace("\n", " ⏎ "),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (alias.isShell) "${alias.scope.name.lowercase()} · shell" else alias.scope.name.lowercase(),
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "[$runLabel]",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Term.green else Term.faint,
            modifier = Modifier.clickable(enabled = enabled, onClick = onRun),
        )
    }
}

@Composable
private fun RepoStatusBar(controller: RepoController, onRefresh: () -> Unit) {
    val status = controller.status
    Row(
        Modifier.fillMaxWidth().height(21.dp).background(Term.green)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            " ${branchLabel(controller)} ",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.black,
        )
        Text(
            when {
                controller.repository.isBare -> " bare "
                status == null -> " loading "
                status.isClean -> " clean "
                else -> " dirty "
            },
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.black.copy(alpha = 0.8f),
        )
        Text(
            " ${status?.stagedCount ?: 0} staged  " +
                "${status?.unstagedCount ?: 0} unstaged  " +
                "${controller.commits.size}${if (controller.historyHasMore) "+" else ""} commits  " +
                "${controller.stashCount} stash ",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.black.copy(alpha = 0.8f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "git ${controller.gitVersion.major}.${controller.gitVersion.minor}.${controller.gitVersion.patch}",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.black.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (controller.busy) "working…" else "[r] refresh",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.black,
            modifier = Modifier.clickable(enabled = !controller.busy) { onRefresh() },
        )
    }
}

private fun recoveryRequest(entry: ReflogEntry, action: CommitAction): CommitActionRequest {
    val commit = Commit(
        hash = entry.hash,
        parents = emptyList(),
        authorName = "",
        authorEmail = "",
        authorTime = entry.time,
        committerName = "",
        committerEmail = "",
        committerTime = entry.time,
        subject = entry.subject,
    )
    return CommitActionRequest(
        action = action,
        commit = commit,
        initialResetMode = if (action == CommitAction.Reset) ResetMode.Hard else ResetMode.Mixed,
        initialRefName = if (action == CommitAction.Branch) reflogSuggestedBranch(entry) else "",
    )
}

private fun branchLabel(controller: RepoController): String {
    val branch = controller.status?.branch
    return branch?.head ?: when {
        controller.repository.isBare -> "(bare)"
        branch?.detached == true -> "(detached)"
        else -> "(no HEAD)"
    }
}

private fun statusCode(entry: StatusEntry, staged: Boolean): String {
    if (entry.kind == StatusKind.Untracked) return "?"
    if (entry.kind == StatusKind.Unmerged) return "!"
    if (entry.kind == StatusKind.Renamed) return "R"
    val code = if (staged) entry.indexStatus else entry.workTreeStatus
    return when (code) {
        'A' -> "A"
        'D' -> "D"
        'M' -> "M"
        'T' -> "T"
        'R' -> "R"
        'C' -> "C"
        'U' -> "!"
        '?' -> "?"
        else -> code.toString()
    }
}

private fun statusColor(entry: StatusEntry, staged: Boolean): Color = when {
    entry.kind == StatusKind.Unmerged -> Term.red
    entry.kind == StatusKind.Untracked -> Term.greenDim
    staged -> Term.amber
    else -> Term.cyan
}

private fun formatAge(epochSeconds: Long): String {
    val seconds = (System.currentTimeMillis() / 1000 - epochSeconds).coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3600}h"
        seconds < 604_800 -> "${seconds / 86_400}d"
        else -> "${seconds / 604_800}w"
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatDate(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
