package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.git.CommandShellPreference
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.git.ShellFlavor
import org.baconana.mybeautifulgit_chan.git.displayCommandLine
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.git.redactSecrets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitCommandLineTest {

    @Test
    fun `splits plain arguments`() {
        assertEquals(
            GitCommandLine.Parsed.Args(listOf("status", "--short")),
            GitCommandLine.parse("status --short"),
        )
    }

    @Test
    fun `keeps quoted arguments together`() {
        assertEquals(
            GitCommandLine.Parsed.Args(listOf("commit", "-m", "hello world", "trailer")),
            GitCommandLine.parse("commit -m \"hello world\" trailer"),
        )
        assertEquals(
            GitCommandLine.Parsed.Args(listOf("commit", "-m", "it's fine")),
            GitCommandLine.parse("commit -m \"it's fine\""),
        )
    }

    @Test
    fun `keeps an empty quoted argument`() {
        assertEquals(
            GitCommandLine.Parsed.Args(listOf("commit", "-m", "")),
            GitCommandLine.parse("commit -m \"\""),
        )
    }

    @Test
    fun `reports empty input and unclosed quotes`() {
        assertEquals(GitCommandLine.Parsed.Empty, GitCommandLine.parse("   "))
        assertEquals(GitCommandLine.Parsed.UnclosedQuote, GitCommandLine.parse("commit -m \"oops"))
    }

    @Test
    fun `quotes shell metacharacters safely for POSIX`() {
        assertEquals(
            "'name; touch /tmp/pwned'",
            quoteArg("name; touch /tmp/pwned", ShellFlavor.POSIX),
        )
        assertEquals("'left | right'", quoteArg("left | right", ShellFlavor.POSIX))
        assertEquals("'it'\"'\"'s'", quoteArg("it's", ShellFlavor.POSIX))
        assertEquals("commit", quoteArg("commit", ShellFlavor.POSIX))
    }

    @Test
    fun `quotes shell metacharacters safely for cmd`() {
        assertEquals(
            "\"name; touch /tmp/pwned\"",
            quoteArg("name; touch /tmp/pwned", ShellFlavor.CMD),
        )
        assertEquals("\"left | right\"", quoteArg("left | right", ShellFlavor.CMD))
        assertEquals("\"left & right\"", quoteArg("left & right", ShellFlavor.CMD))
        assertEquals("\"it's\"", quoteArg("it's", ShellFlavor.CMD))
        assertEquals("\"back`tick\"", quoteArg("back`tick", ShellFlavor.CMD))
        assertEquals("commit", quoteArg("commit", ShellFlavor.CMD))
    }

    @Test
    fun `quotes shell metacharacters safely for PowerShell`() {
        assertEquals(
            "'name; touch /tmp/pwned'",
            quoteArg("name; touch /tmp/pwned", ShellFlavor.POWERSHELL),
        )
        assertEquals("'left | right'", quoteArg("left | right", ShellFlavor.POWERSHELL))
        assertEquals("'left & right'", quoteArg("left & right", ShellFlavor.POWERSHELL))
        assertEquals("'it''s'", quoteArg("it's", ShellFlavor.POWERSHELL))
        assertEquals("'$(touch /tmp/pwned)'", quoteArg("$(touch /tmp/pwned)", ShellFlavor.POWERSHELL))
        assertEquals("'back`tick'", quoteArg("back`tick", ShellFlavor.POWERSHELL))
        assertEquals("commit", quoteArg("commit", ShellFlavor.POWERSHELL))
    }

    @Test
    fun `quotes percent signs for cmd instead of leaving them expandable`() {
        val quoted = quoteArg("%PATH%", ShellFlavor.CMD)

        assertFalse(quoted.contains("%PATH%"))
        assertTrue(quoted.contains("PATH"))
    }

    @Test
    fun `default command display follows the configured copy shell`() {
        val previous = CommandShellPreference.flavor
        try {
            CommandShellPreference.flavor = ShellFlavor.CMD

            assertEquals("\"name; touch /tmp/pwned\"", quoteArg("name; touch /tmp/pwned"))
            assertEquals(
                "git commit -m \"message; touch /tmp/pwned\"",
                displayCommandLine("git", listOf("commit", "-m", "message; touch /tmp/pwned")),
            )
        } finally {
            CommandShellPreference.flavor = previous
        }
    }

    @Test
    fun `redacts URL credentials and exact secrets from display text`() {
        val text = "password=token-123 url=https://user:secret@example.com/team/repo.git"
        val redacted = redactSecrets(text, listOf("token-123"))

        assertFalse(redacted.contains("token-123"))
        assertFalse(redacted.contains("user:secret"))
        assertTrue(redacted.contains("password=***"))
        assertTrue(redacted.contains("https://***@example.com/team/repo.git"))
    }

    @Test
    fun `builds a safe redacted command line for display and copying`() {
        val result = GitResult(
            args = listOf(
                "remote",
                "add",
                "origin",
                "https://user:secret@example.com/team/repo.git",
            ),
            cwd = null,
            exitCode = 0,
            stdout = "",
            stderr = "",
            durationMs = 0,
        )

        assertEquals(
            "git remote add origin 'https://***@example.com/team/repo.git'",
            result.commandLine,
        )
        assertEquals(
            "git commit -m 'message; touch /tmp/pwned'",
            displayCommandLine(
                "git",
                listOf("commit", "-m", "message; touch /tmp/pwned"),
                ShellFlavor.POSIX,
            ),
        )
    }

    @Test
    fun `keeps credentials out of exception messages`() {
        val result = GitResult(
            args = listOf("remote", "add", "origin", "https://user:secret@example.com/repo.git"),
            cwd = null,
            exitCode = 1,
            stdout = "",
            stderr = "fatal: https://user:secret@example.com/repo.git",
            durationMs = 1,
        )

        val message = GitCommandException(result).message.orEmpty()

        assertFalse(message.contains("secret"))
        assertTrue(message.contains("***@example.com"))
    }
}
