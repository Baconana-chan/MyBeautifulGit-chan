package org.baconana.mybeautifulgit_chan.domain

/** Config scopes exposed by `git config --show-scope`. */
enum class GitConfigScope(val flag: String) {
    System("--system"),
    Global("--global"),
    Local("--local"),
    Worktree("--worktree"),
}

/** System/global writes affect every repository on the machine. */
fun requiresGitConfigScopeConfirmation(scope: GitConfigScope): Boolean =
    scope == GitConfigScope.System || scope == GitConfigScope.Global

/**
 * The type is inferred for the keys whose Git syntax is well known.
 * Unknown keys are deliberately kept as String: the editor must still be able
 * to edit configuration written by other tools or newer Git versions.
 */
enum class GitConfigValueType {
    String,
    Boolean,
    Integer,
    Color,
    Path,
    BooleanOrNamedValue,
}

data class GitConfigEntry(
    val key: String,
    val value: String,
    val scope: GitConfigScope,
    val origin: String = "",
) {
    val type: GitConfigValueType
        get() = inferGitConfigValueType(key)

    val isAlias: Boolean
        get() = key.startsWith("alias.", ignoreCase = true)

    val isIncludeIf: Boolean
        get() = key.startsWith("includeIf.", ignoreCase = true)
}

/** Исполняемый git-alias. Команда остаётся строкой, которую раскрывает сам git. */
data class GitAlias(
    val name: String,
    val command: String,
    val scope: GitConfigScope,
) {
    val isShell: Boolean
        get() = command.trimStart().startsWith("!")
}

/**
 * Собирает aliases из уже прочитанного config. Значения из более позднего
 * scope перекрывают ранние, как это делает git config. Непригодные для запуска
 * имена (пробелы, управляющие символы, ведущий '-') не попадают в UI.
 */
fun gitAliases(entries: List<GitConfigEntry>): List<GitAlias> {
    val aliases = linkedMapOf<String, GitAlias>()
    for (entry in entries) {
        if (!entry.isAlias) continue
        val name = entry.key.substringAfter('.', missingDelimiterValue = "")
        if (name.isBlank() ||
            name.startsWith('-') ||
            name.any { it.isWhitespace() || it.isISOControl() } ||
            entry.value.isBlank()
        ) {
            continue
        }
        aliases[name.lowercase()] = GitAlias(name, entry.value, entry.scope)
    }
    return aliases.values.sortedBy { it.name.lowercase() }
}

enum class GitConfigValidationError {
    KeyRequired,
    KeyInvalid,
    AliasNameRequired,
    AliasNameInvalid,
    BooleanInvalid,
    IntegerInvalid,
    ColorInvalid,
    IncludeIfConditionInvalid,
    IncludeIfPathRequired,
    ValueInvalid,
}

data class GitConfigValidation(
    val type: GitConfigValueType,
    val error: GitConfigValidationError? = null,
) {
    val ok: Boolean
        get() = error == null
}

private val booleanKeys = setOf(
    "core.bare",
    "core.ignorecase",
    "core.filemode",
    "core.symlinks",
    "core.logallrefupdates",
    "core.quotepath",
    "core.sparsecheckout",
    "core.untrackedcache",
    "core.commitgraph",
    "core.usereplacerefs",
    "commit.gpgsign",
    "commit.status",
    "commit.verbose",
    "tag.gpgsign",
    "push.followtags",
    "push.gpgsign",
    "push.autosetupremote",
    "fetch.prune",
    "fetch.prunetags",
    "fetch.writecommitgraph",
    "rebase.autosquash",
    "rebase.autostash",
    "rerere.enabled",
    "rerere.autoupdate",
    "http.sslverify",
    "http.followredirects",
    "credential.interactive",
    "log.showsignature",
    "maintenance.auto",
    "submodule.recurse",
    "worktree.guessremote",
    "gc.autodetach",
    "gc.writecommitgraph",
)

private val integerKeys = setOf(
    "core.compression",
    "core.bigfilethreshold",
    "core.packedgitlimit",
    "core.packedgitwindowsize",
    "core.deltabasecachelimit",
    "gc.auto",
    "gc.autopacklimit",
    "gc.packrefs",
    "pack.window",
    "pack.depth",
    "pack.threads",
    "index.threads",
    "http.postbuffer",
    "http.lowspeedlimit",
    "http.lowspeedtime",
    "protocol.version",
    "log.date",
)

private val booleanOrNamedKeys = setOf(
    "core.autocrlf",
    "pull.rebase",
    "protocol.ext.allow",
    "diff.renames",
)

private val namedBooleanValues = mapOf(
    "core.autocrlf" to setOf("input"),
    "pull.rebase" to setOf("merges", "preserve", "interactive"),
    "protocol.ext.allow" to setOf("always", "never", "user"),
    "diff.renames" to setOf("copies", "copies-harder"),
)

private val colorKeys = setOf(
    "color.ui",
    "color.pager",
    "color.interactive",
    "color.branch",
    "color.diff",
    "color.status",
    "color.decorate",
    "color.grep",
    "color.remote",
    "color.showbranch",
)

