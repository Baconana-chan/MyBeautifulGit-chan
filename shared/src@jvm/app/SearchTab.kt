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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.BlameLine
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.GrepMatch
import org.baconana.mybeautifulgit_chan.domain.GrepResult
import org.baconana.mybeautifulgit_chan.git.quoteArg
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

enum class SearchMode { History, Blame, Grep }

/** Запрос на открытие поиска по файлу из панели diff. */
data class SearchOpenRequest(
    val mode: SearchMode,
    val path: String,
    val id: Long,
)

@Composable
fun SearchTab(
    controller: RepoController,
    scope: CoroutineScope,
    mode: SearchMode,
    path: String,
    query: String,
    onModeChange: (SearchMode) -> Unit,
    onPathChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    openRequest: SearchOpenRequest?,
    onOpenRequestHandled: () -> Unit,
    onOpenCommit: (String) -> Unit,
) {
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        onModeChange(request.mode)
        onPathChange(request.path)
        // Запускаем чтение в scope вкладки, а не в LaunchedEffect: очистка
        // openRequest не должна отменять уже начатую загрузку.
        when (request.mode) {
            SearchMode.History -> scope.launch { controller.loadFileHistory(request.path) }
            SearchMode.Blame -> scope.launch { controller.loadBlame(request.path) }
            SearchMode.Grep -> Unit
        }
        onOpenRequestHandled()
    }

    val loading = when (mode) {
        SearchMode.History -> controller.fileHistoryLoading
        SearchMode.Blame -> controller.blameLoading
        SearchMode.Grep -> controller.grepLoading
    }
    val canRun = !controller.busy && !loading &&
        when (mode) {
            SearchMode.Grep -> query.isNotBlank()
            else -> path.isNotBlank()
        }

    fun run() {
        if (!canRun) return
        when (mode) {
            SearchMode.History -> scope.launch { controller.loadFileHistory(path) }
            SearchMode.Blame -> scope.launch { controller.loadBlame(path) }
            SearchMode.Grep -> scope.launch { controller.runGrep(query, path) }
        }
    }

    Column(Modifier.fillMaxSize().background(Term.panel)) {
        Row(
            Modifier.fillMaxWidth().height(26.dp).background(Term.raised)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "SEARCH",
                fontSize = 8.5.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.SemiBold,
                color = Term.green,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(12.dp))
            SearchModeButton("HISTORY", mode == SearchMode.History) { onModeChange(SearchMode.History) }
            SearchModeButton("BLAME", mode == SearchMode.Blame) { onModeChange(SearchMode.Blame) }
            SearchModeButton("GREP", mode == SearchMode.Grep) { onModeChange(SearchMode.Grep) }
            Spacer(Modifier.weight(1f))
            Text(
                "read-only git",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Divider()
        SearchInputs(
            mode = mode,
            path = path,
            query = query,
            loading = loading,
            canRun = canRun,
            onPathChange = onPathChange,
            onQueryChange = onQueryChange,
            onRun = { run() },
        )
        Divider()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (mode) {
                SearchMode.History -> HistoryResults(
                    controller = controller,
                    onOpenCommit = onOpenCommit,
                )
                SearchMode.Blame -> BlameResults(
                    controller = controller,
                    onOpenCommit = onOpenCommit,
                )
                SearchMode.Grep -> GrepResults(controller)
            }
        }
    }
}

@Composable
private fun SearchModeButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().background(if (active) Term.panel else Term.raised)
            .clickable(onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Term.green else Term.faint,
            letterSpacing = 0.8.sp,
        )
    }
    Spacer(Modifier.width(2.dp))
}

