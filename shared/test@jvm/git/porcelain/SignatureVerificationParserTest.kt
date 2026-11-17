package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SignatureVerificationParserTest {

    @Test
    fun `maps gpg trust and failure output`() {
        assertEquals(
            CommitSignatureStatus.Good,
            SignatureVerificationParser.parse(
                "gpg: Signature made today\ngpg: Good signature from \"Ada <ada@example.com>\"",
                exitOk = true,
            ),
        )
        assertEquals(
            CommitSignatureStatus.Untrusted,
            SignatureVerificationParser.parse(
                "gpg: Good signature from \"Ada\"\n" +
                    "gpg: WARNING: This key is not certified with a trusted signature!",
                exitOk = true,
            ),
        )
        assertEquals(
            CommitSignatureStatus.Bad,
            SignatureVerificationParser.parse("gpg: BAD signature from \"Ada\"", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.Revoked,
            SignatureVerificationParser.parse("gpg: WARNING: This key has been revoked", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.ExpiredKey,
            SignatureVerificationParser.parse("gpg: Note: This key has expired!", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.Expired,
            SignatureVerificationParser.parse("gpg: Signature expired", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.CannotCheck,
            SignatureVerificationParser.parse("gpg: Can't check signature: No public key", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.NoSignature,
            SignatureVerificationParser.parse("error: no signature found", exitOk = false),
        )
    }

    @Test
    fun `recognises ssh signatures and empty unsigned output`() {
        assertEquals(
            CommitSignatureStatus.Good,
            SignatureVerificationParser.parse(
                "Good \"git\" signature for Ada with ED25519 key SHA256:abc",
                exitOk = true,
            ),
        )
        assertEquals(
            CommitSignatureStatus.NoSignature,
            SignatureVerificationParser.parse("", exitOk = false),
        )
        assertEquals(
            CommitSignatureStatus.Good,
            SignatureVerificationParser.parse("", exitOk = true),
        )
    }

    @Test
    fun `embedded show output without a signature is not verified`() {
        assertEquals(
            CommitSignatureStatus.Good,
            SignatureVerificationParser.parseEmbedded("gpg: Good signature from \"Ada\""),
        )
        assertEquals(
            CommitSignatureStatus.Bad,
            SignatureVerificationParser.parseEmbedded("gpg: BAD signature from \"Ada\""),
        )
        assertEquals(
            CommitSignatureStatus.NoSignature,
            SignatureVerificationParser.parseEmbedded("commit abc\nAuthor: Ada"),
        )
    }
}
