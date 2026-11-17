package org.baconana.mybeautifulgit_chan.domain

data class SubmoduleUpdateOptions(
    val init: Boolean = true,
    val recursive: Boolean = true,
    /** `true` forces a checkout and bypasses submodule.<name>.update from local config. */
    val checkout: Boolean = true,
)

/**
 * Commands are built here so the UI can show exactly what the controller will run.
 * Paths are passed after `--`; init/update/sync accept an optional selected path.
 */
fun submoduleInitArgs(path: String? = null): List<String> = buildList {
    add("submodule")
    add("init")
    path?.trim()?.takeIf { it.isNotEmpty() }?.let {
        add("--")
        add(it)
    }
}

fun submoduleUpdateArgs(
    path: String? = null,
    options: SubmoduleUpdateOptions = SubmoduleUpdateOptions(),
): List<String> = buildList {
    add("submodule")
    add("update")
    if (options.init) add("--init")
    if (options.recursive) add("--recursive")
    if (options.checkout) add("--checkout")
    path?.trim()?.takeIf { it.isNotEmpty() }?.let {
        add("--")
        add(it)
    }
}

fun submoduleSyncArgs(path: String? = null, recursive: Boolean = false): List<String> = buildList {
    add("submodule")
    add("sync")
    if (recursive) add("--recursive")
    path?.trim()?.takeIf { it.isNotEmpty() }?.let {
        add("--")
        add(it)
    }
}

fun submoduleAddArgs(url: String, path: String): List<String> = listOf(
    "submodule",
    "add",
    "--",
    url.trim(),
    path.trim(),
)

/** deinit stops the submodule; git rm removes its gitlink and .gitmodules entry. */
fun submoduleDeinitArgs(path: String): List<String> =
    listOf("submodule", "deinit", "-f", "--", path.trim())

fun submoduleRemoveArgs(path: String): List<String> =
    listOf("rm", "-f", "--", path.trim())

fun validSubmodulePath(value: String): Boolean {
    val clean = value.trim()
    if (clean.isEmpty() || clean.startsWith("-")) return false
    if (clean.startsWith("/") || clean.startsWith("\\") || Regex("^[A-Za-z]:").containsMatchIn(clean)) {
        return false
    }
    if (clean.any { it.isISOControl() || it == '\\' }) return false
    if (clean.endsWith("/") || clean.contains("//")) return false
    val parts = clean.split('/')
    if (parts.any { it.isEmpty() || it == "." || it == ".." || it == ".git" }) return false
    return true
}

fun validSubmoduleUrl(value: String): Boolean = validTransportUrl(value)
