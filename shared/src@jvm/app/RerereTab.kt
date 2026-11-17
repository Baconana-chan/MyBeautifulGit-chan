package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.RerereDetail
import org.baconana.mybeautifulgit_chan.domain.RerereEntry
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider

@Composable
fun RerereTab(
    controller: RepoController,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val state = controller.rerere
    val entries = state?.entries.orEmpty()
    var selectedHash by remember(controller.repository.root) { mutableStateOf<String?>(null) }
    var confirmClear by remember(controller.repository.root) { mutableStateOf(false) }

    LaunchedEffect(entries) {
        if (entries.none { it.hash == selectedHash }) {
            selectedHash = entries.firstOrNull()?.hash
        }
    }
    LaunchedEffect(selectedHash) {
        val hash = selectedHash ?: return@LaunchedEffect
        if (controller.rerereDetail?.hash != hash) controller.selectRerere(hash)
    }

    Column(modifier.fillMaxSize().background(Term.bg)) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).background(Term.panel)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.rerereTitle,
                fontSize = 10.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (state?.enabled == true) strings.rerereEnabled else strings.rerereDisabled,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = if (state?.enabled == true) Term.green else Term.faint,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${entries.size} ${strings.rerereRecorded}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$ ${strings.rerereConfigHint} ${if (state?.enabled == true) "false" else "true"}",
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = if (state?.enabled == true) strings.rerereDisable else strings.rerereEnable,
                enabled = state != null && !controller.busy,
                primary = state?.enabled != true,
            ) {
                scope.launch { controller.setRerereEnabled(state?.enabled != true) }
            }
            Spacer(Modifier.width(6.dp))
            ActionButton(
                key = null,
                label = strings.rerereClear,
                enabled = entries.isNotEmpty() && !controller.busy,
                danger = true,
            ) {
                confirmClear = true
            }
        }
        Divider()

        if (confirmClear) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Term.red.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        strings.rerereClearConfirm,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        fontWeight = FontWeight.Bold,
                        color = Term.red,
                    )
                    Text(
                        strings.rerereClearHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.text,
                    )
                    Text(
                        state?.cachePath.orEmpty(),
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ActionButton(
                    key = null,
                    label = strings.rerereCancel,
                    enabled = !controller.busy,
                ) { confirmClear = false }
                Spacer(Modifier.width(6.dp))
                ActionButton(
                    key = null,
                    label = strings.rerereConfirm,
                    enabled = !controller.busy,
                    danger = true,
                ) {
                    scope.launch {
                        controller.clearRerereCache()
                        confirmClear = false
                    }
                }
            }
            Divider()
        }

        if (entries.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        strings.rerereEmpty,
                        fontSize = 10.sp,
                        fontFamily = Term.mono,
                        color = Term.dim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.rerereEmptyHint,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    Modifier.width(240.dp).fillMaxHeight().background(Term.panel),
                ) {
                    items(entries, key = { it.hash }) { entry ->
                        RerereRow(
                            entry = entry,
                            selected = entry.hash == selectedHash,
                            onClick = { selectedHash = entry.hash },
                        )
                    }
                }
                VDivider()
                val detail = controller.rerereDetail
                if (detail != null && detail.hash == selectedHash) {
                    RerereDetailView(detail, Modifier.weight(1f))
                } else {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(
                            strings.rerereSelectHint,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RerereRow(entry: RerereEntry, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) Term.raised else Term.panel)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            entry.hash,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Term.green else Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "pre ${formatBytes(entry.preimageBytes)} · post ${formatBytes(entry.postimageBytes)}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}

@Composable
private fun RerereDetailView(detail: RerereDetail, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight()) {
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "#${detail.hash}",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "pre ${formatBytes(detail.preimageBytes)} · post ${formatBytes(detail.postimageBytes)}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Divider()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            RerereFilePane(
                label = LocalStrings.current.rererePreimage,
                text = detail.preimage,
                bytes = detail.preimageBytes,
                truncated = detail.preimageTruncated,
                modifier = Modifier.weight(1f),
            )
            VDivider()
            RerereFilePane(
                label = LocalStrings.current.rererePostimage,
                text = detail.postimage,
                bytes = detail.postimageBytes,
                truncated = detail.postimageTruncated,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RerereFilePane(
    label: String,
    text: String?,
    bytes: Long,
    truncated: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier.fillMaxHeight().background(Term.bg)) {
        Row(
            Modifier.fillMaxWidth().height(22.dp).background(Term.panel)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (text == null) strings.rerereMissingFile else formatBytes(bytes),
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = if (text == null) Term.amber else Term.faint,
            )
        }
        Divider()
        if (text == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    strings.rerereMissingFile,
                    fontSize = 8.5.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }
        } else {
            val lines = remember(text) { text.lineSequence().toList() }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
                itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "${index + 1}",
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.width(34.dp),
                        )
                        Text(
                            line.ifEmpty { " " },
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                        )
                    }
                }
                if (truncated) {
                    item {
                        Text(
                            strings.rerereTruncated,
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.amber,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MiB"
    bytes >= 1024 -> "${bytes / 1024} KiB"
    else -> "$bytes B"
}
