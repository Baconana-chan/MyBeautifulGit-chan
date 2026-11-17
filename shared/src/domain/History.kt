package org.baconana.mybeautifulgit_chan.domain

data class Commit(
    val hash: String,
    val parents: List<String>,
    val authorName: String,
    val authorEmail: String,
    val authorTime: Long,
    val committerName: String,
    val committerEmail: String,
    val committerTime: Long,
    val subject: String,
    val refs: List<String> = emptyList(),
) {
    val shortHash: String get() = hash.take(7)
    val isMerge: Boolean get() = parents.size > 1
}

enum class RefKind {
    LocalBranch,
    RemoteBranch,
    Tag,
    Other,
}

data class ReflogEntry(
    val hash: String,
    val selector: String,
    val subject: String,
    val time: Long,
)

data class GitRef(
    val name: String,
    val shortName: String,
    val target: String,
    val objectType: String,
    val kind: RefKind,
    val isHead: Boolean = false,
    val isSymbolic: Boolean = false,
    val symbolicTarget: String? = null,
    val upstream: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val upstreamGone: Boolean = false,
)
