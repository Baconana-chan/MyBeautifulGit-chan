package org.baconana.mybeautifulgit_chan.domain

/**
 * Геометрия одной строки графа. Строки строятся по parent-ам из `git log`,
 * поэтому UI не парсит ASCII-граф.
 *
 * Размеры свёрнутых боковых веток сюда не входят: они считаются лениво по
 * запросу из UI, чтобы первый кадр не обходил историю для каждого merge.
 */
data class CommitGraphRow(
    val commit: Commit,
    val lane: Int,
    val laneCount: Int,
    val activeBefore: IntArray,
    val activeAfter: IntArray,
    val parentLanes: IntArray,
)

/**
 * Граф загруженного окна истории.
 *
 * [collapsedCount] вычисляет размер боковой ветки только для того merge,
 * который реально попал в видимую область LazyColumn. Для свёрнутых merge
 * размер нужен при построении (чтобы скрыть строки), для остальных — по
 * требованию. Результат кэшируется до следующего изменения набора коммитов.
 */
class CommitGraph internal constructor(
    val rows: List<CommitGraphRow>,
    private val collapsedCountProvider: (String) -> Int = { 0 },
) {
    private val collapsedCounts = HashMap<String, Int>()

    val laneCount: Int
        get() = rows.maxOfOrNull { it.laneCount } ?: 1

    fun collapsedCount(hash: String): Int = synchronized(collapsedCounts) {
        collapsedCounts.getOrPut(hash) { collapsedCountProvider(hash) }
    }
}

/**
 * Строит lanes в topo-порядке. Родитель, которого нет в загруженной странице,
 * не продлевает lane: граф заканчивается на границе загруженной истории.
 */
fun buildCommitGraph(
    commits: List<Commit>,
    collapsedMerges: Set<String> = emptySet(),
): CommitGraph {
    val byHash = commits.associateBy { it.hash }
    val hidden = mutableSetOf<String>()

    // Скрывать можно только реально выбранные пользователем merge. Считать
    // боковые ветки для всех merge сразу — квадратичная работа на большой
    // истории, хотя абсолютное большинство строк не свёрнуто.
    if (collapsedMerges.isNotEmpty()) {
        for (merge in commits) {
            if (merge.hash !in collapsedMerges || merge.parents.size < 2) continue
            val side = sideBranchHashes(merge, byHash)
            if (side.isNotEmpty()) hidden += side
        }
    }

    val visible = if (hidden.isEmpty()) commits else commits.filterNot { it.hash in hidden }
    val lanes = mutableListOf<String?>()
    val rows = ArrayList<CommitGraphRow>(visible.size)

    for (commit in visible) {
        var nodeLane = lanes.indexOf(commit.hash)
        if (nodeLane < 0) {
            nodeLane = firstFreeLane(lanes)
            if (nodeLane == lanes.size) {
                lanes.add(commit.hash)
            } else {
                lanes[nodeLane] = commit.hash
            }
        }

        val activeBefore = activeLanes(lanes)
        lanes[nodeLane] = null

        val knownParents = commit.parents
            .distinct()
            .filter { it in byHash }

        val parentLanes = mutableListOf<Int>()
        for ((index, parent) in knownParents.withIndex()) {
            var parentLane = lanes.indexOf(parent)
            if (parentLane < 0) {
                parentLane = if (index == 0 && lanes.getOrNull(nodeLane) == null) {
                    nodeLane
                } else {
                    firstFreeLane(lanes)
                }
                if (parentLane == lanes.size) {
                    lanes.add(parent)
                } else {
                    lanes[parentLane] = parent
                }
            }
            if (parentLane !in parentLanes) parentLanes += parentLane
        }

        while (lanes.isNotEmpty() && lanes.last() == null) {
            lanes.removeAt(lanes.lastIndex)
        }
        val activeAfter = activeLanes(lanes)
        val laneCount = maxOf(
            1,
            (activeBefore.maxOrNull() ?: -1) + 1,
            (activeAfter.maxOrNull() ?: -1) + 1,
            nodeLane + 1,
            (parentLanes.maxOrNull() ?: -1) + 1,
        )

        rows += CommitGraphRow(
            commit = commit,
            lane = nodeLane,
            laneCount = laneCount,
            activeBefore = activeBefore,
            activeAfter = activeAfter,
            parentLanes = parentLanes.toIntArray(),
        )
    }

    val countCache = HashMap<String, Int>()
    return CommitGraph(rows) { hash ->
        synchronized(countCache) {
            countCache.getOrPut(hash) {
                val merge = byHash[hash]
                if (merge == null || merge.parents.size < 2) {
                    0
                } else {
                    sideBranchHashes(merge, byHash).size
                }
            }
        }
    }
}

private fun activeLanes(lanes: List<String?>): IntArray {
    var count = 0
    for (lane in lanes) if (lane != null) count += 1
    val result = IntArray(count)
    var index = 0
    for (lane in lanes.indices) {
        if (lanes[lane] != null) result[index++] = lane
    }
    return result
}

private fun sideBranchHashes(merge: Commit, byHash: Map<String, Commit>): Set<String> {
    if (merge.parents.size < 2) return emptySet()
    val firstParent = byHash[merge.parents.first()] ?: return emptySet()
    val mainline = ancestorHashes(firstParent.hash, byHash)
    return merge.parents.drop(1)
        .flatMap { parent -> ancestorHashes(parent, byHash) }
        .filter { it != merge.hash && it !in mainline }
        .toSet()
}

private fun firstFreeLane(lanes: MutableList<String?>): Int {
    val index = lanes.indexOf(null)
    return if (index >= 0) index else lanes.size
}

private fun ancestorHashes(start: String, byHash: Map<String, Commit>): Set<String> {
    val result = mutableSetOf<String>()
    val stack = ArrayDeque<String>()
    stack.addLast(start)
    while (stack.isNotEmpty()) {
        val hash = stack.removeLast()
        if (!result.add(hash)) continue
        byHash[hash]?.parents.orEmpty().forEach { parent ->
            if (parent in byHash) stack.addLast(parent)
        }
    }
    return result
}
