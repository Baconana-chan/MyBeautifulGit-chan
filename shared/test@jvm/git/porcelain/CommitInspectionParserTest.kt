package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommitInspectionParserTest {

    @Test
    fun `parses text binary and renamed numstat lines`() {
        val files = CommitStatParser.parseNumstat(
            "10\t2\tsrc/a.kt\r\n" +
                "-\t-\tassets/logo.png\n" +
                "1\t1\told name => new name\n" +
                "\n",
        )

        assertEquals(3, files.size)
        assertEquals("src/a.kt", files[0].path)
        assertEquals(10, files[0].additions)
        assertEquals(2, files[0].deletions)
        assertFalse(files[0].binary)

        assertEquals("assets/logo.png", files[1].path)
        assertTrue(files[1].binary)
        assertEquals(null, files[1].additions)

        assertEquals("old name => new name", files[2].path)
        assertFalse(files[2].binary)
    }

    @Test
    fun `maps every git signature code`() {
        fun status(code: String) = CommitSignatureParser.parse("$code\u0000Signer\u0000ABC123\n")

        assertEquals(CommitSignatureStatus.Good, status("G").status)
        assertEquals(CommitSignatureStatus.Untrusted, status("U").status)
        assertEquals(CommitSignatureStatus.Bad, status("B").status)
        assertEquals(CommitSignatureStatus.Expired, status("X").status)
        assertEquals(CommitSignatureStatus.ExpiredKey, status("Y").status)
        assertEquals(CommitSignatureStatus.Revoked, status("R").status)
        assertEquals(CommitSignatureStatus.CannotCheck, status("E").status)
        assertEquals(CommitSignatureStatus.NoSignature, status("N").status)
        assertEquals(CommitSignatureStatus.Unknown, status("?").status)

        val good = status("G")
        assertEquals("Signer", good.signer)
        assertEquals("ABC123", good.key)
    }
}
