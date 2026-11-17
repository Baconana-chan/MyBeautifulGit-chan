package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

/**
 * Диагностическая панель открытого репозитория.
 *
 * Показывает не абстрактный FPS, а конкретные фазы: полный refresh, чтение
 * status/history/refs, построение графа и попадания в кэши. По этим числам
 * видно, что именно тормозит на большом репозитории.
 */
@Composable
fun PerformanceDialog(
    controller: RepoController,
    onDismiss: () -> Unit,
) {
    var report by remember { mutableStateOf(controller.performanceReport()) }
    val spans = report.spans.sortedWith(
        compareByDescending<PerformanceSpan> { it.maxMs }.thenBy { it.name },
    )
    val counters = report.counters.filter { it.name != "profile.reset" }

    DialogShell(title = "PERFORMANCE PROFILE", onDismiss = onDismiss, width = 720.dp) {
        Text(
            "${controller.repository.name} · loaded history and caches for this tab",
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.greenDim,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "HISTORY ${report.historyLoaded}${if (report.historyHasMore) "+" else ""} COMMITS  ·  " +
                "GRAPH ${report.graphRows} ROWS / ${report.graphLanes} LANES  ·  " +
                "INSPECTION CACHE ${report.inspectionCacheEntries} / " +
                formatProfileBytes(report.inspectionCacheBytes),
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Timings are measured in-process. Graph is built only for the loaded page; " +
                "collapse sizes are computed on first access.",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
        Spacer(Modifier.height(10.dp))
        Divider()
        Row(
            Modifier.fillMaxWidth().height(22.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileCell("SPAN", Modifier.weight(1f), color = Term.faint, bold = true)
            ProfileCell("LAST", Modifier.width(78.dp), color = Term.faint, bold = true)
            ProfileCell("AVG", Modifier.width(68.dp), color = Term.faint, bold = true)
            ProfileCell("MAX", Modifier.width(68.dp), color = Term.faint, bold = true)
            ProfileCell("N", Modifier.width(42.dp), color = Term.faint, bold = true)
        }
        Divider()
        if (spans.isEmpty()) {
            Text(
                "No samples yet. Refresh the repository or open the LOG tab.",
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp)) {
                items(spans, key = { it.name }) { span ->
                    Row(
                        Modifier.fillMaxWidth().height(21.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileCell(span.name, Modifier.weight(1f), color = Term.text)
                        ProfileCell(
                            formatProfileDuration(span.lastMs),
                            Modifier.width(78.dp),
                            color = if (span.lastMs >= 250) Term.amber else Term.greenDim,
                        )
                        ProfileCell(formatProfileDuration(span.averageMs), Modifier.width(68.dp))
                        ProfileCell(
                            formatProfileDuration(span.maxMs),
                            Modifier.width(68.dp),
                            color = if (span.maxMs >= 1_000) Term.red else Term.dim,
                        )
                        ProfileCell(span.count.toString(), Modifier.width(42.dp), color = Term.faint)
                    }
                    Divider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (counters.isEmpty()) {
            Text(
                "No cache events recorded yet.",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        } else {
            Text(
                counters.joinToString("  ·  ") { "${it.name} ${it.value}" },
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.dim,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(key = null, label = "REFRESH") {
                report = controller.performanceReport()
            }
            ActionButton(key = null, label = "RESET") {
                controller.resetPerformanceProfile()
                report = controller.performanceReport()
            }
            Spacer(Modifier.weight(1f))
            ActionButton(key = "esc", label = "CLOSE", primary = true, onClick = onDismiss)
        }
    }
}

@Composable
private fun ProfileCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color = Term.dim,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier = modifier,
        fontSize = 8.5.sp,
        fontFamily = Term.mono,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}
