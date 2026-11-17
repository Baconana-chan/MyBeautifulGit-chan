package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import java.io.File

/** Horizontal repository switcher shown above the active repository workspace. */
@Composable
fun RepositoryTabsBar(
    repositories: List<Repository>,
    activeRoot: String?,
    onSelect: (Repository) -> Unit,
    onClose: (Repository) -> Unit,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val tabListState = rememberLazyListState()
    LaunchedEffect(repositories, activeRoot) {
        val index = repositories.indexOfFirst { sameRepositoryRoot(it.root, activeRoot) }
        if (index >= 0) tabListState.animateScrollToItem(index)
    }
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(Term.panelAlt)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "REPOS",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            fontWeight = FontWeight.Bold,
            color = Term.faint,
            letterSpacing = 1.sp,
            modifier = Modifier.a11yHeading(),
        )
        LazyRow(
            state = tabListState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(repositories, key = { it.root }) { repository ->
                val active = sameRepositoryRoot(repository.root, activeRoot)
                Row(
                    Modifier.height(23.dp)
                        .background(if (active) Term.raised else Color.Transparent)
                        .padding(start = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .clickable { onSelect(repository) }
                            .a11yButton(
                                label = repositoryTabLabel(repository, repositories),
                                selected = active,
                                role = Role.Tab,
                            )
                            .a11yFocusRing(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (active) "●" else "○",
                            fontSize = 7.sp,
                            fontFamily = Term.mono,
                            color = if (active) Term.green else Term.faint,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            repositoryTabLabel(repository, repositories),
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) Term.white else Term.dim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp),
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "[×]",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                        modifier = Modifier
                            .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                            .clickable { onClose(repository) }
                            .a11yButton(strings.a11yCloseRepository(repositoryTabLabel(repository, repositories)))
                            .a11yFocusRing()
                            .padding(end = 7.dp),
                    )
                }
            }
        }
        ActionButton(key = null, label = strings.open, onClick = onOpen)
    }
}

internal fun sameRepositoryRoot(first: String?, second: String?): Boolean {
    if (first == null || second == null) return false
    val left = comparableRoot(first)
    val right = comparableRoot(second)
    return left.isNotEmpty() && left == right
}

private fun comparableRoot(path: String): String {
    val trimmed = path.trim()
    val normalized = trimmed.trimEnd('/', '\\').ifBlank { trimmed }
    return if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) {
        normalized.lowercase()
    } else {
        normalized
    }
}

private fun repositoryTabLabel(repository: Repository, repositories: List<Repository>): String {
    val duplicateName = repositories.count { it.name.equals(repository.name, ignoreCase = true) } > 1
    if (!duplicateName) return repository.name
    val parent = File(repository.root).parentFile?.name
    return if (parent.isNullOrBlank()) repository.name else "$parent/${repository.name}"
}