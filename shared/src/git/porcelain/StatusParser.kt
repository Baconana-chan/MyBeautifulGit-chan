package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.BranchState
import org.baconana.mybeautifulgit_chan.domain.RepoStatus
import org.baconana.mybeautifulgit_chan.domain.StatusEntry
import org.baconana.mybeautifulgit_chan.domain.StatusKind
import org.baconana.mybeautifulgit_chan.domain.StatusStage

/**
 * Разбирает вывод:
 * `git status --porcelain=v2 -z --branch --untracked-files=all --ignored=matching`.
 *
 * Формат стабилен и не зависит от локали. Записи разделены NUL, у переименований
 * оригинальный путь идёт следующим NUL-токеном.
 */
object StatusParser {
    fun parse(raw: String): RepoStatus {
        var head: String? = null
        var oid: String? = null
        var upstream: String? = null
        var ahead = 0
        var behind = 0
        var detached = false
        var sawBranchHeader = false

        val entries = mutableListOf<StatusEntry>()
        val tokens = raw.split('\u0000')
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            when {
                token.isEmpty() -> Unit

                token.startsWith("# ") -> {
                    sawBranchHeader = true
                    val header = token.removePrefix("# ")
                    when {
                        header.startsWith("branch.oid ") -> {
                            oid = header.substringAfter(' ').takeUnless { it == "(initial)" }
                        }

                        header.startsWith("branch.head ") -> {
                            val value = header.substringAfter(' ')
                            detached = value == "(detached)"
                            head = value.takeUnless { it == "(detached)" }
                        }

                        header.startsWith("branch.upstream ") -> {
                            upstream = header.substringAfter(' ').ifBlank { null }
                        }

                        header.startsWith("branch.ab ") -> {
                            val counts = header.removePrefix("branch.ab ").split(' ')
                            ahead = counts.getOrNull(0)?.removePrefix("+")?.toIntOrNull() ?: 0
                            behind = counts.getOrNull(1)?.removePrefix("-")?.toIntOrNull() ?: 0
                        }
                    }
                }

                token.startsWith("1 ") -> parseChanged(token)?.let(entries::add)

                token.startsWith("2 ") -> {
                    val originalPath = tokens.getOrNull(index + 1)
                    parseRenamed(token, originalPath)?.let(entries::add)
                    if (originalPath != null) index++
                }

                token.startsWith("u ") -> parseUnmerged(token)?.let(entries::add)

                token.startsWith("? ") -> entries += StatusEntry(
                    path = token.substring(2),
                    kind = StatusKind.Untracked,
                    indexStatus = '.',
                    workTreeStatus = '?',
                )

                token.startsWith("! ") -> entries += StatusEntry(
                    path = token.substring(2),
                    kind = StatusKind.Ignored,
                    indexStatus = '.',
                    workTreeStatus = '!',
                )
            }
            index++
        }

        val branch = if (sawBranchHeader) {
            BranchState(
                head = head,
                oid = oid,
                upstream = upstream,
                ahead = ahead,
                behind = behind,
                detached = detached,
            )
        } else {
            null
        }

        return RepoStatus(branch = branch, entries = entries)
    }

    private fun parseChanged(token: String): StatusEntry? {
        // 1 <XY> <sub> <mH> <mI> <mW> <hH> <hI> <path>
        val parts = token.split(' ', limit = 9)
        if (parts.size < 9) return null
        val xy = parts[1]
        if (xy.length < 2) return null
        return StatusEntry(
            path = parts[8],
            kind = StatusKind.Changed,
            indexStatus = xy[0],
            workTreeStatus = xy[1],
            submodule = parts[2].takeUnless { it == "N..." || it == "N.." },
            modeHead = parts[3],
            modeIndex = parts[4],
            modeWorkTree = parts[5],
            objectHead = parts[6],
            objectIndex = parts[7],
        )
    }

    private fun parseRenamed(token: String, originalPath: String?): StatusEntry? {
        // 2 <XY> <sub> <mH> <mI> <mW> <hH> <hI> <X><score> <path>
        val parts = token.split(' ', limit = 10)
        if (parts.size < 10) return null
        val xy = parts[1]
        if (xy.length < 2) return null
        return StatusEntry(
            path = parts[9],
            originalPath = originalPath,
            kind = StatusKind.Renamed,
            indexStatus = xy[0],
            workTreeStatus = xy[1],
            submodule = parts[2].takeUnless { it == "N..." || it == "N.." },
            modeHead = parts[3],
            modeIndex = parts[4],
            modeWorkTree = parts[5],
            objectHead = parts[6],
            objectIndex = parts[7],
        )
    }

    private fun parseUnmerged(token: String): StatusEntry? {
        // u <XY> <sub> <m1> <m2> <m3> <mW> <h1> <h2> <h3> <path>
        val parts = token.split(' ', limit = 11)
        if (parts.size < 11) return null
        val xy = parts[1]
        if (xy.length < 2) return null
        return StatusEntry(
            path = parts[10],
            kind = StatusKind.Unmerged,
            indexStatus = xy[0],
            workTreeStatus = xy[1],
            submodule = parts[2].takeUnless { it == "N..." || it == "N.." },
            modeWorkTree = parts[6],
            conflictStages = listOf(
                StatusStage(parts[3], parts[7]),
                StatusStage(parts[4], parts[8]),
                StatusStage(parts[5], parts[9]),
            ),
        )
    }
}
