package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus

/**
 * Разбирает человекочитаемый вывод `git verify-commit`, `git verify-tag` и
 * `git show --show-signature`. Код возврата остаётся основным признаком успеха:
 * вывод нужен, чтобы отличить плохую подпись от отсутствующей или неизвестного
 * ключа, а также показать причину пользователю.
 */
object SignatureVerificationParser {

    fun parse(raw: String, exitOk: Boolean): CommitSignatureStatus {
        classify(raw)?.let { return it }
        if (!exitOk && raw.isBlank()) return CommitSignatureStatus.NoSignature
        return if (exitOk) CommitSignatureStatus.Good else CommitSignatureStatus.CannotCheck
    }

    /** `git show --show-signature` возвращает 0 и для unsigned-коммита. */
    fun parseEmbedded(raw: String): CommitSignatureStatus =
        classify(raw) ?: CommitSignatureStatus.NoSignature

    private fun classify(raw: String): CommitSignatureStatus? {
        val text = raw.lowercase()
        return when {
            "bad signature" in text ||
                "badsig" in text -> CommitSignatureStatus.Bad

            "revoked" in text ||
                "revkeysig" in text -> CommitSignatureStatus.Revoked

            "key expired" in text ||
                "key has expired" in text ||
                "expired key" in text ||
                "expkeysig" in text -> CommitSignatureStatus.ExpiredKey

            "signature expired" in text ||
                "expired signature" in text ||
                "expsig" in text -> CommitSignatureStatus.Expired

            "good signature" in text ||
                Regex("""\bgood\b[^\n]{0,80}\bsignature\b""").containsMatchIn(text) ||
                "validsig" in text -> if (
                "untrusted" in text ||
                "this key is not certified" in text ||
                "no indication that the signature belongs" in text
            ) {
                CommitSignatureStatus.Untrusted
            } else {
                CommitSignatureStatus.Good
            }

            "can't check signature" in text ||
                "cannot check signature" in text ||
                "no public key" in text ||
                "public key not found" in text ||
                "errsig" in text -> CommitSignatureStatus.CannotCheck

            "no signature" in text ||
                "no signature found" in text ||
                "does not have a gpg signature" in text ||
                "cannot verify a non-tag object" in text -> CommitSignatureStatus.NoSignature

            else -> null
        }
    }
}
