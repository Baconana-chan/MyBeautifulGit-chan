package org.baconana.mybeautifulgit_chan.domain

data class BranchState(
    val head: String?,
    val oid: String?,
    val upstream: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val detached: Boolean = false,
) {
    val unborn: Boolean get() = oid == null
}

enum class StatusKind {
    Changed,
    Renamed,
    Unmerged,
    Untracked,
    Ignored,
}

/** Одна из трёх версий конфликтного файла (base / ours / theirs). */
data class StatusStage(
    val mode: String,
    val objectId: String,
)

data class StatusEntry(
    val path: String,
    val originalPath: String? = null,
    val kind: StatusKind,
    val indexStatus: Char,
    val workTreeStatus: Char,
    val submodule: String? = null,
    val modeHead: String? = null,
    val modeIndex: String? = null,
    val modeWorkTree: String? = null,
    val objectHead: String? = null,
    val objectIndex: String? = null,
    val conflictStages: List<StatusStage> = emptyList(),
) {
    val isConflicted: Boolean get() = kind == StatusKind.Unmerged

    val isSubmodule: Boolean get() = submodule != null

    /** `S<c><m><u>`: изменён коммит, tracked-содержимое или untracked-файлы. */
    val submoduleDirty: Boolean
        get() = submodule?.let { code ->
            code.length >= 4 && (code[1] == 'C' || code[2] == 'M' || code[3] == 'U')
        } == true

    val isStaged: Boolean
        get() = kind != StatusKind.Untracked &&
            kind != StatusKind.Ignored &&
            !isConflicted &&
            indexStatus != '.'

    val isUnstaged: Boolean
        get() = when (kind) {
            StatusKind.Untracked -> true
            StatusKind.Ignored -> false
            StatusKind.Unmerged -> false
            else -> workTreeStatus != '.'
        }
}

data class RepoStatus(
    val branch: BranchState?,
    val entries: List<StatusEntry>,
) {
    val stagedCount: Int get() = entries.count { it.isStaged }
    val unstagedCount: Int get() = entries.count { it.isUnstaged }
    val untrackedCount: Int get() = entries.count { it.kind == StatusKind.Untracked }
    val ignoredCount: Int get() = entries.count { it.kind == StatusKind.Ignored }
    val conflictedCount: Int get() = entries.count { it.isConflicted }

    val isClean: Boolean get() = entries.none { it.kind != StatusKind.Ignored }
}
