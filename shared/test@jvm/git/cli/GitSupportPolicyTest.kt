package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.git.GitVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitSupportPolicyTest {
    private val policy = GitSupportPolicy()

    @Test
    fun `hard minimum is the compatibility floor`() {
        assertFalse(policy.meetsHardMinimum(GitVersion(2, 29, 9, "2.29.9")))
        assertTrue(policy.meetsHardMinimum(GitVersion(2, 30, 0, "2.30.0")))
    }

    @Test
    fun `full toolset follows the newest planned feature`() {
        assertEquals(GitFeature.entries.maxOf { it.since }, policy.fullToolset)
        assertFalse(policy.hasFullToolset(GitVersion(2, 39, 0, "2.39.0")))
        assertTrue(policy.hasFullToolset(GitVersion(2, 40, 0, "2.40.0")))
    }

    @Test
    fun `security support is independent from feature completeness`() {
        assertFalse(policy.hasSecuritySupport(GitVersion(2, 40, 0, "2.40.0")))
        assertTrue(policy.hasSecuritySupport(GitVersion(2, 52, 0, "2.52.0")))
        assertTrue(policy.hasSecuritySupport(GitVersion(2, 56, 0, "2.56.0")))
    }
}
