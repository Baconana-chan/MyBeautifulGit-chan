package org.baconana.mybeautifulgit_chan.domain

/**
 * Что изменится и что может быть потеряно при reset на выбранный коммит.
 *
 * [lostCommits] — коммиты, которые перестанут быть достижимы из текущей ветки
 * после её перемещения, но не закреплены другими refs. [unreachable] содержит
 * подробный план по rev-list/fsck; reflog и backup-refs остаются путём восстановления.
 * Локальные пути перечислены отдельно, чтобы UI мог показать последствия режима.
 */
data class ResetPreview(
    val lostCommits: Int = 0,
    val lostCommitSubjects: List<String> = emptyList(),
    val stagedPaths: List<String> = emptyList(),
    val unstagedPaths: List<String> = emptyList(),
    val conflictedPaths: List<String> = emptyList(),
    val untrackedPaths: List<String> = emptyList(),
    val keepConflicts: List<String> = emptyList(),
    /** Детальный план по rev-list/fsck; [lostCommits] оставлен для совместимости UI. */
    val unreachable: UnreachablePlan = UnreachablePlan(),
)