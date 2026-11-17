package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.baconana.mybeautifulgit_chan.domain.GitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.GitConfigValueType
import org.baconana.mybeautifulgit_chan.domain.inferGitConfigValueType
import org.baconana.mybeautifulgit_chan.domain.requiresGitConfigScopeConfirmation
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Divider
import org.baconana.mybeautifulgit_chan.ui.mockup.SectionLabel
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.VDivider

private enum class ConfigSection { All, Aliases, IncludeIf }
private enum class ConfigConfirmation { Save, Delete }

@Composable
fun ConfigEditorDialog(controller: RepoController, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<GitConfigEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("") }
    var viewScope by remember { mutableStateOf<GitConfigScope?>(null) }
    var section by remember { mutableStateOf(ConfigSection.All) }
    var editScope by remember { mutableStateOf(GitConfigScope.Local) }
    var editKey by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    var original by remember { mutableStateOf<GitConfigEntry?>(null) }
    var confirmation by remember { mutableStateOf<ConfigConfirmation?>(null) }
    var confirmationScope by remember { mutableStateOf(GitConfigScope.Local) }
    var confirmationKey by remember { mutableStateOf("") }
    var confirmationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun cancelConfirmation() {
        confirmation = null
        confirmationAction = null
    }

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            entries = controller.loadConfigEntries()
            if (controller.error != null) error = controller.error
            loading = false
        }
    }

    fun startNew() {
        cancelConfirmation()
        original = null
        editKey = ""
        editValue = ""
        editScope = viewScope ?: GitConfigScope.Local
        notice = null
    }

    fun select(entry: GitConfigEntry) {
        cancelConfirmation()
        original = entry
        editScope = entry.scope
        editKey = entry.key
        editValue = entry.value
        error = null
        notice = null
    }

    fun save(
        targetScope: GitConfigScope,
        targetKey: String,
        targetValue: String,
        oldValue: String?,
    ) {
        scope.launch {
            error = null
            val cleanKey = targetKey.trim()
            val saved = if (oldValue == null) {
                controller.addConfigEntry(
                    scope = targetScope,
                    key = cleanKey,
                    value = targetValue,
                    confirmed = true,
                )
            } else {
                controller.saveConfigEntry(
                    scope = targetScope,
                    key = cleanKey,
                    value = targetValue,
                    oldValue = oldValue,
                    confirmed = true,
                )
            }
            if (saved) {
                notice = strings.configSaved
                original = GitConfigEntry(
                    key = cleanKey,
                    value = targetValue,
                    scope = targetScope,
                    origin = original?.origin.orEmpty(),
                )
                refresh()
            } else {
                error = controller.error
            }
        }
    }

    fun requestSave() {
        val targetScope = editScope
        val targetKey = editKey.trim()
        val targetValue = editValue
        if (targetKey.isBlank()) return
        val oldValue = original
            ?.takeIf { it.scope == targetScope && it.key.equals(targetKey, ignoreCase = true) }
            ?.value
        val action = { save(targetScope, targetKey, targetValue, oldValue) }
        if (requiresGitConfigScopeConfirmation(targetScope)) {
            confirmation = ConfigConfirmation.Save
            confirmationScope = targetScope
            confirmationKey = targetKey
            confirmationAction = action
        } else {
            action()
        }
    }

    fun delete(entry: GitConfigEntry) {
        scope.launch {
            error = null
            if (controller.deleteConfigEntry(
                    scope = entry.scope,
                    key = entry.key,
                    oldValue = entry.value,
                    confirmed = true,
                )
            ) {
                notice = strings.configDeleted
                original = null
                editKey = ""
                editValue = ""
                refresh()
            } else {
                error = controller.error
            }
        }
    }

    fun requestDelete() {
        val entry = original ?: return
        val action = { delete(entry) }
        if (requiresGitConfigScopeConfirmation(entry.scope)) {
            confirmation = ConfigConfirmation.Delete
            confirmationScope = entry.scope
            confirmationKey = entry.key
            confirmationAction = action
        } else {
            action()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val filtered = entries
        .filter { entry ->
            (viewScope == null || entry.scope == viewScope) &&
                when (section) {
                    ConfigSection.All -> true
                    ConfigSection.Aliases -> entry.isAlias
                    ConfigSection.IncludeIf -> entry.isIncludeIf
                } &&
                (filter.isBlank() ||
                    entry.key.contains(filter, ignoreCase = true) ||
                    entry.value.contains(filter, ignoreCase = true))
        }
        .sortedWith(compareBy({ scopeOrder(it.scope) }, { it.key.lowercase() }))

    DialogShell(title = strings.configTitle, onDismiss = onDismiss, width = 1_020.dp) {
        Column(Modifier.fillMaxWidth().height(650.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScopeChip(strings.configAll, viewScope == null) { viewScope = null }
                ScopeChip(strings.configScopeSystem, viewScope == GitConfigScope.System) {
                    viewScope = GitConfigScope.System
                }
                ScopeChip(strings.configScopeGlobal, viewScope == GitConfigScope.Global) {
                    viewScope = GitConfigScope.Global
                }
                ScopeChip(strings.configScopeLocal, viewScope == GitConfigScope.Local) {
                    viewScope = GitConfigScope.Local
                }
                ScopeChip(strings.configScopeWorktree, viewScope == GitConfigScope.Worktree) {
                    viewScope = GitConfigScope.Worktree
                }
                Spacer(Modifier.weight(1f))
                ActionButton(
                    key = null,
                    label = strings.configReload,
                    enabled = !loading && !controller.busy,
                    onClick = {
                        cancelConfirmation()
                        refresh()
                    },
                )
                Spacer(Modifier.width(6.dp))
                ActionButton(
                    key = null,
                    label = strings.configNew,
                    enabled = !loading && !controller.busy,
                    onClick = ::startNew,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionChip(strings.configAll, section == ConfigSection.All) {
                    section = ConfigSection.All
                }
                SectionChip(strings.configAliases, section == ConfigSection.Aliases) {
                    section = ConfigSection.Aliases
                }
                SectionChip(strings.configIncludeIf, section == ConfigSection.IncludeIf) {
                    section = ConfigSection.IncludeIf
                }
                Spacer(Modifier.width(8.dp))
                TermTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = strings.configFilterPlaceholder,
                    modifier = Modifier.weight(1f),
                )
            }
            confirmation?.let { kind ->
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Term.amber.copy(alpha = 0.12f))
                        .border(1.dp, Term.amber.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.configScopeConfirmation,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            fontWeight = FontWeight.Bold,
                            color = Term.amber,
                        )
                        Text(
                            "${scopeLabel(confirmationScope, strings)} · ${confirmationKey}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        key = null,
                        label = strings.configCancel,
                        enabled = !loading && !controller.busy,
                        onClick = ::cancelConfirmation,
                    )
                    Spacer(Modifier.width(5.dp))
                    ActionButton(
                        key = null,
                        label = strings.configConfirm,
                        danger = kind == ConfigConfirmation.Delete,
                        enabled = !loading && !controller.busy,
                        onClick = {
                            val action = confirmationAction
                            cancelConfirmation()
                            action?.invoke()
                        },
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Divider()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(470.dp).fillMaxHeight().padding(top = 8.dp)) {
                    if (loading) {
                        Text("loading…", fontSize = 9.sp, fontFamily = Term.mono, color = Term.faint)
                    } else if (filtered.isEmpty()) {
                        Text(
                            strings.configNoEntries,
                            fontSize = 9.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered) { entry ->
                                ConfigRow(
                                    entry = entry,
                                    selected = original?.let {
                                        it.scope == entry.scope && it.key == entry.key && it.value == entry.value
                                    } == true,
                                    strings = strings,
                                    onClick = { select(entry) },
                                )
                            }
                        }
                    }
                }
                VDivider()
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp, top = 8.dp),
                ) {
                    SectionLabel(strings.configKey)
                    TermTextField(
                        value = editKey,
                        onValueChange = {
                            editKey = it
                            notice = null
                            cancelConfirmation()
                        },
                        placeholder = "section.name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel(strings.configValue)
                    TermTextArea(
                        value = editValue,
                        onValueChange = {
                            editValue = it
                            notice = null
                            cancelConfirmation()
                        },
                        placeholder = strings.configValue,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${strings.configType}: ${typeLabel(inferGitConfigValueType(editKey), strings)}",
                        fontSize = 8.5.sp,
                        fontFamily = Term.mono,
                        color = Term.faint,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.configScopeHint,
                        fontSize = 8.sp,
                        fontFamily = Term.mono,
                        color = Term.amber,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScopeChip(
                            strings.configScopeSystem,
                            editScope == GitConfigScope.System,
                        ) {
                            editScope = GitConfigScope.System
                            cancelConfirmation()
                        }
                        ScopeChip(
                            strings.configScopeGlobal,
                            editScope == GitConfigScope.Global,
                        ) {
                            editScope = GitConfigScope.Global
                            cancelConfirmation()
                        }
                        ScopeChip(
                            strings.configScopeLocal,
                            editScope == GitConfigScope.Local,
                        ) {
                            editScope = GitConfigScope.Local
                            cancelConfirmation()
                        }
                        ScopeChip(
                            strings.configScopeWorktree,
                            editScope == GitConfigScope.Worktree,
                        ) {
                            editScope = GitConfigScope.Worktree
                            cancelConfirmation()
                        }
                    }
                    original?.let { entry ->
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "${strings.configOrigin}: ${entry.origin}",
                            fontSize = 8.sp,
                            fontFamily = Term.mono,
                            color = Term.faint,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        ErrorBanner(it, onDismiss = { error = null })
                    }
                    notice?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            fontSize = 8.5.sp,
                            fontFamily = Term.mono,
                            color = Term.green,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionButton(
                            key = null,
                            label = strings.configDelete,
                            danger = true,
                            enabled = original != null && !loading && !controller.busy,
                            onClick = ::requestDelete,
                        )
                        Spacer(Modifier.width(7.dp))
                        ActionButton(
                            key = null,
                            label = strings.configSave,
                            primary = true,
                            enabled = editKey.isNotBlank() && !loading && !controller.busy,
                            onClick = ::requestSave,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(
    entry: GitConfigEntry,
    selected: Boolean,
    strings: Strings,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) Term.raised else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.key,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = if (entry.isAlias) Term.cyan else Term.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                entry.scope.name.lowercase(),
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.amber,
                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
            )
            Text(
                typeLabel(entry.type, strings),
                fontSize = 7.5.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
        Text(
            entry.value.replace("\n", "\\n"),
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        color = if (selected) Term.black else Term.dim,
        modifier = Modifier
            .background(if (selected) Term.green else Term.panelAlt)
            .border(1.dp, if (selected) Term.green else Term.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun SectionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 8.sp,
        fontFamily = Term.mono,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Term.green else Term.faint,
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, if (selected) Term.greenDim else Term.border)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
    Spacer(Modifier.width(4.dp))
}

private fun scopeLabel(scope: GitConfigScope, strings: Strings): String = when (scope) {
    GitConfigScope.System -> strings.configScopeSystem
    GitConfigScope.Global -> strings.configScopeGlobal
    GitConfigScope.Local -> strings.configScopeLocal
    GitConfigScope.Worktree -> strings.configScopeWorktree
}

private fun scopeOrder(scope: GitConfigScope): Int = when (scope) {
    GitConfigScope.System -> 0
    GitConfigScope.Global -> 1
    GitConfigScope.Local -> 2
    GitConfigScope.Worktree -> 3
}

private fun typeLabel(type: GitConfigValueType, strings: Strings): String = when (type) {
    GitConfigValueType.String -> strings.configTypeString
    GitConfigValueType.Boolean -> strings.configTypeBoolean
    GitConfigValueType.Integer -> strings.configTypeInteger
    GitConfigValueType.Color -> strings.configTypeColor
    GitConfigValueType.Path -> strings.configTypePath
    GitConfigValueType.BooleanOrNamedValue -> strings.configTypeBooleanOrNamed
}
