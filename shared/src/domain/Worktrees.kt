package org.baconana.mybeautifulgit_chan.domain

/** Options for `git worktree add`. */
data class WorktreeAddOptions(
    val path: String,
    val branch: String = "",
    val base: String = "",
    val createBranch: Boolean = false,
)

/** A path selected for a worktree action; relative paths are resolved by git. */
fun worktreeAddArgs(options: WorktreeAddOptions): List<String> = buildList {
    add("worktree")
    add("add")
    if (options.createBranch) {
        add("-b")
        add(options.branch.trim())
    }
    add(options.path.trim())
    val branch = options.branch.trim()
    val base = options.base.trim()
    // Для существующей ветки/commit-ish второй positional аргумент — это она сама;
    // отдельный start point нужен только при создании ветки или без неё.
    when {
        branch.isNotEmpty() && !options.createBranch -> add(branch)
        base.isNotEmpty() -> add(base)
    }
}

fun worktreeRemoveArgs(path: String, force: Boolean): List<String> = buildList {
    add("worktree")
    add("remove")
    if (force) add("--force")
    add("--")
    add(path.trim())
}

fun worktreeLockArgs(path: String, reason: String? = null): List<String> = buildList {
    add("worktree")
    add("lock")
    val cleanReason = reason?.trim().orEmpty()
    if (cleanReason.isNotEmpty()) {
        add("--reason")
        add(cleanReason)
    }
    add("--")
    add(path.trim())
}

fun worktreeUnlockArgs(path: String): List<String> = listOf(
    "worktree",
    "unlock",
    "--",
    path.trim(),
)

fun worktreePruneArgs(dryRun: Boolean): List<String> = buildList {
    add("worktree")
    add("prune")
    if (dryRun) add("--dry-run")
    add("--verbose")
    // Иначе git может держать отсутствующий worktree ещё несколько месяцев.
    add("--expire")
    add("now")
}

/**
 * Switches [path] to [branch], optionally creating it from [base].
 * `git -C` keeps the command independent of the currently opened worktree.
 */
fun worktreeSwitchArgs(
    path: String,
    branch: String,
    create: Boolean,
    base: String = "",
): List<String> = buildList {
    add("-C")
    add(path.trim())
    add("switch")
    if (create) add("-c")
    add(branch.trim())
    val cleanBase = base.trim()
    if (create && cleanBase.isNotEmpty()) add(cleanBase)
}

fun validWorktreePath(path: String): Boolean {
    val clean = path.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        clean.none { it == '\n' || it == '\r' || it == '\u0000' }
}
