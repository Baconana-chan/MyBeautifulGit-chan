package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

@Composable
fun OperationToastBar(
    toast: OperationToast,
    onCopy: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val accent = if (toast.success) Term.green else Term.red

    Row(
        modifier.widthIn(max = 900.dp)
            .background(Term.panelAlt)
            .border(1.dp, accent)
            .a11yLiveRegion(assertive = !toast.success)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (toast.success) "[ok]" else "[!]",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            toast.message,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
        )
        if (toast.undo != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                "[${strings.toastUndo}]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.amber,
                modifier = Modifier
                    .clickable { onUndo?.invoke() }
                    .a11yButton(strings.toastUndo)
                    .a11yFocusRing(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "$ ${toast.command}",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = if (toast.success) Term.greenDim else Term.amber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "[${strings.toastCopy}]",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.green,
            modifier = Modifier
                .clickable(onClick = onCopy)
                .a11yButton(strings.toastCopy)
                .a11yFocusRing(),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "[x]",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .a11yButton(strings.a11yDismiss)
                .a11yFocusRing(),
        )
    }
}
