package org.baconana.mybeautifulgit_chan.i18n

/**
 * Язык интерфейса. Терминальные заголовки (START, CHANGES, LOG…) остаются
 * английскими в обоих языках — это часть визуального языка приложения.
 */
enum class Language(val code: String, val label: String, val strings: Strings) {
    EN("en", "EN", EnglishStrings),
    RU("ru", "RU", RussianStrings),
    ;

    companion object {
        fun fromCode(value: String?): Language =
            if (value?.trim()?.lowercase()?.startsWith("ru") == true) RU else EN
    }
}

/**
 * Тексты, которые меняются вместе с языком. Английский — исходный, русский —
 * перевод. Технические метки git (`git diff`, `--cached`, пути) не переводятся.
 */
interface Strings {
    val language: Language

    val open: String

    val a11yClose: String
    val a11yDismiss: String
    val a11ySearchCommands: String
    val a11yZoomIn: String
    val a11yZoomOut: String
    val a11yZoomReset: String
    fun a11yCloseRepository(name: String): String
    fun a11yOpenRecent(path: String): String
    fun a11yForgetRecent(path: String): String
    val settingsContrast: String
    val contrastNormal: String
    val contrastHigh: String

    val welcomeTagline: String
    val welcomeNoRecent: String
    fun welcomeEolWarning(major: Int, minor: Int): String
    fun welcomePartialWarning(count: Int, names: String): String
    val welcomeUpdateGit: String
    fun welcomeDontRemind(version: String): String

    val setupInstall: String
    val setupInstalling: String
    val setupInstallFailed: String
    val setupInstallNotDetected: String
    val setupNoInstallOptions: String
    val setupMissingFeatures: String
    fun setupFeatureSince(title: String, major: Int, minor: Int): String
    val setupManualPathTitle: String
    val setupManualPathHint: String
    val setupSearchTitle: String
    val setupSearchHint: String
    val setupAfterInstall: String
    val setupGitNotFound: String
    val setupNotFoundProblem: String
    fun setupTooOldProblem(
        foundMajor: Int,
        foundMinor: Int,
        minimumMajor: Int,
        minimumMinor: Int,
    ): String
    fun setupManualProblem(path: String): String

    val installWingetTitle: String
    val installWingetDetail: String
    val installPortableTitle: String
    val installPortableDetail: String
    val installDownloadTitle: String
    val installWindowsDetail: String
    val installXcodeTitle: String
    val installXcodeDetail: String
    val installBrewTitle: String
    val installBrewDetail: String
    val installMacDetail: String
    fun installPackageManagerTitle(name: String): String
    val installPackageManagerDetail: String
    val installLinuxDetail: String

    fun updateCurrent(version: String): String
    val updateHint: String
    val updateRun: String
    val updateWingetTitle: String
    val updateWingetDetail: String
    val updateBrewTitle: String
    val updateBrewDetail: String
    fun updatePackageManagerTitle(name: String): String
    val updatePackageManagerDetail: String

    val whereLabel: String
    val clonePlaceholder: String
    fun cloneWillCreate(path: String): String
    val cloneInvalidUrl: String
    val cloneInvalidTarget: String
    val folderLabel: String
    val initPlaceholder: String
    fun initHint(branch: String): String

    val pickerEmptyRecent: String
    val pickerRecentHint: String
    val pickerTargetHint: String
    val pickerPathRepo: String
    val pickerPathTarget: String
    val pickerPathInit: String
    val pickerRepoNotFound: String
    val pickerNoSubfolders: String
    val directoryNotFound: String
    val directoryNoAccess: String

    val repoBareNotice: String
    val repoNothingStaged: String
    val repoWorktreeClean: String
    val repoSelectFile: String
    val repoDiffHint: String
    val repoDiffUnified: String
    val repoDiffSplit: String
    val repoDiffIgnoreWhitespace: String
    val repoDiffIgnoreCase: String
    val repoDiffWordSyntax: String
    fun repoDiffHiddenChanges(count: Int): String
    val repoBinaryTitle: String
    val repoBinaryImageTitle: String
    val repoBinaryOld: String
    val repoBinaryNew: String
    val repoBinaryMissing: String
    val repoBinaryNoText: String
    val repoBinaryImageUnavailable: String
    val repoBinarySideBySide: String
    val repoBinaryOnion: String
    val repoBinaryOpacity: String
    val repoDiffLargeTitle: String
    fun repoDiffTooLarge(size: String, limit: String): String
    val repoMergeTitle: String
    val repoMergeConflicts: String
    val repoMergeBase: String
    val repoMergeOurs: String
    val repoMergeTheirs: String
    val repoMergeManual: String
    val repoMergeSave: String
    val repoMergeReload: String
    val repoMergeTakeAllOurs: String
    val repoMergeTakeAllTheirs: String
    val repoMergeUseOurs: String
    val repoMergeUseTheirs: String
    val repoMergeApply: String
    val repoMergeClear: String
    val repoMergeNoConflicts: String
    val repoMergeMalformed: String
    val repoMergeBinary: String
    val repoMergeTooLarge: String
    val repoMergeMissingFile: String
    val repoMergeSelectionRequired: String
    val repoMergeUnavailable: String
    val repoMergeUnsaved: String
    val repoMergeSaved: String
    val repoMergeStageHint: String
    val repoMergeManualHint: String
    val repoMergeVersionMissing: String
    val repoMergeConflict: String
    val toastMergeSaved: String
    val repoMergeSaveFailed: String
    val repoDiscard: String
    val repoAdd: String
    val repoDelete: String
    val repoDeleteConfirm: String
    val repoIgnore: String
    val repoIgnoreAlready: String
    val repoIgnoreUnavailable: String
    val repoIgnoreUnsupportedPath: String
    val repoPathUnsafe: String
    fun repoOpenMetadataBroken(path: String, missing: String): String
    val repoFileTooLarge: String
    val gitOutputTruncated: String
    val repoIgnoreVerifyFailed: String
    val repoDiscardFile: String
    val repoDiscardHunk: String
    val repoDiscardLine: String
    val repoDiscardWarning: String
    val repoDiscardFileNote: String
    val repoDiscardPartialNote: String
    val repoDiscardUntrackedNote: String
    val repoDiscardPatch: String
    val repoDiscardStash: String
    val repoDiscardConfirm: String
    val repoDiscardPatchHint: String
    val repoDiscardStashHint: String
    val repoDiscardPatchUnavailable: String
    val repoDiscardStashPartial: String
    val repoDiscardUnavailable: String
    val repoDiscardRestoreFailed: String
    val repoDiscardApplyFailed: String
    val repoStageHunk: String
    val repoUnstageHunk: String
    val repoConsoleEmpty: String
    val repoCommitSelect: String
    val commitInspectLoading: String
    val commitInspectFailed: String
    val commitDiffEmpty: String
    val commitDiffTruncated: String
    val commitStatEmpty: String
    val commitStatBinary: String
    val commitSignature: String
    val commitSignatureGood: String
    val commitSignatureUntrusted: String
    val commitSignatureBad: String
    val commitSignatureExpired: String
    val commitSignatureExpiredKey: String
    val commitSignatureRevoked: String
    val commitSignatureCannotCheck: String
    val commitSignatureNone: String
    val commitSignatureUnknown: String
    val commitSigner: String
    val commitKey: String
    val signatureVerifyCommit: String
    val signatureVerifyMerge: String
    val signatureVerifyTag: String
    val signatureVerificationRunning: String
    val signatureVerificationTitle: String
    val signatureCheckCommit: String
    val signatureCheckMergeCommit: String
    val signatureCheckMergeTag: String
    val signatureCheckTag: String
    val signatureNoOutput: String
    val signaturePassed: String
    val signatureFailed: String
    val toastSignatureVerified: String
    val toastSignatureFailed: String
    val pullVerifySignatures: String
    val pullVerifySignaturesHint: String
    val advancedCommandDone: String
    val advancedStorageHint: String
    val advancedDangerHint: String
    val advancedPruneWarning: String
    val advancedPrunePreviewRequired: String
    val advancedSparseHint: String
    val advancedSparsePathsRequired: String
    fun advancedSparseInvalidPath(path: String): String
    val advancedLfsHint: String
    val advancedLfsPatternInvalid: String
    val advancedNoLfs: String
    val advancedNotesHint: String
    val advancedNotesEmpty: String
    val advancedNoteTargetInvalid: String
    val advancedNoteEmpty: String
    val advancedNoteTooLong: String
    val advancedArchiveHint: String
    fun advancedArchiveCreated(path: String): String
    val advancedBundleHint: String
    fun advancedBundleCreated(path: String): String
    val advancedFsmonitorHint: String
    val advancedPartialHint: String
    val advancedOutputRequired: String
    fun advancedOutputExists(path: String): String
    val advancedOutputParentMissing: String
    val advancedInputFileMissing: String
    val advancedRefInvalid: String
    val clonePartialHint: String
    val cloneCancelled: String
    val commitFiles: String
    val commitActionCheckout: String
    val commitActionReset: String
    val commitActionCherryPick: String
    val commitActionRevert: String
    val commitActionBranch: String
    val commitActionTag: String
    val commitActionCopyHash: String
    val commitActionReflog: String
    val commitActionBisectGood: String
    val commitActionBisectBad: String
    val commitActionCheckoutHint: String
    val commitActionResetHint: String
    val commitActionResetHardHint: String
    val commitActionResetPreview: String
    val commitActionResetPreviewLoading: String
    val commitActionResetNoLostCommits: String
    val commitActionResetLostCommits: String
    val commitActionUnreachableObjects: String
    val commitActionFsckAlready: String
    val commitActionFsckCommits: String
    val commitActionFsckClean: String
    val commitActionFsckWarning: String
    val commitRewriteLossPreview: String
    val commitActionResetStaged: String
    val commitActionResetUnstaged: String
    val commitActionResetConflicted: String
    val commitActionResetUntracked: String
    val commitActionResetSoftPreview: String
    val commitActionResetMixedPreview: String
    val commitActionResetHardPreview: String
    val commitActionResetKeepPreview: String
    val commitActionResetKeepBlocked: String
    val commitActionResetOverlapping: String
    val commitActionResetUntrackedPreview: String
    val commitActionResetMore: String
    val commitActionRewrite: String
    val commitActionSplit: String
    val commitRewriteHint: String
    val commitRewriteMessage: String
    val commitRewriteAuthor: String
    val commitRewriteLoading: String
    val commitRewriteMode: String
    val commitRewriteModeReword: String
    val commitRewriteModeAmend: String
    val commitRewriteModeSplit: String
    val commitRewriteRewordHint: String
    val commitRewriteAmendHint: String
    val commitRewriteSplitHint: String
    val commitRewriteStart: String
    val commitRewriteCleanRequired: String
    val commitRewriteMergeUnsupported: String
    val commitRewriteRootUnsupported: String
    val commitRewriteNotInHistory: String
    val commitRewritePending: String
    val commitRewriteActiveOperation: String
    val commitRewriteBare: String
    val commitRewriteNoReplacement: String
    val commitRewriteOriginalAlreadyPresent: String
    val commitRewriteNotOnParent: String
    val commitRewriteInvalidMode: String
    val commitRewriteInvalidTree: String
    val commitRewriteBranchRequired: String
    val commitRewriteBranchChanged: String
    val commitRewriteInProgress: String
    val commitRewriteFinish: String
    val commitRewriteCancel: String
    val commitRewriteCancelWarning: String
    val commitRewriteConfirmCancel: String
    val commitRewriteAmendInstruction: String
    val commitRewriteSplitInstruction: String
    val commitRewriteNoAmend: String
    val commitActionInteractiveRebase: String
    val commitActionDelete: String
    val commitActionDeleteHint: String
    val commitActionHistoryRewrite: String
    val commitActionHistoryRewriteHint: String
    val historyRewriteTitle: String
    val historyRewriteHint: String
    val historyRewriteModeReorder: String
    val historyRewriteModeOnto: String
    val historyRewriteModeMoveRange: String
    val historyRewriteModeSquash: String
    val historyRewriteReorderHint: String
    val historyRewriteEarlier: String
    val historyRewriteLater: String
    val historyRewriteEarlierHint: String
    val historyRewriteLaterHint: String
    val historyRewriteNoEarlier: String
    val historyRewriteNoLater: String
    val historyRewriteOntoHint: String
    val historyRewriteMoveRangeHint: String
    val historyRewriteSquashHint: String
    val historyRewriteUpstream: String
    val historyRewriteOnto: String
    val historyRewriteRange: String
    val historyRewriteRangePlaceholder: String
    val historyRewriteMessage: String
    val historyRewriteMessageHint: String
    val historyRewritePreserveMerges: String
    val historyRewriteRun: String
    val historyRewriteCurrentBranch: String
    val historyRewriteRangeExcludes: String
    val historyRewriteEmptyRef: String
    val historyRewriteNotAncestor: String
    val historyRewriteEmptyRange: String
    val historyRewriteRangeNotHead: String
    val historyRewriteRangeNotInHistory: String
    val historyRewriteRangeTooShort: String
    val historyRewriteRangeMerge: String
    val historyRewriteRangeNotLinear: String
    val reorderMergeUnsupported: String
    val reorderNoParent: String
    val reorderNoChild: String
    val reorderAmbiguousChild: String
    val reorderTargetMissing: String
    val reorderNotAdjacent: String
    val toastRebaseOntoDone: String
    val deleteCommitTitle: String
    val deleteCommitHint: String
    val deleteCommitDropTitle: String
    val deleteCommitDropHint: String
    val deleteCommitDropMergeWarning: String
    val deleteCommitDropAction: String
    val deleteCommitResetTitle: String
    val deleteCommitResetHint: String
    val deleteCommitResetMergeWarning: String
    val deleteCommitResetAction: String
    val deleteCommitRevertTitle: String
    val deleteCommitRevertHint: String
    val deleteCommitRevertAction: String
    val deleteCommitFilterTitle: String
    val deleteCommitFilterHint: String
    val deleteCommitFilterAction: String
    val deleteCommitFilterConfirm: String
    val deleteCommitFilterMissing: String
    val deleteCommitFilterInstall: String
    val deleteCommitFilterBackup: String
    val deleteCommitFilterBackupFailed: String
    val deleteCommitRootNoParent: String
    val deleteCommitMergeRevert: String
    val deleteCommitNotInHistory: String
    val deleteCommitDirty: String
    val deleteCommitStashes: String
    val deleteCommitBackupNote: String
    fun deleteCommitFilterBundle(path: String): String
    fun toastFilterRepoDone(path: String): String
    val commitActionFilterRepo: String
    val commitActionFilterRepoHint: String
    val filterRepoTitle: String
    val filterRepoHint: String
    val filterRepoModeRemove: String
    val filterRepoModeReplace: String
    val filterRepoModeRename: String
    val filterRepoRemovePath: String
    val filterRepoRemovePathHint: String
    val filterRepoFind: String
    val filterRepoReplacement: String
    val filterRepoReplaceHint: String
    val filterRepoRegex: String
    val filterRepoOldPath: String
    val filterRepoNewPath: String
    val filterRepoRenameHint: String
    val filterRepoPreview: String
    val filterRepoPreviewRunning: String
    val filterRepoPreviewTitle: String
    val filterRepoPreviewHint: String
    val filterRepoChecking: String
    val filterRepoBackupNote: String
    val filterRepoConfirm: String
    val filterRepoRun: String
    val filterRepoPathRequired: String
    val filterRepoPathInvalid: String
    val filterRepoFindRequired: String
    val filterRepoRegexInvalid: String
    val filterRepoRenameRequired: String
    val filterRepoRenameInvalid: String
    val filterRepoNoRefs: String
    val filterRepoPreviewOutput: String
    val filterRepoBackupFailed: String
    val filterRepoPreviewFailed: String
    val filterRepoContentPreviewHint: String
    val filterRepoAllRefs: String
    fun filterRepoPreviewSummary(before: Int, after: Int, refs: Int, paths: Int): String
    fun filterRepoFailure(detail: String, ref: String, bundle: String): String
    fun toastFilterRepoRewritten(ref: String, bundle: String): String
    val commitActionCherryPickHint: String
    val commitActionRevertHint: String
    val commitActionRange: String
    val commitActionRangePlaceholder: String
    val commitActionRangeHint: String
    val commitActionNoCommit: String
    val commitActionNoCommitHint: String
    val commitActionInvalidRange: String
    val commitActionEmptyRange: String
    val commitActionBranchName: String
    val commitActionBranchHint: String
    val commitActionTagName: String
    val commitActionTagMessage: String
    val commitActionTagHint: String
    val tagSign: String
    val tagSignHint: String
    val tagSignNeedsMessage: String
    val tagNoCommits: String
    val tagNotFound: String
    val tagManagerTitle: String
    val tagManagerNoTags: String
    val tagManagerSelect: String
    val tagCreate: String
    val tagPush: String
    val tagPushAll: String
    val tagDeleteLocal: String
    val tagDeleteRemote: String
    val tagCreateHint: String
    val tagPushRemote: String
    val tagPushHint: String
    val tagPushAllHint: String
    val tagDeleteLocalHint: String
    val tagDeleteRemoteHint: String
    val tagAnnotated: String
    val tagLightweight: String
    val tagNoRemotes: String
    val remoteManagerTitle: String
    val remoteManagerNoRemotes: String
    val remoteManagerSelect: String
    val remoteAdd: String
    val remoteRemove: String
    val remoteEditUrl: String
    val remoteFetch: String
    val remoteName: String
    val remoteUrl: String
    val remoteAddHint: String
    val remoteRemoveHint: String
    val remoteEditUrlHint: String
    val remoteUrlCredentialsHidden: String
    val remoteFetchHint: String
    val remoteFetchPrune: String
    val remoteFetchTagsDefault: String
    val remoteFetchTags: String
    val remoteFetchNoTags: String
    val remoteFetchDepth: String
    val remoteFetchFilter: String
    val remoteFetchFilterHint: String
    val remoteHead: String
    val remoteHeadUnset: String
    val remoteHeadAuto: String
    val remoteHeadSet: String
    val remoteHeadDelete: String
    val remoteHeadAutoHint: String
    val remoteHeadSetHint: String
    val remoteHeadDeleteHint: String
    val remoteHeadBranch: String
    val remoteNoBranches: String
    val remoteInvalidName: String
    val remoteInvalidUrl: String
    val remoteAlreadyExists: String
    val remoteInvalidDepth: String
    val remoteInvalidFilter: String
    val remoteNotFound: String
    val remoteProfileLoading: String
    val remoteProfileFailed: String
    val remoteIdentityLocal: String
    val remoteIdentityUnknown: String
    val remotePreflightPermissions: String
    val remotePreflightDetached: String
    val remotePreflightUnborn: String
    val remotePreflightUnset: String
    val toastRemoteAdded: String
    val toastRemoteRemoved: String
    val toastRemoteUrlUpdated: String
    val toastFetched: String
    val toastRemoteHeadSet: String
    val toastRemoteHeadDeleted: String
    val submoduleManagerTitle: String
    val submoduleManagerNoSubmodules: String
    val submoduleManagerSelect: String
    val submoduleAll: String
    val submoduleInit: String
    val submoduleUpdate: String
    val submoduleSync: String
    val submoduleAdd: String
    val submoduleRemove: String
    val submoduleRecursive: String
    val submoduleInitOnUpdate: String
    val submoduleCheckout: String
    val submoduleCustomUpdateWarning: String
    val submoduleCustomUpdateConfirm: String
    val submodulePath: String
    val submoduleUrl: String
    val submoduleInitHint: String
    val submoduleUpdateHint: String
    val submoduleSyncHint: String
    val submoduleAddHint: String
    val submoduleRemoveHint: String
    val submoduleInvalidPath: String
    val submoduleInvalidUrl: String
    val submoduleNotFound: String
    val submoduleAlreadyExists: String
    val submoduleNone: String
    val submoduleStateCurrent: String
    val submoduleStateModified: String
    val submoduleStateUninitialized: String
    val submoduleStateConflict: String
    val toastSubmoduleInitialized: String
    val toastSubmodulesUpdated: String
    val toastSubmodulesSynced: String
    val toastSubmoduleAdded: String
    val toastSubmoduleRemoved: String
    val pushManagerTitle: String
    val pushModeCurrent: String
    val pushModeAnother: String
    val pushModeTags: String
    val pushModeAllBranches: String
    val pushModeMirror: String
    val pushModeDelete: String
    val pushModeRefspec: String
    val pushRemote: String
    val pushDestination: String
    val pushBranch: String
    val pushRefspecPlaceholder: String
    val pushForce: String
    val pushForceNone: String
    val pushForceLease: String
    val pushForceFlag: String
    val pushForceNoneHint: String
    val pushForceLeaseHint: String
    val pushForceHint: String
    val pushHintCurrent: String
    val pushHintAnother: String
    val pushHintTags: String
    val pushHintAllBranches: String
    val pushHintMirror: String
    val pushHintDelete: String
    val pushHintRefspec: String
    val pushNoCurrentBranch: String
    val pushInvalidRefspec: String
    val pushNoRemotes: String
    val pushConfirm: String
    val toastPushed: String
    val toastAllBranchesPushed: String
    val toastMirrorPushed: String
    val pullManagerTitle: String
    val pullModeMerge: String
    val pullModeRebase: String
    val pullModeFfOnly: String
    val pullRemote: String
    val pullBranch: String
    val pullCurrentBranch: String
    val pullHintMerge: String
    val pullHintRebase: String
    val pullHintFfOnly: String
    val pullLocalChangesHint: String
    val pullNoCurrentBranch: String
    val pullNoRemotes: String
    val pullConfirm: String
    val toastPulled: String
    val commitActionBisectHint: String
    val commitActionInvalidHash: String
    val commitActionInvalidRef: String
    val commitReflogEmpty: String
    val commitReflogLoading: String
    val commitReflogFailed: String
    val reflogPanelHint: String
    val reflogFilterPlaceholder: String
    val reflogNoMatches: String
    val reflogOpenCommit: String
    val reflogRestore: String
    val reflogRestoreHint: String
    val reflogCreateBranch: String
    val reflogCreateBranchHint: String
    val reflogActionHint: String
    val toastCommitCheckout: String
    val toastCommitReset: String
    val toastCommitReworded: String
    val toastCommitRewriteStarted: String
    val toastCommitRewriteFinished: String
    val toastCommitRewriteCancelled: String
    val toastCommitCherryPicked: String
    val toastCommitReverted: String
    val toastBranchCreated: String
    val toastBranchRenamed: String
    val toastBranchDeleted: String
    val toastRemoteBranchDeleted: String
    val toastBranchUpstreamSet: String
    val toastBranchPushed: String
    val toastRemoteBranchRenamed: String
    val branchManagerTitle: String
    val branchManagerLocal: String
    val branchManagerRemote: String
    val branchManagerNoBranches: String
    val branchManagerNoRemotes: String
    val branchManagerSelect: String
    val branchCreate: String
    val branchRename: String
    val branchDelete: String
    val branchSetUpstream: String
    val branchPush: String
    val branchCompare: String
    val branchCreateHint: String
    val branchRenameHint: String
    val branchDeleteHint: String
    val branchForceDelete: String
    val branchForceDeleteHint: String
    val branchRemoteDeleteHint: String
    val branchRemoteRenameHint: String
    val branchNewName: String
    val branchUpstreamName: String
    val branchUpstreamHint: String
    val branchPushRemote: String
    val branchPushName: String
    val branchPushHint: String
    val branchCompareFirst: String
    val branchCompareSecond: String
    val branchCompareHint: String
    val branchCompareLoading: String
    val branchCompareFirstOnly: String
    val branchCompareSecondOnly: String
    val branchCompareEmpty: String
    val branchNoCommits: String
    val branchCannotDeleteCurrent: String
    val branchInvalidRemote: String
    val branchNotFound: String
    val branchNameUnchanged: String
    val toastTagCreated: String
    val toastTagDeleted: String
    val toastTagPushed: String
    val toastTagsPushed: String
    val toastRemoteTagDeleted: String
    val toastBisectGood: String
    val toastBisectBad: String
    val toastBisectStarted: String
    val toastBisectSkipped: String
    val toastBisectReset: String
    fun bisectFirstBadResult(hash: String, subject: String): String
    val bisectTitle: String
    val bisectStartTitle: String
    val bisectBadRef: String
    val bisectGoodRef: String
    val bisectBadPlaceholder: String
    val bisectGoodPlaceholder: String
    val bisectStart: String
    val bisectStartHint: String
    val bisectStatusActive: String
    val bisectStatusComplete: String
    val bisectStatusIdle: String
    val bisectCurrent: String
    val bisectOriginal: String
    val bisectMarkGood: String
    val bisectMarkBad: String
    val bisectSkip: String
    val bisectReset: String
    val bisectResetHint: String
    val bisectCommand: String
    val bisectCommandPlaceholder: String
    val bisectRun: String
    val bisectStop: String
    val bisectRunning: String
    val bisectCommandHint: String
    val bisectAutoLog: String
    val bisectAutoEmpty: String
    val bisectVisualization: String
    val bisectMarksTitle: String
    val bisectNoMarks: String
    val bisectLogTitle: String
    val bisectNoState: String
    val bisectRefresh: String
    val bisectNoCommits: String
    val bisectBare: String
    val bisectAutomationRunning: String
    val bisectAlreadyActive: String
    val bisectInvalidRef: String
    val bisectSameRef: String
    val bisectNotActive: String
    val bisectComplete: String
    val bisectCommandEmpty: String
    val bisectAutomationStopped: String
    val bisectTooManySteps: String
    val bisectCommandTimedOut: String
    val bisectCommandNoExit: String
    val bisectAutomationPaused: String
    val bisectAutomationDone: String
    val logFilters: String
    val logFilterBranch: String
    val logFilterAuthor: String
    val logFilterPath: String
    val logFilterSince: String
    val logFilterUntil: String
    val logFilterMessage: String
    val logFilterPickaxe: String
    val logFilterOnlyMine: String
    val logFilterOnlyCurrent: String
    val logFilterApply: String
    val logFilterClear: String
    val logFilterNoMatch: String
    val logFilterNoIdentity: String
    val logFilterHint: String
    val logFilterBranchPlaceholder: String
    val logFilterAuthorPlaceholder: String
    val logFilterPathPlaceholder: String
    val logFilterDatePlaceholder: String
    val logFilterMessagePlaceholder: String
    val logFilterPickaxePlaceholder: String
    val repoHistory: String
    val repoBlame: String
    val searchHistoryHint: String
    val searchBlameHint: String
    val searchGrepHint: String
    val searchPathPlaceholder: String
    val searchPathOptionalPlaceholder: String
    val searchQueryPlaceholder: String
    val searchHistoryLoading: String
    val searchBlameLoading: String
    val searchGrepLoading: String
    val searchHistoryEmpty: String
    val searchBlameEmpty: String
    val searchGrepEmpty: String
    val searchHistoryFailed: String
    val searchBlameFailed: String
    val searchGrepFailed: String
    val searchPathRequired: String
    val searchQueryRequired: String
    val searchTruncated: String
    val searchCommitNotFound: String
    val searchPrompt: String
    val repoCommitMessageRequired: String
    val repoCommitOptions: String
    val repoCommitAuthor: String
    val repoCommitCommitter: String
    val repoCommitTemplate: String
    val repoCommitTemplateLoad: String
    val repoCommitTemplateHint: String
    val repoCommitTemplateRequired: String
    val repoCommitTemplateMissing: String
    val repoCommitTemplateTooLarge: String
    val repoCommitTemplateEmpty: String
    val repoCommitIdentityHint: String
    val repoCommitIdentityInvalid: String
    val repoCommitNoVerify: String
    val repoCommitMode: String
    val repoCommitModeNormal: String
    val repoCommitModeFixup: String
    val repoCommitModeSquash: String
    val repoCommitTarget: String
    val repoCommitTargetHint: String
    val repoCommitTargetRequired: String
    val repoCommitFixupAmendConflict: String
    val repoCommitRecent: String
    val repoRetryCommit: String
    val repoRetryCommitHint: String
    val repoHunksUnavailable: String
    val repoCommandPlaceholder: String
    val repoCommandHint: String
    val repoCommandEmpty: String
    val repoCommandUnclosedQuote: String
    val repoAliasesHint: String
    val repoAliasesLoading: String
    val repoAliasesEmpty: String
    val repoAliasesReload: String
    val repoAliasesRun: String
    val runnerHistory: String
    val runnerFavorites: String
    val runnerAliases: String
    val runnerJobs: String
    val runnerHistoryHint: String
    val runnerRunBackground: String
    val runnerNoHistory: String
    val runnerNoFavorites: String
    val runnerNoJobs: String
    val runnerRunning: String
    val runnerSucceeded: String
    val runnerFailed: String
    val runnerTimedOut: String
    val runnerCancelled: String
    val runnerCancelling: String
    val runnerCancel: String
    val runnerClear: String
    val runnerClearFinished: String
    val runnerBackgroundHint: String
    val runnerBackgroundStarted: String
    val repoShowIgnored: String
    val repoHideIgnored: String
    val repoOperationInProgress: String
    val repoOperationContinue: String
    val repoOperationAbort: String
    val repoOperationSkip: String
    val repoOperationConfirm: String
    val repoOperationCancel: String
    val repoOperationAbortWarning: String
    val repoOperationSkipWarning: String
    val repoOperationSkipUnsupported: String
    val repoOperationNoActive: String
    val repoOperationUnsupported: String
    val repoOperationResolveConflicts: String
    val repoOperationEditTodo: String
    val rebaseEditorPrepareTitle: String
    val rebaseEditorEditTitle: String
    val rebaseEditorBase: String
    val rebaseEditorAutosquash: String
    val rebaseEditorAutosquashHint: String
    val rebaseEditorTodo: String
    val rebaseEditorAdd: String
    val rebaseEditorEmpty: String
    val rebaseEditorLoading: String
    val rebaseEditorStart: String
    val rebaseEditorSave: String
    val rebaseEditorBackupHint: String
    val rebaseEditorEditHint: String
    val rebaseEditorCommandHint: String
    val rebaseEditorRewordMessage: String
    val rebaseEditorArgument: String
    val rebaseEditorRemove: String
    val rebaseEditorInvalidTodo: String
    val rebaseEditorMissingCommit: String
    val rebaseEditorMissingExec: String
    val rebaseEditorMissingLabel: String
    val rebaseEditorMissingMerge: String
    val rebaseEditorMissingMessage: String
    val rebaseEditorEmptyPlan: String
    val rebaseEditorPlanFailed: String
    val toastInteractiveRebaseStopped: String
    val toastInteractiveRebaseDone: String
    val repoNoCommitInProgress: String
    val repoNoCommitRemaining: String
    val repoNoCommitHint: String
    val repoNoCommitAbortWarning: String
    val rerereTitle: String
    val rerereEnabled: String
    val rerereDisabled: String
    val rerereRecorded: String
    val rerereEnable: String
    val rerereDisable: String
    val rerereClear: String
    val rerereEmpty: String
    val rerereEmptyHint: String
    val rerereSelectHint: String
    val rerereClearConfirm: String
    val rerereClearHint: String
    val rerereConfirm: String
    val rerereCancel: String
    val rererePreimage: String
    val rererePostimage: String
    val rerereMissingFile: String
    val rerereTruncated: String
    val rerereLoadFailed: String
    val rerereClearFailed: String
    val rerereConfigHint: String
    val toastRerereEnabled: String
    val toastRerereDisabled: String
    val toastRerereCleared: String
    fun stashPartialCount(count: Int): String
    val stashTitle: String
    val stashCreateTitle: String
    val stashAllChanges: String
    val stashMessagePlaceholder: String
    val stashIncludeUntracked: String
    val stashIncludeIgnored: String
    val stashStaged: String
    val stashStagedVersion: String
    val stashPartialFiles: String
    val stashNoChanges: String
    val stashPartialHint: String
    val stashCreate: String
    val stashEmpty: String
    val stashUntitled: String
    val stashSelectHint: String
    val stashApply: String
    val stashPop: String
    val stashPopConfirm: String
    val stashPopWarning: String
    val stashDrop: String
    val stashDropConfirm: String
    val stashDropWarning: String
    val stashBranchName: String
    val stashBranchPlaceholder: String
    val stashBranchAction: String
    val stashBranchHint: String
    val stashFiles: String
    val stashShowLoading: String
    val stashShowEmpty: String
    val stashShowTruncated: String
    val stashPatch: String
    val stashBare: String
    val stashPathInvalid: String
    val stashNotFound: String
    val stashBranchDirty: String
    val toastStashCreated: String
    val toastStashApplied: String
    val toastStashPopped: String
    val toastStashDropped: String
    val toastStashBranchCreated: String
    val worktreeManagerTitle: String
    val worktreeAdd: String
    val worktreePrune: String
    val worktreeOpen: String
    val worktreeOpenWindow: String
    val worktreeBranchAction: String
    val worktreeLock: String
    val worktreeUnlock: String
    val worktreeRemove: String
    val worktreeNoWorktrees: String
    val worktreeSelect: String
    val worktreePath: String
    val worktreeBranch: String
    val worktreeBase: String
    val worktreeCreateBranch: String
    val worktreeForce: String
    val worktreeLockReason: String
    val worktreeAddHint: String
    val worktreeBranchHint: String
    val worktreeRemoveHint: String
    val worktreeLockHint: String
    val worktreePruneHint: String
    val worktreePruneConfirm: String
    val worktreePrunePreview: String
    val worktreePruneNothing: String
    val worktreeCancel: String
    val worktreeCurrent: String
    val worktreeBare: String
    val worktreeDetached: String
    val worktreeLocked: String
    val worktreePrunable: String
    val worktreePathPlaceholder: String
    val worktreeBranchPlaceholder: String
    val worktreeBasePlaceholder: String
    val worktreeReasonPlaceholder: String
    val worktreeInvalidPath: String
    val worktreeAlreadyExists: String
    val worktreeInvalidBranch: String
    val worktreeInvalidBase: String
    val worktreeNotFound: String
    val worktreeCannotRemoveCurrent: String
    val worktreeCannotRemoveBare: String
    val worktreeAlreadyLocked: String
    val worktreeNotLocked: String
    val worktreeBranchBare: String
    val toastWorktreeAdded: String
    val toastWorktreeRemoved: String
    val toastWorktreeLocked: String
    val toastWorktreeUnlocked: String
    val toastWorktreeBranchChanged: String
    val toastWorktreesPruned: String
    val hookManagerTitle: String
    val hookSelect: String
    val hookNoHooks: String
    val hookFilterPlaceholder: String
    val hookEnabled: String
    val hookDisabled: String
    val hookSample: String
    val hookEnable: String
    val hookDisable: String
    val hookSave: String
    val hookReload: String
    val hookUnsaved: String
    val hookDiscard: String
    val hookCancel: String
    val hookTruncated: String
    val hookNotFound: String
    val hookTooLarge: String
    val hookSaveFailed: String
    val hookFileExists: String
    val hookPath: String
    val hookPathPlaceholder: String
    val hookPathSet: String
    val hookPathClear: String
    val hookPathEffective: String
    val hookPathInvalid: String
    val hookPathExternalWarning: String
    val hookPathConfirmationRequired: String
    val hookExternalConfirm: String
    val hookOutput: String
    val hookOutputEmpty: String
    val hookOutputClear: String
    val hookCopy: String
    val hookOutputHint: String
    val hookEditorPlaceholder: String
    val hookFile: String
    val hookExit: String
    val hookLoadFailed: String
    val hookSaved: String
    val toastHooksPathSet: String
    val toastHooksPathCleared: String
    fun patchSelectedCount(count: Int): String
    val patchesTitle: String
    val patchExportTitle: String
    val patchSelectMode: String
    val patchRangeMode: String
    val patchSelectAll: String
    val patchClearSelection: String
    val patchRange: String
    val patchRangePlaceholder: String
    val patchOutputDir: String
    val patchOutputDirPlaceholder: String
    val patchOutputFile: String
    val patchOutputFilePlaceholder: String
    val patchModeFiles: String
    val patchModeMailbox: String
    val patchCoverLetter: String
    val patchNumbered: String
    val patchSignoff: String
    val patchBinary: String
    val patchSubjectPrefix: String
    val patchSubjectPrefixPlaceholder: String
    val patchPreview: String
    val patchExport: String
    val patchPreviewHint: String
    val patchExportHint: String
    val patchMailboxTitle: String
    val patchMailboxPath: String
    val patchMailboxPathPlaceholder: String
    val patchLoad: String
    val patchMailboxEmpty: String
    val patchMailboxMessages: String
    val patchMailboxTruncated: String
    val patchRaw: String
    val patchApplyTitle: String
    val patchApplyCheck: String
    val patchApply: String
    val patchApply3way: String
    val patchAm: String
    val patchAmSignoff: String
    val patchApplyHint: String
    val patchNoCommits: String
    val patchNoSelection: String
    val patchSelectionAndRange: String
    val patchInvalidRange: String
    val patchUnknownCommit: String
    val patchInvalidPath: String
    val patchFileNotFound: String
    val patchOutputDirFailed: String
    val toastPatchesExported: String
    val toastPatchCheckPassed: String
    val toastPatchApplied: String
    val toastMailboxApplied: String
    val repoIndexLockWarning: String
    val repoIndexLockDelete: String
    val repoIndexLockDeleteFailed: String
    val toastStaged: String
    val toastUnstaged: String
    val toastStagedAll: String
    val toastUnstagedAll: String
    val toastDiscarded: String
    val toastDiscardedToStash: String
    fun toastDiscardedWithPatch(path: String): String
    val toastAdded: String
    val toastIgnored: String
    val toastCommitted: String
    val toastOperationContinued: String
    val toastOperationAborted: String
    val toastOperationSkipped: String
    val toastTemplateLoaded: String
    val toastCommandDone: String
    val toastFailed: String
    val toastIndexLockRemoved: String
    val toastCopy: String
    val toastUndo: String
    val toastRewriteUndone: String
    val toastRewriteUndoneStashed: String
    val toastUndoUnavailable: String
    val toastUndoHeadChanged: String
    val toastUndoBranchChanged: String
    val toastOperationUndone: String
    val toastOperationRedone: String
    val toastOperationRedoneStashed: String
    val toastOperationNotUndoable: String
    val toastOperationCancelled: String
    val toastRedoUnavailable: String
    val operationRunning: String
    val operationCancelling: String
    val operationCompleted: String
    val operationFailed: String
    val operationCancelled: String
    val operationTimedOut: String
    val operationCancel: String
    val journalUndo: String
    val journalRedo: String
    val journalOperations: String
    val journalEmpty: String
    val journalEmptyHint: String
    val journalOnlyLatest: String
    val journalNoUndo: String
    val journalUndoable: String
    val journalUndone: String
    val settingsGitPathHint: String
    val settingsDefaultBranchHint: String
    val settingsCopyShell: String
    val settingsCopyShellHint: String
    val settingsCloseToTray: String
    val settingsCloseToTrayHint: String
    val settingsStartAtLogin: String
    val settingsStartAtLoginHint: String
    fun settingsStartAtLoginFailed(detail: String): String
    val settingsUpdateChannel: String
    val settingsUpdateChannelChan: String
    val settingsUpdateChannelKun: String
    val settingsUpdateChannelHint: String
    val settingsUpdateCheck: String
    val settingsUpdateChecking: String
    val settingsUpdateOpen: String
    val settingsUpdateNoInstall: String
    val settingsUpdateNoReleases: String
    val settingsUpdateRepositoryUnavailable: String
    fun settingsUpdateUpToDate(current: String): String
    fun settingsUpdateAvailable(current: String, latest: String): String
    fun settingsUpdateFailed(detail: String): String
    val aiSettingsSection: String
    val aiEnabled: String
    val aiEnabledHint: String
    val aiEndpoint: String
    val aiEndpointHint: String
    val aiModel: String
    val aiApiKey: String
    val aiApiKeyHint: String
    val aiMessageLanguage: String
    val aiLanguageAuto: String
    val aiLanguageEnglish: String
    val aiLanguageRussian: String
    val aiStyle: String
    val aiStyleConventional: String
    val aiStylePlain: String
    val aiMaxDiff: String
    val aiTemperature: String
    val aiAllowLocalHttp: String
    val aiAllowLocalHttpHint: String
    val aiSecurityHint: String
    val aiEndpointInvalid: String
    val aiModelRequired: String
    val aiMaxDiffInvalid: String
    val aiTemperatureInvalid: String
    val aiGenerate: String
    val aiGenerateWorktree: String
    val aiRegenerate: String
    val aiPreparing: String
    val aiGenerating: String
    val aiCancel: String
    val aiRestore: String
    val aiSendTitle: String
    val aiSendConfirm: String
    val aiSendWarning: String
    val aiSendLocalWarning: String
    val aiSendWorktreeWarning: String
    val aiDialogEndpoint: String
    val aiDialogBranch: String
    val aiDialogPayload: String
    val aiDialogOmitted: String
    val aiDialogTruncated: String
    val aiDialogRedacted: String
    fun aiPreviewSummary(files: Int, chars: Int): String
    fun aiError(detail: String): String
    val settingsOn: String
    val settingsOff: String
    val trayQuit: String
    val settingsDefaultBranchInvalid: String
    val settingsAuthUsername: String
    val settingsAuthUsernameHint: String
    val settingsAuthToken: String
    val settingsAuthTokenHint: String
    val settingsAuthHelper: String
    val settingsAuthHelperHint: String
    val settingsAuthHelperWarning: String
    val settingsAuthSsh: String
    val settingsAuthSshAgent: String
    val settingsAuthSshKey: String
    val settingsAuthSshKeyHint: String
    val settingsAuthSshKeyRequired: String
    val settingsAuthTokenInvalid: String
    val settingsAuthHelperInvalid: String
    val authErrorTwoFactor: String
    val authErrorTokenRequired: String
    val authErrorHttps: String
    val authErrorSshKey: String
    val authErrorHostKey: String
    val toastAuthFailed: String
    val gitErrorFailed: String
    val gitErrorTimedOut: String
    val gitErrorNotRepository: String
    val gitErrorDirtyWorktree: String
    val gitErrorConflicts: String
    val gitErrorPushRejected: String
    val gitErrorNoUpstream: String
    val gitErrorRemoteNotFound: String
    val gitErrorRefNotFound: String
    val gitErrorPathNotFound: String
    val gitErrorIndexLock: String
    val gitErrorPermission: String
    val gitErrorAlreadyExists: String
    val gitErrorNotFastForward: String
    val gitErrorUnrelatedHistories: String
    val configTitle: String
    val configAll: String
    val configScopeSystem: String
    val configScopeGlobal: String
    val configScopeLocal: String
    val configScopeWorktree: String
    val configAliases: String
    val configIncludeIf: String
    val configFilterPlaceholder: String
    val configKey: String
    val configValue: String
    val configType: String
    val configOrigin: String
    val configNew: String
    val configSave: String
    val configDelete: String
    val configReload: String
    val configNoEntries: String
    val configScopeHint: String
    val configScopeConfirmation: String
    val configConfirm: String
    val configCancel: String
    val configScopeConfirmationRequired: String
    val configSaved: String
    val configDeleted: String
    val configKeyRequired: String
    val configKeyInvalid: String
    val configAliasNameRequired: String
    val configAliasNameInvalid: String
    val configBooleanInvalid: String
    val configIntegerInvalid: String
    val configColorInvalid: String
    val configIncludeIfInvalid: String
    val configIncludeIfPathRequired: String
    val gitMetaTitle: String
    val gitMetaSave: String
    val gitMetaReload: String
    val gitMetaSaved: String
    val gitMetaBare: String
    val gitMetaValidationBlocked: String
    val gitMetaMissing: String
    val gitMetaNoIssues: String
    val gitMetaUnsaved: String
    val gitMetaDiscard: String
    val gitMetaCancel: String
    val gitMetaPlaceholder: String
    val gitMetaWorktree: String
    val gitMetaLoading: String
    val gitMetaChecking: String
    val gitMetaLine: String
    val gitMetaError: String
    val gitMetaWarning: String
    val gitMetaCheck: String
    val gitMetaIssueIgnoreEmptyNegation: String
    val gitMetaIssueIgnoreTrailingSpace: String
    val gitMetaIssueTrailingEscape: String
    val gitMetaIssueUnclosedClass: String
    val gitMetaIssueAttributesMissing: String
    val gitMetaIssueAttributesNameInvalid: String
    val gitMetaIssueAttributesDuplicate: String
    val gitMetaIssueAttributesConflict: String
    val gitMetaIssueMailmapEmailRequired: String
    val gitMetaIssueMailmapEmailInvalid: String
    val gitMetaIssueMailmapTooManyEmails: String
    val gitMetaIssueMailmapUnexpectedText: String
    val gitMetaIssueMailmapDuplicate: String
    val gitMetaIssueMailmapUnclosed: String
    val configValueInvalid: String
    val configTypeString: String
    val configTypeBoolean: String
    val configTypeInteger: String
    val configTypeColor: String
    val configTypePath: String
    val configTypeBooleanOrNamed: String
}

