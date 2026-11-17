package org.baconana.mybeautifulgit_chan.domain

enum class GitOperationKind {
    Merge,
    Rebase,
    CherryPick,
    Revert,
    Am,
    Bisect,
}

/** Действия над незавершённой операцией. */
enum class OperationAction { Continue, Abort, Skip }

/**
 * Аргументы git для действия. null означает, что операция не поддерживает
 * это действие: у merge нет `--skip`, у bisect нет этих команд вообще.
 */
fun GitOperationKind.operationArgs(action: OperationAction): List<String>? = when (this) {
    GitOperationKind.Merge -> when (action) {
        OperationAction.Continue -> listOf("merge", "--continue")
        OperationAction.Abort -> listOf("merge", "--abort")
        OperationAction.Skip -> null
    }

    GitOperationKind.Rebase -> when (action) {
        OperationAction.Continue -> listOf("rebase", "--continue")
        OperationAction.Abort -> listOf("rebase", "--abort")
        OperationAction.Skip -> listOf("rebase", "--skip")
    }

    GitOperationKind.CherryPick -> when (action) {
        OperationAction.Continue -> listOf("cherry-pick", "--continue")
        OperationAction.Abort -> listOf("cherry-pick", "--abort")
        OperationAction.Skip -> listOf("cherry-pick", "--skip")
    }

    GitOperationKind.Revert -> when (action) {
        OperationAction.Continue -> listOf("revert", "--continue")
        OperationAction.Abort -> listOf("revert", "--abort")
        OperationAction.Skip -> listOf("revert", "--skip")
    }

    GitOperationKind.Am -> when (action) {
        OperationAction.Continue -> listOf("am", "--continue")
        OperationAction.Abort -> listOf("am", "--abort")
        OperationAction.Skip -> listOf("am", "--skip")
    }

    GitOperationKind.Bisect -> null
}

/**
 * Незавершённая операция git, восстановленная по state-файлам в gitDir.
 *
 * [step] и [total] есть только у rebase/am, где git сам хранит прогресс.
 */
data class GitOperation(
    val kind: GitOperationKind,
    val step: Int? = null,
    val total: Int? = null,
) {
    val progress: String?
        get() = if (step != null && total != null) "$step/$total" else null
}

/** Оставленный `index.lock`; путь нужен, чтобы объяснить, что именно удаляется. */
data class IndexLock(
    val path: String,
)

/**
 * Быстрое состояние, которое читается без запуска git: текущая операция и lock.
 * Ошибки чтения не должны ломать status/log/refs, поэтому бэкенд возвращает
 * пустое состояние, если state-файлы недоступны.
 */
data class RepositoryRuntime(
    val operation: GitOperation? = null,
    val indexLock: IndexLock? = null,
)
