package org.baconana.mybeautifulgit_chan.domain

enum class SignatureTarget { Commit, Tag, Merge }

enum class SignatureCheckKind { Commit, MergeCommit, MergeTag, Tag }

/**
 * Один запуск проверки подписи. [output] — человекочитаемый stdout/stderr
 * git/gpg/ssh, чтобы UI мог показать не только «good/bad», но и причину.
 */
data class SignatureCheck(
    val kind: SignatureCheckKind,
    val command: String,
    val status: CommitSignatureStatus,
    val output: String,
    val ok: Boolean,
)

data class SignatureVerification(
    val target: String,
    val targetKind: SignatureTarget,
    val checks: List<SignatureCheck>,
) {
    /** Для merge достаточно подписи коммита или встроенного mergetag. */
    val verified: Boolean
        get() = checks.any { it.ok } &&
            checks.none {
                it.status == CommitSignatureStatus.Bad ||
                    it.status == CommitSignatureStatus.Revoked ||
                    it.status == CommitSignatureStatus.Expired ||
                    it.status == CommitSignatureStatus.ExpiredKey
            }
}