object EnglishStrings : Strings {
    override val language = Language.EN

    override val open = "Open"

    override val a11yClose = "Close"
    override val a11yDismiss = "Dismiss"
    override val a11ySearchCommands = "Search commands"
    override val a11yZoomIn = "Zoom in"
    override val a11yZoomOut = "Zoom out"
    override val a11yZoomReset = "Reset interface scale"
    override fun a11yCloseRepository(name: String) = "Close repository $name"
    override fun a11yOpenRecent(path: String) = "Open recent repository $path"
    override fun a11yForgetRecent(path: String) = "Forget recent repository $path"
    override val settingsContrast = "contrast"
    override val contrastNormal = "normal"
    override val contrastHigh = "high"

    override val welcomeTagline = "all of git. no babysitting. with undo."
    override val welcomeNoRecent =
        "no recent repositories — open a folder or drop it into the window"

    override fun welcomeEolWarning(major: Int, minor: Int) =
        "git $major.$minor no longer receives upstream patches. On an LTS distro, " +
            "check that the git package is up to date."

    override fun welcomePartialWarning(count: Int, names: String) =
        "some features unavailable ($count): $names"
    override val welcomeUpdateGit = "update git"
    override fun welcomeDontRemind(version: String) = "don't remind for $version"

    override val setupInstall = "INSTALL"
    override val setupInstalling = "INSTALLING…"
    override val setupInstallFailed = "Installation failed."
    override val setupInstallNotDetected =
        "The installer finished, but git was not detected. Restart the app or set the path manually."
    override val setupNoInstallOptions =
        "Open https://git-scm.com/downloads and install git manually."
    override val setupMissingFeatures = "MISSING IN THIS VERSION"

    override fun setupFeatureSince(title: String, major: Int, minor: Int) =
        "· $title  (since $major.$minor)"

    override val setupManualPathTitle = "SPECIFY PATH MANUALLY"
    override val setupManualPathHint = "Git executable, or the folder containing it."
    override val setupSearchTitle = "SEARCH"
    override val setupSearchHint =
        "PATH → standard Windows / macOS / Linux locations → verify by running `git --version`."
    override val setupAfterInstall =
        "After installing, press CHECK PATH — the app will re-check git and open Welcome."
    override val setupGitNotFound = "System git was not found."
    override val setupNotFoundProblem =
        "Git was not found. Install it or point to the executable manually."

    override fun setupTooOldProblem(
        foundMajor: Int,
        foundMinor: Int,
        minimumMajor: Int,
        minimumMinor: Int,
    ) = "Git $foundMajor.$foundMinor is older than the minimum $minimumMajor.$minimumMinor."

    override fun setupManualProblem(path: String) =
        "Could not run git at «$path». Point to the git executable or the folder containing it."

    override val installWingetTitle = "Install with winget"
    override val installWingetDetail = "winget will request administrator elevation."
    override val installPortableTitle = "PortableGit — no admin rights"
    override val installPortableDetail =
        "Download PortableGit from the Git for Windows page, unpack it anywhere and point " +
            "the app to git.exe."
    override val installDownloadTitle = "Open download page"
    override val installWindowsDetail = "Official Git for Windows installer."
    override val installXcodeTitle = "Install Command Line Tools"
    override val installXcodeDetail =
        "xcode-select installs the Command Line Tools, including git. A system dialog will open."
    override val installBrewTitle = "Install with Homebrew"
    override val installBrewDetail =
        "User-level install without admin rights. Homebrew is already available."
    override val installMacDetail = "Official git installer for macOS."

    override fun installPackageManagerTitle(name: String) = "Install with $name"

    override val installPackageManagerDetail =
        "Distribution package manager. Administrator rights required."
    override val installLinuxDetail = "Installation instructions for Linux distributions."

    override fun updateCurrent(version: String) = "installed: $version"
    override val updateHint =
        "Choose an explicit update. Nothing runs until you press UPDATE."
    override val updateRun = "UPDATE"
    override val updateWingetTitle = "Update with winget"
    override val updateWingetDetail =
        "winget will request administrator elevation and upgrade Git.Git."
    override val updateBrewTitle = "Update with Homebrew"
    override val updateBrewDetail = "Homebrew is already available; no admin rights."
    override fun updatePackageManagerTitle(name: String) = "Update with $name"
    override val updatePackageManagerDetail =
        "The distribution package manager will update git. Administrator rights required."

    override val whereLabel = "WHERE"
    override val clonePlaceholder = "folder or future path"

    override fun cloneWillCreate(path: String) = "will create: $path"
    override val cloneInvalidUrl =
        "Clone URL must not start with '-' and must use http, https, ssh, git, or a local path."
    override val cloneInvalidTarget =
        "Choose an existing folder or a path without '.'/'..' segments."

    override val folderLabel = "FOLDER"
    override val initPlaceholder = "path to a new or existing folder"
    override fun initHint(branch: String) = "git init -b $branch · the folder may not exist"

    override val pickerEmptyRecent = "empty for now"
    override val pickerRecentHint =
        "History and drag & drop are faster\nthan walking folders."
    override val pickerTargetHint = "Choosing a folder for the current operation."
    override val pickerPathRepo = "path to a folder or repository"
    override val pickerPathTarget = "folder or future path"
    override val pickerPathInit = "path to a new or existing folder"
    override val pickerRepoNotFound =
        "git repository not found here — we'll open the folder and git will check."
    override val pickerNoSubfolders = "no subfolders"
    override val directoryNotFound = "Folder not found or not a directory."
    override val directoryNoAccess = "Cannot read folder contents."

