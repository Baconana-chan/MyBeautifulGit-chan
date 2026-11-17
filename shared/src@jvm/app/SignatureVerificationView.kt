package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.domain.CommitSignatureStatus
import org.baconana.mybeautifulgit_chan.domain.SignatureCheckKind
import org.baconana.mybeautifulgit_chan.domain.SignatureVerification
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
internal fun SignatureVerificationView(
    report: SignatureVerification,
    maxOutputLines: Int = 30,
) {
    val strings = LocalStrings.current
    Column(
        Modifier.fillMaxWidth()
            .background(Term.black)
            .padding(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.signatureVerificationTitle,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (report.verified) strings.signaturePassed else strings.signatureFailed,
                fontSize = 8.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (report.verified) Term.green else Term.red,
            )
        }
        report.checks.forEachIndexed { index, check ->
            if (index > 0) Spacer(Modifier.height(5.dp))
            Column(
                Modifier.fillMaxWidth()
                    .background(Term.panelAlt)
                    .padding(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        signatureCheckLabel(strings, check.kind),
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        fontWeight = FontWeight.SemiBold,
                        color = Term.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        signatureStatusText(strings, check.status),
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = signatureStatusColor(check.status),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        check.command,
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "[copy]",
                        fontSize = 7.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier.clickable {
                            DesktopActions.copy(check.command)
                        },
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    check.output.ifBlank { strings.signatureNoOutput },
                    fontSize = 7.5.sp,
                    fontFamily = Term.mono,
                    color = if (
                        check.status == CommitSignatureStatus.Bad ||
                        check.status == CommitSignatureStatus.Revoked
                    ) {
                        Term.red
                    } else {
                        Term.dim
                    },
                    maxLines = maxOutputLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

internal fun signatureStatusText(strings: Strings, status: CommitSignatureStatus): String = when (status) {
    CommitSignatureStatus.Good -> strings.commitSignatureGood
    CommitSignatureStatus.Untrusted -> strings.commitSignatureUntrusted
    CommitSignatureStatus.Bad -> strings.commitSignatureBad
    CommitSignatureStatus.Expired -> strings.commitSignatureExpired
    CommitSignatureStatus.ExpiredKey -> strings.commitSignatureExpiredKey
    CommitSignatureStatus.Revoked -> strings.commitSignatureRevoked
    CommitSignatureStatus.CannotCheck -> strings.commitSignatureCannotCheck
    CommitSignatureStatus.NoSignature -> strings.commitSignatureNone
    CommitSignatureStatus.Unknown -> strings.commitSignatureUnknown
}

internal fun signatureStatusColor(status: CommitSignatureStatus): Color = when (status) {
    CommitSignatureStatus.Good -> Term.green
    CommitSignatureStatus.Untrusted,
    CommitSignatureStatus.Expired,
    CommitSignatureStatus.ExpiredKey -> Term.amber
    CommitSignatureStatus.Bad,
    CommitSignatureStatus.Revoked -> Term.red
    CommitSignatureStatus.CannotCheck,
    CommitSignatureStatus.Unknown -> Term.faint
    CommitSignatureStatus.NoSignature -> Term.dim
}

private fun signatureCheckLabel(strings: Strings, kind: SignatureCheckKind): String = when (kind) {
    SignatureCheckKind.Commit -> strings.signatureCheckCommit
    SignatureCheckKind.MergeCommit -> strings.signatureCheckMergeCommit
    SignatureCheckKind.MergeTag -> strings.signatureCheckMergeTag
    SignatureCheckKind.Tag -> strings.signatureCheckTag
}
