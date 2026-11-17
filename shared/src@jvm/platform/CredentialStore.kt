package org.baconana.mybeautifulgit_chan.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted-at-rest store for the HTTPS token.
 *
 * The AES-GCM key lives next to the encrypted payload in the application config
 * directory, with owner-only permissions where the filesystem supports them.
 * This is deliberately a local encrypted store, not an OS keychain: it protects
 * against accidental plaintext copies and settings-file backups, not against a
 * process running as the same desktop user.
 */
class CredentialStore(
    private val file: Path = AppPaths.credentialsFile(),
    private val keyFile: Path = AppPaths.credentialKeyFile(),
) {
    private val random = SecureRandom()

    @Synchronized
    fun loadToken(): String? {
        // Tighten files created by older builds before reading the payload/key.
        file.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        keyFile.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (Files.isRegularFile(file)) OwnerOnlyPermissions.restrictFile(file)
        if (Files.isRegularFile(keyFile)) OwnerOnlyPermissions.restrictFile(keyFile)
        if (!Files.isRegularFile(file)) return null
        return runCatching {
            val payload = Base64.getDecoder().decode(Files.readString(file).trim())
            if (payload.size < MAGIC.size + 2 || !payload.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
                return null
            }
            val ivLength = payload[MAGIC.size + 1].toInt() and 0xff
            val ivStart = MAGIC.size + 2
            val cipherStart = ivStart + ivLength
            if (ivLength < 12 || cipherStart >= payload.size) return null
            val key = loadKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, payload.copyOfRange(ivStart, cipherStart)),
            )
            cipher.doFinal(payload.copyOfRange(cipherStart, payload.size))
                .toString(Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    @Synchronized
    fun saveToken(token: String?) {
        if (token.isNullOrBlank()) {
            Files.deleteIfExists(file)
            Files.deleteIfExists(keyFile)
            return
        }
        val parent = file.parent ?: keyFile.parent ?: return
        Files.createDirectories(parent)
        OwnerOnlyPermissions.restrictDirectory(parent)
        val key = loadKey() ?: generateKey()
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val payload = MAGIC + byteArrayOf(1, iv.size.toByte()) + iv + encrypted
        // Write the key first: a token file that cannot be decrypted is worse than no token.
        writeAtomic(keyFile, Base64.getEncoder().encodeToString(key))
        writeAtomic(file, Base64.getEncoder().encodeToString(payload))
    }

    private fun loadKey(): ByteArray? {
        keyFile.parent?.let(OwnerOnlyPermissions::restrictDirectory)
        if (!Files.isRegularFile(keyFile)) return null
        OwnerOnlyPermissions.restrictFile(keyFile)
        return runCatching {
            val key = Base64.getDecoder().decode(Files.readString(keyFile).trim())
            key.takeIf { it.size == 32 }
        }.getOrNull()
    }

    private fun generateKey(): ByteArray =
        ByteArray(32).also(random::nextBytes)

    private fun writeAtomic(path: Path, text: String) {
        val parent = path.parent ?: return
        Files.createDirectories(parent)
        OwnerOnlyPermissions.restrictDirectory(parent)
        val temp = Files.createTempFile(parent, "credential-", ".tmp")
        try {
            Files.writeString(temp, text)
            OwnerOnlyPermissions.restrictFile(temp)
            runCatching {
                Files.move(
                    temp,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }.getOrElse {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            }
            OwnerOnlyPermissions.restrictFile(path)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private companion object {
        val MAGIC = byteArrayOf('M'.code.toByte(), 'B'.code.toByte(), 'G'.code.toByte(), '1'.code.toByte())
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
