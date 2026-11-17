package org.baconana.mybeautifulgit_chan.domain

import org.baconana.mybeautifulgit_chan.git.GitResult

/** How SSH transport is authenticated. Agent uses the user's existing ssh-agent/config. */
enum class SshAuthMode(val label: String) {
    Agent("agent"),
    Key("key");

    companion object {
        fun parse(value: String?): SshAuthMode =
            if (value?.trim()?.equals(Key.label, ignoreCase = true) == true) Key else Agent
    }
}

/**
 * Explicit authentication settings for network git operations.
 *
 * The token is kept out of [toString] on purpose: settings and logs must never
 * accidentally expose it. The desktop credential store encrypts it at rest.
 */
data class GitAuth(
    val username: String = "",
    val token: String = "",
    val credentialHelper: String = "",
    val sshMode: SshAuthMode = SshAuthMode.Agent,
    val sshKeyPath: String = "",
) {
    val hasToken: Boolean get() = token.isNotBlank()

    override fun toString(): String =
        "GitAuth(username=$username, token=${if (hasToken) "<set>" else "<empty>"}, " +
            "credentialHelper=${if (credentialHelper.isNotBlank()) "<set>" else "<empty>"}, " +
            "sshMode=$sshMode, " +
            "sshKeyPath=$sshKeyPath)"
}

/** Categories with actionable guidance, not just the raw git stderr. */
enum class AuthFailureKind {
    TwoFactor,
    TokenRequired,
    HttpsCredentials,
    SshKey,
    HostKey,
}

/**
 * Detects the authentication failures a terminal git client commonly sees.
 * The raw error still stays in the console; this only adds a useful next step.
 */
fun detectAuthFailure(result: GitResult): AuthFailureKind? {
    if (result.ok) return null
    return detectAuthFailure(result.stderr.ifBlank { result.stdout })
}

fun detectAuthFailure(output: String): AuthFailureKind? {
    val text = output.lowercase()
    if (text.isBlank()) return null

    val hasTwoFactor = listOf(
        "two-factor",
        "two factor",
        "2fa",
        "verification code",
        "one-time password",
        "one time password",
    ).any(text::contains) || Regex("""\botp\b""").containsMatchIn(text)
    if (hasTwoFactor) return AuthFailureKind.TwoFactor

    if (listOf(
            "host key verification failed",
            "remote host identification has changed",
            "offending ed25519 key",
            "offending rsa key",
        ).any(text::contains)
    ) {
        return AuthFailureKind.HostKey
    }

    if (listOf(
            "permission denied (publickey",
            "no such identity",
            "load key ",
            "error in libcrypto",
            "identity file",
        ).any(text::contains)
    ) {
        return AuthFailureKind.SshKey
    }

    if (listOf(
            "password authentication was removed",
            "support for password authentication",
            "password authentication is not supported",
            "personal access token",
        ).any(text::contains)
    ) {
        return AuthFailureKind.TokenRequired
    }

    if (listOf(
            "authentication failed",
            "could not read username",
            "could not read password",
            "terminal prompts disabled",
            "401 unauthorized",
            "authorization failed",
            "invalid username or password",
            "invalid credentials",
            "bad credentials",
            "http basic: access denied",
        ).any(text::contains)
    ) {
        return AuthFailureKind.HttpsCredentials
    }

    return null
}
