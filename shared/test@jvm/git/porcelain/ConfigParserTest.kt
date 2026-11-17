package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigParserTest {
    @Test
    fun `parses scopes unknown keys aliases includeIf and multiline values`() {
        val output = buildString {
            append("system\u0000file:C:/Program Files/Git/etc/gitconfig\u0000core.symlinks\nfalse\u0000")
            append("global\u0000file:C:/Users/me/.gitconfig\u0000user.name\nMe\u0000")
            append("local\u0000file:.git/config\u0000alias.co\ncheckout\u0000")
            append("local\u0000file:.git/config\u0000includeIf.gitdir:C:/work/.path\nC:/work/config\u0000")
            append("local\u0000file:.git/config\u0000thirdparty.unknown\nline one\nline two\u0000")
            append("command\u0000command line:\u0000color.ui\nfalse\u0000")
            append("worktree\u0000file:.git/config.worktree\u0000worktree.test\nyes\u0000")
        }

        val entries = ConfigParser.parse(output)

        assertEquals(6, entries.size)
        assertTrue(entries.none { it.scope == GitConfigScope.Worktree && it.key == "color.ui" })
        assertEquals(GitConfigScope.System, entries.first().scope)
        assertTrue(entries.any { it.isAlias && it.value == "checkout" })
        assertTrue(entries.any { it.isIncludeIf })
        assertTrue(entries.any { it.key == "thirdparty.unknown" && it.value == "line one\nline two" })
        assertTrue(entries.any { it.scope == GitConfigScope.Worktree })
        assertFalse(entries.any { it.key == "color.ui" && it.scope == GitConfigScope.Worktree })
    }
}
