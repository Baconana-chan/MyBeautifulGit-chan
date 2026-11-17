package org.baconana.mybeautifulgit_chan.domain

enum class PullMode(val flag: String) {
    Merge("--no-rebase"),
    Rebase("--rebase"),
    FfOnly("--ff-only"),
}

/**
 * Режим всегда задаётся флагом, а не pull.rebase/pull.ff из конфигурации.
 * --no-edit не даёт git открыть редактор для merge-коммита.
 */
fun pullArgs(
    remote: String,
    branch: String,
    mode: PullMode,
    verifySignatures: Boolean = false,
): List<String> = buildList {
    add("pull")
    add(mode.flag)
    if (mode == PullMode.Merge) {
        add("--no-edit")
        if (verifySignatures) add("--verify-signatures")
    }
    add(remote.trim())
    add(branch.trim())
}
