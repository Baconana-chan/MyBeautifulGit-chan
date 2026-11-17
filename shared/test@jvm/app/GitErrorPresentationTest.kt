package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitResult
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import org.baconana.mybeautifulgit_chan.i18n.RussianStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitErrorPresentationTest {

    @Test
    fun `parses command exit and original stderr`() {
        val text = """
            git push origin main → exit 1

            ! [rejected] main -> main (fetch first)
            error: failed to push some refs
        """.trimIndent()

        val presented = presentGitError(text, EnglishStrings)!!
        assertEquals("git push origin main", presented.command)
        assertEquals(1, presented.exitCode)
        assertEquals(EnglishStrings.gitErrorPushRejected, presented.message)
        assertEquals(
            "! [rejected] main -> main (fetch first)\nerror: failed to push some refs",
            presented.stderr,
        )
    }

    @Test
    fun `parses the exception format with ascii arrow`() {
        val result = GitResult(
            args = listOf("status"),
            cwd = "C:/repo",
            exitCode = 128,
            stdout = "",
            stderr = "fatal: not a git repository (or any of the parent directories): .git",
            durationMs = 3,
        )
        val exception = GitCommandException(result)

        val presented = presentGitError(exception.message.orEmpty(), EnglishStrings)!!
        assertEquals("git status", presented.command)
        assertEquals(128, presented.exitCode)
        assertEquals(EnglishStrings.gitErrorNotRepository, presented.message)
        assertTrue(presented.stderr!!.startsWith("fatal: not a git repository"))
    }

    @Test
    fun `explains common failures and preserves localization`() {
        assertEquals(
            EnglishStrings.gitErrorDirtyWorktree,
            humanGitErrorMessage("error: Your local changes would be overwritten by checkout.", EnglishStrings),
        )
        assertEquals(
            EnglishStrings.gitErrorConflicts,
            humanGitErrorMessage("CONFLICT (content): Merge conflict in src/a.kt", EnglishStrings),
        )
        assertEquals(
            EnglishStrings.gitErrorIndexLock,
            humanGitErrorMessage(
                "fatal: Unable to create 'C:/repo/.git/index.lock': File exists.",
                EnglishStrings,
            ),
        )
        assertEquals(
            EnglishStrings.authErrorHttps,
            humanGitErrorMessage("fatal: Authentication failed for 'https://example.test/repo.git/'", EnglishStrings),
        )
        assertEquals(
            RussianStrings.gitErrorPushRejected,
            humanGitErrorMessage("error: failed to push some refs", RussianStrings),
        )
        assertEquals(EnglishStrings.gitErrorFailed, humanGitErrorMessage("fatal: something odd", EnglishStrings))
    }

    @Test
    fun `marks timeouts and truncated output`() {
        val timedOut = presentGitError("git fetch → exit 124 (timed out)", EnglishStrings)!!
        assertEquals(EnglishStrings.gitErrorTimedOut, timedOut.message)
        assertNull(timedOut.stderr)

        val truncated = presentGitError(
            "git clone repo → exit 1 (output truncated)\nwarning: clone failed",
            EnglishStrings,
        )!!
        assertTrue(truncated.truncated)
    }

    @Test
    fun `accepts windows line endings in the header`() {
        val presented = presentGitError(
            "git status -> exit 128\r\nfatal: not a git repository (or any of the parent directories): .git",
            EnglishStrings,
        )!!
        assertEquals("git status", presented.command)
        assertEquals(EnglishStrings.gitErrorNotRepository, presented.message)
    }

    @Test
    fun `does not reinterpret validation messages`() {
        assertNull(presentGitError(EnglishStrings.pickerRepoNotFound, EnglishStrings))
        assertNull(presentGitError("", EnglishStrings))
    }
}
