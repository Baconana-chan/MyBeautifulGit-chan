package org.baconana.mybeautifulgit_chan.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.domain.AdvancedCommandResult
import org.baconana.mybeautifulgit_chan.domain.ArchiveFormat
import org.baconana.mybeautifulgit_chan.domain.BisectLogParser
import org.baconana.mybeautifulgit_chan.domain.BisectState
import org.baconana.mybeautifulgit_chan.domain.BlameResult
import org.baconana.mybeautifulgit_chan.domain.BranchComparison
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.CommitGraph
import org.baconana.mybeautifulgit_chan.domain.CommitInspection
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.DiffLineKind
import org.baconana.mybeautifulgit_chan.domain.FetchOptions
import org.baconana.mybeautifulgit_chan.domain.ParsedDiff
import org.baconana.mybeautifulgit_chan.domain.GitAlias
import org.baconana.mybeautifulgit_chan.domain.GitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.GitConfigValidationError
import org.baconana.mybeautifulgit_chan.domain.gitAliases
import org.baconana.mybeautifulgit_chan.domain.GitMetaFile
import org.baconana.mybeautifulgit_chan.domain.GitMetaFileContent
import org.baconana.mybeautifulgit_chan.domain.GitNoteEntry
import org.baconana.mybeautifulgit_chan.domain.LfsState
import org.baconana.mybeautifulgit_chan.domain.PartialCloneState
import org.baconana.mybeautifulgit_chan.domain.SparseCheckoutState
import org.baconana.mybeautifulgit_chan.domain.archiveArgs
import org.baconana.mybeautifulgit_chan.domain.bundleCreateArgs
import org.baconana.mybeautifulgit_chan.domain.bundleListHeadsArgs
import org.baconana.mybeautifulgit_chan.domain.bundleVerifyArgs
import org.baconana.mybeautifulgit_chan.domain.isObjectId
import org.baconana.mybeautifulgit_chan.domain.notesAddArgs
import org.baconana.mybeautifulgit_chan.domain.notesRemoveArgs
import org.baconana.mybeautifulgit_chan.domain.parseNotesList
import org.baconana.mybeautifulgit_chan.domain.parseSparsePatterns
import org.baconana.mybeautifulgit_chan.domain.requiresGitConfigScopeConfirmation
import org.baconana.mybeautifulgit_chan.domain.validAdvancedRef
import org.baconana.mybeautifulgit_chan.domain.validLfsPattern
import org.baconana.mybeautifulgit_chan.domain.validSparsePath
import org.baconana.mybeautifulgit_chan.domain.validateGitMeta
import org.baconana.mybeautifulgit_chan.domain.GitOperation
import org.baconana.mybeautifulgit_chan.domain.GitOperationKind
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.GrepResult
import org.baconana.mybeautifulgit_chan.domain.HookDetail
import org.baconana.mybeautifulgit_chan.domain.HookEntry
import org.baconana.mybeautifulgit_chan.domain.HookOutput
import org.baconana.mybeautifulgit_chan.domain.HooksState
import org.baconana.mybeautifulgit_chan.domain.HistoryWindow
import org.baconana.mybeautifulgit_chan.domain.IndexLock
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.OperationAction
import org.baconana.mybeautifulgit_chan.domain.PatchExportOptions
import org.baconana.mybeautifulgit_chan.domain.PatchExportResult
import org.baconana.mybeautifulgit_chan.domain.PatchMailbox
import org.baconana.mybeautifulgit_chan.domain.PatchMailboxParser
import org.baconana.mybeautifulgit_chan.domain.PushForce
import org.baconana.mybeautifulgit_chan.domain.PushMode
import org.baconana.mybeautifulgit_chan.domain.PushOptions
import org.baconana.mybeautifulgit_chan.domain.PullMode
import org.baconana.mybeautifulgit_chan.domain.RebaseCommand
import org.baconana.mybeautifulgit_chan.domain.RebasePlan
import org.baconana.mybeautifulgit_chan.domain.RebaseTodo
import org.baconana.mybeautifulgit_chan.domain.RebaseTodoEntry
import org.baconana.mybeautifulgit_chan.domain.RebaseTodoProblem
import org.baconana.mybeautifulgit_chan.domain.RefKind
import org.baconana.mybeautifulgit_chan.domain.ReflogEntry
import org.baconana.mybeautifulgit_chan.domain.RemoteProfile
import org.baconana.mybeautifulgit_chan.domain.gitInsteadOfRules
import org.baconana.mybeautifulgit_chan.domain.parseRemoteUrl
import org.baconana.mybeautifulgit_chan.domain.parseRemoteUrls
import org.baconana.mybeautifulgit_chan.domain.MergeConflict
import org.baconana.mybeautifulgit_chan.domain.MergeLineChoice
import org.baconana.mybeautifulgit_chan.domain.MergeParse
import org.baconana.mybeautifulgit_chan.domain.MergeSide
import org.baconana.mybeautifulgit_chan.domain.RerereDetail
import org.baconana.mybeautifulgit_chan.domain.RerereEntry
import org.baconana.mybeautifulgit_chan.domain.RerereState
import org.baconana.mybeautifulgit_chan.domain.RepoStatus
import org.baconana.mybeautifulgit_chan.domain.ResetPreview
import org.baconana.mybeautifulgit_chan.domain.RepositoryRuntime
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.StashCreateOptions
import org.baconana.mybeautifulgit_chan.domain.StashDetail
import org.baconana.mybeautifulgit_chan.domain.StashEntry
import org.baconana.mybeautifulgit_chan.domain.StatusEntry
import org.baconana.mybeautifulgit_chan.domain.SignatureCheck
import org.baconana.mybeautifulgit_chan.domain.SignatureCheckKind
import org.baconana.mybeautifulgit_chan.domain.SignatureTarget
import org.baconana.mybeautifulgit_chan.domain.SignatureVerification
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.SubmoduleUpdateOptions
import org.baconana.mybeautifulgit_chan.domain.submoduleAddArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleDeinitArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleInitArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleRemoveArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleSyncArgs
import org.baconana.mybeautifulgit_chan.domain.submoduleUpdateArgs
import org.baconana.mybeautifulgit_chan.domain.validSubmodulePath
import org.baconana.mybeautifulgit_chan.domain.validSubmoduleUrl
import org.baconana.mybeautifulgit_chan.domain.UnreachablePlan
import org.baconana.mybeautifulgit_chan.domain.Worktree
import org.baconana.mybeautifulgit_chan.domain.WorktreeAddOptions
import org.baconana.mybeautifulgit_chan.domain.amArgs
import org.baconana.mybeautifulgit_chan.domain.applyPatchArgs
import org.baconana.mybeautifulgit_chan.domain.bisectMarkArgs
import org.baconana.mybeautifulgit_chan.domain.bisectResetArgs
import org.baconana.mybeautifulgit_chan.domain.bisectSkipArgs
import org.baconana.mybeautifulgit_chan.domain.bisectStartArgs
import org.baconana.mybeautifulgit_chan.domain.formatPatchArgs
import org.baconana.mybeautifulgit_chan.domain.validBisectRef
import org.baconana.mybeautifulgit_chan.domain.validGitConfigKey
import org.baconana.mybeautifulgit_chan.domain.validateGitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.validHookName
import org.baconana.mybeautifulgit_chan.domain.validPatchPath
import org.baconana.mybeautifulgit_chan.domain.validWorktreePath
import org.baconana.mybeautifulgit_chan.domain.worktreeAddArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeLockArgs
import org.baconana.mybeautifulgit_chan.domain.worktreePruneArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeRemoveArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeSwitchArgs
import org.baconana.mybeautifulgit_chan.domain.worktreeUnlockArgs
import org.baconana.mybeautifulgit_chan.domain.fetchArgs
import org.baconana.mybeautifulgit_chan.domain.pushArgs
import org.baconana.mybeautifulgit_chan.domain.operationArgs
import org.baconana.mybeautifulgit_chan.domain.buildCommitGraph
import org.baconana.mybeautifulgit_chan.domain.buildPartialPatch
import org.baconana.mybeautifulgit_chan.domain.applyConflictChoices
import org.baconana.mybeautifulgit_chan.domain.applyConflictSide
import org.baconana.mybeautifulgit_chan.domain.branchDeleteArgs
import org.baconana.mybeautifulgit_chan.domain.branchRenameArgs
import org.baconana.mybeautifulgit_chan.domain.branchUpstreamArgs
import org.baconana.mybeautifulgit_chan.domain.parsePatch
import org.baconana.mybeautifulgit_chan.domain.parseConflictMarkers
import org.baconana.mybeautifulgit_chan.domain.mergeHistoryWindow
import org.baconana.mybeautifulgit_chan.domain.pullArgs
import org.baconana.mybeautifulgit_chan.domain.tagDeleteArgs
import org.baconana.mybeautifulgit_chan.domain.validFetchDepth
import org.baconana.mybeautifulgit_chan.domain.validFetchFilter
import org.baconana.mybeautifulgit_chan.domain.validRemoteName
import org.baconana.mybeautifulgit_chan.domain.validRemoteUrl
import org.baconana.mybeautifulgit_chan.domain.validPushRefspec
import org.baconana.mybeautifulgit_chan.git.GitBackend
import org.baconana.mybeautifulgit_chan.git.GitRunControl
import org.baconana.mybeautifulgit_chan.git.GitBlob
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.git.porcelain.SignatureVerificationParser
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.git.redactSecrets
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import org.baconana.mybeautifulgit_chan.i18n.Strings
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

enum class LogKind { Command, Output, Error, Success, Info }

data class LogLine(val text: String, val kind: LogKind)

enum class BackgroundCommandState { Running, Succeeded, Failed, TimedOut, Cancelled }

/** Задача raw-runner, которая не держит operationMutex и не блокирует UI. */
data class BackgroundCommand(
    val id: Long,
    val command: String,
    val state: BackgroundCommandState,
    val exitCode: Int? = null,
    val durationMs: Long? = null,
    val cancelling: Boolean = false,
    val progress: OperationProgress? = null,
)

enum class OperationOutcome { Running, Succeeded, Failed, Cancelled, TimedOut }

/** Долгая git-операция: от старта до краткого уведомления о завершении. */
data class OperationActivity(
    val id: Long,
    val command: String,
    val startedAtMs: Long,
    val outcome: OperationOutcome = OperationOutcome.Running,
    val cancelling: Boolean = false,
    val progress: OperationProgress? = null,
    val durationMs: Long? = null,
    val completedAtMs: Long? = null,
) {
    val running: Boolean get() = outcome == OperationOutcome.Running
}

private data class OperationRun(val id: Long, val control: GitRunControl)

/** Локальный ref, сохраняющий tip до переписывания истории. */
data class RewriteBackup(
    val ref: String,
    val previousTip: String,
    val branch: String?,
    val operation: String,
)

/** Backup и ожидаемый HEAD после операции; Undo разрешён только пока HEAD не менялся. */
data class RewriteUndo(
    val backup: RewriteBackup,
    val postTip: String,
)

data class OperationToast(
    val id: Long,
    val message: String,
    val command: String,
    val success: Boolean,
    val undo: RewriteUndo? = null,
)

/** Состояние записи журнала приложения; Undone можно повторить, пока нет новой операции. */
enum class JournalEntryState { Applied, Undone }

/**
 * Обратимая часть операции. Rewrite уже умеет откатываться на backup-ref;
 * commit и checkout откатываются точным reset/checkout, не трогая чужие ветки.
 */
sealed interface JournalAction {
    data class Rewrite(val undo: RewriteUndo) : JournalAction

    data class CommitReset(
        val beforeTip: String,
        val afterTip: String,
        val branch: String?,
    ) : JournalAction

    data class Checkout(
        val beforeRef: String,
        val beforeTip: String,
        val afterRef: String,
        val afterTip: String,
    ) : JournalAction
}

/** Одна запись единого журнала: что, когда, чем закончилось и чем отменить. */
data class JournalEntry(
    val id: Long,
    val timestampMs: Long,
    val message: String,
    val command: String,
    val success: Boolean,
    val action: JournalAction? = null,
    val state: JournalEntryState = JournalEntryState.Applied,
)

enum class DiscardScope { File, Hunk, Lines }

/** Что именно выбросить из рабочего дерева. Index при этом не меняется. */
data class DiscardTarget(
    val entry: StatusEntry,
    val scope: DiscardScope,
    val hunkIndex: Int = -1,
    val lineIndexes: Set<Int> = emptySet(),
)

enum class DiscardBackup { None, Patch, Stash }

enum class CommitMode { Normal, Fixup, Squash }

enum class ResetMode(val flag: String) {
    Soft("--soft"),
    Mixed("--mixed"),
    Hard("--hard"),
    Keep("--keep"),
}

enum class RewriteMode { Reword, Amend, Split }

/**
 * Состояние многошагового rewrite: ветка уже стоит на parent выбранного
 * коммита, а descendants ждут повторного применения после того, как user
 * создаст замену.
 */
data class PendingRewrite(
    val targetHash: String,
    val targetSubject: String,
    val originalTip: String,
    val parentHash: String,
    val branch: String,
    val backupRef: String,
    val mode: RewriteMode,
)

/**
 * Разрешённый, но ещё не завершённый `--no-commit` range. Git не оставляет
 * CHERRY_PICK_HEAD для `-n`, поэтому продолжение ведёт контроллер: после
 * stage конфликтного файла применяется следующий коммит из remaining.
 */
data class PendingNoCommit(
    val kind: GitOperationKind,
    val range: String,
    val remaining: List<String>,
    val total: Int,
    val originalTip: String,
)

/** Одна сторона binary/image diff: размер и, для картинки, байты. */
data class DiffVersion(
    val label: String,
    val size: Long?,
    val missing: Boolean = false,
    val bytes: ByteArray? = null,
)

/** Данные для случая, когда обычный текстовый diff неприменим. */
data class DiffFileInfo(
    val path: String,
    val old: DiffVersion,
    val new: DiffVersion,
    val binary: Boolean,
    val image: Boolean,
    val tooLarge: Boolean = false,
    val limitBytes: Long = 0,
)

/** Версии конфликтного файла и исходный текст, открытый в merge editor. */
data class MergeEditorState(
    val path: String,
    val base: String?,
    val ours: String?,
    val theirs: String?,
    val original: String,
    val binary: Boolean = false,
    val tooLarge: Boolean = false,
    val error: String? = null,
)

private data class LoadedDiff(
    val text: String,
    val fileInfo: DiffFileInfo? = null,
)

private data class RerereFile(
    val text: String,
    val bytes: Long,
    val truncated: Boolean,
)

/** Файлы, через которые git получает todo и сообщения reword. */
private data class RebaseWorkspace(
    val directory: File,
    val todo: File,
    val sequenceEditor: File,
    val messageEditor: File,
    val messages: File,
)

private const val MAX_RERERE_ENTRIES = 200
private const val MAX_RERERE_BYTES = 256 * 1024
private const val MAX_MAILBOX_BYTES = 2 * 1024 * 1024
private const val MAX_SAFE_TEXT_FILE_BYTES = 2L * 1024 * 1024
private const val MAX_FILTER_REPO_PREVIEW_BYTES = 16L * 1024 * 1024
private const val MAX_FILTER_REPO_PREVIEW_LINES = 200_000
private const val MAX_PATCH_EXPORT_BYTES = 16L * 1024 * 1024
private const val MAX_BISECT_LOG_BYTES = 256 * 1024
private const val MAX_BISECT_OUTPUT_CHARS = 128 * 1024
private const val BISECT_COMMAND_TIMEOUT_SECONDS = 600L
private const val MAX_BISECT_STEPS = 10_000
private const val MAX_HOOK_BYTES = 512 * 1024
private const val MAX_HOOK_OUTPUT_CHARS = 64 * 1024
private const val MAX_ADVANCED_OUTPUT_CHARS = 64 * 1024
private const val MAX_ADVANCED_NOTE_CHARS = 8_000
private const val HISTORY_PAGE_SIZE = 200
private const val MAX_INSPECTION_CACHE_ENTRIES = 8
private const val MAX_INSPECTION_CACHE_BYTES = 24L * 1024 * 1024

private data class RefreshSnapshot(
    val status: RepoStatus?,
    val commits: List<Commit>,
    val commitsHasMore: Boolean,
    val historyReused: Boolean,
    val refs: List<GitRef>,
    val remotes: List<String>,
    val remoteUrls: Map<String, String>,
    val worktrees: List<Worktree>,
    val submodules: List<Submodule>,
    val stashCount: Int,
    val stashes: List<StashEntry>,
    val rerere: RerereState,
    val bisect: BisectState,
    val hooks: HooksState,
)

/**
 * Состояние открытого репозитория и операции первого вертикального среза:
 * status → diff → stage/unstage → commit.
 *
 * Все git-вызовы уходят в Dispatchers.IO; Compose state обновляется уже на main.
 */
