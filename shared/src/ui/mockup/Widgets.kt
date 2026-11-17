package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.background(Term.panel).border(1.dp, Term.border)) {
        if (title != null) {
            Row(
                Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontSize = 9.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.SemiBold,
                    color = Term.green,
                    letterSpacing = 1.sp,
                    modifier = Modifier.a11yHeading(),
                )
                if (trailing != null) {
                    Spacer(Modifier.weight(1f))
                    Text(trailing, fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
                }
            }
            Divider()
        }
        content()
    }
}

@Composable
fun Divider(color: Color = Term.border) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun VDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(Term.border))
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        fontWeight = FontWeight.SemiBold,
        color = Term.dim,
        letterSpacing = 1.sp,
        modifier = modifier.a11yHeading(),
    )
}

@Composable
fun TermButton(
    hotkey: String,
    label: String,
    badge: String? = null,
    active: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier.height(21.dp)
            .background(if (active) Term.green else Term.panelAlt)
            .border(1.dp, if (active) Term.green else Term.border)
            .clickable { onClick() }
            .a11yButton(label = label, selected = active)
            .a11yFocusRing()
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "[$hotkey]",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = if (active) Term.black else Term.green,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = if (active) Term.black else Term.text,
        )
        if (badge != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                "($badge)",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (active) Term.black else Term.amber,
            )
        }
    }
}

@Composable
fun TabItem(
    index: Int,
    label: String,
    badge: String? = null,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxHeight()
            .background(if (active) Term.green else Term.panel)
            .clickable { onClick() }
            .a11yButton(label = label, selected = active, role = Role.Tab)
            .a11yFocusRing()
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$index:",
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = if (active) Term.black else Term.faint,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Term.black else Term.dim,
            letterSpacing = 0.5.sp,
        )
        if (badge != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                "($badge)",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (active) Term.black else Term.amber,
            )
        }
    }
}

@Composable
fun RefTag(ref: MockRef) {
    Text(
        "[${ref.label}]",
        fontSize = 9.sp,
        fontFamily = Term.mono,
        color = refColor(ref.kind),
    )
}

fun refColor(kind: RefKind): Color = when (kind) {
    RefKind.Head, RefKind.Branch -> Term.green
    RefKind.Remote -> Term.cyan
    RefKind.Tag -> Term.amber
}

@Composable
fun LabeledValue(label: String, value: String, valueColor: Color = Term.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            letterSpacing = 0.8.sp,
            modifier = Modifier.width(58.dp),
        )
        Text(
            value,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = valueColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InspectAction(
    hotkey: String,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(22.dp)
            .background(if (danger) Term.red.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .a11yButton(label = label)
            .a11yFocusRing()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "[$hotkey]",
            fontSize = 10.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = if (danger) Term.red else Term.green,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = Term.mono,
            color = if (danger) Term.red else Term.text,
        )
    }
}

@Composable
fun FileCodeTag(code: String) {
    val color = when (code) {
        "A" -> Term.green
        "D" -> Term.red
        "R" -> Term.cyan
        else -> Term.amber
    }
    Text(
        code,
        fontSize = 9.sp,
        fontFamily = Term.mono,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
fun KeyHint(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("[$key]", fontSize = 9.sp, fontFamily = Term.mono, color = Term.green)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
    }
}
