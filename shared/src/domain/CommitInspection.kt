package org.baconana.mybeautifulgit_chan.domain

enum class CommitSignatureStatus {
    Good,
    Untrusted,
    Bad,
    Expired,
    ExpiredKey,
    Revoked,
    CannotCheck,
    NoSignature,
    Unknown,
}

data class CommitSignature(
    val status: CommitSignatureStatus = CommitSignatureStatus.Unknown,
    val signer: String? = null,
    val key: String? = null,
)

/** Одна строка `git show --numstat`; null additions/deletions означает binary. */
data class CommitFileStat(
    val path: String,
    val additions: Int? = null,
    val deletions: Int? = null,
    val binary: Boolean = false,
)

data class CommitStats(
    val files: List<CommitFileStat> = emptyList(),
) {
    val additions: Int get() = files.sumOf { it.additions ?: 0 }
    val deletions: Int get() = files.sumOf { it.deletions ?: 0 }
}

/**
 * Полный инспектор выбранного коммита: diff относительно первого parent,
 * пофайловая статистика и результат проверки подписи.
 */
data class CommitInspection(
    val hash: String,
    val diff: String,
    val stats: CommitStats,
    val signature: CommitSignature = CommitSignature(),
    val diffTruncated: Boolean = false,
)
