package org.baconana.mybeautifulgit_chan.domain

import org.baconana.mybeautifulgit_chan.git.GitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthTest {

    @Test
    fun `classifies two factor and token-only server errors`() {
        assertEquals(
            AuthFailureKind.TwoFactor,
            detectAuthFailure("remote: 2FA is required for this account"),
        )
        assertEquals(
            AuthFailureKind.TokenRequired,
            detectAuthFailure("remote: Support for password authentication was removed"),
        )
    }

    @Test
    fun `classifies https ssh and host key failures`() {
        assertEquals(
            AuthFailureKind.HttpsCredentials,
            detectAuthFailure("fatal: Authentication failed for 'https://example.com/repo'"),
        )
        assertEquals(
            AuthFailureKind.SshKey,
            detectAuthFailure("git@example.com: Permission denied (publickey)."),
        )
        assertEquals(
            AuthFailureKind.HostKey,
            detectAuthFailure("Host key verification failed."),
        )
    }

    @Test
    fun `successful results and unrelated errors have no auth classification`() {
        assertNull(detectAuthFailure("fatal: not a git repository"))
        assertNull(
            detectAuthFailure(
                GitResult(
                    args = listOf("status"),
                    cwd = null,
                    exitCode = 0,
                    stdout = "",
                    stderr = "Authentication failed",
                    durationMs = 1,
                ),
            ),
        )
    }
}