class RepoController(
    val repository: Repository,
    private val backend: GitBackend,
    val gitVersion: GitVersion,
) {
    /** Обновляется из UI при смене языка; контроллер при этом не пересоздаётся. */
    var strings: Strings = EnglishStrings

    var status by mutableStateOf<RepoStatus?>(null)
        private set
    var commits by mutableStateOf<List<Commit>>(emptyList())
        private set
    /** В загруженном окне есть ещё коммиты; следующая страница читается по требованию. */
    var historyHasMore by mutableStateOf(false)
        private set
    var historyLoadingMore by mutableStateOf(false)
        private set
    private var historyCache: List<Commit> = emptyList()
    private var historyCacheHasMore = false
    private var historyCacheFilter: LogFilter? = null
    var logFilter by mutableStateOf(LogFilter())
        private set
    /** Merge-коммиты, боковые ветки которых свёрнуты в graph-е. */
    var collapsedMerges by mutableStateOf<Set<String>>(emptySet())
        private set
    private var currentUserEmail: String? = null
    var refs by mutableStateOf<List<GitRef>>(emptyList())
        private set
    /** Настроенные remotes, нужны для push/upstream даже до первого fetch. */
    var remotes by mutableStateOf<List<String>>(emptyList())
        private set
    /** URL для fetch по имени remote; обновляется вместе с [remotes]. */
    var remoteUrls by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var branchComparison by mutableStateOf<BranchComparison?>(null)
        private set
    var branchCompareLoading by mutableStateOf(false)
        private set
    var branchCompareError by mutableStateOf<String?>(null)
        private set
    private var branchCompareRequest = 0L
    var worktrees by mutableStateOf<List<Worktree>>(emptyList())
        private set
    var submodules by mutableStateOf<List<Submodule>>(emptyList())
        private set
    var stashCount by mutableStateOf(0)
        private set
    var stashes by mutableStateOf<List<StashEntry>>(emptyList())
        private set
    var stashDetail by mutableStateOf<StashDetail?>(null)
        private set
    var stashDetailLoading by mutableStateOf(false)
        private set
    var stashDetailError by mutableStateOf<String?>(null)
        private set
    var patchMailbox by mutableStateOf<PatchMailbox?>(null)
        private set
    var patchMailboxLoading by mutableStateOf(false)
        private set
    var patchMailboxError by mutableStateOf<String?>(null)
        private set
    private var stashDetailRequest = 0
    var rerere by mutableStateOf<RerereState?>(null)
        private set
    var rerereDetail by mutableStateOf<RerereDetail?>(null)
        private set
    var operation by mutableStateOf<GitOperation?>(null)
        private set
    var bisect by mutableStateOf(BisectState())
        private set
    var bisectAutoRunning by mutableStateOf(false)
        private set
    var bisectAutoLog by mutableStateOf("")
        private set
    var bisectAutoResult by mutableStateOf<String?>(null)
        private set
    private var bisectAutoCancelled = false
    private var bisectRunner: BisectCommandRunner? = null
    var hooks by mutableStateOf(HooksState())
        private set
    var hookDetail by mutableStateOf<HookDetail?>(null)
        private set
    var hookDetailLoading by mutableStateOf(false)
        private set
    var hookDetailError by mutableStateOf<String?>(null)
        private set
    var hookOutput by mutableStateOf<HookOutput?>(null)
        private set
    private var hookDetailRequest = 0L
    var pendingRewrite by mutableStateOf<PendingRewrite?>(null)
        private set
    var pendingNoCommit by mutableStateOf<PendingNoCommit?>(null)
        private set
    var indexLock by mutableStateOf<IndexLock?>(null)
        private set
    /** Backup текущего интерактивного rebase; Undo создаётся после его завершения. */
    private var pendingRebaseBackup: RewriteBackup? = null

    var selectedPath by mutableStateOf<String?>(null)
        private set
    var selectedStaged by mutableStateOf(false)
        private set
    private var selectionRequest = 0L
    private val selectionMutex = Mutex()
    private val operationMutex = Mutex()
    var diff by mutableStateOf("")
        private set
    var diffFileInfo by mutableStateOf<DiffFileInfo?>(null)
        private set
    var commitInspection by mutableStateOf<CommitInspection?>(null)
        private set
    var commitInspectionLoading by mutableStateOf(false)
        private set
    var commitInspectionError by mutableStateOf<String?>(null)
        private set
    private var commitInspectionRequest = 0L
    private val inspectionCache = BoundedCache<String, CommitInspection>(
        maxEntries = MAX_INSPECTION_CACHE_ENTRIES,
        maxWeight = MAX_INSPECTION_CACHE_BYTES,
        weigher = { _, inspection ->
            inspection.diff.length.toLong() * 2L + 2_048L
        },
    )
    /** Замеры чтения/графа/кэша; сбрасываются из экрана профиля. */
    val profiler = PerformanceProfiler()
    private var graphCache: CommitGraph? = null
    private var graphCacheCommits: List<Commit>? = null
    private var graphCacheCollapsed: Set<String>? = null
    var signatureVerification by mutableStateOf<SignatureVerification?>(null)
        private set
    var signatureVerificationLoading by mutableStateOf(false)
        private set
    private var signatureVerificationRequest = 0L

    var advancedResult by mutableStateOf<AdvancedCommandResult?>(null)
        private set
    var advancedLoading by mutableStateOf(false)
        private set
    var prunePreview by mutableStateOf<String?>(null)
        private set
    var sparseState by mutableStateOf(SparseCheckoutState())
        private set
    var lfsState by mutableStateOf(LfsState())
        private set
    var lfsLoading by mutableStateOf(false)
        private set
    var gitNotes by mutableStateOf<List<GitNoteEntry>>(emptyList())
        private set
    var gitNoteText by mutableStateOf("")
        private set
    var notesLoading by mutableStateOf(false)
        private set
    var partialCloneState by mutableStateOf(PartialCloneState())
        private set
    var fsmonitorConfig by mutableStateOf("")
        private set
    var fileHistoryPath by mutableStateOf<String?>(null)
        private set
    var fileHistoryCommits by mutableStateOf<List<Commit>>(emptyList())
        private set
    var fileHistoryLoading by mutableStateOf(false)
        private set
    var fileHistoryError by mutableStateOf<String?>(null)
        private set
    private var fileHistoryRequest = 0L
    var blameResult by mutableStateOf<BlameResult?>(null)
        private set
    var blameLoading by mutableStateOf(false)
        private set
    var blameError by mutableStateOf<String?>(null)
        private set
    private var blameRequest = 0L
    var grepResult by mutableStateOf<GrepResult?>(null)
        private set
    var grepLoading by mutableStateOf(false)
        private set
    var grepError by mutableStateOf<String?>(null)
        private set
    private var grepRequest = 0L
    var reflogHash by mutableStateOf<String?>(null)
        private set
    var reflogEntries by mutableStateOf<List<ReflogEntry>>(emptyList())
        private set
    var reflogLoading by mutableStateOf(false)
        private set
    var reflogError by mutableStateOf<String?>(null)
        private set
    private var reflogRequest = 0L
    var mergeEditor by mutableStateOf<MergeEditorState?>(null)
        private set
    var mergeDraft by mutableStateOf("")
        private set
    var mergeSelections by mutableStateOf<Map<Int, MergeLineChoice>>(emptyMap())
        private set
    var mergeParse by mutableStateOf(MergeParse(emptyList(), false))
        private set
    private var mergeSavedText by mutableStateOf("")

    val mergeConflicts: List<MergeConflict>
        get() = mergeParse.conflicts

    val mergeDirty: Boolean
        get() = mergeEditor != null && mergeDraft != mergeSavedText
    /** Целые хунки, выбранные для частичного коммита; индексы относятся к [diff]. */
    var selectedHunks by mutableStateOf<Set<Int>>(emptySet())
        private set

    var commitMessage by mutableStateOf("")
    var amend by mutableStateOf(false)
    var sign by mutableStateOf(false)
    var commitMode by mutableStateOf(CommitMode.Normal)
    var fixupTarget by mutableStateOf("")
    var noVerify by mutableStateOf(false)
    var commitAuthor by mutableStateOf("")
    var commitCommitter by mutableStateOf("")
    var commitTemplatePath by mutableStateOf("")

    // Nested git calls are common: refresh() is called from actions that already
    // hold busy. A depth counter prevents an inner finally from clearing the
    // flag while the outer operation is still running.
    private var busyDepth by mutableStateOf(0)
    var busy: Boolean
        get() = busyDepth > 0
        private set(value) {
            busyDepth = if (value) busyDepth + 1 else (busyDepth - 1).coerceAtLeast(0)
        }
    var error by mutableStateOf<String?>(null)
        private set

    /** Точная команда последнего неудачного commit; нужна для явного retry. */
    private var retryCommitArgs by mutableStateOf<List<String>?>(null)

    val retryCommitAvailable: Boolean
        get() = retryCommitArgs != null && !busy

    val retryCommitCommand: String?
        get() = retryCommitArgs?.let { args ->
            (listOf("git") + args).joinToString(" ") { quoteArg(it) }
        }

    val log = mutableStateListOf<LogLine>()

    var backgroundCommands by mutableStateOf<List<BackgroundCommand>>(emptyList())
        private set
    private var nextBackgroundCommandId = 0L
    private val backgroundRunControls = ConcurrentHashMap<Long, GitRunControl>()
    private val backgroundProgressBuffers = mutableMapOf<Long, StringBuilder>()

    /** Активная долгая операция и короткое уведомление после её завершения. */
    var operationActivity by mutableStateOf<OperationActivity?>(null)
        private set
    private var nextOperationActivityId = 0L
    @Volatile
    private var activeOperationControl: GitRunControl? = null
    private val operationProgressBuffer = StringBuilder()
    @Volatile
    private var lastOperationProgressAt = 0L

    /** Прерывает текущий долгий git-процесс; backend завершит дерево процессов. */
    fun cancelOperationActivity() {
        val current = operationActivity ?: return
        if (!current.running || current.cancelling) return
        operationActivity = current.copy(cancelling = true)
        activeOperationControl?.cancel()
    }

    fun dismissOperationActivity(id: Long) {
        val current = operationActivity ?: return
        if (current.id == id && !current.running) operationActivity = null
    }

    var toast by mutableStateOf<OperationToast?>(null)
        private set
    private var nextToastId = 0L

    /** Последняя доступная отмена переписывания; живёт в toast. */
    var lastRewriteUndo by mutableStateOf<RewriteUndo?>(null)
        private set
    private var backupSequence = 0L

    /** Единый журнал операций; новые записи идут в конец. */
    val journal = mutableStateListOf<JournalEntry>()
    private val journalUndoStack = mutableStateListOf<Long>()
    private val journalRedoStack = mutableStateListOf<Long>()
    private var nextJournalId = 0L

    val lastJournalEntry: JournalEntry?
        get() = journal.lastOrNull()
    val journalUndoDepth: Int
        get() = journalUndoStack.size
    val journalRedoDepth: Int
        get() = journalRedoStack.size

    val canUndoJournal: Boolean
        get() = !busy &&
            operation == null &&
            pendingRewrite == null &&
            pendingNoCommit == null &&
            journalUndoStack.isNotEmpty()

    val canRedoJournal: Boolean
        get() = !busy &&
            operation == null &&
            pendingRewrite == null &&
            pendingNoCommit == null &&
            journalRedoStack.isNotEmpty()

    val canCommit: Boolean
        get() = when (commitMode) {
            CommitMode.Normal -> amend || (status?.stagedCount ?: 0) > 0
            CommitMode.Fixup, CommitMode.Squash ->
                !amend && (status?.stagedCount ?: 0) > 0 && fixupTarget.isNotBlank()
        }

    val commitOptionsActive: Boolean
        get() = commitMode != CommitMode.Normal ||
            noVerify ||
            commitAuthor.isNotBlank() ||
            commitCommitter.isNotBlank() ||
            commitTemplatePath.isNotBlank()

    /** Частичный discard безопасен только для обычного изменённого файла. */
    val canDiscardSelectedPartially: Boolean
        get() {
            val entry = status?.entries?.firstOrNull { it.path == selectedPath } ?: return false
            return !selectedStaged &&
                entry.kind == StatusKind.Changed &&
                !entry.isConflicted &&
                !entry.isSubmodule &&
                entry.isUnstaged
        }

    val visibleEntries: List<StatusEntry>
        get() = status?.entries?.filter { it.kind != StatusKind.Ignored }.orEmpty()

    val ignoredEntries: List<StatusEntry>
        get() = status?.entries?.filter { it.kind == StatusKind.Ignored }.orEmpty()

    /**
     * Граф строится только для загруженного окна истории и кэшируется, пока
     * не изменился список коммитов или набор свёрнутых merge.
     */
    fun commitGraph(): CommitGraph {
        val currentCommits = commits
        val currentCollapsed = collapsedMerges
        val cached = graphCache
        if (cached != null &&
            graphCacheCommits === currentCommits &&
            graphCacheCollapsed == currentCollapsed
        ) {
            profiler.increment("graph.cache.hit")
            return cached
        }
        profiler.increment("graph.cache.miss")
        val graph = profiler.measure("graph.build") {
            buildCommitGraph(currentCommits, currentCollapsed)
        }
        graphCache = graph
        graphCacheCommits = currentCommits
        graphCacheCollapsed = currentCollapsed
        return graph
    }

    fun performanceReport(): PerformanceReport {
        val graph = graphCache.takeIf { graphCacheCommits === commits }
        return PerformanceReport(
            spans = profiler.snapshot(),
            counters = profiler.counters(),
            historyLoaded = commits.size,
            historyHasMore = historyHasMore,
            graphRows = graph?.rows?.size ?: 0,
            graphLanes = graph?.laneCount ?: 0,
            inspectionCacheEntries = inspectionCache.size,
            inspectionCacheBytes = inspectionCache.weight,
        )
    }

    fun resetPerformanceProfile() {
        profiler.reset()
    }

    /**
     * Лениво читает полный diff, numstat и подпись только для выбранного коммита.
     * Отдельный request-токен не даёт позднему ответу перетереть более новый выбор.
     */
    suspend fun loadCommitInspection(hash: String) {
        require(hash.isNotBlank()) { "hash must not be blank" }
        if (commitInspection?.hash != hash) clearSignatureVerification()
        if (commitInspection?.hash == hash && commitInspectionError == null) {
            // Выбор мог вернуться к уже загруженному коммиту, пока другой ещё читался.
            // Инвалидируем чужой запрос, иначе поздний ответ перетрёт состояние.
            commitInspectionRequest++
            commitInspectionLoading = false
            return
        }

        inspectionCache[hash]?.let { cached ->
            commitInspectionRequest++
            commitInspectionLoading = false
            commitInspectionError = null
            commitInspection = cached
            profiler.increment("cache.inspection.hit")
            return
        }
        profiler.increment("cache.inspection.miss")

        val request = ++commitInspectionRequest
        commitInspectionLoading = true
        commitInspectionError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                backend.commitInspection(repository, hash)
            }
            inspectionCache.put(hash, loaded)
            if (request == commitInspectionRequest) commitInspection = loaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == commitInspectionRequest) {
                commitInspectionError = displayError(error)
            }
        } finally {
            if (request == commitInspectionRequest) commitInspectionLoading = false
        }
    }

    /**
     * Явно запускает `git verify-commit` и показывает полный вывод gpg/ssh.
     * Для merge дополнительно проверяет встроенный mergetag, если он есть.
     */
    suspend fun verifyCommitSignature(hash: String, merge: Boolean = false): Boolean {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        val targetKind = if (merge) SignatureTarget.Merge else SignatureTarget.Commit
        return runSignatureVerification(hash, targetKind) {
            val checks = mutableListOf<SignatureCheck>()
            val commitResult = runSignatureCommand(listOf("verify-commit", hash))
            logVerification(commitResult)
            checks += signatureCheck(
                kind = if (merge) SignatureCheckKind.MergeCommit else SignatureCheckKind.Commit,
                result = commitResult,
            )

            if (merge) {
                val commitObject = runSignatureCommand(listOf("cat-file", "-p", hash))
                if (commitObject.ok && hasEmbeddedMergetag(commitObject.stdout)) {
                    val embedded = runSignatureCommand(
                        listOf("show", "--no-patch", "--show-signature", "--format=", hash),
                    )
                    logVerification(embedded)
                    val raw = signatureRawOutput(embedded)
                    val status = SignatureVerificationParser.parseEmbedded(raw)
                    checks += SignatureCheck(
                        kind = SignatureCheckKind.MergeTag,
                        command = displayCommand(embedded),
                        status = status,
                        output = visibleSignatureOutput(raw),
                        ok = status == CommitSignatureStatus.Good ||
                            status == CommitSignatureStatus.Untrusted,
                    )
                }
            }
            checks
        }
    }

    /** Проверяет подписанный tag через `git verify-tag`; lightweight tag даёт NoSignature. */
    suspend fun verifyTagSignature(name: String): Boolean {
        val tag = localTag(name) ?: run {
            error = strings.tagNotFound
            return false
        }
        if (!tag.name.startsWith("refs/tags/")) {
            error = strings.tagNotFound
            return false
        }
        return runSignatureVerification(tag.name, SignatureTarget.Tag) {
            val result = runSignatureCommand(listOf("verify-tag", tag.name))
            logVerification(result)
            listOf(signatureCheck(SignatureCheckKind.Tag, result))
        }
    }

    fun clearSignatureVerification() {
        signatureVerificationRequest++
        signatureVerification = null
        signatureVerificationLoading = false
    }

    private suspend fun runSignatureVerification(
        target: String,
        targetKind: SignatureTarget,
        load: suspend () -> List<SignatureCheck>,
    ): Boolean {
        val request = ++signatureVerificationRequest
        signatureVerification = null
        signatureVerificationLoading = true
        error = null
        return try {
            val checks = load()
            if (request != signatureVerificationRequest) return false
            val report = SignatureVerification(target, targetKind, checks)
            signatureVerification = report
            publishToast(
                message = if (report.verified) {
                    strings.toastSignatureVerified
                } else {
                    strings.toastSignatureFailed
                },
                command = checks.joinToString(" · ") { it.command },
                success = report.verified,
            )
            report.verified
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (request == signatureVerificationRequest) {
                signatureVerification = null
                error = displayError(failure)
            }
            false
        } finally {
            if (request == signatureVerificationRequest) {
                signatureVerificationLoading = false
            }
        }
    }

    private suspend fun runSignatureCommand(args: List<String>): GitResult =
        withContext(Dispatchers.IO) {
            backend.run(repository.root, args, timeoutSeconds = 60)
        }

    private fun signatureCheck(kind: SignatureCheckKind, result: GitResult): SignatureCheck {
        val raw = signatureRawOutput(result)
        return SignatureCheck(
            kind = kind,
            command = displayCommand(result),
            status = SignatureVerificationParser.parse(raw, result.ok),
            output = visibleSignatureOutput(raw),
            ok = result.ok,
        )
    }

    private fun signatureRawOutput(result: GitResult): String = buildString {
        if (result.stdout.isNotBlank()) append(result.stdout.trimEnd())
        if (result.stderr.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(result.stderr.trimEnd())
        }
    }

    private fun visibleSignatureOutput(raw: String): String {
        val visible = redactDisplayText(raw)
        return if (visible.length > MAX_SIGNATURE_OUTPUT_CHARS) {
            visible.take(MAX_SIGNATURE_OUTPUT_CHARS) + "\n…"
        } else {
            visible
        }
    }

    private fun hasEmbeddedMergetag(commitObject: String): Boolean =
        commitObject.lineSequence().any { it.startsWith("mergetag ") }

    private fun logVerification(result: GitResult) {
        addLog("$ ${displayCommand(result)}", LogKind.Command)
        result.stdout.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), LogKind.Output) }
        result.stderr.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), if (result.ok) LogKind.Output else LogKind.Error) }
        addLog(
            "[exit ${result.exitCode} · ${result.durationMs} ms]",
            when {
                result.cancelled -> LogKind.Info
                result.ok -> LogKind.Success
                else -> LogKind.Error
            },
        )
        if (result.cancelled) addLog(strings.operationCancelled, LogKind.Info)
        if (result.outputTruncated) addLog(strings.gitOutputTruncated, LogKind.Info)
    }

    /** Загружает `git log --follow` для пути; запоздалый ответ не перетирает новый запрос. */
    suspend fun loadFileHistory(path: String) {
        val clean = searchPath(path)
        if (clean == null) {
            fileHistoryPath = null
            fileHistoryCommits = emptyList()
            fileHistoryLoading = false
            fileHistoryError = strings.searchPathRequired
            return
        }
        val request = ++fileHistoryRequest
        fileHistoryPath = clean
        fileHistoryCommits = emptyList()
        fileHistoryLoading = true
        fileHistoryError = null
        try {
            val commits = withContext(Dispatchers.IO) {
                backend.fileHistory(repository, clean, maxCount = 200)
            }
            if (request == fileHistoryRequest) fileHistoryCommits = commits
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == fileHistoryRequest) {
                fileHistoryError = displayError(error)
            }
        } finally {
            if (request == fileHistoryRequest) fileHistoryLoading = false
        }
    }

    /** Загружает blame версии файла из HEAD с ограничением 5000 строк. */
    suspend fun loadBlame(path: String) {
        val clean = searchPath(path)
        if (clean == null) {
            blameResult = null
            blameLoading = false
            blameError = strings.searchPathRequired
            return
        }
        val request = ++blameRequest
        blameResult = null
        blameLoading = true
        blameError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                backend.blame(repository, clean, maxLines = 5000)
            }
            if (request == blameRequest) blameResult = loaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == blameRequest) {
                blameError = displayError(error)
            }
        } finally {
            if (request == blameRequest) blameLoading = false
        }
    }

    /** Ищет фиксированную строку в отслеживаемых файлах; путь — необязательный фильтр. */
    suspend fun runGrep(query: String, path: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            grepResult = null
            grepLoading = false
            grepError = strings.searchQueryRequired
            return
        }
        val cleanPath = searchPath(path).orEmpty()
        val request = ++grepRequest
        grepResult = null
        grepLoading = true
        grepError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                backend.grep(repository, cleanQuery, path = cleanPath, maxMatches = 500)
            }
            if (request == grepRequest) grepResult = loaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == grepRequest) {
                grepError = displayError(error)
            }
        } finally {
            if (request == grepRequest) grepLoading = false
        }
    }

    /** Полный Commit для перехода из истории/blame, даже если его нет в текущем graph. */
    suspend fun commitForNavigation(hash: String): Commit? {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return null
        }
        busy = true
        error = null
        return try {
            val commit = withContext(Dispatchers.IO) {
                backend.log(repository, maxCount = 1, rev = hash, filter = LogFilter(onlyCurrent = true))
                    .firstOrNull()
            }
            if (commit == null) error = strings.searchCommitNotFound
            commit
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        } finally {
            busy = false
        }
    }

    private fun searchPath(path: String): String? {
        val clean = path.trim()
        if (clean.isEmpty() || clean.any { it == '\u0000' }) return null
        return clean
    }

    private fun pathError(error: Exception): String =
        if (error is RepositoryPathException) strings.repoPathUnsafe else displayError(error)

    suspend fun checkoutCommit(hash: String): Boolean {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        val target = resolveCommitHash(hash) ?: return false
        val beforeTip = rewriteHead() ?: return false
        val beforeRef = currentBranchOrNull() ?: beforeTip
        return runCommitAction(
            args = listOf("checkout", target),
            message = strings.toastCommitCheckout,
            journalAction = JournalAction.Checkout(
                beforeRef = beforeRef,
                beforeTip = beforeTip,
                afterRef = target,
                afterTip = target,
            ),
        )
    }

    suspend fun resetCommit(hash: String, mode: ResetMode): Boolean {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        val target = resolveCommitHash(hash) ?: return false
        val tip = rewriteHead() ?: return false
        val backup = createRewriteBackup(
            operation = "reset-${mode.name.lowercase()}",
            tip = tip,
            branch = currentBranchOrNull(),
        ) ?: return false
        return runCommitAction(
            args = listOf("reset", mode.flag, target),
            message = strings.toastCommitReset,
            undo = RewriteUndo(backup, target),
        )
    }

    /**
     * Удаляет выбранный коммит вместе со всем хвостом через reset --hard на
     * его родителя. Это не drop в rebase: потомки не переприменяются.
     */
    suspend fun deleteCommitByReset(commit: Commit): Boolean {
        if (operation != null || pendingRewrite != null || pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        val parent = commit.parents.firstOrNull() ?: run {
            error = strings.deleteCommitRootNoParent
            return false
        }
        val tip = rewriteHead() ?: return false
        val ancestor = rewriteRead(listOf("merge-base", "--is-ancestor", commit.hash, tip))
            ?: return false
        if (!ancestor.ok) {
            error = strings.deleteCommitNotInHistory
            return false
        }
        return resetCommit(parent, ResetMode.Hard)
    }

    /** Проверяет, установлен ли git-filter-repo (он не входит в git). */
    suspend fun filterRepoAvailable(): Boolean = try {
        val result = withContext(Dispatchers.IO) {
            backend.run(
                repository.root,
                listOf("filter-repo", "--version"),
                timeoutSeconds = 30,
            )
        }
        logCheck(result)
        result.ok
    } catch (_: Exception) {
        false
    }

    /**
     * Вычищает коммит из всех локальных refs через git-filter-repo.
     *
     * Это не удаление diff: fast-export/fast-import переносит деревья, и
     * последующий коммит может сохранить те же файлы. Для удаления изменения
     * из одной ветки нужен drop в rebase. Перед запуском создаётся bundle всех
     * refs, потому что filter-repo по умолчанию удаляет reflogs/старые объекты
     * и origin remotes, а обычный reset-undo после него невозможен.
     */
    suspend fun deleteCommitWithFilterRepo(commit: Commit): Boolean {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return false
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return false
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status?.isClean != true) {
            error = strings.deleteCommitDirty
            return false
        }
        if (stashCount > 0) {
            error = strings.deleteCommitStashes
            return false
        }
        if (!filterRepoAvailable()) {
            error = strings.deleteCommitFilterMissing
            return false
        }
        if (!validCommitHash(commit.hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        val refs = rewriteRead(
            listOf(
                "for-each-ref",
                "--contains=${commit.hash}",
                "--format=%(refname)",
            ),
        ) ?: return false
        if (!refs.ok) {
            error = describe(refs)
            return false
        }
        if (refs.stdout.isBlank()) {
            error = strings.deleteCommitNotInHistory
            return false
        }

        val bundle = createFilterRepoBundle(commit) ?: return false
        val result = runGit(
            filterRepoDeleteArgs(commit.hash),
            environment = NONINTERACTIVE_OPERATION_ENV,
        ) ?: return false
        if (!result.ok) {
            error = buildString {
                error?.let { append(it) }
                append('\n')
                append(strings.deleteCommitFilterBundle(bundle.absolutePath))
            }
            return false
        }
        lastRewriteUndo = null
        refresh()
        publishToast(
            message = strings.toastFilterRepoDone(bundle.absolutePath),
            command = result.commandLine,
        )
        return true
    }

    private suspend fun createFilterRepoBundle(commit: Commit): File? =
        createFilterRepoBundle("commit-${commit.hash.take(7)}")

    private suspend fun createFilterRepoBundle(label: String): File? {
        val dir = File(repository.gitDir, "mybeautifulgit/backups")
        if (!dir.exists() && !dir.mkdirs()) {
            error = strings.deleteCommitFilterBackupFailed
            return null
        }
        val safeLabel = label.lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .take(48)
            .ifEmpty { "filter-repo" }
        val file = File(
            dir,
            "${System.currentTimeMillis()}-${++backupSequence}-$safeLabel.bundle",
        )
        val result = rewriteGit(listOf("bundle", "create", file.absolutePath, "--all"))
            ?: return null
        return if (result.ok) file else null
    }

    private fun filterRepoDeleteArgs(hash: String): List<String> = listOf(
        "filter-repo",
        "--force",
        "--commit-callback",
        "if commit.original_id == b\"$hash\": commit.skip()",
    )

    /** Настоящий dry-run filter-repo: ничего не меняет, но пишет оба fast-export потока. */
    suspend fun filterRepoPreview(request: FilterRepoRequest): FilterRepoPreview? {
        if (!filterRepoPreflight()) return null
        val refs = filterRepoRefs() ?: return null
        val spec = buildFilterRepoSpec(request, refs, dryRun = true) ?: return null
        busy = true
        error = null
        return try {
            val result = withContext(Dispatchers.IO) {
                backend.run(
                    repository.root,
                    spec.args,
                    timeoutSeconds = 300,
                    environment = NONINTERACTIVE_OPERATION_ENV,
                )
            }
            logCheck(result)
            if (!result.ok) {
                error = buildString {
                    append(strings.filterRepoPreviewFailed)
                    append('\n')
                    append(describe(result))
                }
                null
            } else {
                readFilterRepoPreview(dryRunCommand = spec.displayCommand)
                    ?.also { clearFilterRepoDryRunOutput() }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        } finally {
            spec.expressionsFile?.delete()
            busy = false
        }
    }

    /**
     * Переписывает все локальные refs через filter-repo. Перед запуском
     * обязательно создаются backup-ref и bundle; backup-ref не входит в --refs,
     * поэтому сам не переписывается. Восстановление — через ref/bundle, а не
     * branch-only Undo, потому что меняются все refs.
     */
    suspend fun filterRepoRewrite(request: FilterRepoRequest): Boolean {
        if (!filterRepoPreflight()) return false
        val branch = rewriteBranch() ?: return false
        val tip = rewriteHead() ?: return false
        val refs = filterRepoRefs() ?: return false
        val spec = buildFilterRepoSpec(request, refs, dryRun = false) ?: return false
        return try {
            val backup = createRewriteBackup("filter-repo", tip, branch) ?: run {
                error = strings.filterRepoBackupFailed
                return false
            }
            val bundle = createFilterRepoBundle("filter-repo-${tip.take(7)}") ?: run {
                error = strings.filterRepoBackupFailed
                return false
            }

            val result = runGit(
                spec.args,
                environment = NONINTERACTIVE_OPERATION_ENV,
            )
            if (result == null) {
                error = strings.filterRepoFailure(
                    error ?: strings.toastFailed,
                    backup.ref,
                    bundle.absolutePath,
                )
                return false
            }
            if (!result.ok) {
                error = strings.filterRepoFailure(
                    error ?: strings.toastFailed,
                    backup.ref,
                    bundle.absolutePath,
                )
                return false
            }

            refresh()
            // filter-repo rewrites every ref. A branch-only Undo would leave the
            // repository inconsistent, so recovery is the backup ref + bundle.
            lastRewriteUndo = null
            publishToast(
                message = strings.toastFilterRepoRewritten(backup.ref, bundle.absolutePath),
                command = result.commandLine,
            )
            return true
        } finally {
            spec.expressionsFile?.delete()
        }
    }

    private suspend fun filterRepoPreflight(): Boolean {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return false
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return false
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status?.isClean != true) {
            error = strings.deleteCommitDirty
            return false
        }
        if (stashCount > 0) {
            error = strings.deleteCommitStashes
            return false
        }
        if (currentBranchOrNull() == null) {
            error = strings.commitRewriteBranchRequired
            return false
        }
        if (!filterRepoAvailable()) {
            error = strings.deleteCommitFilterMissing
            return false
        }
        return true
    }

    private suspend fun filterRepoRefs(): List<String>? {
        val result = rewriteRead(listOf("for-each-ref", "--format=%(refname)")) ?: return null
        if (!result.ok) {
            error = describe(result)
            return null
        }
        val refs = result.stdout.lineSequence()
            .map { it.trim() }
            .filter { ref ->
                ref.startsWith("refs/") &&
                    !ref.startsWith("refs/mybeautifulgit/") &&
                    ref != "refs/stash" &&
                    !ref.startsWith("refs/replace/")
            }
            .distinct()
            .toList()
        if (refs.isEmpty()) {
            error = strings.filterRepoNoRefs
            return null
        }
        return refs
    }

    private fun buildFilterRepoSpec(
        request: FilterRepoRequest,
        refs: List<String>,
        dryRun: Boolean,
    ): FilterRepoSpec? {
        var expressionsFile: File? = null
        val modeArgs = when (request.mode) {
            FilterRepoMode.RemovePath -> {
                val path = normalizeFilterRepoPath(request.path)
                if (path == null) {
                    error = if (request.path.isBlank()) {
                        strings.filterRepoPathRequired
                    } else {
                        strings.filterRepoPathInvalid
                    }
                    return null
                }
                listOf("--invert-paths", "--path", path)
            }
            FilterRepoMode.ReplaceText -> {
                val find = request.find
                if (find.isBlank()) {
                    error = strings.filterRepoFindRequired
                    return null
                }
                if (find.any { it == '\n' || it == '\r' || it == '\u0000' } ||
                    "==>" in find ||
                    request.replacement.any { it == '\n' || it == '\r' || it == '\u0000' }
                ) {
                    error = strings.filterRepoFindRequired
                    return null
                }
                if (request.regex && runCatching { Regex(find) }.isFailure) {
                    error = strings.filterRepoRegexInvalid
                    return null
                }
                val file = createFilterRepoExpressions(request, find) ?: return null
                expressionsFile = file
                listOf("--replace-text", file.absolutePath)
            }
            FilterRepoMode.RenamePath -> {
                val oldPath = normalizeFilterRepoPath(request.oldPath)
                val newPath = normalizeFilterRepoPath(request.newPath)
                if (oldPath == null || newPath == null) {
                    error = if (request.oldPath.isBlank() || request.newPath.isBlank()) {
                        strings.filterRepoRenameRequired
                    } else {
                        strings.filterRepoRenameInvalid
                    }
                    return null
                }
                if (oldPath == newPath) {
                    error = strings.filterRepoRenameInvalid
                    return null
                }
                listOf("--path-rename", "$oldPath:$newPath")
            }
        }
        val args = buildList {
            add("filter-repo")
            add("--force")
            add("--refs")
            addAll(refs)
            addAll(modeArgs)
            if (dryRun) add("--dry-run")
        }
        val command = buildString {
            append("git filter-repo --force --refs <all refs except backup>")
            when (request.mode) {
                FilterRepoMode.RemovePath ->
                    append(" --invert-paths --path ").append(quoteArg(request.path.trim()))
                FilterRepoMode.ReplaceText ->
                    append(" --replace-text <temporary expressions file>")
                FilterRepoMode.RenamePath ->
                    append(" --path-rename ")
                        .append(quoteArg("${request.oldPath.trim()}:${request.newPath.trim()}"))
            }
            if (dryRun) append(" --dry-run")
        }
        return FilterRepoSpec(
            args = args,
            expressionsFile = expressionsFile,
            displayCommand = command,
        )
    }

    private fun createFilterRepoExpressions(
        request: FilterRepoRequest,
        find: String,
    ): File? {
        val dir = File(repository.gitDir, "mybeautifulgit/filter-repo")
        if (!dir.exists() && !dir.mkdirs()) {
            error = strings.filterRepoBackupFailed
            return null
        }
        val file = File(
            dir,
            "${System.currentTimeMillis()}-${++backupSequence}-expressions.txt",
        )
        val prefix = if (request.regex) "regex:" else "literal:"
        return try {
            file.writeText("$prefix$find==>${request.replacement}\n")
            file.deleteOnExit()
            file
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        }
    }

    private fun normalizeFilterRepoPath(value: String): String? {
        val raw = value.trim().replace('\\', '/')
        if (raw.isEmpty() || raw.startsWith('/') || raw.startsWith('-') || raw.startsWith("~/")) return null
        if (raw.length >= 3 && raw[1] == ':' && raw[2] == '/') return null
        if (raw.any { it == '\n' || it == '\r' || it == '\u0000' || it == '*' || it == '?' || it == '[' || it == ']' }) {
            return null
        }
        val normalized = raw.removePrefix("./").trimEnd('/')
        if (normalized.isEmpty() || normalized == ".") return null
        if (normalized == ".git" || normalized.startsWith(".git/")) return null
        if (normalized.split('/').any { it.isEmpty() || it == ".." || it == "." }) return null
        return normalized
    }

    private fun readFilterRepoPreview(
        dryRunCommand: String,
    ): FilterRepoPreview? {
        val original = File(repository.gitDir, "filter-repo/fast-export.original")
        val filtered = File(repository.gitDir, "filter-repo/fast-export.filtered")
        if (!original.isFile || !filtered.isFile) {
            error = strings.filterRepoPreviewOutput
            return null
        }
        if (original.length() > MAX_FILTER_REPO_PREVIEW_BYTES ||
            filtered.length() > MAX_FILTER_REPO_PREVIEW_BYTES
        ) {
            error = strings.repoFileTooLarge
            return null
        }
        val changedRefs = File(repository.gitDir, "filter-repo/changed-refs")
        if (changedRefs.isFile && changedRefs.length() > MAX_FILTER_REPO_PREVIEW_BYTES) {
            error = strings.repoFileTooLarge
            return null
        }
        val before = countFastExportCommits(original)
        val after = countFastExportCommits(filtered)
        val refs = countChangedRefs()
        val paths = changedFastExportPaths(original, filtered)
        return FilterRepoPreview(
            command = dryRunCommand,
            commitsBefore = before,
            commitsAfter = after,
            changedRefs = refs,
            changedPaths = paths,
            summary = strings.filterRepoPreviewSummary(before, after, refs, paths),
        )
    }

    private fun clearFilterRepoDryRunOutput() {
        listOf("fast-export.original", "fast-export.filtered", "changed-refs").forEach { name ->
            File(repository.gitDir, "filter-repo/$name").delete()
        }
    }

    private fun countFastExportCommits(file: File): Int = runCatching {
        file.useLines { lines ->
            lines.take(MAX_FILTER_REPO_PREVIEW_LINES).count { it.startsWith("commit ") }
        }
    }.getOrDefault(0)

    private fun countChangedRefs(): Int {
        val file = File(repository.gitDir, "filter-repo/changed-refs")
        return if (!file.isFile) 0 else runCatching {
            file.useLines { lines ->
                lines.take(MAX_FILTER_REPO_PREVIEW_LINES).count { it.isNotBlank() }
            }
        }.getOrDefault(0)
    }

    private fun changedFastExportPaths(original: File, filtered: File): Int {
        fun paths(file: File): Set<String> = runCatching {
            file.useLines { lines ->
                lines.take(MAX_FILTER_REPO_PREVIEW_LINES).mapNotNull { line ->
                    when {
                        line.startsWith("M ") -> line.split(' ', limit = 4).getOrNull(3)
                        line.startsWith("D ") -> line.substring(2).trim()
                        line.startsWith("C ") -> line.split(' ', limit = 3).getOrNull(2)
                        else -> null
                    }
                }.toSet()
            }
        }.getOrDefault(emptySet())
        val before = paths(original)
        val after = paths(filtered)
        return (before - after).size + (after - before).size
    }

    private data class FilterRepoSpec(
        val args: List<String>,
        val expressionsFile: File?,
        val displayCommand: String,
    )

    /** Читает последствия reset до подтверждения, не меняя репозиторий. */
    suspend fun resetPreview(hash: String): ResetPreview? {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return null
        }
        busy = true
        error = null
        return try {
            withContext(Dispatchers.IO) {
                backend.resetPreview(repository, hash)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        } finally {
            busy = false
        }
    }

    /**
     * План потери достижимости для reset или rewrite: [targets] — будущие
     * закреплённые коммиты (для reset один, для rewrite — parents заменяемого).
     */
    suspend fun unreachablePlan(targets: List<String>): UnreachablePlan? {
        if (targets.any { !validCommitHash(it) }) {
            error = strings.commitActionInvalidHash
            return null
        }
        busy = true
        error = null
        return try {
            withContext(Dispatchers.IO) {
                backend.unreachablePlan(repository, targets)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        } finally {
            busy = false
        }
    }

    /**
     * Staged diff for the optional AI commit-message helper. This deliberately
     * reads the index only (`git diff --cached`) and never includes unstaged or
     * untracked worktree files. The caller sanitizes and bounds the text.
     */
    suspend fun stagedDiffForAi(): String? {
        if (repository.workTree == null) {
            error = strings.repoBareNotice
            return null
        }
        if ((status?.stagedCount ?: 0) == 0) {
            error = strings.repoNothingStaged
            return null
        }
        return try {
            val result = withContext(Dispatchers.IO) {
                backend.run(
                    cwd = repository.root,
                    args = listOf(
                        "diff",
                        "--cached",
                        "--no-color",
                        "--no-ext-diff",
                        "--no-textconv",
                        "--unified=3",
                    ),
                    timeoutSeconds = 60,
                )
            }
            if (!result.ok && !result.stdoutTruncated) {
                error = describe(result)
                null
            } else {
                result.stdout
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = displayError(failure)
            null
        }
    }

    /**
     * Explicit worktree fallback for AI generation. It includes only unstaged
     * tracked changes plus bounded previews of untracked text files; staged
     * content is not included. The UI keeps this behind a separate action.
     */
    suspend fun worktreeDiffForAi(): String? {
        val root = repository.workTree
        if (root == null) {
            error = strings.repoBareNotice
            return null
        }
        val entries = status?.entries.orEmpty()
            .filter { it.isUnstaged && !it.isConflicted && it.kind != StatusKind.Ignored }
        if (entries.isEmpty()) {
            error = strings.repoNothingStaged
            return null
        }
        val trackedPaths = entries
            .filter { it.kind != StatusKind.Untracked }
            .map { it.path }
        val untracked = entries.filter { it.kind == StatusKind.Untracked }

        val trackedDiff = if (trackedPaths.isEmpty()) {
            null
        } else {
            try {
                backend.run(
                    cwd = repository.root,
                    args = listOf(
                        "diff",
                        "--no-color",
                        "--no-ext-diff",
                        "--no-textconv",
                        "--unified=3",
                        "--",
                    ) + trackedPaths,
                    timeoutSeconds = 60,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = displayError(failure)
                return null
            }
        }
        if (trackedDiff != null && !trackedDiff.ok && !trackedDiff.stdoutTruncated) {
            error = describe(trackedDiff)
            return null
        }

        val builder = StringBuilder()
        trackedDiff?.stdout?.trimEnd()?.let { if (it.isNotBlank()) builder.append(it) }
        for (entry in untracked) {
            if (builder.length >= AI_WORKTREE_DIFF_MAX_CHARS) break
            if (isKnownBinaryPath(entry.path)) continue
            val file = runCatching {
                RepositoryPaths.resolve(File(root), entry.path)
            }.getOrNull() ?: continue
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue
            if (Files.size(file) > MAX_TEXT_DIFF_BYTES || hasNulPrefix(file)) continue
            val preview = untrackedDiff(entry.path).trimEnd()
            if (preview.isBlank() || preview.startsWith("File is too large")) continue
            builder.append('\n').append(preview)
        }
        return builder.toString().trim().ifBlank { null }
    }

    /** Полное сообщение выбранного коммита для редактора reword/amend. */
    suspend fun loadCommitMessage(hash: String): String? {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return null
        }
        busy = true
        error = null
        return try {
            withContext(Dispatchers.IO) { backend.commitMessage(repository, hash) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        } finally {
            busy = false
        }
    }

    /**
     * Переписывает сообщение и автора выбранного коммита (включая root и merge-target),
     * затем возвращает его потомков через cherry-pick.
     */
    suspend fun rewordCommit(commit: Commit, message: String, author: String): Boolean {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) {
            error = strings.repoCommitMessageRequired
            return false
        }
        val identity = parseIdentity(author) ?: run {
            error = strings.repoCommitIdentityInvalid
            return false
        }
        val tip = rewriteHead() ?: return false
        if (!rewritePreflight(commit, tip, allowComplexTarget = true)) return false
        val branch = rewriteBranch() ?: return false
        val backup = createRewriteBackup("reword", tip, branch) ?: return false

        busy = true
        error = null
        try {
            val tree = rewriteRead(listOf("rev-parse", "${commit.hash}^{tree}")) ?: return false
            if (!tree.ok) {
                error = describe(tree)
                return false
            }
            val treeHash = tree.stdout.trim()
            if (!validCommitHash(treeHash)) {
                error = strings.commitRewriteInvalidTree
                return false
            }
            val created = rewriteGit(
                args = buildList {
                    add("commit-tree")
                    add(treeHash)
                    commit.parents.forEach { parent ->
                        add("-p")
                        add(parent)
                    }
                    add("-m")
                    add(cleanMessage)
                },
                environment = mapOf(
                    "GIT_AUTHOR_NAME" to identity.name,
                    "GIT_AUTHOR_EMAIL" to identity.email,
                    "GIT_AUTHOR_DATE" to "@${commit.authorTime}",
                ),
            ) ?: return false
            if (!created.ok) return false
            val newHash = created.stdout.trim().takeIf(::validCommitHash) ?: run {
                error = strings.commitRewriteInvalidTree
                return false
            }
            val reset = rewriteGit(listOf("reset", "--hard", newHash)) ?: return false
            if (!reset.ok) return false

            var last = reset
            if (tip != commit.hash) {
                val replay = rewriteGit(
                    args = listOf("cherry-pick", "${commit.hash}..$tip"),
                    environment = NONINTERACTIVE_OPERATION_ENV,
                ) ?: return false
                last = replay
                if (!replay.ok) {
                    refresh()
                    return false
                }
            }
            refresh()
            publishToast(
                message = strings.toastCommitReworded,
                command = last.commandLine,
                undo = rewriteHead()?.let { RewriteUndo(backup, it) },
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Начинает amend/split: target остаётся в worktree, ветка стоит на parent. */
    suspend fun startCommitRewrite(
        commit: Commit,
        mode: RewriteMode,
        message: String,
        author: String,
    ): Boolean {
        if (mode == RewriteMode.Reword) {
            error = strings.commitRewriteInvalidMode
            return false
        }
        val cleanMessage = message.trim()
        if (mode == RewriteMode.Amend && cleanMessage.isEmpty()) {
            error = strings.repoCommitMessageRequired
            return false
        }
        val identity = parseIdentity(author) ?: run {
            error = strings.repoCommitIdentityInvalid
            return false
        }
        val tip = rewriteHead() ?: return false
        if (!rewritePreflight(commit, tip)) return false
        val branch = rewriteBranch() ?: return false
        val backup = createRewriteBackup(
            operation = if (mode == RewriteMode.Amend) "amend-rewrite" else "split",
            tip = tip,
            branch = branch,
        ) ?: return false
        val parent = commit.parents.single()

        busy = true
        error = null
        try {
            val targetReset = rewriteGit(listOf("reset", "--hard", commit.hash)) ?: return false
            if (!targetReset.ok) return false
            val resetMode = if (mode == RewriteMode.Amend) "--soft" else "--mixed"
            val parentReset = rewriteGit(listOf("reset", resetMode, parent)) ?: return false
            if (!parentReset.ok) return rollbackRewrite(tip)

            pendingRewrite = PendingRewrite(
                targetHash = commit.hash,
                targetSubject = commit.subject,
                originalTip = tip,
                parentHash = parent,
                branch = branch,
                backupRef = backup.ref,
                mode = mode,
            )
            commitMessage = if (mode == RewriteMode.Amend) cleanMessage else ""
            commitAuthor = identity.format()
            commitCommitter = ""
            retryCommitArgs = null
            amend = false
            commitMode = CommitMode.Normal
            fixupTarget = ""
            noVerify = false
            commitTemplatePath = ""
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastCommitRewriteStarted,
                command = parentReset.commandLine,
                success = true,
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Завершает split/amend и возвращает сохранённых потомков. */
    suspend fun finishCommitRewrite(): Boolean {
        val pending = pendingRewrite ?: return false
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        val branch = rewriteBranch() ?: return false
        if (branch != pending.branch) {
            error = strings.commitRewriteBranchChanged
            return false
        }
        if (status?.isClean != true) {
            error = strings.commitRewriteCleanRequired
            return false
        }
        val head = rewriteHead() ?: return false
        if (head == pending.parentHash) {
            error = strings.commitRewriteNoReplacement
            return false
        }
        if (head == pending.originalTip) {
            pendingRewrite = null
            cleanupAfterRewrite()
            refresh()
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastCommitRewriteFinished,
                command = "git reset --hard ${pending.originalTip}",
                success = true,
            )
            return true
        }
        val basedOnParent = rewriteRead(listOf("merge-base", "--is-ancestor", pending.parentHash, head))
            ?: return false
        if (!basedOnParent.ok) {
            error = strings.commitRewriteNotOnParent
            return false
        }
        val containsOriginal = rewriteRead(listOf("merge-base", "--is-ancestor", pending.originalTip, head))
            ?: return false
        if (containsOriginal.ok) {
            error = strings.commitRewriteOriginalAlreadyPresent
            return false
        }

        busy = true
        error = null
        try {
            pendingRewrite = null
            var last: GitResult? = null
            if (pending.originalTip != pending.targetHash) {
                last = rewriteGit(
                    args = listOf("cherry-pick", "${pending.targetHash}..${pending.originalTip}"),
                    environment = NONINTERACTIVE_OPERATION_ENV,
                ) ?: return false
                if (!last.ok) {
                    refresh()
                    return false
                }
            }
            cleanupAfterRewrite()
            refresh()
            val undo = rewriteHead()?.let { postTip ->
                RewriteUndo(
                    backup = RewriteBackup(
                        ref = pending.backupRef,
                        previousTip = pending.originalTip,
                        branch = pending.branch,
                        operation = pending.mode.name.lowercase(),
                    ),
                    postTip = postTip,
                )
            }
            publishToast(
                message = strings.toastCommitRewriteFinished,
                command = last?.commandLine ?: "git cherry-pick",
                undo = undo,
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Отменяет split/amend и возвращает ветку на исходный tip. */
    suspend fun cancelCommitRewrite(): Boolean {
        val pending = pendingRewrite ?: return false
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        val branch = rewriteBranch() ?: return false
        if (branch != pending.branch) {
            error = strings.commitRewriteBranchChanged
            return false
        }
        busy = true
        error = null
        try {
            val result = rewriteGit(listOf("reset", "--hard", pending.originalTip)) ?: return false
            if (!result.ok) return false
            pendingRewrite = null
            cleanupAfterRewrite()
            refresh()
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastCommitRewriteCancelled,
                command = result.commandLine,
                success = true,
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Снимает с git настоящий todo для выбранного коммита и его потомков. */
    suspend fun loadInteractiveRebasePlan(
        commit: Commit,
        autosquash: Boolean = false,
    ): RebasePlan? {
        val base = commit.parents.firstOrNull()
        return loadInteractiveRebasePlan(base = base, root = base == null, autosquash = autosquash)
    }

    /**
     * Пробный `git rebase -i` с sequence-editor, который копирует todo и
     * завершается с ошибкой. Так приложение получает ровно тот список, который
     * построил git, включая label/reset/merge для --rebase-merges, но ничего не
     * переписывает до подтверждения пользователя.
     */
    suspend fun loadInteractiveRebasePlan(
        base: String?,
        root: Boolean,
        autosquash: Boolean = false,
    ): RebasePlan? {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return null
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return null
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return null
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return null
        }
        if (status == null) refresh()
        // Refresh мог обнаружить операцию, которую ещё не знало state.
        if (operation != null || pendingRewrite != null || pendingNoCommit != null) {
            error = strings.commitRewriteActiveOperation
            return null
        }
        if (status?.isClean != true) {
            error = strings.commitRewriteCleanRequired
            return null
        }
        val tip = rewriteHead() ?: return null
        if (!root) {
            if (base == null || !validCommitHash(base)) {
                error = strings.commitActionInvalidHash
                return null
            }
            val ancestor = rewriteRead(listOf("merge-base", "--is-ancestor", base, tip))
                ?: return null
            if (!ancestor.ok) {
                error = if (ancestor.exitCode == 1) {
                    strings.commitRewriteNotInHistory
                } else {
                    describe(ancestor)
                }
                return null
            }
        }
        val countArgs = if (root) {
            listOf("rev-list", "--count", "HEAD")
        } else {
            listOf("rev-list", "--count", "$base..HEAD")
        }
        val count = rewriteRead(countArgs) ?: return null
        if (!count.ok) {
            error = describe(count)
            return null
        }
        if ((count.stdout.trim().toIntOrNull() ?: 0) <= 0) {
            error = strings.rebaseEditorEmptyPlan
            return null
        }

        val workspace = writeRebaseWorkspace(
            entries = emptyList(),
            messages = emptyMap(),
            abortAfterSequence = true,
        ) ?: return null
        val args = interactiveRebaseArgs(base, root, autosquash)
        val result = rebaseRead(args, rebaseEditorEnv(workspace))
            ?: return null
        val todoText = runCatching { workspace.todo.readText() }.getOrNull()
        if (todoText == null) {
            error = strings.rebaseEditorPlanFailed
            return null
        }
        val entries = RebaseTodo.parse(todoText)
        if (entries.isEmpty()) {
            error = strings.rebaseEditorEmptyPlan
            return null
        }
        // Если git почему-то успел начать rebase, не оставляем его запущенным.
        if (result.ok) {
            refreshRuntimeState()
            if (operation != null) {
                rebaseRead(listOf("rebase", "--abort"))
                refreshRuntimeState()
            }
            error = strings.rebaseEditorPlanFailed
            return null
        }
        val withMessages = attachRewordMessages(entries)
        return RebasePlan(
            base = base,
            root = root,
            entries = withMessages,
            autosquash = autosquash,
        )
    }

    /** Запускает интерактивный rebase с отредактированным todo. */
    suspend fun startInteractiveRebase(plan: RebasePlan): Boolean {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return false
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return false
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status?.isClean != true) {
            error = strings.commitRewriteCleanRequired
            return false
        }
        if (!plan.root) {
            if (plan.base == null || !validCommitHash(plan.base)) {
                error = strings.commitActionInvalidHash
                return false
            }
        }
        if (!validateRebaseTodo(plan.entries)) return false

        val tip = rewriteHead() ?: return false
        val branch = rewriteBranch() ?: return false
        if (!plan.root) {
            val ancestor = rewriteRead(listOf("merge-base", "--is-ancestor", plan.base!!, tip))
                ?: return false
            if (!ancestor.ok) {
                error = strings.commitRewriteNotInHistory
                return false
            }
        }

        val prepared = attachRewordMessages(plan.entries)
        val workspace = writeRebaseWorkspace(prepared, rewordMessages(prepared)) ?: return false
        val backup = createRewriteBackup("interactive-rebase", tip, branch) ?: return false
        pendingRebaseBackup = backup
        val result = runGit(
            args = interactiveRebaseArgs(plan.base, plan.root, autosquash = false),
            environment = rebaseEditorEnv(workspace),
        ) ?: run {
            pendingRebaseBackup = null
            return false
        }
        if (result.ok) {
            refresh()
            if (operation != null) {
                publishToast(
                    message = strings.toastInteractiveRebaseStopped,
                    command = result.commandLine,
                )
                return false
            }
            finishInteractiveRebase(backup, result.commandLine)
            return true
        }
        val failure = error
        refresh()
        if (failure != null) error = failure
        if (operation != null) {
            publishToast(
                message = strings.toastInteractiveRebaseStopped,
                command = result.commandLine,
            )
        } else {
            pendingRebaseBackup = null
        }
        return false
    }

    /** Читает оставшийся todo, пока rebase стоит на конфликте/edit/break. */
    suspend fun loadRebaseTodo(): RebasePlan? {
        if (operation?.kind != GitOperationKind.Rebase) {
            error = strings.repoOperationNoActive
            return null
        }
        val loaded = try {
            withContext(Dispatchers.IO) {
                RepositoryPaths.readTextLimited(
                    File(repository.gitDir),
                    "rebase-merge/git-rebase-todo",
                    MAX_SAFE_TEXT_FILE_BYTES,
                )
            }
        } catch (error: Exception) {
            this.error = pathError(error)
            return null
        }
        if (loaded.truncated) {
            error = strings.repoFileTooLarge
            return null
        }
        val entries = attachRewordMessages(RebaseTodo.parse(loaded.text))
        if (entries.isEmpty()) {
            error = strings.rebaseEditorEmptyPlan
            return null
        }
        return RebasePlan(base = null, root = false, entries = entries)
    }

    /** Заменяет remaining-todo через `git rebase --edit-todo`. */
    suspend fun saveRebaseTodo(entries: List<RebaseTodoEntry>): Boolean {
        if (operation?.kind != GitOperationKind.Rebase) {
            error = strings.repoOperationNoActive
            return false
        }
        if (!validateRebaseTodo(entries)) return false
        val prepared = attachRewordMessages(entries)
        val workspace = writeRebaseWorkspace(prepared, rewordMessages(prepared)) ?: return false
        val result = runGit(
            args = listOf("rebase", "--edit-todo"),
            environment = rebaseEditorEnv(workspace),
        ) ?: return false
        if (result.ok) {
            refresh()
            return true
        }
        val failure = error
        refresh()
        if (failure != null) error = failure
        return false
    }

    private suspend fun attachRewordMessages(
        entries: List<RebaseTodoEntry>,
    ): List<RebaseTodoEntry> = entries.map { entry ->
        if (entry.command != RebaseCommand.Reword) return@map entry
        val reference = entry.hash.ifBlank { entry.argument }
        val resolved = rebaseRead(listOf("rev-parse", "--verify", "$reference^{commit}"))
        val fullHash = resolved?.takeIf { it.ok }?.stdout?.trim()
            ?.takeIf(::validCommitHash) ?: reference
        val message = entry.message.takeIf { it.isNotBlank() }
            ?: rebaseRead(listOf("show", "-s", "--format=%B", fullHash))
                ?.takeIf { it.ok }
                ?.stdout
                ?.trimEnd()
                ?.takeIf { it.isNotBlank() }
            ?: entry.subject
        entry.copy(
            argument = fullHash,
            message = message,
        )
    }

    private suspend fun finishInteractiveRebase(backup: RewriteBackup, command: String) {
        pendingRebaseBackup = null
        val postTip = rewriteHead() ?: backup.previousTip
        publishToast(
            message = strings.toastInteractiveRebaseDone,
            command = command,
            undo = RewriteUndo(backup, postTip),
        )
    }

    /**
     * Меняет местами выбранный коммит с соседним родителем или ребёнком
     * через todo интерактивного rebase. Меняются только две соседние
     * pick-строки; merge и неоднозначного ребёнка отклоняем.
     */
    suspend fun moveCommit(commit: Commit, earlier: Boolean): Boolean {
        if (commit.isMerge) {
            error = strings.reorderMergeUnsupported
            return false
        }
        val firstHash: String
        val secondHash: String
        val base: String?
        val root: Boolean
        if (earlier) {
            val parentHash = commit.parents.firstOrNull() ?: run {
                error = strings.reorderNoParent
                return false
            }
            val parent = commits.firstOrNull { it.hash == parentHash } ?: run {
                error = strings.reorderTargetMissing
                return false
            }
            firstHash = parent.hash
            secondHash = commit.hash
            base = parent.parents.firstOrNull()
            root = parent.parents.isEmpty()
        } else {
            val children = commits.filter { it.parents.contains(commit.hash) }
            if (children.isEmpty()) {
                error = strings.reorderNoChild
                return false
            }
            if (children.size > 1) {
                error = strings.reorderAmbiguousChild
                return false
            }
            val child = children.single()
            if (child.isMerge) {
                error = strings.reorderMergeUnsupported
                return false
            }
            firstHash = commit.hash
            secondHash = child.hash
            base = commit.parents.firstOrNull()
            root = commit.parents.isEmpty()
        }

        val plan = loadInteractiveRebasePlan(base, root) ?: return false
        val firstIndex = plan.entries.indexOfFirst { entryMatchesHash(it, firstHash) }
        val secondIndex = plan.entries.indexOfFirst { entryMatchesHash(it, secondHash) }
        if (firstIndex < 0 || secondIndex < 0) {
            error = strings.reorderTargetMissing
            return false
        }
        if (kotlin.math.abs(firstIndex - secondIndex) != 1) {
            error = strings.reorderNotAdjacent
            return false
        }
        val firstEntry = plan.entries[firstIndex]
        val secondEntry = plan.entries[secondIndex]
        if (firstEntry.command != RebaseCommand.Pick || secondEntry.command != RebaseCommand.Pick) {
            error = strings.reorderMergeUnsupported
            return false
        }
        val updated = plan.entries.toMutableList()
        updated[firstIndex] = secondEntry
        updated[secondIndex] = firstEntry
        return startInteractiveRebase(plan.copy(entries = updated))
    }

    /** `git rebase --onto <onto> <upstream>` для текущей локальной ветки. */
    suspend fun rebaseOnto(
        upstream: String,
        onto: String,
        preserveMerges: Boolean = false,
    ): Boolean {
        val cleanUpstream = upstream.trim()
        val cleanOnto = onto.trim()
        if (cleanUpstream.isBlank() || cleanOnto.isBlank()) {
            error = strings.historyRewriteEmptyRef
            return false
        }
        val resolvedUpstream = resolveCommitHash(cleanUpstream) ?: return false
        val resolvedOnto = resolveCommitHash(cleanOnto) ?: return false
        return rebaseOntoResolved(resolvedUpstream, resolvedOnto, preserveMerges)
    }

    /**
     * Переносит commits после начала range (A..B) на новый base. Для
     * однозначности range должен заканчиваться текущим HEAD; произвольный
     * upstream доступен через rebase --onto.
     */
    suspend fun moveRange(
        range: String,
        onto: String,
        preserveMerges: Boolean = false,
    ): Boolean {
        val clean = range.trim()
        if (!validRevisionRange(clean)) {
            error = strings.commitActionInvalidRange
            return false
        }
        val parts = clean.split("..", limit = 2)
        val from = parts.getOrNull(0)?.trim().orEmpty()
        val to = parts.getOrNull(1)?.trim().orEmpty()
        if (from.isBlank() || to.isBlank()) {
            error = strings.commitActionInvalidRange
            return false
        }
        val head = rewriteHead() ?: return false
        val end = resolveCommitHash(to) ?: return false
        if (!end.equals(head, ignoreCase = true)) {
            error = strings.historyRewriteRangeNotHead
            return false
        }
        return rebaseOnto(from, onto, preserveMerges)
    }

    /**
     * Собирает range A..B в один коммит. Первый коммит range получает
     * заданное сообщение (или сохраняет своё), остальные превращаются в fixup;
     * коммиты после B остаются pick. Merge-коммиты в range не поддерживаются.
     */
    suspend fun squashRange(range: String, message: String = ""): Boolean {
        val clean = range.trim()
        if (!validRevisionRange(clean)) {
            error = strings.commitActionInvalidRange
            return false
        }
        val parts = clean.split("..", limit = 2)
        val from = parts.getOrNull(0)?.trim().orEmpty()
        val to = parts.getOrNull(1)?.trim().orEmpty()
        if (from.isBlank() || to.isBlank()) {
            error = strings.commitActionInvalidRange
            return false
        }
        val upstream = resolveCommitHash(from) ?: return false
        val end = resolveCommitHash(to) ?: return false
        val tip = rewriteHead() ?: return false
        val endIsAncestor = rewriteRead(listOf("merge-base", "--is-ancestor", end, tip))
            ?: return false
        if (!endIsAncestor.ok) {
            error = strings.historyRewriteRangeNotInHistory
            return false
        }
        val startIsAncestor = rewriteRead(listOf("merge-base", "--is-ancestor", upstream, end))
            ?: return false
        if (!startIsAncestor.ok) {
            error = strings.historyRewriteRangeNotInHistory
            return false
        }
        val commitsResult = rewriteRead(listOf("rev-list", "--reverse", clean)) ?: return false
        if (!commitsResult.ok) {
            error = describe(commitsResult)
            return false
        }
        val rangeHashes = commitsResult.stdout.lineSequence()
            .map { it.trim() }
            .filter { validCommitHash(it) }
            .toList()
        if (rangeHashes.size < 2) {
            error = strings.historyRewriteRangeTooShort
            return false
        }
        val merges = rewriteRead(listOf("rev-list", "--merges", clean)) ?: return false
        if (!merges.ok) {
            error = describe(merges)
            return false
        }
        if (merges.stdout.isNotBlank()) {
            error = strings.historyRewriteRangeMerge
            return false
        }

        val plan = loadInteractiveRebasePlan(upstream, root = false) ?: return false
        val indexes = rangeHashes.map { hash ->
            plan.entries.indexOfFirst { entryMatchesHash(it, hash) }
        }
        if (indexes.any { it < 0 }) {
            error = strings.historyRewriteRangeNotLinear
            return false
        }
        val first = indexes.minOrNull() ?: return false
        val last = indexes.maxOrNull() ?: return false
        val firstEntry = plan.entries[first]
        if (firstEntry.command != RebaseCommand.Pick) {
            error = strings.historyRewriteRangeNotLinear
            return false
        }
        val updated = plan.entries.toMutableList()
        for (index in first..last) {
            val entry = updated[index]
            if (!entry.command.takesCommit || rangeHashes.none { entryMatchesHash(entry, it) }) {
                error = strings.historyRewriteRangeNotLinear
                return false
            }
            updated[index] = if (index == first) {
                if (message.isBlank()) {
                    entry
                } else {
                    entry.copy(command = RebaseCommand.Reword, message = message.trim())
                }
            } else {
                entry.copy(command = RebaseCommand.Fixup, argument = entry.hash)
            }
        }
        return startInteractiveRebase(plan.copy(entries = updated))
    }

    private suspend fun rebaseOntoResolved(
        upstream: String,
        onto: String,
        preserveMerges: Boolean,
    ): Boolean {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return false
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return false
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status == null) refresh()
        if (status?.isClean != true) {
            error = strings.commitRewriteCleanRequired
            return false
        }
        val tip = rewriteHead() ?: return false
        val branch = rewriteBranch() ?: return false
        val ancestor = rewriteRead(listOf("merge-base", "--is-ancestor", upstream, tip))
            ?: return false
        if (!ancestor.ok) {
            error = strings.historyRewriteNotAncestor
            return false
        }
        val count = rewriteRead(listOf("rev-list", "--count", "$upstream..HEAD")) ?: return false
        if (!count.ok) {
            error = describe(count)
            return false
        }
        if ((count.stdout.trim().toIntOrNull() ?: 0) <= 0) {
            error = strings.historyRewriteEmptyRange
            return false
        }
        val backup = createRewriteBackup("rebase-onto", tip, branch) ?: return false
        pendingRebaseBackup = backup
        val args = buildList {
            add("rebase")
            if (preserveMerges) add("--rebase-merges")
            add("--onto")
            add(onto)
            add(upstream)
        }
        val result = runGit(args, environment = NONINTERACTIVE_OPERATION_ENV) ?: run {
            pendingRebaseBackup = null
            return false
        }
        if (result.ok) {
            refresh()
            if (operation != null) {
                publishToast(
                    message = strings.toastInteractiveRebaseStopped,
                    command = result.commandLine,
                )
                return false
            }
            pendingRebaseBackup = null
            val postTip = rewriteHead() ?: backup.previousTip
            publishToast(
                message = strings.toastRebaseOntoDone,
                command = result.commandLine,
                undo = RewriteUndo(backup, postTip),
            )
            return true
        }
        val failure = error
        refresh()
        if (failure != null) error = failure
        if (operation != null) {
            publishToast(
                message = strings.toastInteractiveRebaseStopped,
                command = result.commandLine,
            )
        } else {
            pendingRebaseBackup = null
        }
        return false
    }

    private fun entryMatchesHash(entry: RebaseTodoEntry, hash: String): Boolean =
        entry.hash.isNotBlank() && (entry.hash == hash || hash.startsWith(entry.hash))

    private fun validateRebaseTodo(entries: List<RebaseTodoEntry>): Boolean {
        val problem = RebaseTodo.validate(entries) ?: return true
        error = when (problem) {
            RebaseTodoProblem.Empty -> strings.rebaseEditorEmptyPlan
            RebaseTodoProblem.Newline -> strings.rebaseEditorInvalidTodo
            RebaseTodoProblem.MissingCommit -> strings.rebaseEditorMissingCommit
            RebaseTodoProblem.MissingExec -> strings.rebaseEditorMissingExec
            RebaseTodoProblem.MissingLabel -> strings.rebaseEditorMissingLabel
            RebaseTodoProblem.MissingMerge -> strings.rebaseEditorMissingMerge
            RebaseTodoProblem.MissingMessage -> strings.rebaseEditorMissingMessage
        }
        return false
    }

    private fun interactiveRebaseArgs(
        base: String?,
        root: Boolean,
        autosquash: Boolean,
    ): List<String> = buildList {
        add("rebase")
        add("-i")
        add("--rebase-merges")
        if (autosquash) add("--autosquash") else add("--no-autosquash")
        if (root) add("--root") else add(base ?: "HEAD")
    }

    private fun rewordMessages(entries: List<RebaseTodoEntry>): Map<String, String> =
        entries.filter { it.command == RebaseCommand.Reword }
            .associate { entry ->
                entry.argument to (entry.message.ifBlank { entry.subject }.trimEnd() + "\n")
            }

    private fun writeRebaseWorkspace(
        entries: List<RebaseTodoEntry>,
        messages: Map<String, String>,
        abortAfterSequence: Boolean = false,
    ): RebaseWorkspace? {
        val base = File(repository.gitDir)
        val relative = "mybeautifulgit/rebase-todo"
        return try {
            if (RepositoryPaths.exists(base, relative) &&
                !RepositoryPaths.deleteTree(base, relative)
            ) {
                throw RepositoryPathException("cannot clear the rebase workspace")
            }
            val dir = RepositoryPaths.createDirectories(base, relative)
            val todo = RepositoryPaths.writeText(
                base,
                "$relative/todo",
                RebaseTodo.render(entries),
            ).toFile()
            val messageDir = RepositoryPaths.createDirectories(base, "$relative/messages")
            messages.forEach { (hash, text) ->
                RepositoryPaths.writeText(base, "$relative/messages/$hash.txt", text)
            }
            val sequence = RepositoryPaths.writeText(
                base,
                "$relative/sequence-editor.sh",
                if (abortAfterSequence) {
                    // Capture: Git's todo already is $1; copy it out and abort the rebase.
                    "#!/bin/sh\ncat \"\$1\" > ${shellQuote(posixPath(todo))}\nexit 1\n"
                } else {
                    "#!/bin/sh\ncat ${shellQuote(posixPath(todo))} > \"\$1\"\n"
                },
            ).toFile()
            val message = RepositoryPaths.writeText(
                base,
                "$relative/message-editor.sh",
                buildString {
                    append("#!/bin/sh\n")
                    append("done_file=")
                    append(
                        shellQuote(
                            posixPath(File(repository.gitDir, "rebase-merge/done")),
                        ),
                    )
                    append('\n')
                    append("[ -f \"\$done_file\" ] || exit 0\n")
                    append("target=\"\$1\"\n")
                    append("line=\$(tail -n 1 \"\$done_file\")\n")
                    append("set -- \$line\n")
                    append("[ \"\$1\" = reword ] || exit 0\n")
                    append("hash=\$2\n")
                    append("message=")
                    append(shellQuote(posixPath(messageDir.toFile())))
                    append("/\"\$hash\".txt\n")
                    append("[ -f \"\$message\" ] && cat \"\$message\" > \"\$target\"\n")
                    append("exit 0\n")
                },
            ).toFile()
            RebaseWorkspace(dir.toFile(), todo, sequence, message, messageDir.toFile())
        } catch (error: Exception) {
            this.error = pathError(error)
            null
        }
    }

    private fun rebaseEditorEnv(workspace: RebaseWorkspace): Map<String, String> = mapOf(
        "GIT_SEQUENCE_EDITOR" to "sh ${shellQuote(posixPath(workspace.sequenceEditor))}",
        "GIT_EDITOR" to "sh ${shellQuote(posixPath(workspace.messageEditor))}",
        "GIT_MERGE_AUTOEDIT" to "no",
    )

    private fun posixPath(file: File): String = file.absolutePath.replace('\\', '/')

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"

    /** Чтение plan-capture/edit-todo без превращения ожидаемого exit 1 в error. */
    private suspend fun rebaseRead(
        args: List<String>,
        environment: Map<String, String> = emptyMap(),
    ): GitResult? = try {
        val result = withContext(Dispatchers.IO) {
            backend.run(
                repository.root,
                args,
                timeoutSeconds = 300,
                environment = environment,
            )
        }
        logCheck(result)
        result
    } catch (error: Exception) {
        this.error = displayError(error)
        null
    }

    private data class RewriteUndoOutcome(val command: String, val stashed: Boolean)

    private fun journalOperationInProgress(): Boolean {
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return true
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return true
        }
        if (pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return true
        }
        return false
    }

    /** Откатывает rewrite на backup-ref; вызывается и toast-Undo, и журналом. */
    private suspend fun performRewriteUndo(undo: RewriteUndo): RewriteUndoOutcome? {
        if (journalOperationInProgress()) return null
        if (currentBranchOrNull() != undo.backup.branch) {
            error = strings.toastUndoBranchChanged
            return null
        }
        val head = rewriteHead() ?: return null
        if (!head.equals(undo.postTip, ignoreCase = true)) {
            error = strings.toastUndoHeadChanged
            return null
        }

        var stashed = false
        if (status?.isClean != true) {
            val stash = rewriteGit(
                listOf(
                    "stash",
                    "push",
                    "--include-untracked",
                    "-m",
                    "git-chan: before undo ${undo.backup.operation}",
                ),
            ) ?: return null
            if (!stash.ok) return null
            stashed = true
        }

        val result = rewriteGit(listOf("reset", "--hard", undo.backup.ref)) ?: return null
        if (!result.ok) return null
        lastRewriteUndo = null
        refresh()
        return RewriteUndoOutcome(result.commandLine, stashed)
    }

    /** Помечает rewrite, отменённый кнопкой в toast, в журнале. */
    private fun markRewriteEntryUndone(undo: RewriteUndo) {
        val index = journal.indexOfLast { entry ->
            val action = entry.action as? JournalAction.Rewrite ?: return@indexOfLast false
            entry.state == JournalEntryState.Applied &&
                action.undo.backup.ref == undo.backup.ref &&
                action.undo.postTip.equals(undo.postTip, ignoreCase = true)
        }
        if (index < 0) return
        val entry = journal[index]
        journal[index] = entry.copy(state = JournalEntryState.Undone)
        journalUndoStack.remove(entry.id)
        if (entry.id == lastJournalEntry?.id) journalRedoStack.add(entry.id)
    }

    private fun setJournalEntryState(entryId: Long, state: JournalEntryState) {
        val index = journal.indexOfFirst { it.id == entryId }
        if (index >= 0) journal[index] = journal[index].copy(state = state)
    }

    /**
     * Возвращает ветку на backup-ref переписывания. Если после операции
     * появились локальные изменения, сначала сохраняет их в stash.
     */
    suspend fun undoRewrite(): Boolean {
        val undo = lastRewriteUndo ?: run {
            error = strings.toastUndoUnavailable
            return false
        }

        busy = true
        error = null
        try {
            val outcome = performRewriteUndo(undo) ?: return false
            markRewriteEntryUndone(undo)
            publishToast(
                message = if (outcome.stashed) {
                    strings.toastRewriteUndoneStashed
                } else {
                    strings.toastRewriteUndone
                },
                command = outcome.command,
                recordJournal = false,
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Отменяет последнюю обратимую операцию из единого журнала. */
    suspend fun undoJournal(): Boolean {
        val id = journalUndoStack.lastOrNull() ?: run {
            error = strings.toastUndoUnavailable
            return false
        }
        val entry = journal.firstOrNull { it.id == id && it.state == JournalEntryState.Applied }
            ?: run {
                journalUndoStack.remove(id)
                error = strings.toastUndoUnavailable
                return false
            }
        val action = entry.action ?: run {
            error = strings.toastOperationNotUndoable
            return false
        }

        busy = true
        error = null
        try {
            val (message, command) = when (action) {
                is JournalAction.Rewrite -> {
                    val outcome = performRewriteUndo(action.undo) ?: return false
                    (if (outcome.stashed) {
                        strings.toastRewriteUndoneStashed
                    } else {
                        strings.toastRewriteUndone
                    }) to outcome.command
                }

                is JournalAction.CommitReset -> {
                    if (journalOperationInProgress()) return false
                    if (currentBranchOrNull() != action.branch) {
                        error = strings.toastUndoBranchChanged
                        return false
                    }
                    val head = rewriteHead() ?: return false
                    if (!head.equals(action.afterTip, ignoreCase = true)) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    val result = rewriteGit(listOf("reset", "--soft", action.beforeTip))
                        ?: return false
                    if (!result.ok) return false
                    refresh()
                    strings.toastOperationUndone to result.commandLine
                }

                is JournalAction.Checkout -> {
                    if (journalOperationInProgress()) return false
                    val head = rewriteHead() ?: return false
                    if (!head.equals(action.afterTip, ignoreCase = true)) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    val target = rewriteRead(
                        listOf("rev-parse", "--verify", "${action.beforeRef}^{commit}"),
                    ) ?: return false
                    if (
                        !target.ok ||
                        !target.stdout.trim().equals(action.beforeTip, ignoreCase = true)
                    ) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    val result = rewriteGit(listOf("checkout", action.beforeRef))
                        ?: return false
                    if (!result.ok) return false
                    refresh()
                    strings.toastOperationUndone to result.commandLine
                }
            }

            setJournalEntryState(entry.id, JournalEntryState.Undone)
            journalUndoStack.remove(entry.id)
            journalRedoStack.add(entry.id)
            publishToast(
                message = message,
                command = command,
                recordJournal = false,
            )
            return true
        } finally {
            busy = false
        }
    }

    /** Повторяет последнюю отменённую операцию, если HEAD и ветка не ушли вперёд. */
    suspend fun redoJournal(): Boolean {
        val id = journalRedoStack.lastOrNull() ?: run {
            error = strings.toastRedoUnavailable
            return false
        }
        val entry = journal.firstOrNull { it.id == id && it.state == JournalEntryState.Undone }
            ?: run {
                journalRedoStack.remove(id)
                error = strings.toastRedoUnavailable
                return false
            }
        val action = entry.action ?: run {
            error = strings.toastOperationNotUndoable
            return false
        }

        busy = true
        error = null
        try {
            val (message, command) = when (action) {
                is JournalAction.Rewrite -> {
                    if (journalOperationInProgress()) return false
                    val undo = action.undo
                    if (currentBranchOrNull() != undo.backup.branch) {
                        error = strings.toastUndoBranchChanged
                        return false
                    }
                    val head = rewriteHead() ?: return false
                    if (!head.equals(undo.backup.previousTip, ignoreCase = true)) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    var stashed = false
                    if (status?.isClean != true) {
                        val stash = rewriteGit(
                            listOf(
                                "stash",
                                "push",
                                "--include-untracked",
                                "-m",
                                "git-chan: before redo ${undo.backup.operation}",
                            ),
                        ) ?: return false
                        if (!stash.ok) return false
                        stashed = true
                    }
                    val result = rewriteGit(listOf("reset", "--hard", undo.postTip))
                        ?: return false
                    if (!result.ok) return false
                    lastRewriteUndo = undo
                    refresh()
                    (if (stashed) {
                        strings.toastOperationRedoneStashed
                    } else {
                        strings.toastOperationRedone
                    }) to result.commandLine
                }

                is JournalAction.CommitReset -> {
                    if (journalOperationInProgress()) return false
                    if (currentBranchOrNull() != action.branch) {
                        error = strings.toastUndoBranchChanged
                        return false
                    }
                    val head = rewriteHead() ?: return false
                    if (!head.equals(action.beforeTip, ignoreCase = true)) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    val result = rewriteGit(listOf("reset", "--soft", action.afterTip))
                        ?: return false
                    if (!result.ok) return false
                    refresh()
                    strings.toastOperationRedone to result.commandLine
                }

                is JournalAction.Checkout -> {
                    if (journalOperationInProgress()) return false
                    val head = rewriteHead() ?: return false
                    if (!head.equals(action.beforeTip, ignoreCase = true)) {
                        error = strings.toastUndoHeadChanged
                        return false
                    }
                    val result = rewriteGit(listOf("checkout", action.afterRef))
                        ?: return false
                    if (!result.ok) return false
                    refresh()
                    strings.toastOperationRedone to result.commandLine
                }
            }

            setJournalEntryState(entry.id, JournalEntryState.Applied)
            journalRedoStack.remove(entry.id)
            journalUndoStack.add(entry.id)
            publishToast(
                message = message,
                command = command,
                undo = (action as? JournalAction.Rewrite)?.undo,
                recordJournal = false,
            )
            return true
        } finally {
            busy = false
        }
    }

    suspend fun cherryPickCommit(hash: String, noCommit: Boolean = false): Boolean {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        if (noCommit) {
            return startNoCommit(GitOperationKind.CherryPick, hash, listOf(hash))
        }
        return runCommitAction(
            args = listOf("cherry-pick", hash),
            message = strings.toastCommitCherryPicked,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
    }

    suspend fun cherryPickRange(
        from: String,
        to: String,
        noCommit: Boolean = false,
    ): Boolean = cherryPickRange("$from..$to", noCommit)

    suspend fun cherryPickRange(range: String, noCommit: Boolean = false): Boolean {
        val clean = range.trim()
        if (!validRevisionRange(clean)) {
            error = strings.commitActionInvalidRange
            return false
        }
        if (noCommit) {
            return startNoCommit(GitOperationKind.CherryPick, clean)
        }
        return runCommitAction(
            args = listOf("cherry-pick", clean),
            message = strings.toastCommitCherryPicked,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
    }

    suspend fun revertCommit(hash: String, noCommit: Boolean = false): Boolean {
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        if (noCommit) {
            return startNoCommit(GitOperationKind.Revert, hash, listOf(hash))
        }
        return runCommitAction(
            args = listOf("revert", "--no-edit", hash),
            message = strings.toastCommitReverted,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
    }

    suspend fun revertRange(
        from: String,
        to: String,
        noCommit: Boolean = false,
    ): Boolean = revertRange("$from..$to", noCommit)

    suspend fun revertRange(range: String, noCommit: Boolean = false): Boolean {
        val clean = range.trim()
        if (!validRevisionRange(clean)) {
            error = strings.commitActionInvalidRange
            return false
        }
        if (noCommit) {
            return startNoCommit(GitOperationKind.Revert, clean)
        }
        return runCommitAction(
            args = listOf("revert", "--no-edit", clean),
            message = strings.toastCommitReverted,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
    }

    suspend fun createBranch(name: String, hash: String): Boolean {
        val clean = name.trim()
        if (!validCommitHash(hash) || !validRefName(clean)) {
            error = strings.commitActionInvalidRef
            return false
        }
        return runCommitAction(listOf("branch", clean, hash), strings.toastBranchCreated)
    }

    /** Создаёт ветку от текущего HEAD, не переключаясь на неё. */
    suspend fun createBranchAtHead(name: String): Boolean {
        val clean = name.trim()
        if (!validRefName(clean)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (status?.branch?.oid == null && commits.isEmpty()) {
            error = strings.branchNoCommits
            return false
        }
        return runCommitAction(listOf("branch", clean), strings.toastBranchCreated)
    }

    /** `git branch -m` обновляет и upstream-конфигурацию, если ветка была текущей. */
    suspend fun renameBranch(oldName: String, newName: String): Boolean {
        val target = localBranch(oldName)
        val cleanNew = newName.trim()
        if (target == null) {
            error = strings.branchNotFound
            return false
        }
        if (!validRefName(cleanNew)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (target.shortName == cleanNew) {
            error = strings.branchNameUnchanged
            return false
        }
        if (!validRefFormat(target.name)) return false
        return runCommitAction(
            branchRenameArgs(target.shortName, cleanNew),
            strings.toastBranchRenamed,
        )
    }

    /** По умолчанию безопасное `-d`; force требует явного `-D` в UI. */
    suspend fun deleteLocalBranch(name: String, force: Boolean): Boolean {
        val target = localBranch(name)
        if (target == null) {
            error = strings.branchNotFound
            return false
        }
        if (target.isHead) {
            error = strings.branchCannotDeleteCurrent
            return false
        }
        if (!validRefFormat(target.name)) return false
        return runCommitAction(
            branchDeleteArgs(target.shortName, force),
            strings.toastBranchDeleted,
        )
    }

    suspend fun deleteRemoteBranch(remote: String, branch: String): Boolean {
        val cleanRemote = remote.trim()
        val cleanBranch = branch.trim()
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        if (!validRefName(cleanBranch)) {
            error = strings.commitActionInvalidRef
            return false
        }
        return runCommitAction(
            listOf("push", cleanRemote, "--delete", cleanBranch),
            strings.toastRemoteBranchDeleted,
        )
    }

    suspend fun setBranchUpstream(branch: String, upstream: String): Boolean {
        val local = localBranch(branch)
        val remoteRef = remoteBranch(upstream)
        if (local == null || remoteRef == null) {
            error = strings.branchNotFound
            return false
        }
        if (!validRefFormat(local.name) || !validRefFormat(remoteRef.name)) return false
        val upstreamArg = if (upstream.trim().startsWith("refs/remotes/")) {
            remoteRef.name
        } else {
            remoteRef.shortName
        }
        return runCommitAction(
            branchUpstreamArgs(local.shortName, upstreamArg),
            strings.toastBranchUpstreamSet,
        )
    }

    /** Push без force: создаёт remote-ветку (если её нет) и ставит upstream. */
    suspend fun pushBranch(branch: String, remote: String, remoteBranch: String): Boolean {
        val local = localBranch(branch)
        val cleanRemote = remote.trim()
        val cleanRemoteBranch = remoteBranch.trim().ifBlank { local?.shortName.orEmpty() }
        if (local == null) {
            error = strings.branchNotFound
            return false
        }
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        if (!validRefName(cleanRemoteBranch)) {
            error = strings.commitActionInvalidRef
            return false
        }
        val refspec = if (cleanRemoteBranch == local.shortName) {
            local.shortName
        } else {
            "${local.shortName}:$cleanRemoteBranch"
        }
        return runCommitAction(
            listOf("push", "--set-upstream", cleanRemote, refspec),
            strings.toastBranchPushed,
        )
    }

    /** Общий push для текущей/другой ветки, тегов, --all/--mirror, удаления и refspec. */
    suspend fun push(remote: String, options: PushOptions): Boolean {
        val cleanRemote = remote.trim()
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        val current = refs.firstOrNull {
            it.kind == RefKind.LocalBranch && it.isHead
        }?.shortName
        val normalized = when (options.mode) {
            PushMode.CurrentBranch -> {
                if (current == null) {
                    error = strings.pushNoCurrentBranch
                    return false
                }
                options.copy(source = current)
            }

            PushMode.AnotherBranch -> {
                if (current == null) {
                    error = strings.pushNoCurrentBranch
                    return false
                }
                val destination = options.destination.trim()
                if (!validRefName(destination)) {
                    error = strings.commitActionInvalidRef
                    return false
                }
                options.copy(source = current, destination = destination)
            }

            PushMode.DeleteBranch -> {
                val branch = options.branch.trim()
                if (!validRefName(branch)) {
                    error = strings.commitActionInvalidRef
                    return false
                }
                options.copy(branch = branch, force = PushForce.None)
            }

            PushMode.Refspec -> {
                val refspec = options.refspec.trim()
                if (!validPushRefspec(refspec)) {
                    error = strings.pushInvalidRefspec
                    return false
                }
                options.copy(refspec = refspec)
            }

            PushMode.AllTags, PushMode.AllBranches, PushMode.Mirror -> options
        }
        val message = when (options.mode) {
            PushMode.AllTags -> strings.toastTagsPushed
            PushMode.AllBranches -> strings.toastAllBranchesPushed
            PushMode.Mirror -> strings.toastMirrorPushed
            PushMode.DeleteBranch -> strings.toastRemoteBranchDeleted
            else -> strings.toastPushed
        }
        return runCommitAction(pushArgs(cleanRemote, normalized), message)
    }

    /** Pull с явным режимом; remote и ветка не берутся из pull.* конфигурации. */
    suspend fun pull(
        remote: String,
        branch: String,
        mode: PullMode,
        verifySignatures: Boolean = false,
    ): Boolean {
        val cleanRemote = remote.trim()
        val cleanBranch = branch.trim()
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        if (!validRefName(cleanBranch)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (refs.none { it.kind == RefKind.LocalBranch && it.isHead }) {
            error = strings.pullNoCurrentBranch
            return false
        }

        val result = runGit(
            args = pullArgs(cleanRemote, cleanBranch, mode, verifySignatures),
            environment = NONINTERACTIVE_OPERATION_ENV,
        ) ?: return false
        notify(result, strings.toastPulled)
        val failure = if (result.ok) null else error
        if (result.ok) {
            selectedHunks = emptySet()
            clearMergeEditor()
        }
        // При конфликте git возвращает non-zero, но операция уже начата;
        // refresh нужен, чтобы показать merge/rebase editor и продолжить/прервать.
        refresh()
        if (!result.ok) error = failure
        return result.ok
    }

    /**
     * Переименование remote-ветки — две операции: push нового ref из remote-tracking
     * и удаление старого. Если удаление упадёт, новая ветка уже останется на remote.
     */
    suspend fun renameRemoteBranch(remote: String, oldName: String, newName: String): Boolean {
        val cleanRemote = remote.trim()
        val cleanOld = oldName.trim()
        val cleanNew = newName.trim()
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        val source = remoteBranch("$cleanRemote/$cleanOld")
        if (source == null) {
            error = strings.branchNotFound
            return false
        }
        if (!validRefName(cleanNew)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (cleanOld == cleanNew) {
            error = strings.branchNameUnchanged
            return false
        }

        val push = runGit(
            listOf("push", cleanRemote, "${source.name}:refs/heads/$cleanNew"),
        ) ?: return false
        notify(push, strings.toastRemoteBranchRenamed)
        if (!push.ok) return false

        val delete = runGit(listOf("push", cleanRemote, "--delete", cleanOld)) ?: return false
        notify(delete, strings.toastRemoteBranchRenamed)
        if (delete.ok) {
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
        } else {
            refresh()
            error = describe(delete)
        }
        return delete.ok
    }

    /** Сравнивает две существующие локальные/remote-ветки и читает уникальные коммиты. */
    suspend fun compareBranches(firstInput: String, secondInput: String): Boolean {
        val first = compareRef(firstInput)
        val second = compareRef(secondInput)
        if (first == null || second == null) {
            branchCompareError = strings.branchNotFound
            return false
        }
        val request = ++branchCompareRequest
        branchComparison = null
        branchCompareLoading = true
        branchCompareError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                backend.compareBranches(repository, first, second, maxCommits = 100)
            }
            if (request == branchCompareRequest) branchComparison = loaded
            return true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == branchCompareRequest) {
                branchCompareError = displayError(error)
            }
            return false
        } finally {
            if (request == branchCompareRequest) branchCompareLoading = false
        }
    }

    fun clearBranchComparison() {
        branchCompareRequest++
        branchComparison = null
        branchCompareLoading = false
        branchCompareError = null
    }

    fun dismissBranchCompareError() {
        branchCompareError = null
    }

    suspend fun createTag(
        name: String,
        message: String,
        hash: String,
        signed: Boolean = false,
    ): Boolean {
        val clean = name.trim()
        if (!validCommitHash(hash) || !validRefName(clean)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (signed && message.isBlank()) {
            error = strings.tagSignNeedsMessage
            return false
        }
        return runCommitAction(tagArgs(clean, message, signed, hash), strings.toastTagCreated)
    }

    /** Создаёт lightweight/annotated/signed тег на текущем HEAD без checkout. */
    suspend fun createTagAtHead(name: String, message: String, signed: Boolean): Boolean {
        val clean = name.trim()
        if (!validRefName(clean)) {
            error = strings.commitActionInvalidRef
            return false
        }
        if (signed && message.isBlank()) {
            error = strings.tagSignNeedsMessage
            return false
        }
        if (status?.branch?.oid == null && commits.isEmpty()) {
            error = strings.tagNoCommits
            return false
        }
        return runCommitAction(tagArgs(clean, message, signed, null), strings.toastTagCreated)
    }

    suspend fun deleteLocalTag(name: String): Boolean {
        val tag = localTag(name)
        if (tag == null) {
            error = strings.tagNotFound
            return false
        }
        if (!validRefFormat(tag.name)) return false
        return runCommitAction(tagDeleteArgs(tag.shortName), strings.toastTagDeleted)
    }

    suspend fun deleteRemoteTag(remote: String, name: String): Boolean {
        val cleanRemote = remote.trim()
        val tag = localTag(name)
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        if (tag == null) {
            error = strings.tagNotFound
            return false
        }
        return runCommitAction(
            listOf("push", cleanRemote, "--delete", "refs/tags/${tag.shortName}"),
            strings.toastRemoteTagDeleted,
        )
    }

    suspend fun pushTag(remote: String, name: String): Boolean {
        val cleanRemote = remote.trim()
        val tag = localTag(name)
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        if (tag == null) {
            error = strings.tagNotFound
            return false
        }
        return runCommitAction(
            listOf("push", cleanRemote, "refs/tags/${tag.shortName}:refs/tags/${tag.shortName}"),
            strings.toastTagPushed,
        )
    }

    suspend fun pushAllTags(remote: String): Boolean {
        val cleanRemote = remote.trim()
        if (cleanRemote !in remotes) {
            error = strings.branchInvalidRemote
            return false
        }
        return runCommitAction(
            listOf("push", cleanRemote, "--tags"),
            strings.toastTagsPushed,
        )
    }

    fun remoteUrl(name: String): String = remoteUrls[name.trim()].orEmpty()

    /**
     * Provider-agnostic identity for a remote. Reads Git config only: no API,
     * no permission guess, and no network request. Multiple URL values and
     * `url.<base>.insteadOf` rewrites are included; missing pieces stay UNKNOWN.
     */
    suspend fun loadRemoteProfile(name: String): RemoteProfile? {
        val clean = name.trim()
        if (clean !in remotes) {
            error = strings.remoteNotFound
            return null
        }
        return try {
            val entries = withContext(Dispatchers.IO) { backend.configEntries(repository) }
            val rules = gitInsteadOfRules(entries)
            fun values(suffix: String): List<String> = entries.asSequence()
                .filter { it.key.equals("remote.$clean.$suffix", ignoreCase = true) }
                .map { it.value.trim() }
                .filter { it.isNotEmpty() }
                .toList()

            var fetchUrls = parseRemoteUrls(values("url"), rules)
            val pushUrls = parseRemoteUrls(values("pushurl"), rules)
            if (fetchUrls.isEmpty()) {
                remoteUrls[clean]?.trim()?.takeIf { it.isNotEmpty() }?.let { fallback ->
                    fetchUrls = listOf(parseRemoteUrl(fallback, rules))
                }
            }
            if (fetchUrls.isEmpty() && pushUrls.isEmpty()) {
                error = strings.remoteNotFound
                return null
            }
            RemoteProfile(
                name = clean,
                fetchUrls = fetchUrls,
                pushUrls = pushUrls,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = displayError(failure)
            null
        }
    }

    /** Настроенные remote-tracking ветки выбранного remote без символической HEAD. */
    fun remoteBranches(name: String): List<GitRef> {
        val clean = name.trim()
        val prefix = "refs/remotes/$clean/"
        return refs.filter {
            it.kind == RefKind.RemoteBranch &&
                !it.isSymbolic &&
                it.name.startsWith(prefix)
        }
    }

    /** Текущий refs/remotes/<remote>/HEAD в коротком виде, например origin/main. */
    fun remoteHead(name: String): String? {
        val clean = name.trim()
        val ref = refs.firstOrNull {
            it.kind == RefKind.RemoteBranch && it.name == "refs/remotes/$clean/HEAD"
        } ?: return null
        val target = ref.symbolicTarget ?: return null
        val prefix = "refs/remotes/$clean/"
        return if (target.startsWith(prefix)) target.removePrefix(prefix) else target
    }

    suspend fun addRemote(name: String, url: String): Boolean {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (!validRemoteName(cleanName)) {
            error = strings.remoteInvalidName
            return false
        }
        if (!validRemoteUrl(cleanUrl)) {
            error = strings.remoteInvalidUrl
            return false
        }
        if (cleanName in remotes) {
            error = strings.remoteAlreadyExists
            return false
        }
        return runCommitAction(
            listOf("remote", "add", cleanName, cleanUrl),
            strings.toastRemoteAdded,
        )
    }

    suspend fun removeRemote(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        return runCommitAction(
            listOf("remote", "remove", cleanName),
            strings.toastRemoteRemoved,
        )
    }

    suspend fun editRemoteUrl(name: String, url: String): Boolean {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        if (!validRemoteUrl(cleanUrl)) {
            error = strings.remoteInvalidUrl
            return false
        }
        return runCommitAction(
            listOf("remote", "set-url", cleanName, cleanUrl),
            strings.toastRemoteUrlUpdated,
        )
    }

    /** Fetch только выбранного remote; флаги соответствуют настройкам UI. */
    suspend fun fetchRemote(remote: String, options: FetchOptions): Boolean {
        val cleanRemote = remote.trim()
        if (cleanRemote !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        if (!validFetchDepth(options.depth)) {
            error = strings.remoteInvalidDepth
            return false
        }
        if (!validFetchFilter(options.filter)) {
            error = strings.remoteInvalidFilter
            return false
        }
        val normalized = options.copy(
            depth = options.depth.trim(),
            filter = options.filter.trim(),
        )
        return runCommitAction(fetchArgs(cleanRemote, normalized), strings.toastFetched)
    }

    suspend fun setRemoteHeadAuto(remote: String): Boolean {
        val cleanRemote = remote.trim()
        if (cleanRemote !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        return runCommitAction(
            listOf("remote", "set-head", cleanRemote, "--auto"),
            strings.toastRemoteHeadSet,
        )
    }

    suspend fun setRemoteHead(remote: String, branch: String): Boolean {
        val cleanRemote = remote.trim()
        var cleanBranch = branch.trim()
        if (cleanRemote !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        val prefix = "refs/remotes/$cleanRemote/"
        if (cleanBranch.startsWith(prefix)) cleanBranch = cleanBranch.removePrefix(prefix)
        if (!validRefName(cleanBranch)) {
            error = strings.commitActionInvalidRef
            return false
        }
        return runCommitAction(
            listOf("remote", "set-head", cleanRemote, cleanBranch),
            strings.toastRemoteHeadSet,
        )
    }

    suspend fun deleteRemoteHead(remote: String): Boolean {
        val cleanRemote = remote.trim()
        if (cleanRemote !in remotes) {
            error = strings.remoteNotFound
            return false
        }
        return runCommitAction(
            listOf("remote", "set-head", cleanRemote, "--delete"),
            strings.toastRemoteHeadDeleted,
        )
    }

    /** Читает patch/mbox с ограничением, чтобы огромный файл не попал в UI целиком. */
    suspend fun loadPatchMailbox(path: String): Boolean {
        val clean = path.trim()
        if (!validPatchPath(clean)) {
            patchMailboxError = strings.patchInvalidPath
            return false
        }
        patchMailboxLoading = true
        patchMailboxError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                val file = resolvePatchPath(clean)
                if (!file.isFile) throw IllegalStateException(strings.patchFileNotFound)
                val truncated = file.length() > MAX_MAILBOX_BYTES
                val bytes = file.inputStream().use { input ->
                    input.readNBytes(MAX_MAILBOX_BYTES)
                }
                PatchMailboxParser.parse(
                    raw = String(bytes, Charsets.UTF_8),
                    path = file.path,
                    truncated = truncated,
                )
            }
            patchMailbox = loaded
            return true
        } catch (error: Exception) {
            patchMailbox = null
            patchMailboxError = displayError(error)
            return false
        } finally {
            patchMailboxLoading = false
        }
    }

    fun dismissPatchMailboxError() {
        patchMailboxError = null
    }

    /**
     * Экспортирует выбранные коммиты или диапазон в файлы/mbox.
     * previewOnly и outputFile используют `--stdout`, чтобы получить один mailbox.
     */
    suspend fun exportPatches(options: PatchExportOptions): PatchExportResult? {
        val hashes = options.hashes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val range = options.range.trim()
        if (hashes.isEmpty() && range.isEmpty()) {
            error = strings.patchNoSelection
            return null
        }
        if (hashes.isNotEmpty() && range.isNotEmpty()) {
            error = strings.patchSelectionAndRange
            return null
        }
        if (range.isNotEmpty() &&
            (range.startsWith("-") || range.any { it == '\n' || it == '\r' || it == '\u0000' })
        ) {
            error = strings.patchInvalidRange
            return null
        }

        val orderedHashes = if (hashes.isEmpty()) {
            emptyList()
        } else {
            val indexes = commits.mapIndexed { index, commit -> commit.hash to index }.toMap()
            if (hashes.any { it !in indexes }) {
                error = strings.patchUnknownCommit
                return null
            }
            hashes.sortedByDescending { indexes[it] ?: Int.MAX_VALUE }
        }

        val stdout = options.previewOnly || options.outputFile.isNotBlank()
        val outputFile = options.outputFile.trim().takeIf { it.isNotEmpty() }?.let(::resolvePatchPath)
        val outputDir = if (!stdout) {
            val requested = options.outputDir.trim()
            val dir = if (requested.isEmpty()) {
                File(repository.gitDir, "chan-patches")
            } else {
                resolvePatchPath(requested)
            }
            if (!dir.exists() && !dir.mkdirs()) {
                error = strings.patchOutputDirFailed
                return null
            }
            dir
        } else {
            null
        }

        val mailbox = StringBuilder()
        var mailboxBytes = 0L
        var lastResult: GitResult? = null
        val revisions: List<List<String>> = if (range.isNotEmpty()) {
            listOf(listOf(range))
        } else {
            orderedHashes.map { listOf(it) }
        }

        revisions.forEachIndexed { index, revisionList ->
            val args = formatPatchArgs(
                revisions = revisionList,
                outputDir = outputDir?.path,
                stdout = stdout,
                single = hashes.isNotEmpty(),
                // Cover letter has no useful meaning when each selected commit is
                // formatted by a separate `format-patch -1` call.
                coverLetter = options.coverLetter && hashes.isEmpty(),
                numbered = options.numbered,
                signoff = options.signoff,
                binary = options.binary,
                subjectPrefix = options.subjectPrefix,
                // Selected commits are formatted one by one; keep their file
                // numbering increasing so equal subjects cannot overwrite files.
                startNumber = if (hashes.isNotEmpty()) index + 1 else null,
            )
            val result = runGit(args) ?: return null
            lastResult = result
            if (result.outputTruncated) {
                error = strings.repoFileTooLarge
                return null
            }
            if (!result.ok) return null
            if (stdout) {
                val separator = if (mailbox.isNotEmpty() && !mailbox.endsWith("\n")) "\n" else ""
                val chunkBytes = result.stdout.toByteArray(Charsets.UTF_8).size.toLong()
                val separatorBytes = separator.toByteArray(Charsets.UTF_8).size.toLong()
                if (mailboxBytes + separatorBytes + chunkBytes > MAX_PATCH_EXPORT_BYTES) {
                    error = strings.repoFileTooLarge
                    return null
                }
                mailbox.append(separator)
                mailbox.append(result.stdout)
                mailboxBytes += separatorBytes + chunkBytes
            }
        }

        val mailboxText = mailbox.toString()
        var writtenFile = ""
        if (outputFile != null) {
            try {
                outputFile.parentFile?.mkdirs()
                withContext(Dispatchers.IO) { outputFile.writeText(mailboxText) }
                writtenFile = outputFile.path
                patchMailbox = PatchMailboxParser.parse(mailboxText, writtenFile)
            } catch (error: Exception) {
                this.error = displayError(error)
                return null
            }
        }
        val files = outputDir?.let(::patchFiles).orEmpty()
        lastResult?.let { result ->
            if (files.isNotEmpty() || writtenFile.isNotEmpty()) {
                notify(result, strings.toastPatchesExported)
            }
        }
        return PatchExportResult(
            mailbox = mailboxText,
            outputFile = writtenFile,
            outputDir = outputDir?.path.orEmpty(),
            files = files,
        )
    }

    suspend fun applyPatchFile(
        path: String,
        check: Boolean = false,
        threeWay: Boolean = false,
    ): Boolean {
        val clean = path.trim()
        if (!validPatchPath(clean)) {
            error = strings.patchInvalidPath
            return false
        }
        val file = resolvePatchPath(clean)
        if (!file.isFile) {
            error = strings.patchFileNotFound
            return false
        }
        return runCommitAction(
            applyPatchArgs(file.path, check, threeWay),
            if (check) strings.toastPatchCheckPassed else strings.toastPatchApplied,
        )
    }

    suspend fun applyMailbox(
        path: String,
        threeWay: Boolean = false,
        signoff: Boolean = false,
    ): Boolean {
        val clean = path.trim()
        if (!validPatchPath(clean)) {
            error = strings.patchInvalidPath
            return false
        }
        val file = resolvePatchPath(clean)
        if (!file.isFile) {
            error = strings.patchFileNotFound
            return false
        }
        return runCommitAction(
            amArgs(file.path, threeWay, signoff),
            strings.toastMailboxApplied,
        )
    }

    private fun resolvePatchPath(raw: String): File {
        val file = File(raw)
        return if (file.isAbsolute) file else File(repository.root, raw)
    }

    private fun patchFiles(dir: File): List<String> =
        dir.listFiles().orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name }
            .map { it.path }

    /** Выбранный worktree по пути; относительный путь считается от корня репозитория. */
    private fun worktreeTarget(path: String): Worktree? {
        val clean = path.trim()
        if (clean.isEmpty()) return null
        return worktrees.firstOrNull { sameWorktreePath(it.path, clean) }
    }

    private fun sameWorktreePath(first: String, second: String): Boolean {
        val firstPath = runCatching {
            File(first).let { if (it.isAbsolute) it else File(repository.root, first) }.canonicalFile
        }.getOrElse { File(first).absoluteFile.normalize() }
        val secondPath = runCatching {
            File(second).let { if (it.isAbsolute) it else File(repository.root, second) }.canonicalFile
        }.getOrElse { File(second).absoluteFile.normalize() }
        return firstPath.path.equals(
            secondPath.path,
            ignoreCase = System.getProperty("os.name").orEmpty().lowercase().contains("win"),
        )
    }

    private fun validWorktreeBase(value: String): Boolean {
        val clean = value.trim()
        return clean.isNotEmpty() &&
            !clean.startsWith("-") &&
            clean.none { it == '\n' || it == '\r' || it == '\u0000' }
    }

    suspend fun addWorktree(options: WorktreeAddOptions): Boolean {
        val path = options.path.trim()
        if (!validWorktreePath(path)) {
            error = strings.worktreeInvalidPath
            return false
        }
        if (sameWorktreePath(path, repository.root) || worktreeTarget(path) != null) {
            error = strings.worktreeAlreadyExists
            return false
        }
        val branch = options.branch.trim()
        if (options.createBranch && !validRefName(branch)) {
            error = strings.worktreeInvalidBranch
            return false
        }
        if (!options.createBranch && branch.isNotEmpty() && !validRefName(branch)) {
            error = strings.worktreeInvalidBranch
            return false
        }
        if (options.base.isNotBlank() && !validWorktreeBase(options.base)) {
            error = strings.worktreeInvalidBase
            return false
        }
        return runCommitAction(
            worktreeAddArgs(options.copy(path = path, branch = branch)),
            strings.toastWorktreeAdded,
        )
    }

    suspend fun removeWorktree(path: String, force: Boolean = false): Boolean {
        val target = worktreeTarget(path)
        if (target == null) {
            error = strings.worktreeNotFound
            return false
        }
        if (sameWorktreePath(target.path, repository.root)) {
            error = strings.worktreeCannotRemoveCurrent
            return false
        }
        if (target.isBare) {
            error = strings.worktreeCannotRemoveBare
            return false
        }
        return runCommitAction(
            worktreeRemoveArgs(target.path, force),
            strings.toastWorktreeRemoved,
        )
    }

    suspend fun lockWorktree(path: String, reason: String = ""): Boolean {
        val target = worktreeTarget(path)
        if (target == null) {
            error = strings.worktreeNotFound
            return false
        }
        if (target.isLocked) {
            error = strings.worktreeAlreadyLocked
            return false
        }
        return runCommitAction(
            worktreeLockArgs(target.path, reason),
            strings.toastWorktreeLocked,
        )
    }

    suspend fun unlockWorktree(path: String): Boolean {
        val target = worktreeTarget(path)
        if (target == null) {
            error = strings.worktreeNotFound
            return false
        }
        if (!target.isLocked) {
            error = strings.worktreeNotLocked
            return false
        }
        return runCommitAction(
            worktreeUnlockArgs(target.path),
            strings.toastWorktreeUnlocked,
        )
    }

    /** Переключает или создаёт ветку непосредственно в выбранном worktree. */
    suspend fun branchInWorktree(
        path: String,
        branch: String,
        create: Boolean = true,
        base: String = "",
    ): Boolean {
        val target = worktreeTarget(path)
        if (target == null) {
            error = strings.worktreeNotFound
            return false
        }
        if (target.isBare) {
            error = strings.worktreeBranchBare
            return false
        }
        val cleanBranch = branch.trim()
        if (!validRefName(cleanBranch)) {
            error = strings.worktreeInvalidBranch
            return false
        }
        if (base.isNotBlank() && !validWorktreeBase(base)) {
            error = strings.worktreeInvalidBase
            return false
        }
        return runCommitAction(
            worktreeSwitchArgs(target.path, cleanBranch, create, base),
            strings.toastWorktreeBranchChanged,
        )
    }

    /** Сухой прогон `worktree prune`; возвращает вывод git или null при ошибке. */
    suspend fun previewWorktreePrune(): String? {
        val result = runGit(worktreePruneArgs(dryRun = true)) ?: return null
        if (!result.ok) return null
        // prune --verbose печатает список в stderr, а не в stdout.
        val output = listOf(result.stdout.trim(), result.stderr.trim())
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        return output.ifBlank { strings.worktreePruneNothing }
    }

    suspend fun pruneWorktrees(): Boolean = runCommitAction(
        worktreePruneArgs(dryRun = false),
        strings.toastWorktreesPruned,
    )

    /** Explicit selected path, or null meaning "all submodules". */
    private fun submoduleTarget(path: String?): String? {
        val clean = path?.trim().orEmpty()
        return if (clean.isEmpty()) null else submodules.firstOrNull { it.path == clean }?.path
    }

    private fun requireSubmodules(): Boolean {
        if (submodules.isNotEmpty()) return true
        error = strings.submoduleNone
        return false
    }

    suspend fun initSubmodules(path: String? = null): Boolean {
        if (!requireSubmodules()) return false
        val target = submoduleTarget(path)
        if (path?.isNotBlank() == true && target == null) {
            error = strings.submoduleNotFound
            return false
        }
        return runCommitAction(
            submoduleInitArgs(target),
            strings.toastSubmoduleInitialized,
        )
    }

    suspend fun updateSubmodules(
        path: String? = null,
        options: SubmoduleUpdateOptions = SubmoduleUpdateOptions(),
    ): Boolean {
        if (!requireSubmodules()) return false
        val target = submoduleTarget(path)
        if (path?.isNotBlank() == true && target == null) {
            error = strings.submoduleNotFound
            return false
        }
        return runCommitAction(
            submoduleUpdateArgs(target, options),
            strings.toastSubmodulesUpdated,
        )
    }

    suspend fun syncSubmodules(path: String? = null, recursive: Boolean = false): Boolean {
        if (!requireSubmodules()) return false
        val target = submoduleTarget(path)
        if (path?.isNotBlank() == true && target == null) {
            error = strings.submoduleNotFound
            return false
        }
        return runCommitAction(
            submoduleSyncArgs(target, recursive),
            strings.toastSubmodulesSynced,
        )
    }

    suspend fun addSubmodule(url: String, path: String): Boolean {
        val cleanUrl = url.trim()
        val cleanPath = path.trim()
        if (!validSubmoduleUrl(cleanUrl)) {
            error = strings.submoduleInvalidUrl
            return false
        }
        if (!validSubmodulePath(cleanPath)) {
            error = strings.submoduleInvalidPath
            return false
        }
        if (submodules.any { it.path == cleanPath }) {
            error = strings.submoduleAlreadyExists
            return false
        }
        return runCommitAction(
            submoduleAddArgs(cleanUrl, cleanPath),
            strings.toastSubmoduleAdded,
        )
    }

    /**
     * deinit then git rm. If the second step fails, the deinit has already changed
     * the worktree/config; refresh so the manager shows that partial state.
     */
    suspend fun removeSubmodule(path: String): Boolean {
        if (!requireSubmodules()) return false
        val target = submoduleTarget(path)
        if (target == null) {
            error = strings.submoduleNotFound
            return false
        }
        val deinit = runGit(submoduleDeinitArgs(target)) ?: return false
        if (!deinit.ok) {
            val failure = error
            notify(deinit, strings.toastFailed)
            refresh()
            error = failure
            return false
        }
        val removed = runGit(submoduleRemoveArgs(target)) ?: return false
        val failure = if (removed.ok) null else error
        notify(removed, strings.toastSubmoduleRemoved)
        refresh()
        if (!removed.ok) error = failure
        return removed.ok
    }

    /** Список hooks из текущего core.hooksPath и содержимое выбранного файла. */
    suspend fun loadHookDetail(name: String) {
        val entry = hookEntry(name)
        if (entry == null) {
            hookDetail = null
            hookDetailError = strings.hookNotFound
            return
        }
        val request = ++hookDetailRequest
        hookDetailLoading = true
        hookDetailError = null
        try {
            val detail = withContext(Dispatchers.IO) {
                val file = RepositoryPaths.existingExternalFile(File(entry.path))
                val truncated = Files.size(file) > MAX_HOOK_BYTES
                val bytes = Files.newInputStream(
                    file,
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS,
                ).use { input -> input.readNBytes(MAX_HOOK_BYTES) }
                HookDetail(
                    name = entry.name,
                    path = file.toString(),
                    content = bytes.toString(Charsets.UTF_8),
                    truncated = truncated,
                )
            }
            if (request == hookDetailRequest) hookDetail = detail
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == hookDetailRequest) {
                hookDetail = null
                hookDetailError = pathError(error)
            }
        } finally {
            if (request == hookDetailRequest) hookDetailLoading = false
        }
    }

    fun dismissHookDetailError() {
        hookDetailError = null
    }

    suspend fun saveHook(
        name: String,
        content: String,
        allowExternal: Boolean = false,
    ): Boolean {
        val entry = hookEntry(name)
        if (entry == null) {
            error = strings.hookNotFound
            return false
        }
        if (!allowExternal && hooksDirectoryRequiresConfirmation(File(entry.path).parentFile)) {
            error = strings.hookPathConfirmationRequired
            return false
        }
        if (content.toByteArray(Charsets.UTF_8).size > MAX_HOOK_BYTES) {
            error = strings.hookTooLarge
            return false
        }
        return try {
            withContext(Dispatchers.IO) {
                val file = RepositoryPaths.writeExternalFile(File(entry.path), content).toFile()
                if (entry.enabled && !entry.sample && !entry.disabled) {
                    file.setExecutable(true)
                }
            }
            refresh()
            loadHookDetail(entry.name)
            true
        } catch (error: Exception) {
            this.error = pathError(error)
            false
        }
    }

    /**
     * Активный hook отключается переименованием в `<name>.disabled`;
     * при включении файл возвращается на место. Пример `.sample` при
     * включении копируется в активное имя, поэтому его можно безопасно
     * редактировать как шаблон.
     */
    suspend fun setHookEnabled(
        name: String,
        enabled: Boolean,
        allowExternal: Boolean = false,
    ): Boolean {
        val entry = hookEntry(name)
        if (entry == null) {
            error = strings.hookNotFound
            return false
        }
        if (!allowExternal && hooksDirectoryRequiresConfirmation(File(entry.path).parentFile)) {
            error = strings.hookPathConfirmationRequired
            return false
        }
        if (entry.enabled == enabled) return true
        val directory = File(hooks.directory)
        return try {
            withContext(Dispatchers.IO) {
                val active = File(directory, entry.name)
                val disabled = File(directory, entry.name + ".disabled")
                when {
                    enabled && entry.sample -> {
                        if (active.exists()) throw IllegalStateException(strings.hookFileExists)
                        RepositoryPaths.copyExternalFile(File(entry.path), active)
                        active.setExecutable(true)
                    }

                    enabled && entry.disabled -> {
                        if (active.exists()) throw IllegalStateException(strings.hookFileExists)
                        moveHookFile(File(entry.path), active)
                        active.setExecutable(true)
                    }

                    !enabled && entry.enabled -> {
                        if (disabled.exists()) throw IllegalStateException(strings.hookFileExists)
                        moveHookFile(File(entry.path), disabled)
                    }

                    else -> throw IllegalStateException(strings.hookNotFound)
                }
            }
            refresh()
            if (hookDetail?.name == entry.name) loadHookDetail(entry.name)
            true
        } catch (error: Exception) {
            this.error = pathError(error)
            false
        }
    }

    private fun moveHookFile(source: File, target: File) {
        if (target.exists()) throw IllegalStateException(strings.hookFileExists)
        RepositoryPaths.copyExternalFile(source, target, move = true)
    }

    /** Устанавливает локальный core.hooksPath; пустая строка возвращает стандартный каталог. */
    suspend fun setHooksPath(path: String, allowExternal: Boolean = false): Boolean {
        val clean = path.trim()
        if (clean.startsWith("-") || clean.any { it == '\n' || it == '\r' || it == '\u0000' }) {
            error = strings.hookPathInvalid
            return false
        }
        if (clean == hooks.configuredPath) return true
        val target = hooksDirectory(clean)
        if (clean.isNotEmpty()) {
            if (!allowExternal && hooksDirectoryRequiresConfirmation(target)) {
                error = strings.hookPathConfirmationRequired
                return false
            }
            try {
                RepositoryPaths.checkExternalPath(target)
            } catch (error: Exception) {
                this.error = pathError(error)
                return false
            }
        }
        if (clean.isEmpty()) {
            val current = withContext(Dispatchers.IO) {
                runCatching {
                    backend.run(
                        repository.root,
                        listOf("config", "--get", "core.hooksPath"),
                        timeoutSeconds = 30,
                    )
                }.getOrNull()
            }
            if (current?.ok != true || current.stdout.trim().isEmpty()) {
                refresh()
                return true
            }
            val result = runGit(listOf("config", "--unset", "core.hooksPath")) ?: return false
            notify(result, strings.toastHooksPathCleared)
            if (result.ok) refresh()
            return result.ok
        }
        val result = runGit(listOf("config", "core.hooksPath", clean)) ?: return false
        notify(result, strings.toastHooksPathSet)
        if (result.ok) refresh()
        return result.ok
    }

    suspend fun resetHooksPath(): Boolean = setHooksPath("")

    fun clearHookOutput() {
        hookOutput = null
    }

    private fun loadHooksState(): HooksState {
        val config = runCatching {
            backend.run(
                repository.root,
                listOf("config", "--get", "core.hooksPath"),
                timeoutSeconds = 30,
            )
        }.getOrNull()
        val configured = if (config?.ok == true) config.stdout.trim() else ""
        val directory = hooksDirectory(configured)
        val safe = runCatching { RepositoryPaths.checkExternalPath(directory) }.isSuccess
        return HooksState(
            directory = directory.path,
            configuredPath = configured,
            entries = if (safe) readHookEntries(directory) else emptyList(),
            outsideCommonDir = hooksOutsideCommonDir(directory),
            outsideHooksDir = hooksDirectoryRequiresConfirmation(directory),
        )
    }

    /** Resolves core.hooksPath exactly as loadHooksState does. */
    private fun hooksDirectory(configured: String): File {
        if (configured.isBlank()) return File(repository.commonDir, "hooks")
        val requested = File(configured)
        return if (requested.isAbsolute) requested else File(repository.root, requested.path)
    }

    private fun hooksOutsideCommonDir(directory: File): Boolean {
        val common = File(repository.commonDir).toPath().toAbsolutePath().normalize()
        val candidate = directory.toPath().toAbsolutePath().normalize()
        return !candidate.startsWith(common)
    }

    private fun hooksDirectoryRequiresConfirmation(directory: File?): Boolean {
        if (directory == null) return true
        val default = File(repository.commonDir, "hooks").toPath().toAbsolutePath().normalize()
        val candidate = directory.toPath().toAbsolutePath().normalize()
        return candidate != default
    }

    /** True when a non-empty core.hooksPath would resolve outside commonDir. */
    fun isExternalHooksPath(path: String): Boolean {
        val clean = path.trim()
        return clean.isNotEmpty() && hooksOutsideCommonDir(hooksDirectory(clean))
    }

    /** True when setting this path would leave the default commonDir/hooks directory. */
    fun hooksPathRequiresConfirmation(path: String): Boolean {
        val clean = path.trim()
        return clean.isNotEmpty() && hooksDirectoryRequiresConfirmation(hooksDirectory(clean))
    }

    private fun readHookEntries(directory: File): List<HookEntry> {
        if (!directory.isDirectory) return emptyList()
        val groups = LinkedHashMap<String, MutableList<File>>()
        directory.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { file ->
                var base = file.name
                if (base.endsWith(".disabled")) base = base.removeSuffix(".disabled")
                if (base.endsWith(".sample")) base = base.removeSuffix(".sample")
                if (!validHookName(base)) return@forEach
                groups.getOrPut(base) { ArrayList() }.add(file)
            }
        return groups.map { (base, files) ->
            val active = files.firstOrNull {
                !it.name.endsWith(".disabled") && !it.name.endsWith(".sample")
            }
            val disabledFile = files.firstOrNull { it.name.endsWith(".disabled") }
            val sampleFile = files.firstOrNull { it.name.endsWith(".sample") }
            val selected = active ?: disabledFile ?: sampleFile ?: files.first()
            HookEntry(
                name = base,
                fileName = selected.name,
                path = selected.path,
                enabled = active != null,
                sample = active == null && disabledFile == null && sampleFile != null,
                disabled = active == null && disabledFile != null,
                executable = selected.canExecute(),
                size = selected.length(),
            )
        }.sortedBy { it.name }
    }

    private fun hookEntry(name: String): HookEntry? =
        hooks.entries.firstOrNull { it.name == name || it.fileName == name }

    /**
     * Запускает bisect с известными bad/good. Оба ref обязательны: иначе
     * состояние нельзя показать как полноценный диапазон поиска.
     */
    suspend fun startBisect(bad: String, good: String): Boolean {
        if (repository.workTree == null) {
            error = strings.bisectBare
            return false
        }
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (operation != null) {
            error = if (operation?.kind == GitOperationKind.Bisect) {
                strings.bisectAlreadyActive
            } else {
                strings.repoOperationInProgress
            }
            return false
        }
        val badRef = bad.trim()
        val goodRef = good.trim()
        if (!validBisectRef(badRef) || !validBisectRef(goodRef)) {
            error = strings.bisectInvalidRef
            return false
        }
        if (badRef == goodRef) {
            error = strings.bisectSameRef
            return false
        }
        val started = runCommitAction(
            args = bisectStartArgs(badRef, goodRef),
            message = strings.toastBisectStarted,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
        if (started) {
            bisectAutoResult = null
            bisectAutoLog = ""
        }
        return started
    }

    /** Запускает bisect при необходимости и помечает выбранный коммит. */
    suspend fun markBisect(hash: String, good: Boolean): Boolean {
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (!validCommitHash(hash)) {
            error = strings.commitActionInvalidHash
            return false
        }
        if (operation != null && operation?.kind != GitOperationKind.Bisect) {
            error = strings.repoOperationInProgress
            return false
        }
        if (bisect.complete) {
            error = strings.bisectComplete
            return false
        }
        if (operation?.kind != GitOperationKind.Bisect) {
            val started = runGit(listOf("bisect", "start")) ?: return false
            if (!started.ok) return false
        }
        return markBisectAt(hash, good)
    }

    /** Помечает текущий checkout, как GOOD/BAD; используется и автоматическим прогоном. */
    suspend fun markBisectCurrent(good: Boolean): Boolean {
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (operation?.kind != GitOperationKind.Bisect) {
            error = strings.bisectNotActive
            return false
        }
        if (bisect.complete) {
            error = strings.bisectComplete
            return false
        }
        return markBisectCurrentInternal(good)
    }

    suspend fun skipBisect(): Boolean {
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (operation?.kind != GitOperationKind.Bisect) {
            error = strings.bisectNotActive
            return false
        }
        if (bisect.complete) {
            error = strings.bisectComplete
            return false
        }
        return skipBisectInternal()
    }

    suspend fun resetBisect(): Boolean {
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (operation?.kind != GitOperationKind.Bisect && !bisect.active && bisect.log.isBlank()) {
            error = strings.bisectNotActive
            return false
        }
        if (operation != null && operation?.kind != GitOperationKind.Bisect) {
            error = strings.repoOperationInProgress
            return false
        }
        val reset = runCommitAction(
            args = bisectResetArgs(),
            message = strings.toastBisectReset,
            environment = NONINTERACTIVE_OPERATION_ENV,
        )
        if (reset) bisectAutoResult = null
        return reset
    }

    private suspend fun markBisectAt(hash: String, good: Boolean): Boolean {
        val result = runGit(
            args = bisectMarkArgs(good, hash),
            environment = NONINTERACTIVE_OPERATION_ENV,
        ) ?: return false
        notify(result, if (good) strings.toastBisectGood else strings.toastBisectBad)
        if (result.ok) {
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
        }
        return result.ok
    }

    private suspend fun markBisectCurrentInternal(good: Boolean): Boolean = runCommitAction(
        args = bisectMarkArgs(good),
        message = if (good) strings.toastBisectGood else strings.toastBisectBad,
        environment = NONINTERACTIVE_OPERATION_ENV,
    )

    private suspend fun skipBisectInternal(): Boolean = runCommitAction(
        args = bisectSkipArgs(),
        message = strings.toastBisectSkipped,
        environment = NONINTERACTIVE_OPERATION_ENV,
    )

    /**
     * Автоматически прогоняет команду на каждом кандидате. Код 0 — good,
     * 125 — skip, остальные — bad. Остановка пользователем убивает текущий
     * процесс и не помечает кандидата.
     */
    suspend fun runBisectAutomation(rawCommand: String): Boolean {
        val command = rawCommand.trim()
        if (repository.workTree == null) {
            error = strings.bisectBare
            return false
        }
        if (bisectAutoRunning) {
            error = strings.bisectAutomationRunning
            return false
        }
        if (command.isEmpty()) {
            error = strings.bisectCommandEmpty
            return false
        }
        if (operation?.kind != GitOperationKind.Bisect) {
            error = strings.bisectNotActive
            return false
        }
        if (bisect.complete) {
            error = strings.bisectComplete
            return false
        }

        val runner = BisectCommandRunner(
            workingDirectory = File(repository.workTree),
            timeoutSeconds = BISECT_COMMAND_TIMEOUT_SECONDS,
        )
        bisectRunner = runner
        bisectAutoRunning = true
        bisectAutoCancelled = false
        bisectAutoLog = ""
        bisectAutoResult = null
        appendBisectLog("$ $command")
        try {
            var steps = 0
            while (true) {
                if (bisectAutoCancelled) {
                    bisectAutoResult = strings.bisectAutomationStopped
                    appendBisectLog(strings.bisectAutomationStopped)
                    return false
                }
                if (operation?.kind != GitOperationKind.Bisect || bisect.complete) break
                if (++steps > MAX_BISECT_STEPS) {
                    error = strings.bisectTooManySteps
                    return false
                }

                val result = withContext(Dispatchers.IO) { runner.run(command) }
                logBisectCommand(command, result)
                if (result.output.isNotBlank()) appendBisectLog(result.output)
                when {
                    result.cancelled -> {
                        bisectAutoResult = strings.bisectAutomationStopped
                        appendBisectLog(strings.bisectAutomationStopped)
                        return false
                    }
                    result.timedOut -> {
                        error = strings.bisectCommandTimedOut
                        appendBisectLog(strings.bisectCommandTimedOut)
                        return false
                    }
                    result.error != null -> {
                        error = redactDisplayText(result.error)
                        appendBisectLog(result.error)
                        return false
                    }
                }
                if (bisectAutoCancelled) {
                    bisectAutoResult = strings.bisectAutomationStopped
                    appendBisectLog(strings.bisectAutomationStopped)
                    return false
                }
                val exitCode = result.exitCode
                if (exitCode == null) {
                    error = strings.bisectCommandNoExit
                    return false
                }
                appendBisectLog("[exit $exitCode]")
                val stepOk = when (exitCode) {
                    0 -> markBisectCurrentInternal(good = true)
                    125 -> skipBisectInternal()
                    else -> markBisectCurrentInternal(good = false)
                }
                if (!stepOk) return false
                if (bisectAutoCancelled) {
                    bisectAutoResult = strings.bisectAutomationStopped
                    appendBisectLog(strings.bisectAutomationStopped)
                    return false
                }
            }

            val firstBad = bisect.firstBad
            bisectAutoResult = when {
                firstBad != null -> strings.bisectFirstBadResult(firstBad.shortHash, firstBad.subject)
                operation?.kind == GitOperationKind.Bisect -> strings.bisectAutomationPaused
                else -> strings.bisectAutomationDone
            }
            appendBisectLog(bisectAutoResult.orEmpty())
            return true
        } finally {
            bisectRunner?.cancel()
            bisectRunner = null
            bisectAutoRunning = false
            bisectAutoCancelled = false
        }
    }

    /** Останавливает текущий shell-процесс; следующий шаг не выполняется. */
    fun stopBisectAutomation() {
        if (!bisectAutoRunning) return
        bisectAutoCancelled = true
        bisectRunner?.cancel()
    }

    fun clearBisectAutomationLog() {
        bisectAutoLog = ""
        bisectAutoResult = null
    }

    private fun appendBisectLog(text: String) {
        val value = redactDisplayText(text).trimEnd()
        if (value.isBlank()) return
        val combined = if (bisectAutoLog.isBlank()) value else "${bisectAutoLog}\n$value"
        bisectAutoLog = if (combined.length > MAX_BISECT_OUTPUT_CHARS) {
            "…\n" + combined.takeLast(MAX_BISECT_OUTPUT_CHARS - 2)
        } else {
            combined
        }
    }

    private fun logBisectCommand(command: String, result: BisectCommandResult) {
        addLog("$ ${redactDisplayText(command)}", LogKind.Command)
        result.output.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach {
                addLog(
                    redactDisplayText(it),
                    if (result.exitCode == 0) LogKind.Output else LogKind.Error,
                )
            }
        val status = when {
            result.timedOut -> "timeout"
            result.cancelled -> "cancelled"
            result.error != null -> redactDisplayText(result.error)
            else -> "exit ${result.exitCode}"
        }
        addLog("[$status]", if (result.exitCode == 0) LogKind.Success else LogKind.Error)
    }

    private fun loadBisectState(operation: GitOperation?, commits: List<Commit>): BisectState {
        val logFile = File(repository.gitDir, "BISECT_LOG")
        val raw = if (!logFile.isFile) {
            ""
        } else {
            runCatching {
                logFile.inputStream().use { input ->
                    input.readNBytes(MAX_BISECT_LOG_BYTES).toString(Charsets.UTF_8)
                }
            }.getOrDefault("")
        }
        val originalRef = runCatching {
            val loaded = RepositoryPaths.readTextLimited(
                File(repository.gitDir),
                "BISECT_START",
                MAX_SAFE_TEXT_FILE_BYTES,
            )
            if (loaded.truncated) "" else loaded.text.trim()
        }.getOrDefault("")
        val current = commits.firstOrNull()
        return BisectLogParser.parse(
            raw = raw,
            active = operation?.kind == GitOperationKind.Bisect,
            originalRef = originalRef,
            currentHash = current?.hash.orEmpty(),
            currentSubject = current?.subject.orEmpty(),
        )
    }

    /** Полный reflog для панели: все HEAD и branch transitions, новые сверху. */
    suspend fun loadReflog() {
        val request = ++reflogRequest
        reflogHash = null
        reflogEntries = emptyList()
        reflogLoading = true
        reflogError = null
        try {
            val entries = withContext(Dispatchers.IO) {
                backend.reflog(repository, maxCount = 1000)
            }
            if (request == reflogRequest) reflogEntries = entries
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == reflogRequest) {
                reflogError = displayError(error)
            }
        } finally {
            if (request == reflogRequest) reflogLoading = false
        }
    }

    /** Лениво читает reflog для выбранного коммита; панель открывается сразу. */
    suspend fun loadCommitReflog(hash: String) {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val request = ++reflogRequest
        reflogHash = hash
        reflogEntries = emptyList()
        reflogLoading = true
        reflogError = null
        try {
            val entries = withContext(Dispatchers.IO) {
                backend.reflog(repository, maxCount = 1000)
                    .filter { it.hash.equals(hash, ignoreCase = true) }
            }
            if (request == reflogRequest) reflogEntries = entries
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == reflogRequest) {
                reflogError = displayError(error)
            }
        } finally {
            if (request == reflogRequest) reflogLoading = false
        }
    }

    fun clearCommitReflog() {
        reflogRequest++
        reflogHash = null
        reflogEntries = emptyList()
        reflogLoading = false
        reflogError = null
    }

    fun dismissReflogError() {
        reflogError = null
    }

    private suspend fun rewriteHead(): String? {
        val result = rewriteRead(listOf("rev-parse", "--verify", "HEAD")) ?: return null
        if (!result.ok) {
            error = strings.branchNoCommits
            return null
        }
        return result.stdout.trim().takeIf(::validCommitHash)
    }

    /** HEAD ещё может быть unborn (первый commit); ошибку в этом случае не ставим. */
    private suspend fun currentTipOrNull(): String? {
        val result = rewriteRead(listOf("rev-parse", "--verify", "HEAD")) ?: return null
        return if (result.ok) result.stdout.trim().takeIf(::validCommitHash) else null
    }

    private suspend fun resolveCommitHash(hash: String): String? {
        val result = rewriteRead(listOf("rev-parse", "--verify", "$hash^{commit}")) ?: return null
        if (!result.ok) {
            error = describe(result)
            return null
        }
        return result.stdout.trim().takeIf(::validCommitHash)
    }

    private suspend fun currentBranchOrNull(): String? {
        val result = rewriteRead(listOf("symbolic-ref", "--quiet", "--short", "HEAD")) ?: return null
        if (!result.ok) return null
        return result.stdout.trim().takeIf { it.isNotEmpty() }
    }

    /** Rewrite разрешён только на локальной ветке: иначе reset/checkout заденет не тот ref. */
    private suspend fun rewriteBranch(): String? =
        currentBranchOrNull() ?: run {
            error = strings.commitRewriteBranchRequired
            null
        }

    private fun backupRefName(operation: String, tip: String): String {
        val label = operation.lowercase()
            .replace(Regex("[^a-z0-9-]+"), "-")
            .trim('-')
            .take(24)
            .ifEmpty { "rewrite" }
        return "refs/mybeautifulgit/backups/${System.currentTimeMillis()}-${++backupSequence}-$label-${tip.take(7)}"
    }

    private suspend fun createRewriteBackup(
        operation: String,
        tip: String,
        branch: String?,
    ): RewriteBackup? {
        val ref = backupRefName(operation, tip)
        val result = rewriteGit(listOf("update-ref", ref, tip)) ?: return null
        if (!result.ok) return null
        return RewriteBackup(ref = ref, previousTip = tip, branch = branch, operation = operation)
    }

    /** Проверки, общие для reword и edit/split, до изменения ветки. */
    private suspend fun rewritePreflight(
        commit: Commit,
        tip: String,
        allowComplexTarget: Boolean = false,
    ): Boolean {
        if (repository.isBare) {
            error = strings.commitRewriteBare
            return false
        }
        if (pendingRewrite != null) {
            error = strings.commitRewritePending
            return false
        }
        if (operation != null) {
            error = strings.commitRewriteActiveOperation
            return false
        }
        if (status?.isClean != true) {
            error = strings.commitRewriteCleanRequired
            return false
        }
        if (commit.parents.isEmpty() && !allowComplexTarget) {
            error = strings.commitRewriteRootUnsupported
            return false
        }
        if (commit.parents.size > 1 && !allowComplexTarget) {
            error = strings.commitRewriteMergeUnsupported
            return false
        }
        val ancestor = rewriteRead(listOf("merge-base", "--is-ancestor", commit.hash, tip))
            ?: return false
        if (!ancestor.ok) {
            error = if (ancestor.exitCode == 1) {
                strings.commitRewriteNotInHistory
            } else {
                describe(ancestor)
            }
            return false
        }
        val merges = rewriteRead(listOf("rev-list", "--merges", "${commit.hash}..$tip"))
            ?: return false
        if (!merges.ok) {
            error = describe(merges)
            return false
        }
        if (merges.stdout.trim().isNotEmpty()) {
            error = strings.commitRewriteMergeUnsupported
            return false
        }
        return true
    }

    /** Возвращает ветку на исходный tip после неудачной подготовки. */
    private suspend fun rollbackRewrite(tip: String): Boolean {
        val failure = error
        rewriteGit(listOf("reset", "--hard", tip))
        refresh()
        error = failure
        return false
    }

    private fun cleanupAfterRewrite() {
        commitMessage = ""
        commitAuthor = ""
        commitCommitter = ""
        retryCommitArgs = null
        amend = false
        commitMode = CommitMode.Normal
        fixupTarget = ""
        noVerify = false
        commitTemplatePath = ""
        selectedHunks = emptySet()
        clearMergeEditor()
    }

    private suspend fun rewriteGit(
        args: List<String>,
        environment: Map<String, String> = emptyMap(),
    ): GitResult? = operationMutex.withLock {
        rewriteGitLocked(args, environment)
    }

    private suspend fun rewriteGitLocked(
        args: List<String>,
        environment: Map<String, String> = emptyMap(),
    ): GitResult? {
        val run = beginOperation(args)
        var result: GitResult? = null
        return try {
            val completed = withContext(Dispatchers.IO) {
                backend.runControlled(
                    repository.root,
                    args,
                    timeoutSeconds = 300,
                    environment = environment,
                    control = run.control,
                )
            }
            result = completed
            logResult(completed)
            if (completed.cancelled) {
                refreshRuntimeState()
            } else if (!completed.ok) {
                error = describe(completed)
                refreshRuntimeState()
            }
            completed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            this.error = displayError(error)
            refreshRuntimeState()
            null
        } finally {
            finishOperation(run, result)
        }
    }

    private suspend fun rewriteRead(args: List<String>): GitResult? {
        return try {
            val result = withContext(Dispatchers.IO) {
                backend.run(repository.root, args, timeoutSeconds = 60)
            }
            logCheck(result)
            result
        } catch (error: Exception) {
            this.error = displayError(error)
            null
        }
    }

    private suspend fun runCommitAction(
        args: List<String>,
        message: String,
        environment: Map<String, String> = emptyMap(),
        undo: RewriteUndo? = null,
        journalAction: JournalAction? = null,
    ): Boolean {
        retryCommitArgs = null
        branchCompareRequest++
        branchComparison = null
        branchCompareLoading = false
        branchCompareError = null
        val result = runGit(args, environment = environment) ?: return false
        notify(result, message, undo, journalAction)
        if (result.ok) {
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
        } else {
            // Конфликт оставляет операцию и файлы в status; без полного refresh
            // merge editor остался бы на предыдущем выборе и не открыл конфликт.
            val failure = error
            refresh()
            if (failure != null) error = failure
        }
        return result.ok
    }

    /**
     * Выполняет range по одному коммиту через `--no-commit`. Так после
     * конфликта можно разрешить файл, застейджить его и продолжить со
     * следующего коммита, не создавая ни одного коммита.
     */
    private suspend fun startNoCommit(
        kind: GitOperationKind,
        range: String,
        fixedCommits: List<String>? = null,
    ): Boolean {
        if (pendingNoCommit != null || operation != null) {
            error = strings.repoOperationInProgress
            return false
        }
        val commits = fixedCommits ?: resolveNoCommitRange(range, kind) ?: return false
        if (commits.isEmpty()) {
            error = strings.commitActionEmptyRange
            return false
        }
        val headResult = runGit(listOf("rev-parse", "--verify", "HEAD")) ?: return false
        if (!headResult.ok) {
            error = strings.branchNoCommits
            return false
        }
        val originalTip = headResult.stdout.lineSequence().firstOrNull()?.trim()
        if (originalTip.isNullOrBlank()) {
            error = strings.branchNoCommits
            return false
        }
        pendingNoCommit = PendingNoCommit(
            kind = kind,
            range = range,
            remaining = commits,
            total = commits.size,
            originalTip = originalTip,
        )
        return applyNextNoCommit()
    }

    private suspend fun resolveNoCommitRange(
        range: String,
        kind: GitOperationKind,
    ): List<String>? {
        val result = runGit(listOf("rev-list", "--reverse", range)) ?: return null
        if (!result.ok) {
            val failure = error
            refresh()
            if (failure != null) error = failure
            return null
        }
        val commits = result.stdout.lineSequence()
            .map { it.trim() }
            .filter { validCommitHash(it) }
            .toList()
        if (commits.isEmpty()) {
            error = strings.commitActionEmptyRange
            return null
        }
        // git revert разбирает диапазон от новых коммитов к старым. Без этого
        // последовательный --no-commit loop конфликтовал бы на каждом шаге.
        return if (kind == GitOperationKind.Revert) commits.asReversed() else commits
    }

    private suspend fun applyNextNoCommit(): Boolean {
        var pending = pendingNoCommit ?: return false
        while (pending.remaining.isNotEmpty()) {
            val commit = pending.remaining.first()
            val command = noCommitCommand(pending.kind) ?: run {
                error = strings.repoOperationUnsupported
                pendingNoCommit = null
                return false
            }
            val result = runGit(
                listOf(command, "--no-commit", commit),
                environment = NONINTERACTIVE_OPERATION_ENV,
            ) ?: return false
            pending = pending.copy(remaining = pending.remaining.drop(1))
            pendingNoCommit = pending
            if (!result.ok) {
                val failure = error
                refresh()
                if (failure != null) error = failure
                if (status?.entries?.any { it.isConflicted } == true) return false
                pendingNoCommit = null
                return false
            }
        }
        pendingNoCommit = null
        selectedHunks = emptySet()
        clearMergeEditor()
        refresh()
        publishToast(
            message = when (pending.kind) {
                GitOperationKind.CherryPick -> strings.toastCommitCherryPicked
                GitOperationKind.Revert -> strings.toastCommitReverted
                else -> strings.toastCommandDone
            },
            command = "git ${noCommitCommand(pending.kind)} --no-commit ${pending.range}",
        )
        return true
    }

    /** Продолжает no-commit range после разрешения и stage текущего конфликта. */
    suspend fun continueNoCommit(): Boolean {
        val pending = pendingNoCommit ?: run {
            error = strings.repoOperationNoActive
            return false
        }
        if (operation != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status?.entries?.any { it.isConflicted } == true) {
            error = strings.repoOperationResolveConflicts
            return false
        }
        val command = noCommitCommand(pending.kind) ?: run {
            error = strings.repoOperationUnsupported
            return false
        }
        busy = true
        error = null
        try {
            val quit = runGit(
                listOf(command, "--quit"),
                environment = NONINTERACTIVE_OPERATION_ENV,
            ) ?: return false
            if (!quit.ok) return false
            return applyNextNoCommit()
        } finally {
            busy = false
        }
    }

    /** Отменяет no-commit range и возвращает ветку к коммиту до его начала. */
    suspend fun abortNoCommit(): Boolean {
        val pending = pendingNoCommit ?: run {
            error = strings.repoOperationNoActive
            return false
        }
        val command = noCommitCommand(pending.kind) ?: run {
            error = strings.repoOperationUnsupported
            return false
        }
        busy = true
        error = null
        try {
            runGit(listOf(command, "--quit"), environment = NONINTERACTIVE_OPERATION_ENV)
            val reset = runGit(listOf("reset", "--hard", pending.originalTip)) ?: return false
            if (!reset.ok) return false
            pendingNoCommit = null
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
            publishToast(
                message = strings.toastOperationAborted,
                command = reset.commandLine,
            )
            return true
        } finally {
            busy = false
        }
    }

    private fun noCommitCommand(kind: GitOperationKind): String? = when (kind) {
        GitOperationKind.CherryPick -> "cherry-pick"
        GitOperationKind.Revert -> "revert"
        else -> null
    }

    private fun validRevisionRange(range: String): Boolean {
        if (range.isBlank() || range.startsWith("-")) return false
        if (range.any { it.isWhitespace() || it.isISOControl() }) return false
        return range.contains("..") && !range.contains("...")
    }

    private fun validCommitHash(hash: String): Boolean =
        hash.matches(Regex("^[0-9a-fA-F]{4,64}$"))

    private fun validRefName(name: String): Boolean {
        if (name.isBlank() || name.startsWith("-") || name == "@") return false
        if (name.any { it.isWhitespace() || it.isISOControl() }) return false
        if (name.contains("..") || name.contains("@{") || name.contains("//")) return false
        if (name.startsWith("/") || name.endsWith("/") || name.endsWith(".")) return false
        if (name.endsWith(".lock") || name.split('/').any { it.startsWith(".") || it.endsWith(".lock") }) {
            return false
        }
        return name.none { it in "~^:?*[\\" }
    }

    /**
     * Refs read from Git are passed through Git's own format check before they
     * become positional arguments. This accepts names like `refs/tags/-foo`
     * that are valid refs but would be unsafe as bare option arguments.
     */
    private suspend fun validRefFormat(ref: String): Boolean {
        val clean = ref.trim()
        if (!clean.startsWith("refs/")) {
            error = strings.commitActionInvalidRef
            return false
        }
        val result = rewriteRead(listOf("check-ref-format", clean)) ?: return false
        if (!result.ok) {
            error = strings.commitActionInvalidRef
            return false
        }
        return true
    }

    private fun localTag(value: String): GitRef? {
        val clean = value.trim()
        return refs.firstOrNull {
            it.kind == RefKind.Tag &&
                (it.shortName == clean || it.name == clean)
        }
    }

    private fun tagArgs(
        name: String,
        message: String,
        signed: Boolean,
        hash: String?,
    ): List<String> {
        val target = if (hash == null) emptyList() else listOf(hash)
        return when {
            signed -> listOf("tag", "-s", "-m", message, name) + target
            message.isBlank() -> listOf("tag", name) + target
            else -> listOf("tag", "-a", "-m", message, name) + target
        }
    }

    private fun localBranch(value: String): GitRef? {
        val clean = value.trim()
        return refs.firstOrNull {
            it.kind == RefKind.LocalBranch &&
                (it.shortName == clean || it.name == clean)
        }
    }

    private fun remoteBranch(value: String): GitRef? {
        val clean = value.trim()
        return refs.firstOrNull {
            it.kind == RefKind.RemoteBranch &&
                !it.isSymbolic &&
                (it.shortName == clean || it.name == clean)
        }
    }

    private fun compareRef(value: String): String? {
        val clean = value.trim()
        if (clean.isEmpty() || clean.startsWith("-") || clean.any { it.isWhitespace() || it.isISOControl() }) {
            return null
        }
        return refs.firstOrNull {
            (it.kind == RefKind.LocalBranch || it.kind == RefKind.RemoteBranch) &&
                !it.isSymbolic &&
                (it.shortName == clean || it.name == clean)
        }?.shortName
    }

    suspend fun refresh() {
        operationMutex.withLock { refreshLocked() }
    }

    private fun effectiveLogFilter(): LogFilter = logFilter.copy(
        author = if (logFilter.onlyMine) currentUserEmail.orEmpty() else logFilter.author,
        onlyMine = false,
    ).normalized()

    /**
     * Читает следующую страницу истории без перечитывания уже показанных
     * коммитов. Страница добавляется к текущему окну; граф пересоберётся
     * только после изменения списка.
     */
    suspend fun loadMoreHistory() {
        if (historyLoadingMore || busy || !historyHasMore) return
        val filter = effectiveLogFilter()
        if (historyCacheFilter != filter) return

        historyLoadingMore = true
        busy = true
        val started = System.nanoTime()
        try {
            val loaded = withContext(Dispatchers.IO) {
                profiler.measureSuspend("read.history.more") {
                    backend.log(
                        repository,
                        maxCount = HISTORY_PAGE_SIZE,
                        filter = filter,
                        skip = commits.size,
                    )
                }
            }
            val known = commits.mapTo(HashSet(commits.size * 2)) { it.hash }
            val appended = loaded.filterNot { it.hash in known }
            if (appended.isNotEmpty()) {
                val combined = commits + appended
                commits = combined
                historyCache = combined
            }
            // Если git вернул только уже известные OID, не повторяем тот же
            // skip бесконечно: следующий refresh начнёт окно заново.
            historyCacheHasMore = loaded.size >= HISTORY_PAGE_SIZE && appended.isNotEmpty()
            historyHasMore = historyCacheHasMore
            profiler.increment("history.page.loaded")
            profiler.increment("history.appended", appended.size.toLong())
            error = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            profiler.record(
                "history.load-more",
                (System.nanoTime() - started) / 1_000_000,
            )
            historyLoadingMore = false
            busy = false
        }
    }

    private suspend fun refreshLocked() {
        busy = true
        val refreshStarted = System.nanoTime()
        try {
            // Runtime читаем отдельно: status может упасть из-за index.lock,
            // но объяснение и кнопка удаления всё равно должны появиться.
            profiler.measureSuspend("refresh.runtime") { refreshRuntimeState() }
            val runtimeOperation = operation
            val effectiveFilter = effectiveLogFilter()
            val cacheMatches = historyCacheFilter == effectiveFilter
            val cachedHistory = if (cacheMatches) historyCache else emptyList()
            val cachedHasMore = cacheMatches && historyCacheHasMore

            val snapshot = withContext(Dispatchers.IO) {
                val loadedStatus = repository.workTree?.let {
                    profiler.measureSuspend("read.status") { backend.status(repository) }
                }
                val loadedPage = if (effectiveFilter.author.isNotBlank() || !logFilter.onlyMine) {
                    profiler.measureSuspend("read.history.page") {
                        backend.log(
                            repository,
                            maxCount = HISTORY_PAGE_SIZE,
                            filter = effectiveFilter,
                        )
                    }
                } else {
                    emptyList()
                }
                val historyWindow = mergeHistoryWindow(
                    cached = cachedHistory,
                    cachedHasMore = cachedHasMore,
                    fresh = loadedPage,
                    pageSize = HISTORY_PAGE_SIZE,
                    allowHeadAdvance = effectiveFilter.onlyCurrent,
                )
                val loadedRefs = profiler.measureSuspend("read.refs") { backend.refs(repository) }
                // Remote names come from repository config and must never look
                // like command-line options when used by fetch/push/remote.
                val loadedRemotes = profiler.measureSuspend("read.remotes") {
                    runCatching { backend.remotes(repository) }
                        .getOrDefault(emptyList())
                        .filter(::validRemoteName)
                }
                val loadedRemoteUrls = profiler.measureSuspend("read.remote-urls") {
                    runCatching { backend.remoteUrls(repository) }
                        .getOrDefault(emptyMap())
                        .filterKeys(::validRemoteName)
                }
                val loadedWorktrees = profiler.measureSuspend("read.worktrees") {
                    runCatching { backend.worktrees(repository) }
                        .getOrDefault(emptyList())
                }
                val loadedSubmodules = if (
                    repository.workTree != null &&
                    File(repository.root, ".gitmodules").isFile
                ) {
                    profiler.measureSuspend("read.submodules") {
                        runCatching { backend.submodules(repository) }.getOrDefault(emptyList())
                    }
                } else {
                    emptyList()
                }
                val loadedStashes = profiler.measureSuspend("read.stashes") {
                    runCatching { backend.stashList(repository) }.getOrNull()
                }
                val loadedStashCount = loadedStashes?.size ?: profiler.measureSuspend("read.stash-count") {
                    runCatching { backend.stashCount(repository) }.getOrDefault(0)
                }
                val loadedRerere = profiler.measureSuspend("read.rerere") { loadRerereState() }
                val loadedBisect = profiler.measureSuspend("read.bisect") {
                    loadBisectState(runtimeOperation, historyWindow.commits)
                }
                val loadedHooks = profiler.measureSuspend("read.hooks") { loadHooksState() }
                RefreshSnapshot(
                    status = loadedStatus,
                    commits = historyWindow.commits,
                    commitsHasMore = historyWindow.hasMore,
                    historyReused = historyWindow.reusedCached,
                    refs = loadedRefs,
                    remotes = loadedRemotes,
                    remoteUrls = loadedRemoteUrls,
                    worktrees = loadedWorktrees,
                    submodules = loadedSubmodules,
                    stashCount = loadedStashCount,
                    stashes = loadedStashes.orEmpty(),
                    rerere = loadedRerere,
                    bisect = loadedBisect,
                    hooks = loadedHooks,
                )
            }
            status = snapshot.status
            commits = snapshot.commits
            historyCache = snapshot.commits
            historyCacheFilter = effectiveFilter
            historyCacheHasMore = snapshot.commitsHasMore
            historyHasMore = snapshot.commitsHasMore
            refs = snapshot.refs
            remotes = snapshot.remotes
            remoteUrls = snapshot.remoteUrls
            worktrees = snapshot.worktrees
            submodules = snapshot.submodules
            stashCount = snapshot.stashCount
            stashes = snapshot.stashes
            stashDetail?.let { detail ->
                if (snapshot.stashes.none { it.selector == detail.selector }) {
                    stashDetail = null
                }
            }
            rerere = snapshot.rerere
            bisect = snapshot.bisect
            hooks = snapshot.hooks
            hookDetail?.let { detail ->
                if (snapshot.hooks.entries.none { it.name == detail.name }) {
                    hookDetail = null
                    hookDetailError = null
                }
            }
            rerereDetail?.let { detail ->
                if (snapshot.rerere.entries.none { it.hash == detail.hash }) {
                    rerereDetail = null
                }
            }
            error = null
            keepSelection()
            profiler.increment(
                if (snapshot.historyReused) "history.cache.reuse" else "history.cache.replace",
            )
            profiler.increment("history.page.refreshed")
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            profiler.record(
                "refresh.total",
                (System.nanoTime() - refreshStarted) / 1_000_000,
            )
            busy = false
        }
    }

    /** Загружает файлы и patch выбранного stash; не меняет рабочее дерево. */
    suspend fun loadStashDetail(selector: String) {
        val entry = stashEntry(selector) ?: return
        val request = ++stashDetailRequest
        stashDetail = null
        stashDetailLoading = true
        stashDetailError = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                backend.stashShow(repository, entry.selector, maxBytes = 512 * 1024)
            }
            if (request == stashDetailRequest && stashes.any { it.selector == entry.selector }) {
                stashDetail = loaded
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (request == stashDetailRequest) {
                stashDetailError = displayError(error)
                stashDetail = null
            }
        } finally {
            if (request == stashDetailRequest) stashDetailLoading = false
        }
    }

    fun dismissStashDetailError() {
        stashDetailError = null
    }

    /**
     * `stash push` для всех изменений или только для выбранных путей.
     * `-a` включает ignored и untracked; `--staged` требует git >= 2.35.
     */
    suspend fun createStash(options: StashCreateOptions): Boolean {
        if (repository.isBare) {
            error = strings.stashBare
            return false
        }
        if (operation != null || pendingRewrite != null || pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        val currentStatus = status
        if (currentStatus == null) {
            error = strings.stashNoChanges
            return false
        }
        // -a умеет сохранять только ignored-файлы, поэтому для него не требуем
        // непустой обычный status; git сам сообщит, если сохранять нечего.
        if (!options.includeIgnored && (currentStatus.isClean || currentStatus.entries.isEmpty())) {
            error = strings.stashNoChanges
            return false
        }
        if (options.stagedOnly &&
            gitVersion < GitVersion(2, 35, 0, "2.35.0")
        ) {
            error = strings.stashStagedVersion
            return false
        }

        val knownPaths = currentStatus.entries.map { it.path }.toSet()
        val paths = options.paths
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (paths.any { path ->
                path !in knownPaths || path.any { it == '\n' || it == '\r' || it == '\u0000' }
            }
        ) {
            error = strings.stashPathInvalid
            return false
        }

        val message = options.message.trim()
        val includeUntracked = options.includeUntracked || currentStatus.entries.any {
            it.path in paths && it.kind == StatusKind.Untracked
        }
        val args = buildList {
            add("stash")
            add("push")
            when {
                options.includeIgnored -> add("--all")
                includeUntracked -> add("--include-untracked")
            }
            if (options.stagedOnly) add("--staged")
            if (message.isNotEmpty()) {
                add("-m")
                add(message)
            }
            if (paths.isNotEmpty()) {
                add("--")
                addAll(paths)
            }
        }

        val before = stashCount
        val result = runGit(args) ?: return false
        if (!result.ok) return false
        refresh()
        if (stashCount <= before) {
            error = strings.stashNoChanges
            return false
        }
        notify(result, strings.toastStashCreated)
        return true
    }

    /** Применяет stash; pop удаляет его только после успешного применения. */
    suspend fun applyStash(selector: String, pop: Boolean): Boolean {
        val entry = stashEntry(selector) ?: return false
        if (operation != null || pendingRewrite != null || pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        val result = runGit(listOf("stash", if (pop) "pop" else "apply", entry.selector))
            ?: return false
        val failure = if (result.ok) null else describe(result)
        refresh()
        if (failure != null) {
            error = failure
            return false
        }
        notify(result, if (pop) strings.toastStashPopped else strings.toastStashApplied)
        return true
    }

    suspend fun dropStash(selector: String): Boolean {
        val entry = stashEntry(selector) ?: return false
        val result = runGit(listOf("stash", "drop", entry.selector)) ?: return false
        val failure = if (result.ok) null else describe(result)
        refresh()
        if (failure != null) {
            error = failure
            return false
        }
        notify(result, strings.toastStashDropped)
        return true
    }

    /** Создаёт ветку на базе stash и применяет его; при успехе git удаляет stash. */
    suspend fun branchFromStash(selector: String, branchName: String): Boolean {
        val entry = stashEntry(selector) ?: return false
        if (repository.isBare) {
            error = strings.stashBare
            return false
        }
        if (operation != null || pendingRewrite != null || pendingNoCommit != null) {
            error = strings.repoOperationInProgress
            return false
        }
        if (status?.isClean != true) {
            error = strings.stashBranchDirty
            return false
        }
        val branch = branchName.trim()
        if (!validRefName(branch)) {
            error = strings.commitActionInvalidRef
            return false
        }
        val result = runGit(listOf("stash", "branch", branch, entry.selector)) ?: return false
        val failure = if (result.ok) null else describe(result)
        refresh()
        if (failure != null) {
            error = failure
            return false
        }
        notify(result, strings.toastStashBranchCreated)
        return true
    }

    private fun stashEntry(selector: String): StashEntry? {
        val clean = selector.trim()
        return stashes.firstOrNull { it.selector == clean } ?: run {
            error = strings.stashNotFound
            null
        }
    }

    fun toggleMergeCollapsed(hash: String) {
        collapsedMerges = if (hash in collapsedMerges) {
            collapsedMerges - hash
        } else {
            collapsedMerges + hash
        }
    }

    /** Применяет фильтры истории; для «только мои» заранее читает user.email. */
    suspend fun applyLogFilter(filter: LogFilter): Boolean {
        val normalized = filter.normalized()
        if (normalized.onlyMine) {
            busy = true
            try {
                if (currentUserEmail.isNullOrBlank()) {
                    val result = withContext(Dispatchers.IO) {
                        backend.run(
                            repository.root,
                            listOf("config", "--get", "user.email"),
                            timeoutSeconds = 30,
                        )
                    }
                    logCheck(result)
                    val email = result.stdout.lineSequence().firstOrNull()?.trim().orEmpty()
                    if (!result.ok || email.isBlank()) {
                        error = strings.logFilterNoIdentity
                        return false
                    }
                    currentUserEmail = email
                }
            } catch (error: Exception) {
                this.error = displayError(error)
                return false
            } finally {
                busy = false
            }
        }
        logFilter = normalized
        refresh()
        return true
    }

    /** Загружает выбранную запись кэша отдельно: refresh читает только список. */
    suspend fun selectRerere(hash: String) {
        if (rerere?.entries?.none { it.hash == hash } != false) return
        busy = true
        try {
            rerereDetail = withContext(Dispatchers.IO) { readRerereDetail(hash) }
            if (rerereDetail == null) error = strings.rerereLoadFailed
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            busy = false
        }
    }

    /** Включает или выключает rerere в локальном config и обновляет состояние. */
    suspend fun setRerereEnabled(enabled: Boolean): Boolean {
        val result = runGit(
            listOf("config", "rerere.enabled", if (enabled) "true" else "false"),
        ) ?: return false
        notify(result, if (enabled) strings.toastRerereEnabled else strings.toastRerereDisabled)
        if (result.ok) refresh()
        return result.ok
    }

    /**
     * Удаляет только commonDir/rr-cache. `git rerere clear` для этого не подходит:
     * он сбрасывает metadata текущего abort, а не все записанные решения.
     */
    suspend fun clearRerereCache(): Boolean {
        busy = true
        return try {
            val directory = File(repository.commonDir, "rr-cache")
            val removed = withContext(Dispatchers.IO) {
                RepositoryPaths.deleteTree(
                    File(repository.commonDir),
                    "rr-cache",
                    allowFinalSymlink = true,
                )
            }
            if (!removed) {
                error = strings.rerereClearFailed
                false
            } else {
                rerereDetail = null
                toast = OperationToast(
                    id = ++nextToastId,
                    message = strings.toastRerereCleared,
                    command = directory.path,
                    success = true,
                )
                refresh()
                true
            }
        } catch (error: Exception) {
            this.error = pathError(error)
            false
        } finally {
            busy = false
        }
    }

    suspend fun select(entry: StatusEntry, staged: Boolean) {
        busy = true
        try {
            loadSelection(entry, staged)
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            busy = false
        }
    }

    fun editMergeDraft(text: String) {
        if (mergeEditor == null) return
        mergeDraft = text
        mergeSelections = emptyMap()
        mergeParse = parseConflictMarkers(text)
    }

    fun toggleMergeLine(conflictId: Int, side: MergeSide, lineIndex: Int) {
        val conflict = mergeConflicts.firstOrNull { it.id == conflictId } ?: return
        val valid = when (side) {
            MergeSide.Ours -> lineIndex in conflict.ours.indices
            MergeSide.Theirs -> lineIndex in conflict.theirs.indices
        }
        if (!valid) return
        val current = mergeSelections[conflictId] ?: MergeLineChoice()
        mergeSelections = mergeSelections + (conflictId to current.toggle(side, lineIndex))
    }

    /** Немедленно заменяет блок одной стороной, включая пустую сторону. */
    fun applyMergeConflictSide(conflictId: Int, side: MergeSide): Boolean {
        val conflict = mergeConflicts.firstOrNull { it.id == conflictId } ?: return false
        val choice = when (side) {
            MergeSide.Ours -> MergeLineChoice(ours = conflict.ours.indices.toSet())
            MergeSide.Theirs -> MergeLineChoice(theirs = conflict.theirs.indices.toSet())
        }
        val updated = applyConflictChoices(mergeDraft, mapOf(conflictId to choice))
        mergeDraft = updated
        mergeSelections = emptyMap()
        mergeParse = parseConflictMarkers(updated)
        error = null
        return true
    }

    fun clearMergeSelection(conflictId: Int) {
        mergeSelections = mergeSelections - conflictId
    }

    /** Заменяет один блок выбранными строками; остальные маркеры не трогает. */
    fun applyMergeConflict(conflictId: Int): Boolean {
        val choice = mergeSelections[conflictId]
        if (choice == null || choice.isEmpty) {
            error = strings.repoMergeSelectionRequired
            return false
        }
        val updated = applyConflictChoices(mergeDraft, mapOf(conflictId to choice))
        mergeDraft = updated
        // После замены блока конфликты перенумеровываются; старые выборы
        // для соседних блоков больше нельзя сопоставить с новыми id.
        mergeSelections = emptyMap()
        mergeParse = parseConflictMarkers(updated)
        error = null
        return true
    }

    /** «Взять всё наше/их» с сохранением контекста между конфликтами. */
    fun takeAllMergeSide(side: MergeSide): Boolean {
        if (mergeEditor == null) return false
        if (mergeConflicts.isEmpty()) {
            error = strings.repoMergeNoConflicts
            return false
        }
        val updated = applyConflictSide(mergeDraft, side)
        mergeDraft = updated
        mergeSelections = emptyMap()
        mergeParse = parseConflictMarkers(updated)
        error = null
        return true
    }

    /** Сохраняет ровно текущий draft: маркеры остаются маркерами. */
    suspend fun saveMergeMarkers(): Boolean {
        val state = mergeEditor ?: return false
        if (state.binary || state.tooLarge || state.error != null) {
            error = state.error ?: strings.repoMergeUnavailable
            return false
        }
        val draftToSave = mergeDraft
        val root = repository.workTree?.let(::File) ?: run {
            error = strings.repoMergeUnavailable
            return false
        }
        busy = true
        error = null
        try {
            val saved = withContext(Dispatchers.IO) {
                RepositoryPaths.writeText(root, state.path, draftToSave)
            }
            mergeSavedText = draftToSave
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastMergeSaved,
                command = saved.toString(),
                success = true,
            )
            refresh()
            return true
        } catch (error: Exception) {
            this.error = pathError(error)
            return false
        } finally {
            busy = false
        }
    }

    /** Явно перечитывает файл и три stage-версии, выбрасывая несохранённые правки. */
    suspend fun reloadMergeEditor() {
        val entry = status?.entries?.firstOrNull { it.path == selectedPath } ?: return
        if (!entry.isConflicted) return
        mergeEditor = null
        busy = true
        try {
            loadSelection(entry, selectedStaged)
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            busy = false
        }
    }

    suspend fun stage(entry: StatusEntry) {
        retryCommitArgs = null
        val args = listOf("add", "-A", "--") + paths(entry)
        val result = runGit(args) ?: return
        notify(result, strings.toastStaged)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    /** Явное действие для untracked-строки: добавить файл в index. */
    suspend fun addUntracked(entry: StatusEntry) {
        if (entry.kind != StatusKind.Untracked) return
        retryCommitArgs = null
        val result = runGit(listOf("add", "-A", "--") + paths(entry)) ?: return
        notify(result, strings.toastAdded)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    /**
     * Добавляет untracked-файл в корневой .gitignore и проверяет результат
     * тем же git, который будет применять правило: check-ignore. Если
     * проверка не прошла, .gitignore восстанавливается.
     */
    suspend fun ignore(entry: StatusEntry): Boolean {
        if (entry.kind != StatusKind.Untracked) {
            error = strings.repoIgnoreUnavailable
            return false
        }
        val pattern = gitignorePattern(entry.path)
        if (pattern == null) {
            error = strings.repoIgnoreUnsupportedPath
            return false
        }

        busy = true
        error = null
        try {
            val check = withContext(Dispatchers.IO) {
                backend.run(
                    repository.root,
                    listOf("check-ignore", "-q", "--", entry.path),
                )
            }
            logCheck(check)
            if (check.exitCode !in 0..1) {
                error = describe(check)
                return false
            }
            if (check.ok) {
                toast = OperationToast(
                    id = ++nextToastId,
                    message = strings.repoIgnoreAlready,
                    command = check.commandLine,
                    success = true,
                )
                return true
            }

            val root = File(repository.root)
            val ignoreFile = RepositoryPaths.resolve(root, ".gitignore")
            val existed = Files.isRegularFile(ignoreFile, LinkOption.NOFOLLOW_LINKS)
            val previous = if (existed) {
                withContext(Dispatchers.IO) {
                    RepositoryPaths.readTextLimited(root, ".gitignore", MAX_SAFE_TEXT_FILE_BYTES)
                }
            } else {
                LimitedRepositoryText("", false)
            }
            if (previous.truncated) {
                error = strings.repoFileTooLarge
                return false
            }
            val previousText = previous.text
            withContext(Dispatchers.IO) {
                val separator = if (previousText.isEmpty() || previousText.endsWith("\n")) "" else "\n"
                RepositoryPaths.writeText(
                    root,
                    ".gitignore",
                    previousText + separator + pattern + "\n",
                )
            }

            val verified = withContext(Dispatchers.IO) {
                backend.run(
                    repository.root,
                    listOf("check-ignore", "-v", "--", entry.path),
                )
            }
            logResult(verified)
            if (!verified.ok) {
                withContext(Dispatchers.IO) {
                    if (existed) {
                        RepositoryPaths.writeText(root, ".gitignore", previousText)
                    } else {
                        RepositoryPaths.deleteFile(root, ".gitignore")
                    }
                }
                error = strings.repoIgnoreVerifyFailed
                toast = OperationToast(
                    id = ++nextToastId,
                    message = strings.toastFailed,
                    command = verified.commandLine,
                    success = false,
                )
                return false
            }

            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastIgnored,
                command = verified.commandLine,
                success = true,
            )
            selectedHunks = emptySet()
            refresh()
            return true
        } catch (error: Exception) {
            this.error = pathError(error)
            return false
        } finally {
            busy = false
        }
    }

    suspend fun unstage(entry: StatusEntry) {
        retryCommitArgs = null
        val args = if (status?.branch?.unborn == true) {
            listOf("rm", "--cached", "-r", "--") + paths(entry)
        } else {
            listOf("restore", "--staged", "--") + paths(entry)
        }
        val result = runGit(args) ?: return
        notify(result, strings.toastUnstaged)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    suspend fun stageAll() {
        retryCommitArgs = null
        val result = runGit(listOf("add", "-A")) ?: return
        notify(result, strings.toastStagedAll)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    suspend fun unstageAll() {
        retryCommitArgs = null
        val args = if (status?.branch?.unborn == true) {
            listOf("rm", "--cached", "-r", "--", ".")
        } else {
            listOf("restore", "--staged", "--", ".")
        }
        val result = runGit(args) ?: return
        notify(result, strings.toastUnstagedAll)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    /** Stage/unstage весь выбранный хунк через патч в stdin. */
    suspend fun applyHunk(hunkIndex: Int) = applyPartial(hunkIndex, null)

    /** Stage/unstage одну строку выбранного хунка через патч в stdin. */
    suspend fun applyLine(hunkIndex: Int, lineIndex: Int) =
        applyPartial(hunkIndex, setOf(lineIndex))

    /**
     * Выбрасывает изменения из worktree. Index не трогаем: staged-часть
     * остаётся ровно такой, какой была. Для hunk/строк сначала собираем
     * патч оставшихся изменений, возвращаем файл к index и накладываем
     * только этот остаток — так соседние правки не теряются.
     */
    suspend fun discard(target: DiscardTarget, backup: DiscardBackup = DiscardBackup.None): Boolean {
        retryCommitArgs = null
        if (target.entry.isConflicted) {
            error = strings.repoDiscardUnavailable
            return false
        }
        return if (target.scope == DiscardScope.File) {
            discardFile(target.entry, backup)
        } else {
            discardPartial(target, backup)
        }
    }

    private suspend fun discardFile(entry: StatusEntry, backup: DiscardBackup): Boolean {
        if (!entry.isUnstaged) {
            error = strings.repoDiscardUnavailable
            return false
        }
        if (backup == DiscardBackup.Stash) {
            return stashFile(entry)
        }

        val patchPath = if (backup == DiscardBackup.Patch) {
            val patch = loadDiffText(entry, staged = false)
            if (!patch.startsWith("diff --git ")) {
                error = strings.repoDiscardPatchUnavailable
                return false
            }
            savePatch(patch, entry.path) ?: return false
        } else {
            null
        }

        val result = if (entry.kind == StatusKind.Untracked) {
            runGit(listOf("clean", "-f", "--", entry.path))
        } else {
            runGit(listOf("restore", "--worktree", "--", entry.path))
        } ?: return false
        if (!result.ok) {
            notify(result, strings.toastFailed)
            return false
        }

        selectedHunks = emptySet()
        notify(
            result,
            if (patchPath != null) strings.toastDiscardedWithPatch(patchPath) else strings.toastDiscarded,
        )
        refresh()
        return true
    }

    private suspend fun stashFile(entry: StatusEntry): Boolean {
        val args = buildList {
            add("stash"); add("push")
            if (entry.kind == StatusKind.Untracked) {
                add("--include-untracked")
            } else {
                add("--keep-index")
            }
            add("-m"); add("chan discard: ${entry.path}")
            add("--"); add(entry.path)
        }
        val result = runGit(args) ?: return false
        if (!result.ok) {
            notify(result, strings.toastFailed)
            return false
        }
        selectedHunks = emptySet()
        notify(result, strings.toastDiscardedToStash)
        refresh()
        return true
    }

    private suspend fun discardPartial(target: DiscardTarget, backup: DiscardBackup): Boolean {
        if (backup == DiscardBackup.Stash) {
            error = strings.repoDiscardStashPartial
            return false
        }
        if (selectedStaged || target.entry.path != selectedPath || !canDiscardSelectedPartially) {
            error = strings.repoDiscardUnavailable
            return false
        }

        val parsed = parsePatch(diff)
        val hunk = parsed.hunks.getOrNull(target.hunkIndex)
        if (hunk == null || hunk.isDeletion) {
            error = strings.repoDiscardUnavailable
            return false
        }
        val selectedLines = if (target.scope == DiscardScope.Hunk) {
            null
        } else {
            changeGroupIndexes(hunk, target.lineIndexes)
        }
        val patch = buildPartialPatch(
            hunk = hunk,
            selectedLineIndexes = selectedLines,
            unstage = false,
        )
        if (patch.isBlank()) {
            error = strings.repoDiscardUnavailable
            return false
        }
        val patchPath = if (backup == DiscardBackup.Patch) {
            savePatch(patch, target.entry.path) ?: return false
        } else {
            null
        }

        val fullPatch = diff
        val remainder = buildRemainderPatch(parsed, target)
        val reset = runGit(listOf("restore", "--worktree", "--", target.entry.path)) ?: return false
        if (!reset.ok) {
            notify(reset, strings.toastFailed)
            return false
        }

        var last = reset
        if (remainder.isNotBlank()) {
            val applied = runGit(
                args = listOf("apply", "--whitespace=nowarn", "--unidiff-zero"),
                stdin = remainder,
            )
            if (applied == null || !applied.ok) {
                val restored = runGit(
                    args = listOf("apply", "--whitespace=nowarn", "--unidiff-zero"),
                    stdin = fullPatch,
                )
                error = if (restored?.ok != true) {
                    strings.repoDiscardRestoreFailed
                } else {
                    strings.repoDiscardApplyFailed
                }
                return false
            }
            last = applied
        }

        selectedHunks = emptySet()
        notify(
            last,
            if (patchPath != null) strings.toastDiscardedWithPatch(patchPath) else strings.toastDiscarded,
        )
        refresh()
        return true
    }

    /** Патч всех изменений, которые должны остаться после частичного discard. */
    private fun buildRemainderPatch(parsed: ParsedDiff, target: DiscardTarget): String {
        val selections = parsed.hunks.associate { hunk ->
            hunk.index to when {
                target.scope == DiscardScope.Hunk && hunk.index == target.hunkIndex -> emptySet<Int>()
                target.scope == DiscardScope.Lines && hunk.index == target.hunkIndex -> {
                    val discarded = changeGroupIndexes(hunk, target.lineIndexes)
                    hunk.lines.withIndex()
                        .filter { (_, line) ->
                            line.kind == DiffLineKind.Added || line.kind == DiffLineKind.Removed
                        }
                        .map { (index, _) -> index }
                        .filterNot { it in discarded }
                        .toSet()
                }
                else -> null
            }
        }
        return buildPartialPatch(parsed, selections, unstage = false)
    }

    private suspend fun savePatch(patch: String, label: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val base = File(repository.commonDir)
                RepositoryPaths.createDirectories(base, "chan-backups")
                val stamp = System.currentTimeMillis()
                val safeLabel = label.replace(Regex("[^A-Za-z0-9._-]+"), "_")
                    .trim('_').take(40).ifBlank { "changes" }
                RepositoryPaths.writeText(
                    base,
                    "chan-backups/discard-$stamp-$safeLabel.patch",
                    patch,
                ).toString()
            }
        } catch (error: Exception) {
            this.error = pathError(error)
            null
        }
    }

    /**
     * В блоке замены строки pairing-ятся по порядку: -old/+new — одна
     * логическая строка. Если выбросить только +new, -old останется рядом;
     * поэтому discard на строке откатывает её пару целиком.
     */
    private fun changeGroupIndexes(
        hunk: org.baconana.mybeautifulgit_chan.domain.DiffHunk,
        lineIndexes: Set<Int>,
    ): Set<Int> {
        val result = mutableSetOf<Int>()
        val removed = mutableListOf<Int>()
        val added = mutableListOf<Int>()

        fun flush() {
            repeat(maxOf(removed.size, added.size)) { index ->
                val unit = buildSet {
                    removed.getOrNull(index)?.let(::add)
                    added.getOrNull(index)?.let(::add)
                }
                if (unit.any { it in lineIndexes }) result += unit
            }
            removed.clear()
            added.clear()
        }

        for ((index, line) in hunk.lines.withIndex()) {
            when (line.kind) {
                DiffLineKind.Added -> added += index
                DiffLineKind.Removed -> removed += index
                DiffLineKind.Context, DiffLineKind.Meta -> flush()
                DiffLineKind.Header, DiffLineKind.Hunk -> Unit
            }
        }
        flush()
        return result
    }

    fun toggleHunkSelection(hunkIndex: Int) {
        if (selectedStaged) return
        selectedHunks = if (hunkIndex in selectedHunks) {
            selectedHunks - hunkIndex
        } else {
            selectedHunks + hunkIndex
        }
    }

    /** Stage выбранные хунки и создаёт обычный git-коммит из получившегося index. */
    suspend fun commitSelectedHunks(): Boolean {
        retryCommitArgs = null
        if (selectedStaged) return false
        if (selectedHunks.isEmpty()) return false

        if (!validateCommitInput()) return false

        val parsed = parsePatch(diff)
        val hunkIndexes = selectedHunks.filter { index ->
            parsed.hunks.getOrNull(index)?.let { !it.isDeletion } == true
        }.toSet()
        val patch = buildPartialPatch(parsed, hunkIndexes, unstage = false)
        if (patch.isBlank()) {
            error = strings.repoHunksUnavailable
            return false
        }

        val applied = runGit(
            args = listOf("apply", "--cached", "--whitespace=nowarn", "--unidiff-zero"),
            stdin = patch,
        ) ?: return false
        if (!applied.ok) {
            notify(applied, strings.toastFailed)
            return false
        }

        selectedHunks = emptySet()
        return commit()
    }

    /**
     * Выполняет произвольную git-команду в контексте репозитория.
     * Команда разбирается без shell; `git` в начале можно не писать.
     */
    suspend fun runCommand(raw: String): Boolean {
        retryCommitArgs = null
        val args = parseCommand(raw) ?: return false
        return executeCommand(args)
    }

    /**
     * Запускает команду без operationMutex и busy: UI остаётся доступным.
     * Задачи намеренно не отменяют друг друга; конкурентные записи Git
     * обрабатывает своими lock-файлами и обычной ошибкой.
     */
    suspend fun runCommandInBackground(raw: String): Boolean {
        retryCommitArgs = null
        error = null
        val args = parseCommand(raw) ?: return false
        val commandLine = displayCommandLine("git", args)
        val id = ++nextBackgroundCommandId
        val control = GitRunControl { chunk -> updateBackgroundProgress(id, chunk) }
        backgroundRunControls[id] = control
        synchronized(backgroundProgressBuffers) {
            backgroundProgressBuffers[id] = StringBuilder()
        }
        backgroundCommands = (
            listOf(BackgroundCommand(id, commandLine, BackgroundCommandState.Running)) +
                backgroundCommands
            ).take(MAX_BACKGROUND_COMMANDS)
        addLog("${strings.runnerBackgroundStarted} · $commandLine", LogKind.Info)

        return try {
            val result = withContext(Dispatchers.IO) {
                backend.runControlled(
                    repository.root,
                    args,
                    timeoutSeconds = BACKGROUND_COMMAND_TIMEOUT_SECONDS,
                    control = control,
                )
            }
            logResult(result)
            backgroundCommands = backgroundCommands.map { job ->
                if (job.id != id) {
                    job
                } else {
                    job.copy(
                        state = when {
                            result.cancelled -> BackgroundCommandState.Cancelled
                            result.timedOut -> BackgroundCommandState.TimedOut
                            result.ok -> BackgroundCommandState.Succeeded
                            else -> BackgroundCommandState.Failed
                        },
                        exitCode = result.exitCode,
                        durationMs = result.durationMs,
                        cancelling = false,
                    )
                }
            }
            if (result.cancelled) {
                refreshRuntimeState()
            } else if (!result.ok) {
                error = describe(result)
                refreshRuntimeState()
            }
            notify(result, strings.toastCommandDone)
            if (result.ok) {
                selectedHunks = emptySet()
                refresh()
            }
            result.ok
        } catch (cancelled: CancellationException) {
            markBackgroundFailed(id)
            throw cancelled
        } catch (failure: Exception) {
            val message = displayError(failure)
            error = message
            addLog(message, LogKind.Error)
            markBackgroundFailed(id)
            publishToast(message = strings.toastFailed, command = commandLine, success = false)
            false
        } finally {
            backgroundRunControls.remove(id)
            synchronized(backgroundProgressBuffers) {
                backgroundProgressBuffers.remove(id)
            }
        }
    }

    /** Отменяет именно эту фоновую задачу, не трогая другие и активную операцию. */
    fun cancelBackgroundCommand(id: Long) {
        val job = backgroundCommands.firstOrNull { it.id == id } ?: return
        if (job.state != BackgroundCommandState.Running || job.cancelling) return
        backgroundCommands = backgroundCommands.map {
            if (it.id == id) it.copy(cancelling = true) else it
        }
        backgroundRunControls[id]?.cancel()
    }

    private fun updateBackgroundProgress(id: Long, chunk: String) {
        val buffered = synchronized(backgroundProgressBuffers) {
            val buffer = backgroundProgressBuffers[id] ?: return
            buffer.append(chunk)
            if (buffer.length > MAX_PROGRESS_BUFFER_CHARS) {
                buffer.delete(0, buffer.length - MAX_PROGRESS_BUFFER_CHARS)
            }
            buffer.toString()
        }
        val progress = parseOperationProgress(buffered) ?: return
        val current = backgroundCommands.firstOrNull { it.id == id } ?: return
        if (current.progress == progress) return
        Snapshot.withMutableSnapshot {
            backgroundCommands = backgroundCommands.map { job ->
                if (job.id == id) job.copy(progress = progress) else job
            }
        }
    }

    fun dismissBackgroundCommand(id: Long) {
        backgroundCommands = backgroundCommands.filterNot { it.id == id }
    }

    fun clearFinishedBackgroundCommands() {
        backgroundCommands = backgroundCommands.filter {
            it.state == BackgroundCommandState.Running
        }
    }

    private fun markBackgroundFailed(id: Long) {
        backgroundCommands = backgroundCommands.map { job ->
            if (job.id == id) {
                job.copy(state = BackgroundCommandState.Failed, cancelling = false)
            } else {
                job
            }
        }
    }

    private fun parseCommand(raw: String): List<String>? {
        val tokens = when (val parsed = GitCommandLine.parse(raw)) {
            GitCommandLine.Parsed.Empty -> {
                error = strings.repoCommandEmpty
                return null
            }

            GitCommandLine.Parsed.UnclosedQuote -> {
                error = strings.repoCommandUnclosedQuote
                return null
            }

            is GitCommandLine.Parsed.Args -> parsed.values
        }

        val args = if (tokens.first().equals("git", ignoreCase = true)) {
            tokens.drop(1)
        } else {
            tokens
        }
        if (args.isEmpty()) {
            error = strings.repoCommandEmpty
            return null
        }
        return args
    }

    /** Запускает configured alias по точному имени, не разбирая его как shell-команду. */
    suspend fun runAlias(alias: GitAlias): Boolean {
        retryCommitArgs = null
        if (alias.name.isBlank()) {
            error = strings.repoCommandEmpty
            return false
        }
        return executeCommand(listOf(alias.name))
    }

    private suspend fun executeCommand(args: List<String>): Boolean {
        val result = runGit(args) ?: return false
        notify(result, if (result.ok) strings.toastCommandDone else strings.toastFailed)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
        return result.ok
    }

    /**
     * Продолжает, прерывает или пропускает незавершённую операцию.
     * Для continue отключаем редактор: UI не должен зависнуть на merge/rebase
     * commit message. Точная git-команда попадает в console и toast.
     */
    suspend fun operationAction(action: OperationAction): Boolean {
        retryCommitArgs = null
        val current = operation ?: run {
            error = strings.repoOperationNoActive
            return false
        }
        val args = current.kind.operationArgs(action) ?: run {
            error = if (action == OperationAction.Skip && current.kind == GitOperationKind.Merge) {
                strings.repoOperationSkipUnsupported
            } else {
                strings.repoOperationUnsupported
            }
            return false
        }
        val interactiveBackup = pendingRebaseBackup.takeIf { current.kind == GitOperationKind.Rebase }
        val result = runGit(args, environment = NONINTERACTIVE_OPERATION_ENV) ?: return false
        notify(
            result,
            when (action) {
                OperationAction.Continue -> strings.toastOperationContinued
                OperationAction.Abort -> strings.toastOperationAborted
                OperationAction.Skip -> strings.toastOperationSkipped
            },
        )
        if (result.ok) {
            selectedHunks = emptySet()
            clearMergeEditor()
            refresh()
            if (interactiveBackup != null) {
                if (action == OperationAction.Abort) {
                    pendingRebaseBackup = null
                } else if (operation == null) {
                    pendingRebaseBackup = null
                    val postTip = rewriteHead() ?: interactiveBackup.previousTip
                    publishToast(
                        message = when (action) {
                            OperationAction.Continue -> strings.toastOperationContinued
                            OperationAction.Abort -> strings.toastOperationAborted
                            OperationAction.Skip -> strings.toastOperationSkipped
                        },
                        command = result.commandLine,
                        undo = RewriteUndo(interactiveBackup, postTip),
                    )
                }
            }
        } else {
            // Новый конфликт на следующем шаге должен сразу открыть merge editor.
            val failure = error
            refresh()
            if (failure != null) error = failure
        }
        return result.ok
    }

    suspend fun commit(): Boolean {
        retryCommitArgs = null
        if (pendingRewrite != null && (amend || commitMode != CommitMode.Normal)) {
            error = strings.commitRewriteNoAmend
            return false
        }
        if (!validateCommitInput()) return false

        val amendBackup = if (amend && commitMode == CommitMode.Normal) {
            val tip = rewriteHead() ?: return false
            createRewriteBackup("amend", tip, currentBranchOrNull()) ?: return false
        } else {
            null
        }
        val beforeTip = amendBackup?.previousTip ?: currentTipOrNull()

        val message = commitMessage.trim()
        val author = commitAuthor.trim().takeIf { it.isNotEmpty() }?.let(::parseIdentity)
        val committer = commitCommitter.trim().takeIf { it.isNotEmpty() }?.let(::parseIdentity)
        val authorValue = when {
            author != null -> author.format()
            committer != null -> defaultAuthorIdent() ?: return false
            else -> null
        }

        val args = buildList {
            if (committer != null) {
                add("-c")
                add("user.name=${committer.name}")
                add("-c")
                add("user.email=${committer.email}")
            }
            add("commit")
            when (commitMode) {
                CommitMode.Fixup -> add("--fixup=${fixupTarget.trim()}")
                CommitMode.Squash -> {
                    add("--squash=${fixupTarget.trim()}")
                    // Без -m/--no-edit git открывает редактор и ждёт ввода.
                    add("--no-edit")
                }
                CommitMode.Normal -> {
                    if (amend) add("--amend")
                    if (commitTemplatePath.isNotBlank()) {
                        add("--template=${commitTemplatePath.trim()}")
                    }
                    if (message.isEmpty()) {
                        add("--no-edit")
                    } else {
                        add("-m")
                        add(message)
                    }
                }
            }
            if (sign) add("-S")
            if (noVerify) add("--no-verify")
            authorValue?.let { add("--author=$it") }
        }

        val result = runGit(args) ?: return false
        val afterTip = if (result.ok) rewriteHead() else null
        val undo = if (result.ok && amendBackup != null) {
            afterTip?.let { RewriteUndo(amendBackup, it) }
        } else {
            null
        }
        val journalAction = if (
            result.ok &&
            amendBackup == null &&
            beforeTip != null &&
            afterTip != null &&
            !beforeTip.equals(afterTip, ignoreCase = true)
        ) {
            JournalAction.CommitReset(
                beforeTip = beforeTip,
                afterTip = afterTip,
                branch = currentBranchOrNull(),
            )
        } else {
            null
        }
        notify(result, strings.toastCommitted, undo, journalAction)
        if (result.ok) {
            resetCommitAfterSuccess()
            refresh()
        } else {
            retryCommitArgs = args
        }
        return result.ok
    }

    /** Повторяет ровно ту commit-команду, которая упала на хуке или в git. */
    suspend fun retryCommit(): Boolean {
        val args = retryCommitArgs ?: return false
        val amendBackup = if (args.contains("--amend")) {
            val tip = rewriteHead() ?: return false
            createRewriteBackup("amend", tip, currentBranchOrNull()) ?: return false
        } else {
            null
        }
        val beforeTip = amendBackup?.previousTip ?: currentTipOrNull()
        val result = runGit(args) ?: return false
        val afterTip = if (result.ok) rewriteHead() else null
        val undo = if (result.ok && amendBackup != null) {
            afterTip?.let { RewriteUndo(amendBackup, it) }
        } else {
            null
        }
        val journalAction = if (
            result.ok &&
            amendBackup == null &&
            beforeTip != null &&
            afterTip != null &&
            !beforeTip.equals(afterTip, ignoreCase = true)
        ) {
            JournalAction.CommitReset(
                beforeTip = beforeTip,
                afterTip = afterTip,
                branch = currentBranchOrNull(),
            )
        } else {
            null
        }
        notify(result, strings.toastCommitted, undo, journalAction)
        if (result.ok) {
            retryCommitArgs = null
            resetCommitAfterSuccess()
            refresh()
        } else {
            retryCommitArgs = args
        }
        return result.ok
    }

    fun dismissRetryCommit() {
        retryCommitArgs = null
    }

    private fun resetCommitAfterSuccess() {
        commitMessage = ""
        amend = false
        commitMode = CommitMode.Normal
        fixupTarget = ""
        noVerify = false
        commitTemplatePath = ""
        selectedHunks = emptySet()
        retryCommitArgs = null
    }

    /** Загружает commit template в сообщение: git commit получает готовый -m. */
    suspend fun loadCommitTemplate(): Boolean {
        retryCommitArgs = null
        val rawPath = commitTemplatePath.trim()
        if (rawPath.isEmpty()) {
            error = strings.repoCommitTemplateRequired
            return false
        }
        val file = File(rawPath).let { if (it.isAbsolute) it else File(repository.root, rawPath) }
        busy = true
        error = null
        try {
            val text = withContext(Dispatchers.IO) {
                if (!file.isFile) {
                    this@RepoController.error = strings.repoCommitTemplateMissing
                    return@withContext null
                }
                if (file.length() > MAX_COMMIT_TEMPLATE_BYTES) {
                    this@RepoController.error = strings.repoCommitTemplateTooLarge
                    return@withContext null
                }
                file.readText()
            } ?: return false
            val cleaned = text.removePrefix("\uFEFF")
                .lineSequence()
                .filterNot { it.trimStart().startsWith("#") }
                .joinToString("\n")
                .trim()
            if (cleaned.isEmpty()) {
                error = strings.repoCommitTemplateEmpty
                return false
            }
            commitMessage = cleaned
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastTemplateLoaded,
                command = file.path,
                success = true,
            )
            return true
        } catch (error: Exception) {
            this.error = displayError(error)
            return false
        } finally {
            busy = false
        }
    }

    private fun validateCommitInput(): Boolean {
        when (commitMode) {
            CommitMode.Normal -> if (commitMessage.trim().isEmpty() && !amend) {
                error = strings.repoCommitMessageRequired
                return false
            }

            CommitMode.Fixup, CommitMode.Squash -> {
                if (amend) {
                    error = strings.repoCommitFixupAmendConflict
                    return false
                }
                if (fixupTarget.isBlank()) {
                    error = strings.repoCommitTargetRequired
                    return false
                }
            }
        }
        if (commitAuthor.isNotBlank() && parseIdentity(commitAuthor) == null) {
            error = strings.repoCommitIdentityInvalid
            return false
        }
        if (commitCommitter.isNotBlank() && parseIdentity(commitCommitter) == null) {
            error = strings.repoCommitIdentityInvalid
            return false
        }
        return true
    }

    private fun parseIdentity(value: String): Identity? {
        val match = IDENTITY_PATTERN.matchEntire(value.trim()) ?: return null
        val name = match.groupValues[1].trim()
        val email = match.groupValues[2].trim()
        if (name.isEmpty() || email.isEmpty()) return null
        return Identity(name, email)
    }

    /** Автор из конфигурации нужен, когда задан только committer. */
    private suspend fun defaultAuthorIdent(): String? {
        val result = withContext(Dispatchers.IO) {
            backend.run(repository.root, listOf("var", "GIT_AUTHOR_IDENT"))
        }
        logCheck(result)
        if (!result.ok) {
            error = describe(result)
            return null
        }
        val match = IDENTITY_WITH_DATE.find(result.stdout.trim())
        if (match == null) {
            error = strings.repoCommitIdentityInvalid
            return null
        }
        return match.groupValues[1].trim()
    }

    /** Читает все scopes сразу; неизвестные приложению ключи не отбрасываются. */
    suspend fun loadConfigEntries(): List<GitConfigEntry> {
        busy = true
        error = null
        return try {
            withContext(Dispatchers.IO) { backend.configEntries(repository) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error = displayError(error)
            emptyList()
        } finally {
            busy = false
        }
    }

    /** Все aliases из system/global/local/worktree, включая условные includeIf. */
    suspend fun loadAliases(): List<GitAlias> = gitAliases(loadConfigEntries())

    /**
     * Сохраняет ключ в выбранный scope. Если [oldValue] задан, заменяется только
     * одно multivalue-значение; остальные не затрагиваются.
     */
    suspend fun saveConfigEntry(
        scope: GitConfigScope,
        key: String,
        value: String,
        oldValue: String? = null,
        confirmed: Boolean = false,
    ): Boolean = writeConfigEntry(
        scope = scope,
        key = key,
        value = value,
        oldValue = oldValue,
        append = false,
        confirmed = confirmed,
    )

    /** Добавляет ещё одно значение ключа, сохраняя существующие значения. */
    suspend fun addConfigEntry(
        scope: GitConfigScope,
        key: String,
        value: String,
        confirmed: Boolean = false,
    ): Boolean = writeConfigEntry(
        scope = scope,
        key = key,
        value = value,
        oldValue = null,
        append = true,
        confirmed = confirmed,
    )

    private suspend fun writeConfigEntry(
        scope: GitConfigScope,
        key: String,
        value: String,
        oldValue: String?,
        append: Boolean,
        confirmed: Boolean,
    ): Boolean {
        val cleanKey = key.trim()
        val validation = validateGitConfigEntry(cleanKey, value)
        if (!validation.ok) {
            error = configValidationMessage(validation.error)
            return false
        }
        if (requiresGitConfigScopeConfirmation(scope) && !confirmed) {
            error = strings.configScopeConfirmationRequired
            return false
        }
        busy = true
        error = null
        return try {
            val result = withContext(Dispatchers.IO) {
                if (append) {
                    backend.addConfig(repository, scope, cleanKey, value)
                } else {
                    backend.setConfig(repository, scope, cleanKey, value, oldValue)
                }
            }
            logCheck(result)
            if (!result.ok) {
                error = describe(result)
                false
            } else {
                true
            }
        } catch (error: Exception) {
            this.error = displayError(error)
            false
        } finally {
            busy = false
        }
    }

    /** Удаляет одно точное значение ключа или все значения, если [oldValue] равен null. */
    suspend fun deleteConfigEntry(
        scope: GitConfigScope,
        key: String,
        oldValue: String? = null,
        confirmed: Boolean = false,
    ): Boolean {
        val cleanKey = key.trim()
        if (!validGitConfigKey(cleanKey)) {
            error = strings.configKeyInvalid
            return false
        }
        if (requiresGitConfigScopeConfirmation(scope) && !confirmed) {
            error = strings.configScopeConfirmationRequired
            return false
        }
        busy = true
        error = null
        return try {
            val result = withContext(Dispatchers.IO) {
                backend.unsetConfig(repository, scope, cleanKey, oldValue)
            }
            logCheck(result)
            if (!result.ok) {
                error = describe(result)
                false
            } else {
                true
            }
        } catch (error: Exception) {
            this.error = displayError(error)
            false
        } finally {
            busy = false
        }
    }

    /** Читает один из файлов настройки Git из корня worktree. */
    suspend fun loadGitMetaFile(file: GitMetaFile): GitMetaFileContent? {
        val root = repository.workTree?.let(::File) ?: run {
            error = strings.gitMetaBare
            return null
        }
        busy = true
        error = null
        return try {
            val content = withContext(Dispatchers.IO) {
                if (!RepositoryPaths.exists(root, file.path)) {
                    return@withContext GitMetaFileContent(file, "", false)
                }
                if (!RepositoryPaths.isRegularFile(root, file.path)) {
                    throw RepositoryPathException("not a regular file: ${file.path}")
                }
                val loaded = RepositoryPaths.readTextLimited(
                    root,
                    file.path,
                    MAX_SAFE_TEXT_FILE_BYTES,
                )
                if (loaded.truncated) null else GitMetaFileContent(file, loaded.text, true)
            }
            if (content == null) error = strings.repoFileTooLarge
            content
        } catch (error: Exception) {
            this.error = pathError(error)
            null
        } finally {
            busy = false
        }
    }

    /** Проверяет и сохраняет .gitignore, .gitattributes или .mailmap. */
    suspend fun saveGitMetaFile(file: GitMetaFile, text: String): Boolean {
        val validation = validateGitMeta(file, text)
        if (!validation.ok) {
            error = strings.gitMetaValidationBlocked
            return false
        }
        if (text.toByteArray(Charsets.UTF_8).size > MAX_SAFE_TEXT_FILE_BYTES) {
            error = strings.repoFileTooLarge
            return false
        }
        val root = repository.workTree?.let(::File) ?: run {
            error = strings.gitMetaBare
            return false
        }
        busy = true
        error = null
        return try {
            withContext(Dispatchers.IO) {
                RepositoryPaths.writeText(root, file.path, text)
            }
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.gitMetaSaved,
                command = file.path,
                success = true,
            )
            refresh()
            true
        } catch (error: Exception) {
            this.error = pathError(error)
            false
        } finally {
            busy = false
        }
    }

    private fun configValidationMessage(error: GitConfigValidationError?): String = when (error) {
        GitConfigValidationError.KeyRequired -> strings.configKeyRequired
        GitConfigValidationError.KeyInvalid -> strings.configKeyInvalid
        GitConfigValidationError.AliasNameRequired -> strings.configAliasNameRequired
        GitConfigValidationError.AliasNameInvalid -> strings.configAliasNameInvalid
        GitConfigValidationError.BooleanInvalid -> strings.configBooleanInvalid
        GitConfigValidationError.IntegerInvalid -> strings.configIntegerInvalid
        GitConfigValidationError.ColorInvalid -> strings.configColorInvalid
        GitConfigValidationError.IncludeIfConditionInvalid -> strings.configIncludeIfInvalid
        GitConfigValidationError.IncludeIfPathRequired -> strings.configIncludeIfPathRequired
        GitConfigValidationError.ValueInvalid -> strings.configValueInvalid
        null -> strings.configValueInvalid
    }

    suspend fun removeIndexLock() {
        val lock = indexLock ?: return
        busy = true
        try {
            val removed = withContext(Dispatchers.IO) {
                runCatching { backend.removeIndexLock(repository) }.getOrDefault(false)
            }
            if (!removed) {
                error = strings.repoIndexLockDeleteFailed
                return
            }
            indexLock = null
            toast = OperationToast(
                id = ++nextToastId,
                message = strings.toastIndexLockRemoved,
                command = lock.path,
                success = true,
            )
            refresh()
        } catch (error: Exception) {
            this.error = displayError(error)
        } finally {
            busy = false
        }
    }

    fun dismissError() {
        error = null
    }

    fun dismissToast() {
        toast = null
    }

    fun clearLog() {
        log.clear()
    }

    private suspend fun keepSelection() {
        val entries = visibleEntries
        val current = entries.firstOrNull { it.path == selectedPath }

        if (current == null) {
            // Invalidate any in-flight diff before clearing the selection.
            selectionRequest++
            selectedPath = null
            diff = ""
            diffFileInfo = null
            clearMergeEditor()
            selectedHunks = emptySet()
            val first = entries.firstOrNull { it.isUnstaged } ?: entries.firstOrNull()
            if (first != null) {
                loadSelection(first, staged = first.isStaged && !first.isUnstaged)
            }
            return
        }

        val staged = when {
            current.isUnstaged -> false
            current.isStaged -> true
            else -> selectedStaged
        }
        loadSelection(current, staged)
    }

    private suspend fun loadSelection(entry: StatusEntry, staged: Boolean) {
        selectionMutex.withLock {
            loadSelectionLocked(entry, staged)
        }
    }

    private suspend fun loadSelectionLocked(entry: StatusEntry, staged: Boolean) {
        val request = ++selectionRequest
        if (selectedPath != entry.path || selectedStaged != staged) {
            selectedHunks = emptySet()
        }
        selectedPath = entry.path
        selectedStaged = staged
        if (entry.isConflicted) {
            loadMergeSelection(entry, request)
            if (request != selectionRequest) return
            diff = ""
            diffFileInfo = null
            selectedHunks = emptySet()
            return
        }
        clearMergeEditor()
        val loaded = withContext(Dispatchers.IO) { loadDiff(entry, staged) }
        if (request != selectionRequest) return
        diff = loaded.text
        diffFileInfo = loaded.fileInfo
    }

    /**
     * Несохранённый draft и выбранные строки переживают фоновый refresh.
     * Для явного перечитывания есть reloadMergeEditor().
     */
    private suspend fun loadMergeSelection(entry: StatusEntry, request: Long) {
        if (request != selectionRequest) return
        // Пока выбран тот же конфликт, не перезаписываем draft: Watcher
        // может обновлять status чаще, чем пользователь нажимает save.
        if (mergeEditor?.path == entry.path) return
        mergeEditor = null
        mergeDraft = ""
        mergeSavedText = ""
        mergeSelections = emptyMap()
        mergeParse = MergeParse(emptyList(), false)
        val loaded = withContext(Dispatchers.IO) { loadMergeEditor(entry) }
        if (request != selectionRequest) return
        mergeEditor = loaded
        mergeDraft = loaded.original
        mergeSavedText = loaded.original
        mergeSelections = emptyMap()
        mergeParse = parseConflictMarkers(loaded.original)
    }

    private fun clearMergeEditor() {
        mergeEditor = null
        mergeDraft = ""
        mergeSavedText = ""
        mergeSelections = emptyMap()
        mergeParse = MergeParse(emptyList(), false)
    }

    private fun loadMergeEditor(entry: StatusEntry): MergeEditorState {
        val path = entry.path
        val root = repository.workTree?.let(::File) ?: return MergeEditorState(
            path = path,
            base = null,
            ours = null,
            theirs = null,
            original = "",
            error = strings.repoMergeUnavailable,
        )
        val file = try {
            RepositoryPaths.resolve(root, path)
        } catch (_: RepositoryPathException) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                error = strings.repoPathUnsafe,
            )
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                error = strings.repoMergeMissingFile,
            )
        }
        if (Files.size(file) > MAX_TEXT_DIFF_BYTES) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                tooLarge = true,
                error = strings.repoMergeTooLarge,
            )
        }
        if (isKnownBinaryPath(path) || hasNulPrefix(file)) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                binary = true,
                error = strings.repoMergeBinary,
            )
        }

        val loaded = try {
            RepositoryPaths.readTextLimited(root, path, MAX_TEXT_DIFF_BYTES)
        } catch (error: Exception) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                error = pathError(error),
            )
        }
        if (loaded.truncated) {
            return MergeEditorState(
                path = path,
                base = null,
                ours = null,
                theirs = null,
                original = "",
                tooLarge = true,
                error = strings.repoMergeTooLarge,
            )
        }
        val original = loaded.text
        val base = readConflictVersion(path, 1)
        val ours = readConflictVersion(path, 2)
        val theirs = readConflictVersion(path, 3)
        val tooLarge = base.tooLarge || ours.tooLarge || theirs.tooLarge
        return MergeEditorState(
            path = path,
            base = base.text,
            ours = ours.text,
            theirs = theirs.text,
            original = original,
            tooLarge = tooLarge,
            error = if (tooLarge) strings.repoMergeTooLarge else null,
        )
    }

    private data class ConflictVersion(
        val text: String? = null,
        val tooLarge: Boolean = false,
    )

    private fun readConflictVersion(path: String, stage: Int): ConflictVersion {
        val blob: GitBlob = backend.readBlob(
            repository = repository,
            spec = ":$stage:$path",
            maxBytes = MAX_TEXT_DIFF_BYTES,
        )
        if (blob.tooLarge) return ConflictVersion(tooLarge = true)
        return ConflictVersion(text = blob.bytes?.toString(Charsets.UTF_8))
    }

    private fun loadDiffText(entry: StatusEntry, staged: Boolean): String =
        loadDiff(entry, staged).text

    /**
     * Текстовый diff остаётся строкой для parser'а; binary/image получают
     * отдельную модель с размерами и (для картинок) байтами old/new.
     */
    private fun loadDiff(entry: StatusEntry, staged: Boolean): LoadedDiff {
        if (entry.kind == StatusKind.Ignored) {
            return LoadedDiff("Ignored file — diff is not shown.")
        }

        val path = entry.path
        val image = isImagePath(path)
        val knownBinary = image || isKnownBinaryPath(path)
        val worktree = repository.workTree?.let { root ->
            runCatching { RepositoryPaths.resolve(File(root), path) }.getOrNull()
        }
        val binarySniff = !staged && worktree != null &&
            Files.isRegularFile(worktree, LinkOption.NOFOLLOW_LINKS) &&
            hasNulPrefix(worktree)
        val binaryHint = knownBinary || binarySniff
        val oldSpec = oldBlobSpec(entry, staged)
        val newSpec = if (staged) ":$path" else null
        val oldSize = oldSpec?.let(::blobSize)
        val newSize = if (staged) {
            newSpec?.let(::blobSize)
        } else {
            worktree
                ?.takeIf { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                ?.let { Files.size(it) }
        }
        val limit = when {
            image -> MAX_IMAGE_BYTES
            binaryHint -> MAX_BINARY_BYTES
            else -> MAX_TEXT_DIFF_BYTES
        }
        val tooLarge = (oldSize ?: 0) > limit || (newSize ?: 0) > limit

        if (tooLarge) {
            return LoadedDiff(
                text = "File is too large to preview (${formatSizeForNotice(oldSize, newSize)}).",
                fileInfo = buildFileInfo(
                    entry = entry,
                    staged = staged,
                    image = image,
                    binary = binaryHint,
                    oldSpec = oldSpec,
                    oldSize = oldSize,
                    newSpec = newSpec,
                    newSize = newSize,
                    tooLarge = true,
                    limit = limit,
                ),
            )
        }

        if (binaryHint) {
            return LoadedDiff(
                text = "Binary file — no text diff.",
                fileInfo = buildFileInfo(
                    entry = entry,
                    staged = staged,
                    image = image,
                    binary = true,
                    oldSpec = oldSpec,
                    oldSize = oldSize,
                    newSpec = newSpec,
                    newSize = newSize,
                    limit = limit,
                ),
            )
        }

        if (entry.kind == StatusKind.Untracked && !staged) {
            return LoadedDiff(untrackedDiff(path))
        }

        val result = backend.run(
            repository.root,
            buildList {
                add("diff")
                add("--no-color")
                add("--no-ext-diff")
                add("--unified=3")
                if (staged) add("--cached")
                add("--")
                addAll(paths(entry))
            },
            timeoutSeconds = 60,
        )
        if (!result.ok && !result.stdoutTruncated) {
            val details = result.stderr.trim().ifEmpty { result.stdout.trim() }
            return LoadedDiff("git diff failed:\n$details")
        }
        val raw = result.stdout
        if (isBinaryDiff(raw)) {
            return LoadedDiff(
                text = "Binary file — no text diff.",
                fileInfo = buildFileInfo(
                    entry = entry,
                    staged = staged,
                    image = image,
                    binary = true,
                    oldSpec = oldSpec,
                    oldSize = oldSize,
                    newSpec = newSpec,
                    newSize = newSize,
                    limit = MAX_BINARY_BYTES,
                ),
            )
        }
        if (result.stdoutTruncated || raw.length > MAX_TEXT_DIFF_BYTES) {
            return LoadedDiff(
                text = "File is too large to preview.",
                fileInfo = buildFileInfo(
                    entry = entry,
                    staged = staged,
                    image = false,
                    binary = false,
                    oldSpec = oldSpec,
                    oldSize = oldSize,
                    newSpec = newSpec,
                    newSize = newSize,
                    tooLarge = true,
                    limit = MAX_TEXT_DIFF_BYTES,
                ),
            )
        }
        return LoadedDiff(raw.ifBlank { "No changes to show." })
    }

    private fun oldBlobSpec(entry: StatusEntry, staged: Boolean): String? {
        if (entry.kind == StatusKind.Untracked) return null
        val path = entry.originalPath ?: entry.path
        return if (staged) "HEAD:$path" else ":$path"
    }

    private fun blobSize(spec: String): Long? {
        val result = backend.run(
            repository.root,
            listOf("cat-file", "-s", spec),
            timeoutSeconds = 30,
        )
        return if (result.ok) result.stdout.trim().toLongOrNull() else null
    }

    private fun buildFileInfo(
        entry: StatusEntry,
        staged: Boolean,
        image: Boolean,
        binary: Boolean,
        oldSpec: String?,
        oldSize: Long?,
        newSpec: String?,
        newSize: Long?,
        tooLarge: Boolean = false,
        limit: Long,
    ): DiffFileInfo {
        val oldPath = entry.originalPath ?: entry.path
        val oldLabel = when {
            oldSpec == null -> "missing"
            staged -> "HEAD:$oldPath"
            else -> "index:$oldPath"
        }
        val newLabel = when {
            staged -> "index:${entry.path}"
            newSize == null -> "missing"
            else -> "worktree:${entry.path}"
        }
        val oldBytes = if (image && !tooLarge) {
            oldSpec?.let { spec ->
                backend.readBlob(repository, spec, MAX_IMAGE_BYTES).bytes
            }
        } else {
            null
        }
        val newBytes = if (image && !tooLarge) {
            if (staged) {
                newSpec?.let { spec -> backend.readBlob(repository, spec, MAX_IMAGE_BYTES).bytes }
            } else {
                readFileBytes(entry.path, MAX_IMAGE_BYTES)
            }
        } else {
            null
        }

        return DiffFileInfo(
            path = entry.path,
            old = DiffVersion(
                label = oldLabel,
                size = oldSize,
                missing = oldSpec == null || oldSize == null,
                bytes = oldBytes,
            ),
            new = DiffVersion(
                label = newLabel,
                size = newSize,
                missing = newSize == null,
                bytes = newBytes,
            ),
            binary = binary,
            image = image,
            tooLarge = tooLarge,
            limitBytes = limit,
        )
    }

    private fun readFileBytes(path: String, maxBytes: Long): ByteArray? {
        val root = repository.workTree?.let(::File) ?: return null
        val file = runCatching { RepositoryPaths.resolve(root, path) }.getOrNull() ?: return null
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > maxBytes) {
            return null
        }
        return runCatching {
            val limit = maxBytes.coerceIn(0L, Int.MAX_VALUE.toLong() - 1).toInt()
            val bytes = Files.newInputStream(
                file,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS,
            ).use { it.readNBytes(limit + 1) }
            bytes.takeIf { it.size <= maxBytes }
        }.getOrNull()
    }

    private fun hasNulPrefix(file: Path): Boolean {
        return try {
            Files.newInputStream(
                file,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS,
            ).use { input ->
                val buffer = ByteArray(8192)
                val read = input.read(buffer)
                read > 0 && (0 until read).any { buffer[it] == 0.toByte() }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isBinaryDiff(raw: String): Boolean = raw.lineSequence().any { line ->
        line.startsWith("Binary files ") ||
            line == "GIT binary patch" ||
            line.startsWith("Binary file ")
    }

    private fun isImagePath(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    private fun isKnownBinaryPath(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in KNOWN_BINARY_EXTENSIONS
    }

    private fun formatSizeForNotice(oldSize: Long?, newSize: Long?): String {
        val size = newSize ?: oldSize ?: 0
        return "$size bytes"
    }

    private fun untrackedDiff(path: String): String {
        val root = repository.workTree?.let(::File)
            ?: return "Not a regular file — preview unavailable."
        val file = try {
            RepositoryPaths.resolve(root, path)
        } catch (_: RepositoryPathException) {
            return strings.repoPathUnsafe
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return "Not a regular file — preview unavailable."
        }
        if (Files.size(file) > MAX_TEXT_DIFF_BYTES) {
            return "File is too large to preview (${Files.size(file)} bytes)."
        }

        val loaded = try {
            RepositoryPaths.readTextLimited(root, path, MAX_TEXT_DIFF_BYTES)
        } catch (error: Exception) {
            return pathError(error)
        }
        if (loaded.truncated) {
            return "File is too large to preview."
        }
        val text = loaded.text
        val lines = text.lineSequence().toList()
        return buildString {
            appendLine("diff --git a/$path b/$path")
            appendLine("new file mode 100644")
            appendLine("--- /dev/null")
            appendLine("+++ b/$path")
            appendLine("@@ -0,0 +1,${lines.size} @@")
            lines.forEach { appendLine("+$it") }
        }
    }

    private suspend fun applyPartial(hunkIndex: Int, lineIndexes: Set<Int>?) {
        retryCommitArgs = null
        val hunk = parsePatch(diff).hunks.getOrNull(hunkIndex) ?: return
        val patch = buildPartialPatch(
            hunk = hunk,
            selectedLineIndexes = lineIndexes,
            unstage = selectedStaged,
        )
        if (patch.isBlank()) return

        val result = runGit(
            args = listOf("apply", "--cached", "--whitespace=nowarn", "--unidiff-zero"),
            stdin = patch,
        ) ?: return
        notify(result, if (selectedStaged) strings.toastUnstaged else strings.toastStaged)
        if (result.ok) {
            selectedHunks = emptySet()
            refresh()
        }
    }

    suspend fun countObjects() {
        runAdvanced(listOf("count-objects", "-vH"), timeoutSeconds = 120)
    }

    suspend fun fsckRepository() {
        runAdvanced(
            listOf("fsck", "--full", "--no-progress"),
            timeoutSeconds = 1_800,
        )
    }

    suspend fun gcRepository(aggressive: Boolean = false) {
        val args = buildList {
            add("gc")
            if (aggressive) add("--aggressive")
        }
        runAdvanced(args, timeoutSeconds = 1_800, refreshAfter = true)
    }

    suspend fun repackRepository() {
        runAdvanced(
            listOf("repack", "-ad"),
            timeoutSeconds = 1_800,
            refreshAfter = true,
        )
    }

    /** Prune не запускается без предварительного dry-run в этом же сеансе. */
    suspend fun previewPrune(): Boolean {
        advancedLoading = true
        prunePreview = null
        return try {
            val result = runGit(listOf("prune", "--dry-run"), timeoutSeconds = 300) ?: return false
            advancedResult = advancedResult(result)
            prunePreview = if (result.ok) advancedText(result) else null
            notify(result, strings.advancedCommandDone)
            result.ok
        } finally {
            advancedLoading = false
        }
    }

    suspend fun pruneRepository(): Boolean {
        if (prunePreview == null) {
            error = strings.advancedPrunePreviewRequired
            return false
        }
        val result = runAdvanced(listOf("prune"), timeoutSeconds = 600, refreshAfter = true)
        if (result?.ok == true) prunePreview = null
        return result?.ok == true
    }

    suspend fun loadFsmonitorConfig() {
        val result = runAdvancedDirect(listOf("config", "--get", "core.fsmonitor")) ?: return
        fsmonitorConfig = if (result.ok) result.stdout.trim().ifBlank { "unset" } else "unset"
    }

    suspend fun fsmonitor(action: String): Boolean {
        if (action !in setOf("status", "start", "stop")) return false
        return runAdvanced(
            listOf("fsmonitor--daemon", action),
            timeoutSeconds = 120,
        )?.ok == true
    }

    suspend fun setFsmonitor(enabled: Boolean): Boolean {
        val result = runAdvanced(
            listOf("config", "core.fsmonitor", if (enabled) "true" else "false"),
            timeoutSeconds = 60,
        ) ?: return false
        if (result.ok) loadFsmonitorConfig()
        return result.ok
    }

    suspend fun loadSparseState() {
        val enabled = runAdvancedDirect(listOf("config", "--bool", "core.sparseCheckout"))
        val cone = runAdvancedDirect(listOf("config", "--bool", "core.sparseCheckoutCone"))
        val patterns = if (enabled?.ok == true && enabled.stdout.trim() == "true") {
            runAdvancedDirect(listOf("sparse-checkout", "list"))
        } else {
            null
        }
        sparseState = SparseCheckoutState(
            enabled = enabled?.ok == true && enabled.stdout.trim() == "true",
            cone = cone?.ok != true || cone.stdout.trim() != "false",
            patterns = if (patterns?.ok == true) parseSparsePatterns(patterns.stdout) else emptyList(),
        )
    }

    suspend fun applySparseCheckout(rawPaths: String, cone: Boolean): Boolean {
        val patterns = parseSparsePatterns(rawPaths).map { it.replace('\\', '/') }
        if (patterns.isEmpty()) {
            error = strings.advancedSparsePathsRequired
            return false
        }
        val invalid = patterns.firstOrNull { !validSparsePath(it, cone) }
        if (invalid != null) {
            error = strings.advancedSparseInvalidPath(invalid)
            return false
        }
        val args = buildList {
            add("sparse-checkout")
            add("set")
            add(if (cone) "--cone" else "--no-cone")
            add("--")
            addAll(patterns)
        }
        val result = runAdvanced(args, timeoutSeconds = 600, refreshAfter = true) ?: return false
        if (result.ok) loadSparseState()
        return result.ok
    }

    suspend fun disableSparseCheckout(): Boolean {
        val result = runAdvanced(
            listOf("sparse-checkout", "disable"),
            timeoutSeconds = 600,
            refreshAfter = true,
        ) ?: return false
        if (result.ok) loadSparseState()
        return result.ok
    }

    suspend fun reapplySparseCheckout(): Boolean =
        runAdvanced(
            listOf("sparse-checkout", "reapply"),
            timeoutSeconds = 600,
            refreshAfter = true,
        )?.ok == true

    suspend fun loadLfsState() {
        lfsLoading = true
        try {
            val version = runAdvancedDirect(listOf("lfs", "version")) ?: return
            if (!version.ok) {
                lfsState = LfsState(
                    available = false,
                    version = (version.stderr.ifBlank { version.stdout }).trim()
                        .lineSequence().firstOrNull().orEmpty(),
                )
                return
            }
            val files = runAdvancedDirect(listOf("lfs", "ls-files"), timeoutSeconds = 120)
            lfsState = LfsState(
                available = true,
                version = version.stdout.trim().lineSequence().firstOrNull().orEmpty(),
                files = if (files?.ok == true) {
                    files.stdout.lineSequence().filter { it.isNotBlank() }.take(500).toList()
                } else {
                    emptyList()
                },
            )
        } finally {
            lfsLoading = false
        }
    }

    suspend fun installLfs(): Boolean {
        val result = runAdvanced(listOf("lfs", "install", "--local"), timeoutSeconds = 120)
            ?: return false
        if (result.ok) loadLfsState()
        return result.ok
    }

    suspend fun trackLfs(pattern: String): Boolean {
        val clean = pattern.trim().replace('\\', '/')
        if (!validLfsPattern(clean)) {
            error = strings.advancedLfsPatternInvalid
            return false
        }
        val result = runAdvanced(
            listOf("lfs", "track", clean),
            timeoutSeconds = 120,
            refreshAfter = true,
        ) ?: return false
        if (result.ok) loadLfsState()
        return result.ok
    }

    suspend fun lfsPull(): Boolean {
        val result = runAdvanced(
            listOf("lfs", "pull"),
            timeoutSeconds = 1_800,
            refreshAfter = true,
        ) ?: return false
        if (result.ok) loadLfsState()
        return result.ok
    }

    suspend fun lfsFetchAll(): Boolean {
        val result = runAdvanced(
            listOf("lfs", "fetch", "--all"),
            timeoutSeconds = 1_800,
        ) ?: return false
        if (result.ok) loadLfsState()
        return result.ok
    }

    suspend fun loadNotes() {
        notesLoading = true
        try {
            val result = runAdvancedDirect(listOf("notes", "list")) ?: return
            gitNotes = if (result.ok) parseNotesList(result.stdout) else emptyList()
        } finally {
            notesLoading = false
        }
    }

    suspend fun showNote(target: String): Boolean {
        val hash = resolveNoteObject(target) ?: return false
        val result = runAdvancedDirect(listOf("notes", "show", hash)) ?: return false
        gitNoteText = if (result.ok) result.stdout else ""
        advancedResult = advancedResult(result)
        return result.ok
    }

    suspend fun saveNote(target: String, message: String, append: Boolean): Boolean {
        val hash = resolveNoteObject(target) ?: return false
        if (message.isBlank()) {
            error = strings.advancedNoteEmpty
            return false
        }
        if (message.length > MAX_ADVANCED_NOTE_CHARS) {
            error = strings.advancedNoteTooLong
            return false
        }
        val result = runAdvanced(
            notesAddArgs(hash, message, append),
            timeoutSeconds = 120,
        ) ?: return false
        if (result.ok) {
            loadNotes()
            showNote(hash)
        }
        return result.ok
    }

    suspend fun removeNote(target: String): Boolean {
        val hash = resolveNoteObject(target) ?: return false
        val result = runAdvanced(notesRemoveArgs(hash), timeoutSeconds = 120) ?: return false
        if (result.ok) {
            gitNoteText = ""
            loadNotes()
        }
        return result.ok
    }

    suspend fun loadPartialCloneState() {
        val result = runAdvancedDirect(
            listOf(
                "config",
                "--local",
                "--get-regexp",
                "^(extensions\\.partialclone|remote\\..*\\.promisor|remote\\..*\\.partialclonefilter)$",
            ),
        )
        val details = if (result?.ok == true) {
            result.stdout.lineSequence().filter { it.isNotBlank() }.take(50).toList()
        } else {
            emptyList()
        }
        partialCloneState = PartialCloneState(
            enabled = details.any { line ->
                val value = line.substringAfter(' ', "").lowercase()
                line.startsWith("extensions.partialclone", ignoreCase = true) || value == "true"
            },
            details = details,
        )
    }

    suspend fun createArchive(ref: String, format: ArchiveFormat, output: String): Boolean {
        if (!validAdvancedRef(ref)) {
            error = strings.advancedRefInvalid
            return false
        }
        val file = advancedOutputFile(output, mustExist = false) ?: return false
        val result = runAdvanced(
            archiveArgs(ref, format, file.path),
            timeoutSeconds = 900,
        ) ?: return false
        if (result.ok) {
            advancedResult = result.copy(note = strings.advancedArchiveCreated(file.path))
            refresh()
        }
        return result.ok
    }

    suspend fun createBundle(output: String): Boolean {
        val file = advancedOutputFile(output, mustExist = false) ?: return false
        val result = runAdvanced(bundleCreateArgs(file.path), timeoutSeconds = 900) ?: return false
        if (result.ok) {
            advancedResult = result.copy(note = strings.advancedBundleCreated(file.path))
            refresh()
        }
        return result.ok
    }

    suspend fun verifyBundle(path: String): Boolean {
        val file = advancedOutputFile(path, mustExist = true) ?: return false
        return runAdvanced(bundleVerifyArgs(file.path), timeoutSeconds = 600)?.ok == true
    }

    suspend fun listBundleHeads(path: String): Boolean {
        val file = advancedOutputFile(path, mustExist = true) ?: return false
        return runAdvanced(bundleListHeadsArgs(file.path), timeoutSeconds = 120)?.ok == true
    }

    /** Разрешает цель notes в полный OID, чтобы `-`-аргумент не дошёл до Git. */
    private suspend fun resolveNoteObject(target: String): String? {
        val clean = target.trim()
        if (!validAdvancedRef(clean)) {
            error = strings.advancedNoteTargetInvalid
            return null
        }
        val result = runAdvancedDirect(listOf("rev-parse", "--verify", "--quiet", clean))
            ?: return null
        val oid = result.stdout.trim()
        if (!result.ok || !isObjectId(oid)) {
            error = strings.advancedNoteTargetInvalid
            return null
        }
        return oid
    }

    private fun advancedOutputFile(raw: String, mustExist: Boolean): File? {
        val clean = raw.trim()
        if (clean.isEmpty()) {
            error = strings.advancedOutputRequired
            return null
        }
        val file = File(clean).let { if (it.isAbsolute) it else File(repository.root, clean) }
            .absoluteFile.normalize()
        if (mustExist) {
            if (!file.isFile) {
                error = strings.advancedInputFileMissing
                return null
            }
        } else {
            if (file.exists()) {
                error = strings.advancedOutputExists(file.path)
                return null
            }
            if (file.parentFile?.isDirectory != true) {
                error = strings.advancedOutputParentMissing
                return null
            }
        }
        return file
    }

    private suspend fun runAdvanced(
        args: List<String>,
        timeoutSeconds: Long = 300,
        refreshAfter: Boolean = false,
    ): AdvancedCommandResult? {
        advancedLoading = true
        return try {
            val result = runGit(args, timeoutSeconds = timeoutSeconds) ?: return null
            val report = advancedResult(result)
            advancedResult = report
            notify(result, strings.advancedCommandDone)
            if (result.ok && refreshAfter) refresh()
            report
        } finally {
            advancedLoading = false
        }
    }

    private suspend fun runAdvancedDirect(
        args: List<String>,
        timeoutSeconds: Long = 60,
    ): GitResult? = try {
        val result = withContext(Dispatchers.IO) {
            backend.run(repository.root, args, timeoutSeconds = timeoutSeconds)
        }
        logResult(result)
        result
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        this.error = displayError(error)
        null
    }

    private fun advancedResult(result: GitResult): AdvancedCommandResult = AdvancedCommandResult(
        command = displayCommand(result),
        ok = result.ok,
        exitCode = result.exitCode,
        durationMs = result.durationMs,
        text = advancedText(result),
    )

    private fun advancedText(result: GitResult): String {
        val combined = buildString {
            if (result.stdout.isNotBlank()) append(result.stdout.trimEnd())
            if (result.stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(result.stderr.trimEnd())
            }
        }
        val visible = redactDisplayText(combined)
        return if (visible.length > MAX_ADVANCED_OUTPUT_CHARS) {
            visible.take(MAX_ADVANCED_OUTPUT_CHARS) + "\n…"
        } else {
            visible
        }
    }

    private fun beginOperation(args: List<String>): OperationRun {
        val id = ++nextOperationActivityId
        operationProgressBuffer.setLength(0)
        lastOperationProgressAt = 0L
        val control = GitRunControl { chunk -> updateOperationProgress(id, chunk) }
        activeOperationControl = control
        operationActivity = OperationActivity(
            id = id,
            command = displayCommandLine("git", args),
            startedAtMs = System.currentTimeMillis(),
        )
        return OperationRun(id, control)
    }

    private fun updateOperationProgress(id: Long, chunk: String) {
        val current = operationActivity
        if (current?.id != id || !current.running) return
        val buffered = synchronized(operationProgressBuffer) {
            operationProgressBuffer.append(chunk)
            if (operationProgressBuffer.length > MAX_PROGRESS_BUFFER_CHARS) {
                operationProgressBuffer.delete(0, operationProgressBuffer.length - MAX_PROGRESS_BUFFER_CHARS)
            }
            operationProgressBuffer.toString()
        }
        val progress = parseOperationProgress(buffered) ?: return
        if (progress == current.progress) return
        val now = System.currentTimeMillis()
        if (progress.stage == current.progress?.stage && now - lastOperationProgressAt < 120) return
        lastOperationProgressAt = now
        Snapshot.withMutableSnapshot {
            val latest = operationActivity
            if (latest?.id == id && latest.running) {
                operationActivity = latest.copy(progress = progress)
            }
        }
    }

    private fun finishOperation(run: OperationRun, result: GitResult?) {
        val current = operationActivity
        if (current?.id == run.id) {
            val now = System.currentTimeMillis()
            val duration = result?.durationMs ?: (now - current.startedAtMs).coerceAtLeast(0)
            val outcome = when {
                result == null -> OperationOutcome.Failed
                result.cancelled -> OperationOutcome.Cancelled
                result.timedOut -> OperationOutcome.TimedOut
                result.ok -> OperationOutcome.Succeeded
                else -> OperationOutcome.Failed
            }
            val showNotice = duration >= LONG_OPERATION_NOTICE_MS ||
                outcome == OperationOutcome.Cancelled ||
                outcome == OperationOutcome.TimedOut
            operationActivity = if (showNotice) {
                current.copy(
                    outcome = outcome,
                    cancelling = false,
                    durationMs = duration,
                    completedAtMs = now,
                )
            } else {
                null
            }
        }
        if (activeOperationControl === run.control) activeOperationControl = null
    }

    private suspend fun runGit(
        args: List<String>,
        stdin: String? = null,
        environment: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 300,
    ): GitResult? = operationMutex.withLock {
        runGitLocked(args, stdin, environment, timeoutSeconds)
    }

    private suspend fun runGitLocked(
        args: List<String>,
        stdin: String? = null,
        environment: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 300,
    ): GitResult? {
        busy = true
        error = null
        val run = beginOperation(args)
        var result: GitResult? = null
        return try {
            val completed = withContext(Dispatchers.IO) {
                backend.runControlled(
                    repository.root,
                    args,
                    stdin = stdin,
                    timeoutSeconds = timeoutSeconds,
                    environment = environment,
                    control = run.control,
                )
            }
            result = completed
            logResult(completed)
            if (completed.cancelled) {
                refreshRuntimeState()
            } else if (!completed.ok) {
                error = describe(completed)
                // Команда могла упасть именно из-за index.lock или незавершённой
                // операции; показываем это без ожидания следующего refresh.
                refreshRuntimeState()
            }
            completed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            this.error = displayError(error)
            refreshRuntimeState()
            null
        } finally {
            finishOperation(run, result)
            busy = false
        }
    }

    private fun loadRerereState(): RerereState {
        val cacheDir = File(repository.commonDir, "rr-cache")
        val config = runCatching {
            backend.run(
                repository.root,
                listOf("config", "--bool", "--get", "rerere.enabled"),
                timeoutSeconds = 30,
            )
        }.getOrNull()
        val enabled = config?.ok == true && config.stdout.trim().equals("true", ignoreCase = true)

        val entries = if (cacheDir.isDirectory) {
            runCatching {
                cacheDir.listFiles().orEmpty()
                    .filter { it.isDirectory }
                    .mapNotNull { entryDir ->
                        val preimage = File(entryDir, "preimage")
                        val postimage = File(entryDir, "postimage")
                        if (!preimage.isFile && !postimage.isFile) return@mapNotNull null
                        RerereEntry(
                            hash = entryDir.name,
                            preimageBytes = if (preimage.isFile) preimage.length() else 0L,
                            postimageBytes = if (postimage.isFile) postimage.length() else 0L,
                            modifiedAt = maxOf(
                                entryDir.lastModified(),
                                preimage.lastModified(),
                                postimage.lastModified(),
                            ),
                        )
                    }
                    .sortedByDescending { it.modifiedAt }
                    .take(MAX_RERERE_ENTRIES)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return RerereState(
            enabled = enabled,
            cachePath = cacheDir.path,
            entries = entries,
        )
    }

    private fun readRerereDetail(hash: String): RerereDetail? {
        if (hash == "." || hash == ".." || !hash.matches(Regex("[A-Za-z0-9._-]+"))) return null
        val entryDir = File(File(repository.commonDir, "rr-cache"), hash)
        if (!entryDir.isDirectory) return null
        val preimage = readRerereFile(File(entryDir, "preimage"))
        val postimage = readRerereFile(File(entryDir, "postimage"))
        if (preimage == null && postimage == null) return null
        return RerereDetail(
            hash = hash,
            preimage = preimage?.text,
            postimage = postimage?.text,
            preimageBytes = preimage?.bytes ?: 0L,
            postimageBytes = postimage?.bytes ?: 0L,
            preimageTruncated = preimage?.truncated == true,
            postimageTruncated = postimage?.truncated == true,
        )
    }

    private fun readRerereFile(file: File): RerereFile? {
        if (!file.isFile) return null
        return runCatching {
            val bytes = file.length()
            if (bytes > MAX_RERERE_BYTES) {
                val partial = file.inputStream().use { input ->
                    input.readNBytes(MAX_RERERE_BYTES)
                }
                RerereFile(partial.toString(Charsets.UTF_8), bytes, true)
            } else {
                RerereFile(file.readText(), bytes, false)
            }
        }.getOrNull()
    }

    private suspend fun refreshRuntimeState() {
        val runtime = withContext(Dispatchers.IO) {
            runCatching { backend.runtimeState(repository) }
                .getOrDefault(RepositoryRuntime())
        }
        operation = runtime.operation
        indexLock = runtime.indexLock
    }

    private fun publishToast(
        message: String,
        command: String,
        success: Boolean = true,
        undo: RewriteUndo? = null,
        journalAction: JournalAction? = null,
        recordJournal: Boolean = true,
    ) {
        if (success && undo != null) lastRewriteUndo = undo
        toast = OperationToast(
            id = ++nextToastId,
            message = message,
            command = command,
            success = success,
            undo = if (success) undo else null,
        )
        if (recordJournal) {
            recordJournalEntry(
                message = message,
                command = command,
                success = success,
                action = if (success) {
                    journalAction ?: undo?.let { JournalAction.Rewrite(it) }
                } else {
                    null
                },
            )
        }
    }

    /**
     * Новая операция делает redo-ветку неактуальной, но уже отменённые записи
     * остаются в журнале как история. Стеки хранят только id.
     */
    private fun recordJournalEntry(
        message: String,
        command: String,
        success: Boolean,
        action: JournalAction?,
    ) {
        val entry = JournalEntry(
            id = ++nextJournalId,
            timestampMs = System.currentTimeMillis(),
            message = message,
            command = command,
            success = success,
            action = action,
        )
        journal += entry
        if (action != null) journalUndoStack += entry.id
        journalRedoStack.clear()
        while (journal.size > MAX_JOURNAL_ENTRIES) {
            val removed = journal.removeAt(0)
            journalUndoStack.remove(removed.id)
            journalRedoStack.remove(removed.id)
        }
    }

    private fun displayCommand(result: GitResult): String =
        displayCommandLine(result.executable, result.args)

    private fun redactDisplayText(value: String): String =
        value.lineSequence().joinToString("\n") { redactSecrets(it) }

    private fun displayError(error: Throwable): String =
        redactDisplayText(error.message ?: error.toString())

    private fun notify(
        result: GitResult,
        message: String,
        undo: RewriteUndo? = null,
        journalAction: JournalAction? = null,
    ) {
        val authFailure = result.authFailureText(strings)
        publishToast(
            message = when {
                result.cancelled -> strings.toastOperationCancelled
                result.ok -> message
                authFailure != null -> strings.toastAuthFailed
                else -> strings.toastFailed
            },
            command = displayCommand(result),
            success = result.ok,
            undo = if (result.ok) undo else null,
            journalAction = journalAction,
        )
    }

    private fun logResult(result: GitResult) {
        addLog("$ ${displayCommand(result)}", LogKind.Command)
        result.stdout.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), LogKind.Output) }
        result.stderr.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), if (result.ok) LogKind.Output else LogKind.Error) }
        addLog(
            "[exit ${result.exitCode} · ${result.durationMs} ms]",
            if (result.ok) LogKind.Success else LogKind.Error,
        )
        if (result.outputTruncated) addLog(strings.gitOutputTruncated, LogKind.Info)
        recordHookOutput(result)
    }

    /**
     * Git не отделяет stdout hook-скриптов от остального вывода команды,
     * поэтому храним последнюю операцию целиком. Так вывод pre-commit,
     * commit-msg, pre-push и других hooks доступен в manager без потери.
     */
    private fun recordHookOutput(result: GitResult) {
        val output = buildString {
            if (result.stdout.isNotBlank()) append(redactDisplayText(result.stdout.trimEnd()))
            if (result.stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(redactDisplayText(result.stderr.trimEnd()))
            }
            if (result.outputTruncated) {
                if (isNotEmpty()) append('\n')
                append(strings.gitOutputTruncated)
            }
        }
        val visible = if (output.length > MAX_HOOK_OUTPUT_CHARS) {
            "…\n" + output.takeLast(MAX_HOOK_OUTPUT_CHARS - 2)
        } else {
            output
        }
        hookOutput = HookOutput(
            command = displayCommand(result),
            exitCode = result.exitCode,
            success = result.ok,
            text = visible,
        )
    }

    /** check-ignore возвращает 1 для «не игнорируется» — это не ошибка. */
    private fun logCheck(result: GitResult) {
        addLog("$ ${displayCommand(result)}", LogKind.Command)
        result.stdout.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), LogKind.Output) }
        result.stderr.lineSequence()
            .filter { it.isNotEmpty() }
            .take(120)
            .forEach { addLog(redactDisplayText(it), LogKind.Output) }
        addLog(
            "[exit ${result.exitCode} · ${result.durationMs} ms]",
            if (result.exitCode == 0 || result.exitCode == 1) LogKind.Info else LogKind.Error,
        )
    }

    private fun gitignorePattern(path: String): String? {
        if (path.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        // status paths use '/' on every platform; a backslash is a literal
        // filename character and must stay escaped, not become a separator.
        val normalized = path.trimStart('/')
        if (normalized.isBlank()) return null
        return buildString {
            append('/')
            normalized.forEach { char ->
                if (char == '\\' || char == '*' || char == '?' || char == '[' || char == ']' ||
                    char == '#' || char == '!' || char == ' '
                ) {
                    append('\\')
                }
                append(char)
            }
        }
    }

    private fun addLog(text: String, kind: LogKind) {
        log += LogLine(text, kind)
        if (log.size > MAX_LOG_LINES) {
            repeat(log.size - MAX_LOG_LINES) { log.removeAt(0) }
        }
    }

    private fun describe(result: GitResult): String = buildString {
        append(displayCommand(result))
        append(" → exit ")
        append(result.exitCode)
        if (result.timedOut) append(" (timed out)")
        if (result.outputTruncated) append(" (output truncated)")
        // Keep the original command output here; only secrets are redacted.
        // The UI adds the localized explanation and copies the command separately.
        val details = result.stderr.trim().ifEmpty { result.stdout.trim() }
        if (details.isNotEmpty()) {
            append('\n')
            append(redactDisplayText(details).take(4000))
        }
    }

    private fun paths(entry: StatusEntry): List<String> = buildList {
        entry.originalPath?.let(::add)
        add(entry.path)
    }.distinct()

    private companion object {
        const val MAX_LOG_LINES = 400
        const val MAX_JOURNAL_ENTRIES = 500
        const val MAX_PROGRESS_BUFFER_CHARS = 8 * 1024
        const val LONG_OPERATION_NOTICE_MS = 900L
        const val MAX_SIGNATURE_OUTPUT_CHARS = 64 * 1024
        const val MAX_BACKGROUND_COMMANDS = 20
        const val BACKGROUND_COMMAND_TIMEOUT_SECONDS = 600L
        const val MAX_TEXT_DIFF_BYTES = 2 * 1024 * 1024L
        const val AI_WORKTREE_DIFF_MAX_CHARS = 120_000
        const val MAX_BINARY_BYTES = 8 * 1024 * 1024L
        const val MAX_IMAGE_BYTES = 16 * 1024 * 1024L
        const val MAX_COMMIT_TEMPLATE_BYTES = 512 * 1024L
        val NONINTERACTIVE_OPERATION_ENV = mapOf(
            "GIT_EDITOR" to "true",
            "GIT_SEQUENCE_EDITOR" to "true",
            "GIT_MERGE_AUTOEDIT" to "no",
        )
        val IMAGE_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "avif",
        )
        val KNOWN_BINARY_EXTENSIONS = IMAGE_EXTENSIONS + setOf(
            "pdf", "zip", "gz", "tgz", "bz2", "xz", "7z", "rar", "tar",
            "exe", "dll", "so", "dylib", "class", "jar", "war", "apk", "aab",
            "mp3", "mp4", "m4a", "mov", "avi", "mkv", "wav", "flac", "ogg",
            "ttf", "otf", "woff", "woff2", "sqlite", "db",
        )
        val IDENTITY_PATTERN = Regex("""^(.+?)\s*<([^<>]+)>$""")
        val IDENTITY_WITH_DATE = Regex("""^(.+? <[^<>]+>)\s+\d+\s+[+-]\d{4}$""")
    }

    private data class Identity(val name: String, val email: String) {
        fun format(): String = "$name <$email>"
    }
}