    override val repoBareNotice =
        "a bare repository has no working tree — changes and commit are unavailable; " +
            "history and refs still work."
    override val repoNothingStaged = "nothing staged"
    override val repoWorktreeClean = "working tree clean"
    override val repoSelectFile = "select a file on the left"
    override val repoDiffHint = "hunk / line · git apply --cached"
    override val repoDiffUnified = "unified"
    override val repoDiffSplit = "split"
    override val repoDiffIgnoreWhitespace = "ws"
    override val repoDiffIgnoreCase = "case"
    override val repoDiffWordSyntax = "word · syntax"
    override fun repoDiffHiddenChanges(count: Int) =
        "$count change${if (count == 1) "" else "s"} hidden by whitespace/case filter"
    override val repoBinaryTitle = "BINARY FILE"
    override val repoBinaryImageTitle = "IMAGE DIFF"
    override val repoBinaryOld = "old"
    override val repoBinaryNew = "new"
    override val repoBinaryMissing = "missing"
    override val repoBinaryNoText = "binary content — text diff unavailable"
    override val repoBinaryImageUnavailable = "image could not be decoded"
    override val repoBinarySideBySide = "side-by-side"
    override val repoBinaryOnion = "onion"
    override val repoBinaryOpacity = "opacity"
    override val repoDiffLargeTitle = "LARGE FILE"
    override fun repoDiffTooLarge(size: String, limit: String) =
        "Preview limit is $limit; this side is $size. The diff is not loaded."
    override val repoMergeTitle = "MERGE EDITOR"
    override val repoMergeConflicts = "conflicts"
    override val repoMergeBase = "base"
    override val repoMergeOurs = "ours"
    override val repoMergeTheirs = "theirs"
    override val repoMergeManual = "manual"
    override val repoMergeSave = "save markers"
    override val repoMergeReload = "reload"
    override val repoMergeTakeAllOurs = "take all ours"
    override val repoMergeTakeAllTheirs = "take all theirs"
    override val repoMergeUseOurs = "use ours"
    override val repoMergeUseTheirs = "use theirs"
    override val repoMergeApply = "apply selected"
    override val repoMergeClear = "clear"
    override val repoMergeNoConflicts = "no conflict markers — save to keep the resolved file"
    override val repoMergeMalformed = "some conflict markers are incomplete; fix them manually"
    override val repoMergeBinary = "binary conflict — resolve it in an external editor"
    override val repoMergeTooLarge = "conflict file is too large for the merge editor"
    override val repoMergeMissingFile = "conflicting file is missing in the worktree"
    override val repoMergeSelectionRequired = "select at least one line or a side"
    override val repoMergeUnavailable = "merge editor is unavailable for this file"
    override val repoMergeUnsaved = "unsaved"
    override val repoMergeSaved = "saved"
    override val repoMergeStageHint = "save, then stage the file to mark the conflict resolved"
    override val repoMergeManualHint =
        "edit the full file; conflict markers are kept unless you remove them"
    override val repoMergeVersionMissing = "missing in index"
    override val repoMergeConflict = "conflict"
    override val toastMergeSaved = "merge file saved"
    override val repoMergeSaveFailed = "could not save merge file"
    override val repoDiscard = "discard"
    override val repoAdd = "add"
    override val repoDelete = "delete"
    override val repoDeleteConfirm = "DELETE"
    override val repoIgnore = "ignore"
    override val repoIgnoreAlready = "already ignored by git"
    override val repoIgnoreUnavailable = "Only untracked files can be added to .gitignore."
    override val repoIgnoreUnsupportedPath =
        "This path contains characters that .gitignore cannot represent safely."
    override val repoPathUnsafe =
        "Blocked: the path is a symlink or leaves the repository."
    override fun repoOpenMetadataBroken(path: String, missing: String) =
        "Git cannot open this repository because its metadata is incomplete.\n" +
            "$path\n" +
            "Missing or unreadable: $missing\n" +
            "Restore it from a backup or clone the repository again. Working files were not changed."
    override val repoFileTooLarge =
        "The file is too large to open safely."
    override val gitOutputTruncated =
        "Output was truncated at the safety limit."
    override val repoIgnoreVerifyFailed =
        "Could not verify the new .gitignore rule with git check-ignore; the file was restored."
    override val repoDiscardFile = "file"
    override val repoDiscardHunk = "hunk"
    override val repoDiscardLine = "line"
    override val repoDiscardWarning =
        "This removes changes from the working tree. It cannot be undone unless you save a patch or stash first."
    override val repoDiscardFileNote =
        "Only worktree changes are discarded; staged changes stay in the index."
    override val repoDiscardPartialNote =
        "Only the selected change is reverted; the rest of the file is preserved."
    override val repoDiscardUntrackedNote = "This untracked file will be deleted."
    override val repoDiscardPatch = "save patch"
    override val repoDiscardStash = "stash"
    override val repoDiscardConfirm = "DISCARD"
    override val repoDiscardPatchHint =
        "The patch is written to .git/chan-backups and can be restored with git apply."
    override val repoDiscardStashHint = "Stash keeps a recoverable copy of the file changes."
    override val repoDiscardPatchUnavailable =
        "Cannot create a text patch for this file. Use stash or discard without a patch."
    override val repoDiscardStashPartial =
        "Stash backup is available for whole files only. Use a patch for a hunk or line."
    override val repoDiscardUnavailable =
        "Discard is unavailable for this entry (conflict, rename, submodule, or no unstaged changes)."
    override val repoDiscardRestoreFailed =
        "Could not restore the original worktree after a failed discard. Check the console and the saved patch."
    override val repoDiscardApplyFailed =
        "Could not discard the selected change; the worktree was restored."
    override val repoStageHunk = "stage hunk"
    override val repoUnstageHunk = "unstage hunk"
    override val repoConsoleEmpty = "every executed git command will appear here"
    override val repoCommitSelect = "select a commit"
    override val commitInspectLoading = "reading commit…"
    override val commitInspectFailed = "could not read commit"
    override val commitDiffEmpty = "no textual diff (empty commit or binary file)"
    override val commitDiffTruncated = "diff truncated at the size limit; stats are complete"
    override val commitStatEmpty = "no files changed"
    override val commitStatBinary = "binary"
    override val commitSignature = "SIGNATURE"
    override val commitSignatureGood = "verified · good signature"
    override val commitSignatureUntrusted = "good signature · untrusted key"
    override val commitSignatureBad = "BAD signature"
    override val commitSignatureExpired = "good signature · expired"
    override val commitSignatureExpiredKey = "good signature · expired key"
    override val commitSignatureRevoked = "REVOKED key"
    override val commitSignatureCannotCheck = "cannot check signature"
    override val commitSignatureNone = "unsigned"
    override val commitSignatureUnknown = "signature status unknown"
    override val commitSigner = "SIGNER"
    override val commitKey = "KEY"
    override val signatureVerifyCommit = "verify commit"
    override val signatureVerifyMerge = "verify merge"
    override val signatureVerifyTag = "verify tag"
    override val signatureVerificationRunning = "verifying signature…"
    override val signatureVerificationTitle = "SIGNATURE VERIFICATION"
    override val signatureCheckCommit = "commit signature"
    override val signatureCheckMergeCommit = "merge commit signature"
    override val signatureCheckMergeTag = "embedded merge tag"
    override val signatureCheckTag = "tag signature"
    override val signatureNoOutput = "git returned no verification details"
    override val signaturePassed = "VERIFIED"
    override val signatureFailed = "NOT VERIFIED"
    override val toastSignatureVerified = "signature verified"
    override val toastSignatureFailed = "signature verification failed"
    override val pullVerifySignatures = "verify signatures"
    override val pullVerifySignaturesHint =
        "abort the merge unless the source tip has a valid, trusted signature"
    override val advancedCommandDone = "advanced command finished"
    override val advancedStorageHint =
        "count-objects/fsck are read-only. gc, repack and prune rewrite .git objects; commits stay recoverable through reflog, but unreachable objects can be removed."
    override val advancedDangerHint =
        "This command changes repository storage. Read the exact command and result below."
    override val advancedPruneWarning =
        "prune deletes unreachable objects. Run the dry-run preview first; the button remains disabled until it succeeds."
    override val advancedPrunePreviewRequired =
        "run the prune dry-run preview first"
    override val advancedSparseHint =
        "Sparse-checkout changes which files are present in the working tree; commit history and the index are not rewritten."
    override val advancedSparsePathsRequired = "enter at least one sparse path or pattern"
    override fun advancedSparseInvalidPath(path: String): String =
        "unsafe sparse path: $path (relative paths only; no .., absolute drive or patterns in cone mode)"
    override val advancedLfsHint =
        "Requires the git-lfs extension. Track updates .gitattributes; pull downloads LFS objects; fetch only downloads them into .git/lfs."
    override val advancedLfsPatternInvalid = "invalid LFS path or pattern"
    override val advancedNoLfs = "git-lfs is not installed or is not on PATH"
    override val advancedNotesHint =
        "Notes live in refs/notes/commits. SAVE replaces an existing note for the object; APPEND adds another paragraph."
    override val advancedNotesEmpty = "no notes in refs/notes/commits"
    override val advancedNoteTargetInvalid = "note target must resolve to a Git object"
    override val advancedNoteEmpty = "note text is empty"
    override val advancedNoteTooLong = "note is too long (limit 8000 characters)"
    override val advancedArchiveHint =
        "Archives the selected revision to a new ZIP or TAR.GZ file. Existing files are never overwritten."
    override fun advancedArchiveCreated(path: String): String = "archive written: $path"
    override val advancedBundleHint =
        "CREATE packs all refs into a new bundle. VERIFY and LIST HEADS read an existing bundle without changing the repository."
    override fun advancedBundleCreated(path: String): String = "bundle written: $path"
    override val advancedFsmonitorHint =
        "core.fsmonitor enables the built-in filesystem monitor where Git supports it (Windows/macOS). STATUS, START and STOP control the daemon."
    override val advancedPartialHint =
        "A partial clone is selected when cloning. For an existing repository use a filter while fetching from the remote manager."
    override val advancedOutputRequired = "output path is required"
    override fun advancedOutputExists(path: String): String =
        "file already exists and will not be overwritten: $path"
    override val advancedOutputParentMissing = "output directory does not exist"
    override val advancedInputFileMissing = "file does not exist"
    override val advancedRefInvalid = "invalid revision; whitespace and a leading dash are not allowed"
    override val clonePartialHint =
        "Partial clone downloads file contents on demand. BLOB:NONE avoids all blobs until needed; TREE:0 omits trees as well."
    override val cloneCancelled = "clone cancelled; partial files may remain in the target folder"
    override val commitFiles = "FILES"
    override val commitActionCheckout = "checkout"
    override val commitActionReset = "reset"
    override val commitActionCherryPick = "cherry-pick"
    override val commitActionRevert = "revert"
    override val commitActionBranch = "branch"
    override val commitActionTag = "tag"
    override val commitActionCopyHash = "copy hash"
    override val commitActionReflog = "show in reflog"
    override val commitActionBisectGood = "bisect good"
    override val commitActionBisectBad = "bisect bad"
    override val commitActionCheckoutHint =
        "Checkout detaches HEAD at this commit; local changes may block the checkout."
    override val commitActionResetHint =
        "Moves the current branch to this commit. Soft keeps index/worktree, mixed resets index, hard resets both, keep preserves local changes and aborts on overlap."
    override val commitActionResetHardHint =
        "Hard reset discards uncommitted tracked changes. Commits can be recovered from reflog; uncommitted changes cannot."
    override val commitActionResetPreview = "LOSS PREVIEW"
    override val commitActionResetPreviewLoading = "checking what will be lost…"
    override val commitActionResetNoLostCommits = "no commits will become unreachable"
    override val commitActionResetLostCommits = "commits leaving the branch (reflog/backup keeps them)"
    override val commitActionUnreachableObjects = "unique objects in the lost side (rev-list)"
    override val commitActionFsckAlready = "already unreachable per fsck (no reflogs)"
    override val commitActionFsckCommits = "commits"
    override val commitActionFsckClean = "fsck: no unreachable objects"
    override val commitActionFsckWarning = "fsck failed"
    override val commitRewriteLossPreview = "LEAVING THE BRANCH"
    override val commitActionResetStaged = "staged"
    override val commitActionResetUnstaged = "unstaged"
    override val commitActionResetConflicted = "conflicted"
    override val commitActionResetUntracked = "untracked"
    override val commitActionResetSoftPreview =
        "Index and working tree stay untouched; only the branch moves."
    override val commitActionResetMixedPreview =
        "Working tree stays untouched; staged changes are reset."
    override val commitActionResetHardPreview =
        "Uncommitted tracked changes will be discarded."
    override val commitActionResetKeepPreview =
        "Local changes are kept; reset aborts if they overlap the target."
    override val commitActionResetKeepBlocked =
        "Reset will abort: local changes overlap files changed by this reset."
    override val commitActionResetOverlapping = "overlapping"
    override val commitActionResetUntrackedPreview =
        "Untracked files are kept unless they are in the way of the target."
    override val commitActionResetMore = "more"
    override val commitActionRewrite = "amend / reword"
    override val commitActionSplit = "split commit"
    override val commitRewriteHint =
        "Rewrites the selected commit and replays its descendants. The working tree must be clean; descendant merge commits are not supported. Split/amend require a normal target commit. The old tip remains in reflog."
    override val commitRewriteMessage = "message"
    override val commitRewriteAuthor = "author (Name <email>)"
    override val commitRewriteLoading = "loading commit message…"
    override val commitRewriteMode = "mode"
    override val commitRewriteModeReword = "reword"
    override val commitRewriteModeAmend = "amend"
    override val commitRewriteModeSplit = "split"
    override val commitRewriteRewordHint =
        "Changes only the message and author; the commit tree stays the same."
    override val commitRewriteAmendHint =
        "Starts an editable amend: target changes are staged and message/author are prefilled. Commit the replacement, then finish."
    override val commitRewriteSplitHint =
        "Starts edit + reset: target changes are left unstaged. Create the split commits, then finish."
    override val commitRewriteStart = "START"
    override val commitRewriteCleanRequired =
        "Commit or stash all changes before rewriting history."
    override val commitRewriteMergeUnsupported =
        "Merge commits are not supported for split/amend, and merge descendants are not supported yet."
    override val commitRewriteRootUnsupported =
        "The root commit cannot be split or amended; reword is available."
    override val commitRewriteNotInHistory = "The commit is not in the current HEAD history."
    override val commitRewritePending = "Finish or cancel the current rewrite first."
    override val commitRewriteActiveOperation = "Finish the active git operation first."
    override val commitRewriteBare = "History rewrite is unavailable for a bare repository."
    override val commitRewriteNoReplacement =
        "Create at least one replacement commit before finishing."
    override val commitRewriteOriginalAlreadyPresent =
        "The branch already contains the original tip; reset to the replacement commits or cancel the rewrite."
    override val commitRewriteNotOnParent =
        "The current branch is not based on the rewrite parent."
    override val commitRewriteInvalidMode = "Use reword for a direct rewrite."
    override val commitRewriteInvalidTree = "Cannot read the commit tree for rewrite."
    override val commitRewriteBranchRequired =
        "Switch to a local branch before rewriting history."
    override val commitRewriteBranchChanged =
        "The current branch changed; return to the original branch before finishing or cancelling."
    override val commitRewriteInProgress = "REWRITE IN PROGRESS"
    override val commitRewriteFinish = "FINISH"
    override val commitRewriteCancel = "CANCEL"
    override val commitRewriteCancelWarning =
        "Cancel restores the original tip and discards replacement commits and uncommitted changes."
    override val commitRewriteConfirmCancel = "CONFIRM CANCEL"
    override val commitRewriteAmendInstruction =
        "Stage changes and commit the amended commit, then finish."
    override val commitRewriteSplitInstruction =
        "Stage and commit the replacement pieces, then finish."
    override val commitRewriteNoAmend =
        "Use a normal commit while a rewrite is in progress; do not amend the parent commit."
    override val commitActionInteractiveRebase = "interactive rebase from here"
    override val commitActionDelete = "delete commit"
    override val commitActionDeleteHint =
        "Choose drop, reset --hard, revert or filter-repo; each method explains what it rewrites and what it preserves."
    override val commitActionHistoryRewrite = "history rewrite / move range"
    override val commitActionHistoryRewriteHint =
        "Reorder adjacent commits, run git rebase --onto, move a range or squash a range; history is rewritten with a backup and Undo."
    override val historyRewriteTitle = "HISTORY REWRITE"
    override val historyRewriteHint =
        "All operations rewrite the current branch. A backup ref is created before the first mutation."
    override val historyRewriteModeReorder = "REORDER"
    override val historyRewriteModeOnto = "REBASE --ONTO"
    override val historyRewriteModeMoveRange = "MOVE RANGE"
    override val historyRewriteModeSquash = "SQUASH RANGE"
    override val historyRewriteReorderHint =
        "Swap this commit with its adjacent parent (earlier) or child (later). For arbitrary order use the interactive rebase editor with drag-and-drop."
    override val historyRewriteEarlier = "MOVE EARLIER"
    override val historyRewriteLater = "MOVE LATER"
    override val historyRewriteEarlierHint = "Swap with the parent; descendants are replayed. Conflicts are possible."
    override val historyRewriteLaterHint = "Swap with the child on the current branch; later commits are replayed."
    override val historyRewriteNoEarlier = "No parent: this commit cannot move earlier."
    override val historyRewriteNoLater = "No single child on the current branch: this commit cannot move later."
    override val historyRewriteOntoHint =
        "Replays commits after upstream (exclusive) onto a new base. Current branch only; merge commits are linearized unless preserve merges is on."
    override val historyRewriteMoveRangeHint =
        "Moves commits after the range start through current HEAD onto a new base. The range must end at HEAD; A..B excludes A."
    override val historyRewriteSquashHint =
        "Combines A..B into one commit (A excluded; use A^..B to include A). Later commits are replayed; merge commits in the range are rejected."
    override val historyRewriteUpstream = "upstream (excluded)"
    override val historyRewriteOnto = "new base (onto)"
    override val historyRewriteRange = "range A..B"
    override val historyRewriteRangePlaceholder = "A..B (A is excluded)"
    override val historyRewriteMessage = "squash message (optional)"
    override val historyRewriteMessageHint = "Empty keeps the first commit's message."
    override val historyRewritePreserveMerges = "preserve merges (--rebase-merges)"
    override val historyRewriteRun = "RUN"
    override val historyRewriteCurrentBranch = "current branch"
    override val historyRewriteRangeExcludes = "A..B excludes A; use A^..B to include A."
    override val historyRewriteEmptyRef = "Upstream and new base must not be empty."
    override val historyRewriteNotAncestor = "Upstream is not an ancestor of the current branch."
    override val historyRewriteEmptyRange = "There are no commits after upstream to replay."
    override val historyRewriteRangeNotHead = "Move range requires the range end to be the current HEAD."
    override val historyRewriteRangeNotInHistory = "The range endpoints are not in the current history."
    override val historyRewriteRangeTooShort = "Squash range needs at least two commits."
    override val historyRewriteRangeMerge = "Squash range does not support merge commits."
    override val historyRewriteRangeNotLinear = "The range is not a contiguous pick sequence in the rebase plan."
    override val reorderMergeUnsupported = "Reordering a merge commit is not supported; use the interactive rebase editor."
    override val reorderNoParent = "This commit has no parent to swap with."
    override val reorderNoChild = "This commit has no child on the current branch to swap with."
    override val reorderAmbiguousChild = "This commit has multiple children on the current branch; reorder is ambiguous."
    override val reorderTargetMissing = "The adjacent commit is not present in the current rebase plan."
    override val reorderNotAdjacent = "Only adjacent commits can be swapped from this dialog."
    override val toastRebaseOntoDone = "rebase --onto complete"
    override val deleteCommitTitle = "DELETE COMMIT"
    override val deleteCommitHint =
        "The same commit can be removed in four different ways. Pick the one whose consequences match your history."
    override val deleteCommitDropTitle = "drop in interactive rebase"
    override val deleteCommitDropHint =
        "Keeps the descendants by replaying them on the parent. History is rewritten and conflicts are possible; a backup ref and Undo are created."
    override val deleteCommitDropMergeWarning =
        "A merge is a `merge -C` line in --rebase-merges, not a pick line. Removing that line would implicitly drop the side-branch picks, so drop is disabled for merges; use reset or filter-repo."
    override val deleteCommitDropAction = "OPEN REBASE TODO"
    override val deleteCommitResetTitle = "reset --hard to parent (cut the tail)"
    override val deleteCommitResetHint =
        "Moves the current branch to the parent, deleting this commit and every descendant. Uncommitted tracked changes are lost (untracked files remain); a backup ref and Undo restore the branch tip, not the working tree."
    override val deleteCommitResetMergeWarning =
        "This is a merge commit: reset --hard uses the first parent and discards the other parent's line together with all descendants."
    override val deleteCommitResetAction = "RESET --HARD"
    override val deleteCommitRevertTitle = "revert (no rewrite)"
    override val deleteCommitRevertHint =
        "Creates a new commit with the inverse diff; safe for history that is already pushed. A merge commit needs an explicit -m mainline."
    override val deleteCommitRevertAction = "REVERT"
    override val deleteCommitFilterTitle = "filter-repo (whole history)"
    override val deleteCommitFilterHint =
        "Rewrites every ref and tag, removing the commit from all local history; origin remotes are removed and reflogs/old objects are pruned. fast-export works on trees, not diffs, so a descendant may keep the same file contents. Use drop/rebase to remove a change from one branch."
    override val deleteCommitFilterAction = "RUN FILTER-REPO"
    override val deleteCommitFilterConfirm = "I understand: the rewrite is irreversible"
    override val deleteCommitFilterMissing = "git filter-repo is not installed."
    override val deleteCommitFilterInstall =
        "Install it with pip install git-filter-repo (or your package manager), then reopen this dialog."
    override val deleteCommitFilterBackup = "A bundle backup is created before the rewrite:"
    override val deleteCommitFilterBackupFailed =
        "Could not create a backup bundle; filter-repo was not started."
    override val deleteCommitRootNoParent =
        "A root commit has no parent; reset --hard cannot delete it. Use drop, revert or filter-repo."
    override val deleteCommitMergeRevert =
        "Reverting a merge commit requires -m <parent>; choose drop or filter-repo instead."
    override val deleteCommitNotInHistory =
        "The commit is not reachable from the current history."
    override val deleteCommitDirty =
        "Commit or stash all changes before deleting a commit with filter-repo."
    override val deleteCommitStashes =
        "filter-repo removes stashes; drop or apply them before cleaning history."
    override val deleteCommitBackupNote =
        "reset creates a backup ref (Undo); filter-repo creates a bundle because it prunes old objects and cannot be undone normally."
    override fun deleteCommitFilterBundle(path: String) = "Backup bundle: $path"
    override fun toastFilterRepoDone(path: String) =
        "filter-repo complete; backup bundle: $path"
    override val commitActionFilterRepo = "filter-repo: remove / replace / rename"
    override val commitActionFilterRepoHint =
        "Rewrites every local ref: remove a file or folder, remove/replace a secret or string, or rewrite paths. A dry-run preview and a mandatory backup ref + bundle are created."
    override val filterRepoTitle = "FILTER-REPO"
    override val filterRepoHint =
        "Repo-wide history and working-tree rewrite. The selected commit is only a context anchor; branches, tags and other local refs are filtered."
    override val filterRepoModeRemove = "REMOVE PATH"
    override val filterRepoModeReplace = "REPLACE / SECRET"
    override val filterRepoModeRename = "RENAME PATH"
    override val filterRepoRemovePath = "file or folder path"
    override val filterRepoRemovePathHint =
        "Exact path from the repository root. Folders are removed recursively; globs are not accepted."
    override val filterRepoFind = "find text or secret"
    override val filterRepoReplacement = "replace with"
    override val filterRepoReplaceHint =
        "The value is written to a temporary expressions file, never to the command line or log. An empty replacement removes the match; the default is ***REMOVED***."
    override val filterRepoRegex = "regex"
    override val filterRepoOldPath = "old path"
    override val filterRepoNewPath = "new path"
    override val filterRepoRenameHint =
        "Renames the exact old path prefix to the new path in every selected ref; renames are not followed automatically."
    override val filterRepoPreview = "PREVIEW (--dry-run)"
    override val filterRepoPreviewRunning = "DRY-RUN…"
    override val filterRepoPreviewTitle = "DRY-RUN PREVIEW"
    override val filterRepoPreviewHint =
        "Run this first; it does not change history and does not create a backup."
    override val filterRepoChecking = "checking git filter-repo…"
    override val filterRepoBackupNote =
        "Before the rewrite a backup ref is created under refs/mybeautifulgit/backups, plus a bundle of all refs; both still contain the old secret/history. Keep them secure and remove them after verification. No one-click Undo is offered because every ref is rewritten; recover from the ref/bundle. Rewritten branches/tags need a force-push. Run stays disabled until you confirm."
    override val filterRepoConfirm = "I understand: all refs will be rewritten"
    override val filterRepoRun = "RUN FILTER-REPO"
    override val filterRepoPathRequired = "Enter a file or folder path."
    override val filterRepoPathInvalid =
        "Use an exact relative path without globs, '..', or an absolute path."
    override val filterRepoFindRequired = "Enter the text or secret to replace."
    override val filterRepoRegexInvalid = "Invalid regular expression."
    override val filterRepoRenameRequired = "Enter both old and new paths."
    override val filterRepoRenameInvalid =
        "Use two different exact relative paths without globs, '..', or an absolute path."
    override val filterRepoNoRefs = "No local refs to rewrite."
    override val filterRepoPreviewOutput =
        "filter-repo dry-run finished without preview files; history was not changed."
    override val filterRepoBackupFailed =
        "Could not create the mandatory backup ref or bundle; filter-repo was not started."
    override val filterRepoPreviewFailed =
        "filter-repo dry-run failed; history was not changed."
    override val filterRepoContentPreviewHint =
        "Path counts stay the same for text replacement; changed refs and commit counts show the rewrite scope."
    override val filterRepoAllRefs = "all local refs; backup refs are excluded"
    override fun filterRepoPreviewSummary(before: Int, after: Int, refs: Int, paths: Int) =
        "dry-run: $before commits read, $after in filtered history; $refs refs and $paths paths differ."
    override fun filterRepoFailure(detail: String, ref: String, bundle: String) =
        "$detail\nBackup ref: $ref\nBackup bundle: $bundle"
    override fun toastFilterRepoRewritten(ref: String, bundle: String) =
        "filter-repo complete; backup ref: $ref; bundle: $bundle"
    override val commitActionCherryPickHint =
        "Applies this commit on top of HEAD; conflicts remain in the repository for resolution."
    override val commitActionRevertHint =
        "Creates a new commit that reverses this commit."
    override val commitActionRange = "range"
    override val commitActionRangePlaceholder = "A..B (same revision syntax as git)"
    override val commitActionRangeHint =
        "Range mode ignores the selected commit. Endpoints are inclusive; A itself is not applied."
    override val commitActionNoCommit = "--no-commit"
    override val commitActionNoCommitHint =
        "Apply changes to the index without creating commits. A conflict pauses the range until it is resolved and staged."
    override val commitActionInvalidRange =
        "Enter a revision range like A..B without spaces or a leading dash."
    override val commitActionEmptyRange = "The revision range contains no commits."
    override val commitActionBranchName = "branch name"
    override val commitActionBranchHint =
        "Creates a branch at this commit without checking it out."
    override val commitActionTagName = "tag name"
    override val commitActionTagMessage = "tag message (optional)"
    override val commitActionTagHint =
        "Empty message creates a lightweight tag; a message creates an annotated tag; sign uses git tag -s and requires a message."
    override val tagSign = "sign"
    override val tagSignHint = "signed tag: git tag -s (requires a message and configured signing key)"
    override val tagSignNeedsMessage = "Signed tags require a message."
    override val tagNoCommits = "Cannot create a tag before the first commit."
    override val tagNotFound = "Tag was not found."
    override val tagManagerTitle = "TAGS"
    override val tagManagerNoTags = "no local tags"
    override val tagManagerSelect = "Select a tag to manage it."
    override val tagCreate = "new tag"
    override val tagPush = "push"
    override val tagPushAll = "push all tags"
    override val tagDeleteLocal = "delete local"
    override val tagDeleteRemote = "delete remote"
    override val tagCreateHint =
        "Creates a lightweight tag with an empty message, an annotated tag with a message, or a signed tag with sign enabled."
    override val tagPushRemote = "remote"
    override val tagPushHint = "Pushes only the selected tag to the selected remote."
    override val tagPushAllHint = "Pushes all local tags with git push --tags."
    override val tagDeleteLocalHint = "Deletes the local tag only; the remote tag is not touched."
    override val tagDeleteRemoteHint = "Deletes the tag on the remote; the local tag remains."
    override val tagAnnotated = "annotated"
    override val tagLightweight = "lightweight"
    override val tagNoRemotes = "no remotes configured"
    override val remoteManagerTitle = "REMOTES"
    override val remoteManagerNoRemotes = "no remotes configured"
    override val remoteManagerSelect = "Select a remote to manage it."
    override val remoteAdd = "add remote"
    override val remoteRemove = "remove"
    override val remoteEditUrl = "edit URL"
    override val remoteFetch = "fetch"
    override val remoteName = "remote name"
    override val remoteUrl = "remote URL"
    override val remoteAddHint = "Adds a named remote with git remote add."
    override val remoteRemoveHint =
        "Removes the remote URL and its remote-tracking branches. The server repository is not changed."
    override val remoteEditUrlHint = "Changes the URL used for future fetch and push operations."
    override val remoteUrlCredentialsHidden =
        "The stored URL contains credentials and is hidden. Enter the full URL to replace it."
    override val remoteFetchHint =
        "Fetches the selected remote; every option maps directly to a git fetch flag."
    override val remoteFetchPrune = "prune"
    override val remoteFetchTagsDefault = "tags: default"
    override val remoteFetchTags = "--tags"
    override val remoteFetchNoTags = "--no-tags"
    override val remoteFetchDepth = "depth (e.g. 1)"
    override val remoteFetchFilter = "filter (e.g. blob:none)"
    override val remoteFetchFilterHint =
        "Partial clone filter; the server must support filtering for this fetch."
    override val remoteHead = "remote HEAD"
    override val remoteHeadUnset = "not set"
    override val remoteHeadAuto = "auto"
    override val remoteHeadSet = "set"
    override val remoteHeadDelete = "delete"
    override val remoteHeadAutoHint =
        "Points refs/remotes/<remote>/HEAD at the remote HEAD. Fetch first so the remote refs exist."
    override val remoteHeadSetHint =
        "Points refs/remotes/<remote>/HEAD at the selected remote-tracking branch."
    override val remoteHeadDeleteHint = "Deletes only the symbolic refs/remotes/<remote>/HEAD."
    override val remoteHeadBranch = "remote branch"
    override val remoteNoBranches = "no remote-tracking branches"
    override val remoteInvalidName =
        "Remote name is empty, starts with '-', or contains whitespace, control characters or '..'."
    override val remoteInvalidUrl =
        "Remote URL must use https, ssh, or git; file:// and ext:: URLs are not allowed."
    override val remoteAlreadyExists = "A remote with this name already exists."
    override val remoteInvalidDepth = "Fetch depth must be a positive integer."
    override val remoteInvalidFilter = "Invalid partial-clone filter."
    override val remoteNotFound = "Remote is not configured."
    override val remoteProfileLoading = "reading remote identity…"
    override val remoteProfileFailed = "could not read remote identity"
    override val remoteIdentityLocal = "local filesystem path"
    override val remoteIdentityUnknown = "UNKNOWN"
    override val remotePreflightPermissions =
        "Permissions, pull requests/MRs and checks are UNKNOWN at custom-host level 1; no API is called."
    override val remotePreflightDetached = "detached HEAD"
    override val remotePreflightUnborn = "unborn branch"
    override val remotePreflightUnset = "not set"
    override val toastRemoteAdded = "remote added"
    override val toastRemoteRemoved = "remote removed"
    override val toastRemoteUrlUpdated = "remote URL updated"
    override val toastFetched = "fetch complete"
    override val toastRemoteHeadSet = "remote HEAD updated"
    override val toastRemoteHeadDeleted = "remote HEAD deleted"
    override val submoduleManagerTitle = "SUBMODULES"
    override val submoduleManagerNoSubmodules = "no submodules"
    override val submoduleManagerSelect = "Select a submodule, or manage all submodules."
    override val submoduleAll = "all submodules"
    override val submoduleInit = "init"
    override val submoduleUpdate = "update"
    override val submoduleSync = "sync"
    override val submoduleAdd = "add submodule"
    override val submoduleRemove = "remove"
    override val submoduleRecursive = "recursive"
    override val submoduleInitOnUpdate = "init"
    override val submoduleCheckout = "checkout"
    override val submoduleCustomUpdateWarning =
        "Without --checkout Git may run submodule.<name>.update from local config, including a custom command."
    override val submoduleCustomUpdateConfirm = "allow configured update"
    override val submodulePath = "path"
    override val submoduleUrl = "repository URL"
    override val submoduleInitHint =
        "Registers submodule URLs in .git/config; it does not check out working trees."
    override val submoduleUpdateHint =
        "Checks out the commit recorded by the superproject with --checkout; init also initializes uninitialized submodules. Configured custom update commands require explicit confirmation."
    override val submoduleSyncHint =
        "Updates submodule remote URLs from .gitmodules; it does not fetch or check out."
    override val submoduleAddHint =
        "Clones the repository into the path, adds .gitmodules and stages the gitlink."
    override val submoduleRemoveHint =
        "Runs submodule deinit then git rm. The checkout is removed; .git/modules may remain for recovery."
    override val submoduleInvalidPath =
        "Submodule path must be relative, use '/', and must not contain '..' or a leading '-'."
    override val submoduleInvalidUrl =
        "Submodule URL must use https, ssh, or git; file:// and ext:: URLs are not allowed."
    override val submoduleNotFound = "Submodule is not present in this repository."
    override val submoduleAlreadyExists = "A submodule already exists at this path."
    override val submoduleNone = "There are no submodules in this repository."
    override val submoduleStateCurrent = "current"
    override val submoduleStateModified = "modified"
    override val submoduleStateUninitialized = "uninitialized"
    override val submoduleStateConflict = "conflict"
    override val toastSubmoduleInitialized = "submodule init complete"
    override val toastSubmodulesUpdated = "submodules updated"
    override val toastSubmodulesSynced = "submodule URLs synchronized"
    override val toastSubmoduleAdded = "submodule added"
    override val toastSubmoduleRemoved = "submodule removed"
    override val pushManagerTitle = "PUSH"
    override val pushModeCurrent = "current branch"
    override val pushModeAnother = "another branch"
    override val pushModeTags = "all tags"
    override val pushModeAllBranches = "all branches (--all)"
    override val pushModeMirror = "mirror (--mirror)"
    override val pushModeDelete = "delete remote branch"
    override val pushModeRefspec = "arbitrary refspec"
    override val pushRemote = "remote"
    override val pushDestination = "remote branch / ref"
    override val pushBranch = "remote branch"
    override val pushRefspecPlaceholder = "source:destination"
    override val pushForce = "force"
    override val pushForceNone = "none"
    override val pushForceLease = "--force-with-lease"
    override val pushForceFlag = "--force"
    override val pushForceNoneHint = "Without force; git rejects a non-fast-forward update."
    override val pushForceLeaseHint =
        "Updates the remote only while it still points at the expected commit."
    override val pushForceHint = "Overwrites the remote even if it has commits you do not have."
    override val pushHintCurrent =
        "Publishes the current branch and sets its upstream."
    override val pushHintAnother =
        "Publishes the current branch under a different remote branch name without changing upstream."
    override val pushHintTags = "Publishes all tags with git push --tags."
    override val pushHintAllBranches = "Publishes all local branches with git push --all."
    override val pushHintMirror =
        "Mirrors all refs; remote refs that do not exist locally will be deleted."
    override val pushHintDelete =
        "Deletes the branch on the remote; the local branch is not changed."
    override val pushHintRefspec =
        "Publishes exactly the refspec you enter, without rewriting it."
    override val pushNoCurrentBranch = "Cannot determine the current branch (detached HEAD)."
    override val pushInvalidRefspec = "Invalid push refspec."
    override val pushNoRemotes = "no remotes configured"
    override val pushConfirm = "PUSH"
    override val toastPushed = "push complete"
    override val toastAllBranchesPushed = "all branches pushed"
    override val toastMirrorPushed = "mirror pushed"
    override val pullManagerTitle = "PULL"
    override val pullModeMerge = "merge"
    override val pullModeRebase = "rebase"
    override val pullModeFfOnly = "ff-only"
    override val pullRemote = "remote"
    override val pullBranch = "remote branch"
    override val pullCurrentBranch = "current branch"
    override val pullHintMerge =
        "Uses git pull --no-rebase; divergent histories create a merge commit."
    override val pullHintRebase =
        "Uses git pull --rebase; local commits are replayed on top of the remote branch."
    override val pullHintFfOnly =
        "Uses git pull --ff-only; refuses to run if the update cannot fast-forward."
    override val pullLocalChangesHint =
        "Local changes can block pull; commit or stash them first."
    override val pullNoCurrentBranch = "Cannot pull into detached HEAD."
    override val pullNoRemotes = "no remotes configured"
    override val pullConfirm = "PULL"
    override val toastPulled = "pull complete"
    override val commitActionBisectHint =
        "Starts bisect if needed and marks this commit. Mark another commit to narrow the range."
    override val commitActionInvalidHash = "Invalid commit hash."
    override val commitActionInvalidRef = "Invalid branch or tag name."
    override val commitReflogEmpty = "No reflog entries point to this commit."
    override val commitReflogLoading = "reading reflog…"
    override val commitReflogFailed = "could not read reflog"
    override val reflogPanelHint =
        "All HEAD and branch movements. Select an entry to restore the current branch or recreate a deleted branch."
    override val reflogFilterPlaceholder = "filter by ref, commit or message"
    override val reflogNoMatches = "No reflog entries match the filter."
    override val reflogOpenCommit = "open in log"
    override val reflogRestore = "restore"
    override val reflogRestoreHint =
        "Reset the current branch to this commit. The confirmation starts with --hard; soft, mixed and keep are available."
    override val reflogCreateBranch = "branch"
    override val reflogCreateBranchHint =
        "Create a branch at this commit, useful for a deleted branch. The name is prefilled when it can be derived from the ref."
    override val reflogActionHint =
        "Recovery is confirmed with the exact git command and its consequences before it runs."
    override val toastCommitCheckout = "checkout complete"
    override val toastCommitReset = "reset complete"
    override val toastCommitReworded = "commit reworded"
    override val toastCommitRewriteStarted = "rewrite started"
    override val toastCommitRewriteFinished = "rewrite finished"
    override val toastCommitRewriteCancelled = "rewrite cancelled"
    override val toastCommitCherryPicked = "cherry-pick complete"
    override val toastCommitReverted = "revert complete"
    override val toastBranchCreated = "branch created"
    override val toastBranchRenamed = "branch renamed"
    override val toastBranchDeleted = "branch deleted"
    override val toastRemoteBranchDeleted = "remote branch deleted"
    override val toastBranchUpstreamSet = "upstream set"
    override val toastBranchPushed = "branch pushed"
    override val toastRemoteBranchRenamed = "remote branch renamed"
    override val branchManagerTitle = "BRANCHES"
    override val branchManagerLocal = "LOCAL"
    override val branchManagerRemote = "REMOTE"
    override val branchManagerNoBranches = "no local branches"
    override val branchManagerNoRemotes = "no remote-tracking branches"
    override val branchManagerSelect = "Select a branch to manage it."
    override val branchCreate = "new branch"
    override val branchRename = "rename"
    override val branchDelete = "delete"
    override val branchSetUpstream = "set upstream"
    override val branchPush = "push"
    override val branchCompare = "compare"
    override val branchCreateHint = "Creates a branch at the current HEAD without switching to it."
    override val branchRenameHint =
        "Renames the local branch. Git keeps its upstream configuration when possible."
    override val branchDeleteHint =
        "Safe delete refuses a branch with unmerged commits; force removes it. The commits remain recoverable by hash or reflog."
    override val branchForceDelete = "force (-D)"
    override val branchForceDeleteHint =
        "Force deletes even if the branch is not merged. This only removes the ref; commit objects remain until gc."
    override val branchRemoteDeleteHint =
        "Deletes the branch on the remote. Other clones keep their local copies; recover with a commit hash or reflog where available."
    override val branchRemoteRenameHint =
        "Pushes a new branch from the remote-tracking ref, then deletes the old remote branch. If deletion fails, the new branch remains."
    override val branchNewName = "new branch name"
    override val branchUpstreamName = "upstream branch"
    override val branchUpstreamHint =
        "Sets the upstream with git branch --set-upstream-to; this changes configuration only, not commits."
    override val branchPushRemote = "remote"
    override val branchPushName = "remote branch name"
    override val branchPushHint =
        "Creates or updates the remote branch without force and sets the local upstream."
    override val branchCompareFirst = "first branch / ref"
    override val branchCompareSecond = "second branch / ref"
    override val branchCompareHint =
        "Compares the two refs with rev-list --left-right and shows commits unique to each side."
    override val branchCompareLoading = "reading comparison…"
    override val branchCompareFirstOnly = "only in first"
    override val branchCompareSecondOnly = "only in second"
    override val branchCompareEmpty = "both refs point to the same history"
    override val branchNoCommits = "Cannot create a branch before the first commit."
    override val branchCannotDeleteCurrent =
        "Cannot delete the branch checked out in this worktree."
    override val branchInvalidRemote = "Remote is not configured."
    override val branchNotFound = "Branch or remote ref was not found."
    override val branchNameUnchanged = "The new branch name is the same as the old one."
    override val toastTagCreated = "tag created"
    override val toastTagDeleted = "tag deleted"
    override val toastTagPushed = "tag pushed"
    override val toastTagsPushed = "tags pushed"
    override val toastRemoteTagDeleted = "remote tag deleted"
    override val toastBisectGood = "bisect: marked good"
    override val toastBisectBad = "bisect: marked bad"
    override val toastBisectStarted = "bisect started"
    override val toastBisectSkipped = "bisect: skipped"
    override val toastBisectReset = "bisect reset"
    override fun bisectFirstBadResult(hash: String, subject: String) =
        "first bad commit: $hash ${subject.trim()}".trim()
    override val bisectTitle = "BISECT"
    override val bisectStartTitle = "START BISECT"
    override val bisectBadRef = "bad ref"
    override val bisectGoodRef = "good ref"
    override val bisectBadPlaceholder = "HEAD"
    override val bisectGoodPlaceholder = "last known good, e.g. HEAD~10 or v1.0"
    override val bisectStart = "START"
    override val bisectStartHint =
        "Marks the bad and good refs, then checks out the first midpoint. Both refs are required."
    override val bisectStatusActive = "ACTIVE"
    override val bisectStatusComplete = "COMPLETE"
    override val bisectStatusIdle = "IDLE"
    override val bisectCurrent = "current candidate"
    override val bisectOriginal = "original"
    override val bisectMarkGood = "GOOD"
    override val bisectMarkBad = "BAD"
    override val bisectSkip = "SKIP"
    override val bisectReset = "RESET"
    override val bisectResetHint = "Returns to the original ref and removes bisect refs."
    override val bisectCommand = "verification command"
    override val bisectCommandPlaceholder = "npm test"
    override val bisectRun = "RUN COMMAND"
    override val bisectStop = "STOP"
    override val bisectRunning = "running command…"
    override val bisectCommandHint =
        "Exit 0 = good, 125 = skip, any other exit = bad. Runs in the worktree with a 10-minute timeout per attempt."
    override val bisectAutoLog = "AUTOMATION LOG"
    override val bisectAutoEmpty = "no automation output yet"
    override val bisectVisualization = "BISECT MAP"
    override val bisectMarksTitle = "MARKS"
    override val bisectNoMarks = "no commits marked yet"
    override val bisectLogTitle = "BISECT LOG"
    override val bisectNoState = "BISECT is not active"
    override val bisectRefresh = "refresh"
    override val bisectNoCommits = "history is not loaded"
    override val bisectBare = "Bisect is unavailable in a bare repository."
    override val bisectAutomationRunning = "Stop the current bisect automation first."
    override val bisectAlreadyActive = "BISECT is already active; reset it before starting a new search."
    override val bisectInvalidRef = "Bad and good must be non-empty refs without whitespace."
    override val bisectSameRef = "Bad and good must point to different refs."
    override val bisectNotActive = "BISECT is not active."
    override val bisectComplete = "This bisect has already found its first bad commit; reset it to continue."
    override val bisectCommandEmpty = "Enter a command to run for each bisect candidate."
    override val bisectAutomationStopped = "bisect automation stopped"
    override val bisectTooManySteps = "BISECT automation exceeded its safety step limit."
    override val bisectCommandTimedOut = "The verification command timed out; bisect was not marked."
    override val bisectCommandNoExit = "The verification command produced no exit code."
    override val bisectAutomationPaused = "bisect automation stopped at the current candidate"
    override val bisectAutomationDone = "bisect automation finished"
    override val logFilters = "filters"
    override val logFilterBranch = "branch"
    override val logFilterAuthor = "author"
    override val logFilterPath = "path"
    override val logFilterSince = "since"
    override val logFilterUntil = "until"
    override val logFilterMessage = "message"
    override val logFilterPickaxe = "-S"
    override val logFilterOnlyMine = "only mine"
    override val logFilterOnlyCurrent = "only current"
    override val logFilterApply = "APPLY"
    override val logFilterClear = "CLEAR"
    override val logFilterNoMatch = "no commits match the filters"
    override val logFilterNoIdentity = "Cannot resolve user.email for \"only mine\"."
    override val logFilterHint = "branch/ref overrides scope; uncheck only current to log --all"
    override val logFilterBranchPlaceholder = "branch or ref"
    override val logFilterAuthorPlaceholder = "author pattern"
    override val logFilterPathPlaceholder = "path from root"
    override val logFilterDatePlaceholder = "YYYY-MM-DD"
    override val logFilterMessagePlaceholder = "grep message"
    override val logFilterPickaxePlaceholder = "added/removed text"
    override val repoHistory = "history"
    override val repoBlame = "blame"
    override val searchHistoryHint =
        "Commits touching this path from HEAD. Renames are followed with --follow."
    override val searchBlameHint =
        "Blame of the HEAD version. Click a line to open its commit in LOG."
    override val searchGrepHint =
        "Fixed-string search in tracked working-tree files. Path is an optional filter."
    override val searchPathPlaceholder = "src/File.kt"
    override val searchPathOptionalPlaceholder = "path filter (optional)"
    override val searchQueryPlaceholder = "fixed string to find"
    override val searchHistoryLoading = "reading file history…"
    override val searchBlameLoading = "reading blame…"
    override val searchGrepLoading = "searching…"
    override val searchHistoryEmpty = "No commits touch this path."
    override val searchBlameEmpty = "No lines to blame."
    override val searchGrepEmpty = "No matches."
    override val searchHistoryFailed = "could not read file history"
    override val searchBlameFailed = "could not read blame"
    override val searchGrepFailed = "grep failed"
    override val searchPathRequired = "Enter a path."
    override val searchQueryRequired = "Enter a fixed string to search."
    override val searchTruncated = "output truncated"
    override val searchCommitNotFound = "Could not load this commit."
    override val searchPrompt = "Enter a path and run HISTORY or BLAME, or search with GREP."
    override val repoCommitMessageRequired = "Enter a commit message."
    override val repoCommitOptions = "options"
    override val repoCommitAuthor = "author"
    override val repoCommitCommitter = "committer"
    override val repoCommitTemplate = "commit template"
    override val repoCommitTemplateLoad = "LOAD"
    override val repoCommitTemplateHint =
        "Path relative to the repository root or absolute; lines starting with # are removed."
    override val repoCommitTemplateRequired = "Enter a commit template path."
    override val repoCommitTemplateMissing = "Commit template file was not found."
    override val repoCommitTemplateTooLarge = "Commit template is too large (maximum 512 KiB)."
    override val repoCommitTemplateEmpty = "Commit template has no message after comments are removed."
    override val repoCommitIdentityHint = "Name <email>; empty means git config."
    override val repoCommitIdentityInvalid = "Use the identity format Name <email>."
    override val repoCommitNoVerify = "no-verify"
    override val repoCommitMode = "mode"
    override val repoCommitModeNormal = "normal"
    override val repoCommitModeFixup = "fixup"
    override val repoCommitModeSquash = "squash"
    override val repoCommitTarget = "target"
    override val repoCommitTargetHint = "Commit hash or ref; fixup/squash use the generated message."
    override val repoCommitTargetRequired = "Enter a target commit for fixup/squash."
    override val repoCommitFixupAmendConflict = "Fixup/squash cannot be combined with amend."
    override val repoCommitRecent = "recent commits"
    override val repoRetryCommit = "RETRY"
    override val repoRetryCommitHint =
        "The commit did not finish. Fix the hook or the git error, then retry the exact command."
    override val repoHunksUnavailable =
        "The selected hunks are no longer in the diff — refresh and select again."
    override val repoCommandPlaceholder = "git status"
    override val repoCommandHint =
        "runs git in the repository root; shell pipes and redirects are not supported"
    override val repoCommandEmpty = "Enter a git command."
    override val repoCommandUnclosedQuote = "The command has an unclosed quote."
    override val repoAliasesHint = "click to insert · RUN executes via git"
    override val repoAliasesLoading = "reading aliases…"
    override val repoAliasesEmpty = "no git aliases configured"
    override val repoAliasesReload = "reload"
    override val repoAliasesRun = "RUN"
    override val runnerHistory = "HISTORY"
    override val runnerFavorites = "FAVORITES"
    override val runnerAliases = "ALIASES"
    override val runnerJobs = "JOBS"
    override val runnerHistoryHint = "click to insert · ☆ to favorite"
    override val runnerRunBackground = "BG"
    override val runnerNoHistory = "no commands yet"
    override val runnerNoFavorites = "no favorites yet — press ☆ on a command"
    override val runnerNoJobs = "no background commands"
    override val runnerRunning = "running"
    override val runnerSucceeded = "done"
    override val runnerFailed = "failed"
    override val runnerTimedOut = "timed out"
    override val runnerCancelled = "cancelled"
    override val runnerCancelling = "cancelling…"
    override val runnerCancel = "cancel"
    override val runnerClear = "clear"
    override val runnerClearFinished = "clear done"
    override val runnerBackgroundHint =
        "continues after switching tabs; avoid concurrent git writes"
    override val runnerBackgroundStarted = "started in background"
    override val repoShowIgnored = "show ignored"
    override val repoHideIgnored = "hide ignored"
    override val repoOperationInProgress =
        "an operation is in progress — finish or abort it before starting another."
    override val repoOperationContinue = "continue"
    override val repoOperationAbort = "abort"
    override val repoOperationSkip = "skip"
    override val repoOperationConfirm = "CONFIRM"
    override val repoOperationCancel = "CANCEL"
    override val repoOperationAbortWarning =
        "Aborts the operation and restores its pre-operation state. Uncommitted work may be lost."
    override val repoOperationSkipWarning =
        "Skips the current commit or patch and continues. Its changes are not applied."
    override val repoOperationSkipUnsupported =
        "git merge has no --skip; resolve conflicts and continue, or abort the merge."
    override val repoOperationNoActive = "No unfinished git operation."
    override val repoOperationUnsupported = "This operation does not support that action."
    override val repoOperationResolveConflicts =
        "Resolve all conflicts and stage the files before continuing."
    override val repoOperationEditTodo = "edit todo"
    override val rebaseEditorPrepareTitle = "INTERACTIVE REBASE"
    override val rebaseEditorEditTitle = "EDIT REBASE TODO"
    override val rebaseEditorBase = "base"
    override val rebaseEditorAutosquash = "autosquash"
    override val rebaseEditorAutosquashHint =
        "Reorder fixup!/squash!/amend! commits and convert them to fixup/squash before editing."
    override val rebaseEditorTodo = "todo"
    override val rebaseEditorAdd = "add"
    override val rebaseEditorEmpty = "The rebase todo is empty."
    override val rebaseEditorLoading = "reading git rebase todo…"
    override val rebaseEditorStart = "START REBASE"
    override val rebaseEditorSave = "SAVE TODO"
    override val rebaseEditorBackupHint =
        "A backup-ref is created before the rebase; Undo is offered after it finishes."
    override val rebaseEditorEditHint =
        "This changes the remaining todo of the running rebase. Continue from the operation banner when done."
    override val rebaseEditorCommandHint =
        "Drag ≡ to reorder. Click the command token to choose another action."
    override val rebaseEditorRewordMessage = "reword message"
    override val rebaseEditorArgument = "argument"
    override val rebaseEditorRemove = "remove line"
    override val rebaseEditorInvalidTodo =
        "The todo contains a command that cannot be written safely."
    override val rebaseEditorMissingCommit = "A commit command has no hash."
    override val rebaseEditorMissingExec = "exec needs a command."
    override val rebaseEditorMissingLabel = "label/reset needs a label."
    override val rebaseEditorMissingMerge = "merge needs a label."
    override val rebaseEditorMissingMessage = "reword needs a commit message."
    override val rebaseEditorEmptyPlan = "git produced an empty rebase todo."
    override val rebaseEditorPlanFailed =
        "Could not read git's interactive rebase plan. Check the console for the git error."
    override val toastInteractiveRebaseStopped = "rebase stopped — resolve, edit todo, or continue"
    override val toastInteractiveRebaseDone = "interactive rebase complete"
    override val repoNoCommitInProgress = "NO-COMMIT RANGE"
    override val repoNoCommitRemaining = "left"
    override val repoNoCommitHint =
        "Resolve conflicts and stage the files, then continue. Changes stay staged; no commit is created."
    override val repoNoCommitAbortWarning =
        "Abort discards all uncommitted changes (including files staged before the range) and restores the original HEAD."
    override val rerereTitle = "RERERE"
    override val rerereEnabled = "enabled"
    override val rerereDisabled = "disabled"
    override val rerereRecorded = "recorded resolutions"
    override val rerereEnable = "enable"
    override val rerereDisable = "disable"
    override val rerereClear = "clear cache"
    override val rerereEmpty = "no recorded resolutions"
    override val rerereEmptyHint =
        "Enable rerere, then resolve a conflict once. Git will save the resolution in .git/rr-cache and show it here."
    override val rerereSelectHint = "select a record to inspect preimage and postimage"
    override val rerereClearConfirm = "Clear all recorded rerere resolutions?"
    override val rerereClearHint =
        "Only .git/rr-cache is deleted. Commits, refs and the worktree are untouched. This cannot be undone."
    override val rerereConfirm = "CLEAR"
    override val rerereCancel = "CANCEL"
    override val rererePreimage = "preimage"
    override val rererePostimage = "postimage"
    override val rerereMissingFile = "missing"
    override val rerereTruncated = "truncated at 256 KiB"
    override val rerereLoadFailed = "could not read the selected rerere record"
    override val rerereClearFailed = "could not clear .git/rr-cache"
    override val rerereConfigHint = "git config rerere.enabled"
    override val toastRerereEnabled = "rerere enabled"
    override val toastRerereDisabled = "rerere disabled"
    override val toastRerereCleared = "rerere cache cleared"
    override fun stashPartialCount(count: Int) = "$count selected"
    override val stashTitle = "STASH"
    override val stashCreateTitle = "NEW STASH"
    override val stashAllChanges = "all changes"
    override val stashMessagePlaceholder = "optional message"
    override val stashIncludeUntracked = "untracked"
    override val stashIncludeIgnored = "ignored"
    override val stashStaged = "staged only"
    override val stashStagedVersion = "staged-only stash needs git 2.35+"
    override val stashPartialFiles = "partial stash · files"
    override val stashNoChanges = "there are no local changes to stash"
    override val stashPartialHint =
        "select files for a partial stash, or leave empty to stash all changes"
    override val stashCreate = "CREATE STASH"
    override val stashEmpty = "no stashes"
    override val stashUntitled = "(no message)"
    override val stashSelectHint = "select a stash to inspect and apply it"
    override val stashApply = "APPLY"
    override val stashPop = "POP"
    override val stashPopConfirm = "CONFIRM POP"
    override val stashPopWarning =
        "POP applies the stash, then removes it if applying succeeds."
    override val stashDrop = "DROP"
    override val stashDropConfirm = "CONFIRM DROP"
    override val stashDropWarning = "DROP permanently removes this stash entry."
    override val stashBranchName = "branch"
    override val stashBranchPlaceholder = "new branch name"
    override val stashBranchAction = "BRANCH"
    override val stashBranchHint =
        "Creates a branch at the stash base, checks it out and applies the stash; git drops the stash on success. The worktree must be clean."
    override val stashFiles = "FILES"
    override val stashShowLoading = "loading stash…"
    override val stashShowEmpty = "no patch content"
    override val stashShowTruncated = "patch truncated"
    override val stashPatch = "PATCH"
    override val stashBare = "Stash is unavailable in a bare repository."
    override val stashPathInvalid = "A selected path is not present in the current status."
    override val stashNotFound = "Stash entry no longer exists; refresh the list."
    override val stashBranchDirty =
        "Commit or stash local changes before creating a branch from a stash."
    override val toastStashCreated = "stash created"
    override val toastStashApplied = "stash applied"
    override val toastStashPopped = "stash popped"
    override val toastStashDropped = "stash dropped"
    override val toastStashBranchCreated = "branch created from stash"
    override val worktreeManagerTitle = "WORKTREES"
    override val worktreeAdd = "ADD"
    override val worktreePrune = "PRUNE"
    override val worktreeOpen = "OPEN"
    override val worktreeOpenWindow = "OPEN WINDOW"
    override val worktreeBranchAction = "BRANCH"
    override val worktreeLock = "LOCK"
    override val worktreeUnlock = "UNLOCK"
    override val worktreeRemove = "REMOVE"
    override val worktreeNoWorktrees = "no worktrees"
    override val worktreeSelect = "select a worktree to manage it"
    override val worktreePath = "path"
    override val worktreeBranch = "branch"
    override val worktreeBase = "start point"
    override val worktreeCreateBranch = "create branch (-b)"
    override val worktreeForce = "force"
    override val worktreeLockReason = "lock reason"
    override val worktreeAddHint =
        "Creates a linked worktree. With -b git creates a new branch; otherwise branch is an existing ref or commit-ish. The path must be outside the current worktree or empty."
    override val worktreeBranchHint =
        "Switches the selected worktree. A new branch starts at HEAD (or the supplied start point). The worktree must be able to check out the result."
    override val worktreeRemoveHint =
        "Removes the worktree record and its checkout. Uncommitted changes require --force and are then lost."
    override val worktreeLockHint =
        "A locked worktree cannot be pruned or removed until it is unlocked. The reason is shown by git worktree list."
    override val worktreePruneHint =
        "Prunes only stale administrative worktree records. Checkouts and commits are not deleted; --dry-run shows what would be pruned."
    override val worktreePruneConfirm = "PRUNE"
    override val worktreePrunePreview = "dry-run"
    override val worktreePruneNothing = "nothing to prune"
    override val worktreeCancel = "CANCEL"
    override val worktreeCurrent = "current"
    override val worktreeBare = "bare"
    override val worktreeDetached = "detached"
    override val worktreeLocked = "locked"
    override val worktreePrunable = "prunable"
    override val worktreePathPlaceholder = "C:\\repo-feature"
    override val worktreeBranchPlaceholder = "feature/name"
    override val worktreeBasePlaceholder = "HEAD"
    override val worktreeReasonPlaceholder = "why this worktree is locked"
    override val worktreeInvalidPath =
        "Worktree path is empty, starts with '-' or contains a control character."
    override val worktreeAlreadyExists = "This path is already a worktree of the repository."
    override val worktreeInvalidBranch = "Invalid branch name."
    override val worktreeInvalidBase = "Invalid start point."
    override val worktreeNotFound = "Worktree is no longer present; refresh the list."
    override val worktreeCannotRemoveCurrent =
        "Cannot remove the worktree that is currently open."
    override val worktreeCannotRemoveBare = "Cannot remove a bare repository worktree."
    override val worktreeAlreadyLocked = "Worktree is already locked."
    override val worktreeNotLocked = "Worktree is not locked."
    override val worktreeBranchBare = "A bare worktree has no branch to switch."
    override val toastWorktreeAdded = "worktree added"
    override val toastWorktreeRemoved = "worktree removed"
    override val toastWorktreeLocked = "worktree locked"
    override val toastWorktreeUnlocked = "worktree unlocked"
    override val toastWorktreeBranchChanged = "worktree branch changed"
    override val toastWorktreesPruned = "worktree records pruned"
    override val hookManagerTitle = "HOOKS"
    override val hookSelect = "select a hook to view and edit it"
    override val hookNoHooks = "no hooks in this directory"
    override val hookFilterPlaceholder = "filter hooks"
    override val hookEnabled = "enabled"
    override val hookDisabled = "disabled"
    override val hookSample = "sample"
    override val hookEnable = "ENABLE"
    override val hookDisable = "DISABLE"
    override val hookSave = "SAVE"
    override val hookReload = "RELOAD"
    override val hookUnsaved = "unsaved changes"
    override val hookDiscard = "DISCARD"
    override val hookCancel = "CANCEL"
    override val hookTruncated =
        "file is larger than 512 KiB; only the beginning is shown"
    override val hookNotFound = "Hook no longer exists; refresh the list."
    override val hookTooLarge = "Hook content is larger than 512 KiB."
    override val hookSaveFailed = "Could not save the hook file."
    override val hookFileExists = "That active or disabled hook file already exists."
    override val hookPath = "core.hooksPath"
    override val hookPathPlaceholder = ".githooks, or empty for default"
    override val hookPathSet = "SET"
    override val hookPathClear = "DEFAULT"
    override val hookPathEffective = "effective directory"
    override val hookPathInvalid =
        "Hooks path contains a control character or starts with '-'."
    override val hookPathExternalWarning =
        "core.hooksPath is outside the repository's .git directory. Editing or enabling these hooks can run arbitrary files; confirm explicitly."
    override val hookPathConfirmationRequired =
        "Hooks outside the default .git/hooks directory require explicit confirmation."
    override val hookExternalConfirm = "CONFIRM HOOKS"
    override val hookOutput = "HOOK OUTPUT"
    override val hookOutputEmpty = "no output from the last git operation"
    override val hookOutputClear = "CLEAR"
    override val hookCopy = "COPY"
    override val hookOutputHint =
        "Last git operation; includes stdout and stderr written by hooks."
    override val hookEditorPlaceholder = "#!/bin/sh\n# edit the hook script here"
    override val hookFile = "file"
    override val hookExit = "exit"
    override val hookLoadFailed = "Could not read the hook file."
    override val hookSaved = "hook saved"
    override val toastHooksPathSet = "core.hooksPath updated"
    override val toastHooksPathCleared = "core.hooksPath reset"
    override fun patchSelectedCount(count: Int) = "$count selected"
    override val patchesTitle = "PATCHES"
    override val patchExportTitle = "EXPORT"
    override val patchSelectMode = "COMMITS"
    override val patchRangeMode = "RANGE"
    override val patchSelectAll = "ALL"
    override val patchClearSelection = "CLEAR"
    override val patchRange = "revision range"
    override val patchRangePlaceholder = "HEAD~3..HEAD"
    override val patchOutputDir = "output directory"
    override val patchOutputDirPlaceholder = ".git/chan-patches"
    override val patchOutputFile = "mailbox file"
    override val patchOutputFilePlaceholder = "patches.mbox"
    override val patchModeFiles = "FILES"
    override val patchModeMailbox = "MAILBOX"
    override val patchCoverLetter = "cover letter"
    override val patchNumbered = "numbered"
    override val patchSignoff = "signoff"
    override val patchBinary = "binary"
    override val patchSubjectPrefix = "subject prefix"
    override val patchSubjectPrefixPlaceholder = "PATCH"
    override val patchPreview = "PREVIEW"
    override val patchExport = "EXPORT"
    override val patchPreviewHint =
        "Builds a mailbox in memory with git format-patch --stdout; no files are written."
    override val patchExportHint =
        "Writes one mbox file or a directory of patch files. Cover letters apply to a range only."
    override val patchMailboxTitle = "MAILBOX"
    override val patchMailboxPath = "patch / mailbox path"
    override val patchMailboxPathPlaceholder = "patches.mbox or 0001-change.patch"
    override val patchLoad = "LOAD"
    override val patchMailboxEmpty = "load a patch or preview an export to inspect the mailbox"
    override val patchMailboxMessages = "messages"
    override val patchMailboxTruncated = "mailbox truncated"
    override val patchRaw = "RAW"
    override val patchApplyTitle = "APPLY"
    override val patchApplyCheck = "CHECK"
    override val patchApply = "APPLY"
    override val patchApply3way = "3-way"
    override val patchAm = "AM"
    override val patchAmSignoff = "signoff"
    override val patchApplyHint =
        "apply changes the worktree; --check only verifies the patch. am applies a mailbox and creates commits; --3way resolves against blobs."
    override val patchNoCommits = "no commits loaded"
    override val patchNoSelection = "Select commits or enter a revision range."
    override val patchSelectionAndRange = "Choose either selected commits or a range, not both."
    override val patchInvalidRange = "Invalid revision range."
    override val patchUnknownCommit = "A selected commit is not present in the loaded history."
    override val patchInvalidPath = "Patch path is empty or contains a control character."
    override val patchFileNotFound = "Patch or mailbox file was not found."
    override val patchOutputDirFailed = "Could not create the patch output directory."
    override val toastPatchesExported = "patches exported"
    override val toastPatchCheckPassed = "patch check passed"
    override val toastPatchApplied = "patch applied"
    override val toastMailboxApplied = "mailbox applied"
    override val repoIndexLockWarning =
        "another git process may still be running; delete the lock only if no git command is active."
    override val repoIndexLockDelete = "delete lock"
    override val repoIndexLockDeleteFailed =
        "Could not delete index.lock. Close git and try again."
    override val toastStaged = "staged"
    override val toastUnstaged = "unstaged"
    override val toastStagedAll = "all changes staged"
    override val toastUnstagedAll = "index cleared"
    override val toastDiscarded = "changes discarded"
    override val toastDiscardedToStash = "changes moved to stash"
    override fun toastDiscardedWithPatch(path: String) = "changes discarded · patch saved: $path"
    override val toastAdded = "added to index"
    override val toastIgnored = "added to .gitignore"
    override val toastCommitted = "commit created"
    override val toastOperationContinued = "operation continued"
    override val toastOperationAborted = "operation aborted"
    override val toastOperationSkipped = "operation step skipped"
    override val toastTemplateLoaded = "template loaded into message"
    override val toastCommandDone = "command finished"
    override val toastFailed = "operation failed"
    override val toastIndexLockRemoved = "index.lock removed"
    override val toastCopy = "copy command"
    override val toastUndo = "undo"
    override val toastRewriteUndone = "rewrite undone"
    override val toastRewriteUndoneStashed = "rewrite undone; local changes saved to stash"
    override val toastUndoUnavailable = "Nothing to undo."
    override val toastUndoHeadChanged =
        "History changed after that operation; undo is disabled."
    override val toastUndoBranchChanged =
        "Return to the original branch before undoing this rewrite."
    override val toastOperationUndone = "operation undone"
    override val toastOperationRedone = "operation redone"
    override val toastOperationRedoneStashed = "operation redone; local changes saved to stash"
    override val toastOperationNotUndoable = "This operation has no automatic undo."
    override val toastOperationCancelled = "operation cancelled"
    override val toastRedoUnavailable = "Nothing to redo."
    override val operationRunning = "working"
    override val operationCancelling = "cancelling…"
    override val operationCompleted = "completed"
    override val operationFailed = "failed"
    override val operationCancelled = "cancelled"
    override val operationTimedOut = "timed out"
    override val operationCancel = "cancel"
    override val journalUndo = "undo"
    override val journalRedo = "redo"
    override val journalOperations = "operations"
    override val journalEmpty = "No operations recorded yet."
    override val journalEmptyHint =
        "Run a Git operation; it will appear here with its command, result and undo path."
    override val journalOnlyLatest =
        "Undo and redo apply to the newest reversible operation; the rest stay in the journal."
    override val journalNoUndo = "no automatic undo"
    override val journalUndoable = "undoable"
    override val journalUndone = "undone"
    override val settingsGitPathHint =
        "empty — auto-detect; otherwise a git executable or its folder"
    override val settingsDefaultBranchHint =
        "used by Init; an empty value falls back to main"
    override val settingsCopyShell = "copy shell"
    override val settingsCopyShellHint =
        "syntax used for command previews and copied commands; auto follows the platform"
    override val settingsCloseToTray =
        "close to tray"
    override val settingsCloseToTrayHint =
        "The close button hides the main window in the system tray instead of quitting. Use Quit in the tray menu to exit."
    override val settingsStartAtLogin =
        "start at login"
    override val settingsStartAtLoginHint =
        "Start MyBeautifulGit-chan automatically after you sign in. Off by default."
    override fun settingsStartAtLoginFailed(detail: String) =
        if (detail.isBlank()) "Could not update autostart." else "Could not update autostart: $detail"
    override val settingsUpdateChannel = "release channel"
    override val settingsUpdateChannelChan = "chan · stable"
    override val settingsUpdateChannelKun = "kun · beta"
    override val settingsUpdateChannelHint =
        "chan receives stable GitHub releases; kun also receives beta and prerelease builds."
    override val settingsUpdateCheck = "CHECK FOR UPDATES"
    override val settingsUpdateChecking = "CHECKING…"
    override val settingsUpdateOpen = "OPEN RELEASE"
    override val settingsUpdateNoInstall =
        "Checks GitHub Releases only. Installing desktop updates needs signed packages and comes later."
    override val settingsUpdateNoReleases = "No releases are available on this channel yet."
    override val settingsUpdateRepositoryUnavailable =
        "The release repository is not public yet."
    override fun settingsUpdateUpToDate(current: String) =
        "No newer release for $current."
    override fun settingsUpdateAvailable(current: String, latest: String) =
        "Update available: $latest (current $current)."
    override fun settingsUpdateFailed(detail: String) =
        if (detail.isBlank()) {
            "Could not check GitHub Releases."
        } else {
            "Could not check GitHub Releases: $detail"
        }
    override val aiSettingsSection = "AI COMMIT MESSAGES"
    override val aiEnabled = "commit message AI"
    override val aiEnabledHint =
        "Opt-in. The staged diff is sent to the configured OpenAI-compatible endpoint only after you confirm; worktree content requires a separate explicit action."
    override val aiEndpoint = "endpoint URL"
    override val aiEndpointHint =
        "Full chat-completions URL, for example https://api.openai.com/v1/chat/completions"
    override val aiModel = "model"
    override val aiApiKey = "API key"
    override val aiApiKeyHint =
        "Stored encrypted in the local credential store; never written to settings.properties or logs. Leave blank for a local endpoint without a key."
    override val aiMessageLanguage = "message language"
    override val aiLanguageAuto = "auto"
    override val aiLanguageEnglish = "english"
    override val aiLanguageRussian = "russian"
    override val aiStyle = "style"
    override val aiStyleConventional = "conventional"
    override val aiStylePlain = "plain"
    override val aiMaxDiff = "max diff chars"
    override val aiTemperature = "temperature"
    override val aiAllowLocalHttp = "local HTTP"
    override val aiAllowLocalHttpHint =
        "Allow plain HTTP only for localhost (Ollama, LM Studio and similar). External endpoints must use HTTPS."
    override val aiSecurityHint =
        "Only the chosen diff is sent (staged by default). .env, keys, credentials and common secrets are filtered; binary files are skipped. The diff is treated as untrusted data."
    override val aiEndpointInvalid =
        "AI endpoint must be a valid HTTPS URL (or an allowed localhost HTTP URL)."
    override val aiModelRequired = "Choose an AI model."
    override val aiMaxDiffInvalid = "Max diff must be between 1000 and 100000 characters."
    override val aiTemperatureInvalid = "Temperature must be between 0 and 2."
    override val aiGenerate = "GENERATE"
    override val aiGenerateWorktree = "WORKTREE"
    override val aiRegenerate = "REGENERATE"
    override val aiPreparing = "PREPARING…"
    override val aiGenerating = "GENERATING…"
    override val aiCancel = "CANCEL"
    override val aiRestore = "RESTORE"
    override val aiSendTitle = "SEND STAGED DIFF?"
    override val aiSendConfirm = "SEND"
    override val aiSendWarning =
        "The staged diff will be sent to the configured external service. Nothing is committed."
    override val aiSendLocalWarning =
        "The staged diff will be sent to the configured local endpoint. Nothing is committed."
    override val aiSendWorktreeWarning =
        "The unstaged/untracked worktree diff will be sent to the configured service shown below. Nothing is committed."
    override val aiDialogEndpoint = "endpoint"
    override val aiDialogBranch = "branch"
    override val aiDialogPayload = "payload"
    override val aiDialogOmitted = "sensitive/binary sections omitted"
    override val aiDialogTruncated = "diff truncated"
    override val aiDialogRedacted = "obvious secrets redacted"
    override fun aiPreviewSummary(files: Int, chars: Int) =
        "$files staged ${if (files == 1) "file" else "files"} · $chars characters"
    override fun aiError(detail: String) =
        if (detail.isBlank()) "Could not generate a commit message." else "Could not generate a commit message: $detail"
    override val settingsOn = "on"
    override val settingsOff = "off"
    override val trayQuit = "Quit"
    override val settingsDefaultBranchInvalid =
        "Branch name must not contain spaces or start with a dash."
    override val settingsAuthUsername = "HTTPS username"
    override val settingsAuthUsernameHint =
        "blank becomes git; deploy tokens may require their own username"
    override val settingsAuthToken = "HTTPS token"
    override val settingsAuthTokenHint =
        "stored AES-GCM-encrypted with a key in the config directory; protects accidental copies/backups, not processes running as the same user. Use a personal access token, not your account password."
    override val settingsAuthHelper = "credential.helper"
    override val settingsAuthHelperHint =
        "empty uses git's configured helpers; a token overrides this helper while it is set"
    override val settingsAuthHelperWarning =
        "credential.helper is passed to Git unchanged and may run an arbitrary command. Do not put a token or password here; this value is stored in plaintext settings.properties."
    override val settingsAuthSsh = "SSH auth"
    override val settingsAuthSshAgent = "agent"
    override val settingsAuthSshKey = "key"
    override val settingsAuthSshKeyHint =
        "agent uses SSH_AUTH_SOCK / system OpenSSH; key runs ssh -i <path> -o IdentitiesOnly=yes"
    override val settingsAuthSshKeyRequired =
        "Choose an SSH key file or switch SSH auth back to agent."
    override val settingsAuthTokenInvalid = "The HTTPS token must not contain newlines."
    override val settingsAuthHelperInvalid = "The credential helper must be a single line."
    override val authErrorTwoFactor =
        "The server requires a second factor. Use a personal access token as the HTTPS token, or switch to SSH; a password prompt cannot complete 2FA."
    override val authErrorTokenRequired =
        "Password authentication is disabled. Create a personal access token and enter it as the HTTPS token."
    override val authErrorHttps =
        "HTTPS authentication failed. Check the username and token, or configure a credential helper."
    override val authErrorSshKey =
        "SSH key authentication failed. Check the key path, its passphrase/agent, and that the key is registered with the server."
    override val authErrorHostKey =
        "SSH host key verification failed. Verify the server fingerprint before trusting it."
    override val toastAuthFailed = "authentication failed — see error"
    override val gitErrorFailed = "Git could not complete this operation."
    override val gitErrorTimedOut =
        "The Git command timed out. Check the connection or the process, then try again."
    override val gitErrorNotRepository =
        "This folder is not a Git repository."
    override val gitErrorDirtyWorktree =
        "Local changes would be overwritten. Commit or stash them first."
    override val gitErrorConflicts =
        "The operation stopped because of conflicts. Resolve them, then continue or abort."
    override val gitErrorPushRejected =
        "The remote rejected the push because it has newer commits. Fetch and review before pushing again."
    override val gitErrorNoUpstream =
        "This branch has no upstream. Set an upstream before pulling or pushing."
    override val gitErrorRemoteNotFound =
        "The remote repository or branch was not found."
    override val gitErrorRefNotFound =
        "The requested branch, tag, or commit was not found."
    override val gitErrorPathNotFound =
        "No files matched the requested path."
    override val gitErrorIndexLock =
        "The Git index is locked by another process. Close it or remove index.lock after checking."
    override val gitErrorPermission =
        "Permission denied while accessing the repository or remote."
    override val gitErrorAlreadyExists =
        "The target already exists."
    override val gitErrorNotFastForward =
        "The branch cannot be fast-forwarded because the histories diverged."
    override val gitErrorUnrelatedHistories =
        "The histories are unrelated, so Git refused to merge them."
    override val configTitle = "GIT CONFIG"
    override val configAll = "ALL"
    override val configScopeSystem = "SYSTEM"
    override val configScopeGlobal = "GLOBAL"
    override val configScopeLocal = "LOCAL"
    override val configScopeWorktree = "WORKTREE"
    override val configAliases = "ALIASES"
    override val configIncludeIf = "INCLUDEIF"
    override val configFilterPlaceholder = "filter keys or values"
    override val configKey = "key"
    override val configValue = "value"
    override val configType = "type"
    override val configOrigin = "origin"
    override val configNew = "NEW"
    override val configSave = "SAVE"
    override val configDelete = "DELETE"
    override val configReload = "RELOAD"
    override val configNoEntries = "no config entries in this view"
    override val configScopeHint =
        "target scope for add/update; system and global can affect other repositories"
    override val configScopeConfirmation =
        "SYSTEM/GLOBAL writes affect every repository on this machine. Confirm this change."
    override val configConfirm = "CONFIRM"
    override val configCancel = "CANCEL"
    override val configScopeConfirmationRequired =
        "System/global config changes require confirmation."
    override val configSaved = "config entry saved"
    override val configDeleted = "config entry deleted"
    override val configKeyRequired = "Enter a config key."
    override val configKeyInvalid =
        "Invalid key: use section.name, no leading dash, no control characters or '='."
    override val configAliasNameRequired = "An alias needs a name after alias."
    override val configAliasNameInvalid = "Alias names must not contain whitespace or control characters."
    override val configBooleanInvalid = "Expected a Git boolean (true/false, yes/no, on/off, 1/0)."
    override val configIntegerInvalid = "Expected an integer, optionally followed by k, m or g."
    override val configColorInvalid = "Expected a Git color name, auto/always/never, #rrggbb or ansiN."
    override val configIncludeIfInvalid =
        "includeIf needs a supported condition: gitdir:, gitdir/i:, onbranch: or hasconfig:remote.*.url:."
    override val configIncludeIfPathRequired = "includeIf.path must point to a config file."
    override val gitMetaTitle = "GIT FILES"
    override val gitMetaSave = "SAVE"
    override val gitMetaReload = "RELOAD"
    override val gitMetaSaved = "file saved"
    override val gitMetaBare = "A bare repository has no worktree files to edit."
    override val gitMetaValidationBlocked = "Fix syntax errors before saving."
    override val gitMetaMissing = "file does not exist yet; SAVE will create it"
    override val gitMetaNoIssues = "no syntax issues"
    override val gitMetaUnsaved = "unsaved changes"
    override val gitMetaDiscard = "DISCARD"
    override val gitMetaCancel = "CANCEL"
    override val gitMetaPlaceholder = "one rule per line; comments start with #"
    override val gitMetaWorktree = "worktree"
    override val gitMetaLoading = "loading…"
    override val gitMetaChecking = "checking…"
    override val gitMetaLine = "line"
    override val gitMetaError = "error"
    override val gitMetaWarning = "warning"
    override val gitMetaCheck = "CHECK"
    override val gitMetaIssueIgnoreEmptyNegation = "A negation rule needs a pattern after !."
    override val gitMetaIssueIgnoreTrailingSpace =
        "An unescaped trailing space is ignored by Git."
    override val gitMetaIssueTrailingEscape =
        "The line ends with an unescaped backslash."
    override val gitMetaIssueUnclosedClass =
        "The character class is not closed; Git treats '[' literally."
    override val gitMetaIssueAttributesMissing = "The pattern has no attributes."
    override val gitMetaIssueAttributesNameInvalid = "Invalid attribute name."
    override val gitMetaIssueAttributesDuplicate =
        "An attribute is assigned more than once on this line."
    override val gitMetaIssueAttributesConflict =
        "An attribute is both set and unset on this line."
    override val gitMetaIssueMailmapEmailRequired =
        "A mailmap line needs at least one <email>."
    override val gitMetaIssueMailmapEmailInvalid =
        "Email must be non-empty, contain no spaces or angle brackets, and normally contains @."
    override val gitMetaIssueMailmapTooManyEmails =
        "A mailmap line can contain at most two emails."
    override val gitMetaIssueMailmapUnexpectedText =
        "Unexpected text or angle bracket outside an email mapping."
    override val gitMetaIssueMailmapDuplicate =
        "This commit email is mapped more than once."
    override val gitMetaIssueMailmapUnclosed =
        "Email starts with '<' but has no closing '>'."
    override val configValueInvalid = "Value must not be empty or contain NUL for this key."
    override val configTypeString = "string"
    override val configTypeBoolean = "boolean"
    override val configTypeInteger = "integer"
    override val configTypeColor = "color"
    override val configTypePath = "path"
    override val configTypeBooleanOrNamed = "boolean|named"
}

