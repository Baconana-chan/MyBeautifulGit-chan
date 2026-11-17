package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitConfigTest {
    @Test
    fun `infers common types but keeps unknown keys as strings`() {
        assertEquals(GitConfigValueType.Boolean, inferGitConfigValueType("commit.gpgsign"))
        assertEquals(GitConfigValueType.Integer, inferGitConfigValueType("gc.auto"))
        assertEquals(GitConfigValueType.Color, inferGitConfigValueType("color.ui"))
        assertEquals(GitConfigValueType.Path, inferGitConfigValueType("includeIf.gitdir:C:/repo/.path"))
        assertEquals(GitConfigValueType.BooleanOrNamedValue, inferGitConfigValueType("core.autocrlf"))
        assertEquals(GitConfigValueType.String, inferGitConfigValueType("thirdparty.unknownKey"))
    }

    @Test
    fun `validates keys aliases booleans integers colors and includeIf`() {
        assertTrue(validGitConfigKey("alias.co"))
        assertTrue(validGitConfigKey("thirdparty.section.key"))
        assertTrue(validGitConfigKey("includeIf.gitdir:C:/work/.path"))
        assertFalse(validGitConfigKey("alias"))
        assertFalse(validGitConfigKey("-alias.co"))
        assertFalse(validGitConfigKey("alias.co\nbad"))

        assertTrue(validateGitConfigEntry("alias.co", "checkout").ok)
        assertFalse(validateGitConfigEntry("alias.co", "").ok)
        assertFalse(validateGitConfigEntry("alias.", "checkout").ok)

        assertTrue(validateGitConfigEntry("commit.gpgsign", "yes").ok)
        assertFalse(validateGitConfigEntry("commit.gpgsign", "maybe").ok)
        assertTrue(validateGitConfigEntry("core.autocrlf", "input").ok)
        assertTrue(validateGitConfigEntry("gc.auto", "2g").ok)
        assertFalse(validateGitConfigEntry("gc.auto", "many").ok)
        assertTrue(validateGitConfigEntry("color.ui", "always").ok)
        assertTrue(validateGitConfigEntry("color.branch", "red bold").ok)
        assertFalse(validateGitConfigEntry("color.ui", "chartreuse").ok)

        assertTrue(
            validateGitConfigEntry(
                "includeIf.gitdir:C:/work/.path",
                "C:/work/.gitconfig",
            ).ok,
        )
        assertFalse(
            validateGitConfigEntry(
                "includeIf.unknown-condition.path",
                "C:/work/.gitconfig",
            ).ok,
        )
        assertFalse(
            validateGitConfigEntry(
                "includeIf.gitdir:C:/work/.path",
                "",
            ).ok,
        )

        assertTrue(validateGitConfigEntry("thirdparty.unknownKey", "anything").ok)
    }

    @Test
    fun `requires confirmation only for system and global scopes`() {
        assertTrue(requiresGitConfigScopeConfirmation(GitConfigScope.System))
        assertTrue(requiresGitConfigScopeConfirmation(GitConfigScope.Global))
        assertFalse(requiresGitConfigScopeConfirmation(GitConfigScope.Local))
        assertFalse(requiresGitConfigScopeConfirmation(GitConfigScope.Worktree))
    }

    @Test
    fun `extracts runnable aliases with last definition winning`() {
        val entries = listOf(
            GitConfigEntry("alias.co", "checkout", GitConfigScope.Global),
            GitConfigEntry("alias.co", "switch", GitConfigScope.Local),
            GitConfigEntry("alias.lg", "log --oneline --graph", GitConfigScope.Local),
            GitConfigEntry("alias.shell", "!echo hi", GitConfigScope.Local),
            GitConfigEntry("alias.", "status", GitConfigScope.Local),
            GitConfigEntry("alias.bad name", "status", GitConfigScope.Local),
            GitConfigEntry("core.editor", "vim", GitConfigScope.Global),
        )

        val aliases = gitAliases(entries)

        assertEquals(listOf("co", "lg", "shell"), aliases.map { it.name })
        assertEquals("switch", aliases.single { it.name == "co" }.command)
        assertEquals(GitConfigScope.Local, aliases.single { it.name == "co" }.scope)
        assertTrue(aliases.single { it.name == "shell" }.isShell)
        assertFalse(aliases.single { it.name == "lg" }.isShell)
    }
}
