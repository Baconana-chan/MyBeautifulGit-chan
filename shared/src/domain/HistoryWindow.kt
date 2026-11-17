package org.baconana.mybeautifulgit_chan.domain

/**
 * Результат обновления уже загруженного окна истории первой страницей git log.
 *
 * [reusedCached] отличает дешёвое обновление от полной замены: UI может не
 * пересобирать граф, если список коммитов остался прежним.
 */
data class HistoryWindow(
    val commits: List<Commit>,
    val hasMore: Boolean,
    val reusedCached: Boolean = false,
)

/**
 * Склеивает свежую первую страницу с ранее загруженным хвостом.
 *
 * Хвост используется только пока он гарантированно является продолжением
 * свежей истории:
 * - HEAD не изменился, и свежая страница совпадает с её началом;
 * - либо это история одной текущей ветки ([allowHeadAdvance]), где старый HEAD
 *   виден в новом log, а значит все загруженные предки остаются достижимыми.
 *
 * При смене ветки, reset или смене фильтра старый хвост отбрасывается. Лучше
 * перечитать страницу, чем показать коммиты, которых уже нет в репозитории.
 */
fun mergeHistoryWindow(
    cached: List<Commit>,
    cachedHasMore: Boolean,
    fresh: List<Commit>,
    pageSize: Int,
    allowHeadAdvance: Boolean = false,
): HistoryWindow {
    require(pageSize > 0) { "pageSize must be positive" }
    if (fresh.isEmpty()) return HistoryWindow(emptyList(), hasMore = false)
    if (cached.isEmpty()) {
        return HistoryWindow(fresh, hasMore = fresh.size >= pageSize)
    }

    val sameHead = cached.first().hash == fresh.first().hash
    val samePrefix = sameHead && prefixesMatch(cached, fresh)
    if (samePrefix) {
        if (cached.size <= fresh.size) {
            return HistoryWindow(fresh, hasMore = fresh.size >= pageSize)
        }
        return HistoryWindow(
            commits = fresh + cached.drop(fresh.size),
            hasMore = cachedHasMore,
            reusedCached = true,
        )
    }

    val oldHeadIndex = fresh.indexOfFirst { it.hash == cached.first().hash }
    if (allowHeadAdvance && oldHeadIndex >= 0) {
        val freshHashes = fresh.mapTo(HashSet(fresh.size * 2)) { it.hash }
        return HistoryWindow(
            commits = fresh + cached.filterNot { it.hash in freshHashes },
            hasMore = cachedHasMore,
            reusedCached = true,
        )
    }

    return HistoryWindow(fresh, hasMore = fresh.size >= pageSize)
}

private fun prefixesMatch(cached: List<Commit>, fresh: List<Commit>): Boolean {
    val common = minOf(cached.size, fresh.size)
    for (index in 0 until common) {
        if (cached[index].hash != fresh[index].hash) return false
    }
    return true
}
