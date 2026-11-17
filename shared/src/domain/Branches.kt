package org.baconana.mybeautifulgit_chan.domain

enum class BranchCompareSide {
    First,
    Second,
}

data class BranchCompareCommit(
    val hash: String,
    val subject: String,
    val side: BranchCompareSide,
) {
    val shortHash: String get() = hash.take(7)
}

/**
 * Сравнение двух branch/ref: ahead — только в first, behind — только во second.
 */
data class BranchComparison(
    val first: String,
    val second: String,
    val ahead: Int,
    val behind: Int,
    val commits: List<BranchCompareCommit> = emptyList(),
)