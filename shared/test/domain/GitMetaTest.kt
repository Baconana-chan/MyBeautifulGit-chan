package org.baconana.mybeautifulgit_chan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitMetaTest {
    @Test
    fun `accepts valid gitignore gitattributes and mailmap files`() {
        val ignore = validateGitMeta(
            GitMetaFile.GitIgnore,
            """
            # generated files
            *.log
            !important.log
            build/
            \#literal
            [abc].txt
            """.trimIndent(),
        )
        assertTrue(ignore.ok)
        assertTrue(ignore.warnings.isEmpty())

        val attributes = validateGitMeta(
            GitMetaFile.GitAttributes,
            """
            # line endings
            *.txt text eol=lf
            *.bin binary -diff
            [attr]vendored -text
            """.trimIndent(),
        )
        assertTrue(attributes.ok)
        assertTrue(attributes.warnings.isEmpty())

        val mailmap = validateGitMeta(
            GitMetaFile.Mailmap,
            """
            # canonical names
            Proper Name <commit@example.com>
            <commit2@example.com> <proper@example.com>
            Proper Name <commit3@example.com> <proper3@example.com>
            """.trimIndent(),
        )
        assertTrue(mailmap.ok)
        assertTrue(mailmap.warnings.isEmpty())
    }

    @Test
    fun `reports gitignore and gitattributes syntax problems`() {
        val ignore = validateGitMeta(
            GitMetaFile.GitIgnore,
            "!\nfoo\\\n[abc\nfoo \n",
        )
        assertTrue(ignore.errors.any { it.code == GitMetaIssueCode.IgnoreEmptyNegation })
        assertTrue(ignore.errors.any { it.code == GitMetaIssueCode.TrailingEscape })
        assertTrue(ignore.warnings.any { it.code == GitMetaIssueCode.UnclosedCharacterClass })
        assertTrue(ignore.warnings.any { it.code == GitMetaIssueCode.IgnoreTrailingSpace })

        val attributes = validateGitMeta(
            GitMetaFile.GitAttributes,
            "*.txt\n*.bin - -text\n*.log text text\n*.md text -text\n",
        )
        assertTrue(attributes.warnings.any { it.code == GitMetaIssueCode.AttributesMissing })
        assertTrue(attributes.errors.any { it.code == GitMetaIssueCode.AttributesNameInvalid })
        assertTrue(attributes.errors.any { it.code == GitMetaIssueCode.AttributesDuplicate })
        assertTrue(attributes.warnings.any { it.code == GitMetaIssueCode.AttributesConflict })
    }

    @Test
    fun `checks mailmap email shapes and duplicate mappings`() {
        val result = validateGitMeta(
            GitMetaFile.Mailmap,
            "not an email\n<broken\n<a@b> <c@d> <e@f>\n<a@b> trailing\n",
        )
        assertTrue(result.errors.any { it.code == GitMetaIssueCode.MailmapEmailRequired })
        assertTrue(result.errors.any { it.code == GitMetaIssueCode.MailmapUnclosedEmail })
        assertTrue(result.errors.any { it.code == GitMetaIssueCode.MailmapTooManyEmails })
        assertTrue(result.warnings.any { it.code == GitMetaIssueCode.MailmapUnexpectedText })
        assertTrue(result.warnings.any { it.code == GitMetaIssueCode.MailmapDuplicateMapping })
    }

    @Test
    fun `highlights comments patterns attributes values and mailmap identities`() {
        val ignore = highlightGitMeta(
            GitMetaFile.GitIgnore,
            "# comment\n!keep.log\n*.log\n",
        )
        assertTrue(ignore.any { it.kind == GitMetaTokenKind.Comment })
        assertTrue(ignore.any { it.kind == GitMetaTokenKind.Negation })
        assertTrue(ignore.any { it.kind == GitMetaTokenKind.Pattern })

        val attributes = highlightGitMeta(
            GitMetaFile.GitAttributes,
            "*.txt text eol=lf\n",
        )
        assertTrue(attributes.any { it.kind == GitMetaTokenKind.Pattern })
        assertTrue(attributes.any { it.kind == GitMetaTokenKind.Attribute })
        assertTrue(attributes.any { it.kind == GitMetaTokenKind.Value })

        val mailmap = highlightGitMeta(
            GitMetaFile.Mailmap,
            "Proper Name <commit@example.com>\n",
        )
        assertTrue(mailmap.any { it.kind == GitMetaTokenKind.Name })
        assertTrue(mailmap.any { it.kind == GitMetaTokenKind.Email })
        assertEquals(1, mailmap.count { it.kind == GitMetaTokenKind.Email })
    }
}
