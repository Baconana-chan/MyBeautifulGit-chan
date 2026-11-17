package org.baconana.mybeautifulgit_chan.domain

/** Коммит, который выпадет из достижимой истории после планируемого изменения ref. */
data class UnreachableCommit(
    val hash: String,
    val subject: String,
    val author: String,
    val time: Long,
) {
    val shortHash: String get() = hash.take(7)
}

/**
 * Предварительный расчёт последствий перемещения HEAD/ветки.
 *
 * [commitCount] и [objectCount] считаются через `rev-list` с исключением target
 * и остальных refs, поэтому в план не попадают коммиты, которые останутся
 * достижимыми из другой ветки или тега. [fsckUnreachableObjects] — уже
 * существующие недостижимые объекты по `fsck --no-reflogs`; это не прогноз,
 * а отдельная проверка состояния object database.
 */
data class UnreachablePlan(
    val commitCount: Int = 0,
    val commits: List<UnreachableCommit> = emptyList(),
    val objectCount: Int = 0,
    val fsckUnreachableObjects: Int = 0,
    val fsckUnreachableCommits: Int = 0,
    val fsckError: String? = null,
)
