package org.baconana.mybeautifulgit_chan.domain

enum class PushMode {
    CurrentBranch,
    AnotherBranch,
    AllTags,
    AllBranches,
    Mirror,
    DeleteBranch,
    Refspec,
}

enum class PushForce(val flag: String?) {
    None(null),
    Lease("--force-with-lease"),
    Force("--force"),
}

data class PushOptions(
    val mode: PushMode = PushMode.CurrentBranch,
    val source: String = "",
    val destination: String = "",
    val branch: String = "",
    val refspec: String = "",
    val force: PushForce = PushForce.None,
)

/** Собирает точную команду, которую выполняет контроллер. */
fun pushArgs(remote: String, options: PushOptions): List<String> = buildList {
    add("push")
    if (options.mode != PushMode.DeleteBranch) {
        options.force.flag?.let(::add)
    }
    when (options.mode) {
        PushMode.CurrentBranch -> {
            add("--set-upstream")
            add(remote.trim())
            add(options.source.trim())
        }

        PushMode.AnotherBranch -> {
            add(remote.trim())
            add("${options.source.trim()}:${options.destination.trim()}")
        }

        PushMode.AllTags -> {
            add(remote.trim())
            add("--tags")
        }

        PushMode.AllBranches -> {
            add(remote.trim())
            add("--all")
        }

        PushMode.Mirror -> {
            add(remote.trim())
            add("--mirror")
        }

        PushMode.DeleteBranch -> {
            add(remote.trim())
            add("--delete")
            add(options.branch.trim())
        }

        PushMode.Refspec -> {
            add(remote.trim())
            add(options.refspec.trim())
        }
    }
}

fun validPushRefspec(value: String): Boolean {
    val clean = value.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        clean.none { it.isWhitespace() || it.isISOControl() }
}