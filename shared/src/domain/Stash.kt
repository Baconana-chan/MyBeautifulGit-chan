package org.baconana.mybeautifulgit_chan.domain

/** One entry from `git stash list`. The selector is stable only for the current list load. */
data class StashEntry(
    val selector: String,
    val hash: String,
    val message: String,
    val time: Long,
    val authorName: String = "",
    val authorEmail: String = "",
)

data class StashFileChange(
    val status: String,
    val path: String,
    val oldPath: String? = null,
)

/** Full `git stash show` payload for the details pane. */
data class StashDetail(
    val selector: String,
    val files: List<StashFileChange>,
    val patch: String,
    val truncated: Boolean = false,
)

/** Options for a full or partial (`-- path...` / `--staged`) stash push. */
data class StashCreateOptions(
    val message: String = "",
    val includeUntracked: Boolean = false,
    val includeIgnored: Boolean = false,
    val stagedOnly: Boolean = false,
    val paths: List<String> = emptyList(),
)