@Composable
private fun SearchInputs(
    mode: SearchMode,
    path: String,
    query: String,
    loading: Boolean,
    canRun: Boolean,
    onPathChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onRun: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().background(Term.panel).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (mode == SearchMode.Grep) {
                SearchField(
                    label = "QUERY",
                    value = query,
                    placeholder = strings.searchQueryPlaceholder,
                    onValueChange = onQueryChange,
                    onSubmit = onRun,
                    modifier = Modifier.weight(1.25f),
                )
                Spacer(Modifier.width(7.dp))
                SearchField(
                    label = "PATH",
                    value = path,
                    placeholder = strings.searchPathOptionalPlaceholder,
                    onValueChange = onPathChange,
                    onSubmit = onRun,
                    modifier = Modifier.weight(1f),
                )
            } else {
                SearchField(
                    label = "PATH",
                    value = path,
                    placeholder = strings.searchPathPlaceholder,
                    onValueChange = onPathChange,
                    onSubmit = onRun,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(8.dp))
            ActionButton(
                key = null,
                label = if (loading) "WAIT…" else "RUN",
                primary = true,
                enabled = canRun,
                onClick = onRun,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            when (mode) {
                SearchMode.History -> strings.searchHistoryHint
                SearchMode.Blame -> strings.searchBlameHint
                SearchMode.Grep -> strings.searchGrepHint
            },
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            searchCommand(mode, path, query),
            fontSize = 7.5.sp,
            fontFamily = Term.mono,
            color = Term.greenDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.dim)
        Spacer(Modifier.height(2.dp))
        TermTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun HistoryResults(controller: RepoController, onOpenCommit: (String) -> Unit) {
    val strings = LocalStrings.current
    val commits = controller.fileHistoryCommits
    when {
        controller.fileHistoryLoading -> SearchPlaceholder(strings.searchHistoryLoading, Term.faint)
        controller.fileHistoryError != null -> SearchPlaceholder(
            "${strings.searchHistoryFailed}: ${controller.fileHistoryError}",
            Term.red,
        )
        controller.fileHistoryPath == null -> SearchPlaceholder(strings.searchPrompt, Term.faint)
        commits.isEmpty() -> SearchPlaceholder(strings.searchHistoryEmpty, Term.faint)
        else -> Column(Modifier.fillMaxSize()) {
            SearchResultHeader("${commits.size} commits", controller.fileHistoryPath.orEmpty())
            LazyColumn(Modifier.fillMaxSize()) {
                items(commits, key = { it.hash }) { commit ->
                    HistoryRow(commit, onOpenCommit)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(commit: Commit, onOpenCommit: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(24.dp)
            .clickable { onOpenCommit(commit.hash) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            commit.shortHash,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.greenDim,
            modifier = Modifier.width(62.dp),
        )
        Text(
            commit.subject,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = Term.white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            commit.authorName,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(110.dp),
        )
        Text(
            formatSearchAge(commit.authorTime),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun BlameResults(controller: RepoController, onOpenCommit: (String) -> Unit) {
    val strings = LocalStrings.current
    val result = controller.blameResult
    when {
        controller.blameLoading -> SearchPlaceholder(strings.searchBlameLoading, Term.faint)
        controller.blameError != null -> SearchPlaceholder(
            "${strings.searchBlameFailed}: ${controller.blameError}",
            Term.red,
        )
        result == null -> SearchPlaceholder(strings.searchPrompt, Term.faint)
        result.lines.isEmpty() -> SearchPlaceholder(strings.searchBlameEmpty, Term.faint)
        else -> Column(Modifier.fillMaxSize()) {
            SearchResultHeader(
                "${result.lines.size} lines" + if (result.truncated) " · ${strings.searchTruncated}" else "",
                result.path,
            )
            BlameHeader()
            LazyColumn(Modifier.fillMaxSize()) {
                items(result.lines, key = { "${it.lineNumber}:${it.hash}" }) { line ->
                    BlameRow(line, onOpenCommit)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
                }
            }
        }
    }
}

@Composable
private fun BlameHeader() {
    Row(
        Modifier.fillMaxWidth().height(19.dp).background(Term.raised).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("LINE", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(48.dp))
        Text("COMMIT", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(62.dp))
        Text("AUTHOR", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint, modifier = Modifier.width(110.dp))
        Text("CODE", fontSize = 7.5.sp, fontFamily = Term.mono, color = Term.faint)
    }
}

@Composable
private fun BlameRow(line: BlameLine, onOpenCommit: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(19.dp)
            .clickable { onOpenCommit(line.hash) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            line.lineNumber.toString(),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp).padding(end = 7.dp),
        )
        Text(
            line.hash.take(7),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = if (line.hash.startsWith("000000")) Term.faint else Term.greenDim,
            modifier = Modifier.width(62.dp),
        )
        Text(
            line.authorName,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(110.dp),
        )
        Text(
            line.text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GrepResults(controller: RepoController) {
    val strings = LocalStrings.current
    val result = controller.grepResult
    when {
        controller.grepLoading -> SearchPlaceholder(strings.searchGrepLoading, Term.faint)
        controller.grepError != null -> SearchPlaceholder(
            "${strings.searchGrepFailed}: ${controller.grepError}",
            Term.red,
        )
        result == null -> SearchPlaceholder(strings.searchPrompt, Term.faint)
        result.matches.isEmpty() -> SearchPlaceholder(strings.searchGrepEmpty, Term.faint)
        else -> Column(Modifier.fillMaxSize()) {
            SearchResultHeader(
                "${result.matches.size} matches" + if (result.truncated) " · ${strings.searchTruncated}" else "",
                "",
            )
            GrepMatchList(result)
        }
    }
}

@Composable
private fun GrepMatchList(result: GrepResult) {
    val grouped = remember(result) { result.matches.groupBy { it.path } }
    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (path, matches) ->
            item(key = "path:$path") {
                Row(
                    Modifier.fillMaxWidth().height(20.dp).background(Term.raised)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(path)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "${matches.size}",
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                }
            }
            items(matches, key = { "${it.path}:${it.lineNumber}:${it.text.hashCode()}" }) { match ->
                GrepRow(match)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Term.panelAlt))
            }
        }
    }
}

@Composable
private fun GrepRow(match: GrepMatch) {
    Row(
        Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            match.lineNumber.toString(),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp).padding(end = 7.dp),
        )
        Text(
            match.text,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchResultHeader(summary: String, path: String) {
    Row(
        Modifier.fillMaxWidth().height(20.dp).background(Term.panelAlt)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(summary, fontSize = 8.sp, fontFamily = Term.mono, color = Term.greenDim)
        Spacer(Modifier.width(9.dp))
        Text(
            path,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchPlaceholder(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 9.sp,
            fontFamily = Term.mono,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

private fun searchCommand(mode: SearchMode, path: String, query: String): String = when (mode) {
    SearchMode.History ->
        "git log --follow --max-count=200 --pretty=format:%H%x00%P%x00%an%x00%ae%x00%at%x00%cn%x00%ce%x00%ct%x00%s%x00%D -- ${quoteArg(path.trim())}"
    SearchMode.Blame ->
        "git blame --line-porcelain -L 1,5001 HEAD -- ${quoteArg(path.trim())}"
    SearchMode.Grep -> buildString {
        append("git grep -n -I -F --no-color --null -m 20 -e ")
        append(quoteArg(query.trim()))
        if (path.isNotBlank()) {
            append(" -- ")
            append(quoteArg(path.trim()))
        }
    }
}

private fun formatSearchAge(epochSeconds: Long): String {
    val seconds = (System.currentTimeMillis() / 1000 - epochSeconds).coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3600}h"
        seconds < 604_800 -> "${seconds / 86_400}d"
        else -> "${seconds / 604_800}w"
    }
}
