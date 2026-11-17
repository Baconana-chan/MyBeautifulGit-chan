package org.baconana.mybeautifulgit_chan.domain

/**
 * Фильтры истории. Пустые поля не передаются git; [onlyCurrent] по умолчанию
 * сохраняет прежнее поведение `git log HEAD`.
 */
data class LogFilter(
    val branch: String = "",
    val author: String = "",
    val path: String = "",
    val since: String = "",
    val until: String = "",
    val message: String = "",
    val pickaxe: String = "",
    val onlyMine: Boolean = false,
    val onlyCurrent: Boolean = true,
) {
    val activeCount: Int
        get() = listOf(branch, author, path, since, until, message, pickaxe)
            .count { it.isNotBlank() } +
            (if (onlyMine) 1 else 0) +
            (if (!onlyCurrent && branch.isBlank()) 1 else 0)

    val isActive: Boolean
        get() = activeCount > 0

    fun normalized(): LogFilter = copy(
        branch = branch.trim(),
        author = author.trim(),
        path = path.trim(),
        since = since.trim(),
        until = until.trim(),
        message = message.trim(),
        pickaxe = pickaxe.trim(),
    )
}