object RussianStrings : Strings {
    override val language = Language.RU

    override val open = "Открыть"

    override val a11yClose = "Закрыть"
    override val a11yDismiss = "Закрыть"
    override val a11ySearchCommands = "Поиск команд"
    override val a11yZoomIn = "Увеличить масштаб"
    override val a11yZoomOut = "Уменьшить масштаб"
    override val a11yZoomReset = "Сбросить масштаб интерфейса"
    override fun a11yCloseRepository(name: String) = "Закрыть репозиторий $name"
    override fun a11yOpenRecent(path: String) = "Открыть недавний репозиторий $path"
    override fun a11yForgetRecent(path: String) = "Удалить из недавних $path"
    override val settingsContrast = "контраст"
    override val contrastNormal = "обычный"
    override val contrastHigh = "высокий"

    override val welcomeTagline = "весь git. без нянек. с откатом."
    override val welcomeNoRecent =
        "нет недавних репозиториев — открой папку или перетащи её в окно"

    override fun welcomeEolWarning(major: Int, minor: Int) =
        "ветка git $major.$minor больше не получает upstream-патчи. В LTS-дистрибутиве " +
            "проверьте, что пакет git обновлён."

    override fun welcomePartialWarning(count: Int, names: String) =
        "часть возможностей недоступна ($count): $names"
    override val welcomeUpdateGit = "обновить git"
    override fun welcomeDontRemind(version: String) = "не напоминать для $version"

