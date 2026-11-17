package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.domain.AuthFailureKind
import org.baconana.mybeautifulgit_chan.domain.detectAuthFailure
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.i18n.Strings

fun Strings.authFailureText(kind: AuthFailureKind): String = when (kind) {
    AuthFailureKind.TwoFactor -> authErrorTwoFactor
    AuthFailureKind.TokenRequired -> authErrorTokenRequired
    AuthFailureKind.HttpsCredentials -> authErrorHttps
    AuthFailureKind.SshKey -> authErrorSshKey
    AuthFailureKind.HostKey -> authErrorHostKey
}

fun GitResult.authFailureText(strings: Strings): String? =
    detectAuthFailure(this)?.let(strings::authFailureText)

fun Throwable.authFailureText(strings: Strings): String? =
    (this as? GitCommandException)?.result?.authFailureText(strings)
