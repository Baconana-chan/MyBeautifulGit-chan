package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.CommitSignature
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus

/** Разбирает `git show -s --format=%G?%x00%GS%x00%GK`. */
object CommitSignatureParser {
    fun parse(raw: String): CommitSignature {
        val fields = raw.split('\u0000')
        val code = fields.getOrNull(0)?.trim()?.firstOrNull()
        val signer = fields.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        val key = fields.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
        return CommitSignature(
            status = when (code) {
                'G' -> CommitSignatureStatus.Good
                'U' -> CommitSignatureStatus.Untrusted
                'B' -> CommitSignatureStatus.Bad
                'X' -> CommitSignatureStatus.Expired
                'Y' -> CommitSignatureStatus.ExpiredKey
                'R' -> CommitSignatureStatus.Revoked
                'E' -> CommitSignatureStatus.CannotCheck
                'N' -> CommitSignatureStatus.NoSignature
                else -> CommitSignatureStatus.Unknown
            },
            signer = signer,
            key = key,
        )
    }
}