private val colorNames = setOf(
    "normal",
    "black",
    "red",
    "green",
    "yellow",
    "blue",
    "magenta",
    "cyan",
    "white",
    "default",
    "bold",
    "dim",
    "ul",
    "blink",
    "reverse",
    "italic",
    "strikethrough",
)

private val includeIfPrefixes = listOf(
    "gitdir:",
    "gitdir/i:",
    "onbranch:",
    "hasconfig:remote.",
)

fun inferGitConfigValueType(key: String): GitConfigValueType {
    val lower = key.lowercase()
    return when {
        lower.startsWith("alias.") -> GitConfigValueType.String
        lower.startsWith("includeif.") -> GitConfigValueType.Path
        lower in booleanKeys || lower.startsWith("advice.") -> GitConfigValueType.Boolean
        lower in integerKeys -> GitConfigValueType.Integer
        lower in booleanOrNamedKeys -> GitConfigValueType.BooleanOrNamedValue
        lower in colorKeys || lower.startsWith("color.") -> GitConfigValueType.Color
        else -> GitConfigValueType.String
    }
}

/**
 * A key is passed to `git config` as a single argv item. Reject option-like
 * and control-character keys, but keep arbitrary subsection characters so
 * unknown and third-party keys remain editable.
 */
fun validGitConfigKey(key: String): Boolean {
    if (key.isEmpty() || key.startsWith("-") || !key.contains('.')) return false
    if (key.any { it == '=' || it == '\u0000' || it == '\n' || it == '\r' || it.isISOControl() }) {
        return false
    }
    return true
}

fun validateGitConfigEntry(key: String, value: String): GitConfigValidation {
    val type = inferGitConfigValueType(key)
    if (key.isEmpty()) return GitConfigValidation(type, GitConfigValidationError.KeyRequired)
    if (!validGitConfigKey(key)) return GitConfigValidation(type, GitConfigValidationError.KeyInvalid)
    if (value.contains('\u0000')) {
        return GitConfigValidation(type, GitConfigValidationError.ValueInvalid)
    }

    val lower = key.lowercase()
    if (lower.startsWith("alias.")) {
        val name = key.substring("alias.".length)
        if (name.isEmpty()) return GitConfigValidation(type, GitConfigValidationError.AliasNameRequired)
        if (name.any { it.isWhitespace() || it.isISOControl() }) {
            return GitConfigValidation(type, GitConfigValidationError.AliasNameInvalid)
        }
        if (value.isBlank()) return GitConfigValidation(type, GitConfigValidationError.ValueInvalid)
    }

    if (lower.startsWith("includeif.")) {
        val suffix = ".path"
        val rest = key.substring("includeIf.".length)
        if (!rest.endsWith(suffix, ignoreCase = true)) {
            return GitConfigValidation(type, GitConfigValidationError.IncludeIfConditionInvalid)
        }
        val condition = rest.dropLast(suffix.length)
        if (condition.isEmpty() || includeIfPrefixes.none { condition.lowercase().startsWith(it) }) {
            return GitConfigValidation(type, GitConfigValidationError.IncludeIfConditionInvalid)
        }
        if (value.isBlank()) {
            return GitConfigValidation(type, GitConfigValidationError.IncludeIfPathRequired)
        }
    }

    when (type) {
        GitConfigValueType.Boolean -> if (!isGitBoolean(value)) {
            return GitConfigValidation(type, GitConfigValidationError.BooleanInvalid)
        }

        GitConfigValueType.Integer -> if (!isGitInteger(value)) {
            return GitConfigValidation(type, GitConfigValidationError.IntegerInvalid)
        }

        GitConfigValueType.Color -> if (!isGitColor(value)) {
            return GitConfigValidation(type, GitConfigValidationError.ColorInvalid)
        }

        GitConfigValueType.BooleanOrNamedValue -> {
            val normalized = value.trim().lowercase()
            val allowed = namedBooleanValues[lower].orEmpty()
            if (!isGitBoolean(value) && normalized !in allowed) {
                return GitConfigValidation(type, GitConfigValidationError.BooleanInvalid)
            }
        }

        GitConfigValueType.Path, GitConfigValueType.String -> Unit
    }
    return GitConfigValidation(type)
}

private fun isGitBoolean(value: String): Boolean =
    value.trim().lowercase() in setOf("true", "false", "yes", "no", "on", "off", "1", "0")

private fun isGitInteger(value: String): Boolean =
    value.trim().matches(Regex("^[+-]?[0-9]+(?:[kKmMgG])?$"))

private fun isGitColor(value: String): Boolean {
    val normalized = value.trim().lowercase()
    if (normalized.isEmpty()) return false
    if (normalized in setOf("auto", "always", "never", "true", "false")) return true
    if (normalized in colorNames) return true
    if (normalized.matches(Regex("^#[0-9a-f]{6}$"))) return true
    if (normalized.matches(Regex("^ansi[0-9]{1,3}$"))) return true
    return normalized.split(' ').all { it in colorNames }
}
