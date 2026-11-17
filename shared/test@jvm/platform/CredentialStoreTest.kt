package org.baconana.mybeautifulgit_chan.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CredentialStoreTest {

    @Test
    fun `stores the token encrypted and clears it`() {
        val dir = Files.createTempDirectory("git-chan-credentials")
        val file = dir.resolve("credentials.bin")
        val key = dir.resolve("credentials.key")
        val store = CredentialStore(file, key)
        val token = "ghp_test_token_123"

        store.saveToken(token)
        assertEquals(token, store.loadToken())
        assertFalse(Files.readString(file).contains(token))
        assertFalse(Files.readString(key).contains(token))

        store.saveToken(null)
        assertNull(store.loadToken())
        assertFalse(Files.exists(file))
    }

    @Test
    fun `a different key cannot decrypt the stored token`() {
        val dir = Files.createTempDirectory("git-chan-credentials")
        val file = dir.resolve("credentials.bin")
        CredentialStore(file, dir.resolve("credentials.key")).saveToken("secret")

        val other = CredentialStore(file, dir.resolve("other.key"))
        assertNull(other.loadToken())
    }
}
