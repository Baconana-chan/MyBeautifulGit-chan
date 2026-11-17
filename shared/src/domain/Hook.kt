package org.baconana.mybeautifulgit_chan.domain

/**
 * Один git hook в текущем hooks-каталоге.
 *
 * Отключение выполняется переименованием активного файла в `<name>.disabled`:
 * это одинаково работает на Unix и Windows, где executable-bit не является
 * надёжным переключателем для git. Примеры из `git init` имеют суффикс
 * `.sample` и не исполняются git, пока их не включат.
 */
data class HookEntry(
    val name: String,
    val fileName: String,
    val path: String,
    val enabled: Boolean,
    val sample: Boolean,
    val disabled: Boolean,
    val executable: Boolean,
    val size: Long,
)

data class HookDetail(
    val name: String,
    val path: String,
    val content: String,
    val truncated: Boolean = false,
)

data class HooksState(
    val directory: String = "",
    val configuredPath: String = "",
    val entries: List<HookEntry> = emptyList(),
    /** True when core.hooksPath resolves outside the repository's common .git directory. */
    val outsideCommonDir: Boolean = false,
    /** True when the effective directory is not the default commonDir/hooks directory. */
    val outsideHooksDir: Boolean = false,
)

/** Последний stdout/stderr git-операции; hook-скрипты пишут туда же. */
data class HookOutput(
    val command: String = "",
    val exitCode: Int? = null,
    val success: Boolean = false,
    val text: String = "",
)

fun validHookName(value: String): Boolean {
    val clean = value.trim()
    return clean.isNotEmpty() &&
        clean != "." &&
        clean != ".." &&
        !clean.startsWith("-") &&
        !clean.contains('/') &&
        !clean.contains('\\') &&
        clean.none { it.isWhitespace() || it.isISOControl() }
}
