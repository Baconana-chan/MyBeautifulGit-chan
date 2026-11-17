package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.TermButton
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider
import java.io.File

@Composable
fun DirectoryPickerDialog(
    title: String,
    initial: String,
    recent: List<String>,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit,
    onUseSystem: (String) -> Unit,
    confirmLabel: String = "OPEN",
    pathPlaceholder: String? = null,
    hint: String? = null,
    resolveRepository: Boolean = true,
    showRecent: Boolean = true,
) {
    val strings = LocalStrings.current
    val roots = remember { DirectoryBrowser.roots() }
    var current by remember(initial) {
        mutableStateOf(DirectoryBrowser.normalize(initial.ifBlank { DirectoryBrowser.homeDirectory() }))
    }
    var pathText by remember { mutableStateOf(current) }
    var listing by remember { mutableStateOf<DirectoryListing?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showHidden by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(current, showHidden, reloadToken) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            DirectoryBrowser.list(current, showHidden, strings)
        }
        listing = loaded
        pathText = loaded.directory
        loading = false
    }

    fun navigate(path: String) {
        if (path.isBlank()) return
        current = DirectoryBrowser.normalize(path)
    }

    fun openCurrent() {
        val target = if (resolveRepository) {
            listing?.repositoryRoot ?: listing?.directory ?: current
        } else {
            listing?.directory ?: current
        }
        onChoose(target)
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val dialogWidth = minOf(860.dp, maxWidth - 24.dp)
        val bodyHeight = minOf(430.dp, maxHeight - 140.dp).coerceAtLeast(240.dp)

        DialogShell(
            title = title,
            onDismiss = onDismiss,
            width = dialogWidth,
        ) {
        Row(Modifier.fillMaxWidth().height(bodyHeight)) {
            Column(Modifier.width(190.dp).fillMaxHeight().padding(end = 12.dp)) {
                SectionLabel("PLACES")
                Spacer(Modifier.height(5.dp))
                PlaceRow("~", "home", DirectoryBrowser.homeDirectory(), ::navigate)
                roots.forEach { root ->
                    PlaceRow("▣", root, root, ::navigate)
                }

                if (showRecent) {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("RECENT")
                    Spacer(Modifier.height(5.dp))
                    if (recent.isEmpty()) {
                        Text(
                            strings.pickerEmptyRecent,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                        )
                    } else {
                        recent.take(6).forEach { path ->
                            RecentPickRow(path, onChoose)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    if (showRecent) strings.pickerRecentHint else strings.pickerTargetHint,
                    fontSize = 8.sp,
                    fontFamily = Term.mono,
                    color = Term.faint,
                )
            }

            VDivider()

            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TermTextField(
                        value = pathText,
                        onValueChange = { pathText = it },
                        placeholder = pathPlaceholder ?: strings.pickerPathRepo,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(5.dp))
                    ActionButton(
                        key = null,
                        label = "GO",
                        onClick = { navigate(pathText) },
                    )
                    Spacer(Modifier.width(4.dp))
                    ActionButton(
                        key = null,
                        label = "UP",
                        enabled = listing?.parent != null,
                        onClick = { listing?.parent?.let(::navigate) },
                    )
                }

                Spacer(Modifier.height(7.dp))
                val repositoryRoot = listing?.repositoryRoot
                when {
                    !resolveRepository -> {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Term.green.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "[DIR]",
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                fontWeight = FontWeight.Bold,
                                color = Term.green,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                listing?.directory ?: current,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    repositoryRoot != null -> {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Term.green.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (listing?.isBareRepository == true) "[BARE]" else "[GIT]",
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                fontWeight = FontWeight.Bold,
                                color = if (listing?.isBareRepository == true) Term.amber else Term.green,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                repositoryRoot,
                                fontSize = 8.5.sp,
                                fontFamily = Term.mono,
                                color = Term.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    else -> Text(
                        hint ?: strings.pickerRepoNotFound,
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("FOLDERS")
                    Spacer(Modifier.weight(1f))
                    TermButton(
                        hotkey = ".",
                        label = "hidden",
                        active = showHidden,
                        onClick = { showHidden = !showHidden },
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "[reload]",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.greenDim,
                        modifier = Modifier.clickable { reloadToken++ },
                    )
                }

                Divider()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        loading -> Text(
                            "READING…",
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        listing?.error != null -> Column(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                listing?.error.orEmpty(),
                                fontSize = 9.sp,
                                fontFamily = Term.mono,
                                color = Term.red,
                            )
                            ActionButton(
                                key = null,
                                label = "RETRY",
                                onClick = { reloadToken++ },
                            )
                        }

                        else -> LazyColumn(Modifier.fillMaxWidth()) {
                            listing?.parent?.let { parent ->
                                item("parent") {
                                    DirectoryRow(
                                        label = "..",
                                        path = parent,
                                        isRepository = false,
                                        isBare = false,
                                        navigate = ::navigate,
                                    )
                                }
                            }
                            val entries = listing?.entries.orEmpty()
                            if (entries.isEmpty()) {
                                item("empty") {
                                    Text(
                                        strings.pickerNoSubfolders,
                                        fontSize = 9.sp,
                                        fontFamily = Term.mono,
                                        color = Term.faint,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp),
                                    )
                                }
                            } else {
                                items(entries, key = { it.path }) { entry ->
                                    DirectoryRow(
                                        label = entry.name,
                                        path = entry.path,
                                        isRepository = entry.isRepository,
                                        isBare = entry.isBareRepository,
                                        navigate = ::navigate,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Divider()
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("picker: custom", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.faint)
            Spacer(Modifier.width(9.dp))
            Text(
                "[switch to system]",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.greenDim,
                modifier = Modifier.clickable { onUseSystem(listing?.directory ?: current) },
            )
            Spacer(Modifier.weight(1f))
            ActionButton(
                key = null,
                label = "SYSTEM",
                onClick = { onUseSystem(listing?.directory ?: current) },
            )
            Spacer(Modifier.width(8.dp))
            ActionButton(key = null, label = "CANCEL", onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = confirmLabel,
                primary = true,
                enabled = !loading,
                onClick = { openCurrent() },
            )
        }
        }
    }
}

@Composable
private fun PlaceRow(marker: String, label: String, path: String, onNavigate: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(22.dp)
            .clickable { onNavigate(path) }
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(marker, fontSize = 9.sp, fontFamily = Term.mono, color = Term.green)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentPickRow(path: String, onChoose: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(27.dp)
            .clickable { onChoose(path) }
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("›", fontSize = 11.sp, fontFamily = Term.mono, color = Term.greenDim)
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                File(path).name.ifBlank { path },
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                color = Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                path,
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DirectoryRow(
    label: String,
    path: String,
    isRepository: Boolean,
    isBare: Boolean,
    navigate: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(23.dp)
            .clickable { navigate(path) }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("[D]", fontSize = 8.5.sp, fontFamily = Term.mono, color = Term.greenDim)
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            fontSize = 9.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isRepository) {
            Text(
                if (isBare) "[bare]" else "[git]",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = if (isBare) Term.amber else Term.green,
            )
        }
    }
}