    override val setupInstall = "УСТАНОВИТЬ"
    override val setupInstalling = "УСТАНОВКА…"
    override val setupInstallFailed = "Установка не удалась."
    override val setupInstallNotDetected =
        "Установщик завершился, но git не найден. Перезапустите приложение или укажите путь вручную."
    override val setupNoInstallOptions =
        "Откройте https://git-scm.com/downloads и установите git вручную."
    override val setupMissingFeatures = "ЧЕГО НЕ БУДЕТ НА ЭТОЙ ВЕРСИИ"

    override fun setupFeatureSince(title: String, major: Int, minor: Int) =
        "· $title  (с $major.$minor)"

    override val setupManualPathTitle = "УКАЗАТЬ ПУТЬ ВРУЧНУЮ"
    override val setupManualPathHint = "Исполняемый файл git или папка, в которой он лежит."
    override val setupSearchTitle = "ПОИСК"
    override val setupSearchHint =
        "PATH → стандартные пути Windows / macOS / Linux → проверка запуском `git --version`."
    override val setupAfterInstall =
        "После установки нажмите CHECK PATH — приложение перепроверит git и откроет Welcome."
    override val setupGitNotFound = "Не удалось найти системный git."
    override val setupNotFoundProblem =
        "Git не найден. Установите его или укажите путь к исполняемому файлу вручную."

    override fun setupTooOldProblem(
        foundMajor: Int,
        foundMinor: Int,
        minimumMajor: Int,
        minimumMinor: Int,
    ) = "Git $foundMajor.$foundMinor старше минимальной версии $minimumMajor.$minimumMinor."

    override fun setupManualProblem(path: String) =
        "Не удалось запустить git по пути «$path». Укажите исполняемый файл git " +
            "или папку, в которой он лежит."

    override val installWingetTitle = "Установить через winget"
    override val installWingetDetail = "winget запросит повышение прав администратора."
    override val installPortableTitle = "PortableGit — без прав администратора"
    override val installPortableDetail =
        "Скачайте PortableGit со страницы Git for Windows, распакуйте в любую папку " +
            "и укажите путь к git.exe."
    override val installDownloadTitle = "Открыть страницу загрузки"
    override val installWindowsDetail = "Официальный установщик Git for Windows."
    override val installXcodeTitle = "Установить Command Line Tools"
    override val installXcodeDetail =
        "xcode-select поставит Command Line Tools, включая git. Откроется системный диалог."
    override val installBrewTitle = "Установить через Homebrew"
    override val installBrewDetail =
        "Пользовательская установка без прав администратора. Homebrew уже найден."
    override val installMacDetail = "Официальный установщик git для macOS."

    override fun installPackageManagerTitle(name: String) = "Установить через $name"

    override val installPackageManagerDetail =
        "Пакетный менеджер дистрибутива. Потребуются права администратора."
    override val installLinuxDetail = "Инструкции по установке для Linux-дистрибутивов."

    override fun updateCurrent(version: String) = "установлено: $version"
    override val updateHint =
        "Выберите явное обновление. Ничего не выполняется до нажатия «ОБНОВИТЬ»."
    override val updateRun = "ОБНОВИТЬ"
    override val updateWingetTitle = "Обновить через winget"
    override val updateWingetDetail =
        "winget запросит права администратора и обновит Git.Git."
    override val updateBrewTitle = "Обновить через Homebrew"
    override val updateBrewDetail = "Homebrew уже найден; права администратора не нужны."
    override fun updatePackageManagerTitle(name: String) = "Обновить через $name"
    override val updatePackageManagerDetail =
        "Пакетный менеджер обновит git до доступной версии. Потребуются права администратора."

    override val whereLabel = "КУДА"
    override val clonePlaceholder = "папка или будущий путь"

    override fun cloneWillCreate(path: String) = "будет создано: $path"
    override val cloneInvalidUrl =
        "URL clone не должен начинаться с '-' и должен использовать http, https, ssh, git или локальный путь."
    override val cloneInvalidTarget =
        "Выберите существующую папку или путь без сегментов '.'/'..'."

    override val folderLabel = "ПАПКА"
    override val initPlaceholder = "путь к новой или существующей папке"
    override fun initHint(branch: String) = "git init -b $branch · папка может не существовать"

    override val pickerEmptyRecent = "пока пусто"
    override val pickerRecentHint =
        "История и drag & drop — быстрее,\nчем ходить по папкам."
    override val pickerTargetHint = "Выбор папки для текущей операции."
    override val pickerPathRepo = "путь к папке или репозиторию"
    override val pickerPathTarget = "папка или будущий путь"
    override val pickerPathInit = "путь к новой или существующей папке"
    override val pickerRepoNotFound =
        "git-репозиторий здесь не найден — откроем папку, git проверит сам."
    override val pickerNoSubfolders = "нет подпапок"
    override val directoryNotFound = "Папка не найдена или это не каталог."
    override val directoryNoAccess = "Нет доступа к содержимому папки."

