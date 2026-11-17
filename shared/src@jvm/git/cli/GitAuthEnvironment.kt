package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.domain.GitAuth
import org.baconana.mybeautifulgit_chan.domain.SshAuthMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions

/**
 * Auth material for one Git invocation.
 *
 * The token is written to a temporary owner-only file and read by a credential
 * helper; it is never placed in the environment inherited by hooks. The file
 * is deleted after the Git process (including its hooks) finishes.
 *
 * This closes the accidental env leak. It does not sandbox a hook that
 * deliberately reads files as the same OS user: that requires disabling or
 * isolating hooks, which is a separate product decision.
 */
internal data class PreparedAuth(
    val configArgs: List<String>,
    val environment: Map<String, String>,
    val credentialFile: File?,
    val cleanup: () -> Unit,
)

internal fun GitAuth.prepare(): PreparedAuth {
    val credentialFile = if (hasToken) writeCredentialFile() else null
    return PreparedAuth(
        configArgs = configArgs(credentialFile),
        environment = environment(),
        credentialFile = credentialFile,
        cleanup = {
            if (credentialFile != null) {
                runCatching { Files.deleteIfExists(credentialFile.toPath()) }
            }
        },
    )
}

private fun GitAuth.configArgs(credentialFile: File?): List<String> = buildList {
    val helper = when {
        credentialFile != null ->
            "!cat ${shellQuote(credentialFile.absolutePath.replace('\\', '/'))}"
        credentialHelper.isNotBlank() -> credentialHelper.trim()
        else -> return@buildList
    }
    // An empty helper clears inherited system helpers for this invocation.
    add("-c")
    add("credential.helper=")
    add("-c")
    add("credential.helper=$helper")
}

/** SSH identity path is not a secret; the token is deliberately absent. */
internal fun GitAuth.environment(): Map<String, String> = buildMap {
    if (sshMode == SshAuthMode.Key && sshKeyPath.isNotBlank()) {
        put("GIT_SSH_COMMAND", "ssh -i ${shellQuote(sshKeyPath.trim())} -o IdentitiesOnly=yes")
    }
}

private fun GitAuth.writeCredentialFile(): File {
    val user = username.trim().ifBlank { "git" }
    require(user.none { it == '\n' || it == '\r' || it == '\u0000' }) {
        "Git username must not contain line breaks"
    }
    require(token.none { it == '\n' || it == '\r' || it == '\u0000' }) {
        "Git token must not contain line breaks"
    }

    val file = Files.createTempFile("mbg-git-auth-", ".cred").toFile()
    try {
        restrictPermissions(file.toPath())
        file.writeText("username=$user\npassword=$token\n", Charsets.UTF_8)
        return file
    } catch (error: Exception) {
        runCatching { Files.deleteIfExists(file.toPath()) }
        throw error
    }
}

private fun restrictPermissions(path: Path) {
    runCatching {
        Files.getFileAttributeView(path, PosixFileAttributeView::class.java)
            ?.setPermissions(PosixFilePermissions.fromString("rw-------"))
    }
}

/** POSIX quoting for GIT_SSH_COMMAND and the credential-helper file path. */
private fun shellQuote(value: String): String =
    "'" + value.replace("'", "'\"'\"'") + "'"
