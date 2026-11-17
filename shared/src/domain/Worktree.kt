package org.baconana.mybeautifulgit_chan.domain

/**
 * Одна запись `git worktree list --porcelain`.
 *
 * [branch] хранится в полном виде (`refs/heads/main`), чтобы не терять refs
 * с нестандартными именами; для UI есть [branchName].
 */
data class Worktree(
    val path: String,
    val head: String? = null,
    val branch: String? = null,
    val isBare: Boolean = false,
    val isDetached: Boolean = false,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val isPrunable: Boolean = false,
    val prunableReason: String? = null,
) {
    val branchName: String?
        get() = branch?.removePrefix("refs/heads/")
}