    override val repoBareNotice =
        "у bare-репозитория нет working tree — changes и commit недоступны; " +
            "history и refs работают."
    override val repoNothingStaged = "ничего не проиндексировано"
    override val repoWorktreeClean = "рабочее дерево чистое"
    override val repoSelectFile = "выбери файл слева"
    override val repoDiffHint = "hunk/строки · git apply --cached"
    override val repoDiffUnified = "единый"
    override val repoDiffSplit = "рядом"
    override val repoDiffIgnoreWhitespace = "проб."
    override val repoDiffIgnoreCase = "регистр"
    override val repoDiffWordSyntax = "word diff · подсветка"
    override fun repoDiffHiddenChanges(count: Int) =
        "$count изменени${if (count == 1) "е" else "й"} скрыто фильтром пробелов/регистра"
    override val repoBinaryTitle = "БИНАРНЫЙ ФАЙЛ"
    override val repoBinaryImageTitle = "DIFF ИЗОБРАЖЕНИЯ"
    override val repoBinaryOld = "старое"
    override val repoBinaryNew = "новое"
    override val repoBinaryMissing = "отсутствует"
    override val repoBinaryNoText = "бинарное содержимое — текстовый diff недоступен"
    override val repoBinaryImageUnavailable = "не удалось декодировать изображение"
    override val repoBinarySideBySide = "рядом"
    override val repoBinaryOnion = "наложение"
    override val repoBinaryOpacity = "прозрачность"
    override val repoDiffLargeTitle = "БОЛЬШОЙ ФАЙЛ"
    override fun repoDiffTooLarge(size: String, limit: String) =
        "Лимит предпросмотра — $limit; эта сторона — $size. Diff не загружен."
    override val repoMergeTitle = "MERGE EDITOR"
    override val repoMergeConflicts = "конфликты"
    override val repoMergeBase = "base"
    override val repoMergeOurs = "ours"
    override val repoMergeTheirs = "theirs"
    override val repoMergeManual = "вручную"
    override val repoMergeSave = "сохранить маркеры"
    override val repoMergeReload = "перечитать"
    override val repoMergeTakeAllOurs = "взять всё наше"
    override val repoMergeTakeAllTheirs = "взять всё их"
    override val repoMergeUseOurs = "взять наше"
    override val repoMergeUseTheirs = "взять их"
    override val repoMergeApply = "применить выбранное"
    override val repoMergeClear = "сбросить"
    override val repoMergeNoConflicts =
        "маркеров конфликтов нет — сохраните файл, чтобы оставить разрешённую версию"
    override val repoMergeMalformed =
        "часть маркеров конфликтов неполная; исправьте их вручную"
    override val repoMergeBinary =
        "бинарный конфликт — разрешите его во внешнем редакторе"
    override val repoMergeTooLarge =
        "конфликтный файл слишком большой для merge editor"
    override val repoMergeMissingFile = "конфликтный файл отсутствует в рабочем дереве"
    override val repoMergeSelectionRequired =
        "выберите хотя бы одну строку или сторону"
    override val repoMergeUnavailable = "merge editor недоступен для этого файла"
    override val repoMergeUnsaved = "не сохранено"
    override val repoMergeSaved = "сохранено"
    override val repoMergeStageHint =
        "сохраните файл, затем добавьте его в index, чтобы отметить конфликт разрешённым"
    override val repoMergeManualHint =
        "редактируется весь файл; маркеры сохраняются, пока вы их не удалите"
    override val repoMergeVersionMissing = "нет в index"
    override val repoMergeConflict = "конфликт"
    override val toastMergeSaved = "файл merge сохранён"
    override val repoMergeSaveFailed = "не удалось сохранить файл merge"
    override val repoDiscard = "отбросить"
    override val repoAdd = "добавить"
    override val repoDelete = "удалить"
    override val repoDeleteConfirm = "УДАЛИТЬ"
    override val repoIgnore = "в .gitignore"
    override val repoIgnoreAlready = "уже игнорируется git"
    override val repoIgnoreUnavailable = "В .gitignore можно добавить только untracked-файл."
    override val repoIgnoreUnsupportedPath =
        "Путь содержит символы, которые нельзя безопасно записать в .gitignore."
    override val repoPathUnsafe =
        "Операция заблокирована: путь является symlink или выходит за пределы репозитория."
    override fun repoOpenMetadataBroken(path: String, missing: String) =
        "Git не может открыть репозиторий: служебные данные повреждены.\n" +
            "$path\n" +
            "Отсутствует или недоступно: $missing\n" +
            "Восстановите их из резервной копии или склонируйте репозиторий заново. Рабочие файлы не изменялись."
    override val repoFileTooLarge =
        "Файл слишком большой для безопасного открытия."
    override val gitOutputTruncated =
        "Вывод усечён по пределу безопасности."
    override val repoIgnoreVerifyFailed =
        "Не удалось подтвердить новое правило .gitignore через git check-ignore; файл восстановлен."
    override val repoDiscardFile = "файл"
    override val repoDiscardHunk = "hunk"
    override val repoDiscardLine = "строка"
    override val repoDiscardWarning =
        "Изменения будут удалены из рабочего дерева. Откатить их можно только через сохранённый патч или stash."
    override val repoDiscardFileNote =
        "Отбрасываются только изменения worktree; staged-часть остаётся в index."
    override val repoDiscardPartialNote =
        "Откатывается только выбранное изменение; остальные правки файла сохраняются."
    override val repoDiscardUntrackedNote = "Этот untracked-файл будет удалён."
    override val repoDiscardPatch = "сохранить патч"
    override val repoDiscardStash = "в stash"
    override val repoDiscardConfirm = "ОТБРОСИТЬ"
    override val repoDiscardPatchHint =
        "Патч сохраняется в .git/chan-backups; восстановить можно через git apply."
    override val repoDiscardStashHint = "Stash сохранит восстановимую копию изменений файла."
    override val repoDiscardPatchUnavailable =
        "Для этого файла нельзя создать текстовый патч. Используйте stash или discard без патча."
    override val repoDiscardStashPartial =
        "Stash-бэкап доступен только для файла целиком. Для hunk или строки используйте патч."
    override val repoDiscardUnavailable =
        "Discard недоступен для этой записи (конфликт, переименование, submodule или нет изменений worktree)."
    override val repoDiscardRestoreFailed =
        "Не удалось восстановить исходный worktree после неудачного discard. Проверьте консоль и сохранённый патч."
    override val repoDiscardApplyFailed =
        "Не удалось отбросить выбранное изменение; worktree восстановлен."
    override val repoStageHunk = "hunk в index"
    override val repoUnstageHunk = "hunk из index"
    override val repoConsoleEmpty = "здесь будет каждая выполненная git-команда"
    override val repoCommitSelect = "выберите коммит"
    override val commitInspectLoading = "читаю коммит…"
    override val commitInspectFailed = "не удалось прочитать коммит"
    override val commitDiffEmpty = "текстового diff нет (пустой коммит или binary-файл)"
    override val commitDiffTruncated = "diff обрезан по лимиту размера; статистика полная"
    override val commitStatEmpty = "файлы не изменены"
    override val commitStatBinary = "binary"
    override val commitSignature = "SIGNATURE"
    override val commitSignatureGood = "подпись верна · good signature"
    override val commitSignatureUntrusted = "good signature · ключ не доверен"
    override val commitSignatureBad = "НЕВЕРНАЯ подпись"
    override val commitSignatureExpired = "good signature · истекла"
    override val commitSignatureExpiredKey = "good signature · ключ истёк"
    override val commitSignatureRevoked = "ключ ОТОЗВАН"
    override val commitSignatureCannotCheck = "невозможно проверить подпись"
    override val commitSignatureNone = "без подписи"
    override val commitSignatureUnknown = "статус подписи неизвестен"
    override val commitSigner = "SIGNER"
    override val commitKey = "KEY"
    override val signatureVerifyCommit = "проверить коммит"
    override val signatureVerifyMerge = "проверить merge"
    override val signatureVerifyTag = "проверить тег"
    override val signatureVerificationRunning = "проверяю подпись…"
    override val signatureVerificationTitle = "ПРОВЕРКА ПОДПИСИ"
    override val signatureCheckCommit = "подпись коммита"
    override val signatureCheckMergeCommit = "подпись merge-коммита"
    override val signatureCheckMergeTag = "встроенный тег merge"
    override val signatureCheckTag = "подпись тега"
    override val signatureNoOutput = "git не вывел детали проверки"
    override val signaturePassed = "ПРОВЕРЕНО"
    override val signatureFailed = "НЕ ПРОВЕРЕНО"
    override val toastSignatureVerified = "подпись проверена"
    override val toastSignatureFailed = "подпись не прошла проверку"
    override val pullVerifySignatures = "проверять подписи"
    override val pullVerifySignaturesHint =
        "прервать merge, если tip исходной ветки не имеет валидной доверенной подписи"
    override val advancedCommandDone = "advanced-команда завершена"
    override val advancedStorageHint =
        "count-objects/fsck только читают. gc, repack и prune перезаписывают объекты .git; коммиты восстанавливаются из reflog, но недостижимые объекты могут быть удалены."
    override val advancedDangerHint =
        "Команда изменяет хранилище репозитория. Проверьте точную команду и результат ниже."
    override val advancedPruneWarning =
        "prune удаляет недостижимые объекты. Сначала выполните dry-run; кнопка удаления останется заблокированной до успешного preview."
    override val advancedPrunePreviewRequired =
        "сначала выполните dry-run preview для prune"
    override val advancedSparseHint =
        "Sparse-checkout меняет набор файлов в рабочем дереве; история коммитов и index не переписываются."
    override val advancedSparsePathsRequired = "укажите хотя бы один sparse-путь или паттерн"
    override fun advancedSparseInvalidPath(path: String): String =
        "небезопасный sparse-путь: $path (только относительные пути; без .., абсолютных дисков и паттернов в cone mode)"
    override val advancedLfsHint =
        "Нужно расширение git-lfs. Track обновляет .gitattributes; pull скачивает LFS-объекты; fetch только складывает их в .git/lfs."
    override val advancedLfsPatternInvalid = "некорректный LFS-путь или паттерн"
    override val advancedNoLfs = "git-lfs не установлен или отсутствует в PATH"
    override val advancedNotesHint =
        "Notes хранятся в refs/notes/commits. SAVE заменяет заметку объекта; APPEND добавляет ещё один абзац."
    override val advancedNotesEmpty = "в refs/notes/commits заметок нет"
    override val advancedNoteTargetInvalid = "цель заметки должна указывать на Git-объект"
    override val advancedNoteEmpty = "текст заметки пуст"
    override val advancedNoteTooLong = "заметка слишком длинная (лимит 8000 символов)"
    override val advancedArchiveHint =
        "Архивирует выбранную ревизию в новый ZIP или TAR.GZ. Существующие файлы не перезаписываются."
    override fun advancedArchiveCreated(path: String): String = "архив создан: $path"
    override val advancedBundleHint =
        "CREATE упаковывает все refs в новый bundle. VERIFY и LIST HEADS читают существующий bundle, не меняя репозиторий."
    override fun advancedBundleCreated(path: String): String = "bundle создан: $path"
    override val advancedFsmonitorHint =
        "core.fsmonitor включает встроенный filesystem monitor там, где Git его поддерживает (Windows/macOS). STATUS, START и STOP управляют демоном."
    override val advancedPartialHint =
        "Partial clone выбирается при клонировании. Для существующего репозитория используйте фильтр в fetch из remote manager."
    override val advancedOutputRequired = "нужен путь для файла результата"
    override fun advancedOutputExists(path: String): String =
        "файл уже существует и не будет перезаписан: $path"
    override val advancedOutputParentMissing = "каталог для файла результата не существует"
    override val advancedInputFileMissing = "файл не существует"
    override val advancedRefInvalid = "некорректная ревизия; пробелы и ведущий дефис запрещены"
    override val clonePartialHint =
        "Partial clone скачивает содержимое файлов по требованию. BLOB:NONE откладывает все blob-объекты; TREE:0 не скачивает и деревья."
    override val cloneCancelled =
        "клонирование отменено; в целевой папке могли остаться частичные файлы"
    override val commitFiles = "FILES"
    override val commitActionCheckout = "checkout"
    override val commitActionReset = "reset"
    override val commitActionCherryPick = "cherry-pick"
    override val commitActionRevert = "revert"
    override val commitActionBranch = "branch"
    override val commitActionTag = "tag"
    override val commitActionCopyHash = "копировать hash"
    override val commitActionReflog = "показать в reflog"
    override val commitActionBisectGood = "bisect good"
    override val commitActionBisectBad = "bisect bad"
    override val commitActionCheckoutHint =
        "Checkout отвязывает HEAD и переключает его на этот коммит; локальные изменения могут помешать."
    override val commitActionResetHint =
        "Двигает текущую ветку на этот коммит. Soft сохраняет index/worktree, mixed сбрасывает index, hard — и то и другое, keep сохраняет локальные изменения и отменяется при пересечении."
    override val commitActionResetHardHint =
        "Hard reset отбрасывает незакоммиченные изменения tracked-файлов. Коммиты можно восстановить из reflog, незакоммиченные изменения — нельзя."
    override val commitActionResetPreview = "ПОТЕРИ"
    override val commitActionResetPreviewLoading = "проверяю, что будет потеряно…"
    override val commitActionResetNoLostCommits = "ни один коммит не станет недостижим"
    override val commitActionResetLostCommits = "коммитов покинет ветку (reflog/backup сохраняет)"
    override val commitActionUnreachableObjects = "уникальных объектов в теряемой части (rev-list)"
    override val commitActionFsckAlready = "уже недостижимо по fsck (без reflog)"
    override val commitActionFsckCommits = "коммитов"
    override val commitActionFsckClean = "fsck: недостижимых объектов нет"
    override val commitActionFsckWarning = "fsck завершился ошибкой"
    override val commitRewriteLossPreview = "ПОКИНЕТ ВЕТКУ"
    override val commitActionResetStaged = "в индексе"
    override val commitActionResetUnstaged = "вне индекса"
    override val commitActionResetConflicted = "с конфликтами"
    override val commitActionResetUntracked = "неотслеживаемых"
    override val commitActionResetSoftPreview =
        "Index и worktree не меняются; двигается только ветка."
    override val commitActionResetMixedPreview =
        "Worktree не меняется; staged-состояние сбрасывается."
    override val commitActionResetHardPreview =
        "Незакоммиченные изменения tracked-файлов будут отброшены."
    override val commitActionResetKeepPreview =
        "Локальные изменения сохраняются; reset отменится при пересечении с target."
    override val commitActionResetKeepBlocked =
        "Reset отменится: локальные изменения пересекаются с файлами, которые меняет target."
    override val commitActionResetOverlapping = "пересекается"
    override val commitActionResetUntrackedPreview =
        "Неотслеживаемые файлы сохраняются, если target не перезаписывает их путь."
    override val commitActionResetMore = "ещё"
    override val commitActionRewrite = "amend / reword"
    override val commitActionSplit = "разбить коммит"
    override val commitRewriteHint =
        "Переписывает выбранный коммит и повторяет его потомков. Worktree должен быть чистым; merge-коммиты среди потомков пока не поддерживаются. Split/amend требуют обычный target. Старый tip остаётся в reflog."
    override val commitRewriteMessage = "сообщение"
    override val commitRewriteAuthor = "автор (Name <email>)"
    override val commitRewriteLoading = "загружаю сообщение коммита…"
    override val commitRewriteMode = "режим"
    override val commitRewriteModeReword = "reword"
    override val commitRewriteModeAmend = "amend"
    override val commitRewriteModeSplit = "split"
    override val commitRewriteRewordHint =
        "Меняет только сообщение и автора; дерево коммита остаётся прежним."
    override val commitRewriteAmendHint =
        "Запускает редактируемый amend: изменения target попадут в index, сообщение и автор будут заполнены. Создайте замену и завершите rewrite."
    override val commitRewriteSplitHint =
        "Запускает edit + reset: изменения target останутся unstaged. Создайте несколько коммитов и завершите rewrite."
    override val commitRewriteStart = "НАЧАТЬ"
    override val commitRewriteCleanRequired =
        "Закоммитьте или спрячьте все изменения перед переписыванием истории."
    override val commitRewriteMergeUnsupported =
        "Merge-коммиты не поддерживаются для split/amend, а merge среди потомков — пока нет."
    override val commitRewriteRootUnsupported =
        "Корневой коммит нельзя разбить или amend-нуть; reword доступен."
    override val commitRewriteNotInHistory = "Коммит не входит в текущую историю HEAD."
    override val commitRewritePending = "Сначала завершите или отмените текущий rewrite."
    override val commitRewriteActiveOperation = "Сначала завершите активную git-операцию."
    override val commitRewriteBare = "Переписывание истории недоступно для bare-репозитория."
    override val commitRewriteNoReplacement =
        "Создайте хотя бы один заменяющий коммит перед завершением."
    override val commitRewriteOriginalAlreadyPresent =
        "В ветке уже есть исходный tip; вернитесь на заменяющие коммиты или отмените rewrite."
    override val commitRewriteNotOnParent =
        "Текущая ветка не основана на parent переписываемого коммита."
    override val commitRewriteInvalidMode = "Для прямого переписывания используйте reword."
    override val commitRewriteInvalidTree = "Не удалось прочитать дерево коммита для rewrite."
    override val commitRewriteBranchRequired =
        "Переключитесь на локальную ветку перед переписыванием истории."
    override val commitRewriteBranchChanged =
        "Текущая ветка изменилась; вернитесь на исходную ветку, чтобы завершить или отменить rewrite."
    override val commitRewriteInProgress = "ПЕРЕПИСЫВАНИЕ"
    override val commitRewriteFinish = "ЗАВЕРШИТЬ"
    override val commitRewriteCancel = "ОТМЕНИТЬ"
    override val commitRewriteCancelWarning =
        "Отмена вернёт исходный tip и отбросит заменяющие коммиты и незакоммиченные изменения."
    override val commitRewriteConfirmCancel = "ПОДТВЕРДИТЬ ОТМЕНУ"
    override val commitRewriteAmendInstruction =
        "Застейджите изменения и создайте amend-коммит, затем завершите."
    override val commitRewriteSplitInstruction =
        "Застейджите и создайте заменяющие коммиты, затем завершите."
    override val commitRewriteNoAmend =
        "Во время rewrite используйте обычный commit; не делайте amend parent-коммита."
    override val commitActionInteractiveRebase = "интерактивный rebase отсюда"
    override val commitActionDelete = "удалить коммит"
    override val commitActionDeleteHint =
        "Выберите drop, reset --hard, revert или filter-repo; каждый способ объясняет, что переписывает и что сохраняет."
    override val commitActionHistoryRewrite = "переписывание истории / перенос диапазона"
    override val commitActionHistoryRewriteHint =
        "Перестановка соседних коммитов, git rebase --onto, перенос диапазона или squash диапазона; история переписывается с backup и Undo."
    override val historyRewriteTitle = "ПЕРЕПИСЫВАНИЕ ИСТОРИИ"
    override val historyRewriteHint =
        "Все операции переписывают текущую ветку. Перед первым изменением создаётся backup-ref."
    override val historyRewriteModeReorder = "ПЕРЕСТАНОВКА"
    override val historyRewriteModeOnto = "REBASE --ONTO"
    override val historyRewriteModeMoveRange = "ПЕРЕНОС ДИАПАЗОНА"
    override val historyRewriteModeSquash = "SQUASH ДИАПАЗОНА"
    override val historyRewriteReorderHint =
        "Поменять коммит с соседним родителем (раньше) или ребёнком (позже). Для произвольного порядка используйте редактор интерактивного rebase с drag&drop."
    override val historyRewriteEarlier = "РАНЬШЕ"
    override val historyRewriteLater = "ПОЗЖЕ"
    override val historyRewriteEarlierHint =
        "Поменять с родителем; потомки переприменяются. Возможны конфликты."
    override val historyRewriteLaterHint =
        "Поменять с ребёнком в текущей ветке; последующие коммиты переприменяются."
    override val historyRewriteNoEarlier = "Нет родителя: коммит нельзя переместить раньше."
    override val historyRewriteNoLater =
        "В текущей ветке нет единственного ребёнка: коммит нельзя переместить позже."
    override val historyRewriteOntoHint =
        "Переприменяет коммиты после upstream (не включая) на новый base. Только текущая ветка; merge-коммиты линеаризуются, если не включено сохранение merge."
    override val historyRewriteMoveRangeHint =
        "Переносит коммиты после начала диапазона до текущего HEAD на новый base. Диапазон должен заканчиваться на HEAD; A..B не включает A."
    override val historyRewriteSquashHint =
        "Собирает A..B в один коммит (A не включается; для включения A используйте A^..B). Последующие коммиты переприменяются; merge в диапазоне отклоняются."
    override val historyRewriteUpstream = "upstream (не включая)"
    override val historyRewriteOnto = "новый base (onto)"
    override val historyRewriteRange = "диапазон A..B"
    override val historyRewriteRangePlaceholder = "A..B (A не включается)"
    override val historyRewriteMessage = "сообщение squash (необязательно)"
    override val historyRewriteMessageHint = "Пусто — оставить сообщение первого коммита."
    override val historyRewritePreserveMerges = "сохранять merge (--rebase-merges)"
    override val historyRewriteRun = "ЗАПУСТИТЬ"
    override val historyRewriteCurrentBranch = "текущая ветка"
    override val historyRewriteRangeExcludes = "A..B не включает A; для включения A используйте A^..B."
    override val historyRewriteEmptyRef = "Upstream и новый base не должны быть пустыми."
    override val historyRewriteNotAncestor = "Upstream не является предком текущей ветки."
    override val historyRewriteEmptyRange = "После upstream нет коммитов для переприменения."
    override val historyRewriteRangeNotHead = "Для переноса диапазон должен заканчиваться на текущем HEAD."
    override val historyRewriteRangeNotInHistory = "Границы диапазона не входят в текущую историю."
    override val historyRewriteRangeTooShort = "Для squash диапазона нужно минимум два коммита."
    override val historyRewriteRangeMerge = "Squash диапазона не поддерживает merge-коммиты."
    override val historyRewriteRangeNotLinear =
        "Диапазон не является непрерывной последовательностью pick в rebase-плане."
    override val reorderMergeUnsupported =
        "Перестановка merge-коммита не поддерживается; используйте редактор интерактивного rebase."
    override val reorderNoParent = "У коммита нет родителя для перестановки."
    override val reorderNoChild = "У коммита нет ребёнка в текущей ветке для перестановки."
    override val reorderAmbiguousChild =
        "У коммита несколько детей в текущей ветке; перестановка неоднозначна."
    override val reorderTargetMissing = "Соседний коммит отсутствует в текущем rebase-плане."
    override val reorderNotAdjacent = "Из этого диалога можно менять местами только соседние коммиты."
    override val toastRebaseOntoDone = "rebase --onto завершён"
    override val deleteCommitTitle = "УДАЛИТЬ КОММИТ"
    override val deleteCommitHint =
        "Один и тот же коммит можно убрать четырьмя способами. Выберите тот, последствия которого подходят вашей истории."
    override val deleteCommitDropTitle = "drop в интерактивном rebase"
    override val deleteCommitDropHint =
        "Сохраняет потомков, переприменяя их поверх родителя. История переписывается, возможны конфликты; создаётся backup-ref и доступен Undo."
    override val deleteCommitDropMergeWarning =
        "Merge в --rebase-merges — это строка `merge -C`, а не `pick`. Удаление этой строки неявно выбросит pick-коммиты боковой ветки, поэтому для merge drop отключён; используйте reset или filter-repo."
    override val deleteCommitDropAction = "ОТКРЫТЬ REBASE TODO"
    override val deleteCommitResetTitle = "reset --hard на родителя (отрезать хвост)"
    override val deleteCommitResetHint =
        "Перемещает текущую ветку на родителя и удаляет этот коммит вместе со всеми потомками. Незакоммиченные изменения отслеживаемых файлов теряются (untracked-файлы остаются); backup-ref и Undo восстанавливают tip ветки, а не рабочее дерево."
    override val deleteCommitResetMergeWarning =
        "Это merge-коммит: reset --hard использует первого родителя и отбрасывает линию второго родителя вместе со всеми потомками."
    override val deleteCommitResetAction = "RESET --HARD"
    override val deleteCommitRevertTitle = "revert (без переписывания)"
    override val deleteCommitRevertHint =
        "Создаёт новый коммит с обратным diff; безопасно для уже отправленной истории. Для merge-коммита нужен явный -m mainline."
    override val deleteCommitRevertAction = "REVERT"
    override val deleteCommitFilterTitle = "filter-repo (вся история)"
    override val deleteCommitFilterHint =
        "Переписывает все refs и теги, убирая коммит из всей локальной истории; origin remotes удаляются, reflogs и старые объекты вычищаются. fast-export работает с деревьями, а не diff, поэтому потомок может сохранить те же файлы. Чтобы убрать изменение из одной ветки, используйте drop/rebase."
    override val deleteCommitFilterAction = "ЗАПУСТИТЬ FILTER-REPO"
    override val deleteCommitFilterConfirm = "Понимаю: переписывание необратимо"
    override val deleteCommitFilterMissing = "git filter-repo не установлен."
    override val deleteCommitFilterInstall =
        "Установите его через pip install git-filter-repo (или пакетным менеджером), затем откройте диалог снова."
    override val deleteCommitFilterBackup = "Перед переписыванием создаётся bundle-backup:"
    override val deleteCommitFilterBackupFailed =
        "Не удалось создать backup-bundle; filter-repo не запускался."
    override val deleteCommitRootNoParent =
        "У корневого коммита нет родителя; reset --hard не может его удалить. Используйте drop, revert или filter-repo."
    override val deleteCommitMergeRevert =
        "Для revert merge-коммита нужен -m <parent>; выберите drop или filter-repo."
    override val deleteCommitNotInHistory =
        "Коммит недостижим из текущей истории."
    override val deleteCommitDirty =
        "Закоммитьте или уберите в stash все изменения перед удалением через filter-repo."
    override val deleteCommitStashes =
        "filter-repo удаляет stashes; примените или удалите их перед чисткой истории."
    override val deleteCommitBackupNote =
        "reset создаёт backup-ref (Undo); filter-repo создаёт bundle, потому что вычищает старые объекты и не отменяется обычным Undo."
    override fun deleteCommitFilterBundle(path: String) = "Backup bundle: $path"
    override fun toastFilterRepoDone(path: String) =
        "filter-repo завершён; backup bundle: $path"
    override val commitActionFilterRepo = "filter-repo: удалить / заменить / переименовать"
    override val commitActionFilterRepoHint =
        "Переписывает все локальные refs: удаляет файл или папку, секрет/строку или переписывает пути. Создаются dry-run предпросмотр и обязательные backup-ref и bundle."
    override val filterRepoTitle = "FILTER-REPO"
    override val filterRepoHint =
        "Переписывание всей истории и рабочего дерева. Выбранный коммит — только контекст; фильтруются ветки, теги и остальные локальные refs."
    override val filterRepoModeRemove = "УДАЛИТЬ ПУТЬ"
    override val filterRepoModeReplace = "ЗАМЕНИТЬ / СЕКРЕТ"
    override val filterRepoModeRename = "ПЕРЕИМЕНОВАТЬ ПУТЬ"
    override val filterRepoRemovePath = "путь к файлу или папке"
    override val filterRepoRemovePathHint =
        "Точный путь от корня репозитория. Папки удаляются рекурсивно; glob-шаблоны не принимаются."
    override val filterRepoFind = "найти текст или секрет"
    override val filterRepoReplacement = "заменить на"
    override val filterRepoReplaceHint =
        "Значение записывается во временный файл выражений и не попадает в командную строку или лог. Пустая замена удаляет совпадение; по умолчанию ***REMOVED***."
    override val filterRepoRegex = "regex"
    override val filterRepoOldPath = "старый путь"
    override val filterRepoNewPath = "новый путь"
    override val filterRepoRenameHint =
        "Переименовывает точный префикс старого пути в новый во всех выбранных refs; переименования автоматически не отслеживаются."
    override val filterRepoPreview = "ПРЕДПРОСМОТР (--dry-run)"
    override val filterRepoPreviewRunning = "DRY-RUN…"
    override val filterRepoPreviewTitle = "ПРЕДПРОСМОТР DRY-RUN"
    override val filterRepoPreviewHint =
        "Сначала запустите предпросмотр; он не меняет историю и не создаёт backup."
    override val filterRepoChecking = "проверка git filter-repo…"
    override val filterRepoBackupNote =
        "Перед переписыванием создаётся backup-ref в refs/mybeautifulgit/backups и bundle всех refs; оба всё ещё содержат старый секрет/историю. Храните их безопасно и удалите после проверки. Обычный Undo не предлагается, потому что переписываются все refs; восстанавливайте из ref/bundle. Ветки/теги после переписывания нужно отправить через force-push. Запуск заблокирован до подтверждения."
    override val filterRepoConfirm = "Понимаю: все refs будут переписаны"
    override val filterRepoRun = "ЗАПУСТИТЬ FILTER-REPO"
    override val filterRepoPathRequired = "Введите путь к файлу или папке."
    override val filterRepoPathInvalid =
        "Используйте точный относительный путь без glob, '..' и абсолютных путей."
    override val filterRepoFindRequired = "Введите текст или секрет для замены."
    override val filterRepoRegexInvalid = "Некорректное регулярное выражение."
    override val filterRepoRenameRequired = "Введите старый и новый пути."
    override val filterRepoRenameInvalid =
        "Используйте два разных точных относительных пути без glob, '..' и абсолютных путей."
    override val filterRepoNoRefs = "Нет локальных refs для переписывания."
    override val filterRepoPreviewOutput =
        "dry-run filter-repo завершился без файлов предпросмотра; история не изменена."
    override val filterRepoBackupFailed =
        "Не удалось создать обязательный backup-ref или bundle; filter-repo не запускался."
    override val filterRepoPreviewFailed =
        "dry-run filter-repo завершился ошибкой; история не изменена."
    override val filterRepoContentPreviewHint =
        "При замене текста число путей не меняется; масштаб показывают изменённые refs и число коммитов."
    override val filterRepoAllRefs = "все локальные refs; backup-refs исключены"
    override fun filterRepoPreviewSummary(before: Int, after: Int, refs: Int, paths: Int) =
        "dry-run: прочитано $before коммитов, в отфильтрованной истории $after; изменятся $refs refs и $paths путей."
    override fun filterRepoFailure(detail: String, ref: String, bundle: String) =
        "$detail\nBackup ref: $ref\nBackup bundle: $bundle"
    override fun toastFilterRepoRewritten(ref: String, bundle: String) =
        "filter-repo завершён; backup ref: $ref; bundle: $bundle"
    override val commitActionCherryPickHint =
        "Применяет коммит поверх HEAD; конфликты останутся в репозитории для разрешения."
    override val commitActionRevertHint =
        "Создаёт новый коммит, который отменяет изменения этого коммита."
    override val commitActionRange = "диапазон"
    override val commitActionRangePlaceholder = "A..B (синтаксис ревизий git)"
    override val commitActionRangeHint =
        "Режим диапазона игнорирует выбранный коммит. Границы включительно; сам A не применяется."
    override val commitActionNoCommit = "--no-commit"
    override val commitActionNoCommitHint =
        "Применяет изменения в index без коммитов. Конфликт приостанавливает диапазон до разрешения и stage."
    override val commitActionInvalidRange =
        "Введите диапазон ревизий вида A..B без пробелов и ведущего дефиса."
    override val commitActionEmptyRange = "В диапазоне ревизий нет коммитов."
    override val commitActionBranchName = "имя ветки"
    override val commitActionBranchHint =
        "Создаёт ветку на этом коммите без переключения на неё."
    override val commitActionTagName = "имя тега"
    override val commitActionTagMessage = "сообщение тега (необязательно)"
    override val commitActionTagHint =
        "Пустое сообщение создаёт lightweight-тег; сообщение создаёт annotated-тег; sign использует git tag -s и требует сообщение."
    override val tagSign = "sign"
    override val tagSignHint = "signed-тег: git tag -s (нужно сообщение и настроенный ключ подписи)"
    override val tagSignNeedsMessage = "Для signed-тега нужно сообщение."
    override val tagNoCommits = "Нельзя создать тег до первого коммита."
    override val tagNotFound = "Тег не найден."
    override val tagManagerTitle = "TAGS"
    override val tagManagerNoTags = "локальных тегов нет"
    override val tagManagerSelect = "Выберите тег для управления."
    override val tagCreate = "новый тег"
    override val tagPush = "push"
    override val tagPushAll = "push все теги"
    override val tagDeleteLocal = "удалить локально"
    override val tagDeleteRemote = "удалить на remote"
    override val tagCreateHint =
        "Пустое сообщение — lightweight-тег, сообщение — annotated-тег, sign включает signed-тег."
    override val tagPushRemote = "remote"
    override val tagPushHint = "Отправляет только выбранный тег на выбранный remote."
    override val tagPushAllHint = "Отправляет все локальные теги через git push --tags."
    override val tagDeleteLocalHint = "Удаляет только локальный тег; тег на remote не затрагивается."
    override val tagDeleteRemoteHint = "Удаляет тег на remote; локальный тег остаётся."
    override val tagAnnotated = "annotated"
    override val tagLightweight = "lightweight"
    override val tagNoRemotes = "remote не настроены"
    override val remoteManagerTitle = "REMOTES"
    override val remoteManagerNoRemotes = "remote не настроены"
    override val remoteManagerSelect = "Выберите remote для управления."
    override val remoteAdd = "добавить remote"
    override val remoteRemove = "удалить"
    override val remoteEditUrl = "изменить URL"
    override val remoteFetch = "fetch"
    override val remoteName = "имя remote"
    override val remoteUrl = "URL remote"
    override val remoteAddHint = "Добавляет remote через git remote add."
    override val remoteRemoveHint =
        "Удаляет URL remote и его remote-tracking ветки. Репозиторий на сервере не меняется."
    override val remoteEditUrlHint = "Меняет URL для последующих fetch и push."
    override val remoteUrlCredentialsHidden =
        "Сохранённый URL содержит credentials и скрыт. Введите полный URL, чтобы заменить его."
    override val remoteFetchHint =
        "Fetch выбранного remote; каждая опция напрямую соответствует флагу git fetch."
    override val remoteFetchPrune = "prune"
    override val remoteFetchTagsDefault = "теги: по умолчанию"
    override val remoteFetchTags = "--tags"
    override val remoteFetchNoTags = "--no-tags"
    override val remoteFetchDepth = "глубина (например, 1)"
    override val remoteFetchFilter = "filter (например, blob:none)"
    override val remoteFetchFilterHint =
        "Фильтр partial clone; сервер должен поддерживать фильтрацию."
    override val remoteHead = "remote HEAD"
    override val remoteHeadUnset = "не задан"
    override val remoteHeadAuto = "auto"
    override val remoteHeadSet = "задать"
    override val remoteHeadDelete = "удалить"
    override val remoteHeadAutoHint =
        "Указывает refs/remotes/<remote>/HEAD на HEAD remote. Сначала сделайте fetch, чтобы refs существовали."
    override val remoteHeadSetHint =
        "Указывает refs/remotes/<remote>/HEAD на выбранную remote-tracking ветку."
    override val remoteHeadDeleteHint = "Удаляет только символический refs/remotes/<remote>/HEAD."
    override val remoteHeadBranch = "ветка на remote"
    override val remoteNoBranches = "remote-tracking веток нет"
    override val remoteInvalidName =
        "Имя remote пустое, начинается с '-' или содержит пробелы, управляющие символы или '..'."
    override val remoteInvalidUrl =
        "URL remote должен использовать https, ssh или git; file:// и ext:: запрещены."
    override val remoteAlreadyExists = "Remote с таким именем уже существует."
    override val remoteInvalidDepth = "Глубина fetch должна быть положительным целым числом."
    override val remoteInvalidFilter = "Некорректный фильтр partial clone."
    override val remoteNotFound = "Remote не настроен."
    override val remoteProfileLoading = "чтение идентичности remote…"
    override val remoteProfileFailed = "не удалось прочитать идентичность remote"
    override val remoteIdentityLocal = "локальный путь"
    override val remoteIdentityUnknown = "UNKNOWN"
    override val remotePreflightPermissions =
        "Права, pull request/MR и checks на уровне custom-host 1 остаются UNKNOWN; API не вызывается."
    override val remotePreflightDetached = "detached HEAD"
    override val remotePreflightUnborn = "unborn-ветка"
    override val remotePreflightUnset = "не задан"
    override val toastRemoteAdded = "remote добавлен"
    override val toastRemoteRemoved = "remote удалён"
    override val toastRemoteUrlUpdated = "URL remote изменён"
    override val toastFetched = "fetch выполнен"
    override val toastRemoteHeadSet = "remote HEAD обновлён"
    override val toastRemoteHeadDeleted = "remote HEAD удалён"
    override val submoduleManagerTitle = "SUBMODULES"
    override val submoduleManagerNoSubmodules = "submodules отсутствуют"
    override val submoduleManagerSelect = "Выберите submodule или управляйте всеми сразу."
    override val submoduleAll = "все submodules"
    override val submoduleInit = "init"
    override val submoduleUpdate = "update"
    override val submoduleSync = "sync"
    override val submoduleAdd = "добавить submodule"
    override val submoduleRemove = "удалить"
    override val submoduleRecursive = "recursive"
    override val submoduleInitOnUpdate = "init"
    override val submoduleCheckout = "checkout"
    override val submoduleCustomUpdateWarning =
        "Без --checkout Git может выполнить submodule.<name>.update из локального config, включая произвольную команду."
    override val submoduleCustomUpdateConfirm = "разрешить configured update"
    override val submodulePath = "путь"
    override val submoduleUrl = "URL репозитория"
    override val submoduleInitHint =
        "Прописывает URL submodules в .git/config; рабочие деревья не выгружает."
    override val submoduleUpdateHint =
        "Выгружает коммиты, зафиксированные суперпроектом, через --checkout; init также инициализирует неинициализированные submodules. Настроенные custom update команды требуют явного подтверждения."
    override val submoduleSyncHint =
        "Обновляет URL submodules из .gitmodules; не выполняет fetch и checkout."
    override val submoduleAddHint =
        "Клонирует репозиторий в путь, добавляет .gitmodules и stage для gitlink."
    override val submoduleRemoveHint =
        "Выполняет submodule deinit, затем git rm. Checkout удаляется; .git/modules может остаться для восстановления."
    override val submoduleInvalidPath =
        "Путь submodule должен быть относительным, использовать '/' и не содержать '..' или ведущий '-'."
    override val submoduleInvalidUrl =
        "URL submodule должен использовать https, ssh или git; file:// и ext:: запрещены."
    override val submoduleNotFound = "Такого submodule нет в репозитории."
    override val submoduleAlreadyExists = "По этому пути уже есть submodule."
    override val submoduleNone = "В этом репозитории нет submodules."
    override val submoduleStateCurrent = "актуален"
    override val submoduleStateModified = "изменён"
    override val submoduleStateUninitialized = "не инициализирован"
    override val submoduleStateConflict = "конфликт"
    override val toastSubmoduleInitialized = "submodule init выполнен"
    override val toastSubmodulesUpdated = "submodules обновлены"
    override val toastSubmodulesSynced = "URL submodules синхронизированы"
    override val toastSubmoduleAdded = "submodule добавлен"
    override val toastSubmoduleRemoved = "submodule удалён"
    override val pushManagerTitle = "PUSH"
    override val pushModeCurrent = "текущая ветка"
    override val pushModeAnother = "в другую ветку"
    override val pushModeTags = "все теги"
    override val pushModeAllBranches = "все ветки (--all)"
    override val pushModeMirror = "mirror (--mirror)"
    override val pushModeDelete = "удалить ветку на remote"
    override val pushModeRefspec = "произвольный refspec"
    override val pushRemote = "remote"
    override val pushDestination = "ветка / ref на remote"
    override val pushBranch = "ветка на remote"
    override val pushRefspecPlaceholder = "source:destination"
    override val pushForce = "force"
    override val pushForceNone = "нет"
    override val pushForceLease = "--force-with-lease"
    override val pushForceFlag = "--force"
    override val pushForceNoneHint = "Без force; git отклонит non-fast-forward обновление."
    override val pushForceLeaseHint =
        "Обновляет remote, только если он всё ещё указывает на ожидаемый коммит."
    override val pushForceHint =
        "Перезаписывает remote, даже если там есть коммиты, которых нет локально."
    override val pushHintCurrent =
        "Публикует текущую ветку и устанавливает её upstream."
    override val pushHintAnother =
        "Публикует текущую ветку под другим именем на remote, не меняя upstream."
    override val pushHintTags = "Публикует все теги через git push --tags."
    override val pushHintAllBranches = "Публикует все локальные ветки через git push --all."
    override val pushHintMirror =
        "Зеркалирует все refs; remote refs, которых нет локально, будут удалены."
    override val pushHintDelete =
        "Удаляет ветку на remote; локальная ветка не меняется."
    override val pushHintRefspec =
        "Публикует ровно введённый refspec без переписывания."
    override val pushNoCurrentBranch = "Не удалось определить текущую ветку (detached HEAD)."
    override val pushInvalidRefspec = "Некорректный push refspec."
    override val pushNoRemotes = "remote не настроены"
    override val pushConfirm = "PUSH"
    override val toastPushed = "push выполнен"
    override val toastAllBranchesPushed = "все ветки отправлены"
    override val toastMirrorPushed = "mirror отправлен"
    override val pullManagerTitle = "PULL"
    override val pullModeMerge = "merge"
    override val pullModeRebase = "rebase"
    override val pullModeFfOnly = "ff-only"
    override val pullRemote = "remote"
    override val pullBranch = "ветка на remote"
    override val pullCurrentBranch = "текущая ветка"
    override val pullHintMerge =
        "Использует git pull --no-rebase; расходящиеся истории создадут merge-коммит."
    override val pullHintRebase =
        "Использует git pull --rebase; локальные коммиты переносятся поверх remote-ветки."
    override val pullHintFfOnly =
        "Использует git pull --ff-only; откажется работать, если обновление не fast-forward."
    override val pullLocalChangesHint =
        "Локальные изменения могут помешать pull; сначала закоммитьте или спрячьте их в stash."
    override val pullNoCurrentBranch = "Нельзя выполнить pull в detached HEAD."
    override val pullNoRemotes = "remote не настроены"
    override val pullConfirm = "PULL"
    override val toastPulled = "pull выполнен"
    override val commitActionBisectHint =
        "Запускает bisect при необходимости и помечает этот коммит. Отметьте другой коммит, чтобы сузить диапазон."
    override val commitActionInvalidHash = "Некорректный hash коммита."
    override val commitActionInvalidRef = "Некорректное имя ветки или тега."
    override val commitReflogEmpty = "В reflog нет записей, указывающих на этот коммит."
    override val commitReflogLoading = "читаю reflog…"
    override val commitReflogFailed = "не удалось прочитать reflog"
    override val reflogPanelHint =
        "Все переходы HEAD и веток. Выберите запись, чтобы восстановить текущую ветку или пересоздать удалённую."
    override val reflogFilterPlaceholder = "фильтр по ref, коммиту или сообщению"
    override val reflogNoMatches = "Ни одна запись reflog не подходит под фильтр."
    override val reflogOpenCommit = "открыть в log"
    override val reflogRestore = "восстановить"
    override val reflogRestoreHint =
        "Сбросить текущую ветку на этот коммит. В подтверждении по умолчанию --hard; доступны soft, mixed и keep."
    override val reflogCreateBranch = "ветка"
    override val reflogCreateBranchHint =
        "Создать ветку на этом коммите — пригодится для удалённой ветки. Имя подставляется, если его можно вывести из ref."
    override val reflogActionHint =
        "Перед восстановлением показываются точная git-команда и последствия."
    override val toastCommitCheckout = "checkout выполнен"
    override val toastCommitReset = "reset выполнен"
    override val toastCommitReworded = "коммит переписан"
    override val toastCommitRewriteStarted = "rewrite начат"
    override val toastCommitRewriteFinished = "rewrite завершён"
    override val toastCommitRewriteCancelled = "rewrite отменён"
    override val toastCommitCherryPicked = "cherry-pick выполнен"
    override val toastCommitReverted = "revert выполнен"
    override val toastBranchCreated = "ветка создана"
    override val toastBranchRenamed = "ветка переименована"
    override val toastBranchDeleted = "ветка удалена"
    override val toastRemoteBranchDeleted = "удалённая ветка удалена"
    override val toastBranchUpstreamSet = "upstream установлен"
    override val toastBranchPushed = "ветка отправлена"
    override val toastRemoteBranchRenamed = "удалённая ветка переименована"
    override val branchManagerTitle = "BRANCHES"
    override val branchManagerLocal = "LOCAL"
    override val branchManagerRemote = "REMOTE"
    override val branchManagerNoBranches = "локальных веток нет"
    override val branchManagerNoRemotes = "remote-tracking веток нет"
    override val branchManagerSelect = "Выберите ветку для управления."
    override val branchCreate = "новая ветка"
    override val branchRename = "переименовать"
    override val branchDelete = "удалить"
    override val branchSetUpstream = "upstream"
    override val branchPush = "push"
    override val branchCompare = "сравнить"
    override val branchCreateHint = "Создаёт ветку от текущего HEAD без переключения на неё."
    override val branchRenameHint =
        "Переименовывает локальную ветку. Git по возможности сохраняет её upstream-конфигурацию."
    override val branchDeleteHint =
        "Безопасное удаление откажется удалять ветку с невлитыми коммитами; force удалит её. Коммиты остаются доступны по hash или reflog."
    override val branchForceDelete = "force (-D)"
    override val branchForceDeleteHint =
        "Удаляет ветку даже без merge. Удаляется только ref; commit-объекты живут до gc."
    override val branchRemoteDeleteHint =
        "Удаляет ветку на remote. В других клонах останутся локальные копии; восстановить можно по hash или reflog, если он есть."
    override val branchRemoteRenameHint =
        "Публикует новую ветку из remote-tracking ref, затем удаляет старую ветку на remote. Если удаление не удалось, новая останется."
    override val branchNewName = "новое имя ветки"
    override val branchUpstreamName = "upstream-ветка"
    override val branchUpstreamHint =
        "Устанавливает upstream через git branch --set-upstream-to; меняется только конфигурация, не коммиты."
    override val branchPushRemote = "remote"
    override val branchPushName = "имя ветки на remote"
    override val branchPushHint =
        "Создаёт или обновляет ветку на remote без force и устанавливает локальный upstream."
    override val branchCompareFirst = "первая ветка / ref"
    override val branchCompareSecond = "вторая ветка / ref"
    override val branchCompareHint =
        "Сравнивает refs через rev-list --left-right и показывает коммиты, уникальные для каждой стороны."
    override val branchCompareLoading = "читаю сравнение…"
    override val branchCompareFirstOnly = "только в первой"
    override val branchCompareSecondOnly = "только во второй"
    override val branchCompareEmpty = "обе refs указывают на одну историю"
    override val branchNoCommits = "Нельзя создать ветку до первого коммита."
    override val branchCannotDeleteCurrent =
        "Нельзя удалить ветку, которая открыта в этом worktree."
    override val branchInvalidRemote = "Remote не настроен."
    override val branchNotFound = "Ветка или remote-ref не найдена."
    override val branchNameUnchanged = "Новое имя совпадает со старым."
    override val toastTagCreated = "тег создан"
    override val toastTagDeleted = "тег удалён"
    override val toastTagPushed = "тег отправлен"
    override val toastTagsPushed = "теги отправлены"
    override val toastRemoteTagDeleted = "тег на remote удалён"
    override val toastBisectGood = "bisect: отмечен good"
    override val toastBisectBad = "bisect: отмечен bad"
    override val toastBisectStarted = "bisect запущен"
    override val toastBisectSkipped = "bisect: пропущен"
    override val toastBisectReset = "bisect сброшен"
    override fun bisectFirstBadResult(hash: String, subject: String) =
        "первый bad-коммит: $hash ${subject.trim()}".trim()
    override val bisectTitle = "BISECT"
    override val bisectStartTitle = "ЗАПУСК BISECT"
    override val bisectBadRef = "bad ref"
    override val bisectGoodRef = "good ref"
    override val bisectBadPlaceholder = "HEAD"
    override val bisectGoodPlaceholder = "последний известный good, например HEAD~10 или v1.0"
    override val bisectStart = "ЗАПУСТИТЬ"
    override val bisectStartHint =
        "Помечает bad и good, затем переключает на первую середину. Оба ref обязательны."
    override val bisectStatusActive = "АКТИВЕН"
    override val bisectStatusComplete = "ЗАВЕРШЁН"
    override val bisectStatusIdle = "НЕ ЗАПУЩЕН"
    override val bisectCurrent = "текущий кандидат"
    override val bisectOriginal = "исходный ref"
    override val bisectMarkGood = "GOOD"
    override val bisectMarkBad = "BAD"
    override val bisectSkip = "ПРОПУСТИТЬ"
    override val bisectReset = "СБРОСИТЬ"
    override val bisectResetHint = "Возвращает исходный ref и удаляет bisect-refs."
    override val bisectCommand = "команда проверки"
    override val bisectCommandPlaceholder = "npm test"
    override val bisectRun = "ЗАПУСТИТЬ КОМАНДУ"
    override val bisectStop = "ОСТАНОВИТЬ"
    override val bisectRunning = "команда выполняется…"
    override val bisectCommandHint =
        "Код 0 = good, 125 = skip, остальные = bad. Команда выполняется в рабочем дереве, таймаут — 10 минут на попытку."
    override val bisectAutoLog = "ЛОГ АВТОПРОГОНА"
    override val bisectAutoEmpty = "вывод автопрогона пока пуст"
    override val bisectVisualization = "КАРТА BISECT"
    override val bisectMarksTitle = "МЕТКИ"
    override val bisectNoMarks = "пока нет отмеченных коммитов"
    override val bisectLogTitle = "BISECT LOG"
    override val bisectNoState = "BISECT не запущен"
    override val bisectRefresh = "обновить"
    override val bisectNoCommits = "история не загружена"
    override val bisectBare = "Bisect недоступен в bare-репозитории."
    override val bisectAutomationRunning = "Сначала остановите текущий автопрогон bisect."
    override val bisectAlreadyActive = "BISECT уже запущен; сбросьте его перед новым поиском."
    override val bisectInvalidRef = "bad и good должны быть непустыми ref без пробелов."
    override val bisectSameRef = "bad и good должны указывать на разные ref."
    override val bisectNotActive = "BISECT не запущен."
    override val bisectComplete = "Bisect уже нашёл первый bad-коммит; сбросьте его для продолжения."
    override val bisectCommandEmpty = "Введите команду для проверки каждого кандидата."
    override val bisectAutomationStopped = "автопрогон bisect остановлен"
    override val bisectTooManySteps = "Автопрогон BISECT превысил защитный лимит шагов."
    override val bisectCommandTimedOut = "Команда проверки не уложилась в таймаут; кандидат не отмечен."
    override val bisectCommandNoExit = "Команда проверки не вернула код завершения."
    override val bisectAutomationPaused = "автопрогон bisect остановлен на текущем кандидате"
    override val bisectAutomationDone = "автопрогон bisect завершён"
    override val logFilters = "фильтры"
    override val logFilterBranch = "ветка"
    override val logFilterAuthor = "автор"
    override val logFilterPath = "путь"
    override val logFilterSince = "с"
    override val logFilterUntil = "по"
    override val logFilterMessage = "сообщение"
    override val logFilterPickaxe = "-S"
    override val logFilterOnlyMine = "только мои"
    override val logFilterOnlyCurrent = "только текущая"
    override val logFilterApply = "ПРИМЕНИТЬ"
    override val logFilterClear = "ОЧИСТИТЬ"
    override val logFilterNoMatch = "нет коммитов, подходящих под фильтры"
    override val logFilterNoIdentity = "Не удалось определить user.email для «только мои»."
    override val logFilterHint = "branch/ref перекрывает область; снимите «только текущая» для --all"
    override val logFilterBranchPlaceholder = "ветка или ref"
    override val logFilterAuthorPlaceholder = "шаблон автора"
    override val logFilterPathPlaceholder = "путь от корня"
    override val logFilterDatePlaceholder = "ГГГГ-ММ-ДД"
    override val logFilterMessagePlaceholder = "grep по сообщению"
    override val logFilterPickaxePlaceholder = "добавленный/удалённый текст"
    override val repoHistory = "история"
    override val repoBlame = "blame"
    override val searchHistoryHint =
        "Коммиты, затрагивающие этот путь от HEAD. Переименования отслеживаются через --follow."
    override val searchBlameHint =
        "Blame версии из HEAD. Нажмите на строку, чтобы открыть её коммит в LOG."
    override val searchGrepHint =
        "Поиск фиксированной строки в отслеживаемых файлах рабочего дерева. Путь — необязательный фильтр."
    override val searchPathPlaceholder = "src/File.kt"
    override val searchPathOptionalPlaceholder = "фильтр по пути (необязательно)"
    override val searchQueryPlaceholder = "фиксированная строка для поиска"
    override val searchHistoryLoading = "читаю историю файла…"
    override val searchBlameLoading = "читаю blame…"
    override val searchGrepLoading = "ищу…"
    override val searchHistoryEmpty = "Ни один коммит не затрагивает этот путь."
    override val searchBlameEmpty = "Нет строк для blame."
    override val searchGrepEmpty = "Совпадений нет."
    override val searchHistoryFailed = "не удалось прочитать историю файла"
    override val searchBlameFailed = "не удалось прочитать blame"
    override val searchGrepFailed = "grep завершился ошибкой"
    override val searchPathRequired = "Введите путь."
    override val searchQueryRequired = "Введите фиксированную строку для поиска."
    override val searchTruncated = "вывод обрезан"
    override val searchCommitNotFound = "Не удалось загрузить этот коммит."
    override val searchPrompt = "Введите путь и запустите HISTORY или BLAME, либо ищите через GREP."
    override val repoCommitMessageRequired = "Введите сообщение коммита."
    override val repoCommitOptions = "опции"
    override val repoCommitAuthor = "автор"
    override val repoCommitCommitter = "committer"
    override val repoCommitTemplate = "шаблон коммита"
    override val repoCommitTemplateLoad = "ЗАГРУЗИТЬ"
    override val repoCommitTemplateHint =
        "Путь от корня репозитория или абсолютный; строки, начинающиеся с #, удаляются."
    override val repoCommitTemplateRequired = "Укажите путь к шаблону коммита."
    override val repoCommitTemplateMissing = "Файл шаблона коммита не найден."
    override val repoCommitTemplateTooLarge = "Шаблон коммита слишком большой (максимум 512 КиБ)."
    override val repoCommitTemplateEmpty =
        "В шаблоне не осталось текста после удаления комментариев."
    override val repoCommitIdentityHint = "Name <email>; пусто — из git config."
    override val repoCommitIdentityInvalid = "Используйте формат Name <email>."
    override val repoCommitNoVerify = "no-verify"
    override val repoCommitMode = "режим"
    override val repoCommitModeNormal = "обычный"
    override val repoCommitModeFixup = "fixup"
    override val repoCommitModeSquash = "squash"
    override val repoCommitTarget = "цель"
    override val repoCommitTargetHint =
        "Hash или ref коммита; для fixup/squash сообщение генерирует git."
    override val repoCommitTargetRequired = "Укажите целевой коммит для fixup/squash."
    override val repoCommitFixupAmendConflict = "Fixup/squash нельзя совместить с amend."
    override val repoCommitRecent = "недавние коммиты"
    override val repoRetryCommit = "ПОВТОРИТЬ"
    override val repoRetryCommitHint =
        "Коммит не выполнился. Исправьте hook или ошибку git и повторите ту же команду."
    override val repoHunksUnavailable =
        "Выбранные hunk'и больше не входят в diff — обновите и выберите снова."
    override val repoCommandPlaceholder = "git status"
    override val repoCommandHint =
        "выполняется в корне репозитория; shell-пайпы и редиректы не поддерживаются"
    override val repoCommandEmpty = "Введите git-команду."
    override val repoCommandUnclosedQuote = "В команде не закрыта кавычка."
    override val repoAliasesHint = "клик — вставить · RUN — выполнить через git"
    override val repoAliasesLoading = "читаю alias'ы…"
    override val repoAliasesEmpty = "git-alias'ы не настроены"
    override val repoAliasesReload = "обновить"
    override val repoAliasesRun = "ЗАПУСТИТЬ"
    override val runnerHistory = "ИСТОРИЯ"
    override val runnerFavorites = "ИЗБРАННОЕ"
    override val runnerAliases = "ALIAS'Ы"
    override val runnerJobs = "ФОН"
    override val runnerHistoryHint = "клик — вставить · ☆ — в избранное"
    override val runnerRunBackground = "ФОН"
    override val runnerNoHistory = "команд пока нет"
    override val runnerNoFavorites = "в избранном пусто — нажмите ☆ у команды"
    override val runnerNoJobs = "фоновых команд нет"
    override val runnerRunning = "выполняется"
    override val runnerSucceeded = "готово"
    override val runnerFailed = "ошибка"
    override val runnerTimedOut = "таймаут"
    override val runnerCancelled = "отменено"
    override val runnerCancelling = "отменяется…"
    override val runnerCancel = "отменить"
    override val runnerClear = "очистить"
    override val runnerClearFinished = "убрать завершённые"
    override val runnerBackgroundHint =
        "продолжает работать после переключения вкладок; избегайте одновременных записей git"
    override val runnerBackgroundStarted = "запущено в фоне"
    override val repoShowIgnored = "показать ignored"
    override val repoHideIgnored = "скрыть ignored"
    override val repoOperationInProgress =
        "операция не завершена — завершите или прервите её перед следующей."
    override val repoOperationContinue = "продолжить"
    override val repoOperationAbort = "прервать"
    override val repoOperationSkip = "пропустить"
    override val repoOperationConfirm = "ПОДТВЕРДИТЬ"
    override val repoOperationCancel = "ОТМЕНА"
    override val repoOperationAbortWarning =
        "Операция будет прервана с восстановлением состояния до её начала. Незакоммиченные изменения могут быть потеряны."
    override val repoOperationSkipWarning =
        "Текущий коммит или патч будет пропущен, операция продолжится. Его изменения не будут применены."
    override val repoOperationSkipUnsupported =
        "у git merge нет --skip; разрешите конфликты и продолжите либо прервите merge."
    override val repoOperationNoActive = "Нет незавершённой git-операции."
    override val repoOperationUnsupported = "Эта операция не поддерживает такое действие."
    override val repoOperationResolveConflicts =
        "Разрешите все конфликты и застейджите файлы перед продолжением."
    override val repoOperationEditTodo = "править todo"
    override val rebaseEditorPrepareTitle = "ИНТЕРАКТИВНЫЙ REBASE"
    override val rebaseEditorEditTitle = "ПРАВКА TODO REBASE"
    override val rebaseEditorBase = "база"
    override val rebaseEditorAutosquash = "autosquash"
    override val rebaseEditorAutosquashHint =
        "Переставляет fixup!/squash!/amend! коммиты и превращает их в fixup/squash до редактирования."
    override val rebaseEditorTodo = "todo"
    override val rebaseEditorAdd = "добавить"
    override val rebaseEditorEmpty = "Todo rebase пуст."
    override val rebaseEditorLoading = "читаю git rebase todo…"
    override val rebaseEditorStart = "ЗАПУСТИТЬ REBASE"
    override val rebaseEditorSave = "СОХРАНИТЬ TODO"
    override val rebaseEditorBackupHint =
        "Перед rebase создаётся backup-ref; после завершения появится Undo."
    override val rebaseEditorEditHint =
        "Меняется оставшаяся часть todo запущенного rebase. Затем продолжите из панели операции."
    override val rebaseEditorCommandHint =
        "Тяните ≡ для перестановки. Нажмите на команду, чтобы выбрать другую."
    override val rebaseEditorRewordMessage = "сообщение reword"
    override val rebaseEditorArgument = "аргумент"
    override val rebaseEditorRemove = "удалить строку"
    override val rebaseEditorInvalidTodo =
        "В todo есть команда, которую нельзя безопасно записать."
    override val rebaseEditorMissingCommit = "У команды коммита нет hash."
    override val rebaseEditorMissingExec = "exec требует команду."
    override val rebaseEditorMissingLabel = "label/reset требует имя label."
    override val rebaseEditorMissingMerge = "merge требует имя label."
    override val rebaseEditorMissingMessage = "reword требует сообщение коммита."
    override val rebaseEditorEmptyPlan = "git вернул пустой rebase todo."
    override val rebaseEditorPlanFailed =
        "Не удалось прочитать план интерактивного rebase из git. Ошибка git есть в console."
    override val toastInteractiveRebaseStopped =
        "rebase остановлен — разрешите конфликт, правьте todo или продолжите"
    override val toastInteractiveRebaseDone = "интерактивный rebase завершён"
    override val repoNoCommitInProgress = "NO-COMMIT ДИАПАЗОН"
    override val repoNoCommitRemaining = "осталось"
    override val repoNoCommitHint =
        "Разрешите конфликты и застейджите файлы, затем продолжите. Изменения останутся в index; коммит не создаётся."
    override val repoNoCommitAbortWarning =
        "Отмена выбросит все незакоммиченные изменения (включая застейдженные до диапазона) и вернёт исходный HEAD."
    override val rerereTitle = "RERERE"
    override val rerereEnabled = "включён"
    override val rerereDisabled = "выключен"
    override val rerereRecorded = "записанных решений"
    override val rerereEnable = "включить"
    override val rerereDisable = "выключить"
    override val rerereClear = "очистить кэш"
    override val rerereEmpty = "записанных решений нет"
    override val rerereEmptyHint =
        "Включите rerere и разрешите конфликт один раз. Git сохранит решение в .git/rr-cache и покажет его здесь."
    override val rerereSelectHint = "выберите запись, чтобы посмотреть preimage и postimage"
    override val rerereClearConfirm = "Очистить все записанные решения rerere?"
    override val rerereClearHint =
        "Удаляется только .git/rr-cache. Коммиты, refs и worktree не затрагиваются. Отменить это нельзя."
    override val rerereConfirm = "ОЧИСТИТЬ"
    override val rerereCancel = "ОТМЕНА"
    override val rererePreimage = "preimage"
    override val rererePostimage = "postimage"
    override val rerereMissingFile = "отсутствует"
    override val rerereTruncated = "обрезано до 256 KiB"
    override val rerereLoadFailed = "не удалось прочитать выбранную запись rerere"
    override val rerereClearFailed = "не удалось очистить .git/rr-cache"
    override val rerereConfigHint = "git config rerere.enabled"
    override val toastRerereEnabled = "rerere включён"
    override val toastRerereDisabled = "rerere выключен"
    override val toastRerereCleared = "кэш rerere очищен"
    override fun stashPartialCount(count: Int) = "выбрано: $count"
    override val stashTitle = "STASH"
    override val stashCreateTitle = "НОВЫЙ STASH"
    override val stashAllChanges = "все изменения"
    override val stashMessagePlaceholder = "необязательное сообщение"
    override val stashIncludeUntracked = "untracked"
    override val stashIncludeIgnored = "ignored"
    override val stashStaged = "только staged"
    override val stashStagedVersion = "stash только staged требует git 2.35+"
    override val stashPartialFiles = "частичный stash · файлы"
    override val stashNoChanges = "нет локальных изменений для stash"
    override val stashPartialHint =
        "выберите файлы для частичного stash или оставьте пусто для всех изменений"
    override val stashCreate = "СОЗДАТЬ STASH"
    override val stashEmpty = "нет stash-записей"
    override val stashUntitled = "(без сообщения)"
    override val stashSelectHint = "выберите stash для просмотра и применения"
    override val stashApply = "ПРИМЕНИТЬ"
    override val stashPop = "POP"
    override val stashPopConfirm = "ПОДТВЕРДИТЬ POP"
    override val stashPopWarning =
        "POP применит stash и удалит его только после успешного применения."
    override val stashDrop = "УДАЛИТЬ"
    override val stashDropConfirm = "ПОДТВЕРДИТЬ УДАЛЕНИЕ"
    override val stashDropWarning = "УДАЛЕНИЕ безвозвратно удалит эту stash-запись."
    override val stashBranchName = "ветка"
    override val stashBranchPlaceholder = "имя новой ветки"
    override val stashBranchAction = "СОЗДАТЬ ВЕТКУ"
    override val stashBranchHint =
        "Создаёт ветку от базы stash, переключается на неё и применяет stash; при успехе git удаляет stash. Рабочее дерево должно быть чистым."
    override val stashFiles = "ФАЙЛЫ"
    override val stashShowLoading = "загрузка stash…"
    override val stashShowEmpty = "нет содержимого patch"
    override val stashShowTruncated = "patch обрезан"
    override val stashPatch = "PATCH"
    override val stashBare = "Stash недоступен в bare-репозитории."
    override val stashPathInvalid = "Выбранный путь отсутствует в текущем status."
    override val stashNotFound = "Stash-запись больше не существует; обновите список."
    override val stashBranchDirty =
        "Закоммитьте или спрячьте локальные изменения перед созданием ветки из stash."
    override val toastStashCreated = "stash создан"
    override val toastStashApplied = "stash применён"
    override val toastStashPopped = "stash применён и удалён"
    override val toastStashDropped = "stash удалён"
    override val toastStashBranchCreated = "ветка создана из stash"
    override val worktreeManagerTitle = "WORKTREES"
    override val worktreeAdd = "ДОБАВИТЬ"
    override val worktreePrune = "PRUNE"
    override val worktreeOpen = "ОТКРЫТЬ"
    override val worktreeOpenWindow = "ОТКРЫТЬ В ОКНЕ"
    override val worktreeBranchAction = "ВЕТКА"
    override val worktreeLock = "ЗАБЛОКИРОВАТЬ"
    override val worktreeUnlock = "РАЗБЛОКИРОВАТЬ"
    override val worktreeRemove = "УДАЛИТЬ"
    override val worktreeNoWorktrees = "worktree не найдены"
    override val worktreeSelect = "выберите worktree для управления"
    override val worktreePath = "путь"
    override val worktreeBranch = "ветка"
    override val worktreeBase = "начальная точка"
    override val worktreeCreateBranch = "создать ветку (-b)"
    override val worktreeForce = "force"
    override val worktreeLockReason = "причина блокировки"
    override val worktreeAddHint =
        "Создаёт связанный worktree. С -b git создаёт новую ветку; иначе поле ветки — существующий ref или commit-ish. Путь должен быть вне текущего рабочего дерева или пустым."
    override val worktreeBranchHint =
        "Переключает выбранный worktree. Новая ветка начинается от HEAD (или указанной начальной точки). Проверка должна быть возможна в этом worktree."
    override val worktreeRemoveHint =
        "Удаляет запись worktree и его каталог. Незакоммиченные изменения требуют --force и после этого теряются."
    override val worktreeLockHint =
        "Заблокированный worktree нельзя prune/remove, пока его не разблокировать. Причина видна в git worktree list."
    override val worktreePruneHint =
        "Удаляет только устаревшие административные записи worktree. Каталоги и коммиты не удаляются; --dry-run показывает, что будет затронуто."
    override val worktreePruneConfirm = "PRUNE"
    override val worktreePrunePreview = "dry-run"
    override val worktreePruneNothing = "нечего prune'ить"
    override val worktreeCancel = "ОТМЕНА"
    override val worktreeCurrent = "текущий"
    override val worktreeBare = "bare"
    override val worktreeDetached = "detached"
    override val worktreeLocked = "locked"
    override val worktreePrunable = "prunable"
    override val worktreePathPlaceholder = "C:\\repo-feature"
    override val worktreeBranchPlaceholder = "feature/name"
    override val worktreeBasePlaceholder = "HEAD"
    override val worktreeReasonPlaceholder = "почему worktree заблокирован"
    override val worktreeInvalidPath =
        "Путь worktree пуст, начинается с '-' или содержит управляющий символ."
    override val worktreeAlreadyExists = "Этот путь уже является worktree этого репозитория."
    override val worktreeInvalidBranch = "Некорректное имя ветки."
    override val worktreeInvalidBase = "Некорректная начальная точка."
    override val worktreeNotFound = "Worktree больше не существует; обновите список."
    override val worktreeCannotRemoveCurrent =
        "Нельзя удалить worktree, который сейчас открыт."
    override val worktreeCannotRemoveBare = "Нельзя удалить bare-worktree репозитория."
    override val worktreeAlreadyLocked = "Worktree уже заблокирован."
    override val worktreeNotLocked = "Worktree не заблокирован."
    override val worktreeBranchBare = "У bare-worktree нет ветки для переключения."
    override val toastWorktreeAdded = "worktree добавлен"
    override val toastWorktreeRemoved = "worktree удалён"
    override val toastWorktreeLocked = "worktree заблокирован"
    override val toastWorktreeUnlocked = "worktree разблокирован"
    override val toastWorktreeBranchChanged = "ветка worktree изменена"
    override val toastWorktreesPruned = "записи worktree очищены"
    override val hookManagerTitle = "HOOKS"
    override val hookSelect = "выберите hook для просмотра и редактирования"
    override val hookNoHooks = "в этом каталоге нет hooks"
    override val hookFilterPlaceholder = "фильтр hooks"
    override val hookEnabled = "включён"
    override val hookDisabled = "отключён"
    override val hookSample = "пример"
    override val hookEnable = "ВКЛЮЧИТЬ"
    override val hookDisable = "ОТКЛЮЧИТЬ"
    override val hookSave = "СОХРАНИТЬ"
    override val hookReload = "ПЕРЕЧИТАТЬ"
    override val hookUnsaved = "есть несохранённые изменения"
    override val hookDiscard = "ОТБРОСИТЬ"
    override val hookCancel = "ОТМЕНА"
    override val hookTruncated =
        "файл больше 512 КиБ; показано только начало"
    override val hookNotFound = "Hook больше не существует; обновите список."
    override val hookTooLarge = "Содержимое hook больше 512 КиБ."
    override val hookSaveFailed = "Не удалось сохранить файл hook."
    override val hookFileExists = "Активный или отключённый файл с таким именем уже существует."
    override val hookPath = "core.hooksPath"
    override val hookPathPlaceholder = ".githooks или пусто для стандартного каталога"
    override val hookPathSet = "ЗАДАТЬ"
    override val hookPathClear = "ПО УМОЛЧАНИЮ"
    override val hookPathEffective = "фактический каталог"
    override val hookPathInvalid =
        "Путь hooks содержит управляющий символ или начинается с '-'."
    override val hookPathExternalWarning =
        "core.hooksPath находится вне .git репозитория. Редактирование или включение таких hooks может запускать произвольные файлы; подтвердите явно."
    override val hookPathConfirmationRequired =
        "Hooks вне стандартного каталога .git/hooks требуют явного подтверждения."
    override val hookExternalConfirm = "ПОДТВЕРДИТЬ"
    override val hookOutput = "ВЫВОД HOOKS"
    override val hookOutputEmpty = "последняя git-операция не дала вывода"
    override val hookOutputClear = "ОЧИСТИТЬ"
    override val hookCopy = "КОПИРОВАТЬ"
    override val hookOutputHint =
        "Последняя git-операция; включает stdout и stderr, записанные hooks."
    override val hookEditorPlaceholder = "#!/bin/sh\n# отредактируйте скрипт hook"
    override val hookFile = "файл"
    override val hookExit = "exit"
    override val hookLoadFailed = "Не удалось прочитать файл hook."
    override val hookSaved = "hook сохранён"
    override val toastHooksPathSet = "core.hooksPath обновлён"
    override val toastHooksPathCleared = "core.hooksPath сброшен"
    override fun patchSelectedCount(count: Int) = "выбрано: $count"
    override val patchesTitle = "PATCHES"
    override val patchExportTitle = "ЭКСПОРТ"
    override val patchSelectMode = "КОММИТЫ"
    override val patchRangeMode = "ДИАПАЗОН"
    override val patchSelectAll = "ВСЕ"
    override val patchClearSelection = "СБРОС"
    override val patchRange = "диапазон ревизий"
    override val patchRangePlaceholder = "HEAD~3..HEAD"
    override val patchOutputDir = "каталог вывода"
    override val patchOutputDirPlaceholder = ".git/chan-patches"
    override val patchOutputFile = "файл mailbox"
    override val patchOutputFilePlaceholder = "patches.mbox"
    override val patchModeFiles = "ФАЙЛЫ"
    override val patchModeMailbox = "MAILBOX"
    override val patchCoverLetter = "cover letter"
    override val patchNumbered = "нумеровать"
    override val patchSignoff = "signoff"
    override val patchBinary = "binary"
    override val patchSubjectPrefix = "префикс темы"
    override val patchSubjectPrefixPlaceholder = "PATCH"
    override val patchPreview = "ПРОСМОТР"
    override val patchExport = "ЭКСПОРТ"
    override val patchPreviewHint =
        "Собирает mailbox в памяти через git format-patch --stdout; файлы не создаются."
    override val patchExportHint =
        "Пишет один mbox-файл или каталог patch-файлов. Cover letter доступен только для диапазона."
    override val patchMailboxTitle = "MAILBOX"
    override val patchMailboxPath = "путь к patch / mailbox"
    override val patchMailboxPathPlaceholder = "patches.mbox или 0001-change.patch"
    override val patchLoad = "ЗАГРУЗИТЬ"
    override val patchMailboxEmpty = "загрузите patch или сделайте preview экспорта"
    override val patchMailboxMessages = "сообщения"
    override val patchMailboxTruncated = "mailbox обрезан"
    override val patchRaw = "СЫРОЙ ВИД"
    override val patchApplyTitle = "ПРИМЕНИТЬ"
    override val patchApplyCheck = "ПРОВЕРИТЬ"
    override val patchApply = "ПРИМЕНИТЬ"
    override val patchApply3way = "3-way"
    override val patchAm = "AM"
    override val patchAmSignoff = "signoff"
    override val patchApplyHint =
        "apply меняет worktree; --check только проверяет patch. am применяет mailbox и создаёт коммиты; --3way разрешает по blobs."
    override val patchNoCommits = "коммиты не загружены"
    override val patchNoSelection = "Выберите коммиты или укажите диапазон ревизий."
    override val patchSelectionAndRange = "Выберите либо коммиты, либо диапазон, но не оба."
    override val patchInvalidRange = "Некорректный диапазон ревизий."
    override val patchUnknownCommit = "Выбранный коммит отсутствует в загруженной истории."
    override val patchInvalidPath = "Путь к patch пуст или содержит управляющий символ."
    override val patchFileNotFound = "Patch или mailbox-файл не найден."
    override val patchOutputDirFailed = "Не удалось создать каталог для patch-файлов."
    override val toastPatchesExported = "патчи экспортированы"
    override val toastPatchCheckPassed = "проверка патча прошла"
    override val toastPatchApplied = "патч применён"
    override val toastMailboxApplied = "mailbox применён"
    override val repoIndexLockWarning =
        "другой git-процесс может ещё работать; удаляйте lock, только если никакая " +
            "git-команда не выполняется."
    override val repoIndexLockDelete = "удалить lock"
    override val repoIndexLockDeleteFailed =
        "Не удалось удалить index.lock. Закройте git и попробуйте снова."
    override val toastStaged = "добавлено в index"
    override val toastUnstaged = "убрано из index"
    override val toastStagedAll = "все изменения в index"
    override val toastUnstagedAll = "index очищен"
    override val toastDiscarded = "изменения отброшены"
    override val toastDiscardedToStash = "изменения перемещены в stash"
    override fun toastDiscardedWithPatch(path: String) = "изменения отброшены · патч: $path"
    override val toastAdded = "добавлено в index"
    override val toastIgnored = "добавлено в .gitignore"
    override val toastCommitted = "коммит создан"
    override val toastOperationContinued = "операция продолжена"
    override val toastOperationAborted = "операция прервана"
    override val toastOperationSkipped = "шаг операции пропущен"
    override val toastTemplateLoaded = "шаблон загружен в сообщение"
    override val toastCommandDone = "команда выполнена"
    override val toastFailed = "операция не удалась"
    override val toastIndexLockRemoved = "index.lock удалён"
    override val toastCopy = "копировать команду"
    override val toastUndo = "отменить"
    override val toastRewriteUndone = "переписывание отменено"
    override val toastRewriteUndoneStashed = "переписывание отменено; изменения сохранены в stash"
    override val toastUndoUnavailable = "Нечего отменять."
    override val toastUndoHeadChanged =
        "История изменилась после этой операции; отмена недоступна."
    override val toastUndoBranchChanged =
        "Вернитесь на исходную ветку, чтобы отменить это переписывание."
    override val toastOperationUndone = "операция отменена"
    override val toastOperationRedone = "операция повторена"
    override val toastOperationRedoneStashed = "операция повторена; изменения сохранены в stash"
    override val toastOperationNotUndoable = "У этой операции нет автоматической отмены."
    override val toastOperationCancelled = "операция отменена"
    override val toastRedoUnavailable = "Нечего повторять."
    override val operationRunning = "выполняется"
    override val operationCancelling = "отменяется…"
    override val operationCompleted = "готово"
    override val operationFailed = "ошибка"
    override val operationCancelled = "отменено"
    override val operationTimedOut = "таймаут"
    override val operationCancel = "отменить"
    override val journalUndo = "отменить"
    override val journalRedo = "повторить"
    override val journalOperations = "операций"
    override val journalEmpty = "Журнал операций пуст."
    override val journalEmptyHint =
        "Выполните git-операцию — она появится здесь с командой, результатом и способом отмены."
    override val journalOnlyLatest =
        "Отмена и повтор применяются к последней обратимой операции; остальные остаются в журнале."
    override val journalNoUndo = "нет автоотмены"
    override val journalUndoable = "можно отменить"
    override val journalUndone = "отменена"
    override val settingsGitPathHint =
        "пусто — автоопределение; иначе путь к git или к его папке"
    override val settingsDefaultBranchHint =
        "используется в Init; пустое значение заменится на main"
    override val settingsCopyShell = "shell для копирования"
    override val settingsCopyShellHint =
        "синтаксис для preview и копируемых команд; auto — по платформе"
    override val settingsCloseToTray =
        "закрывать в трей"
    override val settingsCloseToTrayHint =
        "Кнопка закрытия сворачивает главное окно в системный трей. Для выхода выберите «Выйти» в меню трея."
    override val settingsStartAtLogin =
        "запускать при входе"
    override val settingsStartAtLoginHint =
        "Запускать MyBeautifulGit-chan автоматически после входа в систему. По умолчанию выключено."
    override fun settingsStartAtLoginFailed(detail: String) =
        if (detail.isBlank()) "Не удалось изменить автозапуск." else "Не удалось изменить автозапуск: $detail"
    override val settingsUpdateChannel = "канал релизов"
    override val settingsUpdateChannelChan = "chan · stable"
    override val settingsUpdateChannelKun = "kun · beta"
    override val settingsUpdateChannelHint =
        "chan получает стабильные релизы GitHub; kun — также beta и prerelease-сборки."
    override val settingsUpdateCheck = "ПРОВЕРИТЬ ОБНОВЛЕНИЯ"
    override val settingsUpdateChecking = "ПРОВЕРЯЮ…"
    override val settingsUpdateOpen = "ОТКРЫТЬ РЕЛИЗ"
    override val settingsUpdateNoInstall =
        "Проверяются только релизы GitHub. Для установки обновлений нужны подписанные desktop-пакеты — это позже."
    override val settingsUpdateNoReleases = "В этом канале пока нет доступных релизов."
    override val settingsUpdateRepositoryUnavailable =
        "Репозиторий с релизами ещё не публичный."
    override fun settingsUpdateUpToDate(current: String) =
        "Новее $current релизов нет."
    override fun settingsUpdateAvailable(current: String, latest: String) =
        "Доступно обновление: $latest (сейчас $current)."
    override fun settingsUpdateFailed(detail: String) =
        if (detail.isBlank()) {
            "Не удалось проверить GitHub Releases."
        } else {
            "Не удалось проверить GitHub Releases: $detail"
        }
    override val aiSettingsSection = "AI-СООБЩЕНИЯ КОММИТОВ"
    override val aiEnabled = "AI для сообщений коммитов"
    override val aiEnabledHint =
        "Опционально. Staged diff отправляется на настроенный OpenAI-compatible endpoint только после подтверждения; worktree-контент требует отдельного явного действия."
    override val aiEndpoint = "URL endpoint"
    override val aiEndpointHint =
        "Полный URL chat-completions, например https://api.openai.com/v1/chat/completions"
    override val aiModel = "модель"
    override val aiApiKey = "API-ключ"
    override val aiApiKeyHint =
        "Хранится зашифрованно в локальном credential store; не попадает в settings.properties и логи. Оставьте пустым для локального endpoint без ключа."
    override val aiMessageLanguage = "язык сообщения"
    override val aiLanguageAuto = "авто"
    override val aiLanguageEnglish = "английский"
    override val aiLanguageRussian = "русский"
    override val aiStyle = "стиль"
    override val aiStyleConventional = "conventional"
    override val aiStylePlain = "plain"
    override val aiMaxDiff = "лимит diff, симв."
    override val aiTemperature = "температура"
    override val aiAllowLocalHttp = "локальный HTTP"
    override val aiAllowLocalHttpHint =
        "Разрешить plain HTTP только для localhost (Ollama, LM Studio и похожие). Внешние endpoint обязаны использовать HTTPS."
    override val aiSecurityHint =
        "Отправляется только выбранный diff (по умолчанию staged). .env, ключи, credentials и явные секреты фильтруются; бинарные файлы пропускаются. Diff считается недоверенными данными."
    override val aiEndpointInvalid =
        "AI endpoint должен быть корректным HTTPS URL (или разрешённым localhost HTTP URL)."
    override val aiModelRequired = "Укажите модель AI."
    override val aiMaxDiffInvalid = "Лимит diff должен быть от 1000 до 100000 символов."
    override val aiTemperatureInvalid = "Температура должна быть от 0 до 2."
    override val aiGenerate = "GENERATE"
    override val aiGenerateWorktree = "WORKTREE"
    override val aiRegenerate = "REGENERATE"
    override val aiPreparing = "ПОДГОТОВКА…"
    override val aiGenerating = "ГЕНЕРАЦИЯ…"
    override val aiCancel = "ОТМЕНА"
    override val aiRestore = "ВОССТАНОВИТЬ"
    override val aiSendTitle = "ОТПРАВИТЬ STAGED DIFF?"
    override val aiSendConfirm = "ОТПРАВИТЬ"
    override val aiSendWarning =
        "Staged diff будет отправлен на настроенный внешний сервис. Коммит не создаётся."
    override val aiSendLocalWarning =
        "Staged diff будет отправлен на настроенный локальный endpoint. Коммит не создаётся."
    override val aiSendWorktreeWarning =
        "Unstaged/untracked diff рабочего дерева будет отправлен в настроенный сервис, указанный ниже. Коммит не создаётся."
    override val aiDialogEndpoint = "endpoint"
    override val aiDialogBranch = "ветка"
    override val aiDialogPayload = "payload"
    override val aiDialogOmitted = "скрыто секретных/бинарных секций"
    override val aiDialogTruncated = "diff обрезан"
    override val aiDialogRedacted = "явные секреты вырезаны"
    override fun aiPreviewSummary(files: Int, chars: Int) =
        "$files staged ${if (files == 1) "файл" else "файлов"} · $chars символов"
    override fun aiError(detail: String) =
        if (detail.isBlank()) "Не удалось сгенерировать сообщение коммита." else "Не удалось сгенерировать сообщение коммита: $detail"
    override val settingsOn = "вкл"
    override val settingsOff = "выкл"
    override val trayQuit = "Выйти"
    override val settingsDefaultBranchInvalid =
        "Имя ветки не должно содержать пробелы или начинаться с дефиса."
    override val settingsAuthUsername = "HTTPS username"
    override val settingsAuthUsernameHint =
        "пусто — git; для deploy token может понадобиться отдельный username"
    override val settingsAuthToken = "HTTPS token"
    override val settingsAuthTokenHint =
        "хранится с AES-GCM-шифрованием, ключ лежит в каталоге настроек; защищает от случайных копий/бэкапов, но не от процессов того же пользователя. Используйте personal access token, а не пароль аккаунта."
    override val settingsAuthHelper = "credential.helper"
    override val settingsAuthHelperHint =
        "пусто — helpers из конфигурации git; пока задан token, он заменяет helper"
    override val settingsAuthHelperWarning =
        "credential.helper передаётся Git без изменений и может выполнять произвольную команду. Не храните здесь token или пароль; значение сохраняется в plaintext settings.properties."
    override val settingsAuthSsh = "SSH auth"
    override val settingsAuthSshAgent = "agent"
    override val settingsAuthSshKey = "key"
    override val settingsAuthSshKeyHint =
        "agent использует SSH_AUTH_SOCK / системный OpenSSH; key запускает ssh -i <path> -o IdentitiesOnly=yes"
    override val settingsAuthSshKeyRequired =
        "Укажите файл SSH-ключа или верните SSH auth в режим agent."
    override val settingsAuthTokenInvalid = "HTTPS token не должен содержать переводы строк."
    override val settingsAuthHelperInvalid = "credential helper должен быть одной строкой."
    override val authErrorTwoFactor =
        "Сервер требует второй фактор. Используйте personal access token как HTTPS token или переключитесь на SSH; парольный prompt не может завершить 2FA."
    override val authErrorTokenRequired =
        "Парольная аутентификация отключена. Создайте personal access token и укажите его как HTTPS token."
    override val authErrorHttps =
        "HTTPS-аутентификация не удалась. Проверьте username и token или настройте credential helper."
    override val authErrorSshKey =
        "SSH-ключ не прошёл аутентификацию. Проверьте путь к ключу, passphrase/agent и регистрацию ключа на сервере."
    override val authErrorHostKey =
        "Не прошла проверка host key SSH. Сверьте fingerprint сервера, прежде чем доверять ему."
    override val toastAuthFailed = "аутентификация не удалась — см. ошибку"
    override val gitErrorFailed = "Git не смог выполнить операцию."
    override val gitErrorTimedOut =
        "Команда Git не уложилась в лимит времени. Проверьте соединение или процесс и повторите попытку."
    override val gitErrorNotRepository =
        "Эта папка не является Git-репозиторием."
    override val gitErrorDirtyWorktree =
        "Локальные изменения будут перезаписаны. Сначала закоммитьте их или уберите в stash."
    override val gitErrorConflicts =
        "Операция остановлена из-за конфликтов. Разрешите их, затем продолжите или отмените операцию."
    override val gitErrorPushRejected =
        "Удалённый репозиторий отклонил push: там есть более новые коммиты. Сделайте fetch и проверьте изменения перед повторным push."
    override val gitErrorNoUpstream =
        "У этой ветки нет upstream. Настройте upstream перед pull или push."
    override val gitErrorRemoteNotFound =
        "Удалённый репозиторий или ветка не найдены."
    override val gitErrorRefNotFound =
        "Запрошенная ветка, тег или коммит не найдены."
    override val gitErrorPathNotFound =
        "По указанному пути не найдено ни одного файла."
    override val gitErrorIndexLock =
        "Git index заблокирован другим процессом. Закройте его или удалите index.lock после проверки."
    override val gitErrorPermission =
        "Нет доступа к репозиторию или удалённому серверу."
    override val gitErrorAlreadyExists =
        "Объект с таким именем уже существует."
    override val gitErrorNotFastForward =
        "Ветку нельзя перемотать: истории разошлись."
    override val gitErrorUnrelatedHistories =
        "Истории не связаны, поэтому Git отказался выполнять merge."
    override val configTitle = "GIT CONFIG"
    override val configAll = "ВСЕ"
    override val configScopeSystem = "SYSTEM"
    override val configScopeGlobal = "GLOBAL"
    override val configScopeLocal = "LOCAL"
    override val configScopeWorktree = "WORKTREE"
    override val configAliases = "ALIASES"
    override val configIncludeIf = "INCLUDEIF"
    override val configFilterPlaceholder = "фильтр по ключу или значению"
    override val configKey = "ключ"
    override val configValue = "значение"
    override val configType = "тип"
    override val configOrigin = "источник"
    override val configNew = "НОВЫЙ"
    override val configSave = "СОХРАНИТЬ"
    override val configDelete = "УДАЛИТЬ"
    override val configReload = "ОБНОВИТЬ"
    override val configNoEntries = "в этом представлении нет записей config"
    override val configScopeHint =
        "целевой scope для добавления/изменения; system и global влияют на другие репозитории"
    override val configScopeConfirmation =
        "Запись в SYSTEM/GLOBAL влияет на все репозитории на этой машине. Подтвердите изменение."
    override val configConfirm = "ПОДТВЕРДИТЬ"
    override val configCancel = "ОТМЕНА"
    override val configScopeConfirmationRequired =
        "Для изменения system/global config нужно подтверждение."
    override val configSaved = "запись config сохранена"
    override val configDeleted = "запись config удалена"
    override val configKeyRequired = "Укажите ключ config."
    override val configKeyInvalid =
        "Неверный ключ: нужен section.name, без ведущего дефиса, control-символов и '='."
    override val configAliasNameRequired = "После alias нужно имя."
    override val configAliasNameInvalid = "Имя alias не должно содержать пробелы и control-символы."
    override val configBooleanInvalid = "Ожидается Git boolean (true/false, yes/no, on/off, 1/0)."
    override val configIntegerInvalid = "Ожидается целое число, при необходимости с k, m или g."
    override val configColorInvalid = "Ожидается имя цвета Git, auto/always/never, #rrggbb или ansiN."
    override val configIncludeIfInvalid =
        "includeIf требует условие gitdir:, gitdir/i:, onbranch: или hasconfig:remote.*.url:."
    override val gitMetaTitle = "GIT FILES"
    override val gitMetaSave = "СОХРАНИТЬ"
    override val gitMetaReload = "ОБНОВИТЬ"
    override val gitMetaSaved = "файл сохранён"
    override val gitMetaBare = "У bare-репозитория нет файлов worktree для редактирования."
    override val gitMetaValidationBlocked = "Исправьте синтаксические ошибки перед сохранением."
    override val gitMetaMissing = "файла ещё нет; СОХРАНИТЬ создаст его"
    override val gitMetaNoIssues = "синтаксических ошибок нет"
    override val gitMetaUnsaved = "несохранённые изменения"
    override val gitMetaDiscard = "ОТМЕНИТЬ ИЗМЕНЕНИЯ"
    override val gitMetaCancel = "ОТМЕНА"
    override val gitMetaPlaceholder = "одно правило в строке; комментарии начинаются с #"
    override val gitMetaWorktree = "worktree"
    override val gitMetaLoading = "загрузка…"
    override val gitMetaChecking = "проверка…"
    override val gitMetaLine = "строка"
    override val gitMetaError = "ошибка"
    override val gitMetaWarning = "предупреждение"
    override val gitMetaCheck = "ПРОВЕРКА"
    override val gitMetaIssueIgnoreEmptyNegation = "Правилу-исключению нужен шаблон после !."
    override val gitMetaIssueIgnoreTrailingSpace =
        "Неэкранированный пробел в конце строки игнорируется Git."
    override val gitMetaIssueTrailingEscape =
        "Строка заканчивается неэкранированным обратным слэшем."
    override val gitMetaIssueUnclosedClass =
        "Символьный класс не закрыт; Git воспримет '[' буквально."
    override val gitMetaIssueAttributesMissing = "У шаблона нет атрибутов."
    override val gitMetaIssueAttributesNameInvalid = "Неверное имя атрибута."
    override val gitMetaIssueAttributesDuplicate =
        "Атрибут назначен несколько раз в одной строке."
    override val gitMetaIssueAttributesConflict =
        "Атрибут одновременно установлен и снят в одной строке."
    override val gitMetaIssueMailmapEmailRequired =
        "Строке mailmap нужен хотя бы один <email>."
    override val gitMetaIssueMailmapEmailInvalid =
        "Email должен быть непустым, без пробелов и угловых скобок, обычно с @."
    override val gitMetaIssueMailmapTooManyEmails =
        "В строке mailmap может быть не более двух email."
    override val gitMetaIssueMailmapUnexpectedText =
        "Лишний текст или угловая скобка вне отображения email."
    override val gitMetaIssueMailmapDuplicate =
        "Этот commit email отображён более одного раза."
    override val gitMetaIssueMailmapUnclosed =
        "Email начинается с '<', но не имеет закрывающего '>'."
    override val configIncludeIfPathRequired = "includeIf.path должен указывать на config-файл."
    override val configValueInvalid = "Значение не должно быть пустым или содержать NUL для этого ключа."
    override val configTypeString = "строка"
    override val configTypeBoolean = "boolean"
    override val configTypeInteger = "integer"
    override val configTypeColor = "color"
    override val configTypePath = "path"
    override val configTypeBooleanOrNamed = "boolean|named"
}
