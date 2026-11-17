package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefsTest {

    @Test
    fun `keeps tag and branch names after the option separator`() {
        assertEquals(
            listOf("tag", "-d", "--", "-foo"),
            tagDeleteArgs("-foo"),
        )
        assertEquals(
            listOf("branch", "-D", "--", "-foo"),
            branchDeleteArgs("-foo", force = true),
        )
        assertEquals(
            listOf("branch", "-d", "--", "-foo"),
            branchDeleteArgs("-foo", force = false),
        )
    }

    @Test
    fun `separates rename and upstream ref arguments`() {
        assertEquals(
            listOf("branch", "-m", "--", "-old", "new"),
            branchRenameArgs("-old", "new"),
        )
        assertEquals(
            listOf(
                "branch",
                "--set-upstream-to=refs/remotes/origin/main",
                "--",
                "-local",
            ),
            branchUpstreamArgs("-local", "refs/remotes/origin/main"),
        )
    }

    @Test
    fun `does not treat empty names as options`() {
        assertEquals(listOf("tag", "-d", "--", ""), tagDeleteArgs("  "))
        assertEquals(listOf("branch", "-d", "--", ""), branchDeleteArgs("", force = false))
        assertTrue(tagDeleteArgs("-foo").contains("--"))
        assertFalse(tagDeleteArgs("-foo").takeWhile { it != "--" }.any { it == "-foo" })
    }
}
