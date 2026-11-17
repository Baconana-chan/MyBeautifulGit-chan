package org.baconana.mybeautifulgit_chan.domain

enum class SubmoduleState {
    Current,
    Modified,
    Uninitialized,
    Conflict,
}

/**
 * Одна запись `git submodule status --recursive`.
 *
 * [head] — зафиксированный в суперпроекте OID, [describe] — человекочитаемое
 * описание, которое git добавляет в скобках, если оно есть.
 */
data class Submodule(
    val path: String,
    val head: String?,
    val state: SubmoduleState,
    val describe: String? = null,
)
