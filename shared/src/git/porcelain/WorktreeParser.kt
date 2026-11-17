package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.Worktree

/**
 * Разбирает `git worktree list --porcelain`. Формат построчный; записи
 * разделены пустой строкой, неизвестные строки игнорируются.
 */
object WorktreeParser {

    fun parse(raw: String): List<Worktree> {
        val result = ArrayList<Worktree>()
        var path: String? = null
        var head: String? = null
        var branch: String? = null
        var bare = false
        var detached = false
        var locked = false
        var lockReason: String? = null
        var prunable = false
        var prunableReason: String? = null

        fun flush() {
            val current = path
            if (current != null) {
                result += Worktree(
                    path = current,
                    head = head,
                    branch = branch,
                    isBare = bare,
                    isDetached = detached,
                    isLocked = locked,
                    lockReason = lockReason,
                    isPrunable = prunable,
                    prunableReason = prunableReason,
                )
            }
            path = null
            head = null
            branch = null
            bare = false
            detached = false
            locked = false
            lockReason = null
            prunable = false
            prunableReason = null
        }

        raw.lineSequence().forEach { line ->
            when {
                line.isBlank() -> flush()
                line.startsWith("worktree ") -> {
                    if (path != null) flush()
                    path = line.removePrefix("worktree ")
                }

                line.startsWith("HEAD ") -> head = line.removePrefix("HEAD ")
                line == "bare" -> bare = true
                line == "detached" -> detached = true
                line.startsWith("branch ") -> branch = line.removePrefix("branch ")
                line == "locked" -> locked = true
                line.startsWith("locked ") -> {
                    locked = true
                    lockReason = line.removePrefix("locked ")
                }

                line == "prunable" -> prunable = true
                line.startsWith("prunable ") -> {
                    prunable = true
                    prunableReason = line.removePrefix("prunable ")
                }
            }
        }
        flush()
        return result
    }
}
