package org.baconana.mybeautifulgit_chan.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.util.Locale

@Composable
fun OperationActivityBar(controller: RepoController) {
    val strings = LocalStrings.current
    val activity = controller.operationActivity ?: return
    var now by remember(activity.id) { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activity.id, activity.outcome) {
        while (activity.outcome == OperationOutcome.Running) {
            now = System.currentTimeMillis()
            delay(200)
        }
        now = System.currentTimeMillis()
    }
    LaunchedEffect(activity.id, activity.completedAtMs) {
        if (activity.completedAtMs != null) {
            delay(9_000)
            controller.dismissOperationActivity(activity.id)
        }
    }

    val elapsed = activity.durationMs ?: (now - activity.startedAtMs).coerceAtLeast(0)
    // Короткие команды не должны мигать отдельной полосой.
    if (activity.running && elapsed < 650) return

    val accent = when {
        activity.outcome == OperationOutcome.Succeeded -> Term.green
        activity.outcome == OperationOutcome.Failed -> Term.red
        activity.outcome == OperationOutcome.Cancelled -> Term.amber
        activity.outcome == OperationOutcome.TimedOut -> Term.red
        else -> Term.amber
    }
    val label = when {
        activity.cancelling -> strings.operationCancelling
        activity.outcome == OperationOutcome.Running -> strings.operationRunning
        activity.outcome == OperationOutcome.Succeeded -> strings.operationCompleted
        activity.outcome == OperationOutcome.Failed -> strings.operationFailed
        activity.outcome == OperationOutcome.Cancelled -> strings.operationCancelled
        else -> strings.operationTimedOut
    }
    val glyph = when (activity.outcome) {
        OperationOutcome.Running -> if (activity.cancelling) "[..]" else "[»]"
        OperationOutcome.Succeeded -> "[ok]"
        OperationOutcome.Failed -> "[!]"
        OperationOutcome.Cancelled -> "[x]"
        OperationOutcome.TimedOut -> "[!]"
    }

    Column(Modifier.fillMaxWidth()) {
        Divider()
        Row(
            Modifier.fillMaxWidth().height(31.dp).background(Term.panelAlt)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                glyph,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                label,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.a11yLiveRegion(),
            )
            Text(
                "$ ${activity.command}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (activity.progress != null) {
                val progress = activity.progress
                if (progress.percent != null || activity.running) {
                    Text(
                        buildString {
                            progress.stage?.let { append(it).append(' ') }
                            progress.percent?.let { append(it).append('%') }
                        }.ifBlank { "…" },
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 190.dp),
                    )
                    ActivityProgress(progress)
                }
            } else if (activity.running) {
                Text("…", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
                ActivityProgress(null)
            }
            Text(
                formatOperationDuration(elapsed),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
            if (activity.running) {
                Text(
                    if (activity.cancelling) "[…]" else "[${strings.operationCancel}]",
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.cancelling) Term.faint else Term.amber,
                    modifier = Modifier
                        .clickable(enabled = !activity.cancelling) {
                            controller.cancelOperationActivity()
                        }
                        .a11yButton(strings.operationCancel, enabled = !activity.cancelling)
                        .a11yFocusRing(enabled = !activity.cancelling),
                )
            } else {
                Text(
                    "[×]",
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                    modifier = Modifier
                        .clickable {
                            controller.dismissOperationActivity(activity.id)
                        }
                        .a11yButton(strings.a11yDismiss)
                        .a11yFocusRing(),
                )
            }
        }
    }
}

@Composable
private fun ActivityProgress(progress: OperationProgress?) {
    val percent = progress?.percent
    Box(
        Modifier.width(118.dp).height(4.dp).background(Term.border),
    ) {
        if (percent != null) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0.01f, 1f))
                    .background(Term.green),
            )
        } else {
            val transition = rememberInfiniteTransition(label = "operation-progress")
            val shift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "operation-progress-shift",
            )
            Box(
                Modifier.offset(x = (shift * 98f).dp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(Term.amber),
            )
        }
    }
}

internal fun formatOperationDuration(durationMs: Long): String =
    if (durationMs < 1_000) {
        "${durationMs.coerceAtLeast(0)} ms"
    } else {
        String.format(Locale.ROOT, "%.1f s", durationMs / 1000.0)
    }